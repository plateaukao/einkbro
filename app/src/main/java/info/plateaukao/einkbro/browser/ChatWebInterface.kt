package info.plateaukao.einkbro.browser

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.data.remote.ApiResult
import info.plateaukao.einkbro.data.remote.ChatMessage
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.data.remote.ToolCall
import info.plateaukao.einkbro.data.remote.ToolChatMessage
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.task.AgentToolSchema
import info.plateaukao.einkbro.task.BrowserTools
import info.plateaukao.einkbro.task.BrowserToolsImpl
import info.plateaukao.einkbro.task.InitialPageSnapshot
import info.plateaukao.einkbro.task.TaskProgress
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ChatWebInterface(
    val lifecycleScope: LifecycleCoroutineScope,
    private val webView: WebView,
    private var webContent: String,
    private var webTitle: String,
    private var webUrl: String,
    onOpenNewTab: ((String) -> Unit)? = null,
    // Agent-mode extensions: when [agentMode] is true, every user message is routed
    // through a tool-calling loop (chatWithTools) instead of the regular chatStream
    // path. The extra deps are only touched in agent mode.
    private val agentMode: Boolean = false,
    private val initialSnapshot: InitialPageSnapshot? = null,
    private val agentContext: Context? = null,
    private val agentWebViewCallback: WebViewCallback? = null,
    private val agentBrowserState: BrowserState? = null,
    private val agentTtsViewModel: TtsViewModel? = null,
) : KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()

    // Non-agent mode: plain text chat history (role/content pairs).
    private val chatHistory: MutableList<ChatMessage> = mutableListOf()

    // Agent mode: tool-calling chat history (supports tool_call_id + tool_calls).
    private val toolHistory: MutableList<ToolChatMessage> = mutableListOf()

    private val jsHelper = JsHelper(webView, lifecycleScope, onOpenNewTab)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Off-screen browser tool façade for agent mode. Lazy so non-agent chat tabs never
     * allocate one. Captures [initialSnapshot] so the agent can inspect the originating
     * page even after the chat tab has replaced it as the active tab.
     */
    private val agentTools: BrowserToolsImpl by lazy {
        require(agentContext != null && agentWebViewCallback != null &&
            agentBrowserState != null && agentTtsViewModel != null) {
            "agent-mode deps not wired; check setupAiPage call site"
        }
        BrowserToolsImpl(
            context = agentContext,
            webViewCallback = agentWebViewCallback,
            browserState = agentBrowserState,
            config = configManager,
            openAiRepository = openAiRepository,
            ttsViewModel = agentTtsViewModel,
            progressSink = { /* step lines are emitted directly via appendBubble */ },
            finishSink = { /* finish is handled in the agent loop, not through the sink */ },
            initialSnapshot = initialSnapshot,
        )
    }
    private var agentToolsInitialized = false

    companion object {
        private const val WEB_CONTENT_MESSAGE_SUFFIX = "\n this is the web content;"
        private const val MAX_AGENT_ITERATIONS = 12
        private const val MAX_TOOL_RESULT_CHARS = 8_000
        private const val MAX_LINKS_RETURNED = 50
        private const val TAG = "ChatWebInterface"
    }

    @JavascriptInterface
    fun sendMessage(message: String) {
        val chatGptActionInfo = createChatGptActionInfo(message)
        lifecycleScope.launch(Dispatchers.Main) {
            sendMessageWithGptActionInfo(chatGptActionInfo)
        }
    }

    @JavascriptInterface
    fun getWebMetadata(): String {
        return """{"title": "${escapeJsonString(webTitle)}", "url": "${escapeJsonString(webUrl)}"}"""
    }

    @JavascriptInterface
    fun openUrlInNewTab(url: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            jsHelper.openUrlInNewTab(url)
        }
    }

    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun updateWebContent(
        newWebContent: String,
        newWebTitle: String = webTitle,
        newWebUrl: String = webUrl,
    ) {
        this.webContent = newWebContent
        this.webTitle = newWebTitle
        this.webUrl = newWebUrl
    }

    fun sendMessageWithGptActionInfo(gptActionInfo: ChatGPTActionInfo) {
        if (agentMode) {
            runAgentTurn(gptActionInfo.userMessage)
            return
        }

        val currentUserMessage = gptActionInfo.userMessage.toUserMessage()

        val messagesForApi = mutableListOf<ChatMessage>().apply {
            if (gptActionInfo.systemMessage.isNotEmpty()) {
                add(gptActionInfo.systemMessage.toSystemMessage())
            }
            add(createWebContentMessage(webContent))
            addAll(chatHistory)
            add(currentUserMessage)
        }

        val assistantResponseAggregator = StringBuilder()

        jsHelper.startMessageStream {
            lifecycleScope.launch(Dispatchers.IO) {
                openAiRepository.chatStream(
                    messages = messagesForApi,
                    gptActionInfo = gptActionInfo,
                    appendResponseAction = { responseChunk ->
                        jsHelper.sendStreamUpdate(responseChunk)
                        assistantResponseAggregator.append(responseChunk)
                    },
                    doneAction = {
                        jsHelper.sendFinalEmptyUpdate()
                        chatHistory.add(currentUserMessage)
                        chatHistory.add(assistantResponseAggregator.toString().toAssistantMessage())
                    },
                    failureAction = { failure ->
                        Timber.e("AI stream failure: ${failure.kind} ${failure.message}")
                        val userMessage = when (failure.kind) {
                            ApiResult.Kind.MissingKey -> failure.message
                            ApiResult.Kind.RateLimited -> failure.retryAfterSeconds
                                ?.let { "Rate limited — retry after ${it}s" }
                                ?: "Rate limited — try again shortly"
                            ApiResult.Kind.Network -> "Network error — check connection"
                            else -> "AI request failed: ${failure.message}"
                        }
                        jsHelper.sendErrorUpdate(userMessage)
                    }
                )
            }
        }
    }

    // ── Agent mode ─────────────────────────────────────────────────────

    /**
     * Public entry point used by `EBWebView.setupAiPage` to kick off an initial agent
     * turn after the chat HTML has finished loading.
     */
    fun runAgentTurn(userMessage: String) {
        if (configManager.ai.useGeminiApi) {
            jsHelper.sendErrorUpdate("Custom tasks require OpenAI (not Gemini).")
            return
        }
        jsHelper.startMessageStream {
            lifecycleScope.launch { agentLoop(userMessage) }
        }
    }

    private suspend fun agentLoop(userMessage: String) {
        val actionInfo = buildAgentActionInfo()

        // First turn seeds the history with a system prompt that includes the
        // originating-page hint so the model can orient itself without a tool call.
        if (toolHistory.isEmpty()) {
            toolHistory += ToolChatMessage(
                role = "system",
                content = AgentToolSchema.SYSTEM_PROMPT + buildSnapshotHint(),
            )
        }
        toolHistory += ToolChatMessage(role = "user", content = userMessage)

        var iter = 0
        while (iter < MAX_AGENT_ITERATIONS) {
            iter++
            val resp = withContext(Dispatchers.IO) {
                openAiRepository.chatWithTools(toolHistory, AgentToolSchema.tools, actionInfo)
            }
            if (resp == null) {
                appendBubble("\n\n_(LLM call failed on turn $iter)_")
                jsHelper.sendFinalEmptyUpdate()
                return
            }
            val msg = resp.choices.firstOrNull()?.message
            if (msg == null) {
                appendBubble("\n\n_(empty response)_")
                jsHelper.sendFinalEmptyUpdate()
                return
            }

            val toolCalls = msg.toolCalls.orEmpty()

            if (toolCalls.isEmpty()) {
                val text = msg.content.orEmpty().ifBlank { "_(no response)_" }
                toolHistory += ToolChatMessage(role = "assistant", content = text)
                appendBubble("\n\n$text")
                jsHelper.sendFinalEmptyUpdate()
                return
            }

            // Record the assistant tool-call turn so subsequent API calls see
            // a valid conversation transcript.
            toolHistory += ToolChatMessage(
                role = "assistant",
                content = msg.content,
                toolCalls = toolCalls,
            )

            for (call in toolCalls) {
                val preview = call.function.arguments.take(120)
                appendBubble("\n\n🔧 `${call.function.name}` — $preview")
                val result = dispatchAgentTool(call)
                toolHistory += ToolChatMessage(
                    role = "tool",
                    toolCallId = call.id,
                    content = result,
                )
                if (call.function.name == "finish") {
                    // finish already streamed its summary via dispatchAgentTool.
                    jsHelper.sendFinalEmptyUpdate()
                    return
                }
            }
        }

        appendBubble("\n\n_(task did not complete within $MAX_AGENT_ITERATIONS turns)_")
        jsHelper.sendFinalEmptyUpdate()
    }

    private suspend fun dispatchAgentTool(call: ToolCall): String {
        return try {
            val args: JsonObject = try {
                json.parseToJsonElement(call.function.arguments.ifBlank { "{}" }).jsonObject
            } catch (e: Exception) {
                return "error: invalid JSON arguments: ${e.message}"
            }
            when (call.function.name) {
                "get_initial_page_links" -> {
                    val links = agentTools.initialPageLinks()
                    agentToolsInitialized = true
                    encodeLinks(links)
                }
                "read_initial_page" -> {
                    agentToolsInitialized = true
                    val text = agentTools.initialPageText().trim()
                    if (text.isBlank()) "error: no initial page text captured"
                    else text.take(MAX_TOOL_RESULT_CHARS)
                }
                "open_url" -> {
                    val url = args["url"]?.jsonPrimitive?.contentOrNull
                        ?: return "error: missing url"
                    agentToolsInitialized = true
                    val ok = agentTools.openUrlInBg(url)
                    if (ok) "ok: loaded $url" else "error: failed to load $url"
                }
                "read_current_page" -> {
                    agentToolsInitialized = true
                    val text = agentTools.currentBgPageText().trim()
                    if (text.isBlank()) "error: no page loaded or page body is empty"
                    else text.take(MAX_TOOL_RESULT_CHARS)
                }
                "get_page_links" -> {
                    agentToolsInitialized = true
                    val links = agentTools.currentBgPageLinks()
                    encodeLinks(links)
                }
                "note" -> {
                    val text = args["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (text.isNotBlank()) appendBubble("\n\n_${escapeMd(text)}_")
                    "ok"
                }
                "speak" -> {
                    val text = args["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val title = args["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (text.isBlank()) {
                        "error: missing text"
                    } else {
                        agentToolsInitialized = true
                        agentTools.speak(text, title)
                        "ok: queued ${text.length} chars for TTS"
                    }
                }
                "read_initial_html" -> {
                    agentToolsInitialized = true
                    val html = agentTools.initialPageRawHtml()
                    if (html.isBlank()) "error: no initial page HTML captured"
                    else html.take(MAX_TOOL_RESULT_CHARS)
                }
                "get_domain_javascript" -> {
                    agentToolsInitialized = true
                    val js = agentTools.getInitialDomainJavascript()
                    if (js.isBlank()) "(no postLoadJavascript saved for this host)" else js
                }
                "get_domain_css" -> {
                    agentToolsInitialized = true
                    val css = agentTools.getInitialDomainCss()
                    if (css.isBlank()) "(no customCss saved for this host)" else css
                }
                "set_domain_javascript" -> {
                    val code = args["code"]?.jsonPrimitive?.contentOrNull
                        ?: return "error: missing code"
                    agentToolsInitialized = true
                    agentTools.setInitialDomainJavascript(code)
                    val host = android.net.Uri.parse(agentTools.initialPageUrl()).host.orEmpty()
                    if (code.isBlank()) "ok: cleared postLoadJavascript for $host"
                    else "ok: saved ${code.length} chars of postLoadJavascript for $host"
                }
                "set_domain_css" -> {
                    val code = args["code"]?.jsonPrimitive?.contentOrNull
                        ?: return "error: missing code"
                    agentToolsInitialized = true
                    agentTools.setInitialDomainCss(code)
                    val host = android.net.Uri.parse(agentTools.initialPageUrl()).host.orEmpty()
                    if (code.isBlank()) "ok: cleared customCss for $host"
                    else "ok: saved ${code.length} chars of customCss for $host"
                }
                "finish" -> {
                    val summary = args["summary"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (summary.isNotBlank()) appendBubble("\n\n---\n\n$summary")
                    "ok"
                }
                else -> "error: unknown tool ${call.function.name}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool dispatch failed", e)
            "error: ${e.message}"
        }
    }

    private fun encodeLinks(links: List<BrowserTools.Link>): String {
        if (links.isEmpty()) return "[]"
        val array: JsonArray = buildJsonArray {
            links.take(MAX_LINKS_RETURNED).forEach { link ->
                add(buildJsonObject {
                    put("text", JsonPrimitive(link.text))
                    put("href", JsonPrimitive(link.href))
                })
            }
        }
        return array.toString()
    }

    private fun buildSnapshotHint(): String {
        val s = initialSnapshot ?: return ""
        return "\n\nThe user is currently viewing: \"${s.title}\" at ${s.url}."
    }

    private fun buildAgentActionInfo(): ChatGPTActionInfo = ChatGPTActionInfo(
        name = "agent",
        systemMessage = AgentToolSchema.SYSTEM_PROMPT,
        userMessage = "",
        actionType = if (configManager.ai.useCustomGptUrl) GptActionType.SelfHosted else GptActionType.OpenAi,
        model = if (configManager.ai.useCustomGptUrl) configManager.ai.alternativeModel else configManager.ai.gptModel,
    )

    private fun appendBubble(chunk: String) {
        jsHelper.sendStreamUpdate(chunk)
    }

    private fun escapeMd(s: String): String = s.replace("_", "\\_").replace("*", "\\*")

    /**
     * Call this from [EBWebView.destroy] to tear down the off-screen tool WebView.
     * Safe to call even if agent mode was never used.
     */
    fun disposeAgent() {
        if (agentToolsInitialized) {
            try {
                agentTools.dispose()
            } catch (e: Exception) {
                Log.e(TAG, "disposeAgent failed", e)
            }
        }
    }

    private fun createWebContentMessage(content: String): ChatMessage =
        "```$content```$WEB_CONTENT_MESSAGE_SUFFIX".toUserMessage()

    private fun createChatGptActionInfo(message: String): ChatGPTActionInfo =
        ChatGPTActionInfo(
            actionType = configManager.ai.gptForChatWeb,
            userMessage = message,
            model = configManager.ai.getGptTypeModelMap()[configManager.ai.gptForChatWeb]
                ?: configManager.ai.gptModel,
        )
}

/**
 * Helper class to manage JavaScript interactions with WebView.
 * This class is now stateless regarding message content, only forwarding to JS.
 */
class JsHelper(
    private val webView: WebView,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onOpenNewTab: ((String) -> Unit)? = null,
) {
    fun startMessageStream(postAction: () -> Unit = {}) {
        webView.evaluateJavascript("javascript:startMessageStream()") {
            postAction()
        }
    }

    fun sendStreamUpdate(messageChunk: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(messageChunk)}', true, false)",
                null
            )
        }
    }

    fun sendFinalEmptyUpdate() {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript("javascript:receiveMessageFromAndroid('', true, true)", null)
        }
    }

    fun sendErrorUpdate(errorMessage: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(errorMessage)}', true, true)",
                null
            )
        }
    }

    fun openUrlInNewTab(url: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            onOpenNewTab?.invoke(url)
        }
    }

    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
    }
}
