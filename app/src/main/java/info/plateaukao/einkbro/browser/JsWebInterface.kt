package info.plateaukao.einkbro.browser

import android.util.Log
import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.TRANSLATION_CACHE_EXPIRATION_DAYS
import info.plateaukao.einkbro.database.TranslationCache
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.data.remote.ChatMessage
import info.plateaukao.einkbro.data.remote.ChatRole
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.data.remote.TranslateRepository
import info.plateaukao.einkbro.service.WebSpeechHandler
import info.plateaukao.einkbro.unit.DownloadHelper
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// Upper bound on cacheable source text; the translation APIs themselves cap
// around 5000 chars, so anything larger never produces a reusable result.
private const val CACHE_MAX_TEXT_LENGTH = 10_000
private const val MAX_DOWNLOAD_ID_LENGTH = 64
private const val MAX_TTS_TEXT_LENGTH = 4000

// A bare JavaScript identifier: the only shape the page-supplied translation
// callback name is allowed to take before it is interpolated into an
// evaluateJavascript call (the injected monitor always passes "myCallback").
private val JS_IDENTIFIER_REGEX = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")

class JsWebInterface(
    private val webView: EBWebView,
    private val jsBrowserCallback: JsBrowserCallback? = null,
) : KoinComponent {
    private val translateRepository: TranslateRepository = TranslateRepository()
    private val openAiRepository: OpenAiRepository = OpenAiRepository()
    private val configManager: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val coroutineScope: CoroutineScope by inject()
    private val webSpeechHandler: WebSpeechHandler by inject()

    private fun escapeForJs(text: String): String =
        text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

    // Concurrency gates for translation requests. These must be kotlinx suspending
    // semaphores: a page can fire 60+ getTranslation calls in one IntersectionObserver
    // batch, and a thread-blocking semaphore would park the entire Dispatchers.IO pool,
    // deadlocking permit release (and all other IO work) until the app is killed.
    private val semaphoreForTranslate = Semaphore(4)

    // deepL has a limit of 5 requests per second
    private val semaphoreForDeepL = Semaphore(1)

    // Per-session capability token for getTranslation. `getTranslation` is the only
    // page-reachable bridge method that spends the user's translation/LLM key, and a
    // WebView JavaScript interface cannot be scoped to a single origin, so it is present
    // on every page. Without a gate, any loaded site could call
    // androidApp.getTranslation() in a loop to drain the user's OpenAI/Gemini quota or
    // use the key as a free inference proxy (the model output returns via the callback).
    //
    // The token is minted natively in [beginTranslationSession] when the user starts an
    // in-place translation, substituted into the app's own injected text_node_monitor.js,
    // and cleared on navigation or when translation is cleared. A non-null token means a
    // user-initiated translation session is active; the value binds calls to the app's
    // injected script. There is no JS path to set it, so a page cannot forge a session.
    // (Residual: because a JS interface shares the page's JS context, a page that hooks
    // androidApp.getTranslation before the user triggers translation could observe the
    // token and piggyback during the active session — the null check still fully blocks
    // the unconditional, no-interaction abuse.)
    @Volatile
    private var translationToken: String? = null

    fun beginTranslationSession(): String {
        val token = generateTranslationToken()
        translationToken = token
        return token
    }

    fun endTranslationSession() {
        translationToken = null
    }

    private fun generateTranslationToken(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @JavascriptInterface
    fun getTranslation(token: String, originalText: String, elementId: String, callback: String) {
        val activeToken = translationToken
        if (activeToken == null || token != activeToken) {
            Log.w("JsWebInterface", "getTranslation rejected: no active translation session")
            return
        }
        // Reject a callback that isn't a bare identifier; elementId is escaped where it is
        // interpolated below. Together these close the raw-interpolation injection hole.
        if (!callback.matches(JS_IDENTIFIER_REGEX)) {
            Log.w("JsWebInterface", "getTranslation rejected: invalid callback name")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            val currentLanguage = configManager.translation.translationLanguage.value
            val translateApi = webView.translateApi
            val currentTime = System.currentTimeMillis()
            val cacheable = originalText.length <= CACHE_MAX_TEXT_LENGTH
            val textHash = if (cacheable) sha256(originalText) else ""

            if (cacheable) {
                val cachedEntry =
                    bookmarkManager.getTranslationCache(textHash, currentLanguage, translateApi.name)
                if (cachedEntry != null) {
                    val daysDiff = TimeUnit.MILLISECONDS.toDays(currentTime - cachedEntry.timestamp)
                    if (daysDiff < TRANSLATION_CACHE_EXPIRATION_DAYS) {
                        Log.d("JsWebInterface", "Cache hit for: $originalText")
                        // Sliding expiration: bump the timestamp (at most once a day)
                        // so regularly re-read documents don't expire mid-habit.
                        if (daysDiff >= 1) {
                            bookmarkManager.refreshTranslationCacheTimestamp(
                                textHash, currentLanguage, translateApi.name, currentTime
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if (webView.isAttachedToWindow) {
                                webView.evaluateJavascript(
                                    "$callback('${escapeForJs(elementId)}', '${escapeForJs(originalText)}', '${escapeForJs(cachedEntry.translatedText)}')",
                                    null
                                )
                            }
                        }
                        return@launch
                    }
                }
            }

            val semaphore = getSemaphoreForApi(translateApi)
            semaphore.withPermit {
                Log.d("JsWebInterface", "getTranslation: $originalText")
                val translatedString = performTranslation(originalText, translateApi)

                if (translatedString.isNotEmpty() && cacheable) {
                    bookmarkManager.insertTranslationCache(
                        TranslationCache(
                            textHash = textHash,
                            targetLanguage = currentLanguage,
                            translateApi = translateApi.name,
                            translatedText = translatedString,
                            timestamp = currentTime
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    // Invoke the callback even when translation failed (empty string):
                    // the JS side uses it to clear the element's in-flight flag so a
                    // later visibility event can retry instead of blocking it forever.
                    if (webView.isAttachedToWindow) {
                        webView.evaluateJavascript(
                            "$callback('${escapeForJs(elementId)}', '${escapeForJs(originalText)}', '${escapeForJs(translatedString)}')",
                            null
                        )
                    }
                }

                delayIfNeeded(translateApi)
            }
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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
                configManager.translation.translationLanguage.value
            ).orEmpty()

            TRANSLATE_API.GOOGLE -> translateRepository.gTranslateWithApi(
                originalText,
                configManager.translation.translationLanguage.value
            ).orEmpty()

            TRANSLATE_API.OPENAI -> translateWithOpenAi(originalText)

            TRANSLATE_API.GEMINI -> translateWithGemini(originalText)

            TRANSLATE_API.DEEPL -> translateRepository.deepLTranslate(
                originalText,
                configManager.translation.translationLanguage
            ).orEmpty()

            else -> ""
        }
    }

    private suspend fun translateWithOpenAi(originalText: String): String {
        val chatGptActionInfo = ChatGPTActionInfo(
            userMessage = "translate following content to ${configManager.translation.translationLanguage.value}; no other extra explanation:\n",
            actionType = GptActionType.OpenAi,
            model = configManager.ai.gptModel,
        )
        val messages = listOf((chatGptActionInfo.userMessage + originalText).toUserMessage())
        val completion = openAiRepository.chatCompletion(messages, chatGptActionInfo)
        // Return empty on failure (matches Gemini path). The caller treats empty as
        // "no translation" — leaves the placeholder blank and skips caching, so a
        // transient API error doesn't get persisted as the canonical translation
        // for this string.
        return completion?.choices?.firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
            .orEmpty()
    }

    private suspend fun translateWithGemini(originalText: String): String {
        val chatGptActionInfo = ChatGPTActionInfo(
            userMessage = "translate following content to ${configManager.translation.translationLanguage.value}; no other extra explanation:\n",
            actionType = GptActionType.Gemini,
            model = configManager.ai.geminiModel,
        )
        val messages = listOf((chatGptActionInfo.userMessage + originalText).toUserMessage())
        return openAiRepository.queryGemini(messages, chatGptActionInfo).valueOrNull().orEmpty()
    }

    private suspend fun delayIfNeeded(api: TRANSLATE_API) {
        if (api == TRANSLATE_API.DEEPL || api == TRANSLATE_API.GEMINI) {
            delay(1500)
        }
    }
    @JavascriptInterface
    fun getAnchorPosition(left: Float, top: Float, right: Float, bottom: Float) {
        Log.d("touch", "rect: $left, $top, $right, $bottom")
        jsBrowserCallback?.updateSelectionRect(left, top, right, bottom)
    }

    @JavascriptInterface
    fun dismissActionMode(): Boolean {
        val callback = jsBrowserCallback ?: return false
        return if (callback.isActionModeActive()) {
            webView.post { callback.dismissActionMode() }
            true
        } else false
    }

    @JavascriptInterface
    fun ebookPageUp() {
        webView.post {
            if (!configManager.touch.switchTouchAreaAction) webView.pageUpWithNoAnimation()
            else webView.pageDownWithNoAnimation()
        }
    }

    @JavascriptInterface
    fun ebookPageDown() {
        webView.post {
            if (!configManager.touch.switchTouchAreaAction) webView.pageDownWithNoAnimation()
            else webView.pageUpWithNoAnimation()
        }
    }

    @JavascriptInterface
    fun onInnerScrollChanged(isAtTop: Boolean, scrollTop: Int, scrollHeight: Int, clientHeight: Int) {
        webView.isInnerScrollAtTop = isAtTop
        webView.innerScrollTop = scrollTop
        webView.innerScrollHeight = scrollHeight
        webView.innerClientHeight = clientHeight
        webView.post { webView.updatePageInfo() }
    }

    @JavascriptInterface
    fun setTouchOnInnerScrollable(value: Boolean) {
        webView.isTouchOnInnerScrollable = value
    }

    @JavascriptInterface
    fun onBlobDownloadChunk(downloadId: String, base64Chunk: String) {
        if (downloadId.length > MAX_DOWNLOAD_ID_LENGTH) {
            Log.w("JsWebInterface", "Ignoring blob download chunk with invalid id")
            return
        }
        DownloadHelper.appendBlobDownloadChunk(downloadId, base64Chunk)
    }

    @JavascriptInterface
    fun onBlobDownloadComplete(downloadId: String, mimeType: String) {
        if (downloadId.length > MAX_DOWNLOAD_ID_LENGTH) {
            Log.w("JsWebInterface", "Ignoring blob download completion with invalid id")
            return
        }
        DownloadHelper.completeBlobDownload(downloadId, mimeType)
    }

    @JavascriptInterface
    fun onBlobDownloadError(downloadId: String, message: String?) {
        if (downloadId.length > MAX_DOWNLOAD_ID_LENGTH) {
            Log.w("JsWebInterface", "Ignoring blob download error with invalid id")
            return
        }
        DownloadHelper.failBlobDownload(downloadId, message)
    }

    @JavascriptInterface
    fun beginBlobDownload(fileName: String?, mimeType: String?): String {
        val activity = webView.context as? android.app.Activity ?: return ""
        val safeFileName = fileName?.takeIf { it.isNotBlank() } ?: "download"
        return DownloadHelper.beginBlobDownload(activity, safeFileName, mimeType.orEmpty())
    }

    // Web Speech API bridge for assets/speech_synthesis_polyfill.js: WebView has
    // no native speechSynthesis, so the polyfill forwards page utterances here.
    @JavascriptInterface
    fun ttsSpeak(
        text: String,
        lang: String,
        rate: Float,
        pitch: Float,
        voiceName: String,
        utteranceId: String,
    ) {
        if (!utteranceId.matches(WebSpeechHandler.UTTERANCE_ID_REGEX)) return
        if (text.isBlank() || text.length > MAX_TTS_TEXT_LENGTH) return
        webSpeechHandler.speak(webView, text, lang, rate, pitch, voiceName, utteranceId)
    }

    @JavascriptInterface
    fun ttsCancel() = webSpeechHandler.cancel()

    @JavascriptInterface
    fun ttsPause() = webSpeechHandler.pause()

    @JavascriptInterface
    fun ttsResume() = webSpeechHandler.resume()

    @JavascriptInterface
    fun ttsGetVoices(): String = webSpeechHandler.getVoicesJson()
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
