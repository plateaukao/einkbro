package de.baumann.browser.fragment

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.util.Constants
import de.baumann.browser.view.dialog.PrinterDocumentPaperSizeDialog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UiSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, KoinComponent {
    private val config: ConfigManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_ui, rootKey)
        setHasOptionsMenu(true)

        val supportDarkMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        findPreference<ListPreference>("sp_dark_mode")?.isVisible = supportDarkMode

        findPreference<Preference>("sp_clear_recent_bookmarks")!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    config.clearRecentBookmarks()
                    true
                }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // if ti's only for this preference, we should close it right after the dialog
        if (arguments?.getBoolean(Constants.ARG_LAUNCH_TOOLBAR_SETTING, false) == true) {
            activity?.finish()
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        when (key) {
            "nav_position", "start_tab" -> config.restartChanged = true
        }
    }
}