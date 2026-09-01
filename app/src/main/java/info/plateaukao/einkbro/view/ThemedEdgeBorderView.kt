package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.view.compose.UiThemeState

/**
 * The toolbar's page-facing edge as a TRUE themed border: the accent edge
 * line in the current border style, theme background filled on the toolbar
 * side of the line, and full transparency on the page side — the same
 * inside-opaque / outside-transparent semantics as the dialog frames (stamp
 * bites and sketch wobble are die-cut, showing the page through them).
 * Overlaps the content flush against the app bar; [edgeAtTop] is true when
 * the toolbar sits below the band (bottom toolbar).
 */
class ThemedEdgeBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var edgeAtTop: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePath = Path()
    private val fillPath = Path()

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    // Edge space: v=0 is the page-facing side of the band, growing toward the
    // toolbar. Mirrored vertically when the toolbar is above the band.
    private fun y(v: Float): Float = if (edgeAtTop) v else height - v

    override fun onDraw(canvas: Canvas) {
        val border = UiThemeState.uiBorder.value
        val accent = ThemedBorders.accentArgb(context)
        val bg = ThemedBorders.baseArgb(context)
        val w = width.toFloat()
        val h = height.toFloat()

        fun fillFrom(v: Float) {
            paint.style = Paint.Style.FILL
            paint.pathEffect = null
            paint.color = bg
            if (edgeAtTop) canvas.drawRect(0f, v, w, h, paint)
            else canvas.drawRect(0f, 0f, w, h - v, paint)
        }

        fun hline(v: Float, stroke: Float, effect: DashPathEffect? = null) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.pathEffect = effect
            paint.color = accent
            canvas.drawLine(0f, y(v), w, y(v), paint)
            paint.pathEffect = null
        }

        when (border) {
            UiBorder.STAMP -> {
                // straight edge with perforation bites cut into the surface,
                // matching the stamp frame's spacing; the page shows through
                val r = dp(3f)
                val edgeV = dp(0.75f)
                val margin = 3f * r
                val span = w - 2f * margin
                val centers = if (span < 2f * r) listOf(w / 2f) else {
                    val n = kotlin.math.max(1, (span / (3.5f * r)).toInt())
                    val step = span / n
                    List(n) { margin + (it + 0.5f) * step }
                }
                edgePath.reset()
                edgePath.moveTo(0f, y(edgeV))
                centers.forEach { cx ->
                    edgePath.lineTo(cx - r, y(edgeV))
                    val rect = RectF(cx - r, y(edgeV) - r, cx + r, y(edgeV) + r)
                    edgePath.arcTo(rect, 180f, if (edgeAtTop) -180f else 180f)
                }
                edgePath.lineTo(w, y(edgeV))
                fillPath.reset()
                fillPath.addPath(edgePath)
                fillPath.lineTo(w, y(h))
                fillPath.lineTo(0f, y(h))
                fillPath.close()
                paint.style = Paint.Style.FILL
                paint.pathEffect = null
                paint.color = bg
                canvas.drawPath(fillPath, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(1.25f)
                paint.color = accent
                canvas.drawPath(edgePath, paint)
            }
            UiBorder.SKETCH -> {
                // wobbly hand-drawn edge; background follows the wobble
                val a = dp(1.5f)
                val step = dp(14f)
                val base = a + dp(0.75f)
                val n = kotlin.math.max(2, (w / step).toInt())
                edgePath.reset()
                for (k in 0..n) {
                    val x = w * k / n
                    val hsh = kotlin.math.sin(k * 12.9898 + w) * 43758.5453
                    val j = if (k == 0 || k == n) 0f
                        else ((hsh - kotlin.math.floor(hsh)).toFloat() * 2f - 1f) * a
                    if (k == 0) edgePath.moveTo(x, y(base + j)) else edgePath.lineTo(x, y(base + j))
                }
                fillPath.reset()
                fillPath.addPath(edgePath)
                fillPath.lineTo(w, y(h))
                fillPath.lineTo(0f, y(h))
                fillPath.close()
                paint.style = Paint.Style.FILL
                paint.pathEffect = null
                paint.color = bg
                canvas.drawPath(fillPath, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(1.5f)
                paint.color = accent
                canvas.drawPath(edgePath, paint)
            }
            UiBorder.PAPER -> {
                fillFrom(dp(0.5f))
                hline(dp(0.5f), dp(1f))
                hline(dp(3.5f), dp(1f))
            }
            UiBorder.CERTIFICATE -> {
                fillFrom(dp(1.25f))
                hline(dp(1.25f), dp(2.5f))
                hline(dp(4.25f), dp(1f))
            }
            UiBorder.DASHED -> {
                fillFrom(dp(0.75f))
                hline(dp(0.75f), dp(1.5f), DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f))
            }
            // NONE, CLASSIC, ROUND, SHARP, STICKER: solid edge at the border's weight
            else -> {
                val stroke = dp(kotlin.math.max(border.widthDp, 1f))
                fillFrom(stroke / 2f)
                hline(stroke / 2f, stroke)
            }
        }
    }
}
