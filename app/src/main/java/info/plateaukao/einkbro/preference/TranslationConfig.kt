package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.Orientation

class TranslationConfig(private val sp: SharedPreferences) {

    var translationLanguage: TranslationLanguage
        get() = TranslationLanguage.entries[sp.getInt(
            K_TRANSLATE_LANGUAGE,
            TranslationLanguage.EN.ordinal
        )]
        set(value) {
            sp.edit { putInt(K_TRANSLATE_LANGUAGE, value.ordinal) }
        }

    var sourceLanguage: TranslationLanguage
        get() = TranslationLanguage.entries[sp.getInt(
            K_SOURCE_LANGUAGE,
            TranslationLanguage.KO.ordinal
        )]
        set(value) {
            sp.edit { putInt(K_SOURCE_LANGUAGE, value.ordinal) }
        }

    var translationOrientation: Orientation
        get() = Orientation.entries[sp.getInt(
            K_TRANSLATE_ORIENTATION,
            Orientation.Horizontal.ordinal
        )]
        set(value) {
            sp.edit { putInt(K_TRANSLATE_ORIENTATION, value.ordinal) }
        }

    var translationMode: TranslationMode
        get() = sp.getInt(K_TRANSLATION_MODE, TranslationMode.TRANSLATE_BY_PARAGRAPH.ordinal).let { index ->
            TranslationMode.entries.getOrElse(index) { TranslationMode.TRANSLATE_BY_PARAGRAPH }
        }
        set(value) = sp.edit { putInt(K_TRANSLATION_MODE, value.ordinal) }

    var translationTextStyle: TranslationTextStyle
        get() = TranslationTextStyle.entries[sp.getInt("K_TRANSLATION_TEXT_STYLE", 1)]
        set(value) = sp.edit { putInt("K_TRANSLATION_TEXT_STYLE", value.ordinal) }

    var translationPanelSwitched by BooleanPreference(sp, K_TRANSLATE_PANEL_SWITCHED, false)
    var translationScrollSync by BooleanPreference(sp, K_TRANSLATE_SCROLL_SYNC, false)
    var twoPanelLinkHere by BooleanPreference(sp, K_TWO_PANE_LINK_HERE, false)

    var showTranslatedImageToSecondPanel by BooleanPreference(
        sp,
        K_SHOW_TRANSLATED_IMAGE_TO_SECOND_PANEL,
        true
    )

    var enableInplaceParagraphTranslate by
    BooleanPreference(sp, K_ENABLE_IN_PLACE_PARAGRAPH_TRANSLATE, true)

    var preferredTranslateLanguageString by StringPreference(sp, K_TRANSLATED_LANGS)

    companion object {
        const val K_TRANSLATE_LANGUAGE = "sp_translate_language"
        const val K_SOURCE_LANGUAGE = "sp_source_language"
        const val K_TRANSLATE_ORIENTATION = "sp_translate_orientation"
        const val K_TRANSLATION_MODE = "sp_translation_mode"
        const val K_TRANSLATE_PANEL_SWITCHED = "sp_translate_panel_switched"
        const val K_TRANSLATE_SCROLL_SYNC = "sp_translate_scroll_sync"
        const val K_TWO_PANE_LINK_HERE = "sp_two_pane_link_here"
        const val K_SHOW_TRANSLATED_IMAGE_TO_SECOND_PANEL =
            "sp_show_translated_image_to_second_panel"
        const val K_ENABLE_IN_PLACE_PARAGRAPH_TRANSLATE = "sp_enable_in_place_paragraph_translate"
        const val K_TRANSLATED_LANGS = "sp_translated_langs"
    }
}
