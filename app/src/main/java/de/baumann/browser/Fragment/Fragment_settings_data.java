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

import de.baumann.browser.Activity.Whitelist_Javascript;
import de.baumann.browser.Activity.Whitelist_AdBlock;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Task.ExportBookmarksTask;
import de.baumann.browser.Task.ExportWhitelistCookieTask;
import de.baumann.browser.Task.ExportWhitelistJSTask;
import de.baumann.browser.Task.ExportWhitelistAdBlockTask;
import de.baumann.browser.Task.ImportBookmarksTask;
import de.baumann.browser.Task.ImportWhitelistAdBlockTask;
import de.baumann.browser.Task.ImportWhitelistCookieTask;
import de.baumann.browser.Task.ImportWhitelistJSTask;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.View.NinjaToast;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Fragment_settings_data extends PreferenceFragment {

    private BottomSheetDialog dialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_data);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        View dialogView;
        TextView textView;
        Button action_ok;
        Button action_cancel;

        File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File data = Environment.getDataDirectory();

        String currentDBPath = "//data//" + getActivity().getPackageName() + "//files";
        String backupDBPath = "browser_backup//previews";

        final File pv_data = new File(data, currentDBPath);
        final File pv_sd = new File(sd, backupDBPath);

        String currentDBPath2 = "//data//" + getActivity().getPackageName() + "//databases//Ninja4.db";
        String backupDBPath2 = "browser_backup//databases//Browser.db";

        final File db_data = new File(data, currentDBPath2);
        final File db_sd = new File(sd, backupDBPath2);

        switch (preference.getTitleRes()) {
            case R.string.setting_title_whitelist:
                Intent toWhitelist = new Intent(getActivity(), Whitelist_AdBlock.class);
                getActivity().startActivity(toWhitelist);
                break;
            case R.string.setting_title_whitelistJS:
                Intent toJavascript = new Intent(getActivity(), Whitelist_Javascript.class);
                getActivity().startActivity(toJavascript);
                break;
            case R.string.setting_title_export_whitelist:
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        makeBackupDir();
                        new ExportWhitelistAdBlockTask(getActivity()).execute();
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;
            case R.string.setting_title_import_whitelist:
                new ImportWhitelistAdBlockTask(getActivity()).execute();
                break;
            case R.string.setting_title_export_whitelistJS:
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        makeBackupDir();
                        new ExportWhitelistJSTask(getActivity()).execute();
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;
            case R.string.setting_title_import_whitelistJS:
                new ImportWhitelistJSTask(getActivity()).execute();
                break;
            case R.string.setting_title_export_whitelistCookie:
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        makeBackupDir();
                        new ExportWhitelistCookieTask(getActivity()).execute();
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;
            case R.string.setting_title_import_whitelistCookie:
                new ImportWhitelistCookieTask(getActivity()).execute();
                break;
            case R.string.setting_title_export_bookmarks:
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        makeBackupDir();
                        new ExportBookmarksTask(getActivity()).execute();
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;
            case R.string.setting_title_import_bookmarks:
                new ImportBookmarksTask(getActivity()).execute();
                break;
            case R.string.setting_title_export_database:
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();

                        makeBackupDir();
                        try {

                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
                                } else {
                                    BrowserUnit.deleteDir(pv_sd);
                                    BrowserUnit.deleteDir(db_sd);
                                    copyDirectory(pv_data, pv_sd);
                                    copyDirectory(db_data, db_sd);
                                    NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                                }

                            } else {
                                BrowserUnit.deleteDir(pv_sd);
                                BrowserUnit.deleteDir(db_sd);
                                copyDirectory(pv_data, pv_sd);
                                copyDirectory(db_data, db_sd);
                                NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;

            case R.string.setting_title_import_database:

                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();

                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
                                } else {
                                    copyDirectory(pv_sd, pv_data);
                                    copyDirectory(db_sd, db_data);
                                    dialogRestart();
                                }

                            } else {
                                copyDirectory(pv_sd, pv_data);
                                copyDirectory(db_sd, db_data);
                                dialogRestart();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                action_cancel = dialogView.findViewById(R.id.action_cancel);
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

    private void makeBackupDir () {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "browser_backup//");
        File noMedia = new File(backupDir, "//.nomedia");

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
            } else {
                if(!backupDir.exists()) {
                    try {
                        backupDir.mkdirs();
                        noMedia.createNewFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if(!backupDir.exists()) {
                try {
                    backupDir.mkdirs();
                    noMedia.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void dialogRestart () {
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
