package de.baumann.browser.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.print.PrintAttributes
import androidx.core.content.edit
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.Bookmark
import de.baumann.browser.epub.EpubFileInfo
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.util.Constants
import de.baumann.browser.util.TranslationLanguage
import de.baumann.browser.view.Orientation
import de.baumann.browser.view.toolbaricons.ToolbarAction
import de.baumann.browser.view.viewControllers.OverviewTab
import org.koin.core.component.KoinComponent
import java.lang.Exception
import java.util.*
import kotlin.reflect.KMutableProperty0

class ConfigManager(
    private val context: Context,
    private val sp: SharedPreferences
) : KoinComponent {

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    var enableWebBkgndLoad: Boolean
        get() = sp.getBoolean(K_BKGND_LOAD, true)
        set(value) {
            sp.edit { putBoolean(K_BKGND_LOAD, value)}
        }

    var enableJavascript: Boolean
        get() = sp.getBoolean(K_JAVASCRIPT, false)
        set(value) {
            sp.edit { putBoolean(K_JAVASCRIPT, value) }
        }

    var isToolbarOnTop: Boolean
        get() = sp.getBoolean(K_TOOLBAR_TOP, false)
        set(value) {
            sp.edit { putBoolean(K_TOOLBAR_TOP, value) }
        }

    var enableViBinding: Boolean
        get() = sp.getBoolean(K_VI_BINDING, false)
        set(value) {
            sp.edit { putBoolean(K_VI_BINDING, value) }
        }

    var isMultitouchEnabled: Boolean
        get() = sp.getBoolean(K_MULTITOUCH, false)
        set(value) {
            sp.edit { putBoolean(K_MULTITOUCH, value) }
        }

    var whiteBackground: Boolean
        get() = sp.getBoolean(K_WHITE_BACKGROUND, false)
        set(value) {
            sp.edit { putBoolean(K_WHITE_BACKGROUND, value) }
        }

    var useUpDownPageTurn: Boolean
        get() = sp.getBoolean(K_UPDOWN_PAGE_TURN, false)
        set(value) {
            sp.edit { putBoolean(K_UPDOWN_PAGE_TURN, value) }
        }

    var touchAreaHint: Boolean
        get() = sp.getBoolean(K_TOUCH_HINT, true)
        set(value) {
            sp.edit { putBoolean(K_TOUCH_HINT, value) }
        }

    var volumePageTurn: Boolean
        get() = sp.getBoolean(K_VOLUME_PAGE_TURN, true)
        set(value) {
            sp.edit { putBoolean(K_VOLUME_PAGE_TURN, value) }
        }

    var boldFontStyle: Boolean
        get() = sp.getBoolean(K_BOLD_FONT, false)
        set(value) {
            sp.edit { putBoolean(K_BOLD_FONT, value) }
        }

    var shouldSaveTabs: Boolean
        get() = sp.getBoolean(K_SHOULD_SAVE_TABS, false)
        set(value) {
            sp.edit { putBoolean(K_SHOULD_SAVE_TABS, value) }
        }

    var isIncognitoMode: Boolean
        get() = sp.getBoolean(K_IS_INCOGNITO_MODE, false)
        set(value) {
            cookies = !value
            saveHistory = !value
            sp.edit { putBoolean(K_IS_INCOGNITO_MODE, value) }
        }

    var shouldInvert: Boolean
        get() = sp.getBoolean(K_SHOULD_INVERT, false)
        set(value) {
            sp.edit { putBoolean(K_SHOULD_INVERT, value) }
        }

    var adBlock: Boolean
        get() = sp.getBoolean(K_ADBLOCK, true)
        set(value) {
            sp.edit { putBoolean(K_ADBLOCK, value) }
        }

    var cookies: Boolean
        get() = sp.getBoolean(K_COOKIES, true)
        set(value) {
            sp.edit { putBoolean(K_COOKIES, value) }
        }

    var saveHistory: Boolean
        get() = sp.getBoolean(K_SAVE_HISTORY, true)
        set(value) {
            sp.edit { putBoolean(K_SAVE_HISTORY, value) }
        }

    var shareLocation: Boolean
        get() = sp.getBoolean(K_SHARE_LOCATION, false)
        set(value) {
            sp.edit { putBoolean(K_SHARE_LOCATION, value) }
        }

    var enableTouchTurn: Boolean
        get() = sp.getBoolean(K_ENABLE_TOUCH, false)
        set(value) {
            sp.edit { putBoolean(K_ENABLE_TOUCH, value) }
        }

    var keepAwake: Boolean
        get() = sp.getBoolean(K_KEEP_AWAKE, false)
        set(value) {
            sp.edit { putBoolean(K_KEEP_AWAKE, value) }
        }

    var desktop: Boolean
        get() = sp.getBoolean(K_DESKTOP, false)
        set(value) {
            sp.edit { putBoolean(K_DESKTOP, value) }
        }

    var continueMedia: Boolean
        get() = sp.getBoolean(K_MEDIA_CONTINUE, false)
        set(value) {
            sp.edit { putBoolean(K_MEDIA_CONTINUE, value) }
        }

    var restartChanged: Boolean
        get() = sp.getBoolean(K_RESTART_CHANGED, false)
        set(value) {
            sp.edit { putBoolean(K_RESTART_CHANGED, value) }
        }

    fun maybeInitPreference() {
        if (sp.getString("saved_key_ok", "no") == "no") {
            if (Locale.getDefault().country == "CN") {
                sp.edit().putString("SP_SEARCH_ENGINE_9", "2").apply()
            }
            sp.edit {
                putString("saved_key_ok", "yes")
                putString("setting_gesture_tb_up", "08")
                putString("setting_gesture_tb_down", "01")
                putString("setting_gesture_tb_left", "07")
                putString("setting_gesture_tb_right", "06")
                putString("setting_gesture_nav_up", "04")
                putString("setting_gesture_nav_down", "05")
                putString("setting_gesture_nav_left", "03")
                putString("setting_gesture_nav_right", "02")
                putBoolean("SP_LOCATION_9", false)
            }
        }

    }

    var pageReservedOffset: Int
        get() = sp.getInt("sp_page_turn_left_value", 80)
        set(value) {
            sp.edit { putInt("sp_page_turn_left_value", value) }
        }

    var fontSize: Int
        get() = sp.getString(K_FONT_SIZE, "100")?.toInt() ?: 100
        set(value) {
            sp.edit { putString(K_FONT_SIZE, value.toString()) }
        }
    var touchAreaCustomizeY: Int
        get() = sp.getInt(K_TOUCH_AREA_OFFSET, 0)
        set(value) {
            sp.edit { putInt(K_TOUCH_AREA_OFFSET, value) }
        }

    val customUserAgent: String
        get() = sp.getString(K_CUSTOM_USER_AGENT, "") ?: ""

    val customProcessTextUrl: String
        get() = sp.getString(K_CUSTOM_PROCESS_TEXT_URL, "") ?: ""

    var fabPosition: FabPosition
        get() = FabPosition.values()[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {
            sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) }
        }

    var touchAreaType: TouchAreaType
        get() = TouchAreaType.values()[sp.getInt(K_TOUCH_AREA_TYPE, 0)]
        set(value) {
            sp.edit { putInt(K_TOUCH_AREA_TYPE, value.ordinal) }
        }

    var pdfPaperSize: PaperSize
        get() = PaperSize.values()[sp.getInt("pdf_paper_size", PaperSize.ISO_13.ordinal)]
        set(value) {
            sp.edit { putInt("pdf_paper_size", value.ordinal) }
        }

    var translationLanguage: TranslationLanguage
        get() = TranslationLanguage.values()[sp.getInt(K_TRANSLATE_LANGUAGE, getDefaultTranslationLanguage().ordinal)]
        set(value) {
            sp.edit { putInt(K_TRANSLATE_LANGUAGE, value.ordinal) }
        }

    var translationOrientation: Orientation
        get() = Orientation.values()[sp.getInt(K_TRANSLATE_ORIENTATION, Orientation.Horizontal.ordinal)]
        set(value) {
            sp.edit { putInt(K_TRANSLATE_ORIENTATION, value.ordinal) }
        }

    var translationPanelSwitched: Boolean
        get() = sp.getBoolean(K_TRANSLATE_PANEL_SWITCHED, false)
        set(value) {
            sp.edit { putBoolean(K_TRANSLATE_PANEL_SWITCHED, value) }
        }

    var translationScrollSync: Boolean
        get() = sp.getBoolean(K_TRANSLATE_SCROLL_SYNC, false)
        set(value) {
            sp.edit { putBoolean(K_TRANSLATE_SCROLL_SYNC, value) }
        }

    var twoPanelLinkHere: Boolean
        get() = sp.getBoolean(K_TWO_PANE_LINK_HERE, false)
        set(value) {
            sp.edit { putBoolean(K_TWO_PANE_LINK_HERE, value) }
        }

    var switchTouchAreaAction: Boolean
        get() = sp.getBoolean(K_TOUCH_AREA_ACTION_SWITCH, false)
        set(value) {
            sp.edit { putBoolean(K_TOUCH_AREA_ACTION_SWITCH, value) }
        }

    var hideTouchAreaWhenInput: Boolean
        get() = sp.getBoolean(K_TOUCH_AREA_HIDE_WHEN_INPUT, false)
        set(value) {
            sp.edit { putBoolean(K_TOUCH_AREA_HIDE_WHEN_INPUT, value) }
        }

    var customFontChanged: Boolean
        get() = sp.getBoolean(K_CUSTOM_FONT_CHANGED, false)
        set(value) {
            sp.edit { putBoolean(K_CUSTOM_FONT_CHANGED, value) }
        }

    var showRecentBookmarks: Boolean
        get() = sp.getBoolean(K_SHOW_RECENT_BOOKMARKS, false)
        set(value) {
            sp.edit { putBoolean(K_SHOW_RECENT_BOOKMARKS, value) }
        }

    var favoriteUrl: String
        get() = sp.getString(K_FAVORITE_URL, Constants.DEFAULT_HOME_URL)
                ?: Constants.DEFAULT_HOME_URL
        set(value) {
            sp.edit { putString(K_FAVORITE_URL, value) }
        }

    var toolbarActions: List<ToolbarAction>
        get() {
            val key = if (ViewUnit.isLandscape(context)) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
            val iconListString = sp.getString(key, sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings())) ?: ""
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key = if (ViewUnit.isLandscape(context)) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

    var customFontInfo: CustomFontInfo?
        get() = sp.getString(K_CUSTOM_FONT, "")?.toCustomFontInfo()
        set(value) {
            sp.edit { putString(K_CUSTOM_FONT, value?.toSerializedString() ?: "") }
            if (fontType == FontType.CUSTOM) {
                customFontChanged = true
            }
        }

    var recentBookmarks: List<RecentBookmark>
        get() {
            val string = sp.getString(K_RECENT_BOOKMARKS, "") ?: ""
            if (string.isBlank()) return emptyList()

            return try {
                string.split(RECENT_BOOKMARKS_SEPARATOR)
                        .mapNotNull { it.toRecentBookmark() }
                        .sortedByDescending { it.count }
            } catch (exception: Exception) {
                sp.edit { remove(K_RECENT_BOOKMARKS) }
                emptyList()
            }
        }
        set(value) {
            if (value.containsAll(recentBookmarks) && recentBookmarks.containsAll(value)) {
                return
            }

            sp.edit {
                if (value.isEmpty()) {
                    remove(K_RECENT_BOOKMARKS)
                } else {
                    // check if the new value the same as the old one
                    putString(
                            K_RECENT_BOOKMARKS,
                            value.joinToString(RECENT_BOOKMARKS_SEPARATOR) { it.toSerializedString() }
                    )
                }
            }
        }

    fun addRecentBookmark(bookmark: Bookmark) {
        var newList = recentBookmarks.toMutableList()
        val sameItem = newList.firstOrNull { it.url == bookmark.url }
        if (sameItem != null) {
            sameItem.count ++
        } else {
            newList.add(RecentBookmark(bookmark.title, bookmark.url, 1))
        }

        recentBookmarks = if (newList.size > RECENT_BOOKMARK_LIST_SIZE) {
            newList.sortedByDescending { it.count }.subList(0, RECENT_BOOKMARK_LIST_SIZE - 1)
        } else {
            newList.sortedByDescending { it.count }
        }
    }

    fun clearRecentBookmarks() {
        recentBookmarks = emptyList()
    }

    var savedAlbumInfoList: List<AlbumInfo>
        get() {
            val string = sp.getString(K_SAVED_ALBUM_INFO, "") ?: ""
            if (string.isBlank()) return emptyList()

            return try {
                string.split(ALBUM_INFO_SEPARATOR).mapNotNull { it.toAlbumInfo() }
            } catch (exception: Exception) {
                sp.edit { remove(K_SAVED_ALBUM_INFO) }
                emptyList()
            }
        }
        set(value) {
            if (value.containsAll(savedAlbumInfoList) && savedAlbumInfoList.containsAll(value)) {
                return
            }

            sp.edit {
                if (value.isEmpty()) {
                    remove(K_SAVED_ALBUM_INFO)
                } else {
                    // check if the new value the same as the old one
                    putString(
                            K_SAVED_ALBUM_INFO,
                            value.joinToString(ALBUM_INFO_SEPARATOR) { it.toSerializedString() }
                    )
                }
            }
        }

    var currentAlbumIndex: Int
        get() = sp.getInt(K_SAVED_ALBUM_INDEX, 0)
        set(value) {
            if (currentAlbumIndex == value) return

            sp.edit { putInt(K_SAVED_ALBUM_INDEX, value) }
        }

    var dbVersion: Int
        get() = sp.getInt(K_DB_VERSION, 0)
        set(value) = sp.edit { putInt(K_DB_VERSION, value) }

    var fontType: FontType
        get() = FontType.values()[sp.getInt(K_FONT_TYPE, 0)]
        set(value) = sp.edit { putInt(K_FONT_TYPE, value.ordinal) }

    var translationMode: TranslationMode
        get() = TranslationMode.values()[sp.getInt(K_TRANSLATION_MODE, 6)]
        set(value) = sp.edit { putInt(K_TRANSLATION_MODE, value.ordinal) }

    var adSites: MutableSet<String>
        get() = sp.getStringSet(K_ADBLOCK_SITES, mutableSetOf()) ?: mutableSetOf()
        set(value) = sp.edit { putStringSet(K_ADBLOCK_SITES, value) }

    var savedEpubFileInfos: List<EpubFileInfo>
        get() = sp.getString(K_SAVED_EPUBS, "")?.toEpubFileInfoList() ?: mutableListOf()
        set(value) = sp.edit { putString(K_SAVED_EPUBS, toEpubFileInfosString(value))}

    var darkMode:DarkMode
        get() = DarkMode.values()[sp.getString(K_DARK_MODE, "0")?.toInt() ?: 0]
        set(value) = sp.edit { putString(K_DARK_MODE, value.ordinal.toString()) }

    private fun iconStringToEnumList(iconListString: String): List<ToolbarAction> {
        if (iconListString.isBlank()) return listOf()

        return iconListString.split(",").map { ToolbarAction.fromOrdinal(it.toInt()) }
    }

    private fun getDefaultTranslationLanguage(): TranslationLanguage {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
        return TranslationLanguage.findByLanguage(locale.language)
    }

    private fun getDefaultIconStrings(): String =
            ToolbarAction.defaultActions.joinToString(",") { action ->
                action.ordinal.toString()
            }

    companion object {
        const val K_TOUCH_AREA_TYPE = "sp_touch_area_type"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_TOOLBAR_ICONS_FOR_LARGE = "sp_toolbar_icons_for_large"
        const val K_BOLD_FONT = "sp_bold_font"
        const val K_FONT_STYLE_SERIF = "sp_font_style_serif"
        const val K_NAV_POSITION = "nav_position"
        const val K_FONT_SIZE = "sp_fontSize"
        const val K_FAVORITE_URL = "favoriteURL"
        const val K_VOLUME_PAGE_TURN = "volume_page_turn"
        const val K_SHOULD_SAVE_TABS = "sp_shouldSaveTabs"
        const val K_SAVED_ALBUM_INFO = "sp_saved_album_info"
        const val K_SAVED_ALBUM_INDEX = "sp_saved_album_index"
        const val K_DB_VERSION = "sp_db_version"
        const val K_IS_INCOGNITO_MODE = "sp_incognito"
        const val K_ADBLOCK = "SP_AD_BLOCK_9"
        const val K_SAVE_HISTORY = "saveHistory"
        const val K_COOKIES = "SP_COOKIES_9"
        const val K_SHOULD_INVERT = "sp_invert"
        const val K_TRANSLATION_MODE = "sp_translation_mode"
        const val K_ENABLE_TOUCH = "sp_enable_touch"
        const val K_TOUCH_HINT = "sp_touch_area_hint"
        const val K_SCREENSHOT = "screenshot"
        const val K_KEEP_AWAKE = "sp_screen_awake"
        const val K_DESKTOP = "sp_desktop"
        const val K_TRANSLATE_LANGUAGE = "sp_translate_language"
        const val K_TRANSLATE_ORIENTATION = "sp_translate_orientation"
        const val K_TRANSLATE_PANEL_SWITCHED = "sp_translate_panel_switched"
        const val K_TRANSLATE_SCROLL_SYNC = "sp_translate_scroll_sync"
        const val K_ADBLOCK_SITES = "sp_adblock_sites"
        const val K_CUSTOM_USER_AGENT = "userAgent"
        const val K_WHITE_BACKGROUND = "sp_whitebackground"
        const val K_UPDOWN_PAGE_TURN = "sp_useUpDownForPageTurn"
        const val K_CUSTOM_PROCESS_TEXT_URL = "sp_process_text_custom"
        const val K_TWO_PANE_LINK_HERE = "sp_two_pane_link_here"
        const val K_DARK_MODE = "sp_dark_mode"
        const val K_TOUCH_AREA_OFFSET = "sp_touch_area_offset"
        const val K_TOUCH_AREA_ACTION_SWITCH = "sp_touch_area_action_switch"
        const val K_TOUCH_AREA_HIDE_WHEN_INPUT = "sp_touch_area_hide_when_input"
        const val K_SAVED_EPUBS = "sp_saved_epubs"
        const val K_MULTITOUCH = "sp_multitouch"
        const val K_CUSTOM_FONT = "sp_custom_font"
        const val K_CUSTOM_FONT_CHANGED = "sp_custom_font_changed"
        const val K_FONT_TYPE = "sp_font_type"
        const val K_TOOLBAR_TOP = "sp_toolbar_top"
        const val K_VI_BINDING = "sp_enable_vi_binding"
        const val K_MEDIA_CONTINUE = "sp_media_continue"
        const val K_RECENT_BOOKMARKS = "sp_recent_bookmarks"
        const val K_SHOW_RECENT_BOOKMARKS = "sp_new_tab_recent_bookmarks"
        const val K_RESTART_CHANGED = "restart_changed"
        const val K_JAVASCRIPT = "SP_JAVASCRIPT_9"
        const val K_BKGND_LOAD = "sp_background_loading"
        const val K_SHARE_LOCATION = "SP_LOCATION_9"

        private const val ALBUM_INFO_SEPARATOR = "::::"
        private const val RECENT_BOOKMARKS_SEPARATOR = "::::"
        private const val EPUB_FILE_INFO_SEPARATOR = "::::"

        private const val RECENT_BOOKMARK_LIST_SIZE = 10
    }

    private fun String.toEpubFileInfoList(): MutableList<EpubFileInfo> =
            if (this.isEmpty() || this == EPUB_FILE_INFO_SEPARATOR) mutableListOf()
            else this.split(EPUB_FILE_INFO_SEPARATOR).map { fileString ->
                EpubFileInfo.fromString(fileString)
            }.toMutableList()

    private fun toEpubFileInfosString(list: List<EpubFileInfo>): String =
            list.joinToString(separator = EPUB_FILE_INFO_SEPARATOR) { it.toPrefString() }

    fun addSavedEpubFile(epubFileInfo: EpubFileInfo) {
        savedEpubFileInfos = savedEpubFileInfos.toMutableList().apply { add(epubFileInfo) }
    }

    fun removeSavedEpubFile(epubFileInfo: EpubFileInfo) {
        savedEpubFileInfos = savedEpubFileInfos.toMutableList().apply { remove(epubFileInfo) }
    }
}

