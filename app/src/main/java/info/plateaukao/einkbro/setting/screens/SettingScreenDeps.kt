package info.plateaukao.einkbro.setting.screens

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.preference.ConfigManager

/**
 * Minimal set of dependencies needed by the setting screen item builders.
 * Backup-related actions are routed through [BackupOps] so the builders
 * don't need direct access to activity-result launchers.
 */
class SettingScreenDeps(
    val activity: FragmentActivity,
    val config: ConfigManager,
    val lifecycleScope: LifecycleCoroutineScope,
    val backupOps: BackupOps,
)

interface BackupOps {
    fun exportAppData()
    fun importAppData()
    fun shareAppData()
    fun receiveAppData()
    fun exportBookmarks()
    fun importBookmarks()
}
