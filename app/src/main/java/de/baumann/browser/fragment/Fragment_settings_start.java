package de.baumann.browser.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.baumann.browser.activity.Whitelist_Cookie;
import de.baumann.browser.activity.Whitelist_Javascript;
import de.baumann.browser.activity.Whitelist_AdBlock;
import de.baumann.browser.Ninja.R;

public class Fragment_settings_start extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_start, rootKey);

        Objects.requireNonNull(findPreference("start_AdBlock")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Whitelist_AdBlock.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("start_java")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Whitelist_Javascript.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("start_cookie")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Whitelist_Cookie.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
    }
}
