package de.baumann.browser.Fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.baumann.browser.Ninja.R;

public class Fragment_settings_Filter extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_filter);
    }
}
