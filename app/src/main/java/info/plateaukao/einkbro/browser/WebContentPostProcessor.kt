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

        if (ebWebView.shouldUseReaderFont()) {
            ebWebView.settings.textZoom = configManager.readerFontSize
        } else {
            ebWebView.settings.textZoom = configManager.fontSize
        }

        // some strange website scrolling support
        if (configManager.shouldFixScroll(url)) {
            val offset = configManager.pageReservedOffsetInString
            val offsetPercent =
                if (offset.endsWith('%')) offset.take(offset.length - 1).toInt() else 0
            val offsetPixel = if (offset.endsWith('%')) 0 else offset

            val js = HelperUnit.loadAssetFile("fix_scrolling.js").format(offsetPercent / 100.0, offsetPixel)
            ebWebView.evaluateJavascript(js, null);
        }

        if (configManager.shouldTranslateSite(url)) {
            ebWebView.showTranslation()
        }

        // text selection handling
        ebWebView.addSelectionChangeListener()
    }

    companion object {
        private const val zoomAndDesktopTemplateJs =
            "javascript:document.getElementsByName('viewport')[0].setAttribute('content', '%s%s');"

        private const val enableZoomJs = "initial-scale=1,maximum-scale=10.0,"
        val urlScriptMap = mapOf<String, String>()
    }
}
