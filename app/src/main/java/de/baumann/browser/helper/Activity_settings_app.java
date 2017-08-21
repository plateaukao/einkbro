/*
    This file is part of the Browser WebApp.

    Browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import de.baumann.browser.R;


public class Activity_settings_app extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper_main.setTheme(this);

        setContentView(R.layout.activity_settings);
        helper_main.onStart(Activity_settings_app.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.pref_2);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_app, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_close, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_start, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search_main, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_data, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("started", "").apply();

        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SharedPreferences sharedPref;
        private FrameLayout frameLayout;

        private void addOpenSettingsListener() {

            final Activity activity = getActivity();
            Preference reset = findPreference("settings");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    getActivity().startActivity(intent);

                    return true;
                }
            });
        }

        private void addThemeListener() {
            Preference reset = findPreference("theme");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    recreate();
                    return true;
                }
            });
        }

        private void addFullscreenListener() {
            Preference reset = findPreference("fullscreen");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    recreate();
                    return true;
                }
            });
        }

        private void addOrientationListener() {
            Preference reset = findPreference("orientation");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    recreate();
                    return true;
                }
            });
        }

        private void addSwitchListener() {
            Preference reset = findPreference("swipe");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    recreate();
                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            frameLayout = (FrameLayout) getActivity().findViewById(R.id.content_frame);

            addPreferencesFromResource(R.xml.user_settings_app);
            addOpenSettingsListener();
            addThemeListener();addFullscreenListener();
            addOrientationListener();
            addSwitchListener();
        }

        private void recreate () {
            Snackbar snackbar = Snackbar
                    .make(frameLayout, getString(R.string.toast_restart), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sharedPref.edit().putString("openURL", "settings_recreate").apply();
                            getActivity().finish();
                        }
                    });
            snackbar.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}