package info.plateaukao.einkbro.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.ClientCertRequest
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.activity.SettingActivity
import info.plateaukao.einkbro.activity.SettingRoute
import info.plateaukao.einkbro.caption.DualCaptionProcessor
import info.plateaukao.einkbro.data.remote.GoogleDriveRepository
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.EinkImageCache
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.WebViewConfigApplier
import io.github.edsuns.adfilter.AdFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.IOException


class EBWebViewClient(
    private val ebWebView: EBWebView,
    private val addHistoryAction: (String, String) -> Unit,
) : WebViewClient(), KoinComponent {
    private val context: Context = ebWebView.context
    private val config: ConfigManager by inject()
    private val cookie: Cookie by inject()
    private val adBlock: AdBlock by inject()

    private val webContentPostProcessor = WebContentPostProcessor()
    private var hasAdBlock: Boolean = true

    private val adFilter: AdFilter = AdFilter.get()

    private val dualCaptionProcessor = DualCaptionProcessor()

    private val einkImageInterceptor = EinkImageInterceptor(config, EinkImageCache(context))
    private val errorPagePresenter = WebErrorPagePresenter(ebWebView)
    private val sslHandler = WebViewSslHandler(context, config)

    private val userScriptManager: info.plateaukao.einkbro.userscript.UserScriptManager by inject()
    private val coroutineScope: CoroutineScope by inject()
    private val googleDriveRepository: GoogleDriveRepository by inject()

    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    /** Per-site override first, then the global setting, minus the whitelist. */
    private fun isAdBlockEnabled(pageUrl: String?): Boolean {
        if (pageUrl == null) return config.browser.adBlock
        return (config.getDomainConfig(pageUrl).enableAdBlock ?: config.browser.adBlock) &&
                !adBlock.isWhite(pageUrl)
    }


    private var onPageFinishedAction: () -> Unit = {}
    fun setOnPageFinishedAction(action: () -> Unit) {
        onPageFinishedAction = action
    }

    private var lastVisitedHistoryKey: String? = null

    /** URL the userscript menu registry was last cleared for; see [onPageStarted]. */
    private var lastUserScriptUrl: String? = null

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        // Update hasVideo for SPA navigations (e.g., YouTube client-side routing)
        // where onPageFinished doesn't fire
        ebWebView.hasVideo = WebContentPostProcessor.isVideoSiteUrl(url)

        // Drop the captured caption when the user navigates to a different page
        // (including YouTube SPA route changes that bypass loadUrl/resetState).
        // YouTube rewrites the URL with timestamp/playback params (t, pp, ...)
        // while the user watches, which would otherwise wipe the caption mid-play.
        val key = navigationKey(url)
        val previous = lastVisitedHistoryKey
        if (previous != null && previous != key) {
            ebWebView.dualCaption = null
        }
        lastVisitedHistoryKey = key
    }

    private fun navigationKey(url: String): String {
        return try {
            val uri = url.toUri()
            val host = uri.host.orEmpty()
            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                val videoId = uri.getQueryParameter("v")
                    ?: uri.lastPathSegment // youtu.be/<id>, /shorts/<id>
                    ?: ""
                "${uri.scheme}://${host}${uri.path.orEmpty()}?v=$videoId"
            } else {
                "${uri.scheme}://${host}${uri.path.orEmpty()}?${uri.query.orEmpty()}"
            }
        } catch (e: Exception) {
            url
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        ebWebView.currentPageUrl = url
        // resetState() in EBWebView already cleared dualCaption; let the next
        // doUpdateVisitedHistory treat this as a fresh start so it doesn't wipe the
        // caption captured during this page's load.
        lastVisitedHistoryKey = null
        ebWebView.isInnerScrollAtTop = true
        ebWebView.innerScrollTop = 0
        ebWebView.innerScrollHeight = 0
        ebWebView.innerClientHeight = 0
        ebWebView.isTouchOnInnerScrollable = false

        if (isAdBlockEnabled(url)) {
            adFilter.performScript(view, url)
        }

        // Fallback for WebViews without DOCUMENT_START_SCRIPT; newer ones get the
        // blocker injected before any page script via addDocumentStartJavaScript
        // (see WebViewConfigApplier.applyAutoplayBlocker).
        if (!config.browser.enableVideoAutoplay && !WebViewConfigApplier.supportsDocumentStartScript()) {
            ebWebView.evaluateJsFile("disable_video_autoplay.js", withPrefix = false)
        }
        if (!WebViewConfigApplier.supportsDocumentStartScript()) {
            ebWebView.evaluateJsFile("speech_synthesis_polyfill.js", withPrefix = false)
        }

        url?.let { injectForcedViewportWidth(it) }

        // userscripts are page-scoped; reset the per-page menu registry only when the
        // document actually changes. onPageStarted can re-fire on the same document
        // (redirects, progressive commits), and the scripts inject exactly once per
        // document now (see injectUserScripts) — clearing on every call would wipe the
        // menu commands those scripts already registered without re-registering them.
        url?.let { u ->
            if (u != lastUserScriptUrl) {
                ebWebView.userScriptMenuCommands.clear()
                lastUserScriptUrl = u
            }
            injectUserScripts(u, info.plateaukao.einkbro.userscript.RunAt.DOCUMENT_START)
        }
    }

    private fun injectUserScripts(url: String, runAt: info.plateaukao.einkbro.userscript.RunAt) {
        val matching = userScriptManager.getMatchingScripts(url, runAt)
        if (matching.isEmpty()) return
        matching.forEach { parsed ->
            // Inject as a same-origin <script> element rather than evaluateJavascript():
            // eval'd code has no script origin, so Chromium sanitizes its async exceptions
            // to opaque "Script error." and some scripts misbehave. A script tag runs the
            // userscript exactly like a real userscript manager does. The body is passed as
            // base64 to avoid escaping issues with large UTF-8 payloads.
            val js = userScriptManager.buildInjectionJs(parsed)
            val b64 = android.util.Base64.encodeToString(
                js.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP,
            )
            // Guard against running the same script twice in one document. WebView's page
            // callbacks are not once-per-page (redirects, multiple onPageFinished, SPA
            // re-commits all re-fire them), so without this a script that appends UI — e.g.
            // a floating button — would stack a fresh copy on every callback. The flag lives
            // on `window`, which is fresh on each real navigation, so genuine page loads still
            // run the script exactly once. Keyed by script id so distinct scripts are independent.
            val scriptId = parsed.script.id
            val injector = """
                (function(){
                    try {
                        var reg = window.__einkbroInjected || (window.__einkbroInjected = {});
                        if (reg[$scriptId]) return;
                        reg[$scriptId] = true;
                        var s = document.createElement('script');
                        s.textContent = decodeURIComponent(escape(atob('$b64')));
                        (document.head || document.documentElement).appendChild(s);
                        s.parentNode && s.parentNode.removeChild(s);
                    } catch(e) { console.error('einkbro userscript inject', e); }
                })();
            """.trimIndent()
            ebWebView.evaluateJavascript(injector, null)
        }
    }

    private fun injectForcedViewportWidth(url: String) {
        val width = config.getDesktopViewportWidth(url) ?: return
        if (width <= 0) return
        val script = HelperUnit.loadAssetFile("force_viewport_width.js")
            .replace("__WIDTH__", width.toString())
        ebWebView.evaluateJavascript(script, null)
    }

    override fun onPageFinished(view: WebView, url: String) {
        ebWebView.currentPageUrl = url
        ebWebView.updateCssStyle()

        // Re-inject autoplay blocker in onPageFinished to ensure it's in the correct page context
        // (onPageStarted injection may race with the page's own scripts)
        if (!config.browser.enableVideoAutoplay && !WebViewConfigApplier.supportsDocumentStartScript()) {
            ebWebView.evaluateJsFile("disable_video_autoplay.js", withPrefix = false)
        }
        if (!WebViewConfigApplier.supportsDocumentStartScript()) {
            ebWebView.evaluateJsFile("speech_synthesis_polyfill.js", withPrefix = false)
        }

        // Safety net for sites whose hydration chain is broken by our filters
        // (see fix_dsd_pending.js).
        ebWebView.evaluateJsFile("fix_dsd_pending.js", withPrefix = false)

        Log.d("ebWebViewClient", "onPageFinished: ${ebWebView.url}\n$url")
        webContentPostProcessor.postProcess(ebWebView, url)

        if (ebWebView.shouldHideTranslateContext) {
            ebWebView.postDelayed({
                ebWebView.hideTranslateContext()
            }, 2000)
        }

        ebWebView.postDelayed({
            ebWebView.updatePageInfo()
        }, 1000)

        // skip translation pages and AI chat pages (chat.html has no standalone meaning)
        if (config.tab.isSaveHistoryWhenLoad() &&
            !ebWebView.incognito &&
            !ebWebView.isAIPage &&
            !isTranslationDomain(url) &&
            url != BrowserUnit.URL_ABOUT_BLANK &&
            ebWebView.errorPageUrl == null
        ) {
            addHistoryAction(ebWebView.albumTitle, url)
        }

        // touch target tracking for link detection
        ebWebView.evaluateJavascript(
            """
                    (function(){
                        if(window.__einkbroTouchInit) return;
                        window.__einkbroTouchInit = true;
                        function findTargetWithA(e){
                            var tt = e;
                            while(tt){
                                if(tt.tagName.toLowerCase() == "a"){
                                    break;
                                }
                                tt = tt.parentElement;
                            }
                            return tt;
                        }
                        window.addEventListener('touchstart', function(e){
                            if(e.touches.length==1){
                                window._touchTarget = findTargetWithA(e.touches[0].target);
                            }
                        });
                    })();
        """.trimIndent(), null
        )

        if (url != "about:blank") {
            onPageFinishedAction()
            config.getPostLoadJavascript(url)?.let { userJs ->
                ebWebView.evaluateJavascript(
                    "(function(){try{$userJs}catch(e){console.error('einkbro user js',e);}})();",
                    null,
                )
            }
            injectUserScripts(url, info.plateaukao.einkbro.userscript.RunAt.DOCUMENT_END)
        }
    }

    private fun isUserScriptUrl(url: String): Boolean {
        val path = try {
            Uri.parse(url).path?.lowercase().orEmpty()
        } catch (e: Exception) {
            ""
        }
        return path.endsWith(".user.js")
    }

    private fun offerUserScriptInstall(url: String) {
        // The activity downloads the script itself (showing a progress indicator); only
        // the URL is passed, since multi-MB bodies overflow the 1MB Binder limit.
        context.startActivity(
            info.plateaukao.einkbro.activity.UserScriptListActivity.createInstallIntent(context, url)
        )
    }

    private fun isTranslationDomain(url: String): Boolean {
        return url.contains("translate.goog") || url.contains("papago.naver.net")
                || url.contains("papago.naver.com") || url.contains("translate.google.com")
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        handleUri(view, request.url)

    private fun handleUri(webView: WebView, uri: Uri): Boolean {
        val url = uri.toString()
        Log.d("ebWebViewClient", "handleUri: $url")

        if (url.startsWith("einkbro://retry")) {
            // retryErrorPage() consumes the stored failed URL, so repeated taps that
            // arrive before the reload commits collapse into a single network fetch.
            webView.post {
                if (!ebWebView.retryErrorPage()) ebWebView.reload()
            }
            return true
        }

        // Google Drive sync OAuth redirect: exchange the code for tokens, then
        // land the user back in Backup settings where the sync action lives.
        if (url.startsWith(BuildConfig.DRIVE_OAUTH_REDIRECT)) {
            coroutineScope.launch {
                val success = runCatching { googleDriveRepository.completeAuth(uri) }
                    .getOrDefault(false)
                withContext(Dispatchers.Main) {
                    if (success) {
                        context.startActivity(
                            SettingActivity.createIntent(context, SettingRoute.Backup)
                        )
                    } else {
                        EBToast.show(context, R.string.drive_sign_in_failed)
                    }
                }
            }
            return true
        }

        val list = webView.copyBackForwardList()

        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)
            val title = item.title
            val url = item.url
            Log.d("ebWebViewClient", "Title: $title - URL: $url")
        }

        if (url.startsWith("http")) {
            if (isUserScriptUrl(url)) {
                offerUserScriptInstall(url)
                return true
            }
            // Link clicks don't go through loadUrl, so a per-site desktop-mode
            // override would leak to the next site. Changing the UA here makes
            // Chromium reload the *current* page, racing the link navigation —
            // so take the navigation over instead: loadUrl applies the UA for
            // the target and then loads it.
            if (ebWebView.desktopModeChanged(url)) {
                ebWebView.loadUrl(url)
                return true
            }
            return false
        }

        val packageManager = context.packageManager
        val browseIntent = Intent(Intent.ACTION_VIEW).setData(uri)
        if (browseIntent.resolveActivity(packageManager) != null) {
            try {
                context.startActivity(browseIntent)
                return true
            } catch (e: Exception) {
            }
        }
        if (url.startsWith("intent:")) {
            // prevent google map intent from opening google map app directly. Use browser instead!
            if (url.startsWith("intent://www.google.com/maps")) {
                return true
            }
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(context.packageManager) != null || intent.data?.scheme == "market") {
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        EBToast.show(context, R.string.toast_load_error)
                    }
                    return true
                }

                if (maybeHandleFallbackUrl(webView, intent)) return true

                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                //not an intent uri
                return false
            }
        }

        // handle rest scenarios: something like abc://, xyz://
        try {
            context.startActivity(browseIntent)
        } catch (e: Exception) {
            // ignore
        }

        return true //do nothing in other cases
    }

    private fun maybeHandleFallbackUrl(webView: WebView, intent: Intent): Boolean {
        val fallbackUrl = intent.getStringExtra("browser_fallback_url") ?: return false
        if (fallbackUrl.startsWith("market://")) {
            val intent = Intent.parseUri(fallbackUrl, Intent.URI_INTENT_SCHEME)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                EBToast.show(context, R.string.toast_load_error)
            }
            return true
        }

        webView.loadUrl(fallbackUrl)
        return true
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?,
    ) {
        sslHandler.onReceivedHttpAuthRequest(handler)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) = errorPagePresenter.onReceivedError(view, request, error)

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        handleWebRequest(view, Uri.parse(url), null) ?: super.shouldInterceptRequest(view, url)

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        // Fast analytics/tracker blocking (lightweight check before expensive ad-filter)
        if (config.browser.blockAnalytics && isAnalyticsUrl(request.url.toString())) {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        val pageUrl =
            if (request.isForMainFrame) request.url.toString() else ebWebView.currentPageUrl
        if (isAdBlockEnabled(pageUrl)) {
            val result = adFilter.shouldIntercept(view, request)
            if (result.shouldBlock) {
                //Log.d("EBWebViewClient", "blocked\n rule: ${result.rule}\n url:${result.resourceUrl}")
                return result.resourceResponse
            }
        }

        einkImageInterceptor.processEinkImageRequest(request)?.let { return it }

        return handleWebRequest(view, request.url, request.requestHeaders, request.isForMainFrame)
            ?: super.shouldInterceptRequest(view, request)
    }

    private fun isAnalyticsUrl(url: String): Boolean =
        ANALYTICS_DOMAINS.any { url.contains(it) }

    private fun handleWebRequest(
        webView: WebView,
        uri: Uri,
        requestHeaders: Map<String, String>?,
        isMainFrame: Boolean = false,
    ): WebResourceResponse? {
        val url = uri.toString()

        // setAcceptCookie is process-wide, so it must be re-asserted on every
        // request: link-click navigation never goes through loadUrl, and a
        // per-site override on the previous page would otherwise stick forever.
        // currentPageUrl lags for the main frame's own request — use its URL.
        val pageUrl = if (isMainFrame) url else ebWebView.currentPageUrl ?: url
        val acceptCookies = config.getDomainConfig(pageUrl).enableCookies
            ?: (config.browser.cookies || cookie.isWhite(url))
        val manager = CookieManager.getInstance()
        if (acceptCookies && !config.browser.cookies) {
            manager.getCookie(url)
        }
        manager.setAcceptCookie(acceptCookies)

        processCustomFontRequest(uri)?.let { return it }
        dualCaptionProcessor.processUrl(url, requestHeaders)?.let {
            ebWebView.dualCaption = it
            return WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(it.toByteArray())
            )
        }

        return null
    }

    private fun processCustomFontRequest(uri: Uri): WebResourceResponse? {
        if (uri.path?.contains("mycustomfont") == true) {
            val fontUri = if (!ebWebView.shouldUseReaderFont()) {
                config.display.customFontInfo?.url?.toUri() ?: return null
            } else {
                config.display.readerCustomFontInfo?.url?.toUri() ?: return null
            }

            try {
                val inputStream = context.contentResolver.openInputStream(fontUri)
                return WebResourceResponse("application/x-font-ttf", "UTF-8", inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        return null
    }

    override fun onFormResubmission(view: WebView, doNotResend: Message, resend: Message) {
        val holder = view.context as? Activity ?: return
        val showDialog = mutableStateOf(true)
        val rootView = holder.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(holder).apply {
            setViewTreeLifecycleOwner(holder as androidx.lifecycle.LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(holder as androidx.savedstate.SavedStateRegistryOwner)
        }
        rootView.addView(composeView)
        composeView.setContent {
            MyTheme {
                if (showDialog.value) {
                    androidx.compose.material.AlertDialog(
                        onDismissRequest = {
                            showDialog.value = false
                            doNotResend.sendToTarget()
                            rootView.removeView(composeView)
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.dialog_content_resubmission),
                                color = MaterialTheme.colors.onBackground,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDialog.value = false
                                resend.sendToTarget()
                                rootView.removeView(composeView)
                            }) {
                                Text(
                                    text = stringResource(android.R.string.ok),
                                    color = MaterialTheme.colors.onBackground,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDialog.value = false
                                doNotResend.sendToTarget()
                                rootView.removeView(composeView)
                            }) {
                                Text(
                                    text = stringResource(android.R.string.cancel),
                                    color = MaterialTheme.colors.onBackground,
                                )
                            }
                        },
                        backgroundColor = MaterialTheme.colors.background,
                    )
                }
            }
        }
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        sslHandler.onReceivedClientCertRequest(view, request) {
            super.onReceivedClientCertRequest(view, request)
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) =
        sslHandler.onReceivedSslError(view, handler, error)

    companion object {
        private val ANALYTICS_DOMAINS = listOf(
            "google-analytics.com",
            "googletagmanager.com",
            "connect.facebook.net",
            "platform.twitter.com/widgets.js",
            "cdn.segment.com",
            "static.hotjar.com",
            "bat.bing.com",
            "mc.yandex.ru",
            "analytics.tiktok.com",
            "snap.licdn.com",
            "cdn.mouseflow.com",
            "cdn.heapanalytics.com",
        )
    }
}