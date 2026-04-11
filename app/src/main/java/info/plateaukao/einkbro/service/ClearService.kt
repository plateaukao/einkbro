package info.plateaukao.einkbro.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit.clearCache
import info.plateaukao.einkbro.unit.BrowserUnit.clearCookie
import info.plateaukao.einkbro.unit.BrowserUnit.clearHistory
import info.plateaukao.einkbro.unit.BrowserUnit.clearIndexedDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

class ClearService : Service(), KoinComponent {
    private val config: ConfigManager by inject()
    private val coroutineScope: CoroutineScope by inject()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        exitProcess(0) // For remove all WebView thread
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        clear()
        stopSelf()
        return START_STICKY
    }

    private fun clear() {
        if (config.clearCache) clearCache(this)
        if (config.clearCookies) clearCookie()
        if (config.clearHistory) coroutineScope.launch { clearHistory(this@ClearService) }
        if (config.clearIndexedDB) clearIndexedDB(this)
    }
}