package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.PaperSize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrinterDocumentPaperSizeDialog(val context: Context) : KoinComponent {
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