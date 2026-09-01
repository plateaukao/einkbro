package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.view.compose.UiThemeState

/**
 * Page-load progress bar drawn in the theme's border language (dots for
 * stamp, wobble for sketch, double rule for paper/certificate, dashes for
 * dashed; solid otherwise), revealed by the load fraction. [Anchor.CENTER]
 * expands symmetrically from the middle - easier to spot on E-ink - while
 * [Anchor.START] is the classic left/top-anchored bar.
 */
class CenterExpandProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class Orientation { HORIZONTAL, VERTICAL }
    enum class Anchor { CENTER, START }

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

    var anchor: Anchor = Anchor.CENTER
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // Explicit color forced by a caller (dark-mode LIGHTEN behavior);
    // otherwise the theme's accent is resolved live on every draw.
    private var fillColorOverride: Int? = null

    fun setFillColor(color: Int) {
        if (fillColorOverride != color) {
            fillColorOverride = color
            invalidate()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0) return
        val fraction = progress.toFloat() / max
        val w = width.toFloat()
        val h = height.toFloat()
        val length = if (orientation == Orientation.HORIZONTAL) w else h
        val fill = length * fraction

        canvas.save()
        // Reveal the (stable, full-length) pattern by clipping to the loaded
        // fraction, so dots/dashes don't march as progress advances.
        if (orientation == Orientation.HORIZONTAL) {
            val left = if (anchor == Anchor.CENTER) (w - fill) / 2f else 0f
            canvas.clipRect(left, 0f, left + fill, h)
        } else {
            val top = if (anchor == Anchor.CENTER) (h - fill) / 2f else 0f
            canvas.clipRect(0f, top, w, top + fill)
        }
        drawThemedLine(canvas)
        canvas.restore()
    }

    private fun line(canvas: Canvas, cross: Float, stroke: Float) {
        paint.strokeWidth = stroke
        if (orientation == Orientation.HORIZONTAL) {
            canvas.drawLine(0f, cross, width.toFloat(), cross, paint)
        } else {
            canvas.drawLine(cross, 0f, cross, height.toFloat(), paint)
        }
    }

    private fun drawThemedLine(canvas: Canvas) {
        val border = UiThemeState.uiBorder.value
        paint.style = Paint.Style.STROKE
        paint.pathEffect = null
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = fillColorOverride ?: ThemedBorders.accentArgb(context)
        val length = if (orientation == Orientation.HORIZONTAL) width.toFloat() else height.toFloat()
        val breadth = if (orientation == Orientation.HORIZONTAL) height.toFloat() else width.toFloat()
        when (border) {
            UiBorder.DASHED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
                line(canvas, breadth / 2f, dp(1.5f))
            }
            // perforation row, echoing the stamp frame's bite holes
            UiBorder.STAMP -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(0.1f, dp(6f)), 0f)
                paint.strokeCap = Paint.Cap.ROUND
                line(canvas, breadth / 2f, dp(2.5f))
            }
            UiBorder.PAPER -> {
                line(canvas, breadth / 2f - dp(1.5f), dp(1f))
                line(canvas, breadth / 2f + dp(1.5f), dp(1f))
            }
            UiBorder.CERTIFICATE -> {
                line(canvas, dp(1f), dp(2f))
                line(canvas, breadth - dp(0.5f), dp(1f))
            }
            UiBorder.SKETCH -> {
                // deterministic wobble, same recipe as the themed separators
                val amplitude = dp(1.2f)
                val step = dp(12f)
                val mid = breadth / 2f
                val n = kotlin.math.max(2, (length / step).toInt())
                path.reset()
                for (k in 0..n) {
                    val t = k.toFloat() / n
                    val hsh = kotlin.math.sin(k * 12.9898 + length) * 43758.5453
                    val j =
                        if (k == 0 || k == n) 0f
                        else ((hsh - kotlin.math.floor(hsh)).toFloat() * 2f - 1f) * amplitude
                    val along = t * length
                    val x = if (orientation == Orientation.HORIZONTAL) along else mid + j
                    val y = if (orientation == Orientation.HORIZONTAL) mid + j else along
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                paint.strokeWidth = dp(1.5f)
                canvas.drawPath(path, paint)
            }
            // NONE, CLASSIC, ROUND, SHARP, STICKER: solid at the border's weight
            else -> line(canvas, breadth / 2f, dp(kotlin.math.max(border.widthDp, 1.5f)))
        }
        paint.pathEffect = null
    }
}
