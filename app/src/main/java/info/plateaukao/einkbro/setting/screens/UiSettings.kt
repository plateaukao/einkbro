package info.plateaukao.einkbro.setting.screens

import android.content.Intent
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.MenuItemHideActivity
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ValueSettingItem
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import kotlinx.coroutines.launch

fun buildUiSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ActionSettingItem(
            R.string.setting_app_locale,
            0,
            R.string.setting_summary_app_locale,
        ) {
            deps.lifecycleScope.launch {
                val oldLocale = config.uiLocaleLanguage
                TranslationLanguageDialog(deps.activity).showAppLocale()
                if (config.uiLocaleLanguage != oldLocale) deps.activity.recreate()
            }
        },
        BooleanSettingItem(
            R.string.hide_statusbar,
            0,
            R.string.setting_summary_hide_statusbar,
            config.ui::hideStatusbar,
        ),
        BooleanSettingItem(
            R.string.desktop_mode,
            0,
            R.string.setting_summary_desktop,
            config.browser::desktop,
        ),
        BooleanSettingItem(
            R.string.always_enable_zoom,
            0,
            R.string.setting_summary_enable_zoom,
            config.display::enableZoom,
        ),
        BooleanSettingItem(
            R.string.show_default_text_menu,
            0,
            R.string.setting_summary_show_default_text_menu,
            config.ui::showDefaultActionMenu,
        ),
        BooleanSettingItem(
            R.string.show_context_menu_icons,
            0,
            R.string.setting_summary_show_context_menu_icons,
            config.ui::showActionMenuIcons,
        ),
        BooleanSettingItem(
            R.string.show_history_thumbnail_grid,
            0,
            R.string.setting_summary_show_history_thumbnail_grid,
            config.ui::showHistoryThumbnailGrid,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_page_left_value,
            0,
            R.string.setting_summary_page_left_value,
            config.touch::pageReservedOffsetInString
        ),
        ValueSettingItem(
            R.string.setting_title_reader_mode_padding,
            0,
            R.string.setting_summary_reader_mode_padding,
            config.display::paddingForReaderMode
        ),
        ListSettingWithEnumItem(
            R.string.dark_mode,
            0,
            R.string.setting_summary_dark_mode,
            config.display::darkMode,
            listOf(
                R.string.dark_mode_follow_system,
                R.string.dark_mode_force_on,
                R.string.dark_mode_disabled,
            )
        ),
        ListSettingWithEnumItem(
            R.string.eink_image_adjustment,
            0,
            R.string.eink_image_adjustment_summary,
            config.display::einkImageAdjustment,
            listOf(
                R.string.eink_image_off,
                R.string.eink_image_10,
                R.string.eink_image_30,
                R.string.eink_image_50,
                R.string.eink_image_70,
                R.string.eink_image_100,
            )
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_nav_pos,
            0,
            R.string.setting_summary_nav_pos,
            config.ui::fabPosition,
            listOf(
                R.string.setting_summary_nav_pos_right,
                R.string.setting_summary_nav_pos_left,
                R.string.setting_summary_nav_pos_center,
                R.string.setting_summary_nav_pos_not_show,
                R.string.setting_summary_nav_pos_custom,
            )
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_plus_behavior,
            0,
            R.string.setting_summary_plus_behavior,
            config.tab::newTabBehavior,
            listOf(
                R.string.plus_start_input_url,
                R.string.plus_show_homepage,
                R.string.plus_show_bookmarks,
            )
        ),
        ActionSettingItem(
            R.string.setting_clear_recent_bookmarks,
            0,
            R.string.setting_summary_clear_recent_bookmarks,
        ) {
            config.clearRecentBookmarks()
        },
        ActionSettingItem(
            R.string.setting_title_hide_menu_items,
            0,
            R.string.setting_summary_hide_menu_items,
        ) {
            deps.activity.startActivity(Intent(deps.activity, MenuItemHideActivity::class.java))
        },
    )
}
