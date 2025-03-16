package info.plateaukao.einkbro.browser

import android.util.Log
import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.ChatRole
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TranslateRepository
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.Semaphore

class JsWebInterface(private val webView: EBWebView) :
    KoinComponent {
    private val translateRepository: TranslateRepository = TranslateRepository()
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()

    // to control the translation request threshold
    private val semaphoreForTranslate = Semaphore(4)

    // deepL has a limit of 5 requests per second
    private val semaphoreForDeepL = Semaphore(1)

    @JavascriptInterface
    fun getAnchorPosition(left: Float, top: Float, right: Float, bottom: Float) {
        Log.d("touch", "rect: $left, $top, $right, $bottom")
        webView.browserController?.updateSelectionRect(left, top, right, bottom)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @JavascriptInterface
    fun getTranslation(originalText: String, elementId: String, callback: String) {
        val translateApi = webView.translateApi

        GlobalScope.launch(Dispatchers.IO) {
            if (translateApi == TRANSLATE_API.DEEPL || translateApi == TRANSLATE_API.GEMINI) {
                semaphoreForDeepL.acquire()
            } else {
                semaphoreForTranslate.acquire()
            }

            Log.d("JsWebInterface", "getTranslation: $originalText")
            val translatedString = if (translateApi == TRANSLATE_API.PAPAGO) {
                translateRepository.pTranslate(
                    originalText,
                    configManager.translationLanguage.value,
                ).orEmpty()
            } else if (translateApi == TRANSLATE_API.GOOGLE) {
                translateRepository.gTranslateWithApi(
                    originalText,
                    configManager.translationLanguage.value
                ).orEmpty()
            } else if (translateApi == TRANSLATE_API.OPENAI) {
                val chatGptActionInfo = ChatGPTActionInfo(
                    userMessage = "translate following content to ${configManager.translationLanguage.value}; no other extra explanation:\n",
                    actionType = GptActionType.OpenAi,
                    model = "gpt-4o-mini",
                )
                val messages: List<ChatMessage> = listOf(
                    (chatGptActionInfo.userMessage + originalText).toUserMessage()
                )
                val completion = openAiRepository.chatCompletion(messages, chatGptActionInfo)
                if (completion?.choices?.isNotEmpty() == true) {
                    val responseContent = completion.choices
                        .firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
                        ?: "Something went wrong."
                    responseContent
                } else {
                    ""
                }
            } else if (translateApi == TRANSLATE_API.GEMINI) {
                val chatGptActionInfo = ChatGPTActionInfo(
                    userMessage = "translate following content to ${configManager.translationLanguage.value}; no other extra explanation:\n",
                    actionType = GptActionType.Gemini,
                    model = "gemini-2.0-flash",
                )
                val messages: List<ChatMessage> = listOf(
                    (chatGptActionInfo.userMessage + originalText).toUserMessage()
                )
                openAiRepository.queryGemini(messages, chatGptActionInfo)
            } else if (translateApi == TRANSLATE_API.DEEPL) {
                translateRepository.deepLTranslate(
                    originalText,
                    configManager.translationLanguage
                ).orEmpty()
            } else {
                ""
            }

            withContext(Dispatchers.Main) {
                if (webView.isAttachedToWindow && translatedString.isNotEmpty()) {
                    webView.evaluateJavascript(
                        "$callback('$elementId', '$translatedString')",
                        null
                    )
                }
            }
            if (translateApi == TRANSLATE_API.DEEPL || translateApi == TRANSLATE_API.GEMINI) {
                delay(1500)
                semaphoreForDeepL.release()
            } else {
                semaphoreForTranslate.release()
            }
        }
    }
}

fun String.toUserMessage() = ChatMessage(
    role = ChatRole.User,
    content = this
)
