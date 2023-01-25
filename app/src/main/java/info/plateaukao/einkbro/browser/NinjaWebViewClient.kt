package info.plateaukao.einkbro.browser

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.AuthenticationDialogFragment
import nl.siegmann.epublib.domain.Book
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.IOException


class NinjaWebViewClient(
    private val ninjaWebView: NinjaWebView,
    private val addHistoryAction: (String, String) -> Unit
) : WebViewClient(), KoinComponent {
    private val context: Context = ninjaWebView.context
    private val config: ConfigManager by inject()
    private val adBlock: AdBlockV2 by inject()
    private val cookie: Cookie by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(context as Activity) }

    private val white: Boolean = false
    private val webContentPostProcessor = WebContentPostProcessor()
    private var hasAdBlock: Boolean = true
    var book: Book? = null

    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (config.boldFontStyle ||
            config.fontType != FontType.SYSTEM_DEFAULT ||
            config.whiteBackground
        ) {
            ninjaWebView.updateCssStyle()
        }

        webContentPostProcessor.postProcess(ninjaWebView, url)

        if (ninjaWebView.shouldHideTranslateContext) {
            ninjaWebView.postDelayed({
                ninjaWebView.hideTranslateContext()
            }, 2000)
        }

        // skip translation pages
        if (config.saveHistory &&
            !ninjaWebView.incognito &&
            !isTranslationDomain(url) &&
            url != BrowserUnit.URL_ABOUT_BLANK
        ) {
            addHistoryAction(ninjaWebView.albumTitle, url)
        }
    }

    private fun isTranslationDomain(url: String): Boolean {
        return url.contains("translate.goog") || url.contains("papago.naver.net")
                || url.contains("papago.naver.com") || url.contains("translate.google.com")
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        handleUri(view, Uri.parse(url))

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        handleUri(view, request.url)

    private fun handleUri(webView: WebView, uri: Uri): Boolean {
        val url = uri.toString()
        if (url.startsWith("http")) {
            webView.loadUrl(url, ninjaWebView.requestHeaders)
            return true
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
                        NinjaToast.show(context, R.string.toast_load_error)
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
                NinjaToast.show(context, R.string.toast_load_error)
            }
            return true
        }

        webView.loadUrl(fallbackUrl)
        return true
    }

    private val adTxtResponse: WebResourceResponse = WebResourceResponse(
        BrowserUnit.MIME_TYPE_TEXT_PLAIN,
        BrowserUnit.URL_ENCODING,
        ByteArrayInputStream("".toByteArray())
    )

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        AuthenticationDialogFragment { username, password ->
            handler?.proceed(username, password)
        }.show((context as FragmentActivity).supportFragmentManager, "AuthenticationDialog")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        // if https is not available, try http
        if (error?.description == "net::ERR_SSL_PROTOCOL_ERROR" && request != null) {
            ninjaWebView.loadUrl(request.url.buildUpon().scheme("http").build().toString())
        } else {
            Log.e("NinjaWebViewClient", "onReceivedError:${request?.url} / ${error?.description}")
        }
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        handleWebRequest(view, Uri.parse(url)) ?: super.shouldInterceptRequest(view, url)

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? =
        handleWebRequest(view, request.url) ?: super.shouldInterceptRequest(view, request)

    private fun handleWebRequest(webView: WebView, uri: Uri): WebResourceResponse? {
        val url = uri.toString()
        if (hasAdBlock && !white && adBlock.isAd(url)) {
            return adTxtResponse
        }

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

        return null
        // images handling
//        val brightness = 0.2f
//        val lowerCaseUrl = url.lowercase(Locale.ROOT)
//        return if (lowerCaseUrl.contains(".jpg") || lowerCaseUrl.contains(".jpeg")) {
//            val bitmap = Glide.with(webView).asBitmap()
//                .diskCacheStrategy(DiskCacheStrategy.ALL).load(url)
//                .apply(bitmapTransform(BrightnessFilterTransformation(brightness)))
//                .submit().get()
//            WebResourceResponse(
//                "image/jpg", "UTF-8", getBitmapInputStream(
//                    bitmap,
//                    Bitmap.CompressFormat.JPEG
//                )
//            )
//        } else if (lowerCaseUrl.contains(".png")) {
//            val bitmap = Glide.with(webView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL)
//                .load(url)
//                .apply(bitmapTransform(BrightnessFilterTransformation(brightness)))
//                .submit().get()
//            WebResourceResponse(
//                "image/png", "UTF-8", getBitmapInputStream(
//                    bitmap,
//                    Bitmap.CompressFormat.PNG
//                )
//            )
//        } else if (lowerCaseUrl.contains(".webp")) {
//            val bitmap = Glide.with(webView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL)
//                .load(url)
//                .apply(bitmapTransform(BrightnessFilterTransformation(brightness)))
//                .submit().get()
//            WebResourceResponse(
//                "image/webp", "UTF-8", getBitmapInputStream(
//                    bitmap,
//                    Bitmap.CompressFormat.WEBP
//                )
//            )
//        } else {
//            null
//        }
    }

//    private fun getBitmapInputStream(
//        bitmap: Bitmap,
//        compressFormat: Bitmap.CompressFormat
//    ): InputStream {
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        bitmap.compress(compressFormat, 80, byteArrayOutputStream)
//        val bitmapData: ByteArray = byteArrayOutputStream.toByteArray()
//        return ByteArrayInputStream(bitmapData)
//    }

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
            val fontUri = config.customFontInfo?.url?.toUri() ?: return null

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

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        var message = """"SSL Certificate error.""""
        when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> message = """"Certificate authority is not trusted.""""
            SslError.SSL_EXPIRED -> message = """"Certificate has expired.""""
            SslError.SSL_IDMISMATCH -> message = """"Certificate Hostname mismatch.""""
            SslError.SSL_NOTYETVALID -> message = """"Certificate is not yet valid.""""
            SslError.SSL_DATE_INVALID -> message = """"Certificate date is invalid.""""
            SslError.SSL_INVALID -> message = """"Certificate is invalid.""""
        }

        val text = """$message - ${context.getString(R.string.dialog_content_ssl_error)}"""
        Log.e(TAG, "onReceivedSslError: $message")
        if (config.enableCertificateErrorDialog) {
            dialogManager.showOkCancelDialog(
                message = text,
                showInCenter = true,
                okAction = { handler.proceed() },
                cancelAction = { handler.cancel() }
            )
        } else {
            handler.proceed()
        }
    }

    companion object {
        private const val TAG = "NinjaWebViewClient"
    }
}