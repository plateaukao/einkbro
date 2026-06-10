package info.plateaukao.einkbro.setting.screens

import android.content.Intent
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.StatusbarConfigActivity
import info.plateaukao.einkbro.activity.ToolbarConfigActivity
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ToolbarPositionSettingItem

fun buildToolbarSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        DividerSettingItem(R.string.setting_section_toolbar),
        ToolbarPositionSettingItem(
            R.string.setting_title_toolbar_position,
            0,
            0,
            config.ui::toolbarPosition,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_tab_bar,
            0,
            R.string.setting_summary_show_tab_bar,
            config.tab::shouldShowTabBar,
        ),
        ActionSettingItem(
            R.string.toolbar_icons,
            0,
            R.string.toolbar_icons_description,
        ) {
            deps.activity.startActivity(Intent(deps.activity, ToolbarConfigActivity::class.java))
        },
        BooleanSettingItem(
            R.string.setting_title_hideToolbar,
            0,
            R.string.setting_summary_hide,
            config.ui::shouldHideToolbar,
        ),
        BooleanSettingItem(
            R.string.setting_title_toolbarShow,
            0,
            R.string.setting_summary_toolbarShow,
            config.ui::showToolbarFirst,
        ),
        DividerSettingItem(R.string.setting_section_statusbar),
        BooleanSettingItem(
            R.string.setting_title_statusbar_enabled,
            0,
            R.string.setting_summary_statusbar_enabled,
            config.ui::statusbarEnabled,
        ),
        ListSettingWithEnumItem(
            R.string.setting_title_statusbar_position,
            0,
            0,
            config.ui::statusbarPosition,
            listOf(
                R.string.statusbar_position_top,
                R.string.statusbar_position_bottom,
            )
        ),
        ActionSettingItem(
            R.string.setting_title_statusbar_items,
            0,
            R.string.setting_summary_statusbar_items,
        ) {
            deps.activity.startActivity(Intent(deps.activity, StatusbarConfigActivity::class.java))
        },
    )
}
