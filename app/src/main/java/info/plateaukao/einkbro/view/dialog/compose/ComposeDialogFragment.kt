package info.plateaukao.einkbro.view.dialog.compose

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.ToolbarPosition
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


abstract class ComposeDialogFragment : AppCompatDialogFragment(), KoinComponent {
    protected val config: ConfigManager by inject()
    protected lateinit var composeView: ComposeView

    protected var shouldShowInCenter: Boolean = false
    private var dialogAnchorX: Int = -1
    private var dialogAnchorY: Int = -1

    companion object {
        /** Horizontal center X (px) of the last clicked toolbar icon. Set by toolbar, consumed by dialog. */
        var anchorX: Int = -1
        /** Vertical center Y (px) of the last clicked toolbar icon. Set by toolbar, consumed by dialog. */
        var anchorY: Int = -1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NO_FRAME, 0)
        dialogAnchorX = anchorX
        dialogAnchorY = anchorY
        anchorX = -1
        anchorY = -1
        return super.onCreateDialog(savedInstanceState)
    }

    private fun setupDialog() {
        dialog?.apply {
            setCanceledOnTouchOutside(true)
            window?.attributes?.windowAnimations = 0
            val w = window ?: return
            if (!shouldShowInCenter) {
                if (config.isVerticalToolbar) {
                    val horizontalGravity = if (config.toolbarPosition == ToolbarPosition.Left) Gravity.START else Gravity.END
                    w.setGravity(Gravity.TOP or horizontalGravity)
                } else {
                    val verticalGravity = if (config.isToolbarOnTop) Gravity.TOP else Gravity.BOTTOM
                    if (dialogAnchorX >= 0) {
                        w.setGravity(verticalGravity or Gravity.START)
                    } else {
                        w.setGravity(verticalGravity or Gravity.END)
                    }
                }
            }
            w.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupDialog()

        composeView = ComposeView(requireContext())
        setupComposeView()

        return composeView
    }

    override fun onStart() {
        super.onStart()
        if (config.isVerticalToolbar) {
            adjustVerticalPosition()
        } else {
            adjustHorizontalPosition()
        }
    }

    protected open fun adjustHorizontalPosition() {
        if (dialogAnchorX < 0 || shouldShowInCenter) return

        val window = dialog?.window ?: return
        // Hide while repositioning to avoid visible jump from left edge
        window.attributes = window.attributes.apply { alpha = 0f }

        window.decorView.post {
            val w = dialog?.window ?: return@post
            val dialogWidth = w.decorView.width
            val screenWidth = resources.displayMetrics.widthPixels
            val targetX = if (dialogWidth > 0 && screenWidth > 0) {
                (dialogAnchorX - dialogWidth / 2)
                    .coerceIn(0, maxOf(0, screenWidth - dialogWidth))
            } else 0
            w.attributes = w.attributes.apply {
                x = targetX
                alpha = 1f
            }
        }
    }

    protected open fun adjustVerticalPosition() {
        if (dialogAnchorY < 0 || shouldShowInCenter) return

        val window = dialog?.window ?: return
        window.attributes = window.attributes.apply { alpha = 0f }

        window.decorView.post {
            val w = dialog?.window ?: return@post
            val dialogHeight = w.decorView.height
            val screenHeight = resources.displayMetrics.heightPixels
            val targetY = if (dialogHeight > 0 && screenHeight > 0) {
                (dialogAnchorY - dialogHeight / 2)
                    .coerceIn(0, maxOf(0, screenHeight - dialogHeight))
            } else 0
            w.attributes = w.attributes.apply {
                y = targetY
                alpha = 1f
            }
        }
    }

    abstract fun setupComposeView()

}

@Composable
fun HorizontalSeparator() {
    Divider(thickness = 1.dp, color = MaterialTheme.colors.onBackground)
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

