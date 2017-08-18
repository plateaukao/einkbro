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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import de.baumann.browser.R;
import de.baumann.browser.about.About_activity;


public class Activity_settings extends AppCompatActivity {

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper_main.setTheme(this);

        setContentView(R.layout.activity_settings);
        helper_main.onStart(Activity_settings.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.menu_settings);

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
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("started", "").apply();

        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private void addSettings_startListener() {
            Preference reset = findPreference("settings_start");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    helper_main.switchToActivity(getActivity(), Activity_settings_start.class);
                    return true;
                }
            });
        }

        private void addSettings_closeListener() {
            Preference reset = findPreference("settings_close");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    helper_main.switchToActivity(getActivity(), Activity_settings_close.class);
                    return true;
                }
            });
        }

        private void addSettings_dataListener() {
            Preference reset = findPreference("settings_data");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    helper_main.switchToActivity(getActivity(), Activity_settings_data.class);
                    return true;
                }
            });
        }

        private void addSettings_appListener() {
            Preference reset = findPreference("settings_app");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    helper_main.switchToActivity(getActivity(), Activity_settings_app.class);
                    return true;
                }
            });
        }

        private void addSettings_searchMainListener() {
            Preference reset = findPreference("settings_searchMain");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    helper_main.switchToActivity(getActivity(), Activity_settings_searchMain.class);
                    return true;
                }
            });
        }

        private void addLicenseListener() {

            Preference reset = findPreference("license");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    helper_main.switchToActivity(getActivity(), About_activity.class);
                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);
            addLicenseListener();
            addSettings_startListener();
            addSettings_closeListener();
            addSettings_appListener();
            addSettings_dataListener();
            addSettings_searchMainListener();
        }
    }


    @Override
    public void onBackPressed() {
        updateSettings();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            updateSettings();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();

        String URL = sharedPref.getString("openURL","https://github.com/scoute-dich/browser/");

        if (URL.equals("settings_recreate")) {
            updateSettings();
        }

    }

    private void updateSettings () {

        if (sharedPref.getBoolean ("java", false)){
            sharedPref.edit().putString("java_string", getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("java_string", getString(R.string.app_no)).apply();
        }

        if (sharedPref.getString ("cookie", "2").equals("1")){
            sharedPref.edit().putString("cookie_string", getString(R.string.app_yes)).apply();
        } else if (sharedPref.getString ("cookie", "2").equals("2")) {
            sharedPref.edit().putString("cookie_string", getString(R.string.app_yes)).apply();
        } else if (sharedPref.getString ("cookie", "2").equals("3")) {
            sharedPref.edit().putString("cookie_string", getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("loc", false)){
            sharedPref.edit().putString("loc_string", getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("loc_string", getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("blockads_bo", false)){
            sharedPref.edit().putString("blockads_string", getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("blockads_string", getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("request_bo", false)){
            sharedPref.edit().putString("request_string", getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("request_string", getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("pictures", false)){
            sharedPref.edit().putString("pictures_string", getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("pictures_string", getString(R.string.app_no)).apply();
        }

        String URL = sharedPref.getString("openURL","https://github.com/scoute-dich/browser/");

        if (!URL.equals("settings_recreate")) {
            sharedPref.edit().putString("openURL", "settings").apply();
        }

        finishAffinity();
    }
}