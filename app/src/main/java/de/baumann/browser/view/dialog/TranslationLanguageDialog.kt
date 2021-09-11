package de.baumann.browser.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.util.TranslationLanguage

class TranslationLanguageDialog(val context: Context) {
    private val config: ConfigManager = ConfigManager(context)

    fun show(action: (TranslationLanguage) -> Unit) {
        val languages = TranslationLanguage.values().map { it.language }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Translation Language")
            setSingleChoiceItems(languages, config.pdfPaperSize.ordinal) { dialog, selectedIndex ->
                config.translationLanguage = TranslationLanguage.values()[selectedIndex]
                action(config.translationLanguage)
                dialog.dismiss()
            }
        }.create().show()
    }
}