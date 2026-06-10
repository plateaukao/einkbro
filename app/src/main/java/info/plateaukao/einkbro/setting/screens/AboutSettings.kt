package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.LinkSettingItem
import info.plateaukao.einkbro.setting.ProgressActionSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.unit.HelperUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun buildAboutSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> =
    mutableListOf<SettingItemInterface>().apply {
        addAll(LinkSettingItem.entries.filter { it != LinkSettingItem.Manual })
        add(DividerSettingItem())
        if (BuildConfig.showUpdateButton) {
            add(
                ProgressActionSettingItem(
                    R.string.setting_title_github_update,
                    0,
                ) { progressCallback ->
                    withContext(Dispatchers.IO) {
                        HelperUnit.upgradeToLatestRelease(deps.activity) { progress ->
                            progressCallback.updateProgress(progress)
                        }
                    }
                })
            add(
                ProgressActionSettingItem(
                    R.string.setting_title_github_snapshot,
                    0,
                ) { progressCallback ->
                    withContext(Dispatchers.IO) {
                        HelperUnit.upgradeFromSnapshot(deps.activity) { progress ->
                            progressCallback.updateProgress(progress)
                        }
                    }
                })
        }
        add(DividerSettingItem())
    }
