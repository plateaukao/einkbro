package info.plateaukao.einkbro.view

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.webkit.ScriptHandler
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit

class WebViewConfigApplier(
    private val webView: EBWebView,
    private val config: ConfigManager,
) {
    private val cookieManager: CookieManager = CookieManager.getInstance()
    private var autoplayBlockerHandler: ScriptHandler? = null
    private var defaultUserAgentMetadata: UserAgentMetadata? = null
    private var uaMetadataOverridden = false

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
        applyAutoplayBlocker()
    }

    // mediaPlaybackRequiresUserGesture alone can't stop feed sites: muted
    // playback is exempt from the gesture requirement, and IG/FB/X/Threads
    // autoplay muted via JS play(). The blocker script must run before any
    // page script and in every frame (embedded players), which only
    // addDocumentStartJavaScript guarantees; older WebViews fall back to
    // onPageStarted injection in EBWebViewClient.
    private fun applyAutoplayBlocker() {
        if (!supportsDocumentStartScript()) return
        if (!config.browser.enableVideoAutoplay) {
            if (autoplayBlockerHandler == null) {
                autoplayBlockerHandler = WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    HelperUnit.loadAssetFile("disable_video_autoplay.js"),
                    setOf("*"),
                )
            }
        } else {
            autoplayBlockerHandler?.remove()
            autoplayBlockerHandler = null
        }
    }

    companion object {
        fun supportsDocumentStartScript(): Boolean =
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    }

    fun updateUserAgentString() {
        val defaultUserAgentString = EBWebView.getDefaultUserAgent(webView.context)
        val prefix: String =
            defaultUserAgentString.substring(0, defaultUserAgentString.indexOf(")") + 1)

        val isDesktopMode = config.browser.desktop
        try {
            when {
                isDesktopMode ->
                    // the trailing "Mobile Safari" token must go too — sites
                    // like zhihu/xiaohongshu match "Mobile" server-side and
                    // keep serving the app-jump page (issue #498)
                    webView.settings.userAgentString = defaultUserAgentString
                        .replace(prefix, BrowserUnit.UA_DESKTOP_PREFIX)
                        .replace(" Mobile ", " ")

                config.browser.enableCustomUserAgent && config.browser.customUserAgent.isNotBlank() ->
                    webView.settings.userAgentString = config.browser.customUserAgent

                else ->
                    webView.settings.userAgentString =
                        defaultUserAgentString.replace(prefix, BrowserUnit.UA_MOBILE_PREFIX)
            }
            updateUserAgentClientHints(isDesktopMode)
        } catch (e: Exception) {
        }

        webView.settings.useWideViewPort = isDesktopMode
        webView.settings.loadWithOverviewMode = isDesktopMode
    }

    // Overriding the UA string doesn't stop WebView from sending the system
    // default client hints (Sec-CH-UA-Mobile: ?1, Sec-CH-UA-Platform:
    // "Android") and exposing navigator.userAgentData.mobile == true, so
    // sites reading client hints still detect mobile in desktop mode.
    private fun updateUserAgentClientHints(isDesktopMode: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        // don't touch metadata until desktop mode is first enabled: setting it
        // (even to defaults) makes WebView expose high-entropy hints it would
        // otherwise keep empty while the UA string is overridden
        if (!isDesktopMode && !uaMetadataOverridden) return

        val defaultMetadata = defaultUserAgentMetadata
            ?: WebSettingsCompat.getUserAgentMetadata(webView.settings)
                .also { defaultUserAgentMetadata = it }

        val metadata = if (isDesktopMode) {
            // the brand list is sent on every request too, and "Android
            // WebView" in it gives the platform away just like the UA string
            val desktopBrands = defaultMetadata.brandVersionList.map { brandVersion ->
                if (brandVersion.brand.contains("Android"))
                    UserAgentMetadata.BrandVersion.Builder(brandVersion)
                        .setBrand("Google Chrome")
                        .build()
                else brandVersion
            }
            UserAgentMetadata.Builder(defaultMetadata)
                .setBrandVersionList(desktopBrands)
                .setMobile(false)
                .setPlatform("Linux")
                .setModel("")
                .setArchitecture("x86")
                .setBitness(64)
                .build()
        } else {
            defaultMetadata
        }
        WebSettingsCompat.setUserAgentMetadata(webView.settings, metadata)
        uaMetadataOverridden = isDesktopMode
    }

    fun toggleCookieSupport(isEnabled: Boolean) {
        with(cookieManager) {
            setAcceptCookie(isEnabled)
            setAcceptThirdPartyCookies(webView, isEnabled)
        }
    }
}
