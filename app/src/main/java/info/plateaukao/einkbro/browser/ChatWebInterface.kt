package info.plateaukao.einkbro.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ChatWebInterface(
    val lifecycleScope: LifecycleCoroutineScope,
    webView: WebView, // Made private as JsHelper will use it
    private var webContent: String,
    private var webTitle: String,
    private var webUrl: String,
) : KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val chatHistory: MutableList<ChatMessage> = mutableListOf()
    private val jsHelper = JsHelper(webView, lifecycleScope)

    companion object {
        private const val WEB_CONTENT_MESSAGE_SUFFIX = "\n this is the web content;"
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
        // Return web metadata as JSON string for JavaScript to use
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

    fun updateWebContent(newWebContent: String, newWebTitle: String = webTitle, newWebUrl: String = webUrl) {
        this.webContent = newWebContent
        this.webTitle = newWebTitle
        this.webUrl = newWebUrl
        // No direct manipulation of chatHistory needed here for web content updates.
        // The latest webContent will be used when constructing messages for the API.
    }

    fun sendMessageWithGptActionInfo(gptActionInfo: ChatGPTActionInfo) {
        val currentUserMessage = gptActionInfo.userMessage.toUserMessage()

        val messagesForApi = mutableListOf<ChatMessage>().apply {
            if (gptActionInfo.systemMessage.isNotEmpty()) {
                add(gptActionInfo.systemMessage.toSystemMessage())
            }
            add(createWebContentMessage(webContent))
            addAll(chatHistory) // Add previous conversation turns
            add(currentUserMessage) // Add the current user message
        }

        val assistantResponseAggregator = StringBuilder()

        jsHelper.startMessageStream { // Prepares JS for a new message stream
            lifecycleScope.launch(Dispatchers.IO) {
                openAiRepository.chatStream(
                    messages = messagesForApi,
                    gptActionInfo = gptActionInfo,
                    appendResponseAction = { responseChunk ->
                        jsHelper.sendStreamUpdate(responseChunk) // Send chunk to WebView
                        assistantResponseAggregator.append(responseChunk) // Aggregate for history
                    },
                    doneAction = { // Called when OpenAI stream is successfully finished
                        jsHelper.sendFinalEmptyUpdate() // Signal JS that stream is done
                        // Add the user's message and the full assistant response to history
                        chatHistory.add(currentUserMessage)
                        chatHistory.add(assistantResponseAggregator.toString().toAssistantMessage())
                    },
                    failureAction = { // Called on stream failure
                        Timber.e("Error in OpenAI stream")
                        jsHelper.sendErrorUpdate("Sorry, there was an error processing your request.")
                        // Optionally, add an error marker to chatHistory or handle as needed
                    }
                )
            }
        }
    }

    private fun createWebContentMessage(content: String): ChatMessage =
        "```$content```$WEB_CONTENT_MESSAGE_SUFFIX".toUserMessage()

    private fun createChatGptActionInfo(message: String): ChatGPTActionInfo =
        ChatGPTActionInfo(
            actionType = configManager.gptForChatWeb,
            userMessage = message,
            model = configManager.getGptTypeModelMap()[configManager.gptForChatWeb] ?: configManager.gptModel,
        )
}

/**
 * Helper class to manage JavaScript interactions with WebView.
 * This class is now stateless regarding message content, only forwarding to JS.
 */
class JsHelper(
    private val webView: WebView,
    private val lifecycleScope: LifecycleCoroutineScope,
) {
    fun startMessageStream(postAction: () -> Unit = {}) {
        // Tell JS to prepare for a new incoming message stream
        webView.evaluateJavascript("javascript:startMessageStream()") {
            postAction()
        }
    }

    fun sendStreamUpdate(messageChunk: String) {
        // Send a chunk of the message to JS; not the end of the stream yet.
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(messageChunk)}', true, false)",
                null
            )
        }
    }

    fun sendFinalEmptyUpdate() {
        // Signal JS that the message stream has completed successfully.
        // Sends an empty message with the 'done' flag.
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript("javascript:receiveMessageFromAndroid('', true, true)", null)
        }
    }

    fun sendErrorUpdate(errorMessage: String) {
        // Signal JS that an error occurred and the stream is ending.
        // Sends the error message with the 'done' flag.
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(errorMessage)}', true, true)",
                null
            )
        }
    }

    fun openUrlInNewTab(url: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            // Use WebView to open URL in the same browser (new tab functionality would be handled by BrowserController)
            (webView as? EBWebView)?.browserController?.addNewTab(url)
        }
    }

    /**
     * Escape special characters for JavaScript string literals
     */
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\") // Escape backslashes first
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            // Add other escapes as necessary, e.g., for quotes if using double quotes in JS
            .replace("\"", "\\\"")
    }
}
