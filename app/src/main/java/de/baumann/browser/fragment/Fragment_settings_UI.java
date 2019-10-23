package de.baumann.browser.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.baumann.browser.Ninja.R;

public class Fragment_settings_UI extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_ui, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("sp_exit") || key.equals("sp_toggle") || key.equals("sp_add") || key.equals("sp_theme")
                || key.equals("nav_position")  || key.equals("sp_hideOmni") || key.equals("start_tab") || key.equals("sp_hideSB")
                || key.equals("overView_place") || key.equals("overView_hide")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }
}
