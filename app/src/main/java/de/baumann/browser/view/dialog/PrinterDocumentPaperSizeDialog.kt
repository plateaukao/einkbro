package de.baumann.browser.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.PaperSize

class PrinterDocumentPaperSizeDialog(val context: Context) {
    private val config: ConfigManager = ConfigManager(context)

    fun show() {
        val paperSizes = PaperSize.values().map { it.sizeString }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Choose the default paper size")
            setItems(paperSizes) { dialog, selectedIndex ->
                config.pdfPaperSize = PaperSize.values()[selectedIndex]
                dialog.dismiss()
            }
        }.create().show()
    }

}