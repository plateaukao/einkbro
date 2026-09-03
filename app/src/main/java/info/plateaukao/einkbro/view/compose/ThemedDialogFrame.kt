package info.plateaukao.einkbro.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import info.plateaukao.einkbro.view.ThemedBorders

/**
 * Applies the ThemedBorders window frame to a Compose-created dialog window
 * (androidx.compose.ui.window.Dialog / material AlertDialog), so dialogs
 * hosted in Compose-only activities follow the selected border/fill theme
 * like every framework dialog does. Also drops the dim scrim, matching the
 * app's e-ink dialog style. Call it first inside the dialog's content, and
 * make the dialog's own surface transparent (no background, no border) so
 * the window frame shows through.
 *
 * Reads [UiThemeState] so live theme previews retint the frame.
 */
@Composable
fun ThemedDialogWindowFrame() {
    val view = LocalView.current
    // read the state that shapes the frame, so recomposition re-applies it
    UiThemeState.uiBorder.value
    UiThemeState.uiFill.value
    UiThemeState.current.value
    UiThemeState.customColor.value
    UiThemeState.inverted.value
    UiThemeState.gradientLevel.value
    UiThemeState.gradientAngle.value
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.apply {
            setDimAmount(0f)
            setBackgroundDrawable(ThemedBorders.windowPanel(view.context))
        }
    }
}
