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
import de.baumann.browser.Task.ExportWhitelistCookieTask;
import de.baumann.browser.Task.ExportWhitelistJSTask;
import de.baumann.browser.Task.ExportWhitelistAdBlockTask;
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

        String previewsPath_app = "//data//" + getActivity().getPackageName() + "//files";
        String previewsPath_backup = "browser_backup//previews";
        final File previewsFolder_app = new File(data, previewsPath_app);
        final File previewsFolder_backup = new File(sd, previewsPath_backup);

        String databasePath_app = "//data//" + getActivity().getPackageName() + "//databases//Ninja4.db";
        String databasePath_backup = "browser_backup//databases//browser_database.db";
        String bookmarksPath_app = "//data//" + getActivity().getPackageName() + "//databases//pass_DB_v01.db";
        String bookmarksPath_backup = "browser_backup//databases//browser_bookmarks.db";

        final File databaseFile_app = new File(data, databasePath_app);
        final File databaseFile_backup = new File(sd, databasePath_backup);
        final File bookmarkFile_app = new File(data, bookmarksPath_app);
        final File bookmarkFile_backup = new File(sd, bookmarksPath_backup);

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
            case R.string.setting_title_import_whitelist:dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        new ImportWhitelistAdBlockTask(getActivity()).execute();
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
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        new ImportWhitelistJSTask(getActivity()).execute();
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
                dialog = new BottomSheetDialog(getActivity());
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        new ImportWhitelistCookieTask(getActivity()).execute();
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
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    NinjaToast.show(getActivity(), R.string.toast_permission_sdCard_sec);
                                } else {
                                    BrowserUnit.deleteDir(bookmarkFile_backup);
                                    BrowserUnit.exportBookmarks(getActivity());
                                    copyDirectory(bookmarkFile_app, bookmarkFile_backup);
                                    NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                                }

                            } else {
                                BrowserUnit.deleteDir(bookmarkFile_backup);
                                BrowserUnit.exportBookmarks(getActivity());
                                copyDirectory(bookmarkFile_app, bookmarkFile_backup);
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
            case R.string.setting_title_import_bookmarks:
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
                                    BrowserUnit.importBookmarks(getActivity());
                                    copyDirectory(previewsFolder_backup, previewsFolder_app);
                                    copyDirectory(databaseFile_backup, databaseFile_app);
                                    copyDirectory(bookmarkFile_backup, bookmarkFile_app);
                                    dialogRestart();
                                }
                            } else {
                                BrowserUnit.importBookmarks(getActivity());
                                copyDirectory(previewsFolder_backup, previewsFolder_app);
                                copyDirectory(databaseFile_backup, databaseFile_app);
                                copyDirectory(bookmarkFile_backup, bookmarkFile_app);
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
                                    BrowserUnit.deleteDir(previewsFolder_backup);
                                    BrowserUnit.deleteDir(databaseFile_backup);
                                    BrowserUnit.deleteDir(bookmarkFile_backup);
                                    BrowserUnit.exportBookmarks(getActivity());
                                    copyDirectory(previewsFolder_app, previewsFolder_backup);
                                    copyDirectory(databaseFile_app, databaseFile_backup);
                                    copyDirectory(bookmarkFile_app, bookmarkFile_backup);
                                    NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                                }

                            } else {
                                BrowserUnit.deleteDir(previewsFolder_backup);
                                BrowserUnit.deleteDir(databaseFile_backup);
                                BrowserUnit.deleteDir(bookmarkFile_backup);
                                BrowserUnit.exportBookmarks(getActivity());
                                copyDirectory(previewsFolder_app, previewsFolder_backup);
                                copyDirectory(databaseFile_app, databaseFile_backup);
                                copyDirectory(bookmarkFile_app, bookmarkFile_backup);
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
                                    BrowserUnit.importBookmarks(getActivity());
                                    copyDirectory(previewsFolder_backup, previewsFolder_app);
                                    copyDirectory(databaseFile_backup, databaseFile_app);
                                    copyDirectory(bookmarkFile_backup, bookmarkFile_app);
                                    dialogRestart();
                                }
                            } else {
                                BrowserUnit.importBookmarks(getActivity());
                                copyDirectory(previewsFolder_backup, previewsFolder_app);
                                copyDirectory(databaseFile_backup, databaseFile_app);
                                copyDirectory(bookmarkFile_backup, bookmarkFile_app);
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
