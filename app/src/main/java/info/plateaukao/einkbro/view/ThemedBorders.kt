package info.plateaukao.einkbro.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.GRADIENT_END_FRACTION
import info.plateaukao.einkbro.preference.GRADIENT_START_FRACTION
import info.plateaukao.einkbro.preference.ThemePalette
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.preference.UiFill
import info.plateaukao.einkbro.preference.isPattern
import info.plateaukao.einkbro.preference.palette
import info.plateaukao.einkbro.view.compose.UiThemeState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runtime replacements for the background_with_border(_margin) /
 * selected_border_bg XML drawables, so dialog window frames and floating
 * panels follow the selected theme color, border, and fill immediately —
 * no activity restart and no per-theme XML theme overlays needed. Reads
 * the live UiThemeState (kept in sync with config) so drag previews
 * (color wheel, gradient dial) retint window frames instantly.
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

    private fun currentPalette(): ThemePalette =
        UiThemeState.current.value.palette(UiThemeState.customColor.value)

    private fun baseAndAccent(context: Context): Pair<Color, Color> {
        val palette = currentPalette()
        return when {
            UiThemeState.inverted.value -> palette.onBackground to palette.accentDark
            isNight(context) -> Color.Black to palette.accentDark
            else -> palette.background to palette.accent
        }
    }

    /** The theme's accent in the current light/dark/inverted mode. */
    fun accentArgb(context: Context): Int = baseAndAccent(context).second.toArgb()

    /** The theme's background in the current light/dark/inverted mode. */
    fun baseArgb(context: Context): Int = baseAndAccent(context).first.toArgb()

    /** Accent-tinted fill used for UiFill.TONAL. */
    fun tonalFillArgb(context: Context): Int {
        val (base, accent) = baseAndAccent(context)
        val night = UiThemeState.inverted.value || isNight(context)
        return lerp(base, accent, if (night) 0.16f else 0.10f).toArgb()
    }

    /** Start/end colors for the gradient fill, scaled by the user's level. */
    private fun gradientColors(context: Context): IntArray {
        val (base, accent) = baseAndAccent(context)
        val level = UiThemeState.gradientLevel.value
        fun f(fraction: Float) = (fraction * level / 100f).coerceIn(0f, 0.9f)
        return intArrayOf(
            lerp(base, accent, f(GRADIENT_START_FRACTION)).toArgb(),
            lerp(base, accent, f(GRADIENT_END_FRACTION)).toArgb(),
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

    /** Repeating-tile shader for the pattern fills, in the current colors. */
    internal fun patternShader(context: Context, fill: UiFill): android.graphics.BitmapShader {
        val (base, accent) = baseAndAccent(context)
        val night = UiThemeState.inverted.value || isNight(context)
        // faint lines so content on top stays readable
        val lineArgb = lerp(base, accent, if (night) 0.16f else 0.12f).toArgb()
        val baseArgb = base.toArgb()
        val p = context.dp(
            when (fill) {
                UiFill.RULED -> 18f
                UiFill.STRIPES, UiFill.DOTS -> 14f
                else -> 16f
            }
        ).coerceAtLeast(4)
        val bmp = android.graphics.Bitmap.createBitmap(p, p, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(baseArgb)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = lineArgb
            strokeWidth = context.dp(if (fill == UiFill.STRIPES) 1.5f else 1f).toFloat()
        }
        val pf = p.toFloat()
        when (fill) {
            UiFill.STRIPES -> {
                c.drawLine(-pf, pf * 2f, pf * 2f, -pf, paint)
                c.drawLine(0f, pf, pf, 0f, paint)
                c.drawLine(0f, pf * 2f, pf * 2f, 0f, paint)
            }
            UiFill.DOTS -> c.drawCircle(pf / 2f, pf / 2f, context.dp(1.5f).toFloat(), paint)
            UiFill.GRAPH -> {
                c.drawLine(0.5f, 0f, 0.5f, pf, paint)
                c.drawLine(0f, 0.5f, pf, 0.5f, paint)
            }
            UiFill.RULED -> c.drawLine(0f, 0.5f, pf, 0.5f, paint)
            UiFill.CROSSHATCH -> {
                c.drawLine(-pf, pf * 2f, pf * 2f, -pf, paint)
                c.drawLine(0f, pf, pf, 0f, paint)
                c.drawLine(0f, 0f, pf, pf, paint)
                c.drawLine(-pf, -pf, pf * 2f, pf * 2f, paint)
            }
            else -> Unit
        }
        return android.graphics.BitmapShader(
            bmp,
            android.graphics.Shader.TileMode.REPEAT,
            android.graphics.Shader.TileMode.REPEAT,
        )
    }

    /** Solid fill color when the fill is not a gradient. */
    private fun flatFillArgb(context: Context, fill: UiFill): Int = when (fill) {
        UiFill.TONAL -> tonalFillArgb(context)
        else -> baseAndAccent(context).first.toArgb()
    }

    // Applies the resolved fill (flat, tonal, or gradient) to a box drawable.
    private fun GradientDrawable.applyFill(context: Context, fill: UiFill) {
        if (fill == UiFill.GRADIENT) {
            orientation = drawableOrientation()
            colors = gradientColors(context)
        } else {
            setColor(flatFillArgb(context, fill))
        }
    }

    private fun box(
        context: Context,
        forceSolid: Boolean = false,
    ): Drawable {
        val border = UiThemeState.uiBorder.value
        val fill = UiThemeState.uiFill.value
        val accent = baseAndAccent(context).second.toArgb()
        val radius = context.dp(border.frameRadiusDp).toFloat()
        val strokeWidth = context.dp(maxOf(border.widthDp, 1f))

        fun filledBox(withStroke: Boolean, dashed: Boolean = false): Drawable =
            if (fill.isPattern()) {
                PatternBoxDrawable(
                    context, fill, radius,
                    strokeColor = if (withStroke) accent else null,
                    strokeWidth = strokeWidth.toFloat(),
                    dashed = dashed,
                    dashOn = context.dp(5f).toFloat(),
                    dashOff = context.dp(4f).toFloat(),
                )
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    applyFill(context, fill)
                    if (withStroke) {
                        if (dashed) {
                            setStroke(
                                strokeWidth, accent,
                                context.dp(5f).toFloat(), context.dp(4f).toFloat(),
                            )
                        } else {
                            setStroke(strokeWidth, accent)
                        }
                    }
                }
            }

        if (forceSolid) return filledBox(withStroke = true)

        return when (border) {
            // with no fill either, keep a faint hairline so a dialog window
            // still separates from the page behind it
            UiBorder.NONE ->
                if (fill == UiFill.NONE) {
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        applyFill(context, fill)
                        setStroke(
                            context.dp(1f),
                            android.graphics.Color.argb(
                                80,
                                android.graphics.Color.red(accent),
                                android.graphics.Color.green(accent),
                                android.graphics.Color.blue(accent),
                            ),
                        )
                    }
                } else {
                    filledBox(withStroke = false)
                }
            UiBorder.CLASSIC, UiBorder.ROUND, UiBorder.SHARP -> filledBox(withStroke = true)
            UiBorder.DASHED -> filledBox(withStroke = true, dashed = true)
            UiBorder.PAPER -> {
                val layers = LayerDrawable(arrayOf(filledBox(true), filledBox(true)))
                val inset = context.dp(3f)
                layers.setLayerInset(1, inset, inset, inset, inset)
                layers
            }
            UiBorder.CERTIFICATE -> {
                val inner = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(android.graphics.Color.TRANSPARENT)
                    setStroke(context.dp(1f), accent)
                }
                val inset = strokeWidth + context.dp(4f)
                LayerDrawable(arrayOf(filledBox(true), inner)).apply {
                    setLayerInset(1, inset, inset, inset, inset)
                }
            }
            UiBorder.STICKER -> {
                val off = context.dp(4f)
                val shadow = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(accent)
                }
                LayerDrawable(arrayOf(shadow, filledBox(true))).apply {
                    setLayerInset(0, off, off, 0, 0)
                    setLayerInset(1, 0, 0, off, off)
                }
            }
            UiBorder.STAMP -> StampDrawable(
                context, fill, accent, strokeWidth.toFloat(), context.dp(4f).toFloat(),
            )
            UiBorder.SKETCH -> SketchDrawable(
                context, fill, accent, strokeWidth.toFloat(),
                context.dp(2.5f).toFloat(), context.dp(14f).toFloat(),
            )
        }
    }

    // Fill paint setup shared by the path drawables (stamp, sketch): flat,
    // tonal, or a linear-gradient shader at the user's angle.
    internal fun setupFillPaint(
        context: Context,
        fill: UiFill,
        paint: android.graphics.Paint,
        bounds: android.graphics.Rect,
    ) {
        paint.shader = null
        if (fill.isPattern()) {
            paint.shader = patternShader(context, fill)
            return
        }
        if (fill == UiFill.GRADIENT) {
            val colors = gradientColors(context)
            val rad = Math.toRadians(UiThemeState.gradientAngle.value.toDouble())
            val dx = kotlin.math.cos(rad).toFloat()
            val dy = kotlin.math.sin(rad).toFloat()
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val halfLen =
                (kotlin.math.abs(dx) * bounds.width() + kotlin.math.abs(dy) * bounds.height()) / 2f
            paint.shader = android.graphics.LinearGradient(
                cx - dx * halfLen, cy - dy * halfLen,
                cx + dx * halfLen, cy + dy * halfLen,
                colors[0], colors[1], android.graphics.Shader.TileMode.CLAMP,
            )
        } else {
            paint.color = flatFillArgb(context, fill)
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
    private fun contentPad(context: Context): Int {
        val border = UiThemeState.uiBorder.value
        val extra = when (border) {
            UiBorder.STAMP -> 5f
            UiBorder.SKETCH -> 3f
            UiBorder.CERTIFICATE -> 8f
            UiBorder.STICKER -> 5f
            UiBorder.PAPER -> 3f + border.widthDp
            else -> 0f
        }
        val cornerAllowance = border.frameRadiusDp * 0.3f
        // breathing room so text/labels never sit right against the frame
        val breathing = 4f
        return context.dp(border.widthDp + extra + cornerAllowance + breathing)
    }

    private fun withContentPadding(context: Context, drawable: Drawable): Drawable {
        val pad = contentPad(context)
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

/** Postage-stamp frame: straight edges with semicircular perforation bites;
 * corners stay square with long straight runs. */
private class StampDrawable(
    private val context: Context,
    private val fill: UiFill,
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
        ThemedBorders.setupFillPaint(context, fill, paint, bounds)
        canvas.drawPath(path, paint)
        paint.shader = null
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

/** Hand-drawn frame: jittered perimeter polyline, deterministic per size. */
private class SketchDrawable(
    private val context: Context,
    private val fill: UiFill,
    private val strokeColor: Int,
    private val strokeWidth: Float,
    private val amplitude: Float,
    private val step: Float,
) : Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        rebuildPath()
    }

    private fun rebuildPath() {
        path.reset()
        val inset = strokeWidth + amplitude
        val l = bounds.left + inset
        val t = bounds.top + inset
        val rgt = bounds.right - inset
        val b = bounds.bottom - inset
        if (rgt <= l || b <= t) return
        var idx = 0
        fun jitter(): Float {
            val h = kotlin.math.sin(idx * 12.9898 + (rgt - l) + (b - t)) * 43758.5453
            idx++
            return ((h - kotlin.math.floor(h)).toFloat() * 2f - 1f) * amplitude
        }
        fun edge(x1: Float, y1: Float, x2: Float, y2: Float, first: Boolean) {
            val len = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
            val n = kotlin.math.max(2, (len / step).toInt())
            val px = -(y2 - y1) / len
            val py = (x2 - x1) / len
            for (k in 0..n) {
                val tt = k.toFloat() / n
                val j = if (k == 0 || k == n) 0f else jitter()
                val x = x1 + (x2 - x1) * tt + px * j
                val y = y1 + (y2 - y1) * tt + py * j
                if (first && k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        edge(l, t, rgt, t, true)
        edge(rgt, t, rgt, b, false)
        edge(rgt, b, l, b, false)
        edge(l, b, l, t, false)
        path.close()
    }

    override fun draw(canvas: android.graphics.Canvas) {
        paint.style = android.graphics.Paint.Style.FILL
        ThemedBorders.setupFillPaint(context, fill, paint, bounds)
        canvas.drawPath(path, paint)
        paint.shader = null
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

/** Rounded box whose fill is a repeating pattern tile shader. */
private class PatternBoxDrawable(
    private val context: Context,
    private val fill: UiFill,
    private val radius: Float,
    private val strokeColor: Int?,
    private val strokeWidth: Float,
    private val dashed: Boolean,
    private val dashOn: Float,
    private val dashOff: Float,
) : Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: android.graphics.Canvas) {
        val r = android.graphics.RectF(bounds)
        paint.style = android.graphics.Paint.Style.FILL
        paint.pathEffect = null
        paint.shader = ThemedBorders.patternShader(context, fill)
        canvas.drawRoundRect(r, radius, radius, paint)
        paint.shader = null
        if (strokeColor != null) {
            val inset = strokeWidth / 2f
            val rs = android.graphics.RectF(
                r.left + inset, r.top + inset, r.right - inset, r.bottom - inset,
            )
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.color = strokeColor
            paint.pathEffect =
                if (dashed) android.graphics.DashPathEffect(floatArrayOf(dashOn, dashOff), 0f)
                else null
            canvas.drawRoundRect(rs, radius, radius, paint)
            paint.pathEffect = null
        }
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
 * XML style would otherwise pin a static black border, strips any chrome the
 * vendor ROM bakes into the dialog panel, and tints the button bar. Call
 * after create(). [frame] defaults to the edge-to-edge window panel; pass
 * [ThemedBorders.dialogFrame] for the 16dp-margin variant.
 */
fun <T : Dialog> T.withThemedFrame(frame: Drawable = ThemedBorders.windowPanel(context)): T = apply {
    window?.setBackgroundDrawable(frame)
    withoutVendorPanelBorder()
    withThemedButtons()
}

/**
 * Removes the decoration some vendor ROMs give the framework AlertDialog
 * panel. Onyx Boox firmware sets alert_dialog_material's parentPanel to a
 * white, black-stroked rounded shape (its DeviceDefault layout uses a
 * matching foreground) as an e-ink stand-in for the window shadow; under the
 * themed window frame that draws a second box inside the border and hides
 * the frame's fill. Stock Android leaves the panel undecorated, so this is a
 * no-op there.
 */
fun <T : Dialog> T.withoutVendorPanelBorder(): T = apply {
    if (this !is AlertDialog) return@apply
    onContentAttached {
        val panelId = context.resources.getIdentifier("parentPanel", "id", "android")
        val panel = panelId.takeIf { it != 0 }?.let { findViewById<View>(it) }
            ?: findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
        panel?.background = null
        panel?.foreground = null
    }
}

/**
 * Tints an AlertDialog's button-bar buttons with the theme accent, replacing
 * MyButtonStyle's static black-on-white look.
 */
fun <T : Dialog> T.withThemedButtons(): T = apply {
    if (this !is AlertDialog) return@apply
    onContentAttached {
        val accent = ThemedBorders.accentArgb(context)
        intArrayOf(
            DialogInterface.BUTTON_POSITIVE,
            DialogInterface.BUTTON_NEGATIVE,
            DialogInterface.BUTTON_NEUTRAL,
        ).forEach { which -> getButton(which)?.setTextColor(accent) }
    }
}

/**
 * Runs [block] once the dialog's content views exist. They are only
 * installed during show(), so run when the decor attaches -- rather than via
 * setOnShowListener, which call sites may already use for their own
 * purposes -- or immediately if the dialog is already showing.
 */
private fun Dialog.onContentAttached(block: () -> Unit) {
    val decor = window?.decorView ?: return
    if (decor.isAttachedToWindow) {
        block()
    } else {
        decor.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = block()
                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        )
    }
}
