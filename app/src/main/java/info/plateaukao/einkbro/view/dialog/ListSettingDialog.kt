package info.plateaukao.einkbro.view.dialog

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit.dp
import org.koin.core.component.KoinComponent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ListSettingWithNameDialog(
    private val context: Context,
    private val titleId: Int,
    private val names: List<String>,
    private val defaultValue: Int,
    // When supplied, an icon button is shown to the right of the title; tapping it
    // dismisses the dialog (resuming with null) and invokes the action.
    private val titleActionIconResId: Int = 0,
    private val titleActionDescriptionResId: Int = 0,
    private val onTitleAction: (() -> Unit)? = null,
) : KoinComponent {
    suspend fun show() = suspendCoroutine<Int?> { continuation ->
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setSingleChoiceItems(
                names.toTypedArray(),
                defaultValue
            ) { dialog, selectedIndex ->
                dialog.dismiss()
                continuation.resume(selectedIndex)
            }
        }

        var titleActionButton: View? = null
        if (onTitleAction != null && titleActionIconResId != 0) {
            val (titleView, actionButton) = createTitleView()
            titleActionButton = actionButton
            builder.setCustomTitle(titleView)
        } else {
            builder.setTitle(context.resources.getString(titleId))
        }

        builder.create().also { dialog ->
            titleActionButton?.setOnClickListener {
                dialog.dismiss()
                continuation.resume(null)
                onTitleAction?.invoke()
            }
            dialog.show()
            dialog.window?.setLayout(300.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    // Builds a title row holding the title text plus a trailing icon button. Returns the
    // container view and the button so the caller can wire up its click listener. Views are
    // created from a context themed with TouchAreaDialog so text/icon colors resolve the same
    // way as the dialog body (correct in both day and night modes).
    private fun createTitleView(): Pair<View, View> {
        val themed = ContextThemeWrapper(context, R.style.TouchAreaDialog)
        val container = LinearLayout(themed).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24.dp(themed), 16.dp(themed), 8.dp(themed), 8.dp(themed))
        }
        val titleAppearance = TypedValue().also {
            themed.theme.resolveAttribute(android.R.attr.textAppearanceLarge, it, true)
        }.resourceId
        val titleView = TextView(themed).apply {
            text = themed.resources.getString(titleId)
            setTextAppearance(titleAppearance)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
            )
        }
        val borderlessBackground = TypedValue().also {
            themed.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, it, true,
            )
        }.resourceId
        val actionButton = ImageButton(themed).apply {
            setImageResource(titleActionIconResId)
            setBackgroundResource(borderlessBackground)
            if (titleActionDescriptionResId != 0) {
                contentDescription = themed.resources.getString(titleActionDescriptionResId)
            }
            layoutParams = LinearLayout.LayoutParams(40.dp(themed), 40.dp(themed))
        }
        container.addView(titleView)
        container.addView(actionButton)
        return container to actionButton
    }

   fun showBlocked(
       action: (Int) -> Unit
   ) {
       AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
           setTitle(context.resources.getString(titleId))
           setSingleChoiceItems(
               names.toTypedArray(),
               defaultValue
           ) { dialog, selectedIndex ->
               dialog.dismiss()
               action(selectedIndex)
           }
       }.create().also {
           it.show()
           it.window?.setLayout(300.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
       }
   }
}

class ListSettingDialog(
    private val context: Context,
    private val titleId: Int,
    private val nameResIds: List<Int>,
    private val defaultValue: Int
) {
    suspend fun show(): Int? {
        val names = nameResIds.map { context.resources.getString(it) }
        return ListSettingWithNameDialog(context, titleId, names, defaultValue).show()
    }
    fun show(
        action: (Int) -> Unit
    ) {
        val names = nameResIds.map { context.resources.getString(it) }
        ListSettingWithNameDialog(context, titleId, names, defaultValue).showBlocked(action)
    }
}

