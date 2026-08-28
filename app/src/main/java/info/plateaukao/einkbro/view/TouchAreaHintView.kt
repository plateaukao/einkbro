package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * Touch-area overlay that hints its bounds with small rounded corners instead
 * of a full dashed rectangle, so the page stays uncluttered.
 *
 * Only corners that lie inside the screen are drawn: a corner sitting on (or
 * past) a screen edge is skipped because the edge already bounds the area.
 * The decision is made at draw time from the view's current position, so an
 * area the user drags away from the bottom edge grows its bottom corners.
 *
 * The stroke is a dark core over a light halo so it reads on both light and
 * dark page backgrounds, matching the contrast trick of the old dashed border.
 */
class TouchAreaHintView(context: Context) : View(context) {
    companion object {
        private const val RADIUS_DP = 10f
        private const val TAIL_DP = 4f
        private const val CORE_WIDTH_DP = 1.5f
        private const val HALO_WIDTH_DP = 3.5f
        private const val CORE_COLOR = 0xC0000000.toInt()
        private const val HALO_COLOR = 0xC0FFFFFF.toInt()
    }

    var hintVisible = true
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val radius = RADIUS_DP * density
    private val tail = TAIL_DP * density

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = CORE_WIDTH_DP * density
        color = CORE_COLOR
    }
    private val haloPaint = Paint(corePaint).apply {
        strokeWidth = HALO_WIDTH_DP * density
        color = HALO_COLOR
    }

    private val path = Path()
    private val arcRect = RectF()

    // Moving the view via y/translationY only updates render-node properties;
    // it doesn't redraw, so the corner set must be recomputed explicitly.
    override fun setTranslationY(translationY: Float) {
        super.setTranslationY(translationY)
        invalidate()
    }

    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hintVisible) return
        if (!buildPath()) return
        canvas.drawPath(path, haloPaint)
        canvas.drawPath(path, corePaint)
    }

    /** Builds the corner path; returns false when no corner is on screen. */
    private fun buildPath(): Boolean {
        val parent = parent as? View ?: return false
        val onLeftEdge = x <= 0f
        val onTopEdge = y <= 0f
        val onRightEdge = x + width >= parent.width
        val onBottomEdge = y + height >= parent.height

        // Keep the whole stroke inside the view bounds so nothing gets clipped.
        val inset = haloPaint.strokeWidth / 2
        val l = inset
        val t = inset
        val r = width - inset
        val b = height - inset
        val rad = radius.coerceAtMost(minOf(r - l, b - t) / 2)
        val d = rad * 2

        path.rewind()
        if (!onTopEdge && !onLeftEdge) {
            path.moveTo(l, t + rad + tail)
            path.lineTo(l, t + rad)
            arcRect.set(l, t, l + d, t + d)
            path.arcTo(arcRect, 180f, 90f)
            path.lineTo(l + rad + tail, t)
        }
        if (!onTopEdge && !onRightEdge) {
            path.moveTo(r - rad - tail, t)
            path.lineTo(r - rad, t)
            arcRect.set(r - d, t, r, t + d)
            path.arcTo(arcRect, 270f, 90f)
            path.lineTo(r, t + rad + tail)
        }
        if (!onBottomEdge && !onRightEdge) {
            path.moveTo(r, b - rad - tail)
            path.lineTo(r, b - rad)
            arcRect.set(r - d, b - d, r, b)
            path.arcTo(arcRect, 0f, 90f)
            path.lineTo(r - rad - tail, b)
        }
        if (!onBottomEdge && !onLeftEdge) {
            path.moveTo(l + rad + tail, b)
            path.lineTo(l + rad, b)
            arcRect.set(l, b - d, l + d, b)
            path.arcTo(arcRect, 90f, 90f)
            path.lineTo(l, b - rad - tail)
        }
        return !path.isEmpty
    }
}
