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
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

import de.baumann.browser.Browser_1;
import de.baumann.browser.R;
import de.baumann.browser.Browser_2;
import de.baumann.browser.about.About_activity;


public class Activity_settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("started", "").apply();

        // Display the fragment as the activity_screen_main content
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SharedPreferences sharedPref;
        private MAHEncryptor mahEncryptor;
        private String toDecode;

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

        private void addRestore_searchChooseListener() {

            Preference reset = findPreference("searchChoose");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    helper_main.switchToActivity(getActivity(), Activity_settings_search.class, "", false);
                    return true;
                }
            });
        }

        private void addLicenseListener() {

            Preference reset = findPreference("license");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    helper_main.switchToActivity(getActivity(), About_activity.class, "", false);
                    return true;
                }
            });
        }

        private void addBackup_dbListener() {

            Preference reset = findPreference("backup_db");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final CharSequence[] options = {
                            getString(R.string.action_backup),
                            getString(R.string.action_restore)};
                    new AlertDialog.Builder(getActivity())
                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            })
                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            })
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int item) {
                                    if (options[item].equals(getString(R.string.action_backup))) {
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
                                                        + "//databases//" + "bookmarks_DB_v01.db";
                                                String backupDBPath = "//Android//" + "//data//" + "//browser.backup//" + "bookmarks_DB_v01.db";
                                                File currentDB = new File(data, currentDBPath);
                                                File backupDB = new File(sd, backupDBPath);

                                                FileChannel src = new FileInputStream(currentDB).getChannel();
                                                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                                                dst.transferFrom(src, 0, src.size());
                                                src.close();
                                                dst.close();

                                                String currentDBPath2 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "readLater_DB_v01.db";
                                                String backupDBPath2 = "//Android//" + "//data//" + "//browser.backup//" + "readLater_DB_v01.db";
                                                File currentDB2 = new File(data, currentDBPath2);
                                                File backupDB2 = new File(sd, backupDBPath2);

                                                FileChannel src2 = new FileInputStream(currentDB2).getChannel();
                                                FileChannel dst2 = new FileOutputStream(backupDB2).getChannel();
                                                dst2.transferFrom(src2, 0, src2.size());
                                                src2.close();
                                                dst2.close();

                                                String currentDBPath3 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "pass_DB_v01.db";
                                                String backupDBPath3 = "//Android//" + "//data//" + "//browser.backup//" + "pass_DB_v01.db";
                                                File currentDB3 = new File(data, currentDBPath3);
                                                File backupDB3 = new File(sd, backupDBPath3);

                                                FileChannel src3 = new FileInputStream(currentDB3).getChannel();
                                                FileChannel dst3 = new FileOutputStream(backupDB3).getChannel();
                                                dst3.transferFrom(src3, 0, src3.size());
                                                src3.close();
                                                dst3.close();

                                                String currentDBPath4 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "history_DB_v01.db";
                                                String backupDBPath4 = "//Android//" + "//data//" + "//browser.backup//" + "history_DB_v01.db";
                                                File currentDB4 = new File(data, currentDBPath4);
                                                File backupDB4 = new File(sd, backupDBPath4);

                                                FileChannel src4 = new FileInputStream(currentDB4).getChannel();
                                                FileChannel dst4 = new FileOutputStream(backupDB4).getChannel();
                                                dst4.transferFrom(src4, 0, src4.size());
                                                src4.close();
                                                dst4.close();

                                                String whiteList = sharedPref.getString("whiteList", "");

                                                File whiteListBackup = new File(directory, "whiteList.txt");
                                                FileWriter writer = new FileWriter(whiteListBackup);
                                                writer.append(whiteList);
                                                writer.flush();
                                                writer.close();

                                                Toast.makeText(getActivity(), R.string.toast_backup, Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(getActivity(), R.string.toast_backup_not, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    if (options[item].equals(getString(R.string.action_restore))) {
                                        File directory = new File(Environment.getExternalStorageDirectory() + "/Android/data/browser.backup/");

                                        try {
                                            File sd = Environment.getExternalStorageDirectory();
                                            File data = Environment.getDataDirectory();

                                            if (sd.canWrite()) {

                                                File whiteListBackup = new File(directory, "whiteList.txt");
                                                StringBuilder text = new StringBuilder();

                                                BufferedReader br = new BufferedReader(new FileReader(whiteListBackup));
                                                String line;

                                                while ((line = br.readLine()) != null) {
                                                    text.append(line);
                                                    text.append('\n');
                                                }
                                                br.close();
                                                sharedPref.edit().putString("whiteList", text.toString()).apply();

                                                String currentDBPath = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "bookmarks_DB_v01.db";
                                                String backupDBPath = "//Android//" + "//data//" + "//browser.backup//" + "bookmarks_DB_v01.db";
                                                File currentDB = new File(data, currentDBPath);
                                                File backupDB = new File(sd, backupDBPath);

                                                FileChannel src = new FileInputStream(backupDB).getChannel();
                                                FileChannel dst = new FileOutputStream(currentDB).getChannel();
                                                dst.transferFrom(src, 0, src.size());
                                                src.close();
                                                dst.close();

                                                String currentDBPath2 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "readLater_DB_v01.db";
                                                String backupDBPath2 = "//Android//" + "//data//" + "//browser.backup//" + "readLater_DB_v01.db";
                                                File currentDB2 = new File(data, currentDBPath2);
                                                File backupDB2 = new File(sd, backupDBPath2);

                                                FileChannel src2 = new FileInputStream(backupDB2).getChannel();
                                                FileChannel dst2 = new FileOutputStream(currentDB2).getChannel();
                                                dst2.transferFrom(src2, 0, src2.size());
                                                src2.close();
                                                dst2.close();

                                                String currentDBPath3 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "pass_DB_v01.db";
                                                String backupDBPath3 = "//Android//" + "//data//" + "//browser.backup//" + "pass_DB_v01.db";
                                                File currentDB3 = new File(data, currentDBPath3);
                                                File backupDB3 = new File(sd, backupDBPath3);

                                                FileChannel src3 = new FileInputStream(backupDB3).getChannel();
                                                FileChannel dst3 = new FileOutputStream(currentDB3).getChannel();
                                                dst3.transferFrom(src3, 0, src3.size());
                                                src3.close();
                                                dst3.close();

                                                String currentDBPath4 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "history_DB_v01.db";
                                                String backupDBPath4 = "//Android//" + "//data//" + "//browser.backup//" + "history_DB_v01.db";
                                                File currentDB4 = new File(data, currentDBPath4);
                                                File backupDB4 = new File(sd, backupDBPath4);

                                                FileChannel src4 = new FileInputStream(backupDB4).getChannel();
                                                FileChannel dst4 = new FileOutputStream(currentDB4).getChannel();
                                                dst4.transferFrom(src4, 0, src4.size());
                                                src4.close();
                                                dst4.close();

                                                Toast.makeText(getActivity(), R.string.toast_restore, Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(getActivity(), R.string.toast_restore_not, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).show();
                    return true;
                }
            });
        }

        private void addProtectListener() {

            Preference reset = findPreference("protect_PW");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View dialogView = View.inflate(getActivity(), R.layout.dialog_pin, null);

                    final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPin);

                    try {
                        toDecode = mahEncryptor.decode(sharedPref.getString("protect_PW", ""));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pass_userPW.setText(toDecode);

                    builder.setView(dialogView);
                    builder.setTitle(R.string.action_protect);
                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {

                            try {
                                String toEncode = mahEncryptor.encode(pass_userPW.getText().toString().trim());
                                sharedPref.edit().putString("protect_PW", toEncode).apply();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    });

                    final AlertDialog dialog2 = builder.create();
                    // Display the custom alert dialog on interface
                    dialog2.show();
                    helper_editText.showKeyboard(getActivity(), pass_userPW, 0, toDecode, getActivity().getString(R.string.pw_hint));

                    return true;
                }
            });
        }

        private void addWhiteListListener() {

            Preference reset = findPreference("whiteList");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    final android.support.v7.app.AlertDialog.Builder dialog = new android.support.v7.app.AlertDialog.Builder(getActivity())
                            .setTitle(R.string.app_conf)
                            .setMessage(helper_main.textSpannable(getString(R.string.toast_whiteList_confirm)))
                            .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    sharedPref.edit().putString("whiteList", "").apply();
                                    Toast.makeText(getActivity(), R.string.toast_whiteList, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            });
                    dialog.show();

                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings, false);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.user_settings_search, false);
            sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            try {
                mahEncryptor = MAHEncryptor.newInstance(sharedPref.getString("saved_key", ""));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), R.string.toast_error, Toast.LENGTH_SHORT).show();
            }

            addPreferencesFromResource(R.xml.user_settings);
            addLicenseListener();
            addOpenSettingsListener();
            addBackup_dbListener();
            addRestore_searchChooseListener();
            addProtectListener();
            addWhiteListListener();
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getString("lastActivity", "").equals("browser_left")) {
            helper_main.switchToActivity(Activity_settings.this, Browser_1.class, sharedPref.getString("pass_copy_url", ""), true);
        } else {
            helper_main.switchToActivity(Activity_settings.this, Browser_2.class, sharedPref.getString("pass_copy_url", ""), true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPref.getString("lastActivity", "").equals("browser_left")) {
                helper_main.switchToActivity(Activity_settings.this, Browser_1.class, sharedPref.getString("pass_copy_url", ""), true);
            } else {
                helper_main.switchToActivity(Activity_settings.this, Browser_2.class, sharedPref.getString("pass_copy_url", ""), true);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}