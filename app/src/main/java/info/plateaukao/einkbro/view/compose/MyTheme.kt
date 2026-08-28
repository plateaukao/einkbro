package info.plateaukao.einkbro.view.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import info.plateaukao.einkbro.preference.BorderStyle
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.ThemePalette
import info.plateaukao.einkbro.preference.ThemeStyle
import info.plateaukao.einkbro.preference.UiStyle
import info.plateaukao.einkbro.preference.gradientSpec
import info.plateaukao.einkbro.preference.UiTheme
import info.plateaukao.einkbro.preference.palette

/**
 * Holds the currently selected [UiTheme] as Compose state so every MyTheme
 * root recomposes immediately when the user picks a new theme in Settings —
 * no activity restart needed. Initialized from config in EinkBroApplication;
 * kept in sync by the DisplayConfig.uiTheme setter (and the browser's
 * SharedPreferences listener, for writes that bypass the setter, e.g. backup
 * restore).
 */
object UiThemeState {
    val current: MutableState<UiTheme> = mutableStateOf(UiTheme.CLASSIC)
    val darkMode: MutableState<DarkMode> = mutableStateOf(DarkMode.SYSTEM)
    val customColor: MutableState<Color> = mutableStateOf(Color(0xFF4A90D9))
    val uiStyle: MutableState<UiStyle> = mutableStateOf(UiStyle.CLASSIC)
    val inverted: MutableState<Boolean> = mutableStateOf(false)
    val gradientAngle: MutableState<Int> = mutableStateOf(45)
    val gradientLevel: MutableState<Int> = mutableStateOf(100)
}

/**
 * Linear gradient at an arbitrary angle (degrees; 0 = left-to-right,
 * 90 = top-down), spanning the drawn box regardless of its size.
 */
class AngleGradientBrush(
    private val gradientColors: List<Color>,
    private val angleDegrees: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val dx = kotlin.math.cos(rad).toFloat()
        val dy = kotlin.math.sin(rad).toFloat()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val halfLen =
            (kotlin.math.abs(dx) * size.width + kotlin.math.abs(dy) * size.height) / 2f
        return LinearGradientShader(
            from = Offset(cx - dx * halfLen, cy - dy * halfLen),
            to = Offset(cx + dx * halfLen, cy + dy * halfLen),
            colors = gradientColors,
        )
    }
}

/** Applies the user's gradient level (percent) to a spec blend fraction. */
fun Float.withGradientLevel(levelPercent: Int): Float =
    (this * levelPercent / 100f).coerceIn(0f, 0.9f)

/**
 * Whether the app UI should use the dark palette: the Dark mode setting wins
 * (Force on / Disabled), and Follow system falls back to the system setting.
 * This gives every UiTheme a dark variant independent of system dark mode.
 */
@Composable
fun isAppInDarkTheme(): Boolean = UiThemeState.inverted.value || when (UiThemeState.darkMode.value) {
    DarkMode.FORCE_ON -> true
    DarkMode.DISABLED -> false
    DarkMode.SYSTEM -> isSystemInDarkTheme()
}

/**
 * Content color for the app's TopAppBars. M2's TopAppBar background is
 * primarySurface: the accent (primary) in light mode but surface (black) in
 * dark mode — so bar content must be onPrimary in light and onSurface in
 * dark. (Dark onPrimary is black for accent-filled buttons and would be
 * invisible on the dark bar.)
 */
val Colors.onTopBar: Color get() = if (isLight) onPrimary else onSurface

@Composable
fun currentThemeStyle(): ThemeStyle = UiThemeState.uiStyle.value.style

/**
 * Themed frame for in-content bordered items: width, corner radius, and
 * border style come from the current theme's ThemeStyle. A non-positive
 * widthOverride keeps the call site's "no border in this state" behavior;
 * a positive one keeps its emphasis (never thinner than the theme width).
 * NONE styles use a tonal fill instead of a stroke; DOUBLE falls back to a
 * solid stroke for small items (the double frame stays on dialog windows).
 */
