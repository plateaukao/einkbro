package de.baumann.browser.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.print.PrintDocumentAdapter
import android.util.Base64
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.preference.PreferenceManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import de.baumann.browser.Ninja.BuildConfig
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.*
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TranslationMode
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.ViewUnit.dp
import de.baumann.browser.util.PdfDocumentAdapter
import org.apache.commons.text.StringEscapeUtils
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class NinjaWebView : WebView, AlbumController {
    private var dimen144dp = 0
    private var dimen108dp = 0
    private var onScrollChangeListener: OnScrollChangeListener? = null
    private val album: Album
    private val webViewClient: NinjaWebViewClient
    private val webChromeClient: NinjaWebChromeClient
    private val downloadListener: NinjaDownloadListener = NinjaDownloadListener(context)
    private var clickHandler: NinjaClickHandler? = null
    private val gestureDetector: GestureDetector = GestureDetector(context, NinjaGestureListener(this))

    val adBlock: AdBlock
    val cookieHosts: Cookie
    var shouldHideTranslateContext: Boolean = false

    private val javaHosts: Javascript

    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var config: ConfigManager

    var incognito: Boolean = false
        set(value) {
            field = value
            toggleCookieSupport(!incognito)
        }

    var isForeground = false
        private set

    var browserController: BrowserController? = null

    constructor(context: Context?, browserController: BrowserController?) : super(context!!) {
        this.browserController = browserController
        album = Album(context, this, browserController)
        initAlbum()
    }

    override fun onScrollChanged(l: Int, t: Int, old_l: Int, old_t: Int) {
        super.onScrollChanged(l, t, old_l, old_t)
        onScrollChangeListener?.onScrollChange(t, old_t)
    }

    fun setScrollChangeListener(onScrollChangeListener: OnScrollChangeListener?) {
        this.onScrollChangeListener = onScrollChangeListener
    }

    fun updateCssStyle() {
        val cssStyle = (if (config.boldFontStyle) boldFontCss else "") +
                if (config.fontStyleSerif) notoSansSerifFontCss else ""
        injectCss(cssStyle.toByteArray())
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
        dimen144dp = resources.getDimensionPixelSize(R.dimen.layout_width_144dp)
        dimen108dp = resources.getDimensionPixelSize(R.dimen.layout_height_108dp)
        isForeground = false
        adBlock = AdBlock(context)
        javaHosts = Javascript(context)
        cookieHosts = Cookie(context)
        webViewClient = NinjaWebViewClient(this)
        webChromeClient = NinjaWebChromeClient(this)
        clickHandler = NinjaClickHandler(this)
        initWebView()
        initWebSettings()
        initPreferences()
    }

    private fun initWebView() {
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)
        setDownloadListener(downloadListener)
        setOnTouchListener { _, motionEvent: MotionEvent? ->
            gestureDetector.onTouchEvent(motionEvent)
            false
        }

        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(settings, WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                settings.forceDark = WebSettings.FORCE_DARK_ON
            }
        }

        // when in dark mode, the default background color will be the activity background
        setBackgroundColor(context.resources.getColor(R.color.color_transparent))
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

        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun initPreferences() {
        config = ConfigManager(context)

        updateDesktopMode()

        with(settings) {
            setAppCacheEnabled(true)
            setAppCachePath("");
            setAppCacheMaxSize(30*1024*1024)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            textZoom = config.fontSize
            allowFileAccessFromFileURLs = sp.getBoolean("sp_remote", true)
            allowUniversalAccessFromFileURLs = sp.getBoolean("sp_remote", true)
            domStorageEnabled = sp.getBoolean("sp_remote", true)
            databaseEnabled = true
            blockNetworkImage = !sp.getBoolean(context!!.getString(R.string.sp_images), true)
            javaScriptEnabled = sp.getBoolean(context!!.getString(R.string.sp_javascript), true)
            javaScriptCanOpenWindowsAutomatically = sp.getBoolean(context!!.getString(R.string.sp_javascript), true)
            setSupportMultipleWindows(sp.getBoolean(context!!.getString(R.string.sp_javascript), true))
            setGeolocationEnabled(sp.getBoolean(context!!.getString(R.string.sp_location), false))
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            setRenderPriority(WebSettings.RenderPriority.HIGH)
        }
        webViewClient.enableAdBlock(sp.getBoolean(context!!.getString(R.string.sp_ad_block), true))

        toggleCookieSupport(config.cookies)
    }

    fun updateDesktopMode() {
        val isDesktopMode = config.desktop
        if (isDesktopMode) {
            settings.userAgentString = BrowserUnit.UA_DESKTOP
        } else {
            settings.userAgentString = WebSettings.getDefaultUserAgent(context).replace("wv", "")
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
        with(album) {
            setAlbumCover(null)
            albumTitle = context!!.getString(R.string.app_name)
        }
    }

    val requestHeaders: HashMap<String, String>
        get() {
            val requestHeaders = HashMap<String, String>()
            requestHeaders["DNT"] = "1"
            if (sp.getBoolean(context!!.getString(R.string.sp_savedata), false)) {
                requestHeaders["Save-Data"] = "on"
            }
            return requestHeaders
        }

    /* continue playing if preference is set */
    override fun onWindowVisibilityChanged(visibility: Int) {
        if (sp.getBoolean("sp_media_continue", false)) {
            if (visibility != GONE && visibility != INVISIBLE) super.onWindowVisibilityChanged(VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(url: String) {
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        if (url.trim { it <= ' ' }.isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error)
            return
        }
        if (!sp.getBoolean(context!!.getString(R.string.sp_javascript), true)) {
            if (javaHosts.isWhite(url)) {
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.javaScriptEnabled = true
            } else {
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.javaScriptEnabled = false
            }
        }

        if (url.startsWith("javascript")) {
            // Daniel
            super.loadUrl(url)
            return
        }
        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim { it <= ' ' }), requestHeaders)
    }

    override fun getAlbumView(): View = album.albumView

    override fun setAlbumCover(bitmap: Bitmap?) = album.setAlbumCover(bitmap)

    override fun getAlbumTitle(): String = album.albumTitle

    override fun setAlbumTitle(title: String) {
        album.albumTitle = title
    }

    override fun getAlbumUrl(): String = url ?: ""

    private var keepPlaying = false;
    override fun setKeepPlaying(keepPlaying: Boolean?) {
        this.keepPlaying = keepPlaying ?: false
    }

    override fun keepPlaying(): Boolean = keepPlaying

    @Synchronized
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
    }

    @Synchronized
    override fun deactivate() {
        clearFocus()
        isForeground = false
        album.deactivate()
    }

    @Synchronized
    fun update(progress: Int) {
        if (isForeground) {
            browserController?.updateProgress(progress)
        }
        if (isLoadFinish) {
            Handler(
                Looper.getMainLooper()).postDelayed({
                favicon?.let { setAlbumCover(it) }
            } ,
                250
            )
            if (prepareRecord()) {
                browserController?.updateAutoComplete()
            }
        }
    }

    @Synchronized
    fun update(title: String?) {
        album.albumTitle = title ?: ""
        // so that title on bottom bar can be updated
        browserController?.updateProgress(BrowserUnit.PROGRESS_MAX)
    }

    @Synchronized
    override fun destroy() {
        stopLoading()
        onPause()
        clearHistory()
        visibility = GONE
        removeAllViews()
        super.destroy()
    }

    fun createPrintDocumentAdapter(documentName: String, onFinish: ()-> Unit): PrintDocumentAdapter {
        val superAdapter = super.createPrintDocumentAdapter(documentName)
        return PdfDocumentAdapter(documentName, superAdapter, onFinish)
    }

    val isLoadFinish: Boolean
        get() = progress >= BrowserUnit.PROGRESS_MAX

    fun onLongPress() {
        val click = clickHandler!!.obtainMessage()
        click.target = clickHandler
        requestFocusNodeHref(click)
    }

    private fun prepareRecord(): Boolean {
        val title = title
        val url = url
        return !(title == null || title.isEmpty()
                || url == null || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT))
    }

    fun jumpToTop() {
        scrollTo(0, 0)
    }

    fun jumpToBottom() {
        if (isVerticalRead) {
            scrollTo(computeHorizontalScrollRange(), 0)
        } else {
            scrollTo(0, computeVerticalScrollRange())
        }
    }

    fun pageDownWithNoAnimation() {
        if (isVerticalRead) {
            scrollBy(shiftOffset(), 0)
        } else {
            scrollBy(0, shiftOffset())
        }
    }

    fun pageUpWithNoAnimation() {
        if (isVerticalRead) {
            scrollBy(-shiftOffset(), 0)
        } else {
            scrollBy(0, -shiftOffset())
        }
    }

    suspend fun getRawHtml() =  suspendCoroutine<String> { continuation ->
        if (!isReaderModeOn) {
            injectMozReaderModeJs(false)
            evaluateJavascript(getReaderModeBodyHtmlJs) { html ->
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                continuation.resume(processedHtml.substring(1, processedHtml.length - 1)) // handle prefix/postfix "
            }
        } else {
            evaluateJavascript(
                    "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val processedHtml = StringEscapeUtils.unescapeJava(html)
                continuation.resume(processedHtml.substring(1, processedHtml.length - 1)) // handle prefix/postfix "
            }
        }
    }

    // only works in isReadModeOn
    suspend fun getRawText() =  suspendCoroutine<String> { continuation ->
        if (!isReaderModeOn) {
            injectMozReaderModeJs(false)
            evaluateJavascript(getReaderModeBodyTextJs) { text -> continuation.resume(text.substring(1, text.length-2)) }
        } else {
            //if (!isReaderModeOn) continuation.resume("")

            evaluateJavascript(
                "(function() { return document.getElementsByTagName('html')[0].innerText; })();"
            ) { text ->
                val processedText = if (text.startsWith("\"") && text.endsWith("\"")) {
                    text.substring(1, text.length-2)
                } else text
                continuation.resume(processedText)
            }
        }
    }

    private fun shiftOffset(): Int {
        return if (isVerticalRead) {
            width - 40.dp(context)
        } else {
            height - config.pageReservedOffset.dp(context)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (browserController == null) return false

        return if (!browserController!!.handleKeyEvent(event)) {
            super.dispatchKeyEvent(event)
        } else {
            true
        }
    }

    private var isVerticalRead = false
    fun toggleVerticalRead() {
        isVerticalRead = !isVerticalRead
        toggleReaderMode(true)
    }

    var isReaderModeOn = false
    fun toggleReaderMode(isVertical: Boolean = false, getRawTextAction: ((String)->Unit)? = null) {
        isReaderModeOn = !isReaderModeOn
        if (isReaderModeOn) {
            injectMozReaderModeJs(isVertical)
            val getRawTextJs = if (getRawTextAction != null) " return document.getElementsByTagName('html')[0].innerText; " else ""
            evaluateJavascript("(function() { $replaceWithReaderModeBodyJs $getRawTextJs })();", getRawTextAction)
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

        loadUrl("javascript:(function() {" +
                "document.body.innerHTML = document.innerHTMLCache;" +
                "document.body.classList.remove(\"mozac-readerview-body\");" +
                verticalCssString +
                "window.scrollTo(0, 0);" +
                "})()")
    }

    private fun injectMozReaderModeJs(isVertical: Boolean = false) {
        try {
            val buffer = getByteArrayFromAsset("MozReadability.js")
            val cssBuffer = getByteArrayFromAsset(if (isVertical) "verticalReaderview.css" else "readerview.css")

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
            loadUrl("javascript:(function() {" +
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
                    "})()")

        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun removeFBSponsoredPosts() {
        injectJavascript(facebookHideSponsoredPostsJs.toByteArray())
    }

    fun hideTranslateContext() {
        when (config.translationMode) {
            TranslationMode.GOOGLE ->
                evaluateJavascript(hideGTranslateContext, null)
            TranslationMode.PAPAGO ->
                evaluateJavascript(hidePTranslateContext, null)
            else -> Unit
        }
    }

    fun hideGoogleBar() {
        if (config.translationMode == TranslationMode.GOOGLE_URL) {
            evaluateJavascript(hideGUrlTranslateContext, null)
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

    private fun injectJavascript(bytes: ByteArray) {
        try {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectCss(bytes: ByteArray) {
        try {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
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
                document.getElementById("gt-nvframe").remove();
                document.getElementById("gt-logo").remove();
                document.getElementById("wtgbr").remove();
                document.getElementById("gt-c").remove();
                document.getElementById("contentframe").style="top:0px";
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
                var outerHTML = createHtmlBody(article)
                return ('<html>'+ outerHTML +'</html>');
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
        private const val facebookHideSponsoredPostsJs = """
            javascript:(function() {
              var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0); 
              while(posts.length > 0) { posts.pop().style.display = "none"; }
              
              var ads = Array.from(document.getElementsByClassName("bg-s3")).filter(e => e.innerText.indexOf("Sponsored") != -1);
              ads.forEach(el => {el.style.display="none"; el.nextSibling.style.display="none";el.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none"});
            
            var qcleanObserver = new window.MutationObserver(function(mutation, observer){ 
              var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0); 
              while(posts.length > 0) { posts.pop().style.display = "none"; }
              
              var ads = Array.from(document.getElementsByClassName("bg-s3")).filter(e => e.innerText.indexOf("Sponsored") != -1);
              ads.forEach(el => {el.style.display="none"; el.nextSibling.style.display="none";el.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none"});
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

        private const val notoSansSerifFontCss = "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+TC:wght@400&display=swap');" +
                "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400&display=swap');" +
                //"@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@400&display=swap');" +
                "body {\n" +
                "font-family: 'Noto Serif TC', 'Noto Serif JP', serif !important;\n" +
                "}\n"

//        private const val notoSansSerifFontCss = """
//            @font-face {
//                 font-family: custom;
//                 src: url("asset://noto_sans.ttc");
//            }
//            * {
//              font-family: custom, serif !important;
//            }
//        """

        private const val boldFontCss = "* {\n" +
                "\tfont-weight:700 !important;\n" +  /*"\tborder-color: #555555 !important;\n" +*/
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
}

