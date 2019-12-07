package de.baumann.browser.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.task.ExportWhiteListTask;
import de.baumann.browser.task.ImportWhitelistTask;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Fragment_settings_data extends PreferenceFragmentCompat {

    private BottomSheetDialog dialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_data, rootKey);

        File sd = Objects.requireNonNull(getActivity()).getExternalFilesDir(null);
        File data = Environment.getDataDirectory();


        String previewsPath_app = "//data//" + Objects.requireNonNull(getActivity()).getPackageName() + "//";
        String previewsPath_backup = "browser_backup//data//";
        final File previewsFolder_app = new File(data, previewsPath_app);
        final File previewsFolder_backup = new File(sd, previewsPath_backup);

        Objects.requireNonNull(findPreference("data_exAdBlock")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                makeBackupDir();
                                new ExportWhiteListTask(getActivity(), 0).execute();
                            }
                        } else {
                            dialog.cancel();
                            makeBackupDir();
                            new ExportWhiteListTask(getActivity(), 0).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_imAdBlock")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                new ImportWhitelistTask(getActivity(), 0).execute();
                            }
                        } else {
                            dialog.cancel();
                            new ImportWhitelistTask(getActivity(), 0).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_exCookie")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                makeBackupDir();
                                new ExportWhiteListTask(getActivity(), 2).execute();
                            }
                        } else {
                            dialog.cancel();
                            makeBackupDir();
                            new ExportWhiteListTask(getActivity(), 2).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_imCookie")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                new ImportWhitelistTask(getActivity(), 2).execute();
                            }
                        } else {
                            dialog.cancel();
                            new ImportWhitelistTask(getActivity(), 2).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_exJava")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                makeBackupDir();
                                new ExportWhiteListTask(getActivity(), 1).execute();
                            }
                        } else {
                            dialog.cancel();
                            makeBackupDir();
                            new ExportWhiteListTask(getActivity(), 1).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_imJava")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(getActivity());
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                                new ImportWhitelistTask(getActivity(), 1).execute();
                            }
                        } else {
                            dialog.cancel();
                            new ImportWhitelistTask(getActivity(), 1).execute();
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_exDB")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
                dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                int hasWRITE_EXTERNAL_STORAGE = getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    HelperUnit.grantPermissionsStorage(getActivity());
                                    dialog.cancel();
                                } else {
                                    makeBackupDir();
                                    BrowserUnit.deleteDir(previewsFolder_backup);
                                    copyDirectory(previewsFolder_app, previewsFolder_backup);
                                    backupUserPrefs(getActivity());
                                    NinjaToast.show(getActivity(), getString(R.string.toast_export_successful) + "browser_backup");
                                }
                            } else {
                                makeBackupDir();
                                BrowserUnit.deleteDir(previewsFolder_backup);
                                copyDirectory(previewsFolder_app, previewsFolder_backup);
                                backupUserPrefs(getActivity());
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });

        Objects.requireNonNull(findPreference("data_imDB")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {

                View dialogView;
                TextView textView;
                Button action_ok;
                Button action_cancel;

                dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
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
                                    HelperUnit.grantPermissionsStorage(getActivity());
                                    dialog.cancel();
                                } else {
                                    BrowserUnit.deleteDir(previewsFolder_app);
                                    copyDirectory(previewsFolder_backup, previewsFolder_app);
                                    restoreUserPrefs(getActivity());
                                    dialogRestart();
                                }
                            } else {
                                BrowserUnit.deleteDir(previewsFolder_app);
                                copyDirectory(previewsFolder_backup, previewsFolder_app);
                                restoreUserPrefs(getActivity());
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
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                return false;
            }
        });
    }

    private void makeBackupDir () {
        File backupDir = new File(Objects.requireNonNull(getActivity()).getExternalFilesDir(null), "browser_backup//");
        File noMedia = new File(backupDir, "//.nomedia");
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = Objects.requireNonNull(getActivity()).checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                HelperUnit.grantPermissionsStorage(getActivity());
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
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.edit().putInt("restart_changed", 1).apply();
    }

    // If targetLocation does not exist, it will be created.
    private void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }
            String[] children = sourceLocation.list();
            for (String aChildren : Objects.requireNonNull(children)) {
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

    private static void backupUserPrefs(Context context) {

        final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/" + context.getPackageName() + "_preferences.xml");
        final File backupFile = new File(context.getExternalFilesDir(null),
                "browser_backup/preferenceBackup.xml");

        try {
            FileChannel src = new FileInputStream(prefsFile).getChannel();
            FileChannel dst = new FileOutputStream(backupFile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            NinjaToast.show(context, "Backed up user prefs to " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ApplySharedPref")
    private static void restoreUserPrefs(Context context) {
        final File backupFile = new File(context.getExternalFilesDir(null),
                "browser_backup/preferenceBackup.xml");
        String error;

        try {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            SharedPreferences.Editor editor = sharedPreferences.edit();

            InputStream inputStream = new FileInputStream(backupFile);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(inputStream);
            Element root = doc.getDocumentElement();

            Node child = root.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) child;

                    String type = element.getNodeName();
                    String name = element.getAttribute("name");

                    // In my app, all prefs seem to get serialized as either "string" or
                    // "boolean" - this will need expanding if yours uses any other types!
                    if (type.equals("string")) {
                        String value = element.getTextContent();
                        editor.putString(name, value);
                    } else if (type.equals("boolean")) {
                        String value = element.getAttribute("value");
                        editor.putBoolean(name, value.equals("true"));
                    }
                }

                child = child.getNextSibling();

            }

            editor.commit();
            NinjaToast.show(context, "Restored user prefs from " + backupFile.getAbsolutePath());

            return;

        } catch (IOException | SAXException | ParserConfigurationException e) {
            error = e.getMessage();
            e.printStackTrace();
        }

        Toast toast = Toast.makeText(context, "Failed to restore user prefs from " + backupFile.getAbsolutePath() + " - " + error, Toast.LENGTH_SHORT);
        toast.show();
    }
}
