package de.baumann.browser.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.util.TranslationLanguage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslationLanguageDialog(val context: Context): KoinComponent {
    private val config: ConfigManager by inject()

    fun show(action: (TranslationLanguage) -> Unit) {
        val languages = TranslationLanguage.values().map { it.language }.toTypedArray()

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setTitle("Translation Language")
            setSingleChoiceItems(
                    languages,
                    config.translationLanguage.ordinal
            ) { dialog, selectedIndex ->
                config.translationLanguage = TranslationLanguage.values()[selectedIndex]
                action(config.translationLanguage)
                dialog.dismiss()
            }
        }.create().show()
    }
}