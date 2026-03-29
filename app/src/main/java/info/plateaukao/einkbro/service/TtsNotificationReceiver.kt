package info.plateaukao.einkbro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TtsNotificationReceiver : BroadcastReceiver(), KoinComponent {
    private val ttsNotificationManager: TtsNotificationManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        ttsNotificationManager.handleAction(intent.action)
    }
}
