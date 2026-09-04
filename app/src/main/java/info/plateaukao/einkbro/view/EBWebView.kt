package info.plateaukao.einkbro.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.SystemClock
import android.print.PrintDocumentAdapter
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumCallback
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.ChatWebInterface
import info.plateaukao.einkbro.browser.InputController
import info.plateaukao.einkbro.browser.JsBrowserCallback
import info.plateaukao.einkbro.browser.TabController
import info.plateaukao.einkbro.browser.WebChromeCallback
import info.plateaukao.einkbro.browser.WebViewCallback
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.EBClickHandler
import info.plateaukao.einkbro.browser.EBDownloadListener
import info.plateaukao.einkbro.browser.EBWebChromeClient
import info.plateaukao.einkbro.browser.EBWebViewClient
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.browser.JsWebInterface
import info.plateaukao.einkbro.caption.CaptionFetchResult
import info.plateaukao.einkbro.caption.DualCaptionProcessor
import info.plateaukao.einkbro.caption.YouTubeCaptionFetcher
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.unit.BookmarkRenderer
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.FaviconFetcher
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.PdfDocumentAdapter
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.resume



open class EBWebView(
    context: Context,
    var webViewCallback: WebViewCallback?,
) : WebView(context), AlbumController, KoinComponent {
    private var onScrollChangeListener: OnScrollChangeListener? = null
    // var: a lazily restored tab hands its Album over so the tab keeps its
    // identity in the list when the real WebView is created on activation
    override var album: Album = Album(this, webViewCallback as? AlbumCallback)
    internal val webViewClient: EBWebViewClient
    private val webChromeClient: EBWebChromeClient
    private val downloadListener by lazy { EBDownloadListener(this) }
    private val clickHandler: EBClickHandler
    val jsBridge: WebViewJsBridge = WebViewJsBridge(this).apply {
        simulateClickAction = { point -> touchSimulator.simulateClick(point) }
    }

    var dualCaption: String? = null
    var shouldHideTranslateContext: Boolean = false

    var baseUrl: String? = null

    /**
     * URL whose load failed and for which the offline error page is currently shown.
     * Non-null only while the error page is on screen. The error page is rendered with
     * [loadDataWithBaseURL] (not a real navigation), so reload()/retry and the address
     * bar must consult this flag to re-fetch and display the real URL instead of the
     * error document. Cleared in [resetState] whenever a genuine navigation begins.
     */
    var errorPageUrl: String? = null

    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val javascript: Javascript by inject()
    private val cookie: Cookie by inject()
    private val coroutineScope: CoroutineScope by inject()
    private val faviconFetcher: FaviconFetcher by inject()

    // Helpers for delegated concerns
    val readerHelper = WebViewReaderHelper(this, config)
    val translationHelper = WebViewTranslationHelper(this, config)
    val navigationHelper = WebViewNavigationHelper(this, config) { info ->
        (webViewCallback as? InputController)?.updatePageInfo(info)
    }
    private val configApplier = WebViewConfigApplier(this, config)
    private val touchSimulator = WebViewTouchSimulator(this)

    // Delegated reader state
    var isReaderModeOn: Boolean
        get() = readerHelper.isReaderModeOn
        set(value) { readerHelper.isReaderModeOn = value }
    var isVerticalRead: Boolean
        get() = readerHelper.isVerticalRead
        set(value) { readerHelper.isVerticalRead = value }

    // Rendered line advance (physical px) of the vertical-read text, measured
    // after entering vertical mode; 0 when unknown. Vertical page turns snap to
    // integer multiples of it so a page edge never slices a line of text.
    @Volatile
    var verticalLineAdvancePx: Float = 0f
    var isPlainText: Boolean
        get() = readerHelper.isPlainText
        set(value) { readerHelper.isPlainText = value }
    var isEpubReaderMode: Boolean
        get() = readerHelper.isEpubReaderMode
        set(value) { readerHelper.isEpubReaderMode = value }
    val isTwoColumnReaderOn: Boolean
        get() = readerHelper.isTwoColumnActive()

    // Delegated translation state
    var translateApi: TRANSLATE_API
        get() = translationHelper.translateApi
        set(value) { translationHelper.translateApi = value }
    var isTranslateByParagraph: Boolean
        get() = translationHelper.isTranslateByParagraph
        set(value) { translationHelper.isTranslateByParagraph = value }
    override var isTranslatePage = false
        set(value) {
            field = value
            if (value) {
                album.isTranslatePage = true
            }
        }
    override var isAIPage: Boolean = false

    var incognito: Boolean = false
        set(value) {
            field = value
            toggleCookieSupport(!value)
        }

    private var isForeground = false

    // Inner scrollable container state, updated via JsWebInterface callback.
    @Volatile
    var isInnerScrollAtTop: Boolean = true
    @Volatile
    var innerScrollTop: Int = 0
    @Volatile
    var innerScrollHeight: Int = 0
    @Volatile
    var innerClientHeight: Int = 0

    // Set on touchstart when the finger lands inside a CSS-scrollable container,
    // cleared on touchend/touchcancel. Used by SwipeRefreshLayout to skip pull-
    // to-refresh even when the inner element is already at scrollTop = 0.
    @Volatile
    var isTouchOnInnerScrollable: Boolean = false

    // True only if the content was already at top when the touch gesture started.
    // Prevents pull-to-refresh from triggering when scrolling up from the middle.
    var wasAtTopOnTouchStart: Boolean = true
        private set

    // Ebook touch gesture tracking — native interception replaces ebook_touch.js
    // so that taps on iframes (Instagram/Twitter embeds) also trigger page turns.
    private var ebookTouchStartX = 0f
    private var ebookTouchStartY = 0f
    private var ebookTouchStartTime = 0L
    private var ebookTouchTracking = false
    private var ebookTouchMoved = false
    private var ebookTouchMulti = false
    internal var ebookTouchTemporarilyDisabled = false

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            wasAtTopOnTouchStart = scrollY == 0 && isInnerScrollAtTop
        }

        if (!config.touch.isEbookModeActive || ebookTouchTemporarilyDisabled) {
            return super.dispatchTouchEvent(event)
        }

        val moveThresholdPx = EBOOK_MOVE_THRESHOLD_DP.dp(context)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ebookTouchStartX = event.x
                ebookTouchStartY = event.y
                ebookTouchStartTime = SystemClock.uptimeMillis()
                ebookTouchTracking = true
                ebookTouchMoved = false
                ebookTouchMulti = false
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                ebookTouchMulti = true
                ebookTouchTracking = false
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (ebookTouchTracking && !ebookTouchMoved) {
                    val dx = Math.abs(event.x - ebookTouchStartX)
                    val dy = Math.abs(event.y - ebookTouchStartY)
                    if (dx > moveThresholdPx || dy > moveThresholdPx) {
                        ebookTouchMoved = true
                        ebookTouchTracking = false
                    }
                }
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                if (!ebookTouchTracking) {
                    return super.dispatchTouchEvent(event)
                }

                ebookTouchTracking = false

                val duration = SystemClock.uptimeMillis() - ebookTouchStartTime
                if (duration > EBOOK_LONG_PRESS_MS) {
                    return super.dispatchTouchEvent(event)
                }

                // Check if action mode (text selection) is active — dismiss instead of paginating
                val callback = webViewCallback
                if (callback != null && callback.isActionModeActive()) {
                    sendCancelEvent(event)
                    post { callback.dismissActionMode() }
                    return true
                }

                // Qualifying ebook tap — paginate
                sendCancelEvent(event)

                val midX = width / 2f
                if (!config.touch.switchTouchAreaAction) {
                    if (ebookTouchStartX < midX) pageUpWithNoAnimation()
                    else pageDownWithNoAnimation()
                } else {
                    if (ebookTouchStartX < midX) pageDownWithNoAnimation()
                    else pageUpWithNoAnimation()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                ebookTouchTracking = false
                return super.dispatchTouchEvent(event)
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun sendCancelEvent(upEvent: MotionEvent) {
        val cancelEvent = MotionEvent.obtain(upEvent)
        cancelEvent.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }

    override fun onScrollChanged(l: Int, t: Int, old_l: Int, old_t: Int) {
        super.onScrollChanged(l, t, old_l, old_t)
        onScrollChangeListener?.onScrollChange(t, old_t)
    }

    fun setScrollChangeListener(onScrollChangeListener: OnScrollChangeListener?) {
        this.onScrollChangeListener = onScrollChangeListener
    }

    fun setOnPageFinishedAction(action: () -> Unit) = webViewClient.setOnPageFinishedAction(action)

    // Delegated to readerHelper
    fun updateCssStyle() = readerHelper.updateCssStyle()

    fun updateReaderSettingsStyle() = readerHelper.updateReaderSettingsStyle()

    private fun resetState(partial: Boolean = false) {
        errorPageUrl = null
        dualCaption = null
        isTranslatePage = false
        isTranslateByParagraph = false
        webViewCallback?.resetTranslateUI()

        if (!partial) {
            isVerticalRead = false
            isReaderModeOn = false
            verticalLineAdvancePx = 0f
        }
    }

    override fun reload() {
        // When the offline error page is showing, getUrl() is the error document, so a
        // plain super.reload() would just reload the error page. Re-fetch the real URL.
        if (retryErrorPage()) return

        resetState()
        settings.textZoom = config.display.fontSize
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        super.reload()

        postDelayed({
            if (isWebViewDestroyed) return@postDelayed
            if (config.browser.webLoadCacheFirst) settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }, 2000)
    }

    /**
     * Renders the offline error page for [failedUrl]. Uses [loadDataWithBaseURL] rather
     * than navigating to the asset file so the WebView's logical URL stays the failed URL:
     * the toolbar reload and the page's own Retry button then re-fetch the real site
     * instead of reloading file:///android_asset/error_page.html. [pageBaseUrl] carries the
     * url/reason query the page's script reads from location.search.
     */
    fun showOfflineErrorPage(failedUrl: String, pageBaseUrl: String) {
        album.isLoaded = true
        // The error page needs JS for the Retry button and the mini-game; the failed
        // site may have had JS disabled. loadUrl() restores the per-site setting on retry.
        settings.javaScriptEnabled = true
        val html = runCatching {
            context.assets.open("error_page.html").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return
        errorPageUrl = failedUrl
        loadDataWithBaseURL(pageBaseUrl, html, "text/html", "UTF-8", failedUrl)
    }

    /**
     * If the offline error page is showing, re-fetch the failed URL from the network and
     * return true; otherwise return false. Safe to call repeatedly — the flag is consumed
     * on the first call, so rapid Retry taps collapse into a single reload.
     */
    fun retryErrorPage(): Boolean {
        val failed = errorPageUrl ?: return false
        errorPageUrl = null
        // Force a real network fetch; ignore any cache-first setting for the retry so a
        // recovered connection actually reloads the page.
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        loadUrl(failed)
        return true
    }

    override fun goBack() {
        resetState()
        settings.textZoom = config.display.fontSize
        super.goBack()
    }

    /**
     * Reader mode state is per-document: the reader body and the JS-side
     * innerHTMLCache live in the page itself, so when a new document commits
     * through a path that bypasses loadUrl/reload/goBack (link taps, JS
     * navigations), the Kotlin-side flags must be dropped too. Leaving them set
     * makes the next reader-mode tap toggle the stale flag off instead of on,
     * and disableReaderMode() then restores a body cache the new document
     * doesn't have (issue #309). Called from onPageStarted; loads that already
     * went through resetState() make this a no-op.
     */
    fun resetReaderModeForNewPage() {
        if (!isReaderModeOn && !isVerticalRead) return
        isReaderModeOn = false
        isVerticalRead = false
        verticalLineAdvancePx = 0f
        settings.textZoom = config.display.fontSize
    }

    interface OnScrollChangeListener {
        fun onScrollChange(scrollY: Int, oldScrollY: Int)
    }

    init {
        isAIPage = false
        isForeground = false
        webViewClient =
            EBWebViewClient(this) { title, url -> webViewCallback?.addHistory(title, url) }
        webChromeClient = EBWebChromeClient(this, { onChromiumFavicon(it) }, webViewCallback as? WebChromeCallback)
        clickHandler = EBClickHandler { msg, event ->
            (webViewCallback as? InputController)?.onLongPress(msg, event)
        }
        initWebView()
        configApplier.initWebSettings()
        initPreferences()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initWebView() {
        if (BuildConfig.DEBUG || config.browser.debugWebView) {
            setWebContentsDebuggingEnabled(true)
        }

        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)
        setDownloadListener(downloadListener)

        configApplier.updateDarkMode()
        setupJsWebInterface()
    }

    /**
     * The GM_* native bridge. It is attached to every page (a WebView interface
     * cannot be scoped to a single origin), so it hands out no capability without a
     * per-injection token that only the injected userscript shim holds. Kept as a
     * property so the WebViewClient can mint/clear those tokens per navigation.
     */
    lateinit var userScriptBridge: info.plateaukao.einkbro.browser.UserScriptBridge
        private set

    /**
     * The `androidApp` bridge. Retained so a translation session can be opened/closed on
     * it: [JsWebInterface.getTranslation] is the only page-reachable method that spends
     * the user's translation/LLM key, and it is gated on a token minted here.
     */
    lateinit var jsWebInterface: JsWebInterface
        private set

    /** Mint a translation-session token so the injected monitor may call getTranslation. */
    fun beginTranslationSession(): String = jsWebInterface.beginTranslationSession()

    /** End the translation session; further getTranslation calls are rejected. */
    fun endTranslationSession() {
        if (::jsWebInterface.isInitialized) jsWebInterface.endTranslationSession()
    }

    private fun setupJsWebInterface() {
        jsWebInterface = JsWebInterface(this, webViewCallback as? JsBrowserCallback)
        addJavascriptInterface(jsWebInterface, "androidApp")
        userScriptBridge = info.plateaukao.einkbro.browser.UserScriptBridge(this)
        addJavascriptInterface(userScriptBridge, "einkbroGM")
        addJavascriptInterface(
            info.plateaukao.einkbro.browser.StartPageBridge(this),
            "einkbroStartPage",
        )
    }

    // region userscript support

    /**
     * The current page URL, captured on the main thread by the WebViewClient so the
     * userscript bridge can read it from its background thread without touching
     * WebView.getUrl() (which must only be called on the UI thread).
     */
    @Volatile
    var currentPageUrl: String? = null

    /** Menu commands registered via GM_registerMenuCommand on the current page (caption to fnId). */
    val userScriptMenuCommands = LinkedHashMap<String, String>()

    // Guards work that should run once per document even though onPageFinished
    // can fire 4-5 times for one page (redirects, hash navigations, SPA
    // re-commits — see WebContentPostProcessor). Cleared by the client when
    // the document actually changes, alongside the userscript registry.
    val perDocumentOnceKeys = mutableSetOf<String>()

    fun oncePerDocument(key: String, block: () -> Unit) {
        if (perDocumentOnceKeys.add(key)) block()
    }

    fun registerUserScriptMenuCommand(caption: String, fnId: String) {
        userScriptMenuCommands[caption] = fnId
    }

    fun unregisterUserScriptMenuCommand(fnId: String) {
        userScriptMenuCommands.values.remove(fnId)
    }

    fun invokeUserScriptMenuCommand(fnId: String) {
        evaluateJavascript("window.__einkbroGM && window.__einkbroGM.invokeMenu('$fnId');", null)
    }

    fun openInNewTab(url: String) {
        (webViewCallback as? info.plateaukao.einkbro.browser.TabController)?.addNewTab(url)
    }

    // endregion

    // Delegated to configApplier (see WebViewConfigApplier)
    fun initPreferences() {
        configApplier.initPreferences()
        // re-assert per-site overrides; the applier only knows global settings
        url?.let {
            settings.javaScriptEnabled = isJavascriptEnabled(it)
            toggleCookieSupport(shouldAcceptCookies(it))
        }
    }

    /**
     * Re-asserts everything a site rule controls that can change without a
     * reload: JS, cookies, text zoom, injected CSS (font, white background,
     * custom CSS) and colour inversion. Used when a same-document navigation
     * crosses into a different path rule. Desktop mode is deliberately not
     * handled here — it needs a reload (see NinjaWebViewClient).
     */
    fun applySiteOverrides(url: String) {
        settings.javaScriptEnabled = isJavascriptEnabled(url)
        applyImagePolicy(url)
        toggleCookieSupport(shouldAcceptCookies(url))
        if (!shouldUseReaderFont()) {
            settings.textZoom = config.getFontSize(url)
        }
        updateCssStyle()
        ViewUnit.invertColor(this, config.hasInvertedColor(url))
    }

    /** Per-site image switch first, then the global Images setting (issue #634). */
    fun applyImagePolicy(url: String) {
        settings.blockNetworkImage = !config.domain.getEnableImages(url)
    }

    // Per-site override first, then the global setting widened by the whitelist.
    private fun isJavascriptEnabled(url: String): Boolean =
        config.getEffectiveConfig(url).enableJavascript
            ?: (config.browser.enableJavascript || javascript.isWhite(url))

    private fun shouldAcceptCookies(url: String): Boolean =
        config.getEffectiveConfig(url).enableCookies
            ?: (config.browser.cookies || cookie.isWhite(url))

    fun updateUserAgentString() = configApplier.updateUserAgentString()

    fun applyDesktopMode(url: String) = configApplier.applyDesktopMode(url)

    fun desktopModeChanged(url: String): Boolean = configApplier.desktopModeChanged(url)

    private fun toggleCookieSupport(isEnabled: Boolean) = configApplier.toggleCookieSupport(isEnabled)

    private fun initAlbum() {
        album.albumTitle = context!!.getString(R.string.app_name)
        bookmarkManager.findFaviconBitmapBy(albumUrl)?.let {
            setAlbumCover(it)
        }
    }

    val requestHeaders: HashMap<String, String> = HashMap<String, String>().apply {
        put("DNT", "1")
        put("Save-Data", if (config.browser.enableSaveData) "on" else "off")
    }

    /* continue playing if preference is set */
    override fun onWindowVisibilityChanged(visibility: Int) {
        if (config.browser.continueMedia) {
            if (visibility != GONE && visibility != INVISIBLE) super.onWindowVisibilityChanged(
                VISIBLE
            )
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        if (url == Constants.START_PAGE_URL) {
            album.isLoaded = true
            BookmarkRenderer.loadStartPage(this)
            return
        }

        if (webViewCallback?.loadInSecondPane(url) == true) {
            return
        }

        resetState()

        bookmarkManager.findFaviconBitmapBy(url)?.let {
            setAlbumCover(it)
        }

        settings.javaScriptEnabled = isJavascriptEnabled(url)
        applyImagePolicy(url)
        toggleCookieSupport(shouldAcceptCookies(url))
        applyDesktopMode(url)

        pendingRequestedHost = Uri.parse(url).host
        super.loadUrl(url, additionalHttpHeaders)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(url: String) {
        album.isLoaded = true

        val partial = url.startsWith("javascript:") || url.startsWith("content:")
        resetState(partial)

        if (partial) {  // Daniel
            super.loadUrl(url)
            return
        }

        val processedUrl = url.trim { it <= ' ' }
        if (processedUrl.isEmpty()) {
            EBToast.show(context, R.string.toast_load_error)
            return
        }

        if (processedUrl == Constants.START_PAGE_URL) {
            BookmarkRenderer.loadStartPage(this)
            return
        }

        albumTitle = LOADING_TITLE
        // show progress right away
        if (url.startsWith("https")) {
            postDelayed({
                if (!isWebViewDestroyed && progress < FAKE_PRE_PROGRESS) update(FAKE_PRE_PROGRESS)
            }, 200)
        }

        if (webViewCallback?.loadInSecondPane(processedUrl) == true) {
            return
        }

        val strippedUrl = BrowserUnit.stripUrlQuery(processedUrl)

        bookmarkManager.findFaviconBitmapBy(strippedUrl)?.let {
            setAlbumCover(it)
        }

        settings.javaScriptEnabled = isJavascriptEnabled(url)
        applyImagePolicy(strippedUrl)
        toggleCookieSupport(shouldAcceptCookies(url))
        applyDesktopMode(url)

        val finalUrl = BrowserUnit.queryWrapper(context, strippedUrl)
        pendingRequestedHost = Uri.parse(finalUrl).host
        super.loadUrl(finalUrl, requestHeaders)
    }

    fun setAlbumCover(bitmap: Bitmap) = album.setAlbumCover(bitmap)

    private var chatWebInterface: ChatWebInterface? = null
    fun setupAiPage(
        lifecycleScope: LifecycleCoroutineScope,
        webContent: String,
        webTitle: String,
        webUrl: String,
        // Agent-mode extensions — all null in regular chat-with-web usage.
        agentMode: Boolean = false,
        initialSnapshot: info.plateaukao.einkbro.task.InitialPageSnapshot? = null,
        agentContext: android.content.Context? = null,
        agentBrowserState: info.plateaukao.einkbro.activity.BrowserState? = null,
        agentTtsViewModel: info.plateaukao.einkbro.viewmodel.TtsViewModel? = null,
    ) {
        isAIPage = true

        if (chatWebInterface == null) {
            chatWebInterface = ChatWebInterface(
                lifecycleScope = lifecycleScope,
                webView = this,
                webContent = webContent,
                webTitle = webTitle,
                webUrl = webUrl,
                onOpenNewTab = { url -> (webViewCallback as? TabController)?.addNewTab(url) },
                agentMode = agentMode,
                initialSnapshot = initialSnapshot,
                agentContext = agentContext,
                agentWebViewCallback = webViewCallback,
                agentBrowserState = agentBrowserState,
                agentTtsViewModel = agentTtsViewModel,
            )
        } else {
            chatWebInterface?.updateWebContent(webContent, webTitle, webUrl)
        }

        addJavascriptInterface(chatWebInterface!!, "AndroidInterface")
        loadUrl("file:///android_asset/chat.html")
    }

    fun runGptAction(gptActionInfo: ChatGPTActionInfo) {
        chatWebInterface?.sendMessageWithGptActionInfo(gptActionInfo)
    }

    /** Fire an initial user prompt into an already-configured agent chat session. */
    fun runAgentPrompt(prompt: String) {
        chatWebInterface?.runAgentTurn(prompt)
    }

    // Host of the last committed document, and the host whose icon the tab cover
    // was last set from a verified fetch (see FaviconFetcher).
    private var committedHost: String? = null
    private var verifiedCoverHost: String? = null

    // Host the user actually asked for (loadUrl), pending until its document
    // commits; when that commit lands on a different host — a cross-host redirect
    // (threads.net -> threads.com) — the requested host is kept so the favicon is
    // stored under it too, keeping icons on bookmarks that point at the old host.
    private var pendingRequestedHost: String? = null
    private var redirectedFromHost: String? = null

    /**
     * Chromium's onReceivedIcon bitmap is display-only. It is never persisted:
     * Chromium doesn't say which page the icon belongs to, and late deliveries
     * after a navigation (never cancelled) used to be stored under the new page's
     * host. Once this page's icon has been verified, late bitmaps are ignored too.
     */
    private fun onChromiumFavicon(bitmap: Bitmap) {
        if (verifiedCoverHost != null && verifiedCoverHost == committedHost) return
        setAlbumCover(bitmap)
    }

    /**
     * A document committed (doUpdateVisitedHistory). Crossing to another host
     * drops the previous site's icon from the tab: the cover becomes that host's
     * stored icon, or nothing, instead of lingering when the new page has none.
     */
    fun onDocumentCommitted(url: String) {
        val host = Uri.parse(url).host
        val requested = pendingRequestedHost
        pendingRequestedHost = null
        if (host == committedHost) return
        // A loadUrl that committed on another host was redirected; commits without
        // a pending request (link clicks, SPA routing) carry no alias.
        redirectedFromHost = requested?.takeIf { it != host }
        committedHost = host
        verifiedCoverHost = null
        album.setAlbumCover(host?.let { bookmarkManager.findFaviconBitmapBy(url) })
    }

    /**
     * Fetches and stores the finished page's own favicon. The document reports its
     * hostname alongside its icon links, so the icon is keyed by the page it came
     * from — the association WebView's onReceivedIcon cannot provide.
     */
    fun probeFavicon(pageUrl: String) {
        if (!pageUrl.startsWith("http") || isAIPage || errorPageUrl != null) return
        val host = Uri.parse(pageUrl).host ?: return
        val aliasHost = redirectedFromHost?.takeIf { !faviconFetcher.isHandled(it) }
        if (faviconFetcher.isHandled(host) && aliasHost == null) return
        evaluateJsFile("favicon_probe.js", withPrefix = false) { result ->
            val probe = FaviconFetcher.parseProbe(result)
            // No probe (JS unavailable) still gets the /favicon.ico fallback; a probe
            // from a different document than the one that finished is not trusted.
            if (probe != null && probe.host != host) return@evaluateJsFile
            val candidates = probe?.candidates.orEmpty()
            coroutineScope.launch(Dispatchers.IO) {
                val bitmap = faviconFetcher.storeForPage(host, pageUrl, candidates, aliasHost)
                withContext(Dispatchers.Main) {
                    if (isWebViewDestroyed || Uri.parse(url.orEmpty()).host != host) return@withContext
                    verifiedCoverHost = host
                    if (bitmap != null) setAlbumCover(bitmap)
                }
            }
        }
    }


    override var albumTitle: String
        get() = album.albumTitle
        set(value) {
            album.albumTitle = value
            update(value)
        }

    // if url is with prefix data, maybe it's translated data, need to use base url instead
    override val albumUrl: String
        get() = errorPageUrl
            ?: (if (url?.startsWith("data") == true) baseUrl else url).orEmpty()

    override var initAlbumUrl: String = ""
    override fun activate() {
        requestFocus()
        isForeground = true
        album.activate()

        // handle incognito case
        if (incognito || !config.browser.cookies) {
            toggleCookieSupport(false)
        } else {
            toggleCookieSupport(true)
        }

        if (!album.isLoaded && initAlbumUrl.isNotEmpty()) {
            loadUrl(initAlbumUrl)
        }

        resumeWebView()
    }

    override fun deactivate() {
        clearFocus()
        isForeground = false
        album.deactivate()
        if (!config.tab.enableWebBkgndLoad) pauseWebView()
    }

    override fun pauseWebView() {
        onPause()
        //pauseTimers()
    }

    override fun resumeWebView() {
        onResume()
        //resumeTimers()
    }

    fun update(progress: Int) {
        if (isForeground) {
            webViewCallback?.updateProgress(progress)
        }
    }

    fun update(title: String?) {
        album.albumTitle = title.orEmpty()
        // so that title on bottom bar can be updated
        webViewCallback?.updateTitle(album.albumTitle)
    }

    // Guards the postDelayed callbacks above (and agent tools holding weak references
    // to this tab): touching a destroyed WebView is undefined behavior on older vendor
    // builds.
    var isWebViewDestroyed = false
        private set

    override fun destroy() {
        isWebViewDestroyed = true
        chatWebInterface?.disposeAgent()
        stopLoading()
        onPause()
        clearHistory()
        visibility = GONE
        removeAllViews()
        super.destroy()
    }

    fun createPrintDocumentAdapter(
        documentName: String,
        onFinish: () -> Unit,
    ): PrintDocumentAdapter {
        val superAdapter = super.createPrintDocumentAdapter(documentName)
        return PdfDocumentAdapter(documentName, superAdapter, onFinish)
    }

    val isLoadFinish: Boolean
        get() = progress >= BrowserUnit.PROGRESS_MAX

    fun onLongPress(event: MotionEvent) {
        val click = clickHandler.obtainMessage()
        clickHandler.currentMotionEvent = MotionEvent.obtain(event)
        click.target = clickHandler
        requestFocusNodeHref(click)
    }

    //region Navigation (delegated to WebViewNavigationHelper)

    fun isAtTop(): Boolean = navigationHelper.isAtTop()

    fun jumpToTop() = navigationHelper.jumpToTop()

    fun jumpToBottom() = navigationHelper.jumpToBottom()

    open fun pageDownWithNoAnimation() = navigationHelper.pageDownWithNoAnimation()

    open fun pageUpWithNoAnimation() = navigationHelper.pageUpWithNoAnimation()

    fun sendPageDownKey() = navigationHelper.sendPageDownKey()

    fun sendPageUpKey() = navigationHelper.sendPageUpKey()

    fun updatePageInfo() = navigationHelper.updatePageInfo()

    protected fun shiftOffset(): Int = navigationHelper.shiftOffset()

    //endregion

    fun removeTextSelection() = jsBridge.removeTextSelection()

    // Delegated to touchSimulator (see WebViewTouchSimulator)
    fun clickLinkElement(point: Point) = touchSimulator.clickLinkElement(point)

    var isSelectingText: Boolean
        get() = touchSimulator.isSelectingText
        set(value) { touchSimulator.isSelectingText = value }

    fun selectLinkText(point: Point) = touchSimulator.selectLinkText(point)

    var rawHtmlCache: String? = null

    // Cancellable so callers can wrap in withTimeoutOrNull: the JS bridge callback is
    // not guaranteed to fire (CSP-blocked injection, page script errors), and a plain
    // suspendCoroutine would ignore the timeout's cancellation and hang forever.
    suspend fun getRawReaderHtml() = suspendCancellableCoroutine<String> { continuation ->
        if (isPlainText && rawHtmlCache != null) {
            continuation.resume(rawHtmlCache!!)
        } else if (!isReaderModeOn && !isTranslatePage) {
            jsBridge.injectMozReaderModeJs()
            jsBridge.getReaderModeBodyHtml(config.display.readerKeepExtraContent, url) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml = processedHtml.substring(1, processedHtml.length - 1)
                rawHtmlCache = rawHtml
                if (continuation.isActive) continuation.resume(rawHtml)
            }
        } else {
            evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml = processedHtml.substring(1, processedHtml.length - 1)
                rawHtmlCache = rawHtmlCache ?: rawHtml
                if (continuation.isActive) continuation.resume(rawHtml)
            }
        }
    }

    suspend fun getRawText(onGeminiTranscribe: (() -> Unit)? = null): String {
        prepareYoutubeCaption(onGeminiTranscribe)
        return suspendCancellableCoroutine { continuation ->
            getRawTextInto(continuation)
        }
    }

    // Video id of the last YouTube page whose caption fetch came up empty, so a
    // caption-less video doesn't re-hit the network on every AI action.
    private var noCaptionVideoId: String? = null

    // On YouTube video pages, actively fetch the caption transcript so AI features
    // (summarize / chat with web / page AI) work on it instead of the watch-page DOM.
    // No-op when the player's own timedtext request was already captured, or when the
    // video has no captions (getRawText then falls back to Readability extraction).
    // Failures (members-only video, Gemini error) are shown as a toast so a silent
    // fallback to page text isn't mistaken for a successful transcription.
    private suspend fun prepareYoutubeCaption(onGeminiTranscribe: (() -> Unit)?) {
        if (dualCaption != null) return
        val pageUrl = url ?: return
        val videoId = YouTubeCaptionFetcher.extractVideoId(pageUrl) ?: return
        if (videoId == noCaptionVideoId) return
        // Callers with a dialog pass their own notice; everyone else at least gets a
        // toast, because the Gemini fallback can take minutes with no other feedback.
        val notify = onGeminiTranscribe
            ?: { EBToast.show(context, R.string.gemini_transcribing_note) }
        // The full transcript is always fetched; this outer timeout only guards
        // against a hung network. Sized above the Gemini fallback's 10-minute read
        // timeout; on expiry getRawText falls back to page text.
        val result = withTimeoutOrNull(660_000) {
            YouTubeCaptionFetcher().fetchCaption(pageUrl, notify)
        } ?: CaptionFetchResult.Failed("timeout", transient = true)
        when (result) {
            is CaptionFetchResult.Captions -> dualCaption = result.timedTextJson
            CaptionFetchResult.None -> noCaptionVideoId = videoId
            is CaptionFetchResult.Failed -> {
                if (!result.transient) noCaptionVideoId = videoId
                withContext(Dispatchers.Main) {
                    EBToast.show(
                        context,
                        context.getString(R.string.video_transcription_failed, result.message)
                    )
                }
            }
        }
    }

    private fun getRawTextInto(continuation: CancellableContinuation<String>) {
        if (dualCaption != null) {
            continuation.resume(DualCaptionProcessor().convertToHtml(dualCaption ?: ""))
        } else if (!isReaderModeOn) {
            jsBridge.evaluateMozReaderModeJs {
                jsBridge.getReaderModeBodyText(config.display.readerKeepExtraContent) { text ->
                    if (text == "null") {
                        if (continuation.isActive) continuation.resume("")
                    } else {
                        val processedText = if (text.startsWith("\"") && text.endsWith("\"")) {
                            text.substring(1, text.length - 2)
                        } else text
                        if (continuation.isActive) continuation.resume(processedText)
                    }
                }
            }
        } else {
            evaluateJavascript(
                "(function() { return document.getElementsByTagName('html')[0].innerText; })();"
            ) { text ->
                val processedText = if (text.startsWith("\"") && text.endsWith("\"")) {
                    text.substring(1, text.length - 2)
                } else text
                if (continuation.isActive) continuation.resume(processedText)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (webViewCallback?.handleKeyEvent(event) == true) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }

    var hasVideo = false

    var isAudioOnlyMode = false
    fun toggleAudioOnlyMode() {
        isAudioOnlyMode = !isAudioOnlyMode
        if (isAudioOnlyMode) jsBridge.enableAudioOnlyMode()
        else jsBridge.disableAudioOnlyMode()
    }

    //region Reader mode (delegated to WebViewReaderHelper)

    fun toggleVerticalRead() = readerHelper.toggleVerticalRead()

    fun shouldUseReaderFont(): Boolean = readerHelper.shouldUseReaderFont()

    fun toggleReaderMode(isVertical: Boolean = false) = readerHelper.toggleReaderMode(isVertical)

    //endregion

    //region Translation (delegated to WebViewTranslationHelper)

    fun clearTranslationElements() = translationHelper.clearTranslationElements()

    fun translateByParagraphInPlaceReplace() = translationHelper.translateByParagraphInPlaceReplace()

    fun translateByParagraphInPlace() = translationHelper.translateByParagraphInPlace()

    fun addGoogleTranslation() = translationHelper.addGoogleTranslation()

    fun hideTranslateContext() = translationHelper.hideTranslateContext()

    //endregion

    fun showTranslation() = webViewCallback?.showTranslation(this)

    fun addSelectionChangeListener() = jsBridge.addSelectionChangeListener()

    fun highlightTextSelection(highlightStyle: HighlightStyle) =
        jsBridge.highlightTextSelection(highlightStyle)

    suspend fun getSelectedText(): String = jsBridge.getSelectedText()

    fun selectSentence(point: Point) = jsBridge.selectSentence(point)

    fun selectParagraph(point: Point) = jsBridge.selectParagraph(point)

    suspend fun getSelectedTextWithContext(contextLength: Int = 10): String =
        jsBridge.getSelectedTextWithContext(contextLength)

    fun evaluateJsFile(fileName: String, withPrefix: Boolean = true, callback: ValueCallback<String>? = null) =
        jsBridge.evaluateJsFile(fileName, withPrefix, callback)

    // Public wrappers for protected scroll range methods, used by WebViewNavigationHelper
    fun horizontalScrollRange(): Int = computeHorizontalScrollRange()
    fun verticalScrollRange(): Int = computeVerticalScrollRange()

    companion object {
        // Placeholder album title shown while a page is loading
        const val LOADING_TITLE = "..."

        private const val FAKE_PRE_PROGRESS = 5
        private const val EBOOK_MOVE_THRESHOLD_DP = 15
        private const val EBOOK_LONG_PRESS_MS = 400L

        private var cachedDefaultUserAgent: String? = null
        fun getDefaultUserAgent(context: Context): String {
            return cachedDefaultUserAgent ?: WebSettings.getDefaultUserAgent(context)
                .replace("wv", "")
                .replace(Regex("Version/\\d+\\.\\d+\\s"), "")
                .also { cachedDefaultUserAgent = it }
        }
    }

    init {
        initAlbum()
    }
}
