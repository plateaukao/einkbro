package info.plateaukao.einkbro.browser

import android.util.Log
import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.TranslationCache
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
import java.util.concurrent.TimeUnit

private const val CACHE_EXPIRATION_DAYS = 5
private const val CACHE_TEXT_LENGTH_LIMIT = 15

class JsWebInterface(private val webView: EBWebView) :
    KoinComponent {
    private val translateRepository: TranslateRepository = TranslateRepository()
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()

    // to control the translation request threshold
    private val semaphoreForTranslate = Semaphore(4)

    // deepL has a limit of 5 requests per second
    private val semaphoreForDeepL = Semaphore(1)

    @OptIn(DelicateCoroutinesApi::class)
    @JavascriptInterface
    fun getTranslation(originalText: String, elementId: String, callback: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val currentLanguage = configManager.translationLanguage.value
            val currentTime = System.currentTimeMillis()

            // Check cache for short strings
            if (originalText.length < CACHE_TEXT_LENGTH_LIMIT) {
                val cachedEntry = bookmarkManager.getTranslationCache(originalText, currentLanguage)
                if (cachedEntry != null) {
                    val daysDiff = TimeUnit.MILLISECONDS.toDays(currentTime - cachedEntry.timestamp)
                    if (daysDiff < CACHE_EXPIRATION_DAYS) {
                        Log.d("JsWebInterface", "Cache hit for: $originalText")
                        withContext(Dispatchers.Main) {
                            if (webView.isAttachedToWindow) {
                                webView.evaluateJavascript(
                                    "$callback('$elementId', '${cachedEntry.translatedText}')",
                                    null
                                )
                            }
                        }
                        return@launch
                    } else {
                        // Optionally delete old cache entry, though we have a bulk delete method
                    }
                }
            }

            val semaphore = getSemaphoreForApi(webView.translateApi)
            semaphore.acquire()

            Log.d("JsWebInterface", "getTranslation: $originalText")
            val translatedString = performTranslation(originalText, webView.translateApi)

            if (translatedString.isNotEmpty() && originalText.length < CACHE_TEXT_LENGTH_LIMIT) {
                bookmarkManager.insertTranslationCache(
                    TranslationCache(
                        originalText = originalText,
                        targetLanguage = currentLanguage,
                        translatedText = translatedString,
                        timestamp = currentTime
                    )
                )
            }

            withContext(Dispatchers.Main) {
                if (webView.isAttachedToWindow && translatedString.isNotEmpty()) {
                    webView.evaluateJavascript(
                        "$callback('$elementId', '$translatedString')",
                        null
                    )
                }
            }

            delayIfNeeded(webView.translateApi)
            semaphore.release()
        }
    }

    private fun getSemaphoreForApi(api: TRANSLATE_API): Semaphore {
        return if (api == TRANSLATE_API.DEEPL || api == TRANSLATE_API.GEMINI) {
            semaphoreForDeepL
        } else {
            semaphoreForTranslate
        }
    }

    private suspend fun performTranslation(originalText: String, api: TRANSLATE_API): String {
        return when (api) {
            TRANSLATE_API.PAPAGO -> translateRepository.pTranslate(
                originalText,
                configManager.translationLanguage.value
            ).orEmpty()

            TRANSLATE_API.GOOGLE -> translateRepository.gTranslateWithApi(
                originalText,
                configManager.translationLanguage.value
            ).orEmpty()

            TRANSLATE_API.OPENAI -> translateWithOpenAi(originalText)

            TRANSLATE_API.GEMINI -> translateWithGemini(originalText)

            TRANSLATE_API.DEEPL -> translateRepository.deepLTranslate(
                originalText,
                configManager.translationLanguage
            ).orEmpty()

            else -> ""
        }
    }

    private suspend fun translateWithOpenAi(originalText: String): String {
        val chatGptActionInfo = ChatGPTActionInfo(
            userMessage = "translate following content to ${configManager.translationLanguage.value}; no other extra explanation:\n",
            actionType = GptActionType.OpenAi,
            model = configManager.gptModel,
        )
        val messages = listOf((chatGptActionInfo.userMessage + originalText).toUserMessage())
        val completion = openAiRepository.chatCompletion(messages, chatGptActionInfo)
        return completion?.choices?.firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
            ?: "Something went wrong."
    }

    private suspend fun translateWithGemini(originalText: String): String {
        val chatGptActionInfo = ChatGPTActionInfo(
            userMessage = "translate following content to ${configManager.translationLanguage.value}; no other extra explanation:\n",
            actionType = GptActionType.Gemini,
            model = configManager.geminiModel,
        )
        val messages = listOf((chatGptActionInfo.userMessage + originalText).toUserMessage())
        return openAiRepository.queryGemini(messages, chatGptActionInfo)
    }

    private suspend fun delayIfNeeded(api: TRANSLATE_API) {
        if (api == TRANSLATE_API.DEEPL || api == TRANSLATE_API.GEMINI) {
            delay(1500)
        }
    }
    @JavascriptInterface
    fun getAnchorPosition(left: Float, top: Float, right: Float, bottom: Float) {
        Log.d("touch", "rect: $left, $top, $right, $bottom")
        webView.browserController?.updateSelectionRect(left, top, right, bottom)
    }
}

fun String.toUserMessage() = ChatMessage(
    role = ChatRole.User,
    content = this
)
fun String.toSystemMessage() = ChatMessage(
    role = ChatRole.System,
    content = this
)
fun String.toAssistantMessage() = ChatMessage(
    role = ChatRole.Assistant,
    content = this
)
