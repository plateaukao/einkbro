package info.plateaukao.einkbro.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.ChatRole
import info.plateaukao.einkbro.service.OpenAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class ChatWebInterface(
    private val lifecycleOwner: LifecycleOwner,
    private val webView: WebView,
    private val webContent: String
): KoinComponent {
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val messageList: MutableList<ChatMessage> = mutableListOf()

    private val chatGptActionInfo = ChatGPTActionInfo(
        userMessage = "",
        actionType = GptActionType.OpenAi,
        model = configManager.gptModel,
    )

    @JavascriptInterface
    fun sendMessage(message: String) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val markdownResponse = getMarkdownResponse(message)
            val escapedResponse = escapeJsString(markdownResponse)
            withContext(Dispatchers.Main) {
                webView.evaluateJavascript(
                    "javascript:receiveMessageFromAndroid('$escapedResponse')",
                    null
                )
            }
        }
    }

    private suspend fun getMarkdownResponse(message: String): String {
        val userPrompt = if (messageList.isEmpty()) {
            "```$webContent```\n this is the web content; $message"
        } else {
            message
        }
        messageList.add(userPrompt.toUserMessage())
        val completion = openAiRepository.chatCompletion(messageList, chatGptActionInfo)
        if (completion?.choices?.firstOrNull()?.message != null) {
            messageList.add(completion.choices.firstOrNull()?.message!!)
        }

        return completion?.choices?.firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
            ?: "Something went wrong."
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