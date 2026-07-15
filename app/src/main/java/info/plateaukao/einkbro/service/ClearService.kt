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
import kotlinx.coroutines.withTimeoutOrNull
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
        // stopSelf() kills the process (see onDestroy), so it must wait until
        // all clearing has completed and been persisted.
        coroutineScope.launch {
            clear()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun clear() {
        if (config.clearCache) clearCache(this)
        if (config.clearCookies) withTimeoutOrNull(CLEAR_COOKIE_TIMEOUT) { clearCookie() }
        if (config.clearHistory) clearHistory(this)
        if (config.clearIndexedDB) clearIndexedDB(this)
    }

    companion object {
        private const val CLEAR_COOKIE_TIMEOUT = 5000L
    }
}