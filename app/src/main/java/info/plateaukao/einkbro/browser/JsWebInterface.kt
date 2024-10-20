package info.plateaukao.einkbro.browser

import android.util.Log
import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.preference.ConfigManager
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
            if (translateApi == TRANSLATE_API.DEEPL) {
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
            if (translateApi == TRANSLATE_API.DEEPL) {
                delay(300)
                semaphoreForDeepL.release()
            } else {
                semaphoreForTranslate.release()
            }
        }
    }
}
