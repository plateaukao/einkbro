package de.baumann.browser.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.view.dialog.DialogManager
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class FontSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_font, rootKey)

        resultLauncher = BrowserUnit.registerCustomFontSelectionResult(this)

        findPreference<Preference>(ConfigManager.K_CUSTOM_FONT)?.apply {
            summary = config.customFontInfo?.name ?: "not configured"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                BrowserUnit.openFontFilePicker(resultLauncher)
                true
            }
        }
        findPreference<Preference>("settings_font_type")?.apply {
            summary = getString(config.fontType.resId)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                dialogManager.showFontTypeDialog()
                true
            }
        }

        config.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private val preferenceListener = OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            ConfigManager.K_CUSTOM_FONT ->
                findPreference<Preference>(ConfigManager.K_CUSTOM_FONT)?.apply {
                    summary = config.customFontInfo?.name ?: "not configured"
                }
            ConfigManager.K_FONT_TYPE ->
                findPreference<Preference>("settings_font_type")?.apply {
                    summary = getString(config.fontType.resId)
                }
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) { }

    override fun onDestroy() {
        config.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onDestroy()
    }
}