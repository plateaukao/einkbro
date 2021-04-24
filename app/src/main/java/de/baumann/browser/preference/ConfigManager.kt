package de.baumann.browser.preference

import android.content.Context
import android.content.SharedPreferences
import android.print.PrintAttributes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.util.Constants
import de.baumann.browser.view.toolbaricons.ToolbarAction

class ConfigManager(private val context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var touchAreaHint: Boolean
    get() = sp.getBoolean("sp_touch_area_hint", true)
    set(value) {sp.edit { putBoolean("sp_touch_area_hint", value) } }

    var boldFontStyle: Boolean
        get() = sp.getBoolean(K_BOLD_FONT, false)
        set(value) {sp.edit { putBoolean(K_BOLD_FONT, value) } }

    var fontStyleSerif: Boolean
        get() = sp.getBoolean(K_FONT_STYLE_SERIF, false)
        set(value) {sp.edit { putBoolean(K_FONT_STYLE_SERIF, value) } }

    var pageReservedOffset: Int
        get() = sp.getInt("sp_page_turn_left_value", 80)
        set(value) {sp.edit { putInt("sp_page_turn_left_value", value) } }

    var fontSize: Int
        get() = sp.getString(K_FONT_SIZE, "100")?.toInt() ?: 100
        set(value) {sp.edit { putString(K_FONT_SIZE, value.toString()) } }

    var fabPosition: FabPosition
        get() = FabPosition.values()[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) } }

    var screenshot: Int
        get() = sp.getInt("screenshot", 0)
        set(value) {sp.edit { putInt("screenshot", value) } }

    var pdfPaperSize: PaperSize
        get() = PaperSize.values()[sp.getInt("pdf_paper_size", PaperSize.ISO_13.ordinal)]
        set(value) {sp.edit { putInt("pdf_paper_size", value.ordinal) } }

    var favoriteUrl: String
        get() = sp.getString(K_FAVORITE_URL, Constants.DEFAULT_HOME_URL) ?: Constants.DEFAULT_HOME_URL
        set(value) { sp.edit { putString(K_FAVORITE_URL, value) } }

    val toolbarIcons: List<ToolbarAction>
        get() {
            val iconListString = sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings()) ?: ""
            return iconStringToEnumList(iconListString)
        }

    private fun iconStringToEnumList(iconListString: String): List<ToolbarAction> {
        if (iconListString.isBlank()) return listOf()

        return iconListString.split(",").map{ ToolbarAction.fromOrdinal(it.toInt())}
    }

    private fun getDefaultIconStrings(): String {
        val iconArray = context.resources.getStringArray(R.array.default_toolbar_icons)
        return iconArray.joinToString(",")
    }

    companion object {
        const val K_TOUCH_AREA_TYPE = "sp_touch_area_type"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_BOLD_FONT = "sp_bold_font"
        const val K_FONT_STYLE_SERIF = "sp_font_style_serif"
        const val K_NAV_POSITION = "nav_position"
        const val K_FONT_SIZE = "sp_fontSize"
        const val K_FAVORITE_URL = "favoriteURL"
    }
}

enum class PaperSize(val sizeString: String, val mediaSize: PrintAttributes.MediaSize) {
    ISO_13("A4 (13\")", PrintAttributes.MediaSize.ISO_A4),
    SIZE_10("A5 (10\")", PrintAttributes.MediaSize.ISO_A5),
    ISO_67("Hisense A7 (6.7\")", PrintAttributes.MediaSize.PRC_5),
    SIZE_8("C6 (8\")", PrintAttributes.MediaSize.ISO_C6),
}

enum class FabPosition {
    Right, Left, Center
}