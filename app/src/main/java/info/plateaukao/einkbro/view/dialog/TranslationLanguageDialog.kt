package info.plateaukao.einkbro.view.dialog

import android.content.Context
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.TranslationLanguage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslationLanguageDialog(val context: Context) : KoinComponent {
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

    suspend fun showDualCaptionLocale() {
        // add support for "None" option
        val languages = TranslationLanguage.values().map { it.language }
            .toMutableList().apply { add(0, "None") }

        val selectedIndex = ListSettingWithNameDialog(
            context,
            R.string.setting_dual_caption,
            languages,
            getDualCaptionIndex(config.dualCaptionLocale)
        ).show() ?: return

        if (selectedIndex == 0) {
            config.dualCaptionLocale = ""
        } else {
            config.dualCaptionLocale = TranslationLanguage.values()[selectedIndex - 1].value
        }
    }

    private fun getDualCaptionIndex(locale: String): Int =
        if (locale.isEmpty()) 0
        else TranslationLanguage.values().indexOfFirst { it.value == locale } + 1

    suspend fun showPapagoSourceLanguage(): TranslationLanguage? {
        val languages = listOf(
            TranslationLanguage.KO,
            TranslationLanguage.EN,
            TranslationLanguage.JA,
            TranslationLanguage.ZH_CN,
            TranslationLanguage.ZH_TW,
            TranslationLanguage.ES,
            TranslationLanguage.FR,
            TranslationLanguage.VI,
            TranslationLanguage.TH,
            TranslationLanguage.ID,
            TranslationLanguage.DE,
            TranslationLanguage.RU,
            TranslationLanguage.IT,
            TranslationLanguage.PT,
        ).map { it.language }

        val selectedIndex = ListSettingWithNameDialog(
            context,
            R.string.source_language,
            languages,
            languages.indexOf(config.sourceLanguage.language)
        ).show() ?: return null

        config.sourceLanguage = TranslationLanguage.findByLanguage(languages[selectedIndex])
        return config.sourceLanguage
    }
}