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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.utils.Utils_UserAgent;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class helper_browser {

    public static void setupViews (final Activity activity, final class_CustomViewPager viewPager, final WebView webView,
                                   final EditText editText, final ImageButton imageButton_up,
                                   final ImageButton imageButton_down, final ImageButton imageButton_left,
                                   final ImageButton imageButton_right, final AppBarLayout appBarLayout,
                                   final HorizontalScrollView horizontalScrollView) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();

        imageButton_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.pageUp(true);
                imageButton_up.setVisibility(View.GONE);
                imageButton_down.setVisibility(View.GONE);
                appBarLayout.setVisibility(View.VISIBLE);
                helper_browser.setNavArrows(webView, imageButton_left, imageButton_right);
            }
        });

        imageButton_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.pageDown(true);
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                horizontalScrollView.setVisibility(View.GONE);
                return false;
            }
        });

        Toolbar toolbar =(Toolbar) activity.findViewById(R.id.toolbar);
        final TextView urlBar = (TextView) activity.findViewById(R.id.urlBar);

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewPager.getCurrentItem() < 5) {
                    horizontalScrollView.setVisibility(View.GONE);
                    urlBar.setVisibility(View.GONE);
                    editText.setVisibility(View.VISIBLE);
                    helper_editText.showKeyboard(activity, editText, 3, sharedPref.getString("webView_url", ""), activity.getString(R.string.app_search_hint));
                    editText.selectAll();
                } else {
                    Log.i("Browser", "Switched to list");
                }
            }
        });
    }


    public static void switcher (final Activity activity, final WebView mWebView, final TextView urlBar,
                                 final class_CustomViewPager viewPager) {

        final Utils_UserAgent myUserAgent= new Utils_UserAgent();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        sharedPref.edit().putString("started", "yes").apply();

        final String whiteList = sharedPref.getString("whiteList", "");
        final String domain = helper_webView.getDomain(activity, mWebView.getUrl());

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        View dialogView = View.inflate(activity, R.layout.dialog_toggle, null);

        Switch sw_java = (Switch) dialogView.findViewById(R.id.switch1);
        Switch sw_pictures = (Switch) dialogView.findViewById(R.id.switch2);
        Switch sw_location = (Switch) dialogView.findViewById(R.id.switch3);
        Switch sw_cookies = (Switch) dialogView.findViewById(R.id.switch4);
        Switch sw_blockads = (Switch) dialogView.findViewById(R.id.switch5);
        Switch sw_requestDesk = (Switch) dialogView.findViewById(R.id.switch6);
        Switch sw_swipe = (Switch) dialogView.findViewById(R.id.switch7);
        final ImageButton whiteList_js = (ImageButton) dialogView.findViewById(R.id.imageButton_js);

        if (whiteList.contains(domain)) {
            whiteList_js.setImageResource(R.drawable.check_green);
        } else {
            whiteList_js.setImageResource(R.drawable.close_red);
        }
        if (sharedPref.getString("java_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_java.setChecked(true);
        } else {
            sw_java.setChecked(false);
        }
        if (sharedPref.getString("pictures_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_pictures.setChecked(true);
        } else {
            sw_pictures.setChecked(false);
        }
        if (sharedPref.getString("loc_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_location.setChecked(true);
        } else {
            sw_location.setChecked(false);
        }
        if (sharedPref.getString("cookie_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_cookies.setChecked(true);
        } else {
            sw_cookies.setChecked(false);
        }
        if (sharedPref.getString("request_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_requestDesk.setChecked(true);
        } else {
            sw_requestDesk.setChecked(false);
        }
        if (sharedPref.getString("blockads_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_blockads.setChecked(true);
        } else {
            sw_blockads.setChecked(false);
        }
        if (sharedPref.getString("swipe_string", "True").equals(activity.getString(R.string.app_yes))){
            sw_swipe.setChecked(true);
        } else {
            sw_swipe.setChecked(false);
        }
        whiteList_js.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whiteList.contains(domain)) {
                    whiteList_js.setImageResource(R.drawable.close_red);
                    String removed = whiteList.replaceAll(domain, "");
                    sharedPref.edit().putString("whiteList", removed).apply();
                } else {
                    whiteList_js.setImageResource(R.drawable.check_green);
                    sharedPref.edit().putString("whiteList", whiteList + " " + domain).apply();
                }
            }
        });
        sw_java.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("java_string", activity.getString(R.string.app_yes)).apply();
                    mWebView.getSettings().setJavaScriptEnabled(true);
                }else{
                    sharedPref.edit().putString("java_string", activity.getString(R.string.app_no)).apply();
                    mWebView.getSettings().setJavaScriptEnabled(false);
                }

            }
        });
        sw_pictures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("pictures_string", activity.getString(R.string.app_yes)).apply();
                    mWebView.getSettings().setLoadsImagesAutomatically(true);
                }else{
                    sharedPref.edit().putString("pictures_string", activity.getString(R.string.app_no)).apply();
                    mWebView.getSettings().setLoadsImagesAutomatically(false);
                }

            }
        });
        sw_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("loc_string", activity.getString(R.string.app_yes)).apply();
                    mWebView.getSettings().setGeolocationEnabled(true);
                    helper_main.grantPermissionsLoc(activity);
                }else{
                    sharedPref.edit().putString("loc_string", activity.getString(R.string.app_no)).apply();
                    mWebView.getSettings().setGeolocationEnabled(false);
                }

            }
        });
        sw_cookies.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("cookie_string", activity.getString(R.string.app_yes)).apply();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.setAcceptCookie(true);
                }else{
                    sharedPref.edit().putString("cookie_string", activity.getString(R.string.app_no)).apply();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.setAcceptCookie(false);
                }

            }
        });
        sw_blockads.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    //used commit() instead of apply because the new WVC depends on the sharedPref
                    //immediately being available, would not want to miss the change in background process
                    //lag from using apply(), feel free to use apply if you prefer though.
                    sharedPref.edit().putString("blockads_string", activity.getString(R.string.app_yes)).commit();
                    helper_webView.webView_WebViewClient(activity, mWebView, urlBar);
                }else{
                    sharedPref.edit().putString("blockads_string", activity.getString(R.string.app_no)).commit();
                    helper_webView.webView_WebViewClient(activity, mWebView, urlBar);
                }
            }
        });
        sw_requestDesk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("request_string", activity.getString(R.string.app_yes)).apply();
                    myUserAgent.setUserAgent(activity, mWebView, true, mWebView.getUrl());

                }else{
                    sharedPref.edit().putString("request_string", activity.getString(R.string.app_no)).apply();
                    myUserAgent.setUserAgent(activity, mWebView, false, mWebView.getUrl());
                }
            }
        });
        sw_swipe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sharedPref.edit().putString("swipe_string", activity.getString(R.string.app_yes)).apply();
                    viewPager.setPagingEnabled(true);
                }else{
                    sharedPref.edit().putString("swipe_string", activity.getString(R.string.app_no)).apply();
                    viewPager.setPagingEnabled(false);
                }
            }
        });
        builder.setView(dialogView);
        builder.setTitle(R.string.menu_toggle_title);
        builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                mWebView.reload();
            }
        });
        builder.setNegativeButton(R.string.menu_settings, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                helper_main.switchToActivity(activity, Activity_settings.class);
                dialog.cancel();
            }
        });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }


    public static void prepareMenu(Activity activity, Menu menu) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        MenuItem saveBookmark = menu.findItem(R.id.action_save_bookmark);
        MenuItem search_go = menu.findItem(R.id.action_search_go);
        MenuItem search_onSite_go = menu.findItem(R.id.action_search_onSite_go);
        MenuItem search_chooseWebsite = menu.findItem(R.id.action_search_chooseWebsite);
        MenuItem history = menu.findItem(R.id.action_history);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem prev = menu.findItem(R.id.action_prev);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItem cancel = menu.findItem(R.id.action_cancel);
        MenuItem open = menu.findItem(R.id.action_open);
        MenuItem other = menu.findItem(R.id.action_other);
        MenuItem settings = menu.findItem(R.id.action_toggle);

        if (sharedPref.getInt("keyboard", 0) == 0) {
            saveBookmark.setVisible(false);
            search_onSite_go.setVisible(false);
            search_chooseWebsite.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(false);
            search_go.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            saveBookmark.setVisible(false);
            search_chooseWebsite.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            other.setVisible(false);
            open.setVisible(false);
            search_go.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            search_onSite_go.setVisible(false);
            search_chooseWebsite.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            other.setVisible(false);
            open.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            search_go.setVisible(false);
            settings.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 3) {
            saveBookmark.setVisible(false);
            search_onSite_go.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            other.setVisible(false);
            open.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            settings.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 4) {
            saveBookmark.setVisible(false);
            search_chooseWebsite.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            other.setVisible(false);
            open.setVisible(false);
            search_go.setVisible(false);
            settings.setVisible(false);
            search_onSite_go.setVisible(false);
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
            img_left.setVisibility(View.GONE);
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
            img_right.setVisibility(View.GONE);
        }
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

    public static String tab_1 (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_0", "");

        try {
            if (tab_string.isEmpty()) {
                s = activity.getString(R.string.context_tab);
            } else {
                s = tab_string;
            }
        } catch (Exception e) {
            Log.e("Browser", "Unable to get String", e);
            s = activity.getString(R.string.context_tab);
        }
        return s;
    }

    public static String tab_2 (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_1", "");

        try {
            if (tab_string.isEmpty()) {
                s = activity.getString(R.string.context_tab);
            } else {
                s = tab_string;
            }
        } catch (Exception e) {
            Log.e("Browser", "Unable to get String", e);
            s = activity.getString(R.string.context_tab);
        }
        return s;
    }

    public static String tab_3 (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_2", "");

        try {
            if (tab_string.isEmpty()) {
                s = activity.getString(R.string.context_tab);
            } else {
                s = tab_string;
            }
        } catch (Exception e) {
            Log.e("Browser", "Unable to get String", e);
            s = activity.getString(R.string.context_tab);
        }
        return s;
    }

    public static String tab_4 (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_3", "");

        try {
            if (tab_string.isEmpty()) {
                s = activity.getString(R.string.context_tab);
            } else {
                s = tab_string;
            }
        } catch (Exception e) {
            Log.e("Browser", "Unable to get String", e);
            s = activity.getString(R.string.context_tab);
        }
        return s;
    }

    public static String tab_5 (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String s;

        final String tab_string = sharedPref.getString("tab_4", "");

        try {
            if (tab_string.isEmpty()) {
                s = activity.getString(R.string.context_tab);
            } else {
                s = tab_string;
            }
        } catch (Exception e) {
            Log.e("Browser", "Unable to get String", e);
            s = activity.getString(R.string.context_tab);
        }
        return s;
    }

    public static void resetTabs (Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        File tab_1 = new File(activity.getFilesDir() + "/tab_0.jpg");
        tab_1.delete();
        File tab_2 = new File(activity.getFilesDir() + "/tab_1.jpg");
        tab_2.delete();
        File tab_3 = new File(activity.getFilesDir() + "/tab_2.jpg");
        tab_3.delete();
        File tab_4 = new File(activity.getFilesDir() + "/tab_3.jpg");
        tab_4.delete();
        File tab_5 = new File(activity.getFilesDir() + "/tab_4.jpg");
        tab_5.delete();

        sharedPref.edit().putString("tab_0", "").apply();
        sharedPref.edit().putString("tab_1", "").apply();
        sharedPref.edit().putString("tab_2", "").apply();
        sharedPref.edit().putString("tab_3", "").apply();
        sharedPref.edit().putString("tab_4", "").apply();
        sharedPref.edit().putInt("actualTab", 0).apply();
    }
}
