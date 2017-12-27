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

package de.baumann.browser.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;

import java.io.File;

import de.baumann.browser.Ninja.R;

public class helper_main {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;

    public static void grantPermissionsStorage(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.toast_permission_title)
                            .setMessage(R.string.toast_permission_sdCard)
                            .setPositiveButton(activity.getString(R.string.app_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (android.os.Build.VERSION.SDK_INT >= 23)
                                        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.app_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
            }
        }
    }

    public static void grantPermissionsLoc(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            int hasACCESS_FINE_LOCATION = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.toast_permission_title)
                            .setMessage(R.string.toast_permission_loc)
                            .setPositiveButton(activity.getString(R.string.app_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (android.os.Build.VERSION.SDK_INT >= 23)
                                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                REQUEST_CODE_ASK_PERMISSIONS_1);
                                }
                            })
                            .setNegativeButton(activity.getString(R.string.app_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
            }
        }
    }

    public static void setTheme (Context activity) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String theme = sharedPref.getString("theme", "0");

        switch (theme) {
            case "0":
                activity.setTheme(R.style.AppTheme);
                break;
            case "1":
                activity.setTheme(R.style.AppTheme_blue);
                break;
            case "2":
                activity.setTheme(R.style.AppTheme_pink);
                break;
            case "3":
                activity.setTheme(R.style.AppTheme_purple);
                break;
            case "4":
                activity.setTheme(R.style.AppTheme_teal);
                break;
            case "5":
                activity.setTheme(R.style.AppTheme_red);
                break;
            case "6":
                activity.setTheme(R.style.AppTheme_orange);
                break;
            case "7":
                activity.setTheme(R.style.AppTheme_brown);
                break;
            case "8":
                activity.setTheme(R.style.AppTheme_grey);
                break;
            case "9":
                activity.setTheme(R.style.AppTheme_darkGrey);
                break;
        }
    }

    public static int colorAccent (Context context) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("files_startFolder", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()).apply();

        String theme = sp.getString("theme", "0");

        switch (theme) {
            case "5":case "6":case "8":
                return R.color.colorAccent_grey;
            case "7":
                return R.color.colorAccent_brown;
            case "9":
                return R.color.light;
            default:
                return R.color.colorAccent;
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

    public static String secString (String string) {
        if(TextUtils.isEmpty(string)){
            return "";
        }else {
            return  string.replaceAll("'", "\'\'");
        }
    }

    public static void open (String extension, Activity activity, File pathFile, View view) {
        File file = new File(pathFile.getAbsolutePath());
        String fileExtension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
        String text = "Unknown" + ": " + fileExtension;
        switch (extension) {
            case ".gif":case ".bmp":case ".tiff":case ".svg":case ".png":case ".jpg":case ".JPG":case ".jpeg":
                helper_main.openFile(activity, pathFile, "image/*");
                break;
            case ".m3u8":case ".mp3":case ".wma":case ".midi":case ".wav":case ".aac":case ".aif":case ".amp3":case ".weba":
                helper_main.openFile(activity, pathFile, "audio/*");
                break;
            case ".mpeg":case ".mp4":case ".ogg":case ".webm":case ".qt":case ".3gp":case ".3g2":case ".avi":case ".f4v":
            case ".flv":case ".h261":case ".h263":case ".h264":case ".asf":case ".wmv":
                helper_main.openFile(activity, pathFile, "video/*");
                break;
            case ".rtx":case ".csv":case ".txt":case ".vcs":case ".vcf":case ".css":case ".ics":case ".conf":case ".config":case ".java":
                helper_main.openFile(activity, pathFile, "text/*");
                break;
            case ".html":
                helper_main.openFile(activity, pathFile, "text/html");
                break;
            case ".apk":
                helper_main.openFile(activity, pathFile, "application/vnd.file_android.package-archive");
                break;
            case ".pdf":
                helper_main.openFile(activity, pathFile, "application/pdf");
                break;
            case ".doc":
                helper_main.openFile(activity, pathFile, "application/msword");
                break;
            case ".xls":
                helper_main.openFile(activity, pathFile, "application/vnd.ms-excel");
                break;
            case ".ppt":
                helper_main.openFile(activity, pathFile, "application/vnd.ms-powerpoint");
                break;
            case ".docx":
                helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                break;
            case ".pptx":
                helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
                break;
            case ".xlsx":
                helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case ".odt":
                helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.text");
                break;
            case ".ods":
                helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.spreadsheet");
                break;
            case ".odp":
                helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.presentation");
                break;
            case ".zip":
                helper_main.openFile(activity, pathFile, "application/zip");
                break;
            case ".rar":
                helper_main.openFile(activity, pathFile, "application/x-rar-compressed");
                break;
            case ".epub":
                helper_main.openFile(activity, pathFile, "application/epub+zip");
                break;
            case ".cbz":
                helper_main.openFile(activity, pathFile, "application/x-cbz");
                break;
            case ".cbr":
                helper_main.openFile(activity, pathFile, "application/x-cbr");
                break;
            case ".fb2":
                helper_main.openFile(activity, pathFile, "application/x-fb2");
                break;
            case ".rtf":
                helper_main.openFile(activity, pathFile, "application/rtf");
                break;
            case ".opml":
                helper_main.openFile(activity, pathFile, "application/opml");
                break;

            default:
                Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
                snackbar.show();
                break;
        }
    }

    private static void openFile(Activity activity, File file, String string) {

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
            Log.w("Browser", "No activity found");
        }
    }
}