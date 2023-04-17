package info.plateaukao.einkbro.view.dialog

import android.content.Context
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.TranslationLanguage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslationLanguageDialog(val context: Context): KoinComponent {
    private val config: ConfigManager by inject()

    suspend fun show(): TranslationLanguage? {
        val languages = TranslationLanguage.values().map { it.language }

        val selectedIndex = ListSettingWithNameDialog(
            context,
            R.string.translation_language,
            languages,
            config.translationLanguage.ordinal
        ).show() ?: return null

        config.translationLanguage = TranslationLanguage.values()[selectedIndex]
        return config.translationLanguage
    }
}