package info.plateaukao.einkbro.browser

import android.app.Activity
import android.webkit.DownloadListener
import info.plateaukao.einkbro.unit.BrowserUnit.download
import info.plateaukao.einkbro.view.EBWebView

class EBDownloadListener(private val webView: EBWebView) : DownloadListener {
    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        val context = webView.context
        if (context is Activity) {
            download(context, url, contentDisposition, mimeType, webView)
        }
    }
}