enum class PaperSize(val sizeString: String, val mediaSize: PrintAttributes.MediaSize) {
    ISO_13("A4 (13\")", PrintAttributes.MediaSize.ISO_A4),
    SIZE_10("A5 (10\")", PrintAttributes.MediaSize.ISO_A5),
    ISO_67("Hisense A7 (6.7\")", PrintAttributes.MediaSize.PRC_5),
    SIZE_8("C6 (8\")", PrintAttributes.MediaSize.ISO_C6),
}

enum class FabPosition {
    Right, Left, Center, NotShow
}

enum class TranslationMode(val label: String) {
    ONYX("ONYX"),
    GOOGLE("Google Text"),
    PAPAGO("Papago Text"),
    PAPAGO_URL("Papago Full Page"),
    GOOGLE_URL("Google Full Page"),
    PAPAGO_DUAL("Papago Dual Pane"),
    GOOGLE_IN_PLACE("Google in-Place"),
}

enum class FontType(val resId: Int) {
    SYSTEM_DEFAULT(R.string.system_default),
    SERIF(R.string.serif),
    GOOGLE_SERIF(R.string.googleserif),
    CUSTOM(R.string.custom_font)
}

enum class DarkMode {
    SYSTEM, FORCE_ON, DISABLED
}

data class AlbumInfo(
    val title: String,
    val url: String
) {
    fun toSerializedString(): String = "$title::$url"
}

data class CustomFontInfo(
    val name: String,
    val url: String
) {
    fun toSerializedString(): String = "$name::$url"
}

data class RecentBookmark(val name: String, val url: String, var count: Int) {
    fun toSerializedString(): String = "$name::$url::$count"
}

private fun String.toRecentBookmark(): RecentBookmark? {
    val segments = this.split("::", limit = 3)
    if (segments.size != 3) return null
    return RecentBookmark(segments[0], segments[1], segments[2].toInt())
}

private fun String.toAlbumInfo(): AlbumInfo? {
    val segments = this.split("::", limit = 2)
    if (segments.size != 2) return null
    return AlbumInfo(segments[0], segments[1])
}

private fun String.toCustomFontInfo(): CustomFontInfo? {
    val segments = this.split("::", limit = 2)
    if (segments.size != 2) return null
    return CustomFontInfo(segments[0], segments[1])
}

fun KMutableProperty0<Boolean>.toggle(): Boolean = get().not().apply(setter)