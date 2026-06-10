package info.plateaukao.einkbro.view

import android.graphics.Point
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup

class WebViewTouchSimulator(
    private val webView: EBWebView,
) {
    var isSelectingText = false

    fun clickLinkElement(point: Point) {
        webView.ebookTouchTemporarilyDisabled = true
        simulateClick(point)
        webView.postDelayed({ webView.ebookTouchTemporarilyDisabled = false }, 200)
    }

    fun selectLinkText(point: Point) {
        webView.evaluateJavascript(
            """
            javascript:(function() {
                 var tt = window._touchTarget;
                 if(tt){
                     var hrefAttr = tt.getAttribute("href");
                     tt.removeAttribute("href");
                     window._hrefAttr = hrefAttr;

                     var sel = window.getSelection();
                     sel.removeAllRanges();
                 }
            })()
        """.trimIndent()
        ) {
            webView.postDelayed({ simulateLongClick(point) }, 0)
        }
    }

    fun simulateClick(point: Point) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(
                downTime, downTime, KeyEvent.ACTION_DOWN,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        (webView.parent as ViewGroup).dispatchTouchEvent(downEvent)

        val upEvent =
            MotionEvent.obtain(
                downTime, downTime + 700, KeyEvent.ACTION_UP,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        webView.postDelayed(
            {
                (webView.parent as ViewGroup).dispatchTouchEvent(upEvent)
                downEvent.recycle()
                upEvent.recycle()
            }, 50
        )
    }

    private fun simulateLongClick(point: Point) {
        isSelectingText = true
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(
                downTime, downTime, KeyEvent.ACTION_DOWN,
                (point.x + 20).toFloat(), point.y.toFloat(), 0
            )
        (webView.parent as ViewGroup).dispatchTouchEvent(downEvent)

        val upEvent =
            MotionEvent.obtain(
                downTime, downTime + 700, KeyEvent.ACTION_UP,
                point.x.toFloat(), point.y.toFloat(), 0
            )
        webView.postDelayed(
            {
                (webView.parent as ViewGroup).dispatchTouchEvent(upEvent)
                downEvent.recycle()
                upEvent.recycle()
            }, 700
        )
        webView.postDelayed(
            {
                webView.evaluateJavascript(
                    """
                        var tt = window._touchTarget;
                        if(tt){
                            tt.setAttribute("href", window._hrefAttr);
                        }
                """.trimIndent(), null
                )
                isSelectingText = false
            }, 1000
        )
    }
}
