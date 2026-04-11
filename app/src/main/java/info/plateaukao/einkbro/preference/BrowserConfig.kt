package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale
import info.plateaukao.einkbro.search.SearchEngine

class BrowserConfig(private val sp: SharedPreferences) {

    var enableJavascript by BooleanPreference(sp, K_JAVASCRIPT, true)
    var adBlock by BooleanPreference(sp, K_ADBLOCK, true)
    var cookies by BooleanPreference(sp, K_COOKIES, true)
    var shareLocation by BooleanPreference(sp, K_SHARE_LOCATION, false)
    var desktop by BooleanPreference(sp, K_DESKTOP, false)
    var continueMedia by BooleanPreference(sp, K_MEDIA_CONTINUE, false)
    var autoFillForm by BooleanPreference(sp, K_AUTO_FILL, true)
    var shouldTrimInputUrl by BooleanPreference(sp, K_TRIM_INPUT_URL, false)
    var shouldPruneQueryParameters by BooleanPreference(sp, K_PRUNE_QUERY_PARAMETERS, false)
    var debugWebView by BooleanPreference(sp, K_DEBUG_WEBVIEW, false)
    var enableRemoteAccess by BooleanPreference(sp, K_ENABLE_REMOTE_ACCESS, true)
    var enableImages by BooleanPreference(sp, K_ENABLE_IMAGES, true)
    var enableVideoAutoFullscreen by BooleanPreference(sp, K_ENABLE_VIDEO_AUTO_FULLSCREEN, false)
    var enableVideoAutoplay by BooleanPreference(sp, K_ENABLE_VIDEO_AUTOPLAY, false)
    var enableVideoPip by BooleanPreference(sp, K_ENABLE_VIDEO_PIP, false)
    var autoUpdateAdblock by BooleanPreference(sp, K_AUTO_UPDATE_ADBLOCK, false)
    var enableCertificateErrorDialog by BooleanPreference(sp, CERTIFICATE_ERROR_DIALOG, true)
    var enableCustomUserAgent by BooleanPreference(sp, K_ENABLE_CUSTOM_USER_AGENT, false)
    var showBookmarksInInputBar by BooleanPreference(sp, K_SHOW_BOOKMARKS_IN_INPUTBAR, false)
    var enableSaveData by BooleanPreference(sp, K_ENABLE_SAVE_DATA, true)
    var blockAnalytics by BooleanPreference(sp, K_BLOCK_ANALYTICS, false)
    var enableSearchSuggestion by BooleanPreference(sp, "sp_enable_search_suggestion", true)
    var enableViBinding by BooleanPreference(sp, K_VI_BINDING, false)
    var webLoadCacheFirst by BooleanPreference(sp, "sp_web_load_cache_first", false)

    var customUserAgent by StringPreference(sp, K_CUSTOM_USER_AGENT)
    var processTextUrl by StringPreference(sp, K_CUSTOM_PROCESS_TEXT_URL, "")
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
    var adblockHostUrl by StringPreference(
        sp,
        K_ADBLOCK_HOSTS_URL,
        ADBLOCK_URL_DEFAULT
    )

    var adSites: MutableSet<String>
        get() = sp.getStringSet(K_ADBLOCK_SITES, mutableSetOf()) ?: mutableSetOf()
        set(value) = sp.edit { putStringSet(K_ADBLOCK_SITES, value) }

    companion object {
        const val K_JAVASCRIPT = "SP_JAVASCRIPT_9"
        const val K_ADBLOCK = "SP_AD_BLOCK_9"
        const val K_COOKIES = "SP_COOKIES_9"
        const val K_SHARE_LOCATION = "SP_LOCATION_9"
        const val K_DESKTOP = "sp_desktop"
        const val K_MEDIA_CONTINUE = "sp_media_continue"
        const val K_AUTO_FILL = "sp_auto_fill"
        const val K_TRIM_INPUT_URL = "sp_trim_input_url"
        const val K_PRUNE_QUERY_PARAMETERS = "sp_prune_query_parameter"
        const val K_DEBUG_WEBVIEW = "sp_debug_webview"
        const val K_ENABLE_REMOTE_ACCESS = "sp_remote"
        const val K_ENABLE_IMAGES = "SP_IMAGES_9"
        const val K_ENABLE_VIDEO_AUTO_FULLSCREEN = "sp_video_auto_fullscreen"
        const val K_ENABLE_VIDEO_AUTOPLAY = "sp_video_autoplay"
        const val K_ENABLE_VIDEO_PIP = "sp_video_auto_pip"
        const val K_AUTO_UPDATE_ADBLOCK = "sp_auto_update_adblock"
        const val CERTIFICATE_ERROR_DIALOG = "sp_certificate_error_dialog"
        const val K_ENABLE_CUSTOM_USER_AGENT = "sp_custom_user_agent"
        const val K_SHOW_BOOKMARKS_IN_INPUTBAR = "sp_show_bookmarks_in_inputbar"
        const val K_ENABLE_SAVE_DATA = "sp_enable_save_data"
        const val K_BLOCK_ANALYTICS = "sp_block_analytics"
        const val K_VI_BINDING = "sp_enable_vi_binding"
        const val K_CUSTOM_USER_AGENT = "userAgent"
        const val K_CUSTOM_PROCESS_TEXT_URL = "sp_process_text_custom"
        const val K_SEARCH_ENGINE = "SP_SEARCH_ENGINE_9"
        const val K_SEARCH_ENGINE_URL = "sp_search_engine_custom"
        const val K_ADBLOCK_HOSTS_URL = "ab_hosts"
        const val K_ADBLOCK_SITES = "sp_adblock_sites"

        const val ADBLOCK_URL_DEFAULT =
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    }
}
