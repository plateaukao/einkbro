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
import androidx.core.net.toUri
import de.baumann.browser.util.DebugT
import de.baumann.browser.view.dTLoadUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.IOException
import android.webkit.WebResourceResponse
import de.baumann.browser.preference.FontType
import nl.siegmann.epublib.domain.Book

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
    var book: Book? = null

    fun enableAdBlock(enable: Boolean) {
        this.hasAdBlock = enable
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (config.boldFontStyle ||
                config.fontType != FontType.SYSTEM_DEFAULT ||
                config.whiteBackground) {
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
                url != BrowserUnit.URL_ABOUT_BLANK) {
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
            } catch(e: Exception) { }
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

        val fontResponse = processCustomFontRequest(Uri.parse(url))
        if (fontResponse != null) return fontResponse

        return super.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (hasAdBlock && !white && adBlock.isAd(request.url.toString())) {
            return adTxtResponse
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

        processBookResource(request)?.let { return it }
        processCustomFontRequest(request.url)?.let { return it }

        return super.shouldInterceptRequest(view, request)
    }

    private fun processBookResource(request: WebResourceRequest): WebResourceResponse? {
        val currentBook = book ?: return null

        if (request.url.scheme == "img") {
            val resource = currentBook.resources.getByHref(request.url.host.toString())
            return WebResourceResponse(resource.mediaType.name, "UTF-8", ByteArrayInputStream(resource.data))
        }
        return null
    }

    private fun processCustomFontRequest(uri: Uri): WebResourceResponse? {
        if (uri.path?.contains("mycustomfont") == true) {
            val uri = config.customFontInfo?.url?.toUri() ?: return null

            try {
                val inputStream= context.contentResolver.openInputStream(uri)
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
        val holder = IntentUnit.context as? Activity ?: return
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
}