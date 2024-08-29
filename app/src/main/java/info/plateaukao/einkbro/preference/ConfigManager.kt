package info.plateaukao.einkbro.preference

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.epub.EpubFileInfo
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.GestureType
import info.plateaukao.einkbro.view.Orientation
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

class ConfigManager(
    private val context: Context,
    private val sp: SharedPreferences,
) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var enableWebBkgndLoad by BooleanPreference(sp, K_BKGND_LOAD, true)
    var enableJavascript by BooleanPreference(sp, K_JAVASCRIPT, true)
    var isToolbarOnTop by BooleanPreference(sp, K_TOOLBAR_TOP, false)
    var enableViBinding by BooleanPreference(sp, K_VI_BINDING, false)
    var isMultitouchEnabled by BooleanPreference(sp, K_MULTITOUCH, false)
    var useUpDownPageTurn by BooleanPreference(sp, K_UPDOWN_PAGE_TURN, false)
    var touchAreaHint by BooleanPreference(sp, K_TOUCH_HINT, true)
    var volumePageTurn by BooleanPreference(sp, K_VOLUME_PAGE_TURN, true)
    var boldFontStyle by BooleanPreference(sp, K_BOLD_FONT, false)
    var blackFontStyle by BooleanPreference(sp, K_BLACK_FONT, false)
    var shouldSaveTabs by BooleanPreference(sp, K_SHOULD_SAVE_TABS, true)
    var adBlock by BooleanPreference(sp, K_ADBLOCK, true)
    var cookies by BooleanPreference(sp, K_COOKIES, true)
    var shareLocation by BooleanPreference(sp, K_SHARE_LOCATION, false)
    var enableTouchTurn by BooleanPreference(sp, K_ENABLE_TOUCH, false)
    var keepAwake by BooleanPreference(sp, K_KEEP_AWAKE, false)
    var desktop by BooleanPreference(sp, K_DESKTOP, false)
    var continueMedia by BooleanPreference(sp, K_MEDIA_CONTINUE, false)
    var restartChanged by BooleanPreference(sp, K_RESTART_CHANGED, false)
    var autoFillForm by BooleanPreference(sp, K_AUTO_FILL, true)
    var shouldTrimInputUrl by BooleanPreference(sp, K_TRIM_INPUT_URL, false)
    var enableZoom by BooleanPreference(sp, K_ENABLE_ZOOM, true)
    var shouldPruneQueryParameters by BooleanPreference(sp, K_PRUNE_QUERY_PARAMETERS, false)
    var translationPanelSwitched by BooleanPreference(sp, K_TRANSLATE_PANEL_SWITCHED, false)
    var translationScrollSync by BooleanPreference(sp, K_TRANSLATE_SCROLL_SYNC, false)
    var twoPanelLinkHere by BooleanPreference(sp, K_TWO_PANE_LINK_HERE, false)
    var switchTouchAreaAction by BooleanPreference(sp, K_TOUCH_AREA_ACTION_SWITCH, false)
    var longClickAsArrowKey by BooleanPreference(sp, K_TOUCH_AREA_ARROW_KEY, false)
    var hideTouchAreaWhenInput by BooleanPreference(sp, K_TOUCH_AREA_HIDE_WHEN_INPUT, false)
    var customFontChanged by BooleanPreference(sp, K_CUSTOM_FONT_CHANGED, false)
    var debugWebView by BooleanPreference(sp, K_DEBUG_WEBVIEW, false)
    var shouldShowTabBar by BooleanPreference(sp, K_SHOW_TAB_BAR, false)
    var confirmTabClose by BooleanPreference(sp, K_CONFIRM_TAB_CLOSE, false)
    var shouldHideToolbar by BooleanPreference(sp, K_HIDE_TOOLBAR, false)
    var showToolbarFirst by BooleanPreference(sp, K_SHOW_TOOLBAR_FIRST, true)
    var enableNavButtonGesture by BooleanPreference(sp, K_NAV_BUTTON_GESTURE, false)
    var clearCache by BooleanPreference(sp, K_CLEAR_CACHE, false)
    var clearIndexedDB by BooleanPreference(sp, K_CLEAR_INDEXEDDB, false)
    var clearCookies by BooleanPreference(sp, K_CLEAR_COOKIES, false)
    var clearHistory by BooleanPreference(sp, K_CLEAR_HISTORY, false)
    var clearWhenQuit by BooleanPreference(sp, K_CLEAR_WHEN_QUIT, false)
    var enableRemoteAccess by BooleanPreference(sp, K_ENABLE_REMOTE_ACCESS, true)
    var enableImages by BooleanPreference(sp, K_ENABLE_IMAGES, true)
    var enableVideoAutoFullscreen by BooleanPreference(sp, K_ENABLE_VIDEO_AUTO_FULLSCREEN, false)
    var enableVideoPip by BooleanPreference(sp, K_ENABLE_VIDEO_PIP, false)
    var autoUpdateAdblock by BooleanPreference(sp, K_AUTO_UPDATE_ADBLOCK, false)
    var enableCertificateErrorDialog by BooleanPreference(sp, CERTIFICATE_ERROR_DIALOG, true)
    var closeTabWhenNoMoreBackHistory by BooleanPreference(sp, K_CLOSE_TAB_WHEN_BACK, true)
    var enableCustomUserAgent by BooleanPreference(sp, K_ENABLE_CUSTOM_USER_AGENT, false)
    var showBookmarksInInputBar by BooleanPreference(sp, K_SHOW_BOOKMARKS_IN_INPUTBAR, false)

    var showShareSaveMenu by BooleanPreference(sp, K_SHOW_SHARE_SAVE_MENU, false)
    var showContentMenu by BooleanPreference(sp, K_SHOW_CONTENT_MENU, false)

    var showDefaultActionMenu by BooleanPreference(sp, K_SHOW_DEFAULT_ACTION_MENU, false)

    var showTranslatedImageToSecondPanel by BooleanPreference(
        sp,
        K_SHOW_TRANSLATED_IMAGE_TO_SECOND_PANEL,
        true
    )

    var externalSearchWithGpt by BooleanPreference(sp, K_EXTERNAL_SEARCH_WITH_GPT, false)

    var externalSearchWithPopUp by BooleanPreference(sp, K_EXTERNAL_SEARCH_WITH_POPUP, false)

    var enableSaveData by BooleanPreference(sp, K_ENABLE_SAVE_DATA, true)

    var hideStatusbar by BooleanPreference(sp, K_HIDE_STATUSBAR, false)

    var enableOpenAiStream by BooleanPreference(sp, K_ENABLE_OPEN_AI_STREAM, true)

    var isExternalSearchInSameTab by BooleanPreference(sp, K_EXTERNAL_SEARCH_IN_SAME_TAB, false)

    var shouldShowNextAfterRemoveTab by BooleanPreference(
        sp,
        K_SHOW_NEXT_AFTER_REMOVE_TAB,
        false
    )

    var showActionMenuIcons by BooleanPreference(sp, K_SHOW_ACTION_MENU_ICONS, true)
    var enableInplaceParagraphTranslate by
    BooleanPreference(sp, K_ENABLE_IN_PLACE_PARAGRAPH_TRANSLATE, true)

    var isIncognitoMode: Boolean
        get() = sp.getBoolean(K_IS_INCOGNITO_MODE, false)
        set(value) {
            if (!value) {
                cookies = false
                saveHistoryMode = SaveHistoryMode.DISABLED
            }
            sp.edit { putBoolean(K_IS_INCOGNITO_MODE, value) }
        }

    var useOpenAiTts by BooleanPreference(sp, K_USE_OPENAI_TTS, true)

    var pageReservedOffset: Int by IntPreference(sp, K_PRESERVE_HEIGHT, 80)

    var pageReservedOffsetInString: String by StringPreference(
        sp,
        K_PRESERVE_HEIGHT_IN_STRING,
        pageReservedOffset.toString()
    )

    private val K_TTS_LOCALE = "sp_tts_locale"
    var ttsLocale: Locale
        get() = Locale(
            sp.getString(K_TTS_LOCALE, Locale.getDefault().language) ?: Locale.getDefault().language
        )
        set(value) {
            sp.edit { putString(K_TTS_LOCALE, value.language) }
        }

    var fontSize: Int
        get() = sp.getString(K_FONT_SIZE, "100")?.toInt() ?: 100
        set(value) {
            sp.edit { putString(K_FONT_SIZE, value.toString()) }
        }
    var readerFontSize: Int
        get() = sp.getString(K_READER_FONT_SIZE, fontSize.toString())?.toInt() ?: fontSize
        set(value) {
            sp.edit { putString(K_READER_FONT_SIZE, value.toString()) }
        }

    var ttsSpeedValue: Int
        get() = sp.getString(K_TTS_SPEED_VALUE, "100")?.toInt() ?: 100
        set(value) {
            sp.edit { putString(K_TTS_SPEED_VALUE, value.toString()) }
        }

    var touchAreaCustomizeY by IntPreference(sp, K_TOUCH_AREA_OFFSET, 0)

    var fontBoldness by IntPreference(sp, K_FONT_BOLDNESS, 700)

    var customUserAgent by StringPreference(sp, K_CUSTOM_USER_AGENT)
    val customProcessTextUrl by StringPreference(sp, K_CUSTOM_PROCESS_TEXT_URL)
    var preferredTranslateLanguageString by StringPreference(sp, K_TRANSLATED_LANGS)
    var searchEngine by StringPreference(
        sp,
        K_SEARCH_ENGINE,
        if (Locale.getDefault().country == "CN") "2" else "5"
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
    var bookmarkSyncUrl by StringPreference(sp, K_BOOKMARK_SYNC_URL, "")
    var pocketAccessToken by StringPreference(sp, K_POCKET_ACCESS_TOKEN, "")

    var gptApiKey by StringPreference(sp, K_GPT_API_KEY, "")

    var geminiApiKey by StringPreference(sp, K_GEMINI_API_KEY, "")

    var gptSystemPrompt by StringPreference(
        sp,
        K_GPT_SYSTEM_PROMPT,
        "You are a good interpreter."
    )
    var gptUserPromptPrefix by StringPreference(
        sp,
        K_GPT_USER_PROMPT_PREFIX,
        "Translate following content to English:"
    )
    var gptUserPromptForWebPage by StringPreference(
        sp,
        K_GPT_USER_PROMPT_WEB_PAGE,
        "Summarize in 300 words:"
    )
    var papagoApiSecret by StringPreference(sp, K_PAPAGO_API_SECRET, "")
    var imageApiKey by StringPreference(sp, K_IMAGE_API_KEY, "")
    var gptModel by StringPreference(sp, K_GPT_MODEL, "gpt-3.5-turbo")
    var alternativeModel by StringPreference(sp, K_ALTERNATIVE_MODEL, gptModel)
    var geminiModel by StringPreference(sp, K_GEMINI_MODEL, "gemini-1.5-flash")

    var gptUrl by StringPreference(sp, K_GPT_SERVER_URL, "https://api.openai.com")
    var useCustomGptUrl by BooleanPreference(sp, K_USE_CUSTOM_GPT_URL, false)
    var useGeminiApi by BooleanPreference(sp, K_USE_GEMINI_API, false)

    var dualCaptionLocale by StringPreference(sp, K_DUAL_CAPTION_LOCALE, "")

    var multitouchUp by GestureTypePreference(sp, K_MULTITOUCH_UP)
    var multitouchDown by GestureTypePreference(sp, K_MULTITOUCH_DOWN)
    var multitouchLeft by GestureTypePreference(sp, K_MULTITOUCH_LEFT)
    var multitouchRight by GestureTypePreference(sp, K_MULTITOUCH_RIGHT)
    var navGestureUp by GestureTypePreference(sp, K_GESTURE_NAV_UP)
    var navGestureDown by GestureTypePreference(sp, K_GESTURE_NAV_DOWN)
    var navGestureLeft by GestureTypePreference(sp, K_GESTURE_NAV_LEFT)
    var navGestureRight by GestureTypePreference(sp, K_GESTURE_NAV_RIGHT)

    private val K_EXTERNAL_SEARCH_METHOD = "sp_external_search_method"
    var externalSearchMethod: TRANSLATE_API
        get() = TRANSLATE_API.entries[sp.getInt(K_EXTERNAL_SEARCH_METHOD, 0)]
        set(value) {
            sp.edit { putInt(K_EXTERNAL_SEARCH_METHOD, value.ordinal) }
        }
    var fabPosition: FabPosition
        get() = FabPosition.entries[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {
            sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) }
        }

    var touchAreaType: TouchAreaType
        get() = TouchAreaType.entries[sp.getInt(K_TOUCH_AREA_TYPE, 0)]
        set(value) {
            sp.edit(true) { putInt(K_TOUCH_AREA_TYPE, value.ordinal) }
        }

    var pdfPaperSize: PaperSize
        get() = PaperSize.entries[sp.getInt("pdf_paper_size", PaperSize.ISO_13.ordinal)]
        set(value) {
            sp.edit { putInt("pdf_paper_size", value.ordinal) }
        }

    var localeLanguage: TranslationLanguage
        get() = TranslationLanguage.entries[sp.getInt(
            "sp_locale_language",
            getDefaultTranslationLanguage().ordinal
        )]
        set(value) {
            sp.edit { putInt("sp_locale_language", value.ordinal) }
        }

    var translationLanguage: TranslationLanguage
        get() = TranslationLanguage.entries[sp.getInt(
            K_TRANSLATE_LANGUAGE,
            getDefaultTranslationLanguage().ordinal
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

        return shouldFixScroll(url)
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

    var toolbarActions: List<ToolbarAction>
        get() {
            val key =
                if (ViewUnit.isLandscape(context)) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
            val iconListString =
                sp.getString(key, sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings())).orEmpty()
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key =
                    if (ViewUnit.isLandscape(context)) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

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

    var fontType: FontType
        get() = FontType.entries[sp.getInt(K_FONT_TYPE, 0)]
        set(value) = sp.edit { putInt(K_FONT_TYPE, value.ordinal) }
    var readerFontType: FontType
        get() = FontType.entries[sp.getInt(K_READER_FONT_TYPE, fontType.ordinal)]
        set(value) = sp.edit { putInt(K_READER_FONT_TYPE, value.ordinal) }

    var translationMode: TranslationMode
        get() = TranslationMode.entries[sp.getInt(K_TRANSLATION_MODE, 6)]
        set(value) = sp.edit { putInt(K_TRANSLATION_MODE, value.ordinal) }

    var highlightStyle: HighlightStyle
        get() = HighlightStyle.entries[sp.getInt(K_HIGHLIGHT_STYLE, 0)]
        set(value) = sp.edit { putInt(K_HIGHLIGHT_STYLE, value.ordinal) }

    var adSites: MutableSet<String>
        get() = sp.getStringSet(K_ADBLOCK_SITES, mutableSetOf()) ?: mutableSetOf()
        set(value) = sp.edit { putStringSet(K_ADBLOCK_SITES, value) }

    var savedEpubFileInfos: List<EpubFileInfo>
        get() = sp.getString(K_SAVED_EPUBS, "")?.toEpubFileInfoList() ?: mutableListOf()
        set(value) = sp.edit { putString(K_SAVED_EPUBS, toEpubFileInfosString(value)) }

    var darkMode: DarkMode
        get() = DarkMode.entries[sp.getString(K_DARK_MODE, "2")?.toInt() ?: 2]
        set(value) = sp.edit { putString(K_DARK_MODE, value.ordinal.toString()) }

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
                putString(K_SPLIT_SEARCH_ITEMS,
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

    var gptActionList: List<ChatGPTActionInfo>
        get() {
            val str = sp.getString(K_GPT_ACTION_ITEMS, "").orEmpty()
            return if (str.isBlank()) {
                if (gptSystemPrompt.isNotBlank() || gptUserPromptPrefix.isNotBlank()) {
                    listOf(
                        ChatGPTActionInfo(
                            systemMessage = gptSystemPrompt,
                            userMessage = gptUserPromptPrefix
                        )
                    )
                } else {
                    emptyList()
                }
            } else str.convertToDataClass<List<ChatGPTActionInfo>>()
        }
        set(value) {
            sp.edit {
                putString(
                    K_GPT_ACTION_ITEMS,
                    Json.encodeToString(value)
                )
            }
        }

    fun getDefaultActionModel(): String = if (useGeminiApi) {
        geminiModel
    } else if (useCustomGptUrl) {
        alternativeModel
    } else {
        gptModel
    }

    fun getDefaultActionType(): GptActionType = if (useGeminiApi) {
        GptActionType.Gemini
    } else if (useCustomGptUrl) {
        GptActionType.SelfHosted
    } else {
        GptActionType.OpenAi
    }

    fun getGptTypeModelMap(): Map<GptActionType, String> = mapOf(
        GptActionType.Default to getDefaultActionModel(),
        GptActionType.OpenAi to gptModel,
        GptActionType.SelfHosted to alternativeModel,
        GptActionType.Gemini to geminiModel
    )


    var gptActionForExternalSearch: ChatGPTActionInfo?
        get() {
            val str = sp.getString(K_GPT_ACTION_EXTERNAL, "").orEmpty()
            return if (str.isBlank()) null
            else str.convertToDataClass<ChatGPTActionInfo>()
        }
        set(value) {
            sp.edit {
                putString(
                    K_GPT_ACTION_EXTERNAL,
                    Json.encodeToString(value)
                )
            }
        }

    fun addGptAction(action: ChatGPTActionInfo) {
        gptActionList = gptActionList.toMutableList().apply { add(action) }
    }

    fun deleteGptAction(action: ChatGPTActionInfo) {
        gptActionList = gptActionList.toMutableList().apply { remove(action) }
    }

    fun deleteAllGptActions() {
        gptActionList = emptyList()
    }


    private inline fun <reified R : Any> String.convertToDataClass() =
        Json {
            ignoreUnknownKeys = true
        }.decodeFromString<R>(this)

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
        ToolbarAction.defaultActions.joinToString(",") { action ->
            action.ordinal.toString()
        }

    companion object {
        const val K_TOUCH_AREA_TYPE = "sp_touch_area_type"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_TOOLBAR_ICONS_FOR_LARGE = "sp_toolbar_icons_for_large"
        const val K_SCROLL_FIX_LIST = "sp_scroll_fix_list"
        const val K_SEND_PAGE_NAV_KEY_LIST = "sp_send_page_nav_key_list"
        const val K_TRANSLATE_SITE_LIST = "sp_translate_site_list"
        const val K_WHITE_BACKGROUND_LIST = "sp_white_background_list"
        const val K_BOLD_FONT = "sp_bold_font"
        const val K_BLACK_FONT = "sp_black_font"
        const val K_NAV_POSITION = "nav_position"
        const val K_FONT_SIZE = "sp_fontSize"
        const val K_READER_FONT_SIZE = "sp_reader_fontSize"
        const val K_TTS_SPEED_VALUE = "sp_tts_speed"
        const val K_FAVORITE_URL = "favoriteURL"
        const val K_VOLUME_PAGE_TURN = "volume_page_turn"
        const val K_SHOULD_SAVE_TABS = "sp_shouldSaveTabs"
        const val K_SAVED_ALBUM_INFO = "sp_saved_album_info"
        const val K_SAVED_ALBUM_INDEX = "sp_saved_album_index"
        const val K_DB_VERSION = "sp_db_version"
        const val K_IS_INCOGNITO_MODE = "sp_incognito"
        const val K_ADBLOCK = "SP_AD_BLOCK_9"
        const val K_SAVE_HISTORY = "saveHistory"
        const val K_SAVE_HISTORY_MODE = "saveHistoryMode"
        const val K_COOKIES = "SP_COOKIES_9"
        const val K_TRANSLATION_MODE = "sp_translation_mode"
        const val K_ENABLE_TOUCH = "sp_enable_touch"
        const val K_TOUCH_HINT = "sp_touch_area_hint"
        const val K_KEEP_AWAKE = "sp_screen_awake"
        const val K_DESKTOP = "sp_desktop"
        const val K_TRANSLATE_LANGUAGE = "sp_translate_language"
        const val K_SOURCE_LANGUAGE = "sp_source_language"
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
        const val K_FONT_BOLDNESS = "sp_font_boldness"
        const val K_TOUCH_AREA_ACTION_SWITCH = "sp_touch_area_action_switch"
        const val K_TOUCH_AREA_ARROW_KEY = "sp_touch_area_arrow_key"
        const val K_TOUCH_AREA_HIDE_WHEN_INPUT = "sp_touch_area_hide_when_input"
        const val K_SAVED_EPUBS = "sp_saved_epubs"
        const val K_MULTITOUCH = "sp_multitouch"
        const val K_CUSTOM_FONT = "sp_custom_font"
        const val K_READER_CUSTOM_FONT = "sp_reader_custom_font"
        const val K_CUSTOM_FONT_CHANGED = "sp_custom_font_changed"
        const val K_FONT_TYPE = "sp_font_type"
        const val K_READER_FONT_TYPE = "sp_reader_font_type"
        const val K_TOOLBAR_TOP = "sp_toolbar_top"
        const val K_VI_BINDING = "sp_enable_vi_binding"
        const val K_MEDIA_CONTINUE = "sp_media_continue"
        const val K_RECENT_BOOKMARKS = "sp_recent_bookmarks"
        const val K_RESTART_CHANGED = "restart_changed"
        const val K_JAVASCRIPT = "SP_JAVASCRIPT_9"
        const val K_BKGND_LOAD = "sp_background_loading"
        const val K_SHARE_LOCATION = "SP_LOCATION_9"
        const val K_AUTO_FILL = "sp_auto_fill"
        const val K_TRIM_INPUT_URL = "sp_trim_input_url"
        const val K_ENABLE_ZOOM = "sp_enable_zoom"
        const val K_DEBUG_WEBVIEW = "sp_debug_webview"
        const val K_HISTORY_PURGE_TS = "sp_history_purge_ts"
        const val K_PRUNE_QUERY_PARAMETERS = "sp_prune_query_parameter"
        const val K_SHOW_TAB_BAR = "sp_show_tab_bar"
        const val K_NEW_TAB_BEHAVIOR = "sp_plus_behavior"
        const val K_TRANSLATED_LANGS = "sp_translated_langs"
        const val K_FAB_POSITION = "sp_fab_position"
        const val K_FAB_POSITION_LAND = "sp_fab_position_land"
        const val K_CONFIRM_TAB_CLOSE = "sp_close_tab_confirm"
        const val K_HIDE_TOOLBAR = "hideToolbar"
        const val K_SHOW_TOOLBAR_FIRST = "sp_toolbarShow"
        const val K_NAV_BUTTON_GESTURE = "sp_gestures_use"
        const val K_SEARCH_ENGINE = "SP_SEARCH_ENGINE_9"
        const val K_SEARCH_ENGINE_URL = "sp_search_engine_custom"
        const val K_ENABLE_REMOTE_ACCESS = "sp_remote"
        const val K_ENABLE_IMAGES = "SP_IMAGES_9"
        const val K_ENABLE_VIDEO_AUTO_FULLSCREEN = "sp_video_auto_fullscreen"
        const val K_ENABLE_VIDEO_PIP = "sp_video_auto_pip"
        const val K_ADBLOCK_HOSTS_URL = "ab_hosts"
        const val K_AUTO_UPDATE_ADBLOCK = "sp_auto_update_adblock"
        const val CERTIFICATE_ERROR_DIALOG = "sp_certificate_error_dialog"
        const val K_ENABLE_IMAGE_ADJUSTMENT = "sp_image_adjustment"
        const val K_BOOKMARK_SYNC_URL = "sp_bookmark_sync_url"
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

        const val K_MULTITOUCH_UP = "sp_multitouch_up"
        const val K_MULTITOUCH_DOWN = "sp_multitouch_down"
        const val K_MULTITOUCH_LEFT = "sp_multitouch_left"
        const val K_MULTITOUCH_RIGHT = "sp_multitouch_right"

        const val K_GESTURE_NAV_UP = "setting_gesture_nav_up"
        const val K_GESTURE_NAV_DOWN = "setting_gesture_nav_down"
        const val K_GESTURE_NAV_LEFT = "setting_gesture_nav_left"
        const val K_GESTURE_NAV_RIGHT = "setting_gesture_nav_right"

        const val K_POCKET_ACCESS_TOKEN = "sp_pocket_access_token"
        const val K_GPT_API_KEY = "sp_gpt_api_key"
        const val K_GEMINI_API_KEY = "sp_gemini_api_key"
        const val K_GPT_SYSTEM_PROMPT = "sp_gpt_system_prompt"
        const val K_GPT_USER_PROMPT_PREFIX = "sp_gpt_user_prompt"
        const val K_GPT_USER_PROMPT_WEB_PAGE = "sp_gpt_user_prompt_web_page"
        const val K_PAPAGO_API_SECRET = "sp_papago_api_secret"
        const val K_IMAGE_API_KEY = "sp_image_api_key"
        const val K_DUAL_CAPTION_LOCALE = "sp_dual_caption_locale"
        const val K_GPT_MODEL = "sp_gp_model"
        const val K_ALTERNATIVE_MODEL = "sp_alternative_model"
        const val K_GEMINI_MODEL = "sp_gemini_model"
        const val K_SPLIT_SEARCH_STRING = "sp_split_search_prefix"
        const val K_USE_OPENAI_TTS = "sp_use_openai_tts"
        const val K_HIGHLIGHT_STYLE = "sp_highlight_style"

        const val K_SHOW_DEFAULT_ACTION_MENU = "sp_show_default_action_menu"

        const val K_EXTERNAL_SEARCH_WITH_GPT = "sp_external_search_with_gpt"
        const val K_EXTERNAL_SEARCH_WITH_POPUP = "sp_external_search_with_pop"
        const val K_ENABLE_SAVE_DATA = "sp_enable_save_data"
        const val K_HIDE_STATUSBAR = "sp_hide_statusbar"

        const val K_ENABLE_OPEN_AI_STREAM = "sp_enable_open_ai_stream"

        const val K_EXTERNAL_SEARCH_IN_SAME_TAB = "sp_external_search_in_same_tab"

        const val K_SHOW_NEXT_AFTER_REMOVE_TAB = "sp_show_previous_after_remove_tab"

        const val K_SHOW_ACTION_MENU_ICONS = "sp_show_action_menu_icons"

        const val K_ENABLE_IN_PLACE_PARAGRAPH_TRANSLATE = "sp_enable_in_place_paragraph_translate"

        const val K_SHOW_TRANSLATED_IMAGE_TO_SECOND_PANEL =
            "sp_show_translated_image_to_second_panel"

        const val ADBLOCK_URL_DEFAULT =
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

        private val K_PRESERVE_HEIGHT = "sp_page_turn_left_value"
        private val K_PRESERVE_HEIGHT_IN_STRING = "sp_page_turn_left_value_in_string"

        private const val ALBUM_INFO_SEPARATOR = "::::"
        private const val RECENT_BOOKMARKS_SEPARATOR = "::::"
        private const val EPUB_FILE_INFO_SEPARATOR = "::::"

        private const val RECENT_BOOKMARK_LIST_SIZE = 10

        private const val K_SPLIT_SEARCH_ITEMS = "sp_split_search_items"
        private const val K_GPT_ACTION_ITEMS = "sp_gpt_action_items"
        private const val K_GPT_ACTION_EXTERNAL = "sp_gpt_action_external"

        private const val K_GPT_SERVER_URL = "sp_gpt_server_url"
        private const val K_USE_CUSTOM_GPT_URL = "sp_use_custom_gpt_url"
        private const val K_USE_GEMINI_API = "sp_use_gemini_api"
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

class BooleanPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Boolean = false,
) : ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}

class IntPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Int = 0,
) : ReadWriteProperty<Any, Int> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Int =
        sharedPreferences.getInt(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
        sharedPreferences.edit { putInt(key, value) }
}

class StringPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: String = "",
) : ReadWriteProperty<Any, String> {

    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        sharedPreferences.getString(key, defaultValue) ?: defaultValue

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) =
        sharedPreferences.edit { putString(key, value) }
}

class GestureTypePreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
) : ReadWriteProperty<Any, GestureType> {

    override fun getValue(thisRef: Any, property: KProperty<*>): GestureType =
        GestureType.from(sharedPreferences.getString(key, "01") ?: "01")

    override fun setValue(thisRef: Any, property: KProperty<*>, value: GestureType) =
        sharedPreferences.edit { putString(key, value.value) }
}


enum class PaperSize(val sizeString: String, val mediaSize: PrintAttributes.MediaSize) {
    ISO_13("A4 (13\")", PrintAttributes.MediaSize.ISO_A4),
    SIZE_10("A5 (10\")", PrintAttributes.MediaSize.ISO_A5),
    ISO_67("Hisense A7 (6.7\")", PrintAttributes.MediaSize.PRC_5),
    SIZE_8("C6 (8\")", PrintAttributes.MediaSize.ISO_C6),
}

enum class FabPosition {
    Right, Left, Center, NotShow, Custom
}

enum class TranslationMode(val labelResId: Int) {
    ONYX(R.string.onyx),
    GOOGLE(R.string.google_text),
    PAPAGO(R.string.papago_text),
    PAPAGO_URL(R.string.papago_full_page),
    GOOGLE_URL(R.string.google_full_page),
    PAPAGO_DUAL(R.string.papago_dual_pane),
    GOOGLE_IN_PLACE(R.string.google_in_place),
    TRANSLATE_BY_PARAGRAPH(R.string.translate_by_paragraph),
    PAPAGO_TRANSLATE_BY_PARAGRAPH(R.string.papago_translate_by_paragraph),
    PAPAGO_TRANSLATE_BY_SCREEN(R.string.papago_translate_by_screen)
}

