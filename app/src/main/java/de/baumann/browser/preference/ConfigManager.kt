package de.baumann.browser.preference

import android.content.Context
import android.content.SharedPreferences
import android.print.PrintAttributes
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class ConfigManager(private val context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var touchAreaHint: Boolean
    get() = sp.getBoolean("sp_touch_area_hint", true)
    set(value) {sp.edit { putBoolean("sp_touch_area_hint", value) } }

    var boldFontStyle: Boolean
        get() = sp.getBoolean(K_BOLD_FONT, false)
        set(value) {sp.edit { putBoolean(K_BOLD_FONT, value) } }

    var pdfCreated: Boolean
        get() = sp.getBoolean("pdf_create", false)
        set(value) {sp.edit { putBoolean("pdf_create", value) } }

    var fontStyleSerif: Boolean
        get() = sp.getBoolean(K_FONT_STYLE_SERIF, false)
        set(value) {sp.edit { putBoolean(K_FONT_STYLE_SERIF, value) } }

    var pageReservedOffset: Int
        get() = sp.getInt("sp_page_turn_left_value", 80)
        set(value) {sp.edit { putInt("sp_page_turn_left_value", value) } }

    var fontSize: Int
        get() = sp.getString("sp_fontSize", "100")?.toInt() ?: 100
        set(value) {sp.edit { putString("sp_fontSize", value.toString()) } }

    var screenshot: Int
        get() = sp.getInt("screenshot", 0)
        set(value) {sp.edit { putInt("screenshot", value) } }

    var pdfPaperSize: PaperSize
        get() = PaperSize.values()[sp.getInt("pdf_paper_size", PaperSize.ISO_13.ordinal)]
        set(value) {sp.edit { putInt("pdf_paper_size", value.ordinal) } }

    companion object {
        const val K_TOUCH_AREA_TYPE = "sp_touch_area_type"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_BOLD_FONT = "sp_bold_font"
        const val K_FONT_STYLE_SERIF = "sp_font_style_serif"
    }
}

enum class PaperSize(val sizeString: String, val mediaSize: PrintAttributes.MediaSize) {
    ISO_13("A4 (13\")", PrintAttributes.MediaSize.ISO_A4),
    SIZE_10("A5 (10\")", PrintAttributes.MediaSize.ISO_A5),
    ISO_67("Hisense A7 (6.7\")", PrintAttributes.MediaSize.PRC_5),
    SIZE_8("C6 (8\")", PrintAttributes.MediaSize.ISO_C6),
}