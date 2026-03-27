package info.plateaukao.einkbro.view

import android.content.Context
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * A SwipeRefreshLayout that properly handles multi-touch gestures.
 *
 * The standard SwipeRefreshLayout may intercept touch events during a single-finger
 * ACTION_MOVE before a second finger lands, causing two-finger swipe gestures to be
 * misinterpreted as pull-to-refresh. This subclass detects multi-touch in
 * dispatchTouchEvent (which is always called, even after interception) and re-routes
 * events to the child so MultitouchListener can handle them.
 */
class MultiTouchSwipeRefreshLayout(context: Context) : SwipeRefreshLayout(context) {
    private var multiTouchDetected = false
    private var hasIntercepted = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                multiTouchDetected = false
                hasIntercepted = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!multiTouchDetected) {
                    multiTouchDetected = true
                    if (hasIntercepted) {
                        // SwipeRefreshLayout already intercepted — reset its drag/spinner state
                        isEnabled = false
                        isEnabled = true
                        // Re-establish touch sequence on the child with a synthetic ACTION_DOWN
                        // so that the FrameLayout creates a touch target for subsequent events.
                        val downEvent = MotionEvent.obtain(ev).apply {
                            action = MotionEvent.ACTION_DOWN
                        }
                        getChildAt(0)?.dispatchTouchEvent(downEvent)
                        downEvent.recycle()
                    }
                }
            }
        }

        if (multiTouchDetected) {
            // Bypass SwipeRefreshLayout interception; dispatch directly to child
            return getChildAt(0)?.dispatchTouchEvent(ev) ?: false
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (multiTouchDetected) return false
        return super.onInterceptTouchEvent(ev).also { intercepted ->
            if (intercepted) hasIntercepted = true
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (multiTouchDetected) return false
        return super.onTouchEvent(ev)
    }
}
