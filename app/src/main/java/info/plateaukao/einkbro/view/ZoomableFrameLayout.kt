package info.plateaukao.einkbro.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ZoomableFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mScaleFactor = 1.0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mMode = MODE_NONE

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    var enableZoom: Boolean = false
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_ZOOM = 2
    }

    init {
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!enableZoom || childCount == 0) return false
                
                val oldScale = mScaleFactor
                mScaleFactor *= detector.scaleFactor
                mScaleFactor = max(1.0f, min(mScaleFactor, 5.0f))
                
                // Calculate effective scale factor ensuring we don't jump when hitting limits
                val effectiveScaleFactor = mScaleFactor / oldScale

                // Adjust translation to zoom around the focus point
                // newTrans = focus - (focus - oldTrans) * scaleFactor
                mPosX = detector.focusX - (detector.focusX - mPosX) * effectiveScaleFactor
                mPosY = detector.focusY - (detector.focusY - mPosY) * effectiveScaleFactor

                applyTransform()
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (!enableZoom) return false
                mMode = MODE_ZOOM
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                mMode = MODE_NONE
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            // We can implement onDoubleTap here if needed in future
        })
    }
    
    // Intercept touch events to steal them from children when panning/zooming
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!enableZoom) return false
        
        // Always intercept if we are already in a special mode
        if (mMode != MODE_NONE) return true
        
        // If zoomed in, we might want to intercept dragging
        if (mScaleFactor > 1.0f) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mLastTouchX = ev.x
                    mLastTouchY = ev.y
                    // Don't intercept DOWN, let child handle it unless we know for sure (which we don't yet)
                    // But we must record coordinates for manual drag detection
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(ev.x - mLastTouchX)
                    val dy = abs(ev.y - mLastTouchY)
                    if (dx > touchSlop || dy > touchSlop) {
                        // Consumed by drag
                        mMode = MODE_DRAG
                        return true
                    }
                }
            }
        }
        
        // Standard scale gesture detector handling usually happens in onTouchEvent,
        // but for interception, we check the pointer count?
        if (ev.pointerCount > 1) {
             // Multi-touch, likely zoom
             return true
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!enableZoom) return super.onTouchEvent(ev)

        scaleGestureDetector.onTouchEvent(ev)
        
        // Handle dragging manually to work with ScaleGestureDetector logic
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (mMode == MODE_NONE && mScaleFactor > 1.0f) {
                   // This might be start of a drag, or just a click.
                   // We already recorded LastTouchX/Y in onIntercept but onTouchEvent might be called directly
                   mLastTouchX = ev.x
                   mLastTouchY = ev.y
                   // We return true to say "we are interested"
                   return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                 if (mMode == MODE_DRAG && mScaleFactor > 1.0f) {
                     val dx = ev.x - mLastTouchX
                     val dy = ev.y - mLastTouchY
                     mPosX += dx
                     mPosY += dy
                     mLastTouchX = ev.x
                     mLastTouchY = ev.y
                     applyTransform()
                     return true
                 } else if (mMode == MODE_NONE && mScaleFactor > 1.0f) {
                     // Check if we should start dragging (if missed by intercept)
                     val dx = abs(ev.x - mLastTouchX)
                     val dy = abs(ev.y - mLastTouchY)
                      if (dx > touchSlop || dy > touchSlop) {
                        mMode = MODE_DRAG
                        return true
                    }
                 }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mMode = MODE_NONE
            }
        }
        
        return true 
    }

    private fun applyTransform() {
        if (childCount > 0) {
            val customView = getChildAt(0)
            
            // Adjust pivots to 0,0 so our translation math works simply
            if (customView.pivotX != 0f || customView.pivotY != 0f) {
                 customView.pivotX = 0f
                 customView.pivotY = 0f
            }
            
            customView.scaleX = mScaleFactor
            customView.scaleY = mScaleFactor
            customView.translationX = mPosX
            customView.translationY = mPosY
        }
    }

    fun resetScale() {
        mScaleFactor = 1.0f
        mPosX = 0f
        mPosY = 0f
        mMode = MODE_NONE
        if (childCount > 0) {
            val customView = getChildAt(0)
            customView.scaleX = 1.0f
            customView.scaleY = 1.0f
            customView.translationX = 0f
            customView.translationY = 0f
        }
    }
}
