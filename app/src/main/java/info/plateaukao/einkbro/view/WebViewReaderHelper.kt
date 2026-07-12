package info.plateaukao.einkbro.view

import android.content.res.Configuration
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

    // True when enabling vertical read was also what turned reader mode on, so
    // disabling vertical read knows to leave reader mode entirely rather than
    // fall back to the horizontal reader view the user hadn't asked for.
    private var verticalActivatedReaderMode = false

    fun toggleVerticalRead() {
        isVerticalRead = !isVerticalRead
        if (isVerticalRead) {
            if (isReaderModeOn) {
                // Reader content is already showing: swap in the vertical styling
                // without re-running Readability on the already-replaced body.
                verticalActivatedReaderMode = false
                webView.jsBridge.updateCssSlot(
                    WebViewJsBridge.CSS_SLOT_READER,
                    loadAssetFile("verticalReaderview.css")
                )
                webView.jsBridge.updateCssSlot(
                    WebViewJsBridge.CSS_SLOT_VERTICAL,
                    loadAssetFile("vertical_layout.css")
                )
                // applyVerticalTextProcessing() re-applies the reader-settings
                // slot (vertical variant, no two-column) and resets the viewport.
                applyVerticalTextProcessing()
            } else {
                verticalActivatedReaderMode = true
                toggleReaderMode(true)
            }
        } else {
            // process_text_nodes.js irreversibly rewrites the reader DOM (dates to
            // kanji, tate-chu-yoko spans, full-width punctuation), so we can't just
            // swap the CSS back; the body must be rebuilt. A plain webView.reload()
            // does NOT work here — once reader mode replaced the body via
            // document.body.outerHTML, WebView.reload() no longer re-fetches the
            // document, leaving vertical mode stuck. Instead tear reader mode down
            // (disableReaderMode restores the pre-reader body from its cache, no
            // network) and, if the user had been reading horizontally before,
            // re-enter the horizontal reader on that clean body.
            val restoreHorizontalReader = !verticalActivatedReaderMode
            toggleReaderMode()
            if (restoreHorizontalReader) {
                toggleReaderMode(false)
            }
        }
    }

    fun shouldUseReaderFont(): Boolean = isReaderModeOn || webView.isTranslatePage

    fun toggleReaderMode(isVertical: Boolean = false) {
        isReaderModeOn = !isReaderModeOn
        if (isReaderModeOn) {
            // Keep the vertical flag in lockstep with the mode we're entering, so
            // callers that re-enter reader mode directly (e.g. the reader-settings
            // dialog re-parsing the page) preserve vertical read.
            isVerticalRead = isVertical
            webView.jsBridge.evaluateMozReaderModeJs(isVertical) {
                webView.jsBridge.replaceWithReaderModeBody(config.display.readerKeepExtraContent, webView.url) { _ ->
                    if (isVertical) {
                        applyVerticalTextProcessing()
                    } else {
                        updateReaderSettingsStyle()
                    }
                }
            }
            webView.settings.textZoom = config.display.readerFontSize
            updateCssStyle()
        } else {
            // Vertical read only exists inside reader mode; leaving reader mode
            // (e.g. via the reader toolbar icon while vertical is on) exits it too.
            isVerticalRead = false
            webView.jsBridge.disableReaderMode()
            webView.settings.textZoom = config.display.fontSize
            // Recompute the main style slot with the normal-mode font so the
            // reader font doesn't stick after leaving reader mode.
            updateCssStyle()
        }
    }

    /**
     * Post-processes the reader body for vertical reading (dates, tate-chu-yoko
     * spans, list markers), applies the user's reader layout (margin, line
     * spacing), measures the rendered line advance so page turns can snap to
     * line boundaries, then jumps to the reading start (right edge).
     */
    private fun applyVerticalTextProcessing() {
        updateReaderSettingsStyle()
        webView.jsBridge.evaluateJsFile("process_text_nodes.js", false) {
            measureVerticalLineAdvance {
                webView.postDelayed({ webView.jumpToTop() }, 200)
            }
        }
    }

    /**
     * Measures the rendered vertical line advance and stores it (in physical px)
     * on the WebView so page turns can snap to it. Re-run whenever anything that
     * changes line geometry is applied (entering vertical mode, line-spacing or
     * font changes).
     */
    private fun measureVerticalLineAdvance(then: () -> Unit = {}) {
        webView.jsBridge.evaluateJsFile("measure_line_advance.js", false) { value ->
            // JS reports CSS px; native scrolling is in physical px.
            @Suppress("DEPRECATION")
            webView.verticalLineAdvancePx =
                (value?.trim('"')?.toFloatOrNull() ?: 0f) * webView.scale
            then()
        }
    }

    /**
     * Two-column landscape reading is active: the reader body flows into
     * viewport-height columns extending horizontally, so page turns scroll
     * sideways by one viewport width (see WebViewNavigationHelper).
     */
    fun isTwoColumnActive(): Boolean = isReaderModeOn && !isVerticalRead &&
            config.display.readerTwoColumnInLandscape &&
            webView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * User-tunable reader layout (page margin, line spacing, two-column) in its
     * own CSS slot, so the dialog can re-apply it live without touching the
     * base readerview.css slot. Selectors are more specific than readerview.css
     * so they win regardless of slot insertion order.
     *
     * Works in vertical mode too: `padding` is physical and `line-height` sets
     * the breadth of each vertical line (i.e. inter-line spacing) under
     * writing-mode: vertical-rl. The two-column landscape layout is horizontal
     * only — it can't coexist with vertical-rl — so it's skipped there.
     */
    fun updateReaderSettingsStyle() {
        if (!isReaderModeOn) return

        val padding = config.display.paddingForReaderMode
        val lineHeight = String.format(Locale.ROOT, "%.1f", config.display.readerLineSpacing / 10.0)
        val twoColumn = !isVerticalRead && config.display.readerTwoColumnInLandscape
        val css = StringBuilder()
        css.append("body.mozac-readerview-body { padding: ${padding}px !important; }\n")
        css.append(
            ".mozac-readerview-body .mozac-readerview-content p, " +
                    ".mozac-readerview-body .mozac-readerview-content li " +
                    "{ line-height: $lineHeight !important; }\n"
        )
        if (twoColumn) {
            // margin 0 (killing the 8px UA default) + column-gap = 2 * padding
            // make each two-column "page" exactly one viewport wide, so page
            // turns can jump by webView.width without drifting.
            css.append(
                """
                @media screen and (orientation: landscape) {
                  body.mozac-readerview-body {
                    margin: 0 !important;
                    height: 100vh;
                    box-sizing: border-box;
                    overflow-x: auto;
                    overflow-y: hidden;
                    column-count: 2;
                    column-gap: ${padding * 2}px;
                    column-fill: auto;
                  }
                }
                """.trimIndent()
            )
        }
        webView.jsBridge.updateCssSlot(WebViewJsBridge.CSS_SLOT_READER_SETTINGS, css.toString())
        webView.jsBridge.setViewportContent(
            if (twoColumn) WebViewJsBridge.VIEWPORT_FIXED_SCALE
            else WebViewJsBridge.VIEWPORT_DEFAULT
        )
        // Line spacing changes the vertical line advance, so page-turn snapping
        // must be re-measured against the new layout.
        if (isVerticalRead) measureVerticalLineAdvance()
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
