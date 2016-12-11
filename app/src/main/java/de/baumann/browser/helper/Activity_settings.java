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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

import de.baumann.browser.Bookmarks;
import de.baumann.browser.Browser;
import de.baumann.browser.R;


public class Activity_settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        helper_main.onStart(Activity_settings.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.menu_settings);

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

        private void addRestore_searchChooseListener() {

            Preference reset = findPreference("searchChoose");

            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference pref)
                {

                    helper_main.switchToActivity(getActivity(), Activity_settings_search.class, "", false);

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
                            .setMessage(helper_main.textSpannable(getString(R.string.about_text)))
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

                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    final CharSequence[] options = {
                            getString(R.string.action_backup),
                            getString(R.string.action_restore)};
                    new AlertDialog.Builder(getActivity())
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

                                                String currentDBPath3 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "pass.db";
                                                String backupDBPath3 = "//Android//" + "//data//" + "//browser.backup//" + "pass.db";
                                                File currentDB3 = new File(data, currentDBPath3);
                                                File backupDB3 = new File(sd, backupDBPath3);

                                                FileChannel src3 = new FileInputStream(currentDB3).getChannel();
                                                FileChannel dst3 = new FileOutputStream(backupDB3).getChannel();
                                                dst3.transferFrom(src3, 0, src3.size());
                                                src3.close();
                                                dst3.close();

                                                String currentDBPath4 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "history.db";
                                                String backupDBPath4 = "//Android//" + "//data//" + "//browser.backup//" + "history.db";
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

                                                String currentDBPath3 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "pass.db";
                                                String backupDBPath3 = "//Android//" + "//data//" + "//browser.backup//" + "pass.db";
                                                File currentDB3 = new File(data, currentDBPath3);
                                                File backupDB3 = new File(sd, backupDBPath3);

                                                FileChannel src3 = new FileInputStream(backupDB3).getChannel();
                                                FileChannel dst3 = new FileOutputStream(currentDB3).getChannel();
                                                dst3.transferFrom(src3, 0, src3.size());
                                                src3.close();
                                                dst3.close();

                                                String currentDBPath4 = "//data//" + "de.baumann.browser"
                                                        + "//databases//" + "history.db";
                                                String backupDBPath4 = "//Android//" + "//data//" + "//browser.backup//" + "history.db";
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

                    final Activity activity = getActivity();
                    final class_SecurePreferences sharedPrefSec = new class_SecurePreferences(activity, "sharedPrefSec", "Ywn-YM.XK$b:/:&CsL8;=L,y4", true);
                    final String password = sharedPrefSec.getString("protect_PW");

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View dialogView = View.inflate(activity, R.layout.dialog_pin, null);

                    final EditText pass_userPW = (EditText) dialogView.findViewById(R.id.pass_userPin);
                    pass_userPW.setText(password);

                    builder.setView(dialogView);
                    builder.setTitle(R.string.action_protect);
                    builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {

                            String inputTag = pass_userPW.getText().toString().trim();
                            sharedPrefSec.put("protect_PW", inputTag);

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
                    helper_editText.showKeyboard(activity, pass_userPW, 0, password, activity.getString(R.string.pw_hint));

                    return true;
                }
            });
        }

        private void addWhiteListListener() {

            Preference reset = findPreference("whiteList");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    sharedPref.edit().putString("whiteList", "").apply();
                    Toast.makeText(getActivity(), R.string.toast_whiteList, Toast.LENGTH_SHORT).show();

                    return true;
                }
            });
        }

        private void addDonateListListener() {

            Preference reset = findPreference("donate");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NP6TGYDYP9SHY"); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }
            });
        }

        private void addChangelogListener() {

            Preference reset = findPreference("changelog");
            reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref) {
                    Uri uri = Uri.parse("https://github.com/scoute-dich/browser/blob/master/CHANGELOG.md"); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
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
            addRestore_searchChooseListener();
            addProtectListener();
            addWhiteListListener();
            addDonateListListener();
            addChangelogListener();
        }
    }

    @Override
    public void onBackPressed() {
        helper_main.isOpened(Activity_settings.this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getString("lastActivity", "").equals("browser")) {
            helper_main.switchToActivity(Activity_settings.this, Browser.class, sharedPref.getString("pass_copy_url", ""), true);
        } else {
            helper_main.switchToActivity(Activity_settings.this, Bookmarks.class, "", true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            helper_main.isOpened(Activity_settings.this);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPref.getString("lastActivity", "").equals("browser")) {
                helper_main.switchToActivity(Activity_settings.this, Browser.class, sharedPref.getString("pass_copy_url", ""), true);
            } else {
                helper_main.switchToActivity(Activity_settings.this, Bookmarks.class, "", true);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}