package de.baumann.browser.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import de.baumann.browser.browser.NinjaGestureListener
import de.baumann.browser.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs
import kotlin.math.max

open class MultitouchListener(
    context: Context,
    webView: NinjaWebView,
    private val touchCount: Int = 2
) : View.OnTouchListener, KoinComponent {

    private var startPoint0: Point = Point(0, 0)
    private var startPoint1: Point = Point(0, 0)
    private var endPoint0: Point = Point(0, 0)
    private var endPoint1: Point = Point(0, 0)
    private var inSwipe = false

    private val config: ConfigManager by inject()

    private val gestureDetector: GestureDetector = GestureDetector(context, NinjaGestureListener(webView))

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (!config.isMultitouchEnabled) return gestureDetector.onTouchEvent(event)

        if (event.pointerCount != touchCount) return gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                startPoint0 = Point(event.getX(0).toInt(), event.getY(0).toInt())
                startPoint1 = Point(event.getX(1).toInt(), event.getY(1).toInt())
                inSwipe = true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (inSwipe) {
                    val offSetX = endPoint1.x - startPoint1.x
                    val offSetY = endPoint1.y - startPoint1.y
                    Log.i("SWIPE", "offsetX: $offSetX, offsetY: $offSetY")

                    if (isValidSwipe(offSetX, offSetY)) {
                        if (abs(offSetX) > abs(offSetY)) {
                            if (isSameXDirection()) {
                                if (offSetX > 0) onSwipeRight() else onSwipeLeft()
                            }
                        } else {
                            if (isSameYDirection()) {
                                if (offSetY > 0) onSwipeBottom() else onSwipeTop()
                            }
                        }
                        inSwipe = false
                    }
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (inSwipe) {
                    endPoint0 = Point(event.getX(0).toInt(), event.getY(0).toInt())
                    endPoint1 = Point(event.getX(1).toInt(), event.getY(1).toInt())
                }
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    private fun isValidSwipe(offSetX: Int, offSetY: Int) =
            max(abs(offSetX), abs(offSetY)) > SWIPE_THRESHOLD

    private fun isSameXDirection(): Boolean {
        val point0Diff = endPoint0.x - startPoint0.x
        val point1Diff = endPoint1.x - startPoint1.x
        return (point0Diff > 0 && point1Diff > 0) || (point0Diff < 0 && point1Diff < 0)
    }

    private fun isSameYDirection(): Boolean {
        val point0Diff = endPoint0.y - startPoint0.y
        val point1Diff = endPoint1.y - startPoint1.y
        return (point0Diff > 0 && point1Diff > 0) || (point0Diff < 0 && point1Diff < 0)
    }

    open fun onSwipeRight() {}

    open fun onSwipeLeft() {}

    open fun onSwipeTop() {}

    open fun onSwipeBottom() {}

    companion object {
        private const val SWIPE_THRESHOLD = 100
    }
}