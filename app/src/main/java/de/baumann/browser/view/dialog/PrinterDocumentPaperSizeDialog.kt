package de.baumann.browser.view.dialog

import android.content.Context
import android.print.PrintAttributes
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.PaperSize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrinterDocumentPaperSizeDialog(val context: Context): KoinComponent {
    private val config: ConfigManager by inject()

    fun show() {
        val paperSizes = PaperSize.values().map { it.sizeString }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Choose the default paper size")
            setSingleChoiceItems(paperSizes, config.pdfPaperSize.ordinal) { dialog, selectedIndex ->
                config.pdfPaperSize = PaperSize.values()[selectedIndex]
                dialog.dismiss()
            }
        }.create().show()
    }
}