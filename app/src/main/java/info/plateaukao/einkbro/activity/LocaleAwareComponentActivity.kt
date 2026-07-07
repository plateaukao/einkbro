package info.plateaukao.einkbro.activity

import android.content.Context
import androidx.activity.ComponentActivity
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.LocaleManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base for the Compose config/list activities: applies the in-app UI locale
 * override. Previously only 4 of the 11 such activities did this, so a user
 * with a non-system app language got mixed-language screens.
 */
abstract class LocaleAwareComponentActivity : ComponentActivity(), KoinComponent {
    protected val localeAwareConfig: ConfigManager by inject()

    override fun attachBaseContext(newBase: Context) {
        if (localeAwareConfig.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(
                LocaleManager.setLocale(newBase, localeAwareConfig.uiLocaleLanguage)
            )
        } else {
            super.attachBaseContext(newBase)
        }
    }
}
