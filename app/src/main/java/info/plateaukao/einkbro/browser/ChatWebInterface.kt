package info.plateaukao.einkbro.browser

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.OpenAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChatWebInterface(
    val lifecycleScope: LifecycleCoroutineScope,
    webView: WebView,
    private var webContent: String,
) : KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val messageList: MutableList<ChatMessage> = mutableListOf()
    private val jsHelper = JsHelper(webView, lifecycleScope)

    @JavascriptInterface
    fun sendMessage(message: String) {
        val chatGptActionInfo = createChatGptActionInfo(message)
        lifecycleScope.launch(Dispatchers.Main) {
            sendMessageWithGptActionInfo(chatGptActionInfo)
        }
    }

    fun updateWebContent(webContent: String) {
        this.webContent = webContent
        messageList.removeIf { it.content.contains("this is the web content") }
        messageList.add("```$webContent```\n this is the web content;".toUserMessage())
    }

    fun sendMessageWithGptActionInfo(gptActionInfo: ChatGPTActionInfo) {
        if (messageList.isEmpty()) {
            if (gptActionInfo.systemMessage.isNotEmpty()) {
                messageList.add(gptActionInfo.systemMessage.toSystemMessage())
            }
            messageList.add("```$webContent```\n this is the web content;".toUserMessage())
        }

        messageList.add(gptActionInfo.userMessage.toUserMessage())

        jsHelper.startMessageStream {
            lifecycleScope.launch(Dispatchers.IO) {
                openAiRepository.chatStream(
                    messages = messageList,
                    gptActionInfo = gptActionInfo,
                    appendResponseAction = { response -> jsHelper.sendStreamUpdate(response) },
                    doneAction = { jsHelper.completeStream(messageList) },
                    failureAction = { handleFailure() }
                )
            }
        }
    }

    private fun createChatGptActionInfo(message: String): ChatGPTActionInfo {
        return ChatGPTActionInfo(
            actionType = configManager.gptForChatWeb,
            userMessage = message,
            model = configManager.getGptTypeModelMap()[configManager.gptForChatWeb] ?: configManager.gptModel,
        )
    }

    private fun handleFailure() {
        Log.e("ChatWebInterface", "Error in stream")
        jsHelper.sendStreamUpdate("Sorry, there was an error processing your request.", isComplete = true)
    }
}

/**
 * Helper class to manage JavaScript interactions with WebView
 */
class JsHelper(
    private val webView: WebView,
    private val lifecycleScope: LifecycleCoroutineScope,
) {
    private var processingMessage = ""
    fun startMessageStream(postAction: () -> Unit = {}) {
        processingMessage = ""
        webView.evaluateJavascript("javascript:startMessageStream()") {
            postAction()
        }
    }

    fun sendStreamUpdate(message: String, isComplete: Boolean = false) {
        processingMessage += message
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(message)}', true, $isComplete)",
                null
            )
        }
    }

    fun completeStream(messageList: MutableList<ChatMessage>) {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript("javascript:receiveMessageFromAndroid('', true, true)", null)
        }
        messageList.add(processingMessage.toAssistantMessage())
    }

    /**
     * Escape special characters for JavaScript string literals
     */
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}