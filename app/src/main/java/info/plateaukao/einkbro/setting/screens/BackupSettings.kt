package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface

fun buildBackupSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val backupOps = deps.backupOps
    return listOf(
        ActionSettingItem(
            R.string.setting_title_export_appData,
            0,
            R.string.setting_summary_export_appData
        ) { backupOps.exportAppData() },
        ActionSettingItem(
            R.string.setting_title_import_appData,
            0,
            R.string.setting_summary_import_appData
        ) { backupOps.importAppData() },
        ActionSettingItem(
            R.string.setting_title_share_appData,
            0,
            R.string.setting_summary_share_appData
        ) { backupOps.shareAppData() },
        ActionSettingItem(
            R.string.setting_title_receive_appData,
            0,
            R.string.setting_summary_receive_appData
        ) { backupOps.receiveAppData() },
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_export_bookmarks,
            0,
        ) { backupOps.exportBookmarks() },
        ActionSettingItem(
            R.string.setting_title_import_bookmarks,
            0,
        ) { backupOps.importBookmarks() },
    )
}
