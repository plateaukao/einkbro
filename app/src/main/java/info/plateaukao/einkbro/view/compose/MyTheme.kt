package info.plateaukao.einkbro.view.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/**
 * On Android 15+ (targetSdk 35+) windows are forced edge-to-edge, so the
 * settings screens extend their black TopAppBar behind the transparent status
 * bar: the status bar icons must turn white, and the navigation bar icons
 * follow the theme background. On older versions the theme's white status bar
 * is still honored, so the default dark icons must be kept.
 */
@Composable
fun SystemBarIconsForBlackTopBar(darkTheme: Boolean = isSystemInDarkTheme()) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
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

private val DarkColors = darkColors(
    primary = Color.Black,
    onPrimary = Color.Gray,
    secondary = Color.Gray,
    onSecondary = Color.White,
    surface = Color.Black,
    onSurface = Color.Gray,
    background = Color.Black,
    onBackground = Color.Gray,
)
private val LightColors = lightColors(
    primary = Color.Black,
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
)