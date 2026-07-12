package info.plateaukao.einkbro.view

import android.view.KeyEvent
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ViewUnit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WebViewNavigationHelper(
    private val webView: EBWebView,
    private val config: ConfigManager,
    private val onUpdatePageInfo: (String) -> Unit = {},
) {

    fun isAtTop(): Boolean = if (webView.isVerticalRead) {
        currentVerticalPage() <= 0
    } else if (webView.isTwoColumnReaderOn) {
        webView.scrollX == 0
    } else {
        webView.scrollY == 0 && webView.isInnerScrollAtTop
    }

    fun jumpToTop() = if (webView.isVerticalRead) {
        scrollToVerticalPage(0)
    } else if (webView.isTwoColumnReaderOn) {
        webView.scrollTo(0, 0)
    } else {
        webView.scrollTo(0, 0)
        webView.evaluateJavascript("window.__einkbroScrollToTop && window.__einkbroScrollToTop()", null)
    }

    fun jumpToBottom() = if (webView.isVerticalRead) {
        // End of a vertical-rl document is its leftmost edge (last column).
        webView.scrollTo(0, 0)
    } else if (webView.isTwoColumnReaderOn) {
        webView.scrollTo(webView.horizontalScrollRange() - webView.width, 0)
    } else {
        webView.scrollTo(0, webView.verticalScrollRange() - shiftOffset())
    }

    fun pageDownWithNoAnimation() = if (webView.isVerticalRead) {
        // +x = toward the reading start; callers flip direction in vertical mode.
        scrollToVerticalPage(currentVerticalPage() - 1)
    } else if (webView.isTwoColumnReaderOn) {
        // One "page" is exactly one viewport of two columns (the CSS sizes the
        // margins/gap so pages tile by viewport width). Jump to the computed
        // page boundary instead of scrollBy so sub-pixel layout rounding can't
        // accumulate into visible drift over many page turns.
        scrollToTwoColumnPage(currentTwoColumnPage() + 1)
    } else {
        jsPageScroll(1) { handled ->
            if (!handled) {
                webView.scrollBy(0, shiftOffset())
                webView.scrollY = min(webView.verticalScrollRange() - shiftOffset(), webView.scrollY)
            }
        }
    }

    fun pageUpWithNoAnimation() = if (webView.isVerticalRead) {
        scrollToVerticalPage(currentVerticalPage() + 1)
    } else if (webView.isTwoColumnReaderOn) {
        scrollToTwoColumnPage(currentTwoColumnPage() - 1)
    } else {
        jsPageScroll(-1) { handled ->
            if (!handled) {
                webView.scrollBy(0, -shiftOffset())
                webView.scrollY = max(0, webView.scrollY)
            }
        }
    }

    fun sendPageDownKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_DOWN)

    fun sendPageUpKey() = sendKeyEventToView(KeyEvent.KEYCODE_PAGE_UP)

    fun updatePageInfo() {
        try {
            val pageHeight = shiftOffset()
            if (webView.isVerticalRead) {
                val step = verticalPageStepPx()
                if (step <= 0) {
                    onUpdatePageInfo("-/-")
                    return
                }
                val totalPageCount = ceil(verticalPageAnchorX() / step).toInt() + 1
                val currentPage = (currentVerticalPage() + 1).coerceIn(1, totalPageCount)
                onUpdatePageInfo("$currentPage/$totalPageCount")
            } else if (webView.isTwoColumnReaderOn) {
                val totalPageCount = ceil(webView.horizontalScrollRange().toDouble() / webView.width).toInt()
                val currentPage = floor(webView.scrollX.toDouble() / webView.width).toInt() + 1
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

    // --- Vertical-read pagination ---------------------------------------
    // Pages are anchored at the document's right edge (the reading start) and
    // advance by an exact multiple of the rendered line advance, so a page
    // turn never slices a vertical text line at the viewport edge. Absolute
    // anchoring (instead of scrollBy from wherever we are) keeps every page on
    // the same line grid even after clamping at the document's end.

    private fun verticalPageAnchorX(): Int =
        max(0, webView.horizontalScrollRange() - webView.width)

    private fun verticalPageStepPx(): Double {
        val usable = shiftOffset().toDouble()
        val line = webView.verticalLineAdvancePx.toDouble()
        return if (line > 1.0 && line < usable) floor(usable / line) * line else usable
    }

    private fun currentVerticalPage(): Int {
        val step = verticalPageStepPx()
        if (step <= 0) return 0
        return ((verticalPageAnchorX() - webView.scrollX) / step).roundToInt()
    }

    private fun scrollToVerticalPage(page: Int) {
        val anchor = verticalPageAnchorX()
        val x = (anchor - page.coerceAtLeast(0) * verticalPageStepPx())
            .roundToInt().coerceIn(0, anchor)
        webView.scrollTo(x, 0)
    }

    private fun currentTwoColumnPage(): Int =
        Math.round(webView.scrollX.toDouble() / webView.width).toInt()

    private fun scrollToTwoColumnPage(page: Int) {
        val maxScrollX = max(0, webView.horizontalScrollRange() - webView.width)
        webView.scrollTo((page * webView.width).coerceIn(0, maxScrollX), 0)
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
