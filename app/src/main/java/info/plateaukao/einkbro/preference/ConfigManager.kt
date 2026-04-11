package info.plateaukao.einkbro.preference

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.epub.EpubFileInfo
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.util.TranslationLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConfigManager(
    private val context: Context,
    private val sp: SharedPreferences,
) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    val ai = AiConfig(sp)
    val tts = TtsConfig(sp)
    val translation = TranslationConfig(sp)
    val touch = TouchConfig(sp)
    val display = DisplayConfig(sp)
    val browser = BrowserConfig(sp)
    val tab = TabConfig(sp)
    val ui = UiConfig(context, sp)

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var restartChanged by BooleanPreference(sp, K_RESTART_CHANGED, false)

    private var originalSaveHistoryMode: SaveHistoryMode? = null
    var isIncognitoMode: Boolean
        get() = sp.getBoolean(K_IS_INCOGNITO_MODE, false)
        set(value) {
            browser.cookies = !value
            if (value) {
                originalSaveHistoryMode = tab.saveHistoryMode
                tab.saveHistoryMode = SaveHistoryMode.DISABLED
            } else {
                if (originalSaveHistoryMode != null) {
                    tab.saveHistoryMode = originalSaveHistoryMode!!
                    originalSaveHistoryMode = null
                } else {
                    tab.saveHistoryMode = tab.toggledSaveHistoryMode
                }
            }

            sp.edit { putBoolean(K_IS_INCOGNITO_MODE, value) }
        }

    var uiLocaleLanguage by StringPreference(sp, "sp_ui_locale_language", "")

    var favoriteUrl by StringPreference(sp, K_FAVORITE_URL, Constants.DEFAULT_HOME_URL)

    var version by StringPreference(sp, "sp_version", "11.14.0")

    //use string set in sharedpreference
    var domainConfigurationMap = mutableMapOf<String, DomainConfigurationData>()

    var scrollFixList: List<String>
        get() = sp.getStringSet(K_SCROLL_FIX_LIST, mutableSetOf())?.toList() ?: emptyList()
        set(value) = sp.edit { putStringSet(K_SCROLL_FIX_LIST, value.toSet()) }

    fun shouldFixScroll(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldFixScroll } ?: false

    fun toggleFixScroll(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldFixScroll = !config.shouldFixScroll
        bookmarkManager.addDomainConfiguration(config)

        return shouldFixScroll(url)
    }

    // use string set to store the list of sending page navigation key
    var sendPageNavKeyList: List<String>
        get() = sp.getStringSet(K_SEND_PAGE_NAV_KEY_LIST, mutableSetOf())?.toList() ?: emptyList()
        set(value) = sp.edit { putStringSet(K_SEND_PAGE_NAV_KEY_LIST, value.toSet()) }

    fun shouldSendPageNavKey(url: String): Boolean {
        return Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldSendPageNavKey }
            ?: false
    }

    fun toggleSendPageNavKey(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldSendPageNavKey = !config.shouldSendPageNavKey
        bookmarkManager.addDomainConfiguration(config)

        return shouldSendPageNavKey(url)
    }

    var translateSiteList: List<String>
        get() = sp.getStringSet(K_TRANSLATE_SITE_LIST, mutableSetOf())?.toList() ?: emptyList()
        set(value) = sp.edit { putStringSet(K_TRANSLATE_SITE_LIST, value.toSet()) }

    fun shouldTranslateSite(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldTranslateSite } ?: false

    fun toggleTranslateSite(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldTranslateSite = !config.shouldTranslateSite
        bookmarkManager.addDomainConfiguration(config)

        return shouldTranslateSite(url)
    }

    // use string set to store the url list of having white background
    var whiteBackgroundList: List<String>
        get() = sp.getStringSet(K_WHITE_BACKGROUND_LIST, mutableSetOf())?.toList() ?: emptyList()
        set(value) = sp.edit { putStringSet(K_WHITE_BACKGROUND_LIST, value.toSet()) }

    fun whiteBackground(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldUseWhiteBackground } ?: false

    fun toggleWhiteBackground(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldUseWhiteBackground = !config.shouldUseWhiteBackground
        bookmarkManager.addDomainConfiguration(config)
        return whiteBackground(url)
    }

    fun hasInvertedColor(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldInvertColor } ?: false

    fun toggleInvertedColor(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldInvertColor = !config.shouldInvertColor
        bookmarkManager.addDomainConfiguration(config)
        return hasInvertedColor(url)
    }

    var recentBookmarks: List<RecentBookmark>
        get() {
            val string = sp.getString(K_RECENT_BOOKMARKS, "").orEmpty()
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
            sameItem.count++
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

    var savedEpubFileInfos: List<EpubFileInfo>
        get() = sp.getString(K_SAVED_EPUBS, "")?.toEpubFileInfoList() ?: mutableListOf()
        set(value) = sp.edit { putString(K_SAVED_EPUBS, toEpubFileInfosString(value)) }

    var splitSearchItemInfoList: List<SplitSearchItemInfo>
        get() {
            val str = sp.getString(K_SPLIT_SEARCH_ITEMS, "").orEmpty()
            return if (str.isBlank()) emptyList()
            else str.split("###").mapNotNull {
                decodeFromString(SplitSearchItemInfo.serializer(), it)
            }
        }
        private set(value) {
            sp.edit {
                putString(
                    K_SPLIT_SEARCH_ITEMS,
                    value.joinToString("###") {
                        Json.encodeToString(SplitSearchItemInfo.serializer(), it)
                    }
                )
            }
        }

    fun addSplitSearchItem(item: SplitSearchItemInfo) {
        val list = splitSearchItemInfoList.toMutableList()
        list.add(item)
        splitSearchItemInfoList = list
    }

    fun deleteSplitSearchItem(item: SplitSearchItemInfo) {
        val list = splitSearchItemInfoList.toMutableList()
        list.remove(list.first { it.title == item.title })
        splitSearchItemInfoList = list
    }

    fun deleteAllSplitSearchItems() {
        splitSearchItemInfoList = emptyList()
    }

    var clearCache by BooleanPreference(sp, K_CLEAR_CACHE, false)
    var clearIndexedDB by BooleanPreference(sp, K_CLEAR_INDEXEDDB, false)
    var clearCookies by BooleanPreference(sp, K_CLEAR_COOKIES, false)
    var clearHistory by BooleanPreference(sp, K_CLEAR_HISTORY, false)
    var clearWhenQuit by BooleanPreference(sp, K_CLEAR_WHEN_QUIT, false)

    var instapaperUsername by StringPreference(sp, K_INSTAPAPER_USERNAME, "")
    var instapaperPassword by StringPreference(sp, K_INSTAPAPER_PASSWORD, "")

    companion object {
        const val K_SCROLL_FIX_LIST = "sp_scroll_fix_list"
        const val K_SEND_PAGE_NAV_KEY_LIST = "sp_send_page_nav_key_list"
        const val K_TRANSLATE_SITE_LIST = "sp_translate_site_list"
        const val K_WHITE_BACKGROUND_LIST = "sp_white_background_list"
        const val K_FAVORITE_URL = "favoriteURL"
        const val K_DB_VERSION = "sp_db_version"
        const val K_IS_INCOGNITO_MODE = "sp_incognito"
        const val K_WHITE_BACKGROUND = "sp_whitebackground"
        const val K_RECENT_BOOKMARKS = "sp_recent_bookmarks"
        const val K_RESTART_CHANGED = "restart_changed"

        const val K_CLEAR_CACHE = "SP_CLEAR_CACHE_9"
        const val K_CLEAR_HISTORY = "SP_CLEAR_HISTORY_9"
        const val K_CLEAR_COOKIES = "SP_CLEAR_COOKIE_9"
        const val K_CLEAR_INDEXEDDB = "sp_clear_Indexeddb"
        const val K_CLEAR_WHEN_QUIT = "SP_CLEAR_QUIT_9"

        const val K_SAVED_EPUBS = "sp_saved_epubs"

        const val K_INSTAPAPER_USERNAME = "sp_instapaper_username"
        const val K_INSTAPAPER_PASSWORD = "sp_instapaper_password"

        private const val RECENT_BOOKMARKS_SEPARATOR = "::::"
        private const val EPUB_FILE_INFO_SEPARATOR = "::::"

        private const val RECENT_BOOKMARK_LIST_SIZE = 10

        private const val K_SPLIT_SEARCH_ITEMS = "sp_split_search_items"
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

// Property delegates moved to PreferenceDelegates.kt
// Enums moved to PreferenceEnums.kt
