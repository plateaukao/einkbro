package info.plateaukao.einkbro.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.toArgb
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.palette
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runtime replacements for the background_with_border(_margin) /
 * selected_border_bg XML drawables, so dialog window frames and floating
 * panels follow the selected UiTheme accent immediately — no activity
 * restart and no per-theme XML theme overlays needed. The XML drawables
 * resolve their stroke from the static activity theme
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

    private fun roundedBox(context: Context): GradientDrawable {
        val night = isNight(context)
        val palette = config.display.uiTheme.palette(
            androidx.compose.ui.graphics.Color(config.display.customThemeColor)
        )
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.dp(5f).toFloat()
            setColor(if (night) android.graphics.Color.BLACK else palette.background.toArgb())
            setStroke(context.dp(1f), (if (night) palette.accentDark else palette.accent).toArgb())
        }
    }

    /** background_with_border: bordered box for floating panels/buttons. */
    fun panel(context: Context): Drawable = roundedBox(context)

    /** selected_border_bg: double border marking a selected/toggled state. */
    fun selectedPanel(context: Context): Drawable {
        val layers = LayerDrawable(arrayOf(roundedBox(context), roundedBox(context)))
        val inset = context.dp(3f)
        layers.setLayerInset(1, inset, inset, inset, inset)
        return layers
    }

    /** background_with_border_margin: dialog window frame (16dp outer margin). */
    fun dialogFrame(context: Context): Drawable {
        val margin = context.dp(16f)
        return InsetDrawable(roundedBox(context), margin, margin, margin, margin)
    }
}

/**
 * Applies the themed frame to a framework dialog (AlertDialog etc.) whose
 * XML style would otherwise pin a static black border. Call after create().
 */
fun <T : android.app.Dialog> T.withThemedFrame(): T = apply {
    window?.setBackgroundDrawable(ThemedBorders.panel(context))
}
