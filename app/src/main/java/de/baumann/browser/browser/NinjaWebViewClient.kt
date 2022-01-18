package de.baumann.browser.browser

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import java.io.InputStream
import java.net.URISyntaxException
import kotlin.collections.HashMap

import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import de.baumann.browser.util.DebugT
import de.baumann.browser.view.dTLoadUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.IOException
import android.webkit.WebResourceResponse





class NinjaWebViewClient(
        private val ninjaWebView: NinjaWebView,
        private val addHistoryAction: (String) -> Unit
) : WebViewClient(), KoinComponent {
    private val context: Context = ninjaWebView.context
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()
    private val adBlock: AdBlock by inject()
    private val cookie: Cookie by inject()
    private val white: Boolean = false
    private val webContentPostProcessor = WebContentPostProcessor()
    private var hasAdBlock: Boolean = true
    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (config.boldFontStyle || config.fontStyleSerif || config.whiteBackground || config.enableCustomFont) {
            ninjaWebView.updateCssStyle()
        }

        webContentPostProcessor.postProcess(ninjaWebView, url)

        if (ninjaWebView.shouldHideTranslateContext) {
            ninjaWebView.postDelayed({
                ninjaWebView.hideTranslateContext()
            }, 2000)
        }

        // skip translation pages
        if (config.saveHistory && !ninjaWebView.incognito && !isTranslationDomain(url)) {
            addHistoryAction(url)
        }
        dTLoadUrl?.printTime()
        dTLoadUrl = null
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
        if (dTLoadUrl != null) {
            dTLoadUrl?.printPath(uri.toString())
        } else {
            dTLoadUrl = DebugT("loadUrl:${uri}")
        }
        val url = uri.toString()
        val packageManager = context.packageManager
        val browseIntent = Intent(Intent.ACTION_VIEW).setData(uri)
        if (url.startsWith("http")) {
            webView.loadUrl(url, ninjaWebView.requestHeaders)
            return true
        }
        if (browseIntent.resolveActivity(packageManager) != null) {
            context.startActivity(browseIntent)
            return true
        }
        if (url.startsWith("intent:")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(context.packageManager) != null) {
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        NinjaToast.show(context, R.string.toast_load_error)
                    }
                    return true
                }
                //try to find fallback url
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    webView.loadUrl(fallbackUrl)
                    return true
                }
                //invite to install
                val marketIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()))
                if (marketIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(marketIntent)
                    return true
                }
            } catch (e: URISyntaxException) {
                //not an intent uri
                return false
            }
        }
        return true //do nothing in other cases
    }

    private val adPngResponse: WebResourceResponse by lazy {
        val encodedImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        val decodedString: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        WebResourceResponse(
                BrowserUnit.MIME_TYPE_IMAGE,
                BrowserUnit.URL_ENCODING,
                decodedString.inputStream()
        )
    }

    private val adTxtResponse: WebResourceResponse = WebResourceResponse(
            BrowserUnit.MIME_TYPE_TEXT_PLAIN,
            BrowserUnit.URL_ENCODING,
            ByteArrayInputStream("".toByteArray())
    )

    private val adGifResponse: WebResourceResponse by lazy {
        val encodedImage = "R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
        val decodedString: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        WebResourceResponse(
                "image/gif",
                BrowserUnit.URL_ENCODING,
                decodedString.inputStream()
        )
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (hasAdBlock && !white && adBlock.isAd(url))  {
            return adTxtResponse
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {
            if (cookie.isWhite(url)) {
                val manager = CookieManager.getInstance()
                manager.getCookie(url)
                manager.setAcceptCookie(true)
            } else {
                val manager = CookieManager.getInstance()
                manager.setAcceptCookie(false)
            }
        }

        if (url.startsWith("asset")) {
            val asset: String = url.substring("asset://".length)
            val statusCode = 200
            val reasonPhase = "OK"
            val responseHeaders: MutableMap<String, String> = HashMap()
            responseHeaders["Access-Control-Allow-Origin"] = "*"
            val inputStream: InputStream = context.assets.open(asset)
            return WebResourceResponse(
                    "font/" + MimeTypeMap.getFileExtensionFromUrl(asset),
                    "utf-8",
                    statusCode,
                    reasonPhase,
                    responseHeaders,
                    inputStream
            )
        }

        return super.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (hasAdBlock && !white && adBlock.isAd(request.url.toString())) {
            return adTxtResponse
            /*
            val path = request.url.toString()
            if (path.contains("gif", ignoreCase = true)) {
                return adGifResponse
            } else if (path.contains("png", ignoreCase = true)) {
                return adPngResponse
            } else {
                return adGifResponse
            }
             */
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {
            if (cookie.isWhite(request.url.toString())) {
                val manager = CookieManager.getInstance()
                manager.getCookie(request.url.toString())
                manager.setAcceptCookie(true)
            } else {
                val manager = CookieManager.getInstance()
                manager.setAcceptCookie(false)
            }
        }

        Log.i("font", "request url: ${request.url.path}")
        if (request.url.path?.contains("mycustomfont") == true) {
            val uri = config.customFontInfo?.url?.toUri() ?: return super.shouldInterceptRequest(view, request)
            //if (fontWebRequest!= null && customFontUrl == uri) return fontWebRequest


            try {
                val inputStream= context.contentResolver.openInputStream(uri)
                //fontWebRequest = WebResourceResponse("application/x-font-ttf", "UTF-8", inputStream)
                //customFontUrl = uri
                //return fontWebRequest
                return WebResourceResponse("application/x-font-ttf", "UTF-8", inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onFormResubmission(view: WebView, doNotResend: Message, resend: Message) {
        val holder = IntentUnit.getContext() as? Activity ?: return
        val dialog = BottomSheetDialog(holder)
        val dialogView = View.inflate(holder, R.layout.dialog_action, null)

        dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.dialog_content_resubmission)
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
        /*
        var message = "\"SSL Certificate error.\""
        when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> message = "\"Certificate authority is not trusted.\""
            SslError.SSL_EXPIRED -> message = "\"Certificate has expired.\""
            SslError.SSL_IDMISMATCH -> message = "\"Certificate Hostname mismatch.\""
            SslError.SSL_NOTYETVALID -> message = "\"Certificate is not yet valid.\""
            SslError.SSL_DATE_INVALID -> message = "\"Certificate date is invalid.\""
            SslError.SSL_INVALID -> message = "\"Certificate is invalid.\""
        }

        val text = """$message - ${context.getString(R.string.dialog_content_ssl_error)}"""
        val dialog = BottomSheetDialog(context)
        val dialogView = View.inflate(context, R.layout.dialog_action, null)

        dialogView.findViewById<TextView>(R.id.dialog_text).text = text
        dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
            handler.proceed()
            dialog.cancel()
        }
        dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
            handler.cancel()
            dialog.cancel()
        }
        dialog.setContentView(dialogView)
        dialog.show()

        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
         */
    }

    companion object {
        private var customFontUrl: Uri? = null
        private var fontWebRequest: WebResourceResponse? = null
    }
}