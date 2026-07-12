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
            webView.jsBridge.disableReaderMode()
            webView.settings.textZoom = config.display.fontSize
            // Recompute the main style slot with the normal-mode font so the
            // reader font doesn't stick after leaving reader mode.
            updateCssStyle()
        }
    }

    fun updateCssStyle() {
        val url = webView.url.orEmpty()
        val fontType = if (shouldUseReaderFont()) config.display.readerFontType else config.getFontType(url)
        val isBlackFont = config.getBlackFontStyle(url)
        val isBoldFont = config.getBoldFontStyle(url)
        val boldness = config.getFontBoldness(url)

        // The font CSS must come first: its @import rules are only valid before
        // any other rule, so placing e.g. the black-font CSS ahead of it would
        // make the browser silently drop the web font imports.
        val fontCss = when (fontType) {
            FontType.SYSTEM_DEFAULT -> ""
            FontType.SERIF -> WebViewJsBridge.SERIF_FONT_CSS
            FontType.GOOGLE_SERIF -> WebViewJsBridge.NOTO_SANS_SERIF_FONT_CSS
            FontType.CUSTOM -> getCustomFontCss()
            FontType.TC_IANSUI -> WebViewJsBridge.IANSUI_FONT_CSS
            FontType.JA_MINCHO -> WebViewJsBridge.JA_MINCHO_FONT_CSS
            FontType.KO_GAMJA -> WebViewJsBridge.KO_GAMJA_FONT_CSS
        }

        val cssStyle = fontCss +
                (if (isBlackFont) WebViewJsBridge.MAKE_TEXT_BLACK_CSS else "") +
                (if (config.whiteBackground(url)) WebViewJsBridge.WHITE_BACKGROUND_CSS else "") +
                (if (isBoldFont)
                    WebViewJsBridge.BOLD_FONT_CSS.replace("value", "$boldness") else "") +
                (if (isEpubReaderMode) loadAssetFile("readerview.css") else "") +
                einkImageFilterCss() +
                config.getCustomCss(url).orEmpty()
        // Always update, even when blank: an empty blob clears the slot, which
        // is how reverting to system default font (or turning styles off)
        // takes effect without reloading the page.
        webView.jsBridge.updateCssSlot(WebViewJsBridge.CSS_SLOT_MAIN, cssStyle)
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
        val info = if (shouldUseReaderFont()) {
            config.display.readerCustomFontInfo
        } else {
            config.display.customFontInfo
        }
        val fontUrl = info?.url ?: return ""
        // Version the synthetic font URL by the configured font so switching to
        // a different font file forces a refetch, while repeated style updates
        // with the same font keep hitting the already-loaded face.
        val version = fontUrl.hashCode().toUInt().toString(16)
        return WebViewJsBridge.CUSTOM_FONT_CSS
            .replace("mycustomfont", "mycustomfont$version")
            .replace("fontfamily", "fontfamily$version")
    }
}
