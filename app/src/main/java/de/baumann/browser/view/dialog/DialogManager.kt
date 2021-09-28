package de.baumann.browser.view.dialog

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogEditBookmarkBinding
import de.baumann.browser.Ninja.databinding.DialogEditExtensionBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.unit.ViewUnit.dp
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DialogManager(
    private val activity: Activity
): KoinComponent {
    private val config: ConfigManager by inject()

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
            it.window?.setLayout(200.dp(activity), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun showSaveEpubDialog(onNextAction: (Boolean) -> Unit) {
        val options = arrayOf(
            activity.resources.getString(R.string.existing_epub),
            activity.resources.getString(R.string.a_new_epub)
        )

        val builder = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
        builder.setTitle(activity.resources.getString(R.string.save_to))
        builder.setItems(options) { _, index ->
            when(index) {
                0 -> onNextAction(false)
                1 -> onNextAction(true)
            }
        }
        builder.show()
    }

    fun showSavePdfDialog(
        url: String,
        savePdf: (String, String) -> Unit,
    ) {
        val menuView = DialogEditExtensionBinding.inflate(LayoutInflater.from(activity))
        val editTitle = menuView.dialogEdit.apply {
            setHint(R.string.dialog_title_hint)
            setText(HelperUnit.fileName(url))
        }
        val editExtension = menuView.dialogEditExtension
        val filename = URLUtil.guessFileName(url, null, null)
        val extension = filename.substring(filename.lastIndexOf("."))
        if (extension.length <= 8) {
            editExtension.setText(extension)
        }
        showOkCancelDialog(
            title = activity.getString(R.string.menu_edit),
            view = menuView.root,
            okAction = {
                val title = editTitle.text.toString().trim { it <= ' ' }
                val ext= editExtension.text.toString().trim { it <= ' ' }
                val fileName = title + ext
                if (title.isEmpty() || ext.isEmpty() || !extension.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty))
                } else {
                    savePdf(url, fileName)
                }
            },
            cancelAction = { ViewUnit.hideKeyboard(activity) }
        )
    }

    fun showOkCancelDialog(
        title: String? = null,
        messageResId: Int? = null,
        view: View? = null,
        okAction: () -> Unit,
        cancelAction: (() -> Unit)? = null
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setPositiveButton(android.R.string.ok) { _, _ -> okAction() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancelAction?.invoke() }
            .apply {
                title?.let { title -> setTitle(title) }
                view?.let { setView(it) }
                messageResId?.let { setMessage(messageResId) }
            }
            .create().apply {
                window?.setGravity(Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }

    fun showOptionDialog(
        view: View
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setView(view)
            .create().apply {
                window?.setGravity(Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }
}