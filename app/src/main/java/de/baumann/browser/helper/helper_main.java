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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.R;

public class helper_main {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;

    public static void grantPermissionsStorage(final Activity from) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (sharedPref.getBoolean ("perm_notShow", false)){
                int hasWRITE_EXTERNAL_STORAGE = from.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    if (!from.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        new AlertDialog.Builder(from)
                                .setTitle(R.string.app_permissions_title)
                                .setMessage(helper_main.textSpannable(from.getString(R.string.app_permissions)))
                                .setNeutralButton(R.string.toast_notAgain, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        sharedPref.edit()
                                                .putBoolean("perm_notShow", false)
                                                .apply();
                                    }
                                })
                                .setPositiveButton(from.getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            from.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                })
                                .setNegativeButton(from.getString(R.string.toast_cancel), null)
                                .show();
                        return;
                    }
                    from.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                }
            }
        }
    }

    public static void grantPermissionsLoc(final Activity from) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (sharedPref.getBoolean ("perm_notShow", false)){
                int hasACCESS_FINE_LOCATION = from.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                    if (!from.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        new AlertDialog.Builder(from)
                                .setTitle(R.string.app_permissions_title)
                                .setMessage(helper_main.textSpannable(from.getString(R.string.app_permissions)))
                                .setNeutralButton(R.string.toast_notAgain, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        sharedPref.edit()
                                                .putBoolean("perm_notShow", false)
                                                .apply();
                                    }
                                })
                                .setPositiveButton(from.getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            from.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                    REQUEST_CODE_ASK_PERMISSIONS_1);
                                    }
                                })
                                .setNegativeButton(from.getString(R.string.toast_cancel), null)
                                .show();
                        return;
                    }
                    from.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_ASK_PERMISSIONS_1);
                }
            }
        }
    }

    public static void switchToActivity(Activity from, Class to, String Extra, boolean finishFromActivity) {
        Intent intent = new Intent(from, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("url", Extra);
        from.startActivity(intent);
        if (finishFromActivity) {
            from.finish();
        }
    }

    public static void hideKeyboard(Activity from, EditText editText) {
        InputMethodManager imm = (InputMethodManager)from.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        editText.clearFocus();
    }

    public static void showKeyboard(Activity from, EditText editText) {
        InputMethodManager imm = (InputMethodManager) from.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        editText.setSelection(editText.length());
    }

    public static void isOpened (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putBoolean("isOpened", false)
                .apply();
    }

    public static void isClosed (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putBoolean("isOpened", true)
                .apply();
    }

    public static void setOrientation (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        if (sharedPref.getString("orientation", "auto").equals("landscape")) {
            from.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if (sharedPref.getString("orientation", "auto").equals("portrait")) {
            from.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public static SpannableString textSpannable (String text) {
        SpannableString s;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }

    public static File newFile () {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_HH-mm-ss", Locale.getDefault());
        String filename = dateFormat.format(date) + ".jpg";
        return  new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + filename);
    }

    public static String newFileName () {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_HH-mm-ss", Locale.getDefault());
        return  dateFormat.format(date) + ".jpg";
    }

    public static void openFilePicker (final Activity activity, final View view, final String startDir) {

        new ChooserDialog().with(activity)
                .withStartFile(startDir)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(final File pathFile) {

                        final String fileExtension = pathFile.getAbsolutePath().substring(pathFile.getAbsolutePath().lastIndexOf("."));
                        final String fileName = pathFile.getAbsolutePath().substring(pathFile.getAbsolutePath().lastIndexOf("/")+1);
                        final String  fileNameWE = fileName.substring(0, fileName.lastIndexOf("."));

                        final CharSequence[] options = {
                                activity.getString(R.string.choose_menu_1),
                                activity.getString(R.string.choose_menu_2),
                                activity.getString(R.string.choose_menu_3),
                                activity.getString(R.string.choose_menu_4)};

                        final AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

                                dialog.setItems(options, new DialogInterface.OnClickListener() {
                                    @SuppressWarnings("ResultOfMethodCallIgnored")
                                    @Override
                                    public void onClick(DialogInterface dialog, int item) {
                                        if (options[item].equals(activity.getString(R.string.choose_menu_1))) {

                                            String text = (activity.getString(R.string.toast_extension) + ": " + fileExtension);

                                            switch (fileExtension) {
                                                case ".gif":
                                                case ".bmp":
                                                case ".tiff":
                                                case ".svg":
                                                case ".png":
                                                case ".jpg":
                                                case ".jpeg":
                                                    helper_main.openFile(activity, pathFile, "image/*", view);
                                                    break;
                                                case ".m3u8":
                                                case ".mp3":
                                                case ".wma":
                                                case ".midi":
                                                case ".wav":
                                                case ".aac":
                                                case ".aif":
                                                case ".amp3":
                                                case ".weba":
                                                    helper_main.openFile(activity, pathFile, "audio/*", view);
                                                    break;
                                                case ".mpeg":
                                                case ".mp4":
                                                case ".ogg":
                                                case ".webm":
                                                case ".qt":
                                                case ".3gp":
                                                case ".3g2":
                                                case ".avi":
                                                case ".f4v":
                                                case ".flv":
                                                case ".h261":
                                                case ".h263":
                                                case ".h264":
                                                case ".asf":
                                                case ".wmv":
                                                    helper_main.openFile(activity, pathFile, "video/*", view);
                                                    break;
                                                case ".rtx":
                                                case ".csv":
                                                case ".txt":
                                                case ".vcs":
                                                case ".vcf":
                                                case ".css":
                                                case ".ics":
                                                case ".conf":
                                                case ".config":
                                                case ".java":
                                                    helper_main.openFile(activity, pathFile, "text/*", view);
                                                    break;
                                                case ".html":
                                                    helper_main.openFile(activity, pathFile, "text/html", view);
                                                    break;
                                                case ".apk":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.android.package-archive", view);
                                                    break;
                                                case ".pdf":
                                                    helper_main.openFile(activity, pathFile, "application/pdf", view);
                                                    break;
                                                case ".doc":
                                                    helper_main.openFile(activity, pathFile, "application/msword", view);
                                                    break;
                                                case ".xls":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.ms-excel", view);
                                                    break;
                                                case ".ppt":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.ms-powerpoint", view);
                                                    break;
                                                case ".docx":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", view);
                                                    break;
                                                case ".pptx":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.presentationml.presentation", view);
                                                    break;
                                                case ".xlsx":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", view);
                                                    break;
                                                case ".odt":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.text", view);
                                                    break;
                                                case ".ods":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.spreadsheet", view);
                                                    break;
                                                case ".odp":
                                                    helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.presentation", view);
                                                    break;
                                                case ".zip":
                                                    helper_main.openFile(activity, pathFile, "application/zip", view);
                                                    break;
                                                case ".rar":
                                                    helper_main.openFile(activity, pathFile, "application/x-rar-compressed", view);
                                                    break;
                                                case ".epub":
                                                    helper_main.openFile(activity, pathFile, "application/epub+zip", view);
                                                    break;
                                                case ".cbz":
                                                    helper_main.openFile(activity, pathFile, "application/x-cbz", view);
                                                    break;
                                                case ".cbr":
                                                    helper_main.openFile(activity, pathFile, "application/x-cbr", view);
                                                    break;
                                                case ".fb2":
                                                    helper_main.openFile(activity, pathFile, "application/x-fb2", view);
                                                    break;
                                                case ".rtf":
                                                    helper_main.openFile(activity, pathFile, "application/rtf", view);
                                                    break;
                                                case ".opml":
                                                    helper_main.openFile(activity, pathFile, "application/opml", view);
                                                    break;

                                                default:
                                                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                                                    break;
                                            }

                                            String dir = pathFile.getParentFile().getAbsolutePath();
                                            helper_main.openFilePicker(activity, view, dir);
                                        }
                                        if (options[item].equals(activity.getString(R.string.choose_menu_2))) {

                                            if (pathFile.exists()) {
                                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                                sharingIntent.setType("image/png");
                                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                                                sharingIntent.putExtra(Intent.EXTRA_TEXT, fileName);
                                                Uri bmpUri = Uri.fromFile(pathFile);
                                                sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                                activity.startActivity(Intent.createChooser(sharingIntent, (activity.getString(R.string.app_share_file))));
                                            }
                                            String dir = pathFile.getParentFile().getAbsolutePath();
                                            helper_main.openFilePicker(activity, view, dir);
                                        }
                                        if (options[item].equals(activity.getString(R.string.choose_menu_4))) {
                                            final AlertDialog.Builder dialog2 = new AlertDialog.Builder(activity);

                                            dialog2.setMessage(activity.getString(R.string.choose_delete));
                                            dialog2.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    pathFile.delete();
                                                    new Handler().postDelayed(new Runnable() {
                                                        public void run() {
                                                            String dir = pathFile.getParentFile().getAbsolutePath();
                                                            helper_main.openFilePicker(activity, view, dir);
                                                        }
                                                    }, 500);
                                                }
                                            });
                                            dialog2.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    dialog.cancel();
                                                }
                                            });
                                            dialog2.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    // dialog dismiss without button press
                                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                                    helper_main.openFilePicker(activity, view, dir);
                                                }
                                            });
                                            dialog2.show();
                                        }
                                        if (options[item].equals(activity.getString(R.string.choose_menu_3))) {

                                            final LinearLayout layout = new LinearLayout(activity);
                                            layout.setOrientation(LinearLayout.VERTICAL);
                                            layout.setGravity(Gravity.CENTER_HORIZONTAL);
                                            final EditText input = new EditText(activity);
                                            input.setSingleLine(true);
                                            input.setHint(activity.getString(R.string.choose_hint));
                                            input.setText(fileNameWE);
                                            layout.setPadding(30, 0, 50, 0);
                                            layout.addView(input);

                                            new Handler().postDelayed(new Runnable() {
                                                public void run() {
                                                    helper_main.showKeyboard(activity,input);
                                                }
                                            }, 200);

                                            final AlertDialog.Builder dialog2 = new AlertDialog.Builder(activity);

                                                    dialog2.setView(layout);
                                                    dialog2.setMessage(activity.getString(R.string.choose_hint));
                                                    dialog2.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                                        public void onClick(DialogInterface dialog, int whichButton) {

                                                            String inputTag = input.getText().toString().trim();

                                                            File dir = pathFile.getParentFile();
                                                            File to = new File(dir,inputTag + fileExtension);

                                                            pathFile.renameTo(to);
                                                            pathFile.delete();

                                                            new Handler().postDelayed(new Runnable() {
                                                                public void run() {
                                                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                                                    helper_main.openFilePicker(activity, view, dir);
                                                                }
                                                            }, 500);
                                                        }
                                                    });
                                                    dialog2.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                        public void onClick(DialogInterface dialog, int whichButton) {
                                                            dialog.cancel();
                                                        }
                                                    });
                                            dialog2.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    // dialog dismiss without button press
                                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                                    helper_main.openFilePicker(activity, view, dir);
                                                }
                                            });
                                            dialog2.show();
                                        }
                                    }
                                });
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // dialog dismiss without button press
                                String dir = pathFile.getParentFile().getAbsolutePath();
                                helper_main.openFilePicker(activity, view, dir);
                            }
                        });
                        dialog.show();
                    }
                })
                .build()
                .show();
    }

    private static void openFile(Activity activity, File file, String string, View view) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(contentUri,string);

        } else {
            intent.setDataAndType(Uri.fromFile(file),string);
        }

        try {
            activity.startActivity (intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(view, R.string.toast_install_app, Snackbar.LENGTH_LONG).show();
        }
    }
}
