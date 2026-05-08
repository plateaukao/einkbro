package info.plateaukao.einkbro.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import info.plateaukao.einkbro.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShareChosenReceiver : BroadcastReceiver(), KoinComponent {
    private val config: ConfigManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val component: ComponentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
        } ?: return
        config.browser.lastShareComponentPackage = component.packageName
        config.browser.lastShareComponentClass = component.className
    }
}
