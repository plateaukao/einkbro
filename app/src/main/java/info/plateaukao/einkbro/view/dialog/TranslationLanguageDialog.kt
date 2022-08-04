package info.plateaukao.einkbro.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.TranslationLanguage
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