package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ShareLongPressAction
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.SettingItemInterface

fun buildBehaviorSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        // Tab behavior
        BooleanSettingItem(
            R.string.setting_title_saveTabs,
            0,
            R.string.setting_summary_saveTabs,
            config.tab::shouldSaveTabs,
        ),
        BooleanSettingItem(
            R.string.setting_title_background_loading,
            0,
            R.string.setting_summary_background_loading,
            config.tab::enableWebBkgndLoad,
        ),
        BooleanSettingItem(
            R.string.setting_title_next_tab,
            0,
            R.string.setting_summary_next_tab,
            config.tab::shouldShowNextAfterRemoveTab,
        ),
        BooleanSettingItem(
            R.string.settings_title_back_key_behavior,
            0,
            R.string.settings_summary_back_key_behavior,
            config.tab::closeTabWhenNoMoreBackHistory,
        ),
        BooleanSettingItem(
            R.string.setting_title_confirm_tab_close,
            0,
            R.string.setting_summary_confirm_tab_close,
            config.tab::confirmTabClose,
        ),
        DividerSettingItem(),
        // URL & navigation
        BooleanSettingItem(
            R.string.setting_title_trim_input_url,
            0,
            R.string.setting_summary_trim_input_url,
            config.browser::shouldTrimInputUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_prune_query_parameter,
            0,
            R.string.setting_summary_prune_query_parameter,
            config.browser::shouldPruneQueryParameters,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_url_drag_to_action,
            0,
            R.string.setting_summary_enable_url_drag_to_action,
            config.touch::enableDragUrlToAction,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_bookmarks_input_bar,
            0,
            R.string.setting_summary_show_bookmarks_input_bar,
            config.browser::showBookmarksInInputBar,
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_share_long_press,
            0,
            R.string.setting_summary_share_long_press,
            config.browser::shareLongPressAction,
            ShareLongPressAction.entries.map { it.labelResId },
        ),
        DividerSettingItem(),
        // Video
        BooleanSettingItem(
            R.string.setting_title_video_autoplay,
            0,
            R.string.setting_summary_video_autoplay,
            config.browser::enableVideoAutoplay,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_auto_fullscreen,
            0,
            R.string.setting_summary_video_auto_fullscreen,
            config.browser::enableVideoAutoFullscreen,
        ),
        BooleanSettingItem(
            R.string.setting_title_video_pip,
            0,
            R.string.setting_summary_video_pip,
            config.browser::enableVideoPip,
        ),
        DividerSettingItem(),
        // Input & controls
        BooleanSettingItem(
            R.string.setting_title_vi_binding,
            0,
            R.string.setting_summary_vi_binding,
            config.browser::enableViBinding,
        ),
        BooleanSettingItem(
            R.string.setting_title_disable_long_press_toucharea,
            0,
            R.string.setting_summary_disable_long_press_toucharea,
            config.touch::disableLongPressTouchArea,
        ),
        BooleanSettingItem(
            R.string.setting_title_useUpDown,
            0,
            R.string.setting_summary_useUpDownKey,
            config.touch::useUpDownPageTurn,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_pull_to_refresh,
            0,
            R.string.setting_summary_enable_pull_to_refresh,
            config.browser::enablePullToRefresh,
        ),
        DividerSettingItem(),
        // Display & rendering
        BooleanSettingItem(
            R.string.setting_title_screen_awake,
            0,
            R.string.setting_summary_screen_awake,
            config.ui::keepAwake,
        ),
        BooleanSettingItem(
            R.string.setting_title_text_wrap_reflow,
            0,
            R.string.setting_summary_text_wrap_reflow,
            config.display::enableZoomTextWrapReflow,
        ),
        BooleanSettingItem(
            R.string.setting_title_zoom_in_custom_view,
            0,
            R.string.setting_summary_zoom_in_custom_view,
            config.display::zoomInCustomView,
        ),
        DividerSettingItem(),
        // Security & network
        BooleanSettingItem(
            R.string.setting_title_enable_ssl_error_dialog,
            0,
            R.string.setting_summary_enable_ssl_error_dialog,
            config.browser::enableCertificateErrorDialog,
        ),
        BooleanSettingItem(
            R.string.setting_title_enable_web_cache,
            0,
            R.string.setting_summary_enabling_web_cache,
            config.browser::webLoadCacheFirst,
        ),
    )
}
