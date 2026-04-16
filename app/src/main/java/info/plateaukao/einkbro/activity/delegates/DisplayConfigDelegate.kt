package info.plateaukao.einkbro.activity.delegates

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import java.util.Locale

/**
 * Tracks orientation, night-mode, and UI-locale state on behalf of the hosting activity,
 * and drives the recreate / in-place locale update decisions that used to live inline in
 * [info.plateaukao.einkbro.activity.BrowserActivity].
 */
class DisplayConfigDelegate(
    private val activity: Activity,
    private val config: ConfigManager,
    private val onOrientationChanged: (Int) -> Unit,
    private val onLocaleApplied: () -> Unit,
) {
    var orientation: Int = 0
        private set

    private var uiMode: Int = Configuration.UI_MODE_NIGHT_UNDEFINED
    private var currentLocale: String = ""

    fun onCreate() {
        currentLocale = config.uiLocaleLanguage
        orientation = activity.resources.configuration.orientation
    }

    fun onResume() {
        if (currentLocale != config.uiLocaleLanguage) {
            currentLocale = config.uiLocaleLanguage
            applyLocaleInPlace()
        }
        uiMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags =
                activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags != uiMode && config.display.darkMode == DarkMode.SYSTEM) {
                activity.recreate()
                return
            }
        }
        if (newConfig.orientation != orientation) {
            orientation = newConfig.orientation
            onOrientationChanged(orientation)
        }
    }

    private fun applyLocaleInPlace() {
        val languageCode = config.uiLocaleLanguage
        val locale = if (languageCode.isNotEmpty()) Locale.forLanguageTag(languageCode) else Locale.getDefault()
        Locale.setDefault(locale)
        val newConfig = Configuration(activity.resources.configuration)
        newConfig.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newConfig.setLocales(LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(newConfig, activity.resources.displayMetrics)
        onLocaleApplied()
    }
}
