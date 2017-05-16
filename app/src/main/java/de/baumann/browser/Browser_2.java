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

package de.baumann.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.baumann.browser.databases.DbAdapter_ReadLater;
import de.baumann.browser.helper.Activity_settings;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_webView;
import de.baumann.browser.popups.Popup_files;
import de.baumann.browser.popups.Popup_history;
import de.baumann.browser.popups.Popup_pass;
import de.baumann.browser.utils.Utils_AdBlocker;
import de.baumann.browser.utils.Utils_UserAgent;

public class Browser_2 extends AppCompatActivity implements ObservableScrollViewCallbacks {

    private ObservableWebView mWebView;
    private ProgressBar progressBar;
    private ImageButton imageButton;
    private ImageButton imageButton_left;
    private ImageButton imageButton_right;
    private ActionBar actionBar;
    private Bitmap bitmap;
    private EditText editText;
    private TextView urlBar;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private SharedPreferences sharedPref;
    private File shareFile;
    private ValueCallback<Uri[]> mFilePathCallback;

    private String shareString;
    private String mCameraPhotoPath;
    private final String TAG = Browser_2.class.getSimpleName();

    private String domain;

    private static final int REQUEST_CODE_LOLLIPOP = 1;

    private void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }
    private boolean inCustomView() {
        return (mCustomView != null);
    }
    private boolean isNetworkUnAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }
    public final Utils_UserAgent myUserAgent= new Utils_UserAgent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(Browser_2.this, R.color.colorPrimaryEarth));

        WebView.enableSlowWholeDocumentDraw();
        setContentView(R.layout.activity_browser);
        helper_main.onStart(Browser_2.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("openURL", sharedPref.getString("startURL", "https://github.com/scoute-dich/browser/")).apply();
        sharedPref.edit().putInt("keyboard", 0).apply();
        sharedPref.getInt("keyboard", 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(Browser_2.this, R.color.colorPrimaryEarth));
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();

        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        editText = (EditText) findViewById(R.id.editText);
        editText.setVisibility(View.GONE);
        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();

        urlBar = (TextView) findViewById(R.id.urlBar);

        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton.setVisibility(View.INVISIBLE);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebView.scrollTo(0,0);
                imageButton.setVisibility(View.INVISIBLE);
                if (!actionBar.isShowing()) {
                    urlBar.setText(mWebView.getTitle());
                    actionBar.show();
                }
                helper_browser.setNavArrows(mWebView, imageButton_left, imageButton_right);
            }
        });

        SwipeRefreshLayout swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        assert swipeView != null;
        swipeView.setColorSchemeResources(R.color.colorTwo, R.color.colorAccent);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mWebView.reload();
            }
        });

        mWebView = (ObservableWebView) findViewById(R.id.webView);
        if (sharedPref.getString ("fullscreen", "2").equals("2") || sharedPref.getString ("fullscreen", "2").equals("3")){
            mWebView.setScrollViewCallbacks(this);
        }

        mWebChromeClient = new myWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);

        imageButton_left = (ImageButton) findViewById(R.id.imageButton_left);
        imageButton_right = (ImageButton) findViewById(R.id.imageButton_right);

        if (sharedPref.getBoolean ("arrow", false)){
            imageButton_left.setVisibility(View.VISIBLE);
            imageButton_right.setVisibility(View.VISIBLE);
        } else {
            imageButton_left.setVisibility(View.INVISIBLE);
            imageButton_right.setVisibility(View.INVISIBLE);
        }

        helper_webView.webView_Settings(Browser_2.this, mWebView);
        helper_webView.webView_WebViewClient(Browser_2.this, swipeView, mWebView, urlBar);

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        if (isNetworkUnAvailable()) {
            if (sharedPref.getBoolean ("offline", false)){
                if (isNetworkUnAvailable()) { // loading offline
                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    Snackbar.make(mWebView, R.string.toast_cache, Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(mWebView, R.string.toast_noInternet, Snackbar.LENGTH_SHORT).show();
            }
        }

        mWebView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(final String url, String userAgent,
                                        final String contentDisposition, final String mimetype,
                                        long contentLength) {

                registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                final String filename= URLUtil.guessFileName(url, contentDisposition, mimetype);
                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.toast_download_1) + " " + filename, Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.toast_download) + " " + filename , Snackbar.LENGTH_SHORT).show();
                            }
                        });
                snackbar.show();
            }
        });

        helper_browser.toolbar(Browser_2.this, Browser_3.class, mWebView, toolbar);
        helper_editText.editText_EditorAction(editText, Browser_2.this, mWebView, urlBar);
        helper_editText.editText_FocusChange(editText, Browser_2.this);
        helper_main.grantPermissionsStorage(Browser_2.this);

        //////////////////ad block
        Utils_AdBlocker.init(this);
        //////////////////ad block

        onNewIntent(getIntent());
    }

    protected void onNewIntent(final Intent intent) {

        String action = intent.getAction();

        if ("closeAPP".equals(action)) {
            finishAffinity();
        } else {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    mWebView.loadUrl(intent.getStringExtra("URL"));
                }
            }, 300);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri[] results = null;
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    private final BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Snackbar snackbar = Snackbar
                    .make(mWebView, getString(R.string.app_open), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            helper_main.switchToActivity(Browser_2.this, Popup_files.class, "", false);
                        }
                    });
            snackbar.show();
            unregisterReceiver(onComplete);
        }
    };

    private final BroadcastReceiver onComplete2 = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Uri myUri= Uri.fromFile(shareFile);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/*");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, myUri);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            Browser_2.this.startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_image))));
            unregisterReceiver(onComplete2);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final WebView.HitTestResult result = mWebView.getHitTestResult();
        final String url = result.getExtra();

        if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){

            final CharSequence[] options = {
                    getString(R.string.context_saveImage),
                    getString(R.string.context_shareImage),
                    getString(R.string.context_readLater),
                    getString(R.string.context_1),
                    getString(R.string.context_2),
                    getString(R.string.context_3),
                    getString(R.string.context_4),
                    getString(R.string.context_5)
            };
            new AlertDialog.Builder(Browser_2.this)
                    .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.context_saveImage))) {
                                if(url != null) {
                                    try {
                                        Uri source = Uri.parse(url);
                                        DownloadManager.Request request = new DownloadManager.Request(source);
                                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                        request.allowScanningByMediaScanner();
                                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, helper_main.newFileName());
                                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                        dm.enqueue(request);
                                        Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helper_main.newFileName() , Snackbar.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            if (options[item].equals(getString(R.string.context_shareImage))) {
                                if(url != null) {

                                    shareString = helper_main.newFileName();
                                    shareFile = helper_main.newFile(mWebView);

                                    try {
                                        Uri source = Uri.parse(url);
                                        DownloadManager.Request request = new DownloadManager.Request(source);
                                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                        request.allowScanningByMediaScanner();
                                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, shareString);
                                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                        dm.enqueue(request);

                                        Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helper_main.newFileName() , Snackbar.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                                    }
                                    registerReceiver(onComplete2, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                                }
                            }
                            if (options[item].equals(getString(R.string.context_readLater))) {
                                if (url != null) {

                                    if(Uri.parse(url).getHost().length() == 0) {
                                        domain = getString(R.string.app_domain);
                                    } else {
                                        domain = Uri.parse(url).getHost();
                                    }

                                    String domain2 = domain.substring(0,1).toUpperCase() + domain.substring(1);

                                    DbAdapter_ReadLater db = new DbAdapter_ReadLater(Browser_2.this);
                                    db.open();
                                    if(db.isExist(mWebView.getUrl())){
                                        Snackbar.make(editText, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                                    }else{
                                        db.insert(domain2, url, "", "", helper_main.createDate());
                                        Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            }
                            if (options[item].equals(getString(R.string.context_1))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_1.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_2))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_2.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_3))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_3.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_4))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_4.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_5))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_5.class, url, false);
                                }
                            }
                        }
                    }).show();

        } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {

            final CharSequence[] options = {
                    getString(R.string.menu_share_link_copy),
                    getString(R.string.menu_share_link),
                    getString(R.string.context_readLater),
                    getString(R.string.context_1),
                    getString(R.string.context_2),
                    getString(R.string.context_3),
                    getString(R.string.context_4),
                    getString(R.string.context_5)
            };
            new AlertDialog.Builder(Browser_2.this)
                    .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                if (url != null) {
                                    ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                                    Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            if (options[item].equals(getString(R.string.menu_share_link))) {
                                if (url != null) {
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                                    sendIntent.setType("text/plain");
                                    Browser_2.this.startActivity(Intent.createChooser(sendIntent, getResources()
                                            .getString(R.string.app_share_link)));
                                }
                            }
                            if (options[item].equals(getString(R.string.context_readLater))) {
                                if (url != null) {

                                    if(Uri.parse(url).getHost().length() == 0) {
                                        domain = getString(R.string.app_domain);
                                    } else {
                                        domain = Uri.parse(url).getHost();
                                    }

                                    String domain2 = domain.substring(0,1).toUpperCase() + domain.substring(1);

                                    DbAdapter_ReadLater db = new DbAdapter_ReadLater(Browser_2.this);
                                    db.open();
                                    if(db.isExist(mWebView.getUrl())){
                                        Snackbar.make(editText, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                                    }else{
                                        db.insert(domain2, url, "", "", helper_main.createDate());
                                        Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            }
                            if (options[item].equals(getString(R.string.context_1))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_1.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_2))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_2.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_3))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_3.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_4))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_4.class, url, false);
                                }
                            }
                            if (options[item].equals(getString(R.string.context_5))) {
                                if (url != null) {
                                    helper_main.switchToActivity(Browser_2.this, Browser_5.class, url, false);
                                }
                            }
                        }
                    }).show();
        }
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        if (scrollState == ScrollState.UP) {
            imageButton.setVisibility(View.VISIBLE);
            imageButton_left.setVisibility(View.INVISIBLE);
            imageButton_right.setVisibility(View.INVISIBLE);
            if (actionBar.isShowing()) {
                actionBar.hide();
            }
        } else if (scrollState == ScrollState.DOWN) {
            imageButton.setVisibility(View.INVISIBLE);
            urlBar.setText(mWebView.getTitle());
            if (!actionBar.isShowing()) {
                actionBar.show();
            }
            helper_browser.setNavArrows(mWebView, imageButton_left, imageButton_right);
        } else {
            imageButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void onBackPressed() {
        if (inCustomView()) {
            hideCustomView();
        } else if ((mCustomView == null) && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            Snackbar snackbar = Snackbar
                    .make(mWebView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            helper_main.closeApp(Browser_2.this, Browser_1.class, mWebView);
                        }
                    });
            snackbar.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        mWebView.onResume();
        final String URL = sharedPref.getString("openURL","https://github.com/scoute-dich/browser/");
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (URL.equals(mWebView.getUrl()) || URL.equals("")) {
                    Log.i(TAG, "Tab switched");
                } else if (URL.equals("copyLogin")) {
                    Snackbar snackbar = Snackbar
                            .make(mWebView, R.string.pass_copy_userName, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyUN", "")));

                                    Snackbar snackbar = Snackbar
                                            .make(mWebView, R.string.pass_copy_userPW, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyPW", "")));
                                                }
                                            });
                                    snackbar.show();
                                }
                            });
                    snackbar.show();
                } else if (URL.contains("openLogin")) {
                    mWebView.loadUrl(URL.replace("openLogin", ""));
                    Snackbar snackbar = Snackbar
                            .make(mWebView, R.string.pass_copy_userName, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyUN", "")));

                                    Snackbar snackbar = Snackbar
                                            .make(mWebView, R.string.pass_copy_userPW, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyPW", "")));
                                                }
                                            });
                                    snackbar.show();
                                }
                            });
                    snackbar.show();
                } else {
                    mWebView.loadUrl(URL);
                }
            }
        }, 100);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        helper_browser.prepareMenu(Browser_2.this, menu);
        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.showKeyboard(Browser_2.this, editText, 3, "", getString(R.string.app_search_hint));
        }

        if (id == R.id.action_history) {
            helper_main.switchToActivity(Browser_2.this, Popup_history.class, "", false);
        }

        if (id == R.id.action_search_chooseWebsite) {
            helper_editText.editText_searchWeb(editText, Browser_2.this);
        }

        if (id == R.id.action_pass) {
            helper_main.switchToActivity(Browser_2.this, Popup_pass.class, "", false);
            sharedPref.edit().putString("pass_copy_url", mWebView.getUrl()).apply();
            sharedPref.edit().putString("pass_copy_title", mWebView.getTitle()).apply();
        }

        if (id == R.id.action_toggle) {

            sharedPref.edit().putString("started", "yes").apply();

            if(Uri.parse(mWebView.getUrl()).getHost().length() == 0) {
                domain = getString(R.string.app_domain);
            } else {
                domain = Uri.parse(mWebView.getUrl()).getHost();
            }

            final String whiteList = sharedPref.getString("whiteList", "");

            AlertDialog.Builder builder = new AlertDialog.Builder(Browser_2.this);
            View dialogView = View.inflate(Browser_2.this, R.layout.dialog_toggle, null);

            Switch sw_java = (Switch) dialogView.findViewById(R.id.switch1);
            Switch sw_pictures = (Switch) dialogView.findViewById(R.id.switch2);
            Switch sw_location = (Switch) dialogView.findViewById(R.id.switch3);
            Switch sw_cookies = (Switch) dialogView.findViewById(R.id.switch4);
            Switch sw_blockads = (Switch) dialogView.findViewById(R.id.switch5);
            Switch sw_requestdesk = (Switch) dialogView.findViewById(R.id.switch6);
            final ImageButton whiteList_js = (ImageButton) dialogView.findViewById(R.id.imageButton_js);

            if (whiteList.contains(domain)) {
                whiteList_js.setImageResource(R.drawable.check_green);
            } else {
                whiteList_js.setImageResource(R.drawable.close_red);
            }
            if (sharedPref.getString("java_string", "True").equals(getString(R.string.app_yes))){
                sw_java.setChecked(true);
            } else {
                sw_java.setChecked(false);
            }
            if (sharedPref.getString("pictures_string", "True").equals(getString(R.string.app_yes))){
                sw_pictures.setChecked(true);
            } else {
                sw_pictures.setChecked(false);
            }
            if (sharedPref.getString("loc_string", "True").equals(getString(R.string.app_yes))){
                sw_location.setChecked(true);
            } else {
                sw_location.setChecked(false);
            }
            if (sharedPref.getString("cookie_string", "True").equals(getString(R.string.app_yes))){
                sw_cookies.setChecked(true);
            } else {
                sw_cookies.setChecked(false);
            }
            if (sharedPref.getString("request_string", "True").equals(getString(R.string.app_yes))){
                sw_requestdesk.setChecked(true);
            } else {
                sw_requestdesk.setChecked(false);
            }
            if (sharedPref.getString("blockads_string", "True").equals(getString(R.string.app_yes))){
                sw_blockads.setChecked(true);
            } else {
                sw_blockads.setChecked(false);
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

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked){
                        sharedPref.edit().putString("java_string", getString(R.string.app_yes)).apply();
                        mWebView.getSettings().setJavaScriptEnabled(true);
                    }else{
                        sharedPref.edit().putString("java_string", getString(R.string.app_no)).apply();
                        mWebView.getSettings().setJavaScriptEnabled(false);
                    }

                }
            });
            sw_pictures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked){
                        sharedPref.edit().putString("pictures_string", getString(R.string.app_yes)).apply();
                        mWebView.getSettings().setLoadsImagesAutomatically(true);
                    }else{
                        sharedPref.edit().putString("pictures_string", getString(R.string.app_no)).apply();
                        mWebView.getSettings().setLoadsImagesAutomatically(false);
                    }

                }
            });
            sw_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked){
                        sharedPref.edit().putString("loc_string", getString(R.string.app_yes)).apply();
                        mWebView.getSettings().setGeolocationEnabled(true);
                        helper_main.grantPermissionsLoc(Browser_2.this);
                    }else{
                        sharedPref.edit().putString("loc_string", getString(R.string.app_no)).apply();
                        mWebView.getSettings().setGeolocationEnabled(false);
                    }

                }
            });
            sw_cookies.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked){
                        sharedPref.edit().putString("cookie_string", getString(R.string.app_yes)).apply();
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.setAcceptCookie(true);
                    }else{
                        sharedPref.edit().putString("cookie_string", getString(R.string.app_no)).apply();
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.setAcceptCookie(false);
                    }

                }
            });
            sw_blockads.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    SwipeRefreshLayout swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);

                    if(isChecked){
                        //used commit() instead of apply because the new WVC depends on the sharedpref
                        //immediately being available, wouldnt want to miss the change in background process
                        //lag from using apply(), feel free to use apply if you prefer though.
                        sharedPref.edit().putString("blockads_string", getString(R.string.app_yes)).commit();
                        helper_webView.webView_WebViewClient(Browser_2.this, swipeView, mWebView, urlBar);
                    }else{
                        sharedPref.edit().putString("blockads_string", getString(R.string.app_no)).commit();
                        helper_webView.webView_WebViewClient(Browser_2.this, swipeView, mWebView, urlBar);
                    }
                }
            });
            sw_requestdesk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if(isChecked){
                        sharedPref.edit().putString("request_string", getString(R.string.app_yes)).apply();
                        myUserAgent.setUserAgent(Browser_2.this, mWebView, true, mWebView.getUrl());

                    }else{
                        sharedPref.edit().putString("request_string", getString(R.string.app_no)).apply();
                        myUserAgent.setUserAgent(Browser_2.this, mWebView, false, mWebView.getUrl());
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
                    sharedPref.edit().putString("pass_copy_url", mWebView.getUrl()).apply();
                    sharedPref.edit().putString("lastActivity", "browser_2").apply();
                    helper_main.switchToActivity(Browser_2.this, Activity_settings.class, "", true);
                }
            });

            final AlertDialog dialog = builder.create();
            // Display the custom alert dialog on interface
            dialog.show();

        }

        if (id == R.id.menu_save_screenshot) {
            screenshot();
        }

        if (id == R.id.menu_save_bookmark) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.editText_saveBookmark(editText, Browser_2.this, mWebView);
        }

        if (id == R.id.menu_save_readLater) {
            DbAdapter_ReadLater db = new DbAdapter_ReadLater(Browser_2.this);
            db.open();
            if(db.isExist(mWebView.getUrl())){
                Snackbar.make(editText, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
            }else{
                db.insert(helper_webView.getTitle (mWebView), mWebView.getUrl(), "", "", helper_main.createDate());
                Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
            }
        }

        if (id == R.id.menu_save_pass) {
            helper_editText.editText_savePass(Browser_2.this, mWebView, mWebView.getTitle(), mWebView.getUrl());
        }

        if (id == R.id.menu_createShortcut) {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setClassName(Browser_2.this, "de.baumann.browser.Browser_1");
            i.setData(Uri.parse(mWebView.getUrl()));

            Intent shortcut = new Intent();
            shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
            shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, mWebView.getTitle());
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(Browser_2.this.getApplicationContext(), R.mipmap.ic_launcher));
            shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            Browser_2.this.sendBroadcast(shortcut);
            Snackbar.make(mWebView, R.string.menu_createShortcut_success, Snackbar.LENGTH_SHORT).show();
        }

        if (id == R.id.menu_share_screenshot) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            Uri bmpUri = Uri.fromFile(shareFile);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_screenshot))));
        }

        if (id == R.id.menu_share_link) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
        }

        if (id == R.id.menu_share_link_copy) {
            String  url = mWebView.getUrl();
            ClipboardManager clipboard = (ClipboardManager) Browser_2.this.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
            Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
        }

        if (id == R.id.action_downloads) {
            helper_main.switchToActivity(Browser_2.this, Popup_files.class, "", false);
        }

        if (id == R.id.action_search_go) {

            String text = editText.getText().toString();
            helper_webView.openURL(Browser_2.this, mWebView, editText);
            helper_editText.hideKeyboard(Browser_2.this, editText, 0, text, getString(R.string.app_search_hint));
            helper_editText.editText_EditorAction(editText, Browser_2.this, mWebView, urlBar);
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        if (id == R.id.action_search_onSite) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.showKeyboard(Browser_2.this, editText, 1, "", getString(R.string.app_search_hint));
            helper_editText.editText_FocusChange_searchSite(editText, Browser_2.this);
            helper_editText.editText_searchSite(editText, Browser_2.this, mWebView, urlBar);
        }

        if (id == R.id.action_search_onSite_go) {

            String text = editText.getText().toString();

            if (text.startsWith(getString(R.string.app_search))) {
                helper_editText.editText_searchSite(editText, Browser_2.this, mWebView, urlBar);
            } else {
                mWebView.findAllAsync(text);
                helper_editText.hideKeyboard(Browser_2.this, editText, 1, getString(R.string.app_search) + " " + text, getString(R.string.app_search_hint_site));
            }
        }

        if (id == R.id.action_prev) {
            mWebView.findNext(false);
        }

        if (id == R.id.action_next) {
            mWebView.findNext(true);
        }

        if (id == R.id.action_cancel) {
            urlBar.setVisibility(View.VISIBLE);
            urlBar.setText(mWebView.getTitle());
            editText.setVisibility(View.GONE);
            helper_editText.editText_FocusChange(editText, Browser_2.this);
            helper_editText.editText_EditorAction(editText, Browser_2.this, mWebView, urlBar);
            helper_editText.hideKeyboard(Browser_2.this, editText, 0, mWebView.getTitle(), getString(R.string.app_search_hint));
        }

        if (id == R.id.action_save_bookmark) {
            helper_editText.editText_saveBookmark_save(editText, Browser_2.this, mWebView);
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        return super.onOptionsItemSelected(item);
    }

    private class myWebChromeClient extends WebChromeClient {

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
            Log.i(TAG, "onGeolocationPermissionsShowPrompt()");

            final boolean remember = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(Browser_2.this);
            builder.setTitle(R.string.app_location_title);
            builder.setMessage(R.string.app_location_message)
                    .setCancelable(true).setPositiveButton(R.string.app_location_allow, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // origin, allow, remember
                    callback.invoke(origin, true, remember);
                }
            }).setNegativeButton(R.string.app_location_allow_not, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // origin, allow, remember
                    callback.invoke(origin, false, remember);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        @SuppressLint("SetJavaScriptEnabled")
        public void onProgressChanged(WebView view, int progress) {


            progressBar.setProgress(progress);
            progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);

            String whiteList = sharedPref.getString("whiteList", "");

            if(Uri.parse(mWebView.getUrl()).getHost().length() == 0) {
                domain = getString(R.string.app_domain);
            } else {
                domain = Uri.parse(mWebView.getUrl()).getHost();
            }

            if (whiteList.contains(domain)) {
                mWebView.getSettings().setJavaScriptEnabled(true);
            } else {
                if (sharedPref.getString("started", "").equals("yes")) {
                    if (sharedPref.getString("java_string", "True").equals(Browser_2.this.getString(R.string.app_yes))){
                        mWebView.getSettings().setJavaScriptEnabled(true);
                    } else {
                        mWebView.getSettings().setJavaScriptEnabled(false);
                    }
                } else {
                    if (sharedPref.getBoolean ("java", false)){
                        mWebView.getSettings().setJavaScriptEnabled(true);
                    } else {
                        mWebView.getSettings().setJavaScriptEnabled(false);
                    }
                }
            }

            if (actionBar.isShowing()) {
                helper_browser.setNavArrows(mWebView, imageButton_left, imageButton_right);
            }
        }

        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = helper_browser.createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException e) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", e);
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_share_file));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, REQUEST_CODE_LOLLIPOP);

            return true;
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            mWebView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null)
                return;

            mWebView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }

    private void screenshot() {

        shareFile = helper_main.newFile(mWebView);

        try{
            mWebView.measure(View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWebView.layout(0, 0, mWebView.getMeasuredWidth(), mWebView.getMeasuredHeight());
            mWebView.setDrawingCacheEnabled(true);
            mWebView.buildDrawingCache();

            bitmap = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
                    mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int iHeight = bitmap.getHeight();
            canvas.drawBitmap(bitmap, 0, iHeight, paint);
            mWebView.draw(canvas);

        }catch (OutOfMemoryError e) {
            e.printStackTrace();
            Snackbar.make(mWebView, R.string.toast_screenshot_failed, Snackbar.LENGTH_SHORT).show();
        }

        if (bitmap != null) {
            try {
                OutputStream fOut;
                fOut = new FileOutputStream(shareFile);

                bitmap.compress(Bitmap.CompressFormat.PNG, 50, fOut);
                fOut.flush();
                fOut.close();
                bitmap.recycle();

                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.context_saveImage_toast) + " " + shareFile.getName() +
                                ". " + getString(R.string.app_open), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                helper_main.switchToActivity(Browser_2.this, Popup_files.class, "", false);
                            }
                        });
                snackbar.show();

                Uri uri = Uri.fromFile(shareFile);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                sendBroadcast(intent);

            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(mWebView, R.string.toast_perm, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
