package de.baumann.browser.Fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.baumann.browser.Activity.JavascriptActivity;
import de.baumann.browser.Activity.WhitelistActivity;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Task.ExportBookmarksTask;
import de.baumann.browser.Task.ExportWhitelistJSTask;
import de.baumann.browser.Task.ExportWhitelistTask;
import de.baumann.browser.Task.ImportBookmarksTask;
import de.baumann.browser.Task.ImportWhitelistTask;
import de.baumann.browser.Task.ImportWhitelistTaskJS;
import de.baumann.browser.View.NinjaToast;

public class Fragment_settings_data extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_data);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        switch (preference.getTitleRes()) {
            case R.string.setting_title_whitelist:
                Intent toWhitelist = new Intent(getActivity(), WhitelistActivity.class);
                getActivity().startActivity(toWhitelist);
                break;
            case R.string.setting_title_whitelistJS:
                Intent toJavascript = new Intent(getActivity(), JavascriptActivity.class);
                getActivity().startActivity(toJavascript);
                break;
            case R.string.setting_title_export_whitelist:
                new ExportWhitelistTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_whitelist:
                new ImportWhitelistTask(getActivity()).execute();
                break;

            case R.string.setting_title_export_whitelistJS:
                new ExportWhitelistJSTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_whitelistJS:
                new ImportWhitelistTaskJS(getActivity()).execute();
                break;
            case R.string.setting_title_export_bookmarks:
                new ExportBookmarksTask(getActivity()).execute();
                break;
            case R.string.setting_title_import_bookmarks:
                new ImportBookmarksTask(getActivity()).execute();
                break;

            case R.string.setting_title_export_database:
                try {

                    File sd = Environment.getExternalStorageDirectory();
                    File data = Environment.getDataDirectory();

                    String currentDBPath = "//data//" + getActivity().getPackageName();
                    String backupDBPath = "browser_backup";

                    File source = new File(data, currentDBPath);
                    File target = new File(sd, backupDBPath);

                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                            NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
                        } else {
                            copyDirectory(source, target);
                            NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                        }

                    } else {
                        copyDirectory(source, target);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.string.setting_title_import_database:

                final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
                View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();

                        try {

                            File sd = Environment.getExternalStorageDirectory();
                            File data = Environment.getDataDirectory();

                            String currentDBPath = "//data//" + getActivity().getPackageName();
                            String backupDBPath = "browser_backup";

                            File source = new File(data, currentDBPath);
                            File target = new File(sd, backupDBPath);

                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
                                } else {
                                    copyDirectory(target, source);

                                    final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
                                    final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
                                    View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                                    TextView textView = dialogView.findViewById(R.id.dialog_text);
                                    textView.setText(R.string.toast_restart);
                                    Button action_ok = dialogView.findViewById(R.id.action_ok);
                                    action_ok.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            sp.edit().putInt("restart_changed", 1).apply();
                                            getActivity().finish();
                                        }
                                    });
                                    Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                                    action_cancel.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dialog.cancel();
                                        }
                                    });
                                    dialog.setContentView(dialogView);
                                    dialog.show();
                                }

                            } else {
                                copyDirectory(target, source);

                                final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
                                final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
                                View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                                TextView textView = dialogView.findViewById(R.id.dialog_text);
                                textView.setText(R.string.toast_restart);
                                Button action_ok = dialogView.findViewById(R.id.action_ok);
                                action_ok.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        sp.edit().putInt("restart_changed", 1).apply();
                                        getActivity().finish();
                                    }
                                });
                                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                                action_cancel.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.cancel();
                                    }
                                });
                                dialog.setContentView(dialogView);
                                dialog.show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();

                break;

            default:
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // If targetLocation does not exist, it will be created.
    private void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }

            String[] children = sourceLocation.list();
            for (String aChildren : children) {
                copyDirectory(new File(sourceLocation, aChildren),
                        new File(targetLocation, aChildren));
            }
        } else {

            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from InputStream to OutputStream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
