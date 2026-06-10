package info.plateaukao.einkbro.view

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.unit.BrowserUnit

class WebViewConfigApplier(
    private val webView: EBWebView,
    private val config: ConfigManager,
) {
    private val cookieManager: CookieManager = CookieManager.getInstance()

    fun updateDarkMode() {
        if (config.display.darkMode == DarkMode.DISABLED) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val nightModeFlags =
            webView.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val wantDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES ||
            config.display.darkMode == DarkMode.FORCE_ON

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, wantDark)
            if (wantDark) webView.setBackgroundColor(Color.parseColor("#000000"))
        } else if (wantDark) {
            @Suppress("DEPRECATION")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
            @Suppress("DEPRECATION")
            webView.settings.forceDark = WebSettings.FORCE_DARK_ON
            webView.setBackgroundColor(Color.parseColor("#000000"))
        }
    }

    fun initWebSettings() {
        with(webView.settings) {
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
        }
    }

    @Suppress("DEPRECATION")
    fun initPreferences() {

        updateUserAgentString()
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null) // Enable hardware acceleration

        with(webView.settings) {
            // don't load cache by default, so that it won't cause some issues
            if (config.browser.webLoadCacheFirst)
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

            textZoom = config.display.fontSize
            allowFileAccessFromFileURLs = config.browser.enableRemoteAccess
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = config.browser.enableRemoteAccess
            domStorageEnabled = true
            databaseEnabled = true
            blockNetworkImage = !config.browser.enableImages
            javaScriptEnabled = config.browser.enableJavascript
            javaScriptCanOpenWindowsAutomatically = config.browser.enableJavascript
            setSupportMultipleWindows(config.browser.enableJavascript)
            setGeolocationEnabled(config.browser.shareLocation)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = !config.browser.enableVideoAutoplay
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webView.importantForAutofill =
                    if (config.browser.autoFillForm) View.IMPORTANT_FOR_AUTOFILL_YES else View.IMPORTANT_FOR_AUTOFILL_NO
            } else {
                saveFormData = config.browser.autoFillForm
            }
        }
        webView.webViewClient.enableAdBlock(config.browser.adBlock)
        toggleCookieSupport(config.browser.cookies)
    }

    fun updateUserAgentString() {
        val defaultUserAgentString = EBWebView.getDefaultUserAgent(webView.context)
        val prefix: String =
            defaultUserAgentString.substring(0, defaultUserAgentString.indexOf(")") + 1)

        val isDesktopMode = config.browser.desktop
        try {
            when {
                isDesktopMode ->
                    webView.settings.userAgentString =
                        defaultUserAgentString.replace(prefix, BrowserUnit.UA_DESKTOP_PREFIX)

                config.browser.enableCustomUserAgent && config.browser.customUserAgent.isNotBlank() ->
                    webView.settings.userAgentString = config.browser.customUserAgent

                else ->
                    webView.settings.userAgentString =
                        defaultUserAgentString.replace(prefix, BrowserUnit.UA_MOBILE_PREFIX)
            }
        } catch (e: Exception) {
        }

        webView.settings.useWideViewPort = isDesktopMode
        webView.settings.loadWithOverviewMode = isDesktopMode
    }

    fun toggleCookieSupport(isEnabled: Boolean) {
        with(cookieManager) {
            setAcceptCookie(isEnabled)
            setAcceptThirdPartyCookies(webView, isEnabled)
        }
    }
}
