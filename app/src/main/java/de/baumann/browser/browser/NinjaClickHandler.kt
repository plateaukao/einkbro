package de.baumann.browser.browser

import android.os.Handler
import android.os.Message
import de.baumann.browser.view.NinjaWebView

class NinjaClickHandler(private val webView: NinjaWebView) : Handler() {
    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        webView.browserController?.onLongPress(message.data.getString("url"))
    }
}