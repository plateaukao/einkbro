package info.plateaukao.einkbro.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.print.PrintDocumentAdapter
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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
import info.plateaukao.einkbro.caption.DualCaptionProcessor
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.PdfDocumentAdapter
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine



open class EBWebView(
    context: Context,
    var webViewCallback: WebViewCallback?,
) : WebView(context), AlbumController, KoinComponent {
    private var onScrollChangeListener: OnScrollChangeListener? = null
    override val album: Album = Album(this, webViewCallback as? AlbumCallback)
    protected val webViewClient: EBWebViewClient
    private val webChromeClient: EBWebChromeClient
    private val downloadListener: EBDownloadListener = EBDownloadListener(context)
    private val clickHandler: EBClickHandler
    val jsBridge: WebViewJsBridge = WebViewJsBridge(this).apply {
        simulateClickAction = { point -> simulateClick(point) }
    }

    var dualCaption: String? = null
    var shouldHideTranslateContext: Boolean = false

    var baseUrl: String? = null
    private val cookieManager: CookieManager = CookieManager.getInstance()

    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val javascript: Javascript by inject()
    private val cookie: Cookie by inject()
    private val coroutineScope: CoroutineScope by inject()

    // Helpers for delegated concerns
    val readerHelper = WebViewReaderHelper(this, config)
    val translationHelper = WebViewTranslationHelper(this, config)
    val navigationHelper = WebViewNavigationHelper(this, config) { info ->
        (webViewCallback as? InputController)?.updatePageInfo(info)
    }

    // Delegated reader state
    var isReaderModeOn: Boolean
        get() = readerHelper.isReaderModeOn
        set(value) { readerHelper.isReaderModeOn = value }
    var isVerticalRead: Boolean
        get() = readerHelper.isVerticalRead
        set(value) { readerHelper.isVerticalRead = value }
    var isPlainText: Boolean
        get() = readerHelper.isPlainText
        set(value) { readerHelper.isPlainText = value }
    var isEpubReaderMode: Boolean
        get() = readerHelper.isEpubReaderMode
        set(value) { readerHelper.isEpubReaderMode = value }

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
    private var ebookTouchTemporarilyDisabled = false

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

    private fun resetState(partial: Boolean = false) {
        dualCaption = null
        isTranslatePage = false
        isTranslateByParagraph = false
        webViewCallback?.resetTranslateUI()

        if (!partial) {
            isVerticalRead = false
            isReaderModeOn = false
        }
    }

    override fun reload() {
        resetState()
        settings.textZoom = config.display.fontSize
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        super.reload()

        postDelayed({
            if (config.webLoadCacheFirst) settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }, 2000)
    }

    override fun goBack() {
        resetState()
        settings.textZoom = config.display.fontSize
        super.goBack()
    }

    interface OnScrollChangeListener {
        fun onScrollChange(scrollY: Int, oldScrollY: Int)
    }

    init {
        isAIPage = false
        isForeground = false
        webViewClient =
            EBWebViewClient(this) { title, url -> webViewCallback?.addHistory(title, url) }
        webChromeClient = EBWebChromeClient(this, { setAlbumCoverAndSyncDb(it) }, webViewCallback as? WebChromeCallback)
        clickHandler = EBClickHandler { msg, event ->
            (webViewCallback as? InputController)?.onLongPress(msg, event)
        }
        initWebView()
        initWebSettings()
        initPreferences()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initWebView() {
        if (BuildConfig.DEBUG || config.debugWebView) {
            setWebContentsDebuggingEnabled(true)
        }

        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)
        setDownloadListener(downloadListener)

        updateDarkMode()
        setupJsWebInterface()
    }

    private fun setupJsWebInterface() {
        addJavascriptInterface(JsWebInterface(this, webViewCallback as? JsBrowserCallback), "androidApp")
    }

    @Suppress("DEPRECATION")
    private fun updateDarkMode() {
        if (config.display.darkMode == DarkMode.DISABLED) {
            return
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                settings,
                WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES ||
                config.display.darkMode == DarkMode.FORCE_ON
            ) {
                settings.forceDark = WebSettings.FORCE_DARK_ON
                // when in dark mode, the default background color will be the activity background
                setBackgroundColor(Color.parseColor("#000000"))
            }

        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
            }
        }
    }

    private fun initWebSettings() {
        with(settings) {
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
        setLayerType(LAYER_TYPE_HARDWARE, null) // Enable hardware acceleration

        with(settings) {
            // don't load cache by default, so that it won't cause some issues
            if (config.webLoadCacheFirst)
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

            textZoom = config.display.fontSize
            allowFileAccessFromFileURLs = config.enableRemoteAccess
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = config.enableRemoteAccess
            domStorageEnabled = true
            databaseEnabled = true
            blockNetworkImage = !config.enableImages
            javaScriptEnabled = config.enableJavascript
            javaScriptCanOpenWindowsAutomatically = config.enableJavascript
            setSupportMultipleWindows(config.enableJavascript)
            setGeolocationEnabled(config.shareLocation)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = !config.enableVideoAutoplay
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill =
                    if (config.autoFillForm) IMPORTANT_FOR_AUTOFILL_YES else IMPORTANT_FOR_AUTOFILL_NO
            } else {
                saveFormData = config.autoFillForm
            }
        }
        webViewClient.enableAdBlock(config.adBlock)
        toggleCookieSupport(config.cookies)
    }

    fun updateUserAgentString() {
        val defaultUserAgentString = getDefaultUserAgent(context)
        val prefix: String =
            defaultUserAgentString.substring(0, defaultUserAgentString.indexOf(")") + 1)

        val isDesktopMode = config.desktop
        try {
            when {
                isDesktopMode ->
                    settings.userAgentString =
                        defaultUserAgentString.replace(prefix, BrowserUnit.UA_DESKTOP_PREFIX)

                config.enableCustomUserAgent && config.customUserAgent.isNotBlank() ->
                    settings.userAgentString = config.customUserAgent

                else ->
                    settings.userAgentString =
                        defaultUserAgentString.replace(prefix, BrowserUnit.UA_MOBILE_PREFIX)
            }
        } catch (e: Exception) {
        }

        settings.useWideViewPort = isDesktopMode
        settings.loadWithOverviewMode = isDesktopMode
    }

    private fun toggleCookieSupport(isEnabled: Boolean) {
        with(cookieManager) {
            setAcceptCookie(isEnabled)
            setAcceptThirdPartyCookies(this@EBWebView, isEnabled)
        }
    }

    private fun initAlbum() {
        album.albumTitle = context!!.getString(R.string.app_name)
        bookmarkManager.findFaviconBy(albumUrl)?.getBitmap()?.let {
            setAlbumCover(it)
        }
    }

    val requestHeaders: HashMap<String, String> = HashMap<String, String>().apply {
        put("DNT", "1")
        put("Save-Data", if (config.enableSaveData) "on" else "off")
    }

    /* continue playing if preference is set */
    override fun onWindowVisibilityChanged(visibility: Int) {
        if (config.continueMedia) {
            if (visibility != GONE && visibility != INVISIBLE) super.onWindowVisibilityChanged(
                VISIBLE
            )
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        if (webViewCallback?.loadInSecondPane(url) == true) {
            return
        }

        resetState()

        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            setAlbumCover(it)
        }

        settings.javaScriptEnabled = config.enableJavascript || javascript.isWhite(url)
        toggleCookieSupport(config.cookies || cookie.isWhite(url))

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

        albumTitle = "..."
        // show progress right away
        if (url.startsWith("https")) {
            postDelayed({ if (progress < FAKE_PRE_PROGRESS) update(FAKE_PRE_PROGRESS) }, 200)
        }

        if (webViewCallback?.loadInSecondPane(processedUrl) == true) {
            return
        }

        val strippedUrl = BrowserUnit.stripUrlQuery(processedUrl)

        bookmarkManager.findFaviconBy(strippedUrl)?.getBitmap()?.let {
            setAlbumCover(it)
        }

        settings.javaScriptEnabled = config.enableJavascript || javascript.isWhite(url)
        toggleCookieSupport(config.cookies || cookie.isWhite(url))

        super.loadUrl(BrowserUnit.queryWrapper(context, strippedUrl), requestHeaders)
    }

    fun setAlbumCover(bitmap: Bitmap) = album.setAlbumCover(bitmap)

    private var chatWebInterface: ChatWebInterface? = null
    fun setupAiPage(lifecycleScope: LifecycleCoroutineScope, webContent: String, webTitle: String, webUrl: String) {
        isAIPage = true

        if (chatWebInterface == null) {
            chatWebInterface = ChatWebInterface(
                lifecycleScope, this, webContent, webTitle, webUrl,
                onOpenNewTab = { url -> (webViewCallback as? TabController)?.addNewTab(url) }
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

    private fun setAlbumCoverAndSyncDb(bitmap: Bitmap) {
        setAlbumCover(bitmap)

        if (originalUrl == null) return
        val host = Uri.parse(originalUrl).host ?: return
        coroutineScope.launch {
            bookmarkManager.insertFavicon(FaviconInfo(domain = host, bitmap.convertBytes()))
        }
    }

    private fun Bitmap.convertBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 0, stream)
        return stream.toByteArray()
    }


    override var albumTitle: String
        get() = album.albumTitle
        set(value) {
            album.albumTitle = value
            update(value)
        }

    // if url is with prefix data, maybe it's translated data, need to use base url instead
    override val albumUrl: String
        get() = (if (url?.startsWith("data") == true) baseUrl else url).orEmpty()

    override var initAlbumUrl: String = ""
    override fun activate() {
        requestFocus()
        isForeground = true
        album.activate()

        // handle incognito case
        if (incognito || !config.cookies) {
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
        if (!config.enableWebBkgndLoad) pauseWebView()
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

    override fun destroy() {
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

    fun clickLinkElement(point: Point) {
        ebookTouchTemporarilyDisabled = true
        simulateClick(point)
        postDelayed({ ebookTouchTemporarilyDisabled = false }, 200)
    }

    var isSelectingText = false
    fun selectLinkText(point: Point) {
        evaluateJavascript(
            """
            javascript:(function() {
                 var tt = window._touchTarget;
                 if(tt){
                     var hrefAttr = tt.getAttribute("href");
                     tt.removeAttribute("href");
                     window._hrefAttr = hrefAttr;

                     var sel = window.getSelection();
                     sel.removeAllRanges();
                 }
            })()
        """.trimIndent()
        ) {
            postDelayed({ simulateLongClick(point) }, 0)
        }
    }

    private fun simulateClick(point: Point) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(
                downTime, downTime, KeyEvent.ACTION_DOWN,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        (this.parent as ViewGroup).dispatchTouchEvent(downEvent)

        val upEvent =
            MotionEvent.obtain(
                downTime, downTime + 700, KeyEvent.ACTION_UP,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        postDelayed(
            {
                (this.parent as ViewGroup).dispatchTouchEvent(upEvent)
                downEvent.recycle()
                upEvent.recycle()
            }, 50
        )
    }

    private fun simulateLongClick(point: Point) {
        isSelectingText = true
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(
                downTime, downTime, KeyEvent.ACTION_DOWN,
                (point.x + 20).toFloat(), point.y.toFloat(), 0
            )
        (this.parent as ViewGroup).dispatchTouchEvent(downEvent)

        val upEvent =
            MotionEvent.obtain(
                downTime, downTime + 700, KeyEvent.ACTION_UP,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        postDelayed(
            {
                (this.parent as ViewGroup).dispatchTouchEvent(upEvent)
                downEvent.recycle()
                upEvent.recycle()
            }, 700
        )
        postDelayed(
            {
                evaluateJavascript(
                    """
                        var tt = window._touchTarget;
                        if(tt){
                            tt.setAttribute("href", window._hrefAttr);
                        }
                """.trimIndent(), null
                )
                isSelectingText = false
            }, 1000
        )
    }

    var rawHtmlCache: String? = null
    suspend fun getRawReaderHtml() = suspendCoroutine { continuation ->
        if (isPlainText && rawHtmlCache != null) {
            continuation.resume(rawHtmlCache!!)
        } else if (!isReaderModeOn && !isTranslatePage) {
            jsBridge.injectMozReaderModeJs(false)
            jsBridge.getReaderModeBodyHtml(config.display.readerKeepExtraContent, url) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml = processedHtml.substring(1, processedHtml.length - 1)
                rawHtmlCache = rawHtml
                continuation.resume(rawHtml)
            }
        } else {
            evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml = processedHtml.substring(1, processedHtml.length - 1)
                rawHtmlCache = rawHtmlCache ?: rawHtml
                continuation.resume(rawHtml)
            }
        }
    }

    suspend fun getRawText() = suspendCoroutine<String> { continuation ->
        if (dualCaption != null) {
            continuation.resume(DualCaptionProcessor().convertToHtml(dualCaption ?: ""))
        } else if (!isReaderModeOn) {
            jsBridge.evaluateMozReaderModeJs {
                jsBridge.getReaderModeBodyText(config.display.readerKeepExtraContent) { text ->
                    if (text == "null") {
                        continuation.resume("")
                    } else {
                        val processedText = if (text.startsWith("\"") && text.endsWith("\"")) {
                            text.substring(1, text.length - 2)
                        } else text
                        continuation.resume(processedText)
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
                continuation.resume(processedText)
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

    fun applyFontBoldness() = readerHelper.applyFontBoldness()

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
