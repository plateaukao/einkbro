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
import android.util.Base64
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
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.browser.ChatWebInterface
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
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.TranslationTextStyle
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.HelperUnit.loadAssetFile
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.PdfDocumentAdapter
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


open class EBWebView(
    context: Context,
    var browserController: BrowserController?,
) : WebView(context), AlbumController, KoinComponent {
    private var onScrollChangeListener: OnScrollChangeListener? = null
    override val album: Album = Album(this, browserController)
    protected val webViewClient: EBWebViewClient
    private val webChromeClient: EBWebChromeClient
    private val downloadListener: EBDownloadListener = EBDownloadListener(context)
    private val clickHandler: EBClickHandler

    var dualCaption: String? = null
    var shouldHideTranslateContext: Boolean = false

    var baseUrl: String? = null
    protected var isEpubReaderMode = false
    private val cookieManager: CookieManager = CookieManager.getInstance()

    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val javascript: Javascript by inject()
    private val cookie: Cookie by inject()

    var translateApi: TRANSLATE_API = TRANSLATE_API.GOOGLE
    var isTranslateByParagraph = false
    override var isTranslatePage = false
        set(value) {
            field = value
            if (value) {
                album.isTranslatePage = true
            }
        }
    override var isAIPage: Boolean = false

    var isPlainText = false

    var incognito: Boolean = false
        set(value) {
            field = value
            toggleCookieSupport(!value)
        }

    private var isForeground = false

    override fun onScrollChanged(l: Int, t: Int, old_l: Int, old_t: Int) {
        super.onScrollChanged(l, t, old_l, old_t)
        onScrollChangeListener?.onScrollChange(t, old_t)
    }

    fun setScrollChangeListener(onScrollChangeListener: OnScrollChangeListener?) {
        this.onScrollChangeListener = onScrollChangeListener
    }

    fun setOnPageFinishedAction(action: () -> Unit) = webViewClient.setOnPageFinishedAction(action)

    fun updateCssStyle() {
        val fontType = if (shouldUseReaderFont()) config.readerFontType else config.fontType

        val cssStyle =
            (if (config.blackFontStyle) makeTextBlackCss else "") +
                    (if (fontType == FontType.GOOGLE_SERIF) notoSansSerifFontCss else "") +
                    (if (fontType == FontType.TC_IANSUI) iansuiFontCss else "") +
                    (if (fontType == FontType.JA_MINCHO) jaMinchoFontCss else "") +
                    (if (fontType == FontType.KO_GAMJA) koGamjaFontCss else "") +
                    (if (fontType == FontType.SERIF) serifFontCss else "") +
                    (if (config.whiteBackground(url.orEmpty())) whiteBackgroundCss else "") +
                    (if (fontType == FontType.CUSTOM) getCustomFontCss() else "") +
                    (if (config.boldFontStyle)
                        boldFontCss.replace("value", "${config.fontBoldness}") else "") +
                    // all css are purged by epublib. need to add it back if it's epub reader mode
                    if (isEpubReaderMode) loadAssetFile("readerview.css") else ""
        if (cssStyle.isNotBlank()) {
            injectCss(cssStyle.toByteArray())
        }
    }

    private var fontNum = 0
    private fun getCustomFontCss(): String {
        return customFontCss.replace("mycustomfont", "mycustomfont${++fontNum}")
            .replace("fontfamily", "fontfamily${fontNum}")
    }

    private fun resetState(partial: Boolean = false) {
        dualCaption = null
        isTranslatePage = false
        isTranslateByParagraph = false
        browserController?.resetTranslateUI()

        if (!partial) {
            isVerticalRead = false
            isReaderModeOn = false
        }
    }

    override fun reload() {
        resetState()
        settings.textZoom = config.fontSize
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        super.reload()

        postDelayed({
            if (config.webLoadCacheFirst) settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }, 2000)
    }

    override fun goBack() {
        resetState()
        settings.textZoom = config.fontSize
        super.goBack()
    }

    interface OnScrollChangeListener {
        fun onScrollChange(scrollY: Int, oldScrollY: Int)
    }

    init {
        isAIPage = false
        isForeground = false
        webViewClient =
            EBWebViewClient(this) { title, url -> browserController?.addHistory(title, url) }
        webChromeClient = EBWebChromeClient(this) { setAlbumCoverAndSyncDb(it) }
        clickHandler = EBClickHandler(this)
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
        addJavascriptInterface(JsWebInterface(this), "androidApp")
    }

    private fun updateDarkMode() {
        if (config.darkMode == DarkMode.DISABLED) {
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
                config.darkMode == DarkMode.FORCE_ON
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

            textZoom = config.fontSize
            allowFileAccessFromFileURLs = config.enableRemoteAccess
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = config.enableRemoteAccess
            domStorageEnabled = config.enableRemoteAccess
            databaseEnabled = true
            blockNetworkImage = !config.enableImages
            javaScriptEnabled = config.enableJavascript
            javaScriptCanOpenWindowsAutomatically = config.enableJavascript
            setSupportMultipleWindows(config.enableJavascript)
            setGeolocationEnabled(config.shareLocation)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
        val defaultUserAgentString = WebSettings.getDefaultUserAgent(context)
            .replace("wv", "")
            .replace(Regex("Version/\\d+\\.\\d+\\s"), "")
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
        "DNT" to "1"
        "Save-Data" to if (config.enableSaveData) "on" else "off"
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
        if (browserController?.loadInSecondPane(url) == true) {
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

        if (browserController?.loadInSecondPane(processedUrl) == true) {
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
            chatWebInterface = ChatWebInterface(lifecycleScope, this, webContent, webTitle, webUrl)
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
        GlobalScope.launch {
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
            browserController?.updateProgress(progress)
        }
    }

    fun update(title: String?) {
        album.albumTitle = title.orEmpty()
        // so that title on bottom bar can be updated
        browserController?.updateTitle(album.albumTitle)
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
        clickHandler.currentMotionEvent = event
        click.target = clickHandler
        requestFocusNodeHref(click)
    }

    fun isAtTop(): Boolean = if (isVerticalRead) {
        val totalPageCount = computeHorizontalScrollRange() / shiftOffset()
        val currentPage = totalPageCount - (floor(scrollX.toDouble() / shiftOffset()).toInt())
        currentPage == 1
    } else {
        scrollY == 0
    }

    fun jumpToTop() = if (isVerticalRead) {
        scrollTo(computeHorizontalScrollRange() - shiftOffset(), 0)
    } else {
        scrollTo(0, 0)
    }

    fun jumpToBottom() = if (isVerticalRead) {
        scrollTo(computeHorizontalScrollRange() - shiftOffset(), 0)
    } else {
        scrollTo(0, computeVerticalScrollRange() - shiftOffset())
    }

    open fun pageDownWithNoAnimation() = if (isVerticalRead) {
        scrollBy(shiftOffset(), 0)
        scrollX = min(computeHorizontalScrollRange() - width, scrollX)
    } else { // normal case
        val nonNullUrl = url.orEmpty()
        if (config.shouldFixScroll(nonNullUrl) || config.shouldSendPageNavKey(nonNullUrl)) {
            sendPageDownKey()
        } else {
            scrollBy(0, shiftOffset())
            scrollY = min(computeVerticalScrollRange() - shiftOffset(), scrollY)
        }
    }

    open fun pageUpWithNoAnimation() = if (isVerticalRead) {
        scrollBy(-shiftOffset(), 0)
        scrollX = max(0, scrollX)
    } else { // normal case
        val nonNullUrl = url.orEmpty()
        if (config.shouldFixScroll(nonNullUrl) || config.shouldSendPageNavKey(nonNullUrl)) {
            sendPageUpKey()
        } else {
            scrollBy(0, -shiftOffset())
            scrollY = max(0, scrollY)
        }
    }

    fun sendPageDownKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_DOWN)

    fun sendPageUpKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_UP)

    fun removeTextSelection() {
        evaluateJavascript(
            """
            javascript:(function() {
                     var sel = w.getSelection();
                     sel.removeAllRanges();
                 }
            )()
        """.trimIndent()
        ) {}
    }

    var isSelectingText = false
    fun selectLinkText(point: Point) {
        evaluateJavascript(
            """
            javascript:(function() {
                 var tt = w._touchTarget;
                 if(tt){
                     var hrefAttr = tt.getAttribute("href");
                     tt.removeAttribute("href");
                     w._hrefAttr = hrefAttr;
                     
                     var sel = w.getSelection();
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
                        var tt = w._touchTarget;
                        if(tt){
                            tt.setAttribute("href", w._hrefAttr);
                        }
                """.trimIndent(), null
                )
                isSelectingText = false
            }, 1000
        )
    }

    fun updatePageInfo() {
        try {
            val totalPageCount = if (isVerticalRead) {
                computeHorizontalScrollRange() / shiftOffset()
            } else {
                computeVerticalScrollRange() / shiftOffset()
            }
            val info = if (isVerticalRead) {
                "${totalPageCount - (floor(scrollX.toDouble() / shiftOffset()).toInt())}/$totalPageCount"
            } else {
                "${ceil((scrollY + 1).toDouble() / shiftOffset()).toInt()}/$totalPageCount"
            }
            browserController?.updatePageInfo(if (info != "0/0") info else "-/-")
        } catch (e: ArithmeticException) { // prevent divide by zero
            browserController?.updatePageInfo("-/-")
        }
    }

    var rawHtmlCache: String? = null
    suspend fun getRawReaderHtml() = suspendCoroutine { continuation ->
        if (isPlainText && rawHtmlCache != null) {
            continuation.resume(rawHtmlCache!!)
        } else if (!isReaderModeOn && !isTranslatePage) {
            injectMozReaderModeJs(false)
            evaluateJavascript(String.format(getReaderModeBodyHtmlJs, url)) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml =
                    processedHtml.substring(1, processedHtml.length - 1) // handle prefix/postfix
                rawHtmlCache = rawHtml
                continuation.resume(rawHtml)
            }
        } else {
            evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = HelperUnit.unescapeJava(html)
                val rawHtml =
                    processedHtml.substring(1, processedHtml.length - 1) // handle prefix/postfix
                // keep html cache when it's still null
                rawHtmlCache = rawHtmlCache ?: rawHtml
                continuation.resume(rawHtml)
            }
        }
    }

    // only works in isReadModeOn
    suspend fun getRawText() = suspendCoroutine<String> { continuation ->
        if (dualCaption != null) {
            continuation.resume(DualCaptionProcessor().convertToHtml(dualCaption ?: ""))
        } else if (!isReaderModeOn) {
            evaluateMozReaderModeJs {
                evaluateJavascript(getReaderModeBodyTextJs) { text ->
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

    protected fun shiftOffset(): Int {
        return if (isVerticalRead) {
            width - 40.dp(context)
        } else {
            val offset = config.pageReservedOffsetInString
            if (offset.endsWith('%')) {
                var offsetPercent = offset.take(offset.length - 1).toInt();
                height - height * offsetPercent / 100;
            } else {
                height - offset.toInt().dp(context);
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (browserController?.handleKeyEvent(event) == true) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }

    var isVerticalRead = false
    fun toggleVerticalRead() {
        isVerticalRead = !isVerticalRead
        if (isVerticalRead) {
            toggleReaderMode(true)
        } else {
            reload()
        }
    }

    fun shouldUseReaderFont(): Boolean = isReaderModeOn || isTranslatePage

    var isReaderModeOn = false
    fun toggleReaderMode(
        isVertical: Boolean = false,
    ) {
        isReaderModeOn = !isReaderModeOn
        if (isReaderModeOn) {
            evaluateMozReaderModeJs(isVertical) {
                evaluateJavascript(
                    "(function() { ${
                        String.format(replaceWithReaderModeBodyJs, url)
                    } })();"
                ) { _ ->
                    if (isVertical) {
                        evaluateJsFile("process_text_nodes.js", false) {
                            // need to wait for a while to jump to top, so that vertical read starts from beginning
                            postDelayed({ jumpToTop() }, 200)
                        }
                    } else {
                        // add padding
                        setPaddingInReaderMode(config.paddingForReaderMode)
                    }
                }
            }
            settings.textZoom = config.readerFontSize
            updateCssStyle()
        } else {
            disableReaderMode(isVertical)
            settings.textZoom = config.fontSize
        }
    }

    private fun setPaddingInReaderMode(padding: Int) {
        evaluateJavascript("javascript:setPadding($padding)", null)
    }

    fun clearTranslationElements() {
        evaluateJavascript(clearTranslationElementsJs, null)
    }

    fun translateByParagraphInPlace() {
        val textBlockStyle = when (config.translationTextStyle) {
            TranslationTextStyle.NONE -> translatedPCssNone
            TranslationTextStyle.DASHED_BORDER -> translatedPCssDashedBorder
            TranslationTextStyle.VERTICAL_LINE -> translatedPCssVerticalLine
            TranslationTextStyle.GRAY -> translatedPCssGray
            TranslationTextStyle.BOLD -> translatedPCssBold
        }
        injectCss(textBlockStyle.toByteArray())
        evaluateJsFile("translate_by_paragraph.js") {
            evaluateJsFile("text_node_monitor.js", false)
            isTranslateByParagraph = true
        }
    }

    fun showTranslation() = browserController?.showTranslation(this)

    fun addSelectionChangeListener() = evaluateJsFile("text_selection_change.js")

    private var isHighlightCssInjected = false
    fun highlightTextSelection(highlightStyle: HighlightStyle) {
        if (!isHighlightCssInjected) {
            injectCss(loadAssetFile("highlight.css").toByteArray())
            isHighlightCssInjected = true
        }

        val className = when (highlightStyle) {
            HighlightStyle.UNDERLINE -> "highlight_underline"
            HighlightStyle.BACKGROUND_YELLOW -> "highlight_yellow"
            HighlightStyle.BACKGROUND_GREEN -> "highlight_green"
            HighlightStyle.BACKGROUND_BLUE -> "highlight_blue"
            HighlightStyle.BACKGROUND_PINK -> "highlight_pink"
            else -> ""
        }

        evaluateJavascript(
            String.format(loadAssetFile("text_selection_highlight.js"), className).wrapJsFunction(), null
        )
    }

    private fun disableReaderMode(isVertical: Boolean = false) {
        val verticalCssString = if (isVertical) {
            "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = \"" + horizontalLayoutCss + "\";" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "parent.appendChild(style);"
        } else {
            ""
        }

        evaluateJavascript(
            "javascript:(function() {" +
                    "document.body.innerHTML = document.innerHTMLCache;" +
                    "document.body.classList.remove(\"mozac-readerview-body\");" +
                    verticalCssString +
                    "window.scrollTo(0, 0);" +
                    "})()", null
        )
    }

    private fun evaluateMozReaderModeJs(
        isVertical: Boolean = false,
        postAction: (() -> Unit)? = null,
    ) {
        val cssByteArray =
            loadAssetFile(if (isVertical) "verticalReaderview.css" else "readerview.css").toByteArray()
        injectCss(cssByteArray)
        if (isVertical) injectCss(verticalLayoutCss.toByteArray())

        val jsString = HelperUnit.getStringFromAsset("MozReadability.js")
        evaluateJavascript(jsString) {
            postAction?.invoke()
        }
    }

    private fun injectMozReaderModeJs(isVertical: Boolean = false) {
        try {
            val buffer = loadAssetFile("MozReadability.js").toByteArray()
            val cssBuffer =
                loadAssetFile(if (isVertical) "verticalReaderview.css" else "readerview.css").toByteArray()

            val verticalCssString = if (isVertical) {
                "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = \"" + verticalLayoutCss + "\";" +
                        "parent.appendChild(style);"
            } else {
                ""
            }


            // String-ify the script byte-array using BASE64 encoding !!!
            val encodedJs = Base64.encodeToString(buffer, Base64.NO_WRAP)
            val encodedCss = Base64.encodeToString(cssBuffer, Base64.NO_WRAP)
            evaluateJavascript(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'text/javascript';" +
                        "script.innerHTML = window.atob('" + encodedJs + "');" +
                        "parent.appendChild(script);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encodedCss + "');" +
                        "parent.appendChild(style);" +
                        verticalCssString +
                        "window.scrollTo(0, 0);" +
                        "})()", null
            )

        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun hideTranslateContext() {
        when (config.translationMode) {
            TranslationMode.GOOGLE ->
                evaluateJavascript(hideGTranslateContext, null)

            TranslationMode.GOOGLE_URL ->
                evaluateJavascript(hideGUrlTranslateContext, null)

            TranslationMode.PAPAGO ->
                evaluateJavascript(hidePTranslateContext, null)

            else -> Unit
        }
    }

    private fun injectCss(bytes: ByteArray) {
        try {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            loadUrl(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encoded + "');" +
                        "parent.appendChild(style)" +
                        "})()"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSelectedText(): String = suspendCoroutine { continuation ->
        val js = "window.getSelection().toString();"
        evaluateJavascript(js) { value ->
            continuation.resume(value.substring(1, value.length - 1))
        }
    }

    fun selectSentence(point: Point) =
        evaluateJsFile("select_sentence.js") { this.postDelayed({ simulateClick(point) }, 100) }

    fun selectParagraph(point: Point) =
        evaluateJsFile("select_paragraph.js") { this.postDelayed({ simulateClick(point) }, 100) }

    suspend fun getSelectedTextWithContext(contextLength: Int = 10): String =
        suspendCoroutine { continuation ->
            evaluateJsFile("get_selected_text_with_context.js") { value ->
                continuation.resume(value.substring(1, value.length - 1))
            }
        }

    fun addGoogleTranslation() {
        val str = injectGoogleTranslateV2Js()
        evaluateJavascript(str, null)
    }

    private fun injectGoogleTranslateV2Js(): String =
        String.format(
            injectGoogleTranslateV2JsFormat,
            if (config.preferredTranslateLanguageString.isNotEmpty()) "includedLanguages: '${config.preferredTranslateLanguageString}',"
            else ""
        )

    private fun sendKeyEventToView(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        dispatchKeyEvent(downEvent)

        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        dispatchKeyEvent(upEvent)
    }

    fun evaluateJsFile(fileName: String, withPrefix: Boolean = true, callback: ValueCallback<String>? = null) {
        val jsContent = loadAssetFile(fileName)
        if (withPrefix) {
            evaluateJavascript(jsContent.wrapJsFunction(), callback)
        } else {
            evaluateJavascript(jsContent, callback)
        }
    }

    companion object {
        private const val FAKE_PRE_PROGRESS = 5


        // make a String extension to wrap it with Javascript function
        private fun String.wrapJsFunction(): String {
            return "javascript:(function() { $this })()"
        }

        private const val secondPart =
            """setTimeout(
                    function() { 
                          var css=document.createElement('style');
                          css.type='text/css';
                          css.charset='UTF-8';
                          css.appendChild(document.createTextNode('.goog-te-combo, .goog-te-banner *, .goog-te-ftab *, .goog-te-menu *, .goog-te-menu2 *, .goog-te-balloon * {font-size: 8pt !important;}'));
                          var teef=document.getElementById(':0.container');
                          if(teef){
                              teef.contentDocument.head.appendChild(css);
                          }
                    }, 
                    1000);"""

        private const val injectGoogleTranslateV2JsFormat =
            "!function(){!function(){function e(){" +
                    "window.setTimeout(" +
                    "function(){window[t].showBanner(!0)},10)}" +
                    "function n(){" +
                    "return new google.translate.TranslateElement({" +
                    "autoDisplay:!1,floatPosition:0,%s pageLanguage:'auto'" +
                    "})}" +
                    "var t=(document.documentElement.lang,'TE_7777'),o='TECB_7777';" +
                    "if(window[t])e();" +
                    "else if(!window.google||!google.translate||!google.translate.TranslateElement){window[o]||(window[o]=function(){window[t]=n(),e()});" +
                    "var a=document.createElement('script');a.src='https://translate.google.com/translate_a/element.js?cb='+encodeURIComponent(o)+'&client=tee',document.getElementsByTagName('head')[0].appendChild(a);$secondPart}}()}();"

        private const val hidePTranslateContext = """
            javascript:(function() {
                // document.getElementById("sourceEditArea").style.display="none";
                document.getElementById("targetEditArea").scrollIntoView();
            })()
        """
        private const val hideGTranslateContext = """
            javascript:(function() {
                document.getElementsByTagName("header")[0].remove();
                document.querySelector("span[lang]").style.display = "none";
                document.querySelector("div[data-location]").style.display = "none";
            })()
            """
        private const val hideGUrlTranslateContext = """
            javascript:(function() {
                document.querySelector('#gt-nvframe').style = "height:0px";
            })()
            """

        private const val translatedPCssNone = """
            .translated {
                padding: 5px; 
                display: inline-block; 
                line-height: 1.5;
                max-width: 100vw;
            }
        """

        private const val translatedPCssGray = """
            .translated {
                color: gray;
                padding: 5px; 
                display: inline-block; 
                max-width: 100vw;
                line-height: 1.5;
            }
        """

        private const val translatedPCssBold = """
            .translated {
                font-weight: bold;
                padding: 5px; 
                display: inline-block; 
                max-width: 100vw;
                line-height: 1.5;
            }
        """

        private const val translatedPCssDashedBorder = """
            .translated {
                border: 1px dashed lightgray; 
                padding: 5px; 
                display: inline-block; 
                position: relative;
                max-width: 100vw;
                line-height: 1.5;
            }
        """
        private const val translatedPCssVerticalLine = """
            .translated {
                padding: 2px; 
                margin-left: 7px;
                display: inline-block; 
                position: relative;
                max-width: 100vw;
                line-height: 1.5;
            }
            .translated::before {
            content: '';
            display: inline-block;
            width: 2px;
            height: 90%;
            background-color: black;
            position: absolute;
            left: -7px;
          }
        """

        private const val readabilityOptions =
            "{classesToPreserve: preservedClasses, overwriteImgSrc: true}"

        private const val replaceWithReaderModeBodyJs = """
            var documentClone = document.cloneNode(true);
            var article = new Readability(documentClone, $readabilityOptions).parse();
            document.innerHTMLCache = document.body.innerHTML;

            article.readingTime = getReadingTime(article.length, document.documentElement.lang.substring(0, 2));

            document.body.outerHTML = createHtmlBody(article)

            document.getElementsByName('viewport')[0].setAttribute('content', 'width=device-width');
        """

        private const val getReaderModeBodyHtmlJs = """
            javascript:(function() {
                var documentClone = document.cloneNode(true);
                var article = new Readability(documentClone, $readabilityOptions).parse();
                article.readingTime = getReadingTime(article.length, document.documentElement.lang.substring(0, 2));
                var bodyOuterHTML = createHtmlBodyWithUrl(article, "%s")
                var headOuterHTML = document.head.outerHTML;
                return ('<html>'+ headOuterHTML + bodyOuterHTML +'</html>');
            })()
        """
        private const val getReaderModeBodyTextJs = """
            javascript:(function() {
                var documentClone = document.cloneNode(true);
                var article = new Readability(documentClone, $readabilityOptions).parse();
                return article.title + ', ' + article.textContent;
            })()
        """

        private const val verticalLayoutCss = "body {\n" +
                "-webkit-writing-mode: vertical-rl;\n" +
                "writing-mode: vertical-rl;\n" +
                "}\n" +
                "img {\n" +
                "margin: 10px 10px 10px 10px;\n" +
                "float: left;\n" +
                "display: block;\n" +
                "}\n"

        private const val horizontalLayoutCss = "body {\n" +
                "-webkit-writing-mode: horizontal-tb;\n" +
                "writing-mode: horizontal-tb;\n" +
                "}\n"

        private const val notoSansSerifFontCss =
            "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+TC:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Noto Serif TC', 'Noto Serif JP', 'Noto Serif KR', 'Noto Serif SC', serif !important;\n" +
                    "}\n"
        private const val iansuiFontCss =
            "@import url('https://fonts.googleapis.com/css2?family=BIZ+UDPMincho&family=Iansui&display=swap');" +
                    "* {\n" +
                    "font-family: 'Iansui',serif !important;\n" +
                    "}\n"
        private const val jaMinchoFontCss =
            "@import url('https://fonts.googleapis.com/css2?family=Shippori+Mincho:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Shippori Mincho',serif !important;\n" +
                    "}\n"
        private const val koGamjaFontCss =
            "@import url('https://fonts.googleapis.com/css2?family=Gamja+Flower:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Gamja Flower',serif !important;\n" +
                    "}\n"
        private const val serifFontCss =
            "* {\n" +
                    "font-family: serif !important;\n" +
                    "}\n"

        private const val customFontCss = """
            @font-face {
                 font-family: fontfamily;
                 font-weight: 400;
                 font-display: swap;
                 src: url('mycustomfont');
            }
            html body * {
              font-family: fontfamily, serif, popular-symbols, lite-glyphs-outlined, lite-glyphs-filled, snaptu-symbols !important;
            }
        """

        private const val whiteBackgroundCss = """
* {
    color: #000000!important;
    border-color: #555555 !important;
    background-color: #FFFFFF !important;
}
input,select,option,button,textarea {
	border: #FFFFFF !important;
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input[type=button],input[type=submit],input[type=reset],input[type=image] {
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input[type=button]: focus,input[type=submit]: focus,input[type=reset]: focus,input[type=image]: focus, input[type=button]: hover,input[type=submit]: hover,input[type=reset]: hover,input[type=image]: hover {
	background: #FFFFFF !important;
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
        """

        private const val makeTextBlackCss = """
            * {
                color: #000000 !important;
            }
            a, a * { 
                color: #000000 !important;
            }
            input,select,option,button,textarea {
                color: #000000 !important;
            }
            input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {
                color: #000000 !important;
            }
            input[type=button],input[type=submit],input[type=reset],input[type=image] {
                color: #000000 !important;
            }
        """

        private const val boldFontCss = "* {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a,a * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a: visited,a: visited *,a: active,a: active * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a: hover,a: hover * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input,select,option,button,textarea {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input[type=button]: focus,input[type=submit]: focus,input[type=reset]: focus,input[type=image]: focus, input[type=button]: hover,input[type=submit]: hover,input[type=reset]: hover,input[type=image]: hover {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n"

        private val clearTranslationElementsJs = """
            javascript:(function() {
                document.body.innerHTML = document.originalInnerHTML;
                document.body.classList.remove("translated");
            })()
        """.trimIndent()
    }

    init {
        initAlbum()
    }

    fun applyFontBoldness() {
        val fontCss = boldFontCss.replace("value", config.fontBoldness.toString())
        injectCss(fontCss.toByteArray())
    }
}