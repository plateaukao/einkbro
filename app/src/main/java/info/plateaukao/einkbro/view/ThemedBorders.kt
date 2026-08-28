package info.plateaukao.einkbro.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import info.plateaukao.einkbro.preference.BorderStyle
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.ThemePalette
import info.plateaukao.einkbro.preference.ThemeStyle
import info.plateaukao.einkbro.preference.gradientSpec
import info.plateaukao.einkbro.preference.palette
import info.plateaukao.einkbro.view.compose.UiThemeState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runtime replacements for the background_with_border(_margin) /
 * selected_border_bg XML drawables, so dialog window frames and floating
 * panels follow the selected UiTheme accent and ThemeStyle immediately —
 * no activity restart and no per-theme XML theme overlays needed. The XML
 * drawables resolve their stroke from the static activity theme
 * (?android:colorControlNormal), which cannot react to the theme preference.
 */
object ThemedBorders : KoinComponent {
    private val config: ConfigManager by inject()

    // Mirrors isAppInDarkTheme(): the Dark mode setting wins; Follow system
    // falls back to the system night configuration.
    private fun isNight(context: Context): Boolean = when (config.display.darkMode) {
        DarkMode.FORCE_ON -> true
        DarkMode.DISABLED -> false
        DarkMode.SYSTEM ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun Context.dp(value: Float): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    // Read the live UiThemeState (kept in sync with config) instead of the
    // persisted preferences, so drag previews (color wheel, gradient dial)
    // retint window frames instantly before the value is committed.
    private fun currentPalette(): ThemePalette =
        UiThemeState.current.value.palette(UiThemeState.customColor.value)

    /** Start/end colors for a gradient style fill, per display mode. */
    private fun gradientColors(context: Context, start: Float, end: Float): IntArray {
        val palette = currentPalette()
        val level = UiThemeState.gradientLevel.value
        val (base, accent) = when {
            UiThemeState.inverted.value -> palette.onBackground to palette.accentDark
            isNight(context) -> Color.Black to palette.accentDark
            else -> palette.background to palette.accent
        }
        return intArrayOf(
            lerp(base, accent, (start * level / 100f).coerceIn(0f, 0.9f)).toArgb(),
            lerp(base, accent, (end * level / 100f).coerceIn(0f, 0.9f)).toArgb(),
        )
    }

    // GradientDrawable supports only the 8 axis/diagonal orientations, so the
    // free angle snaps to the nearest 45 degrees for window frames/panels.
    private fun drawableOrientation(): GradientDrawable.Orientation {
        val snapped = (((UiThemeState.gradientAngle.value % 360) + 360) % 360 + 22) / 45 % 8
        return when (snapped) {
            0 -> GradientDrawable.Orientation.LEFT_RIGHT
            1 -> GradientDrawable.Orientation.TL_BR
            2 -> GradientDrawable.Orientation.TOP_BOTTOM
            3 -> GradientDrawable.Orientation.TR_BL
            4 -> GradientDrawable.Orientation.RIGHT_LEFT
            5 -> GradientDrawable.Orientation.BR_TL
            6 -> GradientDrawable.Orientation.BOTTOM_TOP
            else -> GradientDrawable.Orientation.BL_TR
        }
    }

    /** Accent-tinted fill used instead of a stroke for BorderStyle.NONE. */
    fun tonalFillArgb(context: Context): Int {
        val palette = currentPalette()
        return when {
            UiThemeState.inverted.value ->
                lerp(palette.onBackground, palette.accentDark, 0.16f).toArgb()
            isNight(context) -> lerp(Color.Black, palette.accentDark, 0.16f).toArgb()
            else -> lerp(palette.background, palette.accent, 0.10f).toArgb()
        }
    }

    private fun box(
        context: Context,
        style: ThemeStyle = UiThemeState.uiStyle.value.style,
        forceSolid: Boolean = false,
    ): Drawable {
        val inverted = UiThemeState.inverted.value
        val night = inverted || isNight(context)
        val palette = currentPalette()
        val accent = (if (night) palette.accentDark else palette.accent).toArgb()
        val fill = when {
            style.borderStyle == BorderStyle.NONE -> tonalFillArgb(context)
            inverted -> palette.onBackground.toArgb()
            night -> android.graphics.Color.BLACK
            else -> palette.background.toArgb()
        }
        val radius = context.dp(style.frameRadiusDp).toFloat()
        val strokeWidth = context.dp(style.borderWidthDp)

        val gradientSpec = style.borderStyle.gradientSpec()
        fun single(withStroke: Boolean) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            if (gradientSpec != null) {
                orientation = drawableOrientation()
                colors = gradientColors(context, gradientSpec.first, gradientSpec.second)
            } else {
                setColor(fill)
            }
            if (withStroke && (gradientSpec == null || gradientSpec.third)) {
                if (style.borderStyle == BorderStyle.DASHED && !forceSolid) {
                    setStroke(
                        strokeWidth, accent,
                        context.dp(5f).toFloat(), context.dp(4f).toFloat(),
                    )
                } else {
                    setStroke(strokeWidth, accent)
                }
            }
        }

        return when {
            style.borderStyle == BorderStyle.STAMP && !forceSolid ->
                StampDrawable(fill, accent, strokeWidth.toFloat(), context.dp(4f).toFloat())
            style.borderStyle == BorderStyle.NONE -> single(withStroke = false)
            style.borderStyle == BorderStyle.DOUBLE && !forceSolid -> {
                val layers = LayerDrawable(arrayOf(single(true), single(true)))
                val inset = context.dp(3f)
                layers.setLayerInset(1, inset, inset, inset, inset)
                layers
            }
            else -> single(withStroke = true)
        }
    }

    /** background_with_border: bordered box for floating panels/buttons. */
    fun panel(context: Context): Drawable = box(context)

    /**
     * Content padding a dialog window needs so the frame border and rounded
     * corners never overlap (crop) the content: reported as drawable padding,
     * which the window adds around its content — the dialog expands instead
     * of losing inner space.
     */
    private fun contentPad(context: Context, style: ThemeStyle): Int {
        val stroke = if (style.borderStyle == BorderStyle.NONE) 0f else style.borderWidthDp
        val doubleExtra =
            if (style.borderStyle == BorderStyle.DOUBLE) 3f + style.borderWidthDp else 0f
        val stampExtra = if (style.borderStyle == BorderStyle.STAMP) 5f else 0f
        val cornerAllowance = style.frameRadiusDp * 0.3f
        return context.dp(stroke + doubleExtra + stampExtra + cornerAllowance)
    }

    private fun withContentPadding(context: Context, drawable: Drawable): Drawable {
        val style = UiThemeState.uiStyle.value.style
        val pad = contentPad(context, style)
        return LayerDrawable(arrayOf(drawable)).apply { setPadding(pad, pad, pad, pad) }
    }

    /** Like [panel] but for use as a dialog *window* background: reports the
     * border as padding so the window grows instead of cropping content. */
    fun windowPanel(context: Context): Drawable =
        withContentPadding(context, box(context))

    /** selected_border_bg: double border marking a selected/toggled state. */
    fun selectedPanel(context: Context): Drawable {
        val layers = LayerDrawable(
            arrayOf(box(context, forceSolid = true), box(context, forceSolid = true))
        )
        val inset = context.dp(3f)
        layers.setLayerInset(1, inset, inset, inset, inset)
        return layers
    }

    /** background_with_border_margin: dialog window frame (16dp outer margin). */
    fun dialogFrame(context: Context): Drawable {
        val margin = context.dp(16f)
        return InsetDrawable(withContentPadding(context, box(context)), margin, margin, margin, margin)
    }
}

/**
 * Postage-stamp drawable: straight edges with semicircular perforation
 * bites; corners stay square. Used for dialog window frames and panels
 * when the STAMP style is selected.
 */
private class StampDrawable(
    private val fillColor: Int,
    private val strokeColor: Int,
    private val strokeWidth: Float,
    private val scallopRadius: Float,
) : Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        rebuildPath()
    }

    private fun rebuildPath() {
        val r = scallopRadius
        val inset = strokeWidth
        val left = bounds.left + inset
        val top = bounds.top + inset
        val right = bounds.right - inset
        val bottom = bounds.bottom - inset
        val w = right - left
        val h = bottom - top
        path.reset()
        if (w <= 0 || h <= 0) return

            fun biteCenters(edge: Float): List<Float> {
            // keep a long straight run at each corner (about 3 bite radii)
            val margin = 3f * r
            val span = edge - 2f * margin
            if (span < 2f * r) return listOf(edge / 2f)
            val n = kotlin.math.max(1, (span / (3.5f * r)).toInt())
            val step = span / n
            return List(n) { margin + (it + 0.5f) * step }
        }

        fun arc(cx: Float, cy: Float, startDeg: Float) {
            path.arcTo(
                android.graphics.RectF(cx - r, cy - r, cx + r, cy + r),
                startDeg, -180f,
            )
        }

        path.moveTo(left, top)
        biteCenters(w).forEach { c -> path.lineTo(left + c - r, top); arc(left + c, top, 180f) }
        path.lineTo(right, top)
        biteCenters(h).forEach { c -> path.lineTo(right, top + c - r); arc(right, top + c, 270f) }
        path.lineTo(right, bottom)
        biteCenters(w).map { w - it }.forEach { c ->
            path.lineTo(left + c + r, bottom); arc(left + c, bottom, 0f)
        }
        path.lineTo(left, bottom)
        biteCenters(h).map { h - it }.forEach { c ->
            path.lineTo(left, top + c + r); arc(left, top + c, 90f)
        }
        path.close()
    }

    override fun draw(canvas: android.graphics.Canvas) {
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = fillColor
        canvas.drawPath(path, paint)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = strokeColor
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/**
 * Applies the themed frame to a framework dialog (AlertDialog etc.) whose
 * XML style would otherwise pin a static black border. Call after create().
 */
fun <T : android.app.Dialog> T.withThemedFrame(): T = apply {
    window?.setBackgroundDrawable(ThemedBorders.windowPanel(context))
}
