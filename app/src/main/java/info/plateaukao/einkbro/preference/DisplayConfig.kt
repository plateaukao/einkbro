package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit

class DisplayConfig(private val sp: SharedPreferences) {

    var fontSize: Int
        get() = sp.getString(K_FONT_SIZE, "100")?.toInt() ?: 100
        set(value) {
            sp.edit { putString(K_FONT_SIZE, value.toString()) }
        }
    var customFontSize: Int
        get() = sp.getString(K_CUSTOM_FONT_SIZE, "100")?.toInt() ?: fontSize
        set(value) {
            sp.edit { putString(K_CUSTOM_FONT_SIZE, value.toString()) }
        }
    var readerFontSize: Int
        get() = sp.getString(K_READER_FONT_SIZE, fontSize.toString())?.toInt() ?: fontSize
        set(value) {
            sp.edit { putString(K_READER_FONT_SIZE, value.toString()) }
        }

    var fontBoldness by IntPreference(sp, K_FONT_BOLDNESS, 700)

    var boldFontStyle by BooleanPreference(sp, K_BOLD_FONT, false)
    var blackFontStyle by BooleanPreference(sp, K_BLACK_FONT, false)

    var fontType: FontType
        get() = FontType.entries[sp.getInt(K_FONT_TYPE, 0)]
        set(value) = sp.edit { putInt(K_FONT_TYPE, value.ordinal) }
    var readerFontType: FontType
        get() = FontType.entries[sp.getInt(K_READER_FONT_TYPE, fontType.ordinal)]
        set(value) = sp.edit { putInt(K_READER_FONT_TYPE, value.ordinal) }

    var customFontInfo: CustomFontInfo?
        get() = sp.getString(K_CUSTOM_FONT, "")?.toCustomFontInfo()
        set(value) {
            sp.edit { putString(K_CUSTOM_FONT, value?.toSerializedString().orEmpty()) }
            if (fontType == FontType.CUSTOM) {
                customFontChanged = true
            }
        }
    var readerCustomFontInfo: CustomFontInfo?
        get() = sp.getString(K_READER_CUSTOM_FONT, "")?.toCustomFontInfo()
        set(value) {
            sp.edit { putString(K_READER_CUSTOM_FONT, value?.toSerializedString().orEmpty()) }
            if (fontType == FontType.CUSTOM) {
                customFontChanged = true
            }
        }

    var fontFolderUri: String?
        get() = sp.getString(K_FONT_FOLDER_URI, null)
        set(value) {
            sp.edit { putString(K_FONT_FOLDER_URI, value.orEmpty()) }
        }

    var customFontChanged by BooleanPreference(sp, K_CUSTOM_FONT_CHANGED, false)

    var darkMode: DarkMode
        get() = DarkMode.entries[sp.getString(K_DARK_MODE, "2")?.toInt() ?: 2]
        set(value) = sp.edit { putString(K_DARK_MODE, value.ordinal.toString()) }

    var einkImageAdjustment: EinkImageAdjustment
        get() = try {
            EinkImageAdjustment.entries.getOrElse(
                sp.getInt(K_ENABLE_IMAGE_ADJUSTMENT, 0)
            ) { EinkImageAdjustment.OFF }
        } catch (e: ClassCastException) {
            // migrate from old boolean preference
            sp.edit { remove(K_ENABLE_IMAGE_ADJUSTMENT) }
            EinkImageAdjustment.OFF
        }
        set(value) = sp.edit { putInt(K_ENABLE_IMAGE_ADJUSTMENT, value.ordinal) }

    var highlightStyle: HighlightStyle
        get() = HighlightStyle.entries[sp.getInt(K_HIGHLIGHT_STYLE, 0)]
        set(value) = sp.edit { putInt(K_HIGHLIGHT_STYLE, value.ordinal) }

    var enableZoom by BooleanPreference(sp, K_ENABLE_ZOOM, true)
    var enableZoomTextWrapReflow by BooleanPreference(sp, K_ENABLE_ZOOM_TEXT_WRAP_REFLOW, false)
    var zoomInCustomView by BooleanPreference(sp, "sp_zoom_in_custom_view", false)
    var readerKeepExtraContent by BooleanPreference(sp, "sp_reader_keep_extra_content", false)

    var paddingForReaderMode by IntPreference(sp, K_PADDING_FOR_READER_MODE, 10)

    var pdfPaperSize: PaperSize
        get() = PaperSize.entries[sp.getInt("pdf_paper_size", PaperSize.ISO_13.ordinal)]
        set(value) {
            sp.edit { putInt("pdf_paper_size", value.ordinal) }
        }

    companion object {
        const val K_FONT_SIZE = "sp_fontSize"
        const val K_CUSTOM_FONT_SIZE = "sp_customFontSize"
        const val K_READER_FONT_SIZE = "sp_reader_fontSize"
        const val K_FONT_BOLDNESS = "sp_font_boldness"
        const val K_BOLD_FONT = "sp_bold_font"
        const val K_BLACK_FONT = "sp_black_font"
        const val K_FONT_TYPE = "sp_font_type"
        const val K_READER_FONT_TYPE = "sp_reader_font_type"
        const val K_CUSTOM_FONT = "sp_custom_font"
        const val K_READER_CUSTOM_FONT = "sp_reader_custom_font"
        const val K_CUSTOM_FONT_CHANGED = "sp_custom_font_changed"
        const val K_FONT_FOLDER_URI = "sp_font_folder_uri"
        const val K_DARK_MODE = "sp_dark_mode"
        const val K_ENABLE_IMAGE_ADJUSTMENT = "sp_image_adjustment"
        const val K_HIGHLIGHT_STYLE = "sp_highlight_style"
        const val K_ENABLE_ZOOM = "sp_enable_zoom"
        const val K_ENABLE_ZOOM_TEXT_WRAP_REFLOW = "sp_enable_zoom_text_wrap_reflow"
        private const val K_PADDING_FOR_READER_MODE = "sp_padding_for_reader_mode"
    }
}
