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
import info.plateaukao.einkbro.preference.palette
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

    private fun currentPalette(): ThemePalette =
        config.display.uiTheme.palette(Color(config.display.customThemeColor))

    /** Accent-tinted fill used instead of a stroke for BorderStyle.NONE. */
    fun tonalFillArgb(context: Context): Int {
        val palette = currentPalette()
        return when {
            config.display.uiThemeInverted ->
                lerp(palette.onBackground, palette.accentDark, 0.16f).toArgb()
            isNight(context) -> lerp(Color.Black, palette.accentDark, 0.16f).toArgb()
            else -> lerp(palette.background, palette.accent, 0.10f).toArgb()
        }
    }

    private fun box(
        context: Context,
        style: ThemeStyle = config.display.uiStyle.style,
        forceSolid: Boolean = false,
    ): Drawable {
        val inverted = config.display.uiThemeInverted
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

        fun single(withStroke: Boolean) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fill)
            if (withStroke) {
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
        val cornerAllowance = style.frameRadiusDp * 0.3f
        return context.dp(stroke + doubleExtra + cornerAllowance)
    }

    private fun withContentPadding(context: Context, drawable: Drawable): Drawable {
        val style = config.display.uiStyle.style
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
 * Applies the themed frame to a framework dialog (AlertDialog etc.) whose
 * XML style would otherwise pin a static black border. Call after create().
 */
fun <T : android.app.Dialog> T.withThemedFrame(): T = apply {
    window?.setBackgroundDrawable(ThemedBorders.windowPanel(context))
}
