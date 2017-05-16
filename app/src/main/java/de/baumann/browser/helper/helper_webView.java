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
import android.util.Patterns;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URISyntaxException;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_History;
import de.baumann.browser.utils.Utils_AdClient;
import de.baumann.browser.utils.Utils_UserAgent;

import static android.content.ContentValues.TAG;
import static android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;

public class helper_webView {
    public static Utils_UserAgent mUtils_UserAgent= new Utils_UserAgent();

    public static String getTitle (WebView webview) {

        return  webview.getTitle().substring(0,1).toUpperCase() + webview.getTitle().substring(1).replace("'", "\\'");
    }


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
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        from.registerForContextMenu(webView);

        if (sharedPref.getString ("cookie", "1").equals("2") || sharedPref.getString ("cookie", "1").equals("3")){
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

            if (sharedPref.getString ("cookie", "1").equals("1") || sharedPref.getString ("cookie", "1").equals("3")){
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

    public static void webView_WebViewClient (final Activity from, final SwipeRefreshLayout swipeRefreshLayout,
                                              final WebView webView, final TextView urlBar) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);


        // crude if-else just to get the functionality in, feel free to make this more concise if you like
        if (sharedPref.getString("blockads_string", "").equals("Enabled")) {
        webView.setWebViewClient(new Utils_AdClient() {

            public void onPageFinished(WebView view, String url) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

                //request desktop desktop definition
                String desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

                //request desktop optimization
                //this compares the sharedPref setting to the current user agent and
                //corrects the user agent only if it is not the same as the shared pref
                //then zooms out to keep it neat if it is the desktop setting.
                if(sharedPref.getString("request_string", "").equals("Enabled")){
                    //sharedPref.edit().putString("request_string", getString(R.string.app_yes)).apply();
                    if (!mUtils_UserAgent.getUserAgent(view).equals(desktopUA)) {
                        mUtils_UserAgent.setUserAgent(view.getContext(), view, true, view.getUrl());
                    }
                }else{
                    if (mUtils_UserAgent.getUserAgent(view).equals(desktopUA)) {
                        mUtils_UserAgent.setUserAgent(view.getContext(), view, false, view.getUrl());
                    }
                }
                // request desktop check every time to make the page look neat
                if (sharedPref.getString("request_string" , "").equals("Enabled")){
                    if (mUtils_UserAgent.getUserAgent(view).equals(desktopUA));
                    view.zoomOut();
                }
                //end request desktop optimization

                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
                urlBar.setText(webView.getTitle());
                sharedPref.edit().putString("openURL", "").apply();

                if (webView.getTitle() != null && !webView.getTitle().equals("about:blank")  && !webView.getTitle().isEmpty()) {

                    DbAdapter_History db = new DbAdapter_History(from);
                    db.open();
                    db.deleteDouble(webView.getUrl());

                    if(db.isExist(helper_main.createDateSecond())){
                        Log.i(TAG, "Entry exists" + webView.getUrl());
                    }else{
                        if (helper_webView.getTitle (webView).contains("'")) {
                            String title = helper_webView.getTitle (webView).replace("'", "");
                            db.insert(title, webView.getUrl(), "", "", helper_main.createDateSecond());

                        } else {
                            db.insert(helper_webView.getTitle (webView), webView.getUrl(), "", "", helper_main.createDateSecond());
                        }
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
                            try {
                                from.startActivity(intent);
                            } catch (Exception e) {
                                Snackbar.make(webView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                            }

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
        else{
            webView.setWebViewClient(new WebViewClient() {

                public void onPageFinished(WebView view, String url) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

                    //access the constant desktop user agent defined in Utils_UserAgent
                    String desktopUA = Utils_UserAgent.DESKTOP_USER_AGENT;;

                    //request desktop optimization
                    //this compares the sharedPref setting to the current user agent and
                    //corrects the user agent only if it is not the same as the shared pref
                    //then zooms out to keep it neat if it is the desktop setting.
                    if(sharedPref.getString("request_string", "").equals("Enabled")){
                        //sharedPref.edit().putString("request_string", getString(R.string.app_yes)).apply();
                        if (!mUtils_UserAgent.getUserAgent(view).equals(desktopUA)) {
                            mUtils_UserAgent.setUserAgent(view.getContext(), view, true, view.getUrl());
                        }
                    }else{
                        if (mUtils_UserAgent.getUserAgent(view).equals(desktopUA)) {
                            mUtils_UserAgent.setUserAgent(view.getContext(), view, false, view.getUrl());
                        }
                    }
                    // request desktop check every time to make the page look neat
                    if (sharedPref.getString("request_string" , "").equals("Enabled")){
                        if (mUtils_UserAgent.getUserAgent(view).equals(desktopUA));
                        view.zoomOut();
                    }
                    //end request desktop optimization

                    super.onPageFinished(view, url);
                    swipeRefreshLayout.setRefreshing(false);
                    urlBar.setText(webView.getTitle());
                    sharedPref.edit().putString("openURL", "").apply();

                    if (webView.getTitle() != null && !webView.getTitle().equals("about:blank")  && !webView.getTitle().isEmpty()) {

                        DbAdapter_History db = new DbAdapter_History(from);
                        db.open();
                        db.deleteDouble(webView.getUrl());

                        if(db.isExist(helper_main.createDateSecond())){
                            Log.i(TAG, "Entry exists" + webView.getUrl());
                        }else{
                            if (helper_webView.getTitle (webView).contains("'")) {
                                String title = helper_webView.getTitle (webView).replace("'", "");
                                db.insert(title, webView.getUrl(), "", "", helper_main.createDateSecond());

                            } else {
                                db.insert(helper_webView.getTitle (webView), webView.getUrl(), "", "", helper_main.createDateSecond());
                            }
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
                                try {
                                    from.startActivity(intent);
                                } catch (Exception e) {
                                    Snackbar.make(webView, R.string.toast_error, Snackbar.LENGTH_SHORT).show();
                                }

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
            from.deleteDatabase("history_DB_v01.db");
            webView.clearHistory();
        }
        sharedPref.edit().putString("started", "").apply();
    }


    public static void openURL (Activity from, WebView mWebView, EditText editText) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        String text = editText.getText().toString();
        String searchEngine = sharedPref.getString("searchEngine", "https://duckduckgo.com/?q=");
        String wikiLang = sharedPref.getString("wikiLang", "en");

        if (text.startsWith("http")) {
            mWebView.loadUrl(text);
        } else if (text.startsWith("www.")) {
            mWebView.loadUrl("https://" + text);
        } else if (Patterns.WEB_URL.matcher(text).matches()) {
            mWebView.loadUrl("https://" + text);
        } else {

            String subStr = null;

            if (text.length() > 3) {
                subStr=text.substring(3);
            }

            if (text.contains(".w ")) {
                mWebView.loadUrl("https://" + wikiLang + ".wikipedia.org/wiki/Spezial:Suche?search=" + subStr);
            } else if (text.startsWith(".f ")) {
                mWebView.loadUrl("https://www.flickr.com/search/?advanced=1&license=2%2C3%2C4%2C5%2C6%2C9&text=" + subStr);
            } else  if (text.startsWith(".m ")) {
                mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + subStr);
            } else if (text.startsWith(".g ")) {
                mWebView.loadUrl("https://github.com/search?utf8=✓&q=" + subStr);
            } else  if (text.startsWith(".s ")) {
                if (Locale.getDefault().getLanguage().contentEquals("de")){
                    mWebView.loadUrl("https://startpage.com/do/search?query=" + subStr + "&lui=deutsch&l=deutsch");
                } else {
                    mWebView.loadUrl("https://startpage.com/do/search?query=" + subStr);
                }
            } else if (text.startsWith(".G ")) {
                if (Locale.getDefault().getLanguage().contentEquals("de")){
                    mWebView.loadUrl("https://www.google.de/search?&q=" + subStr);
                } else {
                    mWebView.loadUrl("https://www.google.com/search?&q=" + subStr);
                }
            } else  if (text.startsWith(".y ")) {
                if (Locale.getDefault().getLanguage().contentEquals("de")){
                    mWebView.loadUrl("https://www.youtube.com/results?hl=de&gl=DE&search_query=" + subStr);
                } else {
                    mWebView.loadUrl("https://www.youtube.com/results?search_query=" + subStr);
                }
            } else  if (text.startsWith(".d ")) {
                if (Locale.getDefault().getLanguage().contentEquals("de")){
                    mWebView.loadUrl("https://duckduckgo.com/?q=" + subStr + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web");
                } else {
                    mWebView.loadUrl("https://duckduckgo.com/?q=" + subStr);
                }
            } else {
                if (searchEngine.contains("https://duckduckgo.com/?q=")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        mWebView.loadUrl("https://duckduckgo.com/?q=" + text + "&kl=de-de&kad=de_DE&k1=-1&kaj=m&kam=osm&kp=-1&kak=-1&kd=1&t=h_&ia=web");
                    } else {
                        mWebView.loadUrl("https://duckduckgo.com/?q=" + text);
                    }
                } else if (searchEngine.contains("https://metager.de/meta/meta.ger3?focus=web&eingabe=")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text);
                    } else {
                        mWebView.loadUrl("https://metager.de/meta/meta.ger3?focus=web&eingabe=" + text +"&focus=web&encoding=utf8&lang=eng");
                    }
                } else if (searchEngine.contains("https://startpage.com/do/search?query=")) {
                    if (Locale.getDefault().getLanguage().contentEquals("de")){
                        mWebView.loadUrl("https://startpage.com/do/search?query=" + text + "&lui=deutsch&l=deutsch");
                    } else {
                        mWebView.loadUrl("https://startpage.com/do/search?query=" + text);
                    }
                }else {
                    mWebView.loadUrl(searchEngine + text);
                }
            }
        }
    }
}