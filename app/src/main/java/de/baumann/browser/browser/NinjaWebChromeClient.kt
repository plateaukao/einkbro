package de.baumann.browser.browser

import android.app.Activity
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.view.NinjaWebView

class NinjaWebChromeClient(private val ninjaWebView: NinjaWebView) : WebChromeClient() {
    override fun onCreateWindow(view: WebView, dialog: Boolean, userGesture: Boolean, resultMsg: Message): Boolean {
            val newWebView = WebView(view.context).apply { initWebView(this) }
        newWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val urlString = request?.url?.toString() ?: return false
                return if (urlString.contains("google") || urlString.contains("facebook")) {
                    view?.loadUrl(urlString)
                    true
                } else {
                    request.url?.let { handleWebViewLinks(urlString) } // you can get your target url here
                    false
                }
            }
        }
        newWebView.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                (ninjaWebView.parent as ViewGroup).removeView(window)
            }
        }
        (ninjaWebView.parent as ViewGroup).addView(newWebView)
        val transport = resultMsg.obj as WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()
        return true
    }

    private fun initWebView(webView: WebView)  {
        val webSettings = webView.settings
        val defaultUserAgent = webSettings.userAgentString

        webSettings.userAgentString = defaultUserAgent.replace("wv", "")
        webSettings.setAppCacheEnabled(true)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        val manager = CookieManager.getInstance()
        manager.setAcceptThirdPartyCookies(webView, true)
    }

    override fun onCloseWindow(window: WebView?) {
        (ninjaWebView.parent as ViewGroup).removeView(window)
    }

    private fun handleWebViewLinks(url: String) = ninjaWebView.browserController?.addNewTab(url)

    override fun onProgressChanged(view: WebView, progress: Int) {
        super.onProgressChanged(view, progress)
        ninjaWebView.update(progress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        ninjaWebView.update(title)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        ninjaWebView.browserController?.onShowCustomView(view, callback)
        super.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        ninjaWebView.browserController?.onHideCustomView()
        super.onHideCustomView()
    }

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        ninjaWebView.browserController?.showFileChooser(filePathCallback)
        return true
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        val activity = ninjaWebView.context as Activity
        HelperUnit.grantPermissionsLoc(activity)
        callback.invoke(origin, true, false)
        super.onGeolocationPermissionsShowPrompt(origin, callback)
    }

}