package info.plateaukao.einkbro.view

import android.app.Activity
import android.os.Build
import androidx.compose.ui.graphics.Color
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.UiTheme
import info.plateaukao.einkbro.view.compose.UiThemeState

/**
 * On Android 12+ the launch splash is drawn by the system before the app
 * runs, from a theme registered with setSplashScreenTheme. Whenever the
 * theme changes, the matching splash style (theme background color plus the
 * logo tinted with the text color; night variants in values-night-v31) is
 * registered for the next launch. The custom color maps to the preset with
 * the nearest hue.
 */
object SplashThemer {
    fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val theme = UiThemeState.current.value
        val target =
            if (theme == UiTheme.CUSTOM) nearestPreset(UiThemeState.customColor.value) else theme
        val styleRes = when (target) {
            UiTheme.LIGHT_BLUE -> R.style.SplashTheme_LightBlue
            UiTheme.DARK_BLUE -> R.style.SplashTheme_DarkBlue
            UiTheme.GREEN -> R.style.SplashTheme_Green
            UiTheme.SEPIA -> R.style.SplashTheme_Sepia
            UiTheme.PURPLE -> R.style.SplashTheme_Purple
            UiTheme.RED -> R.style.SplashTheme_Red
            else -> R.style.SplashTheme_Classic
        }
        activity.splashScreen.setSplashScreenTheme(styleRes)
    }

    private fun nearestPreset(color: Color): UiTheme {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                255,
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
            ),
            hsv,
        )
        if (hsv[1] < 0.15f) return UiTheme.CLASSIC
        val presets = UiTheme.entries.filter { it != UiTheme.CUSTOM && it != UiTheme.CLASSIC }
        return presets.minBy { preset ->
            val presetHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(
                android.graphics.Color.argb(
                    255,
                    (preset.accent.red * 255).toInt(),
                    (preset.accent.green * 255).toInt(),
                    (preset.accent.blue * 255).toInt(),
                ),
                presetHsv,
            )
            val d = kotlin.math.abs(hsv[0] - presetHsv[0])
            kotlin.math.min(d, 360f - d)
        }
    }
}
