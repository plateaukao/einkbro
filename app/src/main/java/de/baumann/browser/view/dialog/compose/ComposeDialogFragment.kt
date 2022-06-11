package de.baumann.browser.view.dialog.compose

import android.app.Dialog
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class ComposeDialogFragment: AppCompatDialogFragment(), KoinComponent {
    protected val config: ConfigManager by inject()

    protected fun setupDialog() {
        dialog?.apply {
            setStyle(STYLE_NO_TITLE, R.style.ComposeDialog)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setGravity((if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM) or Gravity.RIGHT)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }
}

fun Dialog.runClickAndDismiss(config: ConfigManager?, action: ()-> Unit) {
    config ?: return
    action()
    dismiss()
}

@Composable
fun HorizontalSeparator() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(color = MaterialTheme.colors.onBackground)
    )
}

@Composable
fun VerticalSeparator() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(color = MaterialTheme.colors.onBackground)
    )
}

