package info.plateaukao.einkbro.activity.delegates

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.dialog.compose.FontBoldnessDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontBrowserDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ReaderFontDialogFragment
import java.util.Locale

/**
 * Tracks orientation, night-mode, and UI-locale state on behalf of the hosting activity,
 * and drives the recreate / in-place locale update decisions that used to live inline in
 * [info.plateaukao.einkbro.activity.BrowserActivity].
 */
class DisplayConfigDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
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
                newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
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

    fun showFontSizeChangeDialog() {
        val fm = activity.supportFragmentManager
        if (state.ebWebView.shouldUseReaderFont()) {
            ReaderFontDialogFragment { openCustomFontPicker() }.show(fm, "font_dialog")
        } else {
            FontDialogFragment { openCustomFontPicker() }.show(fm, "font_dialog")
        }
    }

    fun showFontBoldnessDialog() {
        FontBoldnessDialogFragment(
            config.display.fontBoldness,
            okAction = { changedBoldness ->
                config.display.fontBoldness = changedBoldness
                state.ebWebView.applyFontBoldness()
            },
        ).show(activity.supportFragmentManager, "FontBoldnessDialog")
    }

    fun increaseFontSize() {
        val fontSize = if (state.ebWebView.shouldUseReaderFont()) config.display.readerFontSize else config.display.fontSize
        changeFontSize(fontSize + 20)
    }

    fun decreaseFontSize() {
        val fontSize = if (state.ebWebView.shouldUseReaderFont()) config.display.readerFontSize else config.display.fontSize
        if (fontSize > 50) changeFontSize(fontSize - 20)
    }

    fun invertColors() {
        val hasInvertedColor = config.toggleInvertedColor(state.ebWebView.url.orEmpty())
        ViewUnit.invertColor(state.ebWebView, hasInvertedColor)
    }

    fun openCustomFontPicker() {
        FontBrowserDialogFragment(isReaderMode = state.ebWebView.shouldUseReaderFont())
            .show(activity.supportFragmentManager, "font_browser_dialog")
    }

    private fun changeFontSize(size: Int) {
        if (state.ebWebView.shouldUseReaderFont()) config.display.readerFontSize = size
        else config.display.fontSize = size
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
