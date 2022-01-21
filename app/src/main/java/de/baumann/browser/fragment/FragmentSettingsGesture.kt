package de.baumann.browser.fragment

import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import de.baumann.browser.Ninja.R
import android.content.SharedPreferences
import androidx.preference.ListPreference
import de.baumann.browser.view.GestureType

class FragmentSettingsGesture : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_gesture, rootKey)

        findPreference<ListPreference>("setting_multitouch_up")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_down")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_left")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_right")?.setGestureEntries()

        findPreference<ListPreference>("setting_gesture_tb_up")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_tb_down")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_tb_left")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_tb_right")?.setGestureEntries()

        findPreference<ListPreference>("setting_gesture_nav_up")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_down")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_left")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_right")?.setGestureEntries()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == "sp_gestures_use" || key == "sp_gesture_action") {
            sp.edit().putInt("restart_changed", 1).apply()
        }
    }

    private fun ListPreference.setGestureEntries() {
        val context = context ?: return
        entries = GestureType.values().map { context.getString(it.resId) }.toTypedArray()
        entryValues = GestureType.values().map { it.value }.toTypedArray()
    }
}