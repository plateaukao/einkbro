package info.plateaukao.einkbro.browser

import android.app.Application
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.NinjaWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebContentPostProcessor : KoinComponent {
    private val configManager: ConfigManager by inject()
    private val application: Application by inject()

    fun postProcess(ninjaWebView: NinjaWebView, url: String) {
        if (url.startsWith("data:text/html")) return

        ViewUnit.invertColor(ninjaWebView, configManager.hasInvertedColor(url))

        for (entry in urlScriptMap) {
            val entryUrl = entry.key
            val script = entry.value
            if (url.contains(entryUrl)) {
                ninjaWebView.evaluateJsFile(script)
            }
        }

        if (url.contains("x.com")) {
            ninjaWebView.evaluateJsFile("twitter_block_ad.js")
        }

        if (!ninjaWebView.shouldUseReaderFont() && (configManager.desktop || configManager.enableZoom)) {
            val context = application.applicationContext
            val width = if (ViewUnit.getWindowWidth(context) < 800) "800" else "device-width"
            ninjaWebView.evaluateJavascript(
                zoomAndDesktopTemplateJs.format(
                    if (configManager.enableZoom) enableZoomJs else "",
                    if (configManager.desktop) "width=$width" else ""
                ),
                null
            )
        }

        if (configManager.enableVideoAutoFullscreen) {
            ninjaWebView.evaluateJsFile("video_auto_fullscreen.js")
        }

        if (ninjaWebView.shouldUseReaderFont()) {
            ninjaWebView.settings.textZoom = configManager.readerFontSize
        } else {
            ninjaWebView.settings.textZoom = configManager.fontSize
        }

        // some strange website scrolling support
        if (configManager.shouldFixScroll(url)) {
            val offset = configManager.pageReservedOffsetInString
            val offsetPercent =
                if (offset.endsWith('%')) offset.take(offset.length - 1).toInt() else 0
            val offsetPixel = if (offset.endsWith('%')) 0 else offset

            val js = HelperUnit.loadAssetFile("fix_scrolling.js").format(offsetPercent / 100.0, offsetPixel)
            ninjaWebView.evaluateJavascript(js, null);
        }

        if (configManager.shouldTranslateSite(url)) {
            ninjaWebView.showTranslation()
        }

        // text selection handling
        ninjaWebView.addSelectionChangeListener()
    }

    companion object {
        private const val SCROLL_FIX_JS = """
           javascript:(function() {
})() 
        """
        private const val zoomAndDesktopTemplateJs =
            "javascript:document.getElementsByName('viewport')[0].setAttribute('content', '%s%s');"

        private const val enableZoomJs = "initial-scale=1,maximum-scale=10.0,"
        val urlScriptMap = mapOf(
            "huxiu.com" to "huxiu.js",
            "x.com" to "twitter_block_ad.js",
            "reddit.com" to "reddit_block_ad.js",
        )
    }
}
