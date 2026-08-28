package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.view.compose.UiThemeState

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
            if (readerFontType == FontType.CUSTOM) {
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
        set(value) {
            sp.edit { putString(K_DARK_MODE, value.ordinal.toString()) }
            UiThemeState.darkMode.value = value
        }

    var uiTheme: UiTheme
        get() = UiTheme.entries.getOrElse(sp.getInt(K_UI_THEME, 0)) { UiTheme.CLASSIC }
        set(value) {
            sp.edit { putInt(K_UI_THEME, value.ordinal) }
            UiThemeState.current.value = value
        }

    var uiBorder: UiBorder
        get() {
            migrateUiStyleIfNeeded()
            return UiBorder.entries.getOrElse(
                sp.getInt(K_UI_BORDER, UiBorder.CLASSIC.ordinal)
            ) { UiBorder.CLASSIC }
        }
        set(value) {
            sp.edit { putInt(K_UI_BORDER, value.ordinal) }
            UiThemeState.uiBorder.value = value
        }

    var uiFill: UiFill
        get() {
            migrateUiStyleIfNeeded()
            return UiFill.entries.getOrElse(sp.getInt(K_UI_FILL, 0)) { UiFill.NONE }
        }
        set(value) {
            sp.edit { putInt(K_UI_FILL, value.ordinal) }
            UiThemeState.uiFill.value = value
        }

    // maps the legacy single style preference onto the border/fill pair
    private fun migrateUiStyleIfNeeded() {
        if (sp.contains(K_UI_BORDER) || !sp.contains(K_UI_STYLE)) return
        val (border, fill) = when (sp.getInt(K_UI_STYLE, 0)) {
            1 -> UiBorder.ROUND to UiFill.NONE
            2 -> UiBorder.SHARP to UiFill.NONE
            3 -> UiBorder.PAPER to UiFill.NONE
            4 -> UiBorder.DASHED to UiFill.NONE
            5 -> UiBorder.NONE to UiFill.TONAL
            6 -> UiBorder.ROUND to UiFill.GRADIENT
            7 -> UiBorder.STAMP to UiFill.NONE
            8, 9 -> UiBorder.NONE to UiFill.GRADIENT
            10 -> UiBorder.SKETCH to UiFill.NONE
            11 -> UiBorder.CERTIFICATE to UiFill.NONE
            12 -> UiBorder.STICKER to UiFill.NONE
            else -> UiBorder.CLASSIC to UiFill.NONE
        }
        sp.edit {
            putInt(K_UI_BORDER, border.ordinal)
            putInt(K_UI_FILL, fill.ordinal)
        }
    }

    // gradient flow direction in degrees (0 = left-to-right, 90 = top-down)
    var gradientAngle: Int
        get() = sp.getInt(K_GRADIENT_ANGLE, 45)
        set(value) {
            sp.edit { putInt(K_GRADIENT_ANGLE, value) }
            UiThemeState.gradientAngle.value = value
        }

    // percent: 100 = the style's default blend strength
    var gradientLevel: Int
        get() = sp.getInt(K_GRADIENT_LEVEL, 100)
        set(value) {
            sp.edit { putInt(K_GRADIENT_LEVEL, value) }
            UiThemeState.gradientLevel.value = value
        }

    var uiThemeInverted: Boolean
        get() = sp.getBoolean(K_UI_THEME_INVERTED, false)
        set(value) {
            sp.edit { putBoolean(K_UI_THEME_INVERTED, value) }
            UiThemeState.inverted.value = value
        }

    var customThemeColor: Int
        get() = sp.getInt(K_CUSTOM_THEME_COLOR, DEFAULT_CUSTOM_THEME_COLOR)
        set(value) {
            sp.edit { putInt(K_CUSTOM_THEME_COLOR, value) }
            UiThemeState.customColor.value = androidx.compose.ui.graphics.Color(value)
        }

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

    var einkImageMode: EinkImageMode
        get() = EinkImageMode.entries.getOrElse(
            sp.getInt(K_EINK_IMAGE_MODE, 0)
        ) { EinkImageMode.DEEP }
        set(value) = sp.edit { putInt(K_EINK_IMAGE_MODE, value.ordinal) }

    var highlightStyle: HighlightStyle
        get() = HighlightStyle.entries[sp.getInt(K_HIGHLIGHT_STYLE, 0)]
        set(value) = sp.edit { putInt(K_HIGHLIGHT_STYLE, value.ordinal) }

    var enableZoom by BooleanPreference(sp, K_ENABLE_ZOOM, true)
    var enableZoomTextWrapReflow by BooleanPreference(sp, K_ENABLE_ZOOM_TEXT_WRAP_REFLOW, false)
    var zoomInCustomView by BooleanPreference(sp, "sp_zoom_in_custom_view", false)
    var readerKeepExtraContent by BooleanPreference(sp, "sp_reader_keep_extra_content", false)

    var paddingForReaderMode by IntPreference(sp, K_PADDING_FOR_READER_MODE, 10)

    // Line spacing (CSS line-height) in tenths: 15 -> 1.5
    var readerLineSpacing by IntPreference(sp, K_READER_LINE_SPACING, 15)
    var readerTwoColumnInLandscape by BooleanPreference(sp, K_READER_TWO_COLUMN_LANDSCAPE, false)

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
        const val K_UI_THEME = "sp_ui_theme"
        const val K_CUSTOM_THEME_COLOR = "sp_custom_theme_color"
        const val K_UI_STYLE = "sp_ui_style"
        const val K_UI_BORDER = "sp_ui_border"
        const val K_UI_FILL = "sp_ui_fill"
        const val K_UI_THEME_INVERTED = "sp_ui_theme_inverted"
        const val K_GRADIENT_ANGLE = "sp_gradient_angle"
        const val K_GRADIENT_LEVEL = "sp_gradient_level"
        const val DEFAULT_CUSTOM_THEME_COLOR = 0xFF4A90D9.toInt()
        const val K_ENABLE_IMAGE_ADJUSTMENT = "sp_image_adjustment"
        const val K_EINK_IMAGE_MODE = "sp_eink_image_mode"
        const val K_HIGHLIGHT_STYLE = "sp_highlight_style"
        const val K_ENABLE_ZOOM = "sp_enable_zoom"
        const val K_ENABLE_ZOOM_TEXT_WRAP_REFLOW = "sp_enable_zoom_text_wrap_reflow"
        private const val K_PADDING_FOR_READER_MODE = "sp_padding_for_reader_mode"
        private const val K_READER_LINE_SPACING = "sp_reader_line_spacing"
        private const val K_READER_TWO_COLUMN_LANDSCAPE = "sp_reader_two_column_landscape"
    }
}
