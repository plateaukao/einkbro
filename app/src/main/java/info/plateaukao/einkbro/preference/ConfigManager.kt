package info.plateaukao.einkbro.preference

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.epub.EpubFileInfo
import info.plateaukao.einkbro.search.SearchEngine
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

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

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var enableWebBkgndLoad by BooleanPreference(sp, K_BKGND_LOAD, true)
    var enableJavascript by BooleanPreference(sp, K_JAVASCRIPT, true)
    var toolbarPosition: ToolbarPosition
        get() {
            val posOrdinal = sp.getInt(K_TOOLBAR_POSITION, -1)
            if (posOrdinal >= 0) return ToolbarPosition.entries.getOrElse(posOrdinal) { ToolbarPosition.Bottom }
            // migrate from old boolean preference
            return if (sp.getBoolean(K_TOOLBAR_TOP, false)) ToolbarPosition.Top else ToolbarPosition.Bottom
        }
        set(value) = sp.edit { putInt(K_TOOLBAR_POSITION, value.ordinal) }

    var isToolbarOnTop: Boolean
        get() = toolbarPosition == ToolbarPosition.Top
        set(value) { toolbarPosition = if (value) ToolbarPosition.Top else ToolbarPosition.Bottom }

    val isVerticalToolbar: Boolean
        get() = toolbarPosition == ToolbarPosition.Left || toolbarPosition == ToolbarPosition.Right
    var enableViBinding by BooleanPreference(sp, K_VI_BINDING, false)
    var shouldSaveTabs by BooleanPreference(sp, K_SHOULD_SAVE_TABS, true)
    var adBlock by BooleanPreference(sp, K_ADBLOCK, true)
    var cookies by BooleanPreference(sp, K_COOKIES, true)
    var shareLocation by BooleanPreference(sp, K_SHARE_LOCATION, false)
    var keepAwake by BooleanPreference(sp, K_KEEP_AWAKE, false)
    var desktop by BooleanPreference(sp, K_DESKTOP, false)
    var continueMedia by BooleanPreference(sp, K_MEDIA_CONTINUE, false)
    var restartChanged by BooleanPreference(sp, K_RESTART_CHANGED, false)
    var autoFillForm by BooleanPreference(sp, K_AUTO_FILL, true)
    var shouldTrimInputUrl by BooleanPreference(sp, K_TRIM_INPUT_URL, false)
    var shouldPruneQueryParameters by BooleanPreference(sp, K_PRUNE_QUERY_PARAMETERS, false)
    var isBookmarkGridView by BooleanPreference(sp, K_BOOKMARK_GRID_VIEW, false)
    var debugWebView by BooleanPreference(sp, K_DEBUG_WEBVIEW, false)
    var shouldShowTabBar by BooleanPreference(sp, K_SHOW_TAB_BAR, false)
    var confirmTabClose by BooleanPreference(sp, K_CONFIRM_TAB_CLOSE, false)
    var shouldHideToolbar by BooleanPreference(sp, K_HIDE_TOOLBAR, false)
    var showToolbarFirst by BooleanPreference(sp, K_SHOW_TOOLBAR_FIRST, true)
    var clearCache by BooleanPreference(sp, K_CLEAR_CACHE, false)
    var clearIndexedDB by BooleanPreference(sp, K_CLEAR_INDEXEDDB, false)
    var clearCookies by BooleanPreference(sp, K_CLEAR_COOKIES, false)
    var clearHistory by BooleanPreference(sp, K_CLEAR_HISTORY, false)
    var clearWhenQuit by BooleanPreference(sp, K_CLEAR_WHEN_QUIT, false)
    var enableRemoteAccess by BooleanPreference(sp, K_ENABLE_REMOTE_ACCESS, true)
    var enableImages by BooleanPreference(sp, K_ENABLE_IMAGES, true)
    var enableVideoAutoFullscreen by BooleanPreference(sp, K_ENABLE_VIDEO_AUTO_FULLSCREEN, false)
    var enableVideoAutoplay by BooleanPreference(sp, K_ENABLE_VIDEO_AUTOPLAY, false)
    var enableVideoPip by BooleanPreference(sp, K_ENABLE_VIDEO_PIP, false)
    var autoUpdateAdblock by BooleanPreference(sp, K_AUTO_UPDATE_ADBLOCK, false)
    var enableCertificateErrorDialog by BooleanPreference(sp, CERTIFICATE_ERROR_DIALOG, true)
    var closeTabWhenNoMoreBackHistory by BooleanPreference(sp, K_CLOSE_TAB_WHEN_BACK, true)
    var enableCustomUserAgent by BooleanPreference(sp, K_ENABLE_CUSTOM_USER_AGENT, false)
    var showBookmarksInInputBar by BooleanPreference(sp, K_SHOW_BOOKMARKS_IN_INPUTBAR, false)

    var showShareSaveMenu by BooleanPreference(sp, K_SHOW_SHARE_SAVE_MENU, false)
    var showContentMenu by BooleanPreference(sp, K_SHOW_CONTENT_MENU, false)

    var showDefaultActionMenu by BooleanPreference(sp, K_SHOW_DEFAULT_ACTION_MENU, false)

    var enableSaveData by BooleanPreference(sp, K_ENABLE_SAVE_DATA, true)

    var blockAnalytics by BooleanPreference(sp, K_BLOCK_ANALYTICS, false)

    var hideStatusbar by BooleanPreference(sp, K_HIDE_STATUSBAR, false)

    var shouldShowNextAfterRemoveTab by BooleanPreference(
        sp,
        K_SHOW_NEXT_AFTER_REMOVE_TAB,
        false
    )

    var showActionMenuIcons by BooleanPreference(sp, K_SHOW_ACTION_MENU_ICONS, true)

    private var originalSaveHistoryMode: SaveHistoryMode? = null
    var isIncognitoMode: Boolean
        get() = sp.getBoolean(K_IS_INCOGNITO_MODE, false)
        set(value) {
            cookies = !value
            if (value) {
                originalSaveHistoryMode = saveHistoryMode
                saveHistoryMode = SaveHistoryMode.DISABLED
            } else {
                if (originalSaveHistoryMode != null) {
                    saveHistoryMode = originalSaveHistoryMode!!
                    originalSaveHistoryMode = null
                } else {
                    saveHistoryMode = toggledSaveHistoryMode
                }
            }

            sp.edit { putBoolean(K_IS_INCOGNITO_MODE, value) }
        }

    var webLoadCacheFirst by BooleanPreference(sp, "sp_web_load_cache_first", false)

    var enableSearchSuggestion by BooleanPreference(sp, "sp_enable_search_suggestion", true)

    var customUserAgent by StringPreference(sp, K_CUSTOM_USER_AGENT)
    val customProcessTextUrl by StringPreference(sp, K_CUSTOM_PROCESS_TEXT_URL)
    var searchEngine by StringPreference(
        sp,
        K_SEARCH_ENGINE,
        if (Locale.getDefault().country == "CN") SearchEngine.BAIDU.ordinal.toString() else SearchEngine.GOOGLE.ordinal.toString()
    )
    var searchEngineUrl by StringPreference(
        sp,
        K_SEARCH_ENGINE_URL,
        "https://www.google.com/search?q=%s"
    )
    var processTextUrl by StringPreference(sp, K_CUSTOM_PROCESS_TEXT_URL, "")
    var adblockHostUrl by StringPreference(
        sp,
        K_ADBLOCK_HOSTS_URL,
        ADBLOCK_URL_DEFAULT
    )

    var fabPosition: FabPosition
        get() = FabPosition.entries[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {
            sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) }
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

    val shouldUseLargeToolbarConfig: Boolean
        get() = if (isVerticalToolbar) !ViewUnit.isLandscape(context) else ViewUnit.isLandscape(context)

    var toolbarActions: List<ToolbarAction>
        get() {
            val key =
                if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
            val iconListString =
                sp.getString(key, sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings())).orEmpty()
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key =
                    if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

    var readerToolbarActions: List<ToolbarAction>
        get() {
            val iconListString =
                sp.getString(K_READER_TOOLBAR_ICONS, "").orEmpty()
            if (iconListString.isBlank()) return ToolbarAction.defaultReaderActions
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                putString(K_READER_TOOLBAR_ICONS, value.map { it.ordinal }.joinToString(","))
            }
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

    var savedAlbumInfoList: List<AlbumInfo>
        get() {
            val string = sp.getString(K_SAVED_ALBUM_INFO, "").orEmpty()
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

    var purgeHistoryTimestamp: Long
        get() = sp.getLong(K_HISTORY_PURGE_TS, 0L)
        set(value) = sp.edit { putLong(K_HISTORY_PURGE_TS, value) }

    var currentAlbumIndex: Int
        get() = sp.getInt(K_SAVED_ALBUM_INDEX, 0)
        set(value) {
            if (currentAlbumIndex == value) return

            sp.edit { putInt(K_SAVED_ALBUM_INDEX, value) }
        }

    var adSites: MutableSet<String>
        get() = sp.getStringSet(K_ADBLOCK_SITES, mutableSetOf()) ?: mutableSetOf()
        set(value) = sp.edit { putStringSet(K_ADBLOCK_SITES, value) }

    var savedEpubFileInfos: List<EpubFileInfo>
        get() = sp.getString(K_SAVED_EPUBS, "")?.toEpubFileInfoList() ?: mutableListOf()
        set(value) = sp.edit { putString(K_SAVED_EPUBS, toEpubFileInfosString(value)) }

    var newTabBehavior: NewTabBehavior
        get() = NewTabBehavior.entries[sp.getString(K_NEW_TAB_BEHAVIOR, "0")?.toInt() ?: 0]
        set(value) = sp.edit { putString(K_NEW_TAB_BEHAVIOR, value.ordinal.toString()) }

    var fabCustomPosition: Point
        get() {
            val str = sp.getString(K_FAB_POSITION, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION, "${value.x},${value.y}") }
        }

    var fabCustomPositionLandscape: Point
        get() {
            val str = sp.getString(K_FAB_POSITION_LAND, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION_LAND, "${value.x},${value.y}") }
        }

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

    // save history logic codes
    var saveHistoryMode: SaveHistoryMode
        get() {
            val str = sp.getString(K_SAVE_HISTORY_MODE, "")
            return if (str.isNullOrEmpty()) {
                // For backward compatibility.
                if (sp.getBoolean(K_SAVE_HISTORY, true)) {
                    SaveHistoryMode.SAVE_WHEN_OPEN
                } else {
                    SaveHistoryMode.DISABLED
                }
            } else {
                SaveHistoryMode.entries[str.toInt()]
            }
        }
        set(value) {
            sp.edit {
                putString(K_SAVE_HISTORY_MODE, value.ordinal.toString())
            }
        }

    fun isSaveHistoryWhenLoad() = saveHistoryMode == SaveHistoryMode.SAVE_WHEN_OPEN
    fun isSaveHistoryWhenClose() = saveHistoryMode == SaveHistoryMode.SAVE_WHEN_CLOSE
    fun isSaveHistoryOn() = saveHistoryMode != SaveHistoryMode.DISABLED

    // For tracking state in fast toggling only
    var toggledSaveHistoryMode: SaveHistoryMode = SaveHistoryMode.SAVE_WHEN_OPEN

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

    var instapaperUsername by StringPreference(sp, K_INSTAPAPER_USERNAME, "")
    var instapaperPassword by StringPreference(sp, K_INSTAPAPER_PASSWORD, "")

    private fun iconStringToEnumList(iconListString: String): List<ToolbarAction> {
        if (iconListString.isBlank()) return listOf()

        return iconListString.split(",").map { ToolbarAction.fromOrdinal(it.toInt()) }
    }

    @Suppress("DEPRECATION")
    private fun getDefaultTranslationLanguage(): TranslationLanguage {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
        return TranslationLanguage.findByLanguage(locale.language)
    }

    private fun getDefaultIconStrings(): String =
        if (ViewUnit.isWideLayout(context)) {
            ToolbarAction.defaultActions.joinToString(",") { action ->
                action.ordinal.toString()
            }
        } else {
            ToolbarAction.defaultActionsForPhone.joinToString(",") { action ->
                action.ordinal.toString()
            }
        }

    companion object {
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_TOOLBAR_ICONS_FOR_LARGE = "sp_toolbar_icons_for_large"
        const val K_READER_TOOLBAR_ICONS = "sp_reader_toolbar_icons"
        const val K_SCROLL_FIX_LIST = "sp_scroll_fix_list"
        const val K_SEND_PAGE_NAV_KEY_LIST = "sp_send_page_nav_key_list"
        const val K_TRANSLATE_SITE_LIST = "sp_translate_site_list"
        const val K_WHITE_BACKGROUND_LIST = "sp_white_background_list"
        const val K_NAV_POSITION = "nav_position"
        const val K_FAVORITE_URL = "favoriteURL"
        const val K_SHOULD_SAVE_TABS = "sp_shouldSaveTabs"
        const val K_SAVED_ALBUM_INFO = "sp_saved_album_info"
        const val K_SAVED_ALBUM_INDEX = "sp_saved_album_index"
        const val K_DB_VERSION = "sp_db_version"
        const val K_IS_INCOGNITO_MODE = "sp_incognito"
        const val K_ADBLOCK = "SP_AD_BLOCK_9"
        const val K_SAVE_HISTORY = "saveHistory"
        const val K_SAVE_HISTORY_MODE = "saveHistoryMode"
        const val K_COOKIES = "SP_COOKIES_9"
        const val K_KEEP_AWAKE = "sp_screen_awake"
        const val K_DESKTOP = "sp_desktop"
        const val K_ADBLOCK_SITES = "sp_adblock_sites"
        const val K_CUSTOM_USER_AGENT = "userAgent"
        const val K_WHITE_BACKGROUND = "sp_whitebackground"
        const val K_CUSTOM_PROCESS_TEXT_URL = "sp_process_text_custom"
        const val K_TOOLBAR_TOP = "sp_toolbar_top"
        const val K_TOOLBAR_POSITION = "sp_toolbar_position"
        const val K_VI_BINDING = "sp_enable_vi_binding"
        const val K_MEDIA_CONTINUE = "sp_media_continue"
        const val K_RECENT_BOOKMARKS = "sp_recent_bookmarks"
        const val K_RESTART_CHANGED = "restart_changed"
        const val K_JAVASCRIPT = "SP_JAVASCRIPT_9"
        const val K_BKGND_LOAD = "sp_background_loading"
        const val K_SHARE_LOCATION = "SP_LOCATION_9"
        const val K_AUTO_FILL = "sp_auto_fill"
        const val K_TRIM_INPUT_URL = "sp_trim_input_url"
        const val K_DEBUG_WEBVIEW = "sp_debug_webview"
        const val K_HISTORY_PURGE_TS = "sp_history_purge_ts"
        const val K_PRUNE_QUERY_PARAMETERS = "sp_prune_query_parameter"
        const val K_SHOW_TAB_BAR = "sp_show_tab_bar"
        const val K_NEW_TAB_BEHAVIOR = "sp_plus_behavior"
        const val K_FAB_POSITION = "sp_fab_position"
        const val K_FAB_POSITION_LAND = "sp_fab_position_land"
        const val K_CONFIRM_TAB_CLOSE = "sp_close_tab_confirm"
        const val K_HIDE_TOOLBAR = "hideToolbar"
        const val K_SHOW_TOOLBAR_FIRST = "sp_toolbarShow"
        const val K_SEARCH_ENGINE = "SP_SEARCH_ENGINE_9"
        const val K_SEARCH_ENGINE_URL = "sp_search_engine_custom"
        const val K_ENABLE_REMOTE_ACCESS = "sp_remote"
        const val K_ENABLE_IMAGES = "SP_IMAGES_9"
        const val K_ENABLE_VIDEO_AUTO_FULLSCREEN = "sp_video_auto_fullscreen"
        const val K_ENABLE_VIDEO_AUTOPLAY = "sp_video_autoplay"
        const val K_ENABLE_VIDEO_PIP = "sp_video_auto_pip"
        const val K_ADBLOCK_HOSTS_URL = "ab_hosts"
        const val K_AUTO_UPDATE_ADBLOCK = "sp_auto_update_adblock"
        const val CERTIFICATE_ERROR_DIALOG = "sp_certificate_error_dialog"
        const val K_CLOSE_TAB_WHEN_BACK = "sp_close_tab_when_no_more_back_history"
        const val K_ENABLE_CUSTOM_USER_AGENT = "sp_custom_user_agent"
        const val K_SHOW_SHARE_SAVE_MENU = "sp_show_share_save_menu"
        const val K_SHOW_CONTENT_MENU = "sp_show_content_menu"
        const val K_SHOW_BOOKMARKS_IN_INPUTBAR = "sp_show_bookmarks_in_inputbar"

        const val K_CLEAR_CACHE = "SP_CLEAR_CACHE_9"
        const val K_CLEAR_HISTORY = "SP_CLEAR_HISTORY_9"
        const val K_CLEAR_COOKIES = "SP_CLEAR_COOKIE_9"
        const val K_CLEAR_INDEXEDDB = "sp_clear_Indexeddb"
        const val K_CLEAR_WHEN_QUIT = "SP_CLEAR_QUIT_9"

        const val K_SAVED_EPUBS = "sp_saved_epubs"

        const val K_SHOW_DEFAULT_ACTION_MENU = "sp_show_default_action_menu"

        const val K_ENABLE_SAVE_DATA = "sp_enable_save_data"
        const val K_BLOCK_ANALYTICS = "sp_block_analytics"
        const val K_HIDE_STATUSBAR = "sp_hide_statusbar"

        const val K_SHOW_NEXT_AFTER_REMOVE_TAB = "sp_show_previous_after_remove_tab"

        const val K_SHOW_ACTION_MENU_ICONS = "sp_show_action_menu_icons"

        const val K_BOOKMARK_GRID_VIEW = "sp_bookmark_grid_view"

        const val ADBLOCK_URL_DEFAULT =
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

        private const val ALBUM_INFO_SEPARATOR = "::::"
        private const val RECENT_BOOKMARKS_SEPARATOR = "::::"
        private const val EPUB_FILE_INFO_SEPARATOR = "::::"

        private const val RECENT_BOOKMARK_LIST_SIZE = 10

        private const val K_SPLIT_SEARCH_ITEMS = "sp_split_search_items"

        const val K_INSTAPAPER_USERNAME = "sp_instapaper_username"
        const val K_INSTAPAPER_PASSWORD = "sp_instapaper_password"
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
