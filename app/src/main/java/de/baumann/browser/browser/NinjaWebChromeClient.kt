package de.baumann.browser.browser

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.view.NinjaWebView

class NinjaWebChromeClient(private val ninjaWebView: NinjaWebView) : WebChromeClient() {
    override fun onCreateWindow(view: WebView, dialog: Boolean, userGesture: Boolean, resultMsg: Message): Boolean {
        val targetWebView = WebView(view.context) // pass a context
        targetWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url?.let{ handleWebViewLinks(it) } // you can get your target url here
                super.onPageStarted(view, url, favicon)
            }
        }
        val transport = resultMsg.obj as WebViewTransport
        transport.webView = targetWebView
        resultMsg.sendToTarget()
        return true
    }

    private fun handleWebViewLinks(url: String) = ninjaWebView.loadUrl(url)

    override fun onProgressChanged(view: WebView, progress: Int) {
        super.onProgressChanged(view, progress)
        ninjaWebView.update(progress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        ninjaWebView.update(title)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        ninjaWebView.browserController.onShowCustomView(view, callback)
        super.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        ninjaWebView.browserController.onHideCustomView()
        super.onHideCustomView()
    }

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        ninjaWebView.browserController.showFileChooser(filePathCallback)
        return true
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        val activity = ninjaWebView.context as Activity
        HelperUnit.grantPermissionsLoc(activity)
        callback.invoke(origin, true, false)
        super.onGeolocationPermissionsShowPrompt(origin, callback)
    }
}