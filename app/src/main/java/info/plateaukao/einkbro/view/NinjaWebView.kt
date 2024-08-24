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
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.browser.JsWebInterface
import info.plateaukao.einkbro.browser.NinjaClickHandler
import info.plateaukao.einkbro.browser.NinjaDownloadListener
import info.plateaukao.einkbro.browser.NinjaWebChromeClient
import info.plateaukao.einkbro.browser.NinjaWebViewClient
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.PdfDocumentAdapter
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


open class NinjaWebView(
    context: Context,
    var browserController: BrowserController?,
) : WebView(context), AlbumController, KoinComponent {
    private var onScrollChangeListener: OnScrollChangeListener? = null
    override val album: Album = Album(this, browserController)
    protected val webViewClient: NinjaWebViewClient
    private val webChromeClient: NinjaWebChromeClient
    private val downloadListener: NinjaDownloadListener = NinjaDownloadListener(context)
    private val clickHandler: NinjaClickHandler

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
    override var isTranslatePage = false
        set(value) {
            field = value
            if (value) {
                album.isTranslatePage = true
            }
        }

    var isPlainText = false

    var incognito: Boolean = false
        set(value) {
            field = value
            toggleCookieSupport(!incognito)
        }

    private var isForeground = false

    override fun onScrollChanged(l: Int, t: Int, old_l: Int, old_t: Int) {
        super.onScrollChanged(l, t, old_l, old_t)
        onScrollChangeListener?.onScrollChange(t, old_t)
    }

    fun setScrollChangeListener(onScrollChangeListener: OnScrollChangeListener?) {
        this.onScrollChangeListener = onScrollChangeListener
    }

    fun updateCssStyle() {
        val fontType = if (shouldUseReaderFont()) config.readerFontType else config.fontType

        val cssStyle =
            (if (config.blackFontStyle) makeTextBlackCss else "") +
                    (if (fontType == FontType.GOOGLE_SERIF) notoSansSerifFontCss else "") +
                    (if (fontType == FontType.TC_WENKAI) wenKaiFontCss else "") +
                    (if (fontType == FontType.JA_MINCHO) jaMinchoFontCss else "") +
                    (if (fontType == FontType.KO_GAMJA) koGamjaFontCss else "") +
                    (if (fontType == FontType.SERIF) serifFontCss else "") +
                    (if (config.whiteBackground(url.orEmpty())) whiteBackgroundCss else "") +
                    (if (fontType == FontType.CUSTOM) getCustomFontCss() else "") +
                    (if (config.boldFontStyle)
                        boldFontCss.replace("value", "${config.fontBoldness}") else "") +
                    // all css are purged by epublib. need to add it back if it's epub reader mode
                    if (isEpubReaderMode) String(
                        getByteArrayFromAsset("readerview.css"),
                        Charsets.UTF_8
                    ) else ""
        if (cssStyle.isNotBlank()) {
            injectCss(cssStyle.toByteArray())
        }
    }

    private var fontNum = 0
    private fun getCustomFontCss(): String {
        return customFontCss.replace("mycustomfont", "mycustomfont${++fontNum}")
            .replace("fontfamily", "fontfamily${fontNum}")
    }

    override fun reload() {
        isTranslatePage = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        isVerticalRead = false
        isReaderModeOn = false
        settings.textZoom = config.fontSize
        super.reload()
    }

    override fun goBack() {
        isTranslatePage = false
        isVerticalRead = false
        isReaderModeOn = false
        settings.textZoom = config.fontSize
        super.goBack()
    }

    interface OnScrollChangeListener {
        fun onScrollChange(scrollY: Int, oldScrollY: Int)
    }

    init {
        isForeground = false
        webViewClient =
            NinjaWebViewClient(this) { title, url -> browserController?.addHistory(title, url) }
        webChromeClient = NinjaWebChromeClient(this) { setAlbumCoverAndSyncDb(it) }
        clickHandler = NinjaClickHandler(this)
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

        with(settings) {
            // don't load cache by default, so that it won't cause some issues
            //cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
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
            setAcceptThirdPartyCookies(this@NinjaWebView, isEnabled)
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

        dualCaption = null
        isTranslatePage = false
        browserController?.resetTranslateUI()

        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            setAlbumCover(it)
        }

        settings.javaScriptEnabled = config.enableJavascript || javascript.isWhite(url)
        toggleCookieSupport(config.cookies || cookie.isWhite(url))

        super.loadUrl(url, additionalHttpHeaders)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(url: String) {
        dualCaption = null
        album.isLoaded = true

        isTranslatePage = false
        browserController?.resetTranslateUI()

        if (url.startsWith("javascript:") || url.startsWith("content:")) {
            // Daniel
            super.loadUrl(url)
            return
        }

        val processedUrl = url.trim { it <= ' ' }
        if (processedUrl.isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error)
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

    fun handlePocketRequestToken(requestToken: String) {
        browserController?.handlePocketRequestToken(requestToken)
    }

    fun setAlbumCover(bitmap: Bitmap) = album.setAlbumCover(bitmap)

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
        scrollX == 0
    } else {
        scrollY == 0
    }

    fun jumpToTop() = scrollTo(0, 0)

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
            callScrollFixPageDown()
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
            callScrollFixPageUp()
        } else {
            scrollBy(0, -shiftOffset())
            scrollY = max(0, scrollY)
        }
    }

    private fun callScrollFixPageDown() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_DOWN)

    private fun callScrollFixPageUp() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_UP)

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
            val info = if (isVerticalRead) {
                "${ceil((scrollX + 1).toDouble() / shiftOffset()).toInt()}/${computeHorizontalScrollRange() / shiftOffset()}"
            } else {
                "${ceil((scrollY + 1).toDouble() / shiftOffset()).toInt()}/${computeVerticalScrollRange() / shiftOffset()}"
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
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                val rawHtml =
                    processedHtml.substring(1, processedHtml.length - 1) // handle prefix/postfix
                rawHtmlCache = rawHtml
                continuation.resume(rawHtml)
            }
        } else {
            evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                val rawHtml =
                    processedHtml.substring(1, processedHtml.length - 1) // handle prefix/postfix
                // keep html cache when it's still null
                rawHtmlCache = rawHtmlCache ?: rawHtml
                continuation.resume(rawHtml)
            }
        }
    }

    suspend fun getRawHtml() = suspendCoroutine { continuation ->
        if (rawHtmlCache != null) {
            continuation.resume(rawHtmlCache)
            return@suspendCoroutine
        }

        evaluateJavascript(
            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
        ) { html ->
            val processedHtml = StringEscapeUtils.unescapeJava(html)
            val rawHtml =
                processedHtml.substring(1, processedHtml.length - 1) // handle prefix/postfix
            rawHtmlCache = rawHtml
            continuation.resume(rawHtml)
        }
    }

    // only works in isReadModeOn
    suspend fun getRawText() = suspendCoroutine<String> { continuation ->
        if (!isReaderModeOn) {
            evaluateMozReaderModeJs {
                evaluateJavascript(getReaderModeBodyTextJs) { text ->
                    continuation.resume(
                        text.substring(
                            1,
                            text.length - 2
                        )
                    )
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
        getRawTextAction: ((String) -> Unit)? = null,
    ) {
        isReaderModeOn = !isReaderModeOn
        if (isReaderModeOn) {
            evaluateMozReaderModeJs(isVertical) {
                val getRawTextJs =
                    if (getRawTextAction != null) " return document.getElementsByTagName('html')[0].innerText; " else ""
                evaluateJavascript(
                    "(function() { ${
                        String.format(
                            replaceWithReaderModeBodyJs,
                            url
                        )
                    } $getRawTextJs })();",
                    getRawTextAction
                )
            }
            settings.textZoom = config.readerFontSize
            updateCssStyle()
        } else {
            disableReaderMode(isVertical)
            settings.textZoom = config.fontSize
        }
    }

    fun clearTranslationElements() {
        evaluateJavascript(clearTranslationElementsJs, null)
    }

    fun translateByParagraphInPlace() {
        evaluateJavascript(translateParagraphJs) {
            evaluateJavascript(textNodesMonitorJs, null)
        }
    }

    fun showTranslation() = browserController?.showTranslation()

    fun addSelectionChangeListener() {
        evaluateJavascript(textSelectionChangeJs, null)
    }

    private var isHighlightCssInjected = false
    fun highlightTextSelection(highlightStyle: HighlightStyle) {
        if (!isHighlightCssInjected) {
            injectCss(getByteArrayFromAsset("highlight.css"))
            isHighlightCssInjected = true
        }

        if (highlightStyle == HighlightStyle.BACKGROUND_NONE) {
            evaluateJavascript(removeHighlightJs, null)
        } else {
            val className = when (highlightStyle) {
                HighlightStyle.UNDERLINE -> "highlight_underline"
                HighlightStyle.BACKGROUND_YELLOW -> "highlight_yellow"
                HighlightStyle.BACKGROUND_GREEN -> "highlight_green"
                HighlightStyle.BACKGROUND_BLUE -> "highlight_blue"
                HighlightStyle.BACKGROUND_PINK -> "highlight_pink"
                else -> ""
            }

            evaluateJavascript(String.format(selectionHighlightJs, className), null)
        }
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
            getByteArrayFromAsset(if (isVertical) "verticalReaderview.css" else "readerview.css")
        injectCss(cssByteArray)
        if (isVertical) injectCss(verticalLayoutCss.toByteArray())

        val jsString = getStringFromAsset("MozReadability.js")
        evaluateJavascript(jsString) {
            evaluateJavascript("javascript:(function() { window.scrollTo(0, 0); })()", null)
            postAction?.invoke()
        }
    }

    private fun injectMozReaderModeJs(isVertical: Boolean = false) {
        try {
            val buffer = getByteArrayFromAsset("MozReadability.js")
            val cssBuffer =
                getByteArrayFromAsset(if (isVertical) "verticalReaderview.css" else "readerview.css")

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

    private fun getByteArrayFromAsset(fileName: String): ByteArray {
        return try {
            val assetInput: InputStream = context.assets.open(fileName)
            val buffer = ByteArray(assetInput.available())
            assetInput.read(buffer)
            assetInput.close()

            buffer
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            ByteArray(0)
        }
    }

    private fun getStringFromAsset(fileName: String): String =
        context.assets.open(fileName).bufferedReader().use { it.readText() }

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

    fun selectSentence(point: Point) {
        evaluateJavascript(jsSelectSentence) {
            this.postDelayed({ simulateClick(point) }, 100)
        }
    }

    fun selectParagraph(point: Point) {
        evaluateJavascript(jsSelectParagraph) {
            this.postDelayed({ simulateClick(point) }, 100)
        }
    }

    suspend fun getSelectedTextWithContext(contextLength: Int = 10): String =
        suspendCoroutine { continuation ->
            evaluateJavascript(jsGetSelectedTextWithContextV2) { value ->
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

    companion object {
        private const val FAKE_PRE_PROGRESS = 5

        private const val jsSelectParagraph = """
            javascript:(function () {
    let selection = window.getSelection();
    if (selection.rangeCount === 0) return;

    let range = selection.getRangeAt(0);
    let startContainer = range.startContainer;
    let endContainer = range.endContainer;

    // Check if the selection is within a single text node
    if (startContainer !== endContainer || startContainer.nodeType !== Node.TEXT_NODE) {
        return;
    }

    let textContent = startContainer.textContent;
    let startOffset = range.startOffset;
    let endOffset = range.endOffset;

    let paragraphStart = startOffset;
    let paragraphEnd = endOffset;

    // Move the start of the range to the start of the paragraph (i.e., look for newline or start of the node)
    while (paragraphStart > 0 && textContent[paragraphStart - 1] !== '\n') {
        paragraphStart--;
    }

    // Move the end of the range to the end of the paragraph (i.e., look for newline or end of the node)
    while (paragraphEnd < textContent.length && textContent[paragraphEnd] !== '\n') {
        paragraphEnd++;
    }

    // Set the range to the paragraph boundaries
    range.setStart(startContainer, paragraphStart);
    range.setEnd(startContainer, paragraphEnd);

    // Clear previous selection and set the new one
    selection.removeAllRanges();
    selection.addRange(range);
})();
        """
        private const val jsSelectSentence = """
            javascript:(function () {
    let selection = window.getSelection();
    if (selection.rangeCount === 0) return;

    let range = selection.getRangeAt(0);
    let startContainer = range.startContainer;
    let endContainer = range.endContainer;

    if (startContainer !== endContainer || startContainer.nodeType !== Node.TEXT_NODE) {
        // Only handle cases where the selection is within a single text node
        return;
    }

    let textContent = startContainer.textContent;
    let startOffset = range.startOffset;
    let endOffset = range.endOffset;

    let sentenceStart = startOffset;
    let sentenceEnd = endOffset;

    // Move the start of the range to the start of the sentence
    while (sentenceStart > 0 && ![".", "?", "。", "!"].includes(textContent[sentenceStart - 1])) {
        sentenceStart--;
    }

    // Move the end of the range to the end of the sentence
    while (sentenceEnd < textContent.length && ![".", "?", "。", "!"].includes(textContent[sentenceEnd])) {
        sentenceEnd++;
    }

    // Set the range to the sentence boundaries
    range.setStart(startContainer, sentenceStart);
    range.setEnd(startContainer, sentenceEnd);

    // Clear previous selection and set the new one
    selection.removeAllRanges();
    selection.addRange(range);
            })();
        """

        private const val jsGetSelectedTextWithContextV2 = """
            javascript:(function() {
    let contextLength = 120;
    let selection = window.getSelection();
    if (selection.rangeCount === 0) return "";

    let range = selection.getRangeAt(0);
    let startContainer = range.startContainer;
    let endContainer = range.endContainer;

    // Handle the case where the selected text spans multiple nodes
    if (startContainer !== endContainer) {
        return "";  // For simplicity, not handling multi-node selections here
    }

    let textContent = startContainer.textContent;
    let startOffset = range.startOffset;
    let endOffset = range.endOffset;

    // Extend previousContext to the previous ".", "。", "?", or "!"
    let contextStartPos = startOffset;
    while (contextStartPos > 0 && ![".", "。", "?", "!"].includes(textContent[contextStartPos - 1])) {
        contextStartPos--;
        if (startOffset - contextStartPos > contextLength) {
            break;
        }
    }

    // Extend nextContext to the next ".", "?", or "。"
    let contextEndPos = endOffset;
    while (contextEndPos < textContent.length && ![".", "?", "。"].includes(textContent[contextEndPos])) {
        contextEndPos++;
        if (contextEndPos - endOffset > contextLength) {
            break;
        }
    }

    let previousContext = textContent.substring(contextStartPos, startOffset);
    let nextContext = textContent.substring(endOffset, contextEndPos+1);

    let selectedText = selection.toString();
    return previousContext + "<<" + selectedText + ">>" + nextContext;
})();
        """

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

        const val textNodesMonitorJs = """
            //const bridge = window.android = new androidApp(context, webView);
            
            function myCallback(elementId, responseString) {
                //console.log("Element ID:", elementId, "Response string:", responseString);
                node = document.getElementById(elementId).nextElementSibling;
                node.textContent = responseString;
                node.style = "border: 1px dashed lightgray; padding: 5px; display: inline-block"
            }
            
            // Create a new IntersectionObserver object
            observer = new IntersectionObserver((entries) => {
              entries.forEach((entry) => {
                // Check if the target node is currently visible
                if (entry.isIntersecting) {
                  //console.log('Node is visible:', entry.target.textContent);
                  const nextNode = entry.target.nextElementSibling;
                          //nextNode.textContent = result;
                  if (nextNode && nextNode.textContent === "") {
                      androidApp.getTranslation(entry.target.textContent, entry.target.id, "myCallback");
                  }
                } else {
                  // The target node is not visible
                  //console.log('Node is not visible');
                }
              });
            });

            // Select all elements with class name 'to-translate'
            targetNodes = document.querySelectorAll('.to-translate');

            // Loop through each target node and start observing it
            targetNodes.forEach((targetNode) => {
              observer.observe(targetNode);
            });
        """
        private const val replaceWithReaderModeBodyJs = """
            var documentClone = document.cloneNode(true);
            var article = new Readability(documentClone, {classesToPreserve: preservedClasses}).parse();
            document.innerHTMLCache = document.body.innerHTML;

            article.readingTime = getReadingTime(article.length, document.lang);

            document.body.outerHTML = createHtmlBody(article)

            document.getElementsByName('viewport')[0].setAttribute('content', 'width=device-width');
        """

        private const val getReaderModeBodyHtmlJs = """
            javascript:(function() {
                var documentClone = document.cloneNode(true);
                var article = new Readability(documentClone, {classesToPreserve: preservedClasses}).parse();
                article.readingTime = getReadingTime(article.length, document.lang);
                var bodyOuterHTML = createHtmlBodyWithUrl(article, "%s")
                var headOuterHTML = document.head.outerHTML;
                return ('<html>'+ headOuterHTML + bodyOuterHTML +'</html>');
            })()
        """
        private const val getReaderModeBodyTextJs = """
            javascript:(function() {
                var documentClone = document.cloneNode(true);
                var article = new Readability(documentClone, {classesToPreserve: preservedClasses}).parse();
                return article.title + ', ' + article.textContent;
            })()
        """
        private const val stripHeaderElementsJs = """
            javascript:(function() {
                var r = document.getElementsByTagName('script');
                for (var i = (r.length-1); i >= 0; i--) {
                    if(r[i].getAttribute('id') != 'a'){
                        r[i].parentNode.removeChild(r[i]);
                    }
                }
            })()
        """

        private const val oldFacebookHideSponsoredPostsJs = """
            javascript:(function() {
            var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0); 
            while(posts.length > 0) { posts.pop().style.display = "none"; }
            
            var qcleanObserver = new window.MutationObserver(function(mutation, observer){ 
               var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0); 
               while(posts.length > 0) { posts.pop().style.display = "none"; }
            });
            
            qcleanObserver.observe(document, { subtree: true, childList: true });
            })()
        """

        private const val verticalLayoutCss = "body {\n" +
                "-webkit-writing-mode: vertical-rl;\n" +
                "writing-mode: vertical-rl;\n" +
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
        private const val wenKaiFontCss =
            "@import url('https://fonts.googleapis.com/css2?family=LXGW+WenKai+TC:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'LXGW WenKai TC',serif !important;\n" +
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
            * {
              font-family: fontfamily !important;
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

        private const val textSelectionChangeJs = """
            var selectedText = "";
            function getSelectionPositionInWebView() {
    let selection = window.getSelection();
    
    if (selection) {
        let range = selection.getRangeAt(0);
        let startNode = range.startContainer;
        let startOffset = range.startOffset;
        let endNode = range.endContainer;
        let endOffset = range.endOffset;
        
        let start = getRectInWebView(startNode, startOffset);
        let end = getRectInWebView(endNode, endOffset);
        
            // Send anchor position to Android
            if (selection.toString() != selectedText) {
                selectedText = selection.toString();
                if (selectedText.length > 0) {
                    androidApp.getAnchorPosition(start.left, start.top, end.right, end.bottom);
                }
            }
    }
}

function getRectInWebView(node, offset) {
    let range = document.createRange();
    range.setStart(node, offset);
    range.setEnd(node, offset);
    let rect = range.getBoundingClientRect();

    return rect;
}

// Call the function to get selection position
document.addEventListener("selectionchange", function() {
    getSelectionPositionInWebView();
    });
        """

        private const val removeHighlightJs = """
            javascript:(function() {
            function removeHighlightFromSelection() {
    const selection = window.getSelection();
    // 檢查是否有選取範圍
    if (!selection.rangeCount) return;
    const range = selection.getRangeAt(0);
    const container = range.commonAncestorContainer;
    // 確保範圍是在一個元素內部
    const parentElement = container.nodeType === 3 ? container.parentNode : container;

    // 查找所有的 highlight divs
    const highlights = parentElement.parentNode.querySelectorAll('div.highlight_underline, div.highlight_yellow, div.highlight_green, div.highlight_blue, div.highlight_pink');

    // 移除每個 highlight div 的外部 HTML
    highlights.forEach(highlight => {
        highlight.outerHTML = highlight.innerHTML;
    });
}

// 綁定一個按鈕來觸發這個函數
removeHighlightFromSelection();
            })()
        """

        private const val selectionHighlightJs = """
            javascript:(function() {
                function highlightSelection() {
  var userSelection = window.getSelection().getRangeAt(0);
  var safeRanges = getSafeRanges(userSelection);
  for (var i = 0; i < safeRanges.length; i++) {
    highlightRange(safeRanges[i]);
  }
}

function highlightRange(range) {
  var newNode = document.createElement("div");
  newNode.className = "%s"
  range.surroundContents(newNode);
}

function getSafeRanges(dangerous) {
  var a = dangerous.commonAncestorContainer;
  // Starts -- Work inward from the start, selecting the largest safe range
  var s = new Array(0), rs = new Array(0);
  if (dangerous.startContainer != a) {
    for (var i = dangerous.startContainer; i != a; i = i.parentNode) {
      s.push(i);
    }
  }
  if (s.length > 0) {
    for (var i = 0; i < s.length; i++) {
      var xs = document.createRange();
      if (i) {
        xs.setStartAfter(s[i - 1]);
        xs.setEndAfter(s[i].lastChild);
      } else {
        xs.setStart(s[i], dangerous.startOffset);
        xs.setEndAfter((s[i].nodeType == Node.TEXT_NODE) ? s[i] : s[i].lastChild);
      }
      rs.push(xs);
    }
  }

  // Ends -- basically the same code reversed
  var e = new Array(0), re = new Array(0);
  if (dangerous.endContainer != a) {
    for (var i = dangerous.endContainer; i != a; i = i.parentNode) {
      e.push(i);
    }
  }
  if (e.length > 0) {
    for (var i = 0; i < e.length; i++) {
      var xe = document.createRange();
      if (i) {
        xe.setStartBefore(e[i].firstChild);
        xe.setEndBefore(e[i - 1]);
      } else {
        xe.setStartBefore((e[i].nodeType == Node.TEXT_NODE) ? e[i] : e[i].firstChild);
        xe.setEnd(e[i], dangerous.endOffset);
      }
      re.unshift(xe);
    }
  }

  // Middle -- the uncaptured middle
  if ((s.length > 0) && (e.length > 0)) {
    var xm = document.createRange();
    xm.setStartAfter(s[s.length - 1]);
    xm.setEndBefore(e[e.length - 1]);
  } else {
    return [dangerous];
  }

  // Concat
  rs.push(xm);
  response = rs.concat(re);

  // Send to Console
  return response;
}

highlightSelection();
            })();
            """
        private val clearTranslationElementsJs = """
            javascript:(function() {
                document.body.innerHTML = document.originalInnerHTML;
                document.body.classList.remove("translated");
            })()
        """.trimIndent()
        private val translateParagraphJs = """
            function fetchNodesWithText(element) {
              var result = [];
              for (var i = 0; i < element.children.length; i++) {
                var child = element.children[i];
                // bypass non-necessary element
                if (
                  child.getAttribute("data-tiara-action-name") === "헤드글씨크기_클릭" ||
                  child.innerText === "original link"
                ) {
                  continue;
                }
                if (child.closest('img, button, code') || child.tagName === "SCRIPT"
                      || child.classList.contains("screen_out")
                      || child.classList.contains("blind")
                      || child.classList.contains("ico_view")
                ) {
                  continue;
                }
                if (
                  ["p", "h1", "h2", "h3", "h4", "h5", "h6", "span", "strong"].includes(child.tagName.toLowerCase()) ||
                  (child.children.length == 0 && child.innerText != "")
                ) {
                  if (child.innerText !== "") {
                    injectTranslateTag(child);
                    console.log(child.textContent + "\n\n");
                    result.push(child);
                  }
                } else {
                  result.push(fetchNodesWithText(child));
                }
              }
              return result;
            }
            
            function generateUUID() {
              var timestamp = new Date().getTime();
              const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                const random = (timestamp + Math.random() * 16) % 16 | 0;
                timestamp = Math.floor(timestamp / 16);
                return (c === 'x' ? random : (random & 0x3 | 0x8)).toString(16);
              });

              return uuid;
            }
            
            function injectTranslateTag(node) {
                // for monitoring visibility
                node.className += " to-translate";
                // for locating element's position
                node.id = generateUUID().toString();
                // for later inserting translated text
               var pElement = document.createElement("p");
               try {
                 //node.after(pElement);
                 node.parentNode.insertBefore(pElement, node.nextSibling);
               } catch(error) {
                //console.log(node.textContent);
                //console.log(error);
               }
            }
            
            if (!document.body.classList.contains("translated")) {
                document.body.classList.add("translated");
                document.originalInnerHTML = document.body.innerHTML;
                fetchNodesWithText(document.body);
            } else {
                if (!document.body.classList.contains("translated_but_hide")) {
                    document.translatedInnerHTML = document.body.innerHTML;
                    document.body.innerHTML = document.originalInnerHTML;
                    document.body.classList.add("translated_but_hide");
                } else {
                    document.body.innerHTML = document.translatedInnerHTML;
                    document.body.classList.remove("translated_but_hide");
                }
            }
            
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