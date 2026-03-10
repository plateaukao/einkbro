package info.plateaukao.einkbro.browser

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import info.plateaukao.einkbro.view.EBWebView

class EBClickHandler(private val webView: EBWebView) : Handler(Looper.getMainLooper()) {

    var currentMotionEvent: MotionEvent? = null

    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        webView.browserController?.onLongPress(message, currentMotionEvent)
        currentMotionEvent?.recycle()
        currentMotionEvent = null
    }
}