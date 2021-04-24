package de.baumann.browser.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.baumann.browser.Ninja.R
import de.baumann.browser.activity.Settings_DataActivity
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.util.Constants
import de.baumann.browser.view.dialog.ToolbarConfigDialog
import de.baumann.browser.view.dialog.TouchAreaDialog
import de.baumann.browser.view.sortlistpreference.MultiSelectDragListPreference
import de.baumann.browser.view.sortlistpreference.MultiSelectDragListPreferenceDialog

class UiSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_ui, rootKey)

        findPreference<Preference>(ConfigManager.K_TOUCH_AREA_TYPE)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    TouchAreaDialog(activity as Context).show()
                    true
                }
        findPreference<Preference>(ConfigManager.K_TOOLBAR_ICONS)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    ToolbarConfigDialog(activity as Context).show()
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
        if ( key == "nav_position" || key == "start_tab") {
            sp.edit().putInt("restart_changed", 1).apply()
        }
    }
}