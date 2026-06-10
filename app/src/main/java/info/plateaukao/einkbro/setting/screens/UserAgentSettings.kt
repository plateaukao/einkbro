package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ValueSettingItem

fun buildUserAgentSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        BooleanSettingItem(
            R.string.setting_title_userAgent_toggle,
            0,
            R.string.setting_summary_userAgent_toggle,
            config.browser::enableCustomUserAgent
        ),
        ValueSettingItem(
            R.string.setting_title_userAgent,
            0,
            R.string.setting_summary_userAgent,
            config.browser::customUserAgent
        ),
    )
}
