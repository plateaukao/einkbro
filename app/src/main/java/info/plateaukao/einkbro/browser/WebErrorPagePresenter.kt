package info.plateaukao.einkbro.browser

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import info.plateaukao.einkbro.view.EBWebView

class WebErrorPagePresenter(
    private val ebWebView: EBWebView,
) {
    internal var lastFailedUrl: String? = null
        private set

    fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        // if https is not available, try http
        if (error?.description == "net::ERR_SSL_PROTOCOL_ERROR" && request != null) {
            ebWebView.loadUrl(request.url.buildUpon().scheme("http").build().toString())
            return
        }
        Log.e("ebWebViewClient", "onReceivedError:${request?.url} / ${error?.description}")

        if (request?.isForMainFrame == true) {
            val scheme = request.url.scheme
            if (scheme == "http" || scheme == "https") {
                showErrorPage(request.url.toString(), error?.description?.toString())
            }
        }
    }

    private fun showErrorPage(failedUrl: String, rawReason: String?) {
        lastFailedUrl = failedUrl
        val friendly = friendlyReason(rawReason)
        val query = "?url=" +
                Uri.encode(failedUrl) +
                "&reason=" + Uri.encode(friendly)
        ebWebView.loadUrl("file:///android_asset/error_page.html$query")
    }

    private fun friendlyReason(raw: String?): String {
        if (raw.isNullOrBlank()) return "Check your connection and try again."
        return when {
            raw.contains("INTERNET_DISCONNECTED") ->
                "You appear to be offline. Check your Wi-Fi or mobile data."
            raw.contains("NAME_NOT_RESOLVED") ->
                "Couldn't find this site. Check the address and try again."
            raw.contains("CONNECTION_REFUSED") ->
                "The server refused the connection."
            raw.contains("CONNECTION_TIMED_OUT") || raw.contains("TIMED_OUT") ->
                "The connection timed out."
            raw.contains("CONNECTION_RESET") ->
                "The connection was reset."
            raw.contains("CONNECTION_CLOSED") ->
                "The connection was closed unexpectedly."
            raw.contains("ADDRESS_UNREACHABLE") ->
                "The server is unreachable."
            raw.contains("SSL") || raw.contains("CERT") ->
                "There's a problem with the site's security certificate."
            else -> "This page couldn't be loaded."
        }
    }
}
