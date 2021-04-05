package de.baumann.browser.browser

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import de.baumann.browser.database.Record
import de.baumann.browser.database.RecordAction
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URISyntaxException
import kotlin.collections.HashMap


class NinjaWebViewClient(private val ninjaWebView: NinjaWebView) : WebViewClient() {
    private val context: Context = ninjaWebView.context
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val config: ConfigManager = ConfigManager(context)
    private val adBlock: AdBlock = ninjaWebView.adBlock
    private val cookie: Cookie = ninjaWebView.cookieHosts
    private val white: Boolean = false
    private var hasAdBlock: Boolean = true
    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (sp.getBoolean("saveHistory", true)) {
            val action = RecordAction(context)
            action.open(true)
            if (action.checkHistory(url)) {
                action.deleteHistoryItemByURL(url)
                action.addHistory(Record(ninjaWebView.title, url, System.currentTimeMillis()))
            } else {
                action.addHistory(Record(ninjaWebView.title, url, System.currentTimeMillis()))
            }
            action.close()
        }
        if (url.contains("facebook.com")) {
            ninjaWebView.removeFBSponsoredPosts()
        }
        // lab: change css
        if (config.boldFontStyle || config.fontStyleSerif) {
            ninjaWebView.updateCssStyle()
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            handleUri(view, Uri.parse(url))

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            handleUri(view, request.url)

    private fun handleUri(webView: WebView, uri: Uri): Boolean {
        val url = uri.toString()
        val parsedUri = Uri.parse(url)
        val packageManager = context.packageManager
        val browseIntent = Intent(Intent.ACTION_VIEW).setData(parsedUri)
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

    private val webResourceResponse = WebResourceResponse(
            BrowserUnit.MIME_TYPE_TEXT_PLAIN,
            BrowserUnit.URL_ENCODING,
            ByteArrayInputStream("".toByteArray())
    )

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (hasAdBlock && !white && adBlock.isAd(url)) {
            return webResourceResponse
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
            return webResourceResponse
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
    }
}