enum class FontType(val resId: Int) {
    SYSTEM_DEFAULT(R.string.system_default),
    SERIF(R.string.serif),
    GOOGLE_SERIF(R.string.googleserif),
    CUSTOM(R.string.custom_font),
    TC_WENKAI(R.string.wenkai_tc),
    JA_MINCHO(R.string.mincho_ja),
    KO_GAMJA(R.string.gamja_flower_ko)
}

enum class DarkMode {
    SYSTEM, FORCE_ON, DISABLED
}

enum class NewTabBehavior {
    START_INPUT, SHOW_HOME, SHOW_RECENT_BOOKMARKS
}

enum class HighlightStyle(
    val color: Color?,
    val stringResId: Int,
    val iconResId: Int,
) {
    UNDERLINE(
        null,
        R.string.underline,
        R.drawable.ic_underscore,
    ),
    BACKGROUND_YELLOW(
        Color.Yellow,
        R.string.yellow,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_GREEN(
        Color.Green,
        R.string.green,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_BLUE(
        Color.Blue,
        R.string.blue,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_PINK(
        Color.Red,
        R.string.pink,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_NONE(
        null,
        R.string.menu_delete,
        R.drawable.icon_delete,
    )
}

enum class SaveHistoryMode {
    SAVE_WHEN_OPEN, SAVE_WHEN_CLOSE, DISABLED
}

fun KMutableProperty0<Boolean>.toggle() = set(!get())