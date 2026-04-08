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

        if (!ebWebView.shouldUseReaderFont() && (configManager.desktop || configManager.enableZoom)) {
            val context = application.applicationContext
            val width = if (ViewUnit.getWindowWidth(context) < 800) "800" else "device-width"
            ebWebView.evaluateJavascript(
                zoomAndDesktopTemplateJs.format(
                    if (configManager.enableZoom) enableZoomJs else "",
                    if (configManager.desktop) "width=$width" else ""
                ),
                null
            )
        }

        if (configManager.enableVideoAutoFullscreen) {
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
            ebWebView.settings.textZoom = configManager.readerFontSize
        } else {
            ebWebView.settings.textZoom = configManager.fontSize
        }

        // inject page scroll helper for JS-based pagination (works with inner scroll containers)
        ebWebView.evaluateJsFile("fix_scrolling.js", withPrefix = false)

        // inject ebook touch mode JS (tap left/right to page up/down within WebView)
        if (configManager.isEbookModeActive) {
            ebWebView.evaluateJsFile("ebook_touch.js", withPrefix = false)
        }

        if (configManager.shouldTranslateSite(url)) {
            ebWebView.showTranslation()
        }

        // DNS prefetch for links on the page
        ebWebView.evaluateJsFile("dns_prefetch.js")

        // text selection handling
        ebWebView.addSelectionChangeListener()

        if (configManager.enableDragUrlToAction) {
            ebWebView.evaluateJavascript(preventLinkDraggingJs, null)
        }

        // https://github.com/plateaukao/einkbro/issues/537
        // https://github.com/emvaized/text-reflow-on-zoom-mobile/blob/main/src/text_reflow_on_pinch_zoom.js
        if (configManager.enableZoomTextWrapReflow) {
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
