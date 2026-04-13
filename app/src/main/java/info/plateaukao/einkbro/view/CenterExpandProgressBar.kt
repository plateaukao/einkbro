package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

/**
 * Progress bar that draws the filled portion starting from the center and
 * expanding symmetrically toward both ends. Easier to spot on E-ink compared
 * to a rotated standard ProgressBar.
 */
class CenterExpandProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class Orientation { HORIZONTAL, VERTICAL }

    var max: Int = 100
        set(value) {
            field = value.coerceAtLeast(1)
            invalidate()
        }

    var progress: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, max)
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }

    var orientation: Orientation = Orientation.HORIZONTAL
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveDefaultColor(context)
    }

    fun setFillColor(color: Int) {
        if (fillPaint.color != color) {
            fillPaint.color = color
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0) return
        val fraction = progress.toFloat() / max
        val w = width.toFloat()
        val h = height.toFloat()
        if (orientation == Orientation.HORIZONTAL) {
            val fillW = w * fraction
            val left = (w - fillW) / 2f
            canvas.drawRect(left, 0f, left + fillW, h, fillPaint)
        } else {
            val fillH = h * fraction
            val top = (h - fillH) / 2f
            canvas.drawRect(0f, top, w, top + fillH, fillPaint)
        }
    }

    private fun resolveDefaultColor(context: Context): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlNormal, tv, true)
        return tv.data
    }
}
