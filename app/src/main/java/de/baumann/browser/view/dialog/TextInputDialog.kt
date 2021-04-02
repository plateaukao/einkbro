package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context

import android.widget.EditText




class TextInputDialog(
   private val context: Context,
   private val title: String,
   private val message: String,
   private val defaultText: String = "",
   private val action: (String) -> Unit,
) {
    fun show() {
        val editText = EditText(context).apply {
            setText(defaultText)
        }

        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    val text = editText.text.toString()
                    action.invoke(text)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }.show()

    }
}