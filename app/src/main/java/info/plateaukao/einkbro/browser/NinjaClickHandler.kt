package info.plateaukao.einkbro.browser

import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import info.plateaukao.einkbro.view.NinjaWebView

class NinjaClickHandler(private val webView: NinjaWebView) : Handler() {

    var currentMotionEvent: MotionEvent? = null

    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        webView.browserController?.onLongPress(message, currentMotionEvent)
    }
}