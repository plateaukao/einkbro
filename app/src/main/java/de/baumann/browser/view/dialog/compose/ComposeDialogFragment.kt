package de.baumann.browser.view.dialog.compose

import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class ComposeDialogFragment(): DialogFragment(), KoinComponent {
    protected val config: ConfigManager by inject()

    protected fun setupDialog() {
        dialog?.apply {
            setStyle(STYLE_NO_TITLE, R.style.TouchAreaDialog)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }
}