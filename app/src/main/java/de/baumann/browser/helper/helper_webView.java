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
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_History;
import de.baumann.browser.utils.Utils_AdBlocker;
import de.baumann.browser.utils.Utils_UserAgent;

import static android.content.ContentValues.TAG;
import static android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;

public class helper_webView {

    public static String getTitle (Activity activity, WebView webview) {
        String title;
        try {
            title = webview.getTitle().replace("'","");
        } catch (Exception e) {
            title = helper_webView.getDomain(activity, "");
            Log.e(TAG, "Unable to get title", e);
        }
        return title ;
    }

    public static String getDomain (Activity activity, String url) {
        String domain;
        try {
            if(Uri.parse(url).getHost().length() == 0) {
                domain = activity.getString(R.string.app_domain);
            } else if (Uri.parse(url).getHost().contains("startpage.de") || Uri.parse(url).getHost().contains("startpage.com")){
                domain = "startpage.com";
            } else {
                domain = Uri.parse(url).getHost().replace("www.", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to get domain", e);
            domain = activity.getString(R.string.app_domain);
        }
        return domain ;
    }


    @SuppressLint("SetJavaScriptEnabled")
    public static void webView_Settings(final Activity activity, final WebView webView) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String fontSizeST = sharedPref.getString("font", "100");
        int fontSize = Integer.parseInt(fontSizeST);

        webView.getSettings().setAppCachePath(activity.getApplicationContext().getCacheDir().getAbsolutePath());
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
        webView.requestDisallowInterceptTouchEvent(true);

        if (sharedPref.getString ("cookie", "2").equals("1")){
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView,true);
            sharedPref.edit().putString("cookie_string", activity.getString(R.string.app_yes)).apply();
        } else if (sharedPref.getString ("cookie", "2").equals("2")) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView,false);
            sharedPref.edit().putString("cookie_string", activity.getString(R.string.app_yes)).apply();
        } else if (sharedPref.getString ("cookie", "2").equals("3")) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(false);
            cookieManager.setAcceptThirdPartyCookies(webView,false);
            sharedPref.edit().putString("cookie_string", activity.getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("java", false)){
            webView.getSettings().setJavaScriptEnabled(true);
            sharedPref.edit().putString("java_string", activity.getString(R.string.app_yes)).apply();
        } else {
            webView.getSettings().setJavaScriptEnabled(false);
            sharedPref.edit().putString("java_string", activity.getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("pictures", false)){
            webView.getSettings().setLoadsImagesAutomatically(true);
            sharedPref.edit().putString("pictures_string", activity.getString(R.string.app_yes)).apply();
        } else {
            webView.getSettings().setLoadsImagesAutomatically(false);
            sharedPref.edit().putString("pictures_string", activity.getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("loc", false)){
            webView.getSettings().setGeolocationEnabled(true);
            helper_main.grantPermissionsLoc(activity);
            sharedPref.edit().putString("loc_string", activity.getString(R.string.app_yes)).apply();
        } else {
            webView.getSettings().setGeolocationEnabled(false);
            sharedPref.edit().putString("loc_string", activity.getString(R.string.app_no)).apply();
        }

        if (sharedPref.getBoolean ("blockads_bo", false)){
            sharedPref.edit().putString("blockads_string", activity.getString(R.string.app_yes)).apply();
        } else {
            sharedPref.edit().putString("blockads_string", activity.getString(R.string.app_no)).apply();
        }

        Utils_UserAgent myUserAgent= new Utils_UserAgent();

        if (sharedPref.getBoolean ("request_bo", false)){
            sharedPref.edit().putString("request_string", activity.getString(R.string.app_yes)).apply();
            myUserAgent.setUserAgent(activity, webView, true, webView.getUrl());
        } else {
            sharedPref.edit().putString("request_string", activity.getString(R.string.app_no)).apply();
            myUserAgent.setUserAgent(activity, webView, false, webView.getUrl());
        }
    }

    public static void webView_WebViewClient (final Activity activity, final WebView webView, final TextView urlBar) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String title = helper_webView.getTitle(activity, webView);
                sharedPref.edit().putString("openURL", "").apply();
                sharedPref.edit().putString("webView_url", webView.getUrl()).apply();

                DbAdapter_History db = new DbAdapter_History(activity);
                db.open();
                db.deleteDouble(webView.getUrl());

                if(db.isExist(helper_main.createDate_norm())){
                    Log.i(TAG, "Entry exists" + webView.getUrl());
                }else{
                    db.insert(helper_main.secString(title), helper_main.secString(webView.getOriginalUrl()), "", "", helper_main.createDate_norm());
                }
            }

            private final Map<String, Boolean> loadedUrls = new HashMap<>();
            // could simply place this section inside and if/else statement
            // inside the activities webview client.  but this way there is a class
            // available to call that is separate from the activity and easier for
            // others to incorporate into their activities as well.

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

                boolean ad;
                if (!loadedUrls.containsKey(url)) {
                    ad = Utils_AdBlocker.isAd(url);
                    loadedUrls.put(url, ad);
                } else {
                    ad = loadedUrls.get(url);
                }

                if (sharedPref.getString("blockads_string", "").equals(activity.getString(R.string.app_yes))) {
                    return ad ? Utils_AdBlocker.createEmptyResource() :
                            super.shouldInterceptRequest(view, url);
                } else {
                    return super.shouldInterceptRequest(view, url);
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

            @Override
            public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler, String host, String realm) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_login, null);

                final EditText pass_titleET = (EditText) dialogView.findViewById(R.id.pass_title);
                final EditText pass_userNameET = (EditText) dialogView.findViewById(R.id.pass_userName);
                final EditText pass_userPWET = (EditText) dialogView.findViewById(R.id.pass_userPW);
                pass_titleET.setVisibility(View.GONE);

                pass_userNameET.setText("");
                pass_userPWET.setText("");

                builder.setView(dialogView);
                builder.setTitle(R.string.pass_edit);
                builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        handler.proceed(pass_userNameET.getText().toString(), pass_userPWET.getText().toString());
                    }
                });
                builder.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                final AlertDialog dialog = builder.create();
                dialog.show();
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
                PackageManager packageManager = activity.getPackageManager();
                Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
                if (browseIntent.resolveActivity(packageManager) != null) {
                    activity.startActivity(browseIntent);
                    return true;
                }
                //if not activity found, try to parse intent://
                if (url.startsWith("intent:")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(activity.getPackageManager()) != null) {
                            try {
                                activity.startActivity(intent);
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
                            activity.startActivity(marketIntent);
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

    public static void openURL (Activity activity, WebView mWebView, EditText editText) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
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
            } else if (text.startsWith(".e ")) {
                mWebView.loadUrl("https://encrypted.google.com/search?q=" + subStr);
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