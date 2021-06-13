package de.baumann.browser.view.dialog

import android.app.Activity
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
}