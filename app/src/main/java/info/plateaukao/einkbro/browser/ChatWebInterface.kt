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
import kotlin.getValue

class ChatWebInterface(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val webView: WebView,
    private val webContent: String,
) : KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val messageList: MutableList<ChatMessage> = mutableListOf()

    @JavascriptInterface
    fun sendMessage(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            //Log.d("ChatWebInterface", "Message: $message")
            webView.evaluateJavascript("javascript:startMessageStream()", null)
        }
        generateMarkdownResponse(message)
    }

    private fun generateMarkdownResponse(message: String) {
        val userPrompt = if (messageList.isEmpty()) {
            "```$webContent```\n this is the web content; $message"
        } else {
            message
        }
        messageList.add(userPrompt.toUserMessage())

        val chatGptActionInfo = ChatGPTActionInfo(
            actionType = configManager.gptForChatWeb,
            model = configManager.getGptTypeModelMap()[configManager.gptForChatWeb] ?: configManager.gptModel,
        )

        openAiRepository.chatStream(messageList, chatGptActionInfo,
            { response ->
                lifecycleScope.launch(Dispatchers.Main) {
                    //Log.d("ChatWebInterface", "Response: $response")
                    webView.evaluateJavascript(
                        "javascript:receiveMessageFromAndroid('${escapeJsString(response)}', true, false)",
                        null
                    )
                }
            },
            doneAction = {
                lifecycleScope.launch(Dispatchers.Main) {
                    //Log.d("ChatWebInterface", "Done")
                    webView.evaluateJavascript("javascript:receiveMessageFromAndroid('', true, true)", null)
                }
            },
            failureAction = {
                Log.e("ChatWebInterface", "Error in stream")
            }
        )

//        if (chatGptActionInfo.actionType != GptActionType.Gemini) {
//            val completion = openAiRepository.chatCompletion(messageList, chatGptActionInfo)
//            if (completion?.choices?.firstOrNull()?.message != null) {
//                messageList.add(completion.choices.firstOrNull()?.message!!)
//            }
//
//            return completion?.choices?.firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
//                ?: "Something went wrong."
//        } else {
//            val result = openAiRepository.queryGemini(messageList, chatGptActionInfo)
//            return result
//        }
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