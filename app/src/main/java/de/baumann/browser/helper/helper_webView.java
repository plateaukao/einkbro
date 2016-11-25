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
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import java.net.URISyntaxException;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_History;

import static android.content.ContentValues.TAG;
import static android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;

public class helper_webView {


    @SuppressLint("SetJavaScriptEnabled")
    public static void webView_Settings(final Activity from, final WebView webView) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        String fontSizeST = sharedPref.getString("font", "100");
        int fontSize = Integer.parseInt(fontSizeST);

        webView.getSettings().setAppCachePath(from.getApplicationContext().getCacheDir().getAbsolutePath());
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setMixedContentMode(MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setTextZoom(fontSize);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        from.registerForContextMenu(webView);

        if (sharedPref.getBoolean ("swipe", false)){
            helper_webView.webView_Touch(from, webView);
        }

        if (sharedPref.getBoolean ("cookie3", false)){
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView,true);
        } else {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView,false);
        }

        if (sharedPref.getString("started", "").equals("yes")) {
            if (sharedPref.getString("java_string", "True").equals(from.getString(R.string.app_yes))){
                webView.getSettings().setJavaScriptEnabled(true);
                sharedPref.edit().putString("java_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setJavaScriptEnabled(false);
                sharedPref.edit().putString("java_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getString("pictures_string", "True").equals(from.getString(R.string.app_yes))){
                webView.getSettings().setLoadsImagesAutomatically(true);
                sharedPref.edit().putString("pictures_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setLoadsImagesAutomatically(false);
                sharedPref.edit().putString("pictures_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getString("loc_string", "True").equals(from.getString(R.string.app_yes))){
                webView.getSettings().setGeolocationEnabled(true);
                helper_main.grantPermissionsLoc(from);
                sharedPref.edit().putString("loc_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setGeolocationEnabled(false);
                sharedPref.edit().putString("loc_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getString("cookie_string", "True").equals(from.getString(R.string.app_yes))){
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                sharedPref.edit().putString("cookie_string", from.getString(R.string.app_yes)).apply();
            } else {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(false);
                sharedPref.edit().putString("cookie_string", from.getString(R.string.app_no)).apply();
            }
        } else {
            if (sharedPref.getBoolean ("java", false)){
                webView.getSettings().setJavaScriptEnabled(true);
                sharedPref.edit().putString("java_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setJavaScriptEnabled(false);
                sharedPref.edit().putString("java_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getBoolean ("pictures", false)){
                webView.getSettings().setLoadsImagesAutomatically(true);
                sharedPref.edit().putString("pictures_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setLoadsImagesAutomatically(false);
                sharedPref.edit().putString("pictures_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getBoolean ("loc", false)){
                webView.getSettings().setGeolocationEnabled(true);
                helper_main.grantPermissionsLoc(from);
                sharedPref.edit().putString("loc_string", from.getString(R.string.app_yes)).apply();
            } else {
                webView.getSettings().setGeolocationEnabled(false);
                sharedPref.edit().putString("loc_string", from.getString(R.string.app_no)).apply();
            }

            if (sharedPref.getBoolean ("cookie", false)){
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                sharedPref.edit().putString("cookie_string", from.getString(R.string.app_yes)).apply();
            } else {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(false);
                sharedPref.edit().putString("cookie_string", from.getString(R.string.app_no)).apply();
            }
        }
    }


    private static void webView_Touch(final Activity from, final WebView webView) {

        webView.setOnTouchListener(new class_OnSwipeTouchListener_webview(from) {
            public void onSwipeRight() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    Snackbar.make(webView, R.string.toast_back, Snackbar.LENGTH_SHORT).show();
                }
            }
            public void onSwipeLeft() {
                if (webView.canGoForward()) {
                    webView.goForward();
                } else {
                    Snackbar.make(webView, R.string.toast_forward, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static void webView_WebViewClient (final Activity from, final SwipeRefreshLayout swipeRefreshLayout,
                                              final WebView webView, final EditText editText) {

        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
                editText.setText(webView.getTitle());
                if (webView.getTitle() != null && !webView.getTitle().equals("about:blank")) {
                    try {
                        final Database_History db = new Database_History(from);
                        db.addBookmark(webView.getTitle(), webView.getUrl());
                        db.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final Uri uri = Uri.parse(url);
                return handleUri(uri);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final Uri uri = request.getUrl();
                return handleUri(uri);
            }

            private boolean handleUri(final Uri uri) {

                Log.i(TAG, "Uri =" + uri);
                final String url = uri.toString();
                // Based on some condition you need to determine if you are going to load the url
                // in your web view itself or in a browser.
                // You can use `host` or `scheme` or any part of the `uri` to decide.

                if (url.startsWith("http")) return false;//open web links as usual
                //try to find browse activity to handle uri
                Uri parsedUri = Uri.parse(url);
                PackageManager packageManager = from.getPackageManager();
                Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
                if (browseIntent.resolveActivity(packageManager) != null) {
                    from.startActivity(browseIntent);
                    return true;
                }
                //if not activity found, try to parse intent://
                if (url.startsWith("intent:")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(from.getPackageManager()) != null) {
                            from.startActivity(intent);
                            return true;
                        }
                        //try to find fallback url
                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            webView.loadUrl(fallbackUrl);
                            return true;
                        }
                        //invite to install
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(
                                Uri.parse("market://details?id=" + intent.getPackage()));
                        if (marketIntent.resolveActivity(packageManager) != null) {
                            from.startActivity(marketIntent);
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        //not an intent uri
                    }
                }
                return true;//do nothing in other cases
            }

        });
    }

    public static void closeWebView (Activity from, WebView webView) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        if (sharedPref.getBoolean ("clearCookies", false)){
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        }

        if (sharedPref.getBoolean ("clearCache", false)){
            webView.clearCache(true);
        }

        if (sharedPref.getBoolean ("clearForm", false)){
            webView.clearFormData();
        }

        if (sharedPref.getBoolean ("history", false)){
            from.deleteDatabase("history.db");
            webView.clearHistory();
        }
        helper_main.isClosed(from);
        sharedPref.edit().putString("started", "").apply();
        from.finishAffinity();
    }
}