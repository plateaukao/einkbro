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
    lifecycleScope: LifecycleCoroutineScope,
    webView: WebView,
    private val webContent: String,
) : KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val messageList: MutableList<ChatMessage> = mutableListOf()
    private val jsHelper = JsHelper(webView, lifecycleScope)

    @JavascriptInterface
    fun sendMessage(message: String) {
        jsHelper.startMessageStream()

        val userPrompt = createUserPrompt(message)
        messageList.add(userPrompt.toUserMessage())

        val chatGptActionInfo = createChatGptActionInfo()

        openAiRepository.chatStream(
            messages = messageList,
            gptActionInfo = chatGptActionInfo,
            appendResponseAction = { response -> jsHelper.sendStreamUpdate(response) },
            doneAction = { jsHelper.completeStream() },
            failureAction = { handleFailure() }
        )
    }

    private fun createUserPrompt(message: String): String {
        return if (messageList.isEmpty()) {
            "```$webContent```\n this is the web content; $message"
        } else {
            message
        }
    }

    private fun createChatGptActionInfo(): ChatGPTActionInfo {
        return ChatGPTActionInfo(
            actionType = configManager.gptForChatWeb,
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
    private val lifecycleScope: LifecycleCoroutineScope
) {
    fun startMessageStream() {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript("javascript:startMessageStream()", null)
        }
    }

    fun sendStreamUpdate(message: String, isComplete: Boolean = false) {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                "javascript:receiveMessageFromAndroid('${escapeJsString(message)}', true, $isComplete)",
                null
            )
        }
    }

    fun completeStream() {
        lifecycleScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript("javascript:receiveMessageFromAndroid('', true, true)", null)
        }
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