package info.plateaukao.einkbro.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.print.PrintDocumentAdapter
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.viewbinding.BuildConfig
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.*
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.DebugT
import info.plateaukao.einkbro.util.PdfDocumentAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


open class NinjaWebView(
    context: Context,
    var browserController: BrowserController?
) : WebView(context), AlbumController, KoinComponent {
    private var onScrollChangeListener: OnScrollChangeListener? = null
    override val album: Album = Album(this, browserController)
    protected val webViewClient: NinjaWebViewClient
    private val webChromeClient: NinjaWebChromeClient
    private val downloadListener: NinjaDownloadListener = NinjaDownloadListener(context)
    private val clickHandler: NinjaClickHandler

    var shouldHideTranslateContext: Boolean = false
    protected var isEpubReaderMode = false

    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()

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
        val cssStyle = (if (config.boldFontStyle) boldFontCss else "") +
                (if (config.blackFontStyle) makeTextBlackCss else "") +
                (if (config.fontType == FontType.GOOGLE_SERIF) notoSansSerifFontCss else "") +
                (if (config.fontType == FontType.SERIF) serifFontCss else "") +
                (if (config.whiteBackground) whiteBackgroundCss else "") +
                (if (config.fontType == FontType.CUSTOM) customFontCss else "") +
                // all css are purgsed by epublib. need to add it back if it's epub reader mode
                if (isEpubReaderMode) String(
                    getByteArrayFromAsset("readerview.css"),
                    Charsets.UTF_8
                ) else ""
        if (cssStyle.isNotBlank()) {
            injectCss(cssStyle.toByteArray())
        }
    }

    override fun reload() {
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        isVerticalRead = false
        isReaderModeOn = false
        super.reload()
    }

    override fun goBack() {
        isVerticalRead = false
        isReaderModeOn = false
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
        val defaultUserAgentString = WebSettings.getDefaultUserAgent(context).replace("wv", "")
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
        with(CookieManager.getInstance()) {
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

    val requestHeaders: HashMap<String, String>
        get() {
            val requestHeaders = HashMap<String, String>()
            requestHeaders["DNT"] = "1"
            requestHeaders["Save-Data"] = "on"
            return requestHeaders
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

        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            setAlbumCover(it)
        }

        super.loadUrl(url, additionalHttpHeaders)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(url: String) {
        album.isLoaded = true
        dTLoadUrl = DebugT("loadUrl")

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

        albumTitle = ""
        // show progress right away
        if (url.startsWith("https")) {
            postDelayed({ if (progress < FAKE_PRE_PROGRESS) update(FAKE_PRE_PROGRESS) }, 200)
        }

        if (browserController?.loadInSecondPane(processedUrl) == true) {
            return
        }

        val strippedUrl = BrowserUnit.stripUrlQuery(processedUrl)
        super.loadUrl(BrowserUnit.queryWrapper(context, strippedUrl), requestHeaders)
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

    override val albumUrl: String
        get() = url ?: ""

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
        album.albumTitle = title ?: ""
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
        onFinish: () -> Unit
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

    fun jumpToTop() {
        scrollTo(0, 0)
        updatePageInfo()
    }

    fun jumpToBottom() {
        if (isVerticalRead) {
            scrollTo(computeHorizontalScrollRange() - shiftOffset(), 0)
        } else {
            scrollTo(0, computeVerticalScrollRange() - shiftOffset())
        }
        updatePageInfo()
    }

    open fun pageDownWithNoAnimation() {
        if (isVerticalRead) {
            scrollBy(shiftOffset(), 0)
            scrollX = min(computeHorizontalScrollRange() - width, scrollX)
        } else {
            scrollBy(0, shiftOffset())
            scrollY = min(computeVerticalScrollRange() - shiftOffset(), scrollY)
        }
        updatePageInfo()
    }

    open fun pageUpWithNoAnimation() {
        if (isVerticalRead) {
            scrollBy(-shiftOffset(), 0)
            scrollX = max(0, scrollX)
        } else {
            scrollBy(0, -shiftOffset())
            scrollY = max(0, scrollY)
        }
        updatePageInfo()
    }

    fun updatePageInfo() {
        val info = if (isVerticalRead) {
            "${ceil((scrollX + 1).toDouble() / shiftOffset()).toInt()}/${computeHorizontalScrollRange() / shiftOffset()}"
        } else {
            "${ceil((scrollY + 1).toDouble() / shiftOffset()).toInt()}/${computeVerticalScrollRange() / shiftOffset()}"
        }
        browserController?.updatePageInfo(if (info != "0/0") info else "-/-")
    }

    suspend fun getRawHtml() = suspendCoroutine<String> { continuation ->
        if (!isReaderModeOn) {
            injectMozReaderModeJs(false)
            evaluateJavascript(String.format(getReaderModeBodyHtmlJs, url)) { html ->
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                continuation.resume(
                    processedHtml.substring(
                        1,
                        processedHtml.length - 1
                    )
                ) // handle prefix/postfix "
            }
        } else {
            evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                continuation.resume(
                    processedHtml.substring(
                        1,
                        processedHtml.length - 1
                    )
                ) // handle prefix/postfix "
            }
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
            height - config.pageReservedOffset.dp(context)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (browserController?.handleKeyEvent(event) == true) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }

    protected var isVerticalRead = false
    fun toggleVerticalRead() {
        isVerticalRead = !isVerticalRead
        toggleReaderMode(true)
    }

    var isReaderModeOn = false
    fun toggleReaderMode(
        isVertical: Boolean = false,
        getRawTextAction: ((String) -> Unit)? = null
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
        } else {
            disableReaderMode(isVertical)
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
        postAction: (() -> Unit)? = null
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

    companion object {
        private const val FAKE_PRE_PROGRESS = 5

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

        private const val replaceWithReaderModeBodyJs = """
            var documentClone = document.cloneNode(true);
            var article = new Readability(documentClone, {classesToPreserve: preservedClasses}).parse();
            document.innerHTMLCache = document.body.innerHTML;

            article.readingTime = getReadingTime(article.length, document.lang);

            document.body.outerHTML = createHtmlBody(article)

            // change font type
            var bodyClasses = document.body.classList;
            bodyClasses.add("serif");
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
                return article.textContent;
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
                    "body {\n" +
                    "font-family: 'Noto Serif TC', 'Noto Serif JP', 'Noto Serif KR', 'Noto Serif SC', serif !important;\n" +
                    //"font-family: serif !important;\n" +
                    "}\n"
        private const val serifFontCss =
            "body {\n" +
                    "font-family: serif !important;\n" +
                    "}\n"

        private const val customFontCss = """
            @font-face {
                 font-family: customfont;
                 font-weight: 400;
                 font-display: swap;
                 src: url('mycustomfont');
            }
            body {
              font-family: customfont !important;
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
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "a,a * {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "a: visited,a: visited *,a: active,a: active * {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "a: hover,a: hover * {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "input,select,option,button,textarea {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n" +
                "input[type=button]: focus,input[type=submit]: focus,input[type=reset]: focus,input[type=image]: focus, input[type=button]: hover,input[type=submit]: hover,input[type=reset]: hover,input[type=image]: hover {\n" +
                "\tfont-weight:700 !important;\n" +
                "}\n"
    }

    init {
        initAlbum()
    }
}

var dTLoadUrl: DebugT? = null