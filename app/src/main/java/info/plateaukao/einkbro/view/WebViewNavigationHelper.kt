package info.plateaukao.einkbro.view

import android.view.KeyEvent
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ViewUnit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class WebViewNavigationHelper(
    private val webView: EBWebView,
    private val config: ConfigManager,
    private val onUpdatePageInfo: (String) -> Unit = {},
) {

    fun isAtTop(): Boolean = if (webView.isVerticalRead) {
        val totalPageCount = webView.horizontalScrollRange() / shiftOffset()
        val currentPage = totalPageCount - (floor(webView.scrollX.toDouble() / shiftOffset()).toInt())
        currentPage == 1
    } else {
        webView.scrollY == 0 && webView.isInnerScrollAtTop
    }

    fun jumpToTop() = if (webView.isVerticalRead) {
        webView.scrollTo(webView.horizontalScrollRange() - shiftOffset(), 0)
    } else {
        webView.scrollTo(0, 0)
        webView.evaluateJavascript("window.__einkbroScrollToTop && window.__einkbroScrollToTop()", null)
    }

    fun jumpToBottom() = if (webView.isVerticalRead) {
        webView.scrollTo(webView.horizontalScrollRange() - shiftOffset(), 0)
    } else {
        webView.scrollTo(0, webView.verticalScrollRange() - shiftOffset())
    }

    fun pageDownWithNoAnimation() = if (webView.isVerticalRead) {
        webView.scrollBy(shiftOffset(), 0)
        webView.scrollX = min(webView.horizontalScrollRange() - webView.width, webView.scrollX)
    } else {
        val nonNullUrl = webView.url.orEmpty()
        if (config.shouldSendPageNavKey(nonNullUrl)) {
            sendPageDownKey()
        } else {
            jsPageScroll(1) { handled ->
                if (!handled) {
                    webView.scrollBy(0, shiftOffset())
                    webView.scrollY = min(webView.verticalScrollRange() - shiftOffset(), webView.scrollY)
                }
            }
        }
    }

    fun pageUpWithNoAnimation() = if (webView.isVerticalRead) {
        webView.scrollBy(-shiftOffset(), 0)
        webView.scrollX = max(0, webView.scrollX)
    } else {
        val nonNullUrl = webView.url.orEmpty()
        if (config.shouldSendPageNavKey(nonNullUrl)) {
            sendPageUpKey()
        } else {
            jsPageScroll(-1) { handled ->
                if (!handled) {
                    webView.scrollBy(0, -shiftOffset())
                    webView.scrollY = max(0, webView.scrollY)
                }
            }
        }
    }

    fun sendPageDownKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_DOWN)

    fun sendPageUpKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_UP)

    fun updatePageInfo() {
        try {
            val pageHeight = shiftOffset()
            if (webView.isVerticalRead) {
                val totalPageCount = webView.horizontalScrollRange() / pageHeight
                val currentPage = totalPageCount - (floor(webView.scrollX.toDouble() / pageHeight).toInt())
                val info = "$currentPage/$totalPageCount"
                onUpdatePageInfo(if (info != "0/0") info else "-/-")
            } else if (webView.innerClientHeight > 0 && webView.verticalScrollRange() <= webView.height + pageHeight / 2) {
                val totalPageCount = webView.innerScrollHeight / webView.innerClientHeight
                val currentPage = ceil((webView.innerScrollTop + 1).toDouble() / webView.innerClientHeight).toInt()
                val info = "$currentPage/$totalPageCount"
                onUpdatePageInfo(if (info != "0/0") info else "-/-")
            } else {
                val totalPageCount = webView.verticalScrollRange() / pageHeight
                val currentPage = ceil((webView.scrollY + 1).toDouble() / pageHeight).toInt()
                val info = "$currentPage/$totalPageCount"
                onUpdatePageInfo(if (info != "0/0") info else "-/-")
            }
        } catch (e: ArithmeticException) {
            onUpdatePageInfo("-/-")
        }
    }

    fun shiftOffset(): Int {
        return if (webView.isVerticalRead) {
            webView.width - 40.dp(webView.context)
        } else {
            val offset = config.touch.pageReservedOffsetInString
            if (offset.endsWith('%')) {
                val offsetPercent = offset.take(offset.length - 1).toInt()
                webView.height - webView.height * offsetPercent / 100
            } else {
                webView.height - offset.toInt().dp(webView.context)
            }
        }
    }

    private fun jsPageScroll(direction: Int, fallback: (Boolean) -> Unit) {
        val offset = config.touch.pageReservedOffsetInString
        val offsetPercent = if (offset.endsWith('%')) offset.take(offset.length - 1).toInt() else 0
        val offsetPx = if (offset.endsWith('%')) 0 else offset.toInt().dp(webView.context)
        webView.evaluateJavascript(
            "window.__einkbroPageScroll && window.__einkbroPageScroll($direction, ${offsetPercent / 100.0}, $offsetPx)"
        ) { result ->
            fallback(result?.trim('"') == "true")
        }
    }

    private fun sendKeyEventToView(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        webView.dispatchKeyEvent(downEvent)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        webView.dispatchKeyEvent(upEvent)
    }
}
