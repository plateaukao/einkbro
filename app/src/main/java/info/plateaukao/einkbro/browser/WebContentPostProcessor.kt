package info.plateaukao.einkbro.browser

import android.app.Application
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebContentPostProcessor : KoinComponent {
    private val configManager: ConfigManager by inject()
    private val application: Application by inject()

    fun postProcess(ebWebView: EBWebView, url: String) {
        if (url.startsWith("data:text/html")) return

        ViewUnit.invertColor(ebWebView, configManager.hasInvertedColor(url))

        for (entry in urlScriptMap) {
            val entryUrl = entry.key
            val script = entry.value
            if (url.contains(entryUrl)) {
                ebWebView.evaluateJsFile(script)
            }
        }

        if (!ebWebView.shouldUseReaderFont() && (configManager.browser.desktop || configManager.display.enableZoom)) {
            val context = application.applicationContext
            val width = if (ViewUnit.getWindowWidth(context) < 800) "800" else "device-width"
            ebWebView.evaluateJavascript(
                zoomAndDesktopTemplateJs.format(
                    if (configManager.display.enableZoom) enableZoomJs else "",
                    if (configManager.browser.desktop) "width=$width" else ""
                ),
                null
            )
        }

        if (configManager.browser.enableVideoAutoFullscreen) {
            ebWebView.evaluateJsFile("video_auto_fullscreen.js")
        }

        // Detect if page has video elements: URL-based for known sites, DOM-based as fallback
        if (isVideoSiteUrl(url)) {
            ebWebView.hasVideo = true
        } else {
            ebWebView.evaluateJavascript(
                "(function() { return document.querySelectorAll('video, iframe[src*=\"youtube\"], iframe[src*=\"vimeo\"]').length > 0; })()"
            ) { result ->
                ebWebView.hasVideo = result == "true"
            }
        }

        if (ebWebView.isAudioOnlyMode) {
            ebWebView.evaluateJsFile("audio_only_mode.js")
        }

        if (ebWebView.shouldUseReaderFont()) {
            ebWebView.settings.textZoom = configManager.display.readerFontSize
        } else {
            ebWebView.settings.textZoom = configManager.getFontSize(url)
        }

        // inject page scroll helper for JS-based pagination (works with inner scroll containers)
        ebWebView.evaluateJsFile("fix_scrolling.js", withPrefix = false)

        // Some sites fire onPageFinished multiple times per page lifecycle — news.daum.net
        // chains a hash navigation + JS-driven reload that produces 4-5 callbacks at varying
        // progress values, and content keeps lazy-rendering after the first "done".
        // We rely on translate_by_paragraph.js being incremental (it skips already-marked
        // subtrees), so each re-fire just picks up new content. Progress == 100 filters out
        // intermediate "page finished but barely loaded" callbacks (progress=10/80) that
        // would otherwise mark almost nothing.
        if (configManager.shouldTranslateSite(url) && ebWebView.progress >= 100) {
            ebWebView.showTranslation()
        }

        // DNS prefetch for links on the page
        ebWebView.evaluateJsFile("dns_prefetch.js")

        // text selection handling
        ebWebView.addSelectionChangeListener()

        if (configManager.touch.enableDragUrlToAction) {
            ebWebView.evaluateJavascript(preventLinkDraggingJs, null)
        }

        // https://github.com/plateaukao/einkbro/issues/537
        // https://github.com/emvaized/text-reflow-on-zoom-mobile/blob/main/src/text_reflow_on_pinch_zoom.js
        if (configManager.display.enableZoomTextWrapReflow) {
            val jsZoomTextWrapReflow = HelperUnit.loadAssetFile("zoom-text-wrap-reflow.js")
            ebWebView.evaluateJavascript(jsZoomTextWrapReflow, null)
        }
    }

    companion object {
        private const val preventLinkDraggingJs = """
            document.addEventListener('dragstart', function(e) {
    if (e.target.tagName === 'A') {
        e.preventDefault();
        return false;
    }
});
        """
        private const val zoomAndDesktopTemplateJs =
            "javascript:document.getElementsByName('viewport')[0].setAttribute('content', '%s%s');"

        private const val enableZoomJs = "initial-scale=1,maximum-scale=10.0,"
        val urlScriptMap = mapOf(
            "github.com" to "github_include_fragment.js"
        )

        private val videoSitePatterns = listOf(
            "youtube.com/watch",
            "youtube.com/shorts",
            "youtu.be/",
            "vimeo.com/",
            "dailymotion.com/video",
            "twitch.tv/",
            "bilibili.com/video",
        )

        fun isVideoSiteUrl(url: String): Boolean =
            videoSitePatterns.any { url.contains(it) }
    }
}
