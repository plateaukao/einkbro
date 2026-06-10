package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute.About
import info.plateaukao.einkbro.activity.SettingRoute.Backup
import info.plateaukao.einkbro.activity.SettingRoute.Behavior
import info.plateaukao.einkbro.activity.SettingRoute.ChatGPT
import info.plateaukao.einkbro.activity.SettingRoute.DataControl
import info.plateaukao.einkbro.activity.SettingRoute.Gesture
import info.plateaukao.einkbro.activity.SettingRoute.Misc
import info.plateaukao.einkbro.activity.SettingRoute.Search
import info.plateaukao.einkbro.activity.SettingRoute.StartControl
import info.plateaukao.einkbro.activity.SettingRoute.Toolbar
import info.plateaukao.einkbro.activity.SettingRoute.Ui
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.LinkSettingItem
import info.plateaukao.einkbro.setting.NavigateSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.VersionSettingItem

fun buildMainSettingItems(): List<SettingItemInterface> = listOf(
    NavigateSettingItem(R.string.setting_title_ui, R.drawable.ic_phone, destination = Ui),
    NavigateSettingItem(
        R.string.setting_title_toolbar,
        R.drawable.ic_toolbar,
        destination = Toolbar
    ),
    NavigateSettingItem(
        R.string.setting_title_behavior,
        R.drawable.icon_ui,
        destination = Behavior
    ),
    NavigateSettingItem(
        R.string.setting_gestures,
        R.drawable.gesture_tap,
        destination = Gesture
    ),
    DividerSettingItem(),
    NavigateSettingItem(
        R.string.setting_title_data,
        R.drawable.icon_backup,
        destination = Backup
    ),
    NavigateSettingItem(
        R.string.setting_title_start_control,
        R.drawable.icon_earth,
        destination = StartControl
    ),
    NavigateSettingItem(
        R.string.setting_title_clear_control,
        R.drawable.ic_data,
        destination = DataControl
    ),
    NavigateSettingItem(
        R.string.setting_title_search,
        R.drawable.icon_search,
        destination = Search
    ),
    DividerSettingItem(),
    NavigateSettingItem(
        R.string.misc,
        R.drawable.icon_dots,
        destination = Misc
    ),
    NavigateSettingItem(
        R.string.setting_title_chat_gpt,
        R.drawable.ic_chat_gpt,
        destination = ChatGPT
    ),
    LinkSettingItem.Manual,
    VersionSettingItem(
        R.string.menu_other_info,
        R.drawable.icon_info,
        destination = About,
    ),
)
