package info.plateaukao.einkbro.view.dialog

import android.app.AlertDialog
import android.content.Context

import android.widget.EditText
import info.plateaukao.einkbro.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TextInputDialog(
   private val context: Context,
   private val title: String,
   private val message: String,
   private val defaultText: String = "",
) {
    suspend fun show() = suspendCoroutine<String?> { continuation ->
        val editText = EditText(context).apply {
            setText(defaultText)
        }

        AlertDialog.Builder(context, R.style.TouchAreaDialog)
                .setTitle(title)
                .setMessage(message)
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    val text = editText.text.toString()
                    continuation.resume(text)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(null)
                }.show()
    }
}