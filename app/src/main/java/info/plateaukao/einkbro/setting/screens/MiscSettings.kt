package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute.UserAgent
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.TranslationTextStyle
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.NavigateSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ValueSettingItem
import info.plateaukao.einkbro.view.dialog.PrinterDocumentPaperSizeDialog
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import kotlinx.coroutines.launch

fun buildMiscSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ListSettingWithEnumItem(
            R.string.setting_title_highlight_style,
            0,
            R.string.setting_summary_highlight_style,
            config = config.display::highlightStyle,
            options = HighlightStyle.entries
                .map { it.stringResId },
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_translation_style,
            0,
            R.string.setting_summary_translation_style,
            config = config.translation::translationTextStyle,
            options = TranslationTextStyle.entries.map { it.stringResId },
        ),
        NavigateSettingItem(
            R.string.setting_title_userAgent,
            0,
            destination = UserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_edit_homepage,
            0,
            config = config::favoriteUrl,
            showValue = false
        ),
        ActionSettingItem(R.string.setting_title_pdf_paper_size, 0) {
            PrinterDocumentPaperSizeDialog(
                deps.activity
            ).show()
        },
        DividerSettingItem(),
//        BooleanSettingItem(
//            R.string.setting_title_enable_inplace_translate,
//            0,
//            R.string.setting_summary_enable_inplace_translate,
//            config::enableInplaceParagraphTranslate
//        ),
        ValueSettingItem(
            R.string.setting_title_translated_langs,
            0,
            R.string.setting_summary_translated_langs,
            config.translation::preferredTranslateLanguageString
        ),
        ValueSettingItem(
            R.string.translate_image_key,
            0,
            R.string.translate_image_key_summary,
            config = config.ai::imageApiKey,
            showValue = false
        ),
        ActionSettingItem(
            R.string.setting_dual_caption,
            0,
            R.string.setting_summary_dual_caption,
        ) {
            deps.lifecycleScope.launch {
                TranslationLanguageDialog(deps.activity).showDualCaptionLocale()
            }
        },
        BooleanSettingItem(
            R.string.setting_title_reader_keep_extra_content,
            0,
            R.string.setting_summary_reader_keep_extra_content,
            config.display::readerKeepExtraContent,
        ),
    )
}
