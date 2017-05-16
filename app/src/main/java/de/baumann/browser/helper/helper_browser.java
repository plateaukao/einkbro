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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.Browser_1;
import de.baumann.browser.Browser_2;
import de.baumann.browser.Browser_3;
import de.baumann.browser.Browser_4;
import de.baumann.browser.Browser_5;
import de.baumann.browser.R;
import de.baumann.browser.popups.Popup_bookmarks;
import de.baumann.browser.popups.Popup_readLater;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

public class helper_browser {


    public static void prepareMenu(Activity activity, Menu menu) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_search, false);

        MenuItem saveBookmark = menu.findItem(R.id.action_save_bookmark);
        MenuItem search = menu.findItem(R.id.action_search);
        MenuItem search_go = menu.findItem(R.id.action_search_go);
        MenuItem search_onSite_go = menu.findItem(R.id.action_search_onSite_go);
        MenuItem search_chooseWebsite = menu.findItem(R.id.action_search_chooseWebsite);
        MenuItem history = menu.findItem(R.id.action_history);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem search_onSite = menu.findItem(R.id.action_search_onSite);
        MenuItem downloads = menu.findItem(R.id.action_downloads);
        MenuItem settings = menu.findItem(R.id.action_settings);
        MenuItem prev = menu.findItem(R.id.action_prev);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItem cancel = menu.findItem(R.id.action_cancel);
        MenuItem pass = menu.findItem(R.id.action_pass);
        MenuItem toggle = menu.findItem(R.id.action_toggle);

        if (sharedPref.getInt("keyboard", 0) == 0) { //could be button state or..?
            saveBookmark.setVisible(false);
            search.setVisible(true);
            search_onSite_go.setVisible(false);
            search_chooseWebsite.setVisible(false);
            history.setVisible(true);
            save.setVisible(true);
            share.setVisible(true);
            search_onSite.setVisible(true);
            downloads.setVisible(true);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(false);
            pass.setVisible(true);
            toggle.setVisible(true);
            search_go.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            saveBookmark.setVisible(false);
            search.setVisible(false);
            search_onSite_go.setVisible(true);
            search_chooseWebsite.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            search_onSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(true);
            next.setVisible(true);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search_go.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            saveBookmark.setVisible(true);
            search.setVisible(false);
            search_onSite_go.setVisible(false);
            search_chooseWebsite.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            search_onSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search_go.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 3) {
            saveBookmark.setVisible(false);
            search.setVisible(false);
            search_onSite_go.setVisible(false);
            search_chooseWebsite.setVisible(true);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            search_onSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            toggle.setVisible(false);
            search_go.setVisible(true);
        }
    }

    public static void setNavArrows(final WebView webview, ImageButton img_left, ImageButton img_right) {

        if (webview.canGoBack()) {
            img_left.setVisibility(View.VISIBLE);
            img_left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    webview.goBack();
                }
            });
        } else {
            img_left.setVisibility(View.INVISIBLE);
        }

        if (webview.canGoForward()) {
            img_right.setVisibility(View.VISIBLE);
            img_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    webview.goForward();
                }
            });
        } else {
            img_right.setVisibility(View.INVISIBLE);
        }
    }

    public static void toolbar (final Activity activity, final WebView webview, Toolbar toolbar) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        toolbar.setOnTouchListener(new class_OnSwipeTouchListener_editText(activity) {
            public void onSwipeTop() {
                helper_main.closeApp(activity, webview);
            }
            public void onSwipeRight() {
                helper_main.switchToActivity(activity, Popup_readLater.class, "", false);
            }
            public void onSwipeLeft() {
                helper_main.switchToActivity(activity, Popup_bookmarks.class, "", false);
            }
        });
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CharSequence[] options = {
                        helper_browser.tab_1(activity, activity.getString(R.string.context_open)),
                        helper_browser.tab_2(activity, activity.getString(R.string.context_open)),
                        helper_browser.tab_3(activity, activity.getString(R.string.context_open)),
                        helper_browser.tab_4(activity, activity.getString(R.string.context_open)),
                        helper_browser.tab_5(activity, activity.getString(R.string.context_open))
                };
                new AlertDialog.Builder(activity)
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                if (options[item].equals(helper_browser.tab_1(activity, activity.getString(R.string.context_open)))) {
                                    sharedPref.edit().putString("openURL", "").apply();
                                    Intent intent = new Intent(activity, Browser_1.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    activity.startActivity(intent);
                                }
                                if (options[item].equals(helper_browser.tab_2(activity, activity.getString(R.string.context_open)))) {
                                    sharedPref.edit().putString("openURL", "").apply();
                                    Intent intent = new Intent(activity, Browser_2.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    activity.startActivity(intent);
                                }
                                if (options[item].equals(helper_browser.tab_3(activity, activity.getString(R.string.context_open)))) {
                                    sharedPref.edit().putString("openURL", "").apply();
                                    Intent intent = new Intent(activity, Browser_3.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    activity.startActivity(intent);
                                }
                                if (options[item].equals(helper_browser.tab_4(activity, activity.getString(R.string.context_open)))) {
                                    sharedPref.edit().putString("openURL", "").apply();
                                    Intent intent = new Intent(activity, Browser_4.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    activity.startActivity(intent);
                                }
                                if (options[item].equals(helper_browser.tab_5(activity, activity.getString(R.string.context_open)))) {
                                    sharedPref.edit().putString("openURL", "").apply();
                                    Intent intent = new Intent(activity, Browser_5.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    activity.startActivity(intent);
                                }
                            }
                        }).show();
            }
        });
    }

    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yy-MM-dd_HH-mm", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    public static String tab_1 (Activity activity, String string) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_1", "");

        try {
            if (tab_string.isEmpty()) {
                s = "(1) " + string + ": \"" + activity.getString(R.string.context_1);
            } else if (tab_string.length() > 16){
                s = "(1) " + string + ": \"" + tab_string.substring(0,20) + " ..." + "\"";
            } else {
                s = "(1) " + string + ": \"" + tab_string + "\"";
            }
        } catch (Exception e) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to get String", e);
            s = "(1) " + string + ": \"" + activity.getString(R.string.context_1);
        }
        return s;
    }

    public static String tab_2 (Activity activity, String string) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_2", "");

        try {
            if (tab_string.isEmpty()) {
                s = "(2) " + string + ": \"" + activity.getString(R.string.context_2);
            } else if (tab_string.length() > 16){
                s = "(2) " + string + ": \"" + tab_string.substring(0,20) + " ..." + "\"";
            } else {
                s = "(2) " + string + ": \"" + tab_string + "\"";
            }
        } catch (Exception e) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to get String", e);
            s = "(2) " + string + ": \"" + activity.getString(R.string.context_2);
        }

        return s;
    }

    public static String tab_3 (Activity activity, String string) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_3", "");

        try {
            if (tab_string.isEmpty()) {
                s = "(3) " + string + ": \"" + activity.getString(R.string.context_3);
            } else if (tab_string.length() > 16){
                s = "(3) " + string + ": \"" + tab_string.substring(0,20) + " ..." + "\"";
            } else {
                s = "(3) " + string + ": \"" + tab_string + "\"";
            }
        } catch (Exception e) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to get String", e);
            s = "(3) " + string + ": \"" + activity.getString(R.string.context_3);
        }
        return s;
    }

    public static String tab_4 (Activity activity, String string) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_4", "");

        try {
            if (tab_string.isEmpty()) {
                s = "(4) " + string + ": \"" + activity.getString(R.string.context_4);
            } else if (tab_string.length() > 16){
                s = "(4) " + string + ": \"" + tab_string.substring(0,20) + " ..." + "\"";
            } else {
                s = "(4) " + string + ": \"" + tab_string + "\"";
            }
        } catch (Exception e) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to get String", e);
            s = "(4) " + string + ": \"" + activity.getString(R.string.context_4);
        }
        return s;
    }

    public static String tab_5 (Activity activity, String string) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_5", "");

        try {
            if (tab_string.isEmpty()) {
                s = "(5) " + string + ": \"" + activity.getString(R.string.context_5);
            } else if (tab_string.length() > 16){
                s = "(5) " + string + ": \"" + tab_string.substring(0,20) + " ..." + "\"";
            } else {
                s = "(5) " + string + ": \"" + tab_string + "\"";
            }
        } catch (Exception e) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to get String", e);
            s = "(5) " + string + ": \"" + activity.getString(R.string.context_5);
        }
        return s;
    }
}
