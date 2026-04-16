package info.plateaukao.einkbro.task

import android.content.Context
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.WebViewCallback
import info.plateaukao.einkbro.data.remote.ChatMessage
import info.plateaukao.einkbro.data.remote.ChatRole
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Default [BrowserTools] implementation backed by an off-screen [EBWebView] and the
 * shared [OpenAiRepository]. One instance is created per [TaskRunner] invocation and
 * [dispose]d when the task completes or is cancelled.
 */
class BrowserToolsImpl(
    private val context: Context,
    private val webViewCallback: WebViewCallback,
    private val browserState: BrowserState,
    private val config: ConfigManager,
    private val openAiRepository: OpenAiRepository,
    private val ttsViewModel: TtsViewModel,
    private val progressSink: (TaskProgress.StepLine) -> Unit,
    private val finishSink: (String) -> Unit,
    private val initialSnapshot: InitialPageSnapshot? = null,
) : BrowserTools {

    private var bgWebView: EBWebView? = null
    private var currentLoadDeferred: CompletableDeferred<Boolean>? = null

    private val json = Json { ignoreUnknownKeys = true }

    // ── Navigation / content ────────────────────────────────────────────

    override suspend fun openUrlInBg(url: String): Boolean {
        val webView = ensureBgWebView()
        val deferred = CompletableDeferred<Boolean>()
        currentLoadDeferred = deferred
        withContext(Dispatchers.Main) { webView.loadUrl(url) }
        val ok = withTimeoutOrNull(PAGE_LOAD_TIMEOUT_MS) { deferred.await() } ?: false
        currentLoadDeferred = null
        return ok
    }

    override suspend fun currentBgPageText(): String {
        val wv = bgWebView ?: return ""
        return withContext(Dispatchers.Main) { wv.getRawText() }
    }

    override suspend fun currentBgPageLinks(): List<BrowserTools.Link> {
        val wv = bgWebView ?: return emptyList()
        val raw = withContext(Dispatchers.Main) { wv.jsBridge.getPageLinks() }
        return parseLinks(raw)
    }

    override suspend fun activeTabText(): String {
        val wv = browserState.ebWebView
        return withContext(Dispatchers.Main) { wv.getRawText() }
    }

    override suspend fun activeTabLinks(): List<BrowserTools.Link> {
        val wv = browserState.ebWebView
        val raw = withContext(Dispatchers.Main) { wv.jsBridge.getPageLinks() }
        return parseLinks(raw)
    }

    override fun activeTabUrl(): String = browserState.ebWebView.url.orEmpty()

    override fun activeTabTitle(): String = browserState.ebWebView.title.orEmpty()

    // ── Originating page snapshot ──────────────────────────────────────

    override fun initialPageUrl(): String = initialSnapshot?.url.orEmpty()
    override fun initialPageTitle(): String = initialSnapshot?.title.orEmpty()
    override fun initialPageText(): String = initialSnapshot?.text.orEmpty()
    override fun initialPageLinks(): List<BrowserTools.Link> = initialSnapshot?.links.orEmpty()

    private fun parseLinks(raw: String): List<BrowserTools.Link> {
        if (raw.isBlank() || raw == "null") return emptyList()
        return try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RawLink.serializer()), raw)
                .map { BrowserTools.Link(it.text, it.href) }
        } catch (e: Exception) {
            android.util.Log.e("BrowserToolsImpl", "Failed to parse links JSON: $raw", e)
            emptyList()
        }
    }

    @Serializable
    private data class RawLink(val text: String, val href: String)

    // ── LLM ─────────────────────────────────────────────────────────────

    override suspend fun askLlm(system: String, user: String): String? {
        val action = summarizeActionInfo()
        val messages = mutableListOf<ChatMessage>()
        if (system.isNotBlank()) messages += ChatMessage(content = system, role = ChatRole.System)
        messages += ChatMessage(content = user, role = ChatRole.User)

        return withContext(Dispatchers.IO) {
            if (action.actionType == GptActionType.Gemini) {
                openAiRepository.queryGemini(messages, action).valueOrNull()?.takeIf { it.isNotBlank() }
            } else {
                val completion = openAiRepository.chatCompletion(messages, action)
                completion?.choices
                    ?.firstOrNull { it.message.role == ChatRole.Assistant }
                    ?.message?.content
            }
        }
    }

    override fun summarizeActionInfo(): ChatGPTActionInfo = ChatGPTActionInfo(
        name = "task",
        systemMessage = "You are a helpful assistant that follows instructions precisely.",
        userMessage = "",
        actionType = config.ai.getDefaultActionType(),
        model = config.ai.getDefaultActionModel(),
    )

    override fun defaultLanguageName(): String {
        val tag = config.uiLocaleLanguage.ifBlank { null }
        val locale = if (tag != null) java.util.Locale.forLanguageTag(tag)
        else java.util.Locale.getDefault()
        val name = locale.getDisplayName(java.util.Locale.ENGLISH)
        return if (name.isBlank()) "the user's language" else name
    }

    override fun speak(text: String, title: String) {
        if (text.isBlank()) return
        ttsViewModel.readArticle(text, title)
    }

    // ── Progress reporting ──────────────────────────────────────────────

    override fun info(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Info, text))

    override fun tool(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Tool, text))

    override fun error(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Error, text))

    override fun finish(markdown: String) = finishSink(markdown)

    // ── Lifecycle ───────────────────────────────────────────────────────

    private fun ensureBgWebView(): EBWebView {
        bgWebView?.let { return it }
        val wv = EBWebView(context, webViewCallback).apply {
            setOnPageFinishedAction {
                currentLoadDeferred?.complete(true)
            }
        }
        bgWebView = wv
        return wv
    }

    override fun dispose() {
        val wv = bgWebView ?: return
        try {
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (_: Exception) {
        }
        bgWebView = null
        currentLoadDeferred?.complete(false)
        currentLoadDeferred = null
    }

    companion object {
        private const val PAGE_LOAD_TIMEOUT_MS = 30_000L
    }
}
