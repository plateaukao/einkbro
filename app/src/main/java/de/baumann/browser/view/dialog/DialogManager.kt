package de.baumann.browser.view.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.ViewUnit

class DialogManager(
    private val activity: Activity
) {
    private val config: ConfigManager by lazy { ConfigManager(activity) }

    fun showFontSizeChangeDialog(
        changeFontSizeAction: (fontSize: Int) -> Unit
    ) {
        val fontArray = activity.resources.getStringArray(R.array.setting_entries_font)
        val valueArray = activity.resources.getStringArray(R.array.setting_values_font)
        val selected = valueArray.indexOf(config.fontSize.toString())
        AlertDialog.Builder(activity, R.style.TouchAreaDialog).apply{
            setTitle("Choose Font Size")
            setSingleChoiceItems(fontArray, selected) { dialog, which ->
                changeFontSizeAction(valueArray[which].toInt())
                dialog.dismiss()
            }
        }.create().also {
            it.show()
            it.window?.setLayout(ViewUnit.dpToPixel(activity, 200).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun showOkCancelDialog(
        context: Context,
        title: String? = null,
        messageResId:Int,
        okAction: () -> Unit,
        cancelAction: (() -> Unit)? = null)
    {
        AlertDialog.Builder(context, R.style.TouchAreaDialog)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                okAction.invoke()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                cancelAction?.invoke()
            }.apply {
                title?.let { title -> setTitle(title) }
            }
            .create().apply {
                window?.setGravity(Gravity.BOTTOM)
            }
            .show()
    }

    fun showOptionDialog(
        context: Context,
        view: View
    ): Dialog {
        val dialog = AlertDialog.Builder(context, R.style.TouchAreaDialog)
            .setView(view)
            .create().apply { window?.setGravity(Gravity.BOTTOM) }
        dialog.show()
        return dialog
    }

}