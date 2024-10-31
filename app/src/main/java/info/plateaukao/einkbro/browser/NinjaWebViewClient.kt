package info.plateaukao.einkbro.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.security.KeyChain
import android.util.Log
import android.view.View
import android.webkit.ClientCertRequest
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.caption.DualCaptionProcessor
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.AuthenticationDialogFragment
import io.github.edsuns.adfilter.AdFilter
import nl.siegmann.epublib.domain.Book
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
    private val dialogManager: DialogManager by lazy { DialogManager(context as Activity) }

    private val webContentPostProcessor = WebContentPostProcessor()
    private var hasAdBlock: Boolean = true
    var book: Book? = null

    private val adFilter: AdFilter = AdFilter.get()

    private val dualCaptionProcessor = DualCaptionProcessor()

    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    private var onPageFinishedAction: () -> Unit = {}
    fun setOnPageFinishedAction(action: () -> Unit) {
        onPageFinishedAction = action
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (config.adBlock) {
            adFilter.performScript(view, url)
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        ebWebView.updateCssStyle()

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

        // skip translation pages
        if (config.isSaveHistoryWhenLoad() &&
            !ebWebView.incognito &&
            !isTranslationDomain(url) &&
            url != BrowserUnit.URL_ABOUT_BLANK
        ) {
            addHistoryAction(ebWebView.albumTitle, url)
        }

        // test
        ebWebView.evaluateJavascript(
            """
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
                    const w=window;
                    w.addEventListener('touchstart',wrappedOnDownFunc);
                    function wrappedOnDownFunc(e){
                        if(e.touches.length==1){
                            w._touchTarget = findTargetWithA(e.touches[0].target);
                        }
                        console.log('hey touched something ' +w._touchTarget);
                    }
        """.trimIndent(), null
        )

        if (url != "about:blank") {
            onPageFinishedAction()
        }
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
        val list = webView.copyBackForwardList()

        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)
            val title = item.title
            val url = item.url
            Log.d("ebWebViewClient", "Title: $title - URL: $url")
        }

        // handle pocket authentication
        if (url.startsWith("einkbropocket://pocket-auth")) {
            val requestToken = url.substringAfter("code=", "")
            ebWebView.handlePocketRequestToken(requestToken)
            return true
        }

        if (url.startsWith("http")) {
//            webView.loadUrl(url, ebWebView.requestHeaders)
//            return true
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
        AuthenticationDialogFragment { username, password ->
            handler?.proceed(username, password)
        }.show((context as FragmentActivity).supportFragmentManager, "AuthenticationDialog")
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        // if https is not available, try http
        if (error?.description == "net::ERR_SSL_PROTOCOL_ERROR" && request != null) {
            ebWebView.loadUrl(request.url.buildUpon().scheme("http").build().toString())
        } else {
            Log.e("ebWebViewClient", "onReceivedError:${request?.url} / ${error?.description}")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        handleWebRequest(view, Uri.parse(url)) ?: super.shouldInterceptRequest(view, url)

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        if (config.adBlock) {
            val result = adFilter.shouldIntercept(view, request)
            if (result.shouldBlock) return result.resourceResponse
        }

        return handleWebRequest(view, request.url) ?: super.shouldInterceptRequest(view, request)
    }

    private fun handleWebRequest(webView: WebView, uri: Uri): WebResourceResponse? {
        val url = uri.toString()

        if (!config.cookies) {
            if (cookie.isWhite(url)) {
                val manager = CookieManager.getInstance()
                manager.getCookie(url)
                manager.setAcceptCookie(true)
            } else {
                val manager = CookieManager.getInstance()
                manager.setAcceptCookie(false)
            }
        }

        processBookResource(uri)?.let { return it }
        processCustomFontRequest(uri)?.let { return it }
        dualCaptionProcessor.processUrl(url)?.let {
            ebWebView.dualCaption = it
            return WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(it.toByteArray())
            )
        }

        return null
    }

    private fun processBookResource(uri: Uri): WebResourceResponse? {
        val currentBook = book ?: return null

        if (uri.scheme == "img") {
            val resource = currentBook.resources.getByHref(uri.host.toString())
            return WebResourceResponse(
                resource.mediaType.name,
                "UTF-8",
                ByteArrayInputStream(resource.data)
            )
        }
        return null
    }

    private fun processCustomFontRequest(uri: Uri): WebResourceResponse? {
        if (uri.path?.contains("mycustomfont") == true) {
            val fontUri = if (!ebWebView.shouldUseReaderFont()) {
                config.customFontInfo?.url?.toUri() ?: return null
            } else {
                config.readerCustomFontInfo?.url?.toUri() ?: return null
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
        val dialog = BottomSheetDialog(holder)
        val dialogView = View.inflate(holder, R.layout.dialog_action, null)

        dialogView.findViewById<TextView>(R.id.dialog_text)
            .setText(R.string.dialog_content_resubmission)
        dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
            resend.sendToTarget()
            dialog.cancel()
        }
        dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
            doNotResend.sendToTarget()
            dialog.cancel()
        }
        dialog.setContentView(dialogView)
        dialog.show()
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    // return true means it's processed
    private fun handlePrivateKeyAlias(request: ClientCertRequest, alias: String?): Boolean {
        val keyAlias = alias ?: return false
        val holder = context as? Activity ?: return false
        try {
            val certChain = KeyChain.getCertificateChain(holder, keyAlias) ?: return false
            val privateKey = KeyChain.getPrivateKey(holder, keyAlias) ?: return false
            request.proceed(privateKey, certChain)
            return true
        } catch (e: Exception) {
            Log.e(
                "ebWebViewClient",
                "Error when getting CertificateChain or PrivateKey for alias '${alias}'",
                e
            )
        }
        return false
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        val holder = view.context as? Activity ?: return
        KeyChain.choosePrivateKeyAlias(
            holder,
            { alias ->
                if (!handlePrivateKeyAlias(request, alias)) {
                    super.onReceivedClientCertRequest(view, request)
                }
            },
            request.keyTypes, request.principals, request.host, request.port, null
        )
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val title = "An Error Occurred!!!"
        var message =
            "The page you are trying to view cannot be shown because the connection isn't private or the authenticity of the received data could not be verified. \n\nIf you want to take the risk and continue viewing the page, please press OK.\n\n\nReason: "
        when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> message += """"Certificate authority is not trusted.""""
            SslError.SSL_EXPIRED -> message += """"Certificate has expired.""""
            SslError.SSL_IDMISMATCH -> message += """"Certificate Hostname mismatch.""""
            SslError.SSL_NOTYETVALID -> message += """"Certificate is not yet valid.""""
            SslError.SSL_DATE_INVALID -> message += """"Certificate date is invalid.""""
            SslError.SSL_INVALID -> message += """"Certificate is invalid.""""
        }

        Log.e(TAG, "onReceivedSslError: $message")
        if (config.enableCertificateErrorDialog) {
            dialogManager.showOkCancelDialog(
                title = title,
                message = message,
                showInCenter = true,
                okAction = { handler.proceed() },
                cancelAction = { handler.cancel() }
            )
        } else {
            handler.proceed()
        }
    }

    companion object {
        private const val TAG = "ebWebViewClient"
    }
}