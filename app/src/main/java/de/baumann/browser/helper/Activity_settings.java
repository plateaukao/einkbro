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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import de.baumann.browser.Browser;
import de.baumann.browser.R;


public class Activity_settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_settings);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        setTitle(R.string.menu_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private void addOpenSettingsListener() {

            final Activity activity = getActivity();
            Preference reset = findPreference("settings");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference pref)
                {

                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    getActivity().startActivity(intent);

                    return true;
                }
            });
        }

        private void addLicenseListener() {

            Preference reset = findPreference("license");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final AlertDialog d = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.about_title)
                            .setMessage(helpers.textSpannable(getString(R.string.about_text)))
                            .setPositiveButton(getString(R.string.toast_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                    d.show();
                    ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                    return true;
                }
            });
        }

        private void addHelpListener() {

            Preference reset = findPreference("help");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final AlertDialog d = new AlertDialog.Builder(getActivity())
                            .setMessage(helpers.textSpannable(getString(R.string.help_text)))
                            .setPositiveButton(getString(R.string.toast_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                    d.show();
                    ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                    return true;
                }
            });
        }

        private void addBackup_dbListener() {

            Preference reset = findPreference("backup_db");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    File directory = new File(Environment.getExternalStorageDirectory() + "/Android/data/browser.backup/");
                    if (!directory.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        directory.mkdirs();
                    }

                    try {
                        File sd = Environment.getExternalStorageDirectory();
                        File data = Environment.getDataDirectory();

                        if (sd.canWrite()) {
                            String currentDBPath = "//data//" + "de.baumann.browser"
                                    + "//databases//" + "browser.db";
                            String backupDBPath = "//Android//" + "//data//" + "//browser.backup//" + "browser.db";
                            File currentDB = new File(data, currentDBPath);
                            File backupDB = new File(sd, backupDBPath);

                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(backupDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();

                            String currentDBPath2 = "//data//" + "de.baumann.browser"
                                    + "//databases//" + "readLater.db";
                            String backupDBPath2 = "//Android//" + "//data//" + "//browser.backup//" + "readLater.db";
                            File currentDB2 = new File(data, currentDBPath2);
                            File backupDB2 = new File(sd, backupDBPath2);

                            FileChannel src2 = new FileInputStream(currentDB2).getChannel();
                            FileChannel dst2 = new FileOutputStream(backupDB2).getChannel();
                            dst2.transferFrom(src2, 0, src2.size());
                            src2.close();
                            dst2.close();

                            Toast.makeText(getActivity(), R.string.toast_backup, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), R.string.toast_backup_not, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }

        private void addRestore_dbListener() {

            Preference reset = findPreference("restore_db");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    try {
                        File sd = Environment.getExternalStorageDirectory();
                        File data = Environment.getDataDirectory();

                        if (sd.canWrite()) {
                            String currentDBPath = "//data//" + "de.baumann.browser"
                                    + "//databases//" + "browser.db";
                            String backupDBPath = "//Android//" + "//data//" + "//browser.backup//" + "browser.db";
                            File currentDB = new File(data, currentDBPath);
                            File backupDB = new File(sd, backupDBPath);

                            FileChannel src = new FileInputStream(backupDB).getChannel();
                            FileChannel dst = new FileOutputStream(currentDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();

                            String currentDBPath2 = "//data//" + "de.baumann.browser"
                                    + "//databases//" + "readLater.db";
                            String backupDBPath2 = "//Android//" + "//data//" + "//browser.backup//" + "readLater.db";
                            File currentDB2 = new File(data, currentDBPath2);
                            File backupDB2 = new File(sd, backupDBPath2);

                            FileChannel src2 = new FileInputStream(backupDB2).getChannel();
                            FileChannel dst2 = new FileOutputStream(currentDB2).getChannel();
                            dst2.transferFrom(src2, 0, src2.size());
                            src2.close();
                            dst2.close();
                            Toast.makeText(getActivity(), R.string.toast_restore, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), R.string.toast_restore_not, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);
            addLicenseListener();
            addOpenSettingsListener();
            addBackup_dbListener();
            addRestore_dbListener();
            addHelpListener();
        }
    }

    @Override
    public void onBackPressed() {
        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("url", "");
        sharedPref.edit()
                .putString("url", "")
                .apply();
        helpers.switchToActivity(Activity_settings.this, Browser.class, url, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String url = sharedPref.getString("url", "");
            sharedPref.edit()
                    .putString("url", "")
                    .apply();
            helpers.switchToActivity(Activity_settings.this, Browser.class, url, true);
        }

        return super.onOptionsItemSelected(item);
    }
}