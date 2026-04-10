package info.plateaukao.einkbro.view

import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API

class WebViewTranslationHelper(
    private val webView: EBWebView,
    private val config: ConfigManager,
) {
    var translateApi: TRANSLATE_API = TRANSLATE_API.GOOGLE
    var isTranslateByParagraph = false

    fun clearTranslationElements() = webView.jsBridge.clearTranslationElements()

    fun translateByParagraphInPlaceReplace() {
        webView.jsBridge.translateByParagraphInPlaceReplace()
        isTranslateByParagraph = true
    }

    fun translateByParagraphInPlace() {
        webView.jsBridge.translateByParagraphInPlace(config.translationTextStyle)
        isTranslateByParagraph = true
    }

    fun addGoogleTranslation() =
        webView.jsBridge.addGoogleTranslation(config.preferredTranslateLanguageString)

    fun hideTranslateContext() = webView.jsBridge.hideTranslateContext(config.translationMode)
}
