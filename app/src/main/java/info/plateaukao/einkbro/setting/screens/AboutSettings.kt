package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.setting.LinkSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface

fun buildAboutSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> =
    LinkSettingItem.entries.filter { it != LinkSettingItem.Manual }
