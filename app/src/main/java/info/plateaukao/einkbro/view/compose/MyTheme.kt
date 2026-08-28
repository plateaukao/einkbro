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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.ThemePalette
import info.plateaukao.einkbro.preference.GRADIENT_END_FRACTION
import info.plateaukao.einkbro.preference.GRADIENT_START_FRACTION
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.preference.UiFill
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
    val uiBorder: MutableState<UiBorder> = mutableStateOf(UiBorder.CLASSIC)
    val uiFill: MutableState<UiFill> = mutableStateOf(UiFill.NONE)
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

/**
 * Postage-stamp outline: straight edges with evenly spaced semicircular
 * perforation bites. Corners stay square (bites keep clear of them), so the
 * corner region remains plain vertical/horizontal edge.
 */
fun stampShape(scallopRadius: Dp): androidx.compose.ui.graphics.Shape =
    object : androidx.compose.ui.graphics.Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
            density: androidx.compose.ui.unit.Density,
        ): androidx.compose.ui.graphics.Outline {
            val r = with(density) { scallopRadius.toPx() }
            val path = androidx.compose.ui.graphics.Path()
            val w = size.width
            val h = size.height

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
                    androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                    startDeg, -180f, forceMoveTo = false,
                )
            }

            path.moveTo(0f, 0f)
            biteCenters(w).forEach { cx -> path.lineTo(cx - r, 0f); arc(cx, 0f, 180f) }
            path.lineTo(w, 0f)
            biteCenters(h).forEach { cy -> path.lineTo(w, cy - r); arc(w, cy, 270f) }
            path.lineTo(w, h)
            biteCenters(w).map { w - it }.forEach { cx -> path.lineTo(cx + r, h); arc(cx, h, 0f) }
            path.lineTo(0f, h)
            biteCenters(h).map { h - it }.forEach { cy -> path.lineTo(0f, cy + r); arc(0f, cy, 90f) }
            path.close()
            return androidx.compose.ui.graphics.Outline.Generic(path)
        }
    }

/**
 * Hand-drawn outline: the rectangle's perimeter walked in short segments,
 * each vertex nudged by a deterministic pseudo-random offset (stable for a
 * given size, so recompositions don't make the line shimmer).
 */