fun Modifier.ebItemFrame(widthOverride: Dp? = null): Modifier = composed {
    val style = currentThemeStyle()
    if (widthOverride != null && widthOverride <= 0.dp) return@composed this
    val shape = RoundedCornerShape(style.itemRadiusDp.dp)
    val width = maxOf(widthOverride ?: 0.dp, style.borderWidthDp.dp)
    when (style.borderStyle) {
        BorderStyle.NONE -> background(
            lerp(
                MaterialTheme.colors.background,
                MaterialTheme.colors.primary,
                if (MaterialTheme.colors.isLight) 0.10f else 0.16f,
            ),
            shape,
        )
        BorderStyle.DASHED -> dashedBorder(width, style.itemRadiusDp.dp, MaterialTheme.colors.primary)
        BorderStyle.GRADIENT, BorderStyle.GRADIENT_FLAT, BorderStyle.GRADIENT_DEEP -> {
            val (start, end, hasBorder) = style.borderStyle.gradientSpec()!!
            val angle = UiThemeState.gradientAngle.value
            val level = UiThemeState.gradientLevel.value
            val filled = background(
                AngleGradientBrush(
                    listOf(
                        lerp(
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.primary,
                            start.withGradientLevel(level),
                        ),
                        lerp(
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.primary,
                            end.withGradientLevel(level),
                        ),
                    ),
                    angle.toFloat(),
                ),
                shape,
            )
            if (hasBorder) filled.border(width, MaterialTheme.colors.primary, shape) else filled
        }
        else -> border(width, MaterialTheme.colors.primary, shape)
    }
}

@Composable
fun MyTheme(
    darkTheme: Boolean = isAppInDarkTheme(),
    content: @Composable () -> Unit
) {
    val uiTheme by UiThemeState.current
    val customColor by UiThemeState.customColor
    val inverted by UiThemeState.inverted
    MaterialTheme(
        colors = remember(uiTheme, customColor, darkTheme, inverted) {
            val palette = uiTheme.palette(customColor)
            when {
                // inverted: the theme's dark text shade becomes the background
                // and the light background tint becomes the text color
                inverted -> palette.toInvertedColors()
                darkTheme -> palette.toDarkColors()
                else -> palette.toLightColors()
            }
        },
        content = content,
    )
}

/**
 * Makes the status bar match the settings screens' TopAppBar. On Android 15+
 * (targetSdk 35+) windows are forced edge-to-edge and the TopAppBar already
 * extends behind the transparent status bar; on older versions the status bar
 * is painted with the top bar's background color (the theme accent in light
 * mode, surface in dark mode) directly. Either way the bar is dark/accent
 * colored, so status bar icons turn light, and the navigation bar icons
 * follow the theme background.
 */
@Composable
fun SystemBarIconsForBlackTopBar(darkTheme: Boolean = isAppInDarkTheme()) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val topBarColor = MaterialTheme.colors.primarySurface
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = topBarColor.toArgb()
        }
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

val NormalTextModifier = Modifier.padding(6.dp)

// The accent lands on primary/secondary(+variants): Material components
// (Button, Switch, Checkbox, TextField cursor, ProgressIndicator) pick it up
// automatically; borders, dividers, and toolbar icons reference
// MaterialTheme.colors.primary. Text and screen surfaces come from the
// theme's onBackground/background so a theme can tint them too. CLASSIC's
// values reproduce the original hardcoded black-and-white palette exactly.
private fun ThemePalette.toLightColors() = lightColors(
    primary = accent,
    primaryVariant = accent,
    onPrimary = Color.White,
    secondary = accent,
    secondaryVariant = accent,
    onSecondary = Color.White,
    surface = background,
    onSurface = onBackground,
    background = background,
    onBackground = onBackground,
)

private fun ThemePalette.toInvertedColors() = darkColors(
    primary = accentDark,
    primaryVariant = accentDark,
    onPrimary = Color.Black,
    secondary = accentDark,
    onSecondary = Color.Black,
    surface = onBackground,
    onSurface = background,
    background = onBackground,
    onBackground = background,
)

private fun ThemePalette.toDarkColors() = darkColors(
    primary = accentDark,
    primaryVariant = accentDark,
    onPrimary = Color.Black,
    secondary = accentDark,
    onSecondary = Color.Black,
    surface = Color.Black,
    onSurface = onBackgroundDark,
    background = Color.Black,
    onBackground = onBackgroundDark,
)