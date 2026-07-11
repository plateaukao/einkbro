package info.plateaukao.einkbro.view

import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.EinkImageMode
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.unit.HelperUnit.loadAssetFile
import java.util.Locale

class WebViewReaderHelper(
    private val webView: EBWebView,
    private val config: ConfigManager,
) {
    var isReaderModeOn = false
    var isVerticalRead = false
    var isPlainText = false
    var isEpubReaderMode = false

    private var fontNum = 0

    fun toggleVerticalRead() {
        isVerticalRead = !isVerticalRead
        if (isVerticalRead) {
            toggleReaderMode(true)
        } else {
            webView.reload()
        }
    }

    fun shouldUseReaderFont(): Boolean = isReaderModeOn || webView.isTranslatePage

    fun toggleReaderMode(isVertical: Boolean = false) {
        isReaderModeOn = !isReaderModeOn
        if (isReaderModeOn) {
            webView.jsBridge.evaluateMozReaderModeJs(isVertical) {
                webView.jsBridge.replaceWithReaderModeBody(config.display.readerKeepExtraContent, webView.url) { _ ->
                    if (isVertical) {
                        webView.jsBridge.evaluateJsFile("process_text_nodes.js", false) {
                            webView.postDelayed({ webView.jumpToTop() }, 200)
                        }
                    } else {
                        webView.jsBridge.setPaddingInReaderMode(config.display.paddingForReaderMode)
                    }
                }
            }
            webView.settings.textZoom = config.display.readerFontSize
            updateCssStyle()
        } else {
            webView.jsBridge.disableReaderMode(isVertical)
            webView.settings.textZoom = config.display.fontSize
        }
    }

    fun applyFontBoldness() = webView.jsBridge.applyFontBoldness(config.display.fontBoldness)

    fun updateCssStyle() {
        val url = webView.url.orEmpty()
        val fontType = if (shouldUseReaderFont()) config.display.readerFontType else config.getFontType(url)
        val isBlackFont = config.getBlackFontStyle(url)
        val isBoldFont = config.getBoldFontStyle(url)
        val boldness = config.getFontBoldness(url)

        val cssStyle =
            (if (isBlackFont) WebViewJsBridge.MAKE_TEXT_BLACK_CSS else "") +
                    (if (fontType == FontType.GOOGLE_SERIF) WebViewJsBridge.NOTO_SANS_SERIF_FONT_CSS else "") +
                    (if (fontType == FontType.TC_IANSUI) WebViewJsBridge.IANSUI_FONT_CSS else "") +
                    (if (fontType == FontType.JA_MINCHO) WebViewJsBridge.JA_MINCHO_FONT_CSS else "") +
                    (if (fontType == FontType.KO_GAMJA) WebViewJsBridge.KO_GAMJA_FONT_CSS else "") +
                    (if (fontType == FontType.SERIF) WebViewJsBridge.SERIF_FONT_CSS else "") +
                    (if (config.whiteBackground(url)) WebViewJsBridge.WHITE_BACKGROUND_CSS else "") +
                    (if (fontType == FontType.CUSTOM) getCustomFontCss() else "") +
                    (if (isBoldFont)
                        WebViewJsBridge.BOLD_FONT_CSS.replace("value", "$boldness") else "") +
                    (if (isEpubReaderMode) loadAssetFile("readerview.css") else "") +
                    einkImageFilterCss() +
                    config.getCustomCss(url).orEmpty()
        if (cssStyle.isNotBlank()) {
            webView.jsBridge.injectCss(cssStyle.toByteArray())
        }
    }

    /**
     * FAST e-ink image mode: adjust images with a render-time CSS filter
     * instead of re-encoding them at the network layer (see EinkImageMode).
     * Roughly matches the DEEP pipeline's tone/saturation lift.
     */
    private fun einkImageFilterCss(): String {
        if (config.display.einkImageMode != EinkImageMode.FAST) return ""
        val strength = config.display.einkImageAdjustment.strength
        if (strength <= 0) return ""
        val t = strength / 100.0
        val brightness = String.format(Locale.ROOT, "%.3f", 1.0 + 0.15 * t)
        val contrast = String.format(Locale.ROOT, "%.3f", 1.0 + 0.2 * t)
        val saturate = String.format(Locale.ROOT, "%.3f", 1.0 + 0.8 * t)
        return "img { filter: brightness($brightness) contrast($contrast) saturate($saturate) !important; }"
    }

    private fun getCustomFontCss(): String {
        return WebViewJsBridge.CUSTOM_FONT_CSS.replace("mycustomfont", "mycustomfont${++fontNum}")
            .replace("fontfamily", "fontfamily${fontNum}")
    }
}