fun sketchShape(amplitude: Dp): androidx.compose.ui.graphics.Shape =
    object : androidx.compose.ui.graphics.Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
            density: androidx.compose.ui.unit.Density,
        ): androidx.compose.ui.graphics.Outline {
            val a = with(density) { amplitude.toPx() }
            val step = with(density) { 14.dp.toPx() }
            // inset the rectangle by the wobble amplitude so the jittered
            // line never leaves the bounds (it would be cropped by clipping
            // or covered by neighboring items)
            val inset = a + 1f
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val path = androidx.compose.ui.graphics.Path()
            fun jitter(i: Int): Float {
                val h = kotlin.math.sin(i * 12.9898 + size.width + size.height) * 43758.5453
                return ((h - kotlin.math.floor(h)).toFloat() * 2f - 1f) * a
            }
            var idx = 0
            fun edge(x1: Float, y1: Float, x2: Float, y2: Float, first: Boolean) {
                val len = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
                val n = kotlin.math.max(2, (len / step).toInt())
                // perpendicular unit vector for the nudge
                val px = -(y2 - y1) / len
                val py = (x2 - x1) / len
                for (k in 0..n) {
                    val t = k.toFloat() / n
                    val j = if (k == 0 || k == n) 0f else jitter(idx)
                    idx++
                    val x = x1 + (x2 - x1) * t + px * j
                    val y = y1 + (y2 - y1) * t + py * j
                    if (first && k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            edge(left, top, right, top, first = true)
            edge(right, top, right, bottom, first = false)
            edge(right, bottom, left, bottom, first = false)
            edge(left, bottom, left, top, first = false)
            path.close()
            return androidx.compose.ui.graphics.Outline.Generic(path)
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

/** Accent-tinted fill color for the TONAL fill. */
@Composable
fun tonalFillColor(): Color = lerp(
    MaterialTheme.colors.background,
    MaterialTheme.colors.primary,
    if (MaterialTheme.colors.isLight) 0.10f else 0.16f,
)

/** Gradient brush for the GRADIENT fill at the user's angle and level. */
@Composable
fun gradientFillBrush(): Brush {
    val angle = UiThemeState.gradientAngle.value
    val level = UiThemeState.gradientLevel.value
    return AngleGradientBrush(
        listOf(
            lerp(
                MaterialTheme.colors.background,
                MaterialTheme.colors.primary,
                GRADIENT_START_FRACTION.withGradientLevel(level),
            ),
            lerp(
                MaterialTheme.colors.background,
                MaterialTheme.colors.primary,
                GRADIENT_END_FRACTION.withGradientLevel(level),
            ),
        ),
        angle.toFloat(),
    )
}

/**
 * Themed frame for in-content bordered items, combining the independent
 * border and fill preferences. A non-positive widthOverride keeps the call
 * site's "no frame in this state" behavior; a positive one keeps its
 * emphasis (never thinner than the border's width).
 */
fun Modifier.ebItemFrame(widthOverride: Dp? = null): Modifier = composed {
    val border = UiThemeState.uiBorder.value
    val fill = UiThemeState.uiFill.value
    if (widthOverride != null && widthOverride <= 0.dp) return@composed this
    // the fill must clip to the border's actual outline (stamp bites,
    // sketch wobble), not to a plain rounded rect
    val shape = when (border) {
        UiBorder.STAMP -> stampShape(4.dp)
        UiBorder.SKETCH -> sketchShape(2.5.dp)
        else -> RoundedCornerShape(border.itemRadiusDp.dp)
    }
    val width = maxOf(widthOverride ?: 0.dp, border.widthDp.dp)
    val accent = MaterialTheme.colors.primary
    val bg = MaterialTheme.colors.background

    if (border == UiBorder.STICKER) {
        // draw shadow, fill, and outline entirely inside the bounds (the
        // front box is inset by the shadow offset), so nothing is cropped
        // by or overlaps neighboring items
        val radius = border.itemRadiusDp
        val fillBrush: Brush? = when (fill) {
            UiFill.NONE -> null
            UiFill.TONAL -> SolidColor(tonalFillColor())
            UiFill.GRADIENT -> gradientFillBrush()
        }
        return@composed drawBehind {
            val off = 3.dp.toPx()
            val corner = CornerRadius(radius.dp.toPx())
            val boxSize = Size(size.width - off, size.height - off)
            drawRoundRect(accent, topLeft = Offset(off, off), size = boxSize, cornerRadius = corner)
            drawRoundRect(bg, size = boxSize, cornerRadius = corner)
            if (fillBrush != null) {
                drawRoundRect(fillBrush, size = boxSize, cornerRadius = corner)
            }
            drawRoundRect(
                accent, size = boxSize, cornerRadius = corner,
                style = Stroke(width.toPx()),
            )
        }
    }

    var m: Modifier = when (fill) {
        UiFill.NONE -> this
        UiFill.TONAL -> background(tonalFillColor(), shape)
        UiFill.GRADIENT -> background(gradientFillBrush(), shape)
    }

    when (border) {
        UiBorder.NONE, UiBorder.STICKER -> m
        UiBorder.CLASSIC, UiBorder.ROUND, UiBorder.SHARP, UiBorder.PAPER ->
            m.border(width, accent, shape)
        UiBorder.DASHED -> m.dashedBorder(width, border.itemRadiusDp.dp, accent)
        UiBorder.STAMP, UiBorder.SKETCH -> m.border(width, accent, shape)
        UiBorder.CERTIFICATE -> m.drawBehind {
            val outer = 3.dp.toPx()
            drawRect(
                color = accent,
                topLeft = Offset(outer / 2f, outer / 2f),
                size = Size(size.width - outer, size.height - outer),
                style = Stroke(outer),
            )
            val inset = outer + 4.dp.toPx()
            drawRect(
                color = accent,
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(1.dp.toPx()),
            )
        }
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