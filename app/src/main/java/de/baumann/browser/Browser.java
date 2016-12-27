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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.baumann.browser.databases.Database_ReadLater;
import de.baumann.browser.helper.Activity_settings;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_webView;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.popups.Popup_history;
import de.baumann.browser.popups.Popup_pass;

public class Browser extends AppCompatActivity implements ObservableScrollViewCallbacks {

    private ObservableWebView mWebView;
    private ProgressBar progressBar;
    private ImageButton imageButton;
    private ImageButton imageButton_left;
    private ImageButton imageButton_right;
    private ActionBar actionBar;
    private Bitmap bitmap;
    private EditText editText;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private SharedPreferences sharedPref;
    private File shareFile;
    private ValueCallback<Uri[]> mFilePathCallback;

    private String progressString;
    private String shareString;
    private String mCameraPhotoPath;
    private String subStr;
    private final String TAG = Browser.class.getSimpleName();

    private String linkIntent2;
    private String linkIntent3;
    private String domain;

    private static final int ID_SAVE_IMAGE = 10;
    private static final int ID_READ_LATER = 11;
    private static final int ID_COPY_LINK = 12;
    private static final int ID_SHARE_LINK = 13;
    private static final int ID_SHARE_IMAGE = 14;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.enableSlowWholeDocumentDraw();
        setContentView(R.layout.activity_browser);
        helper_main.onStart(Browser.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putInt("keyboard", 0).apply();
        sharedPref.getInt("keyboard", 0);

        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        editText = (EditText) findViewById(R.id.editText);
        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();

        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton.setVisibility(View.INVISIBLE);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebView.scrollTo(0,0);
                imageButton.setVisibility(View.INVISIBLE);
                actionBar = getSupportActionBar();
                assert actionBar != null;
                if (!actionBar.isShowing()) {
                    editText.setText(mWebView.getTitle());
                    actionBar.show();
                }
                setNavArrows();
            }
        });

        SwipeRefreshLayout swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        assert swipeView != null;
        swipeView.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
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

        helper_webView.webView_Settings(Browser.this, mWebView);
        helper_webView.webView_WebViewClient(Browser.this, swipeView, mWebView, editText);

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

        helper_editText.editText_Touch(editText, Browser.this, mWebView);
        helper_editText.editText_EditorAction(editText, Browser.this, mWebView);
        helper_editText.editText_FocusChange(editText, Browser.this);

        onNewIntent(getIntent());
        helper_main.grantPermissionsStorage(Browser.this);
    }

    protected void onNewIntent(final Intent intent) {

        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String searchEngine = sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=");
            mWebView.loadUrl(searchEngine + sharedText);
        } else if ("pass".equals(action)) {
            mWebView.loadUrl(intent.getStringExtra("url"));
            setTitle(intent.getStringExtra("title"));
            Snackbar snackbar = Snackbar
                    .make(mWebView, R.string.pass_copy_userName, Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ClipboardManager clipboard = (ClipboardManager) Browser.this.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("userName", intent.getStringExtra("userName")));

                            Snackbar snackbar = Snackbar
                                    .make(mWebView, R.string.pass_copy_userPW, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ClipboardManager clipboard = (ClipboardManager) Browser.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboard.setPrimaryClip(ClipData.newPlainText("userName", intent.getStringExtra("userPW")));
                                        }
                                    });
                            snackbar.show();
                        }
                    });
            snackbar.show();
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            String link = data.toString();
            mWebView.loadUrl(link);
        } else {
            mWebView.loadUrl(intent.getStringExtra("url"));
        }
    }

    private File createImageFile() throws IOException {
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
                    .make(mWebView, getString(R.string.toast_download_2), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
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
            Browser.this.startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_image))));
            unregisterReceiver(onComplete2);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final WebView.HitTestResult result = mWebView.getHitTestResult();

        final MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                final String url = result.getExtra();

                switch (item.getItemId()) {
                    //Save image to external memory
                    case ID_SAVE_IMAGE: {
                        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                        try {
                            if (url != null) {

                                Uri source = Uri.parse(url);
                                DownloadManager.Request request = new DownloadManager.Request(source);
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, helper_main.newFileName());
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.context_saveImage_toast) + " " + helper_main.newFileName() , Snackbar.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    break;

                    case ID_SHARE_IMAGE:
                        if(url != null) {

                            shareString = helper_main.newFileName();
                            shareFile = helper_main.newFile();

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
                        break;

                    case ID_READ_LATER:
                        if (url != null) {

                            if (url.contains("https://")) {
                                linkIntent2 = url.replace("https://", "|");
                            } else if (url.contains("http://")){
                                linkIntent2 = url.replace("http://", "|");
                            }

                            if (linkIntent2.contains("www.")) {
                                linkIntent3 = linkIntent2.replace("www.", "").toUpperCase();
                            } else {
                                linkIntent3 = linkIntent2.toUpperCase();
                            }

                            if (linkIntent3.contains("/")) {
                                domain = linkIntent3.substring(linkIntent3.indexOf('|')+1, linkIntent3.indexOf('/'));
                            } else {
                                domain = linkIntent3.substring(linkIntent3.indexOf('|')+1, linkIntent3.lastIndexOf('.'));
                            }

                            String domain2 = domain.substring(0,1).toUpperCase() + domain.substring(1).toLowerCase();


                            try {
                                final Database_ReadLater db = new Database_ReadLater(Browser.this);
                                db.addBookmark(domain2, url);
                                db.close();
                                Snackbar.make(mWebView, R.string.readLater_added, Snackbar.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    case ID_COPY_LINK:
                        if (url != null) {
                            ClipboardManager clipboard = (ClipboardManager) Browser.this.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                            Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                        }
                        break;

                    case ID_SHARE_LINK:
                        if (url != null) {
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                            sendIntent.setType("text/plain");
                            Browser.this.startActivity(Intent.createChooser(sendIntent, getResources()
                                    .getString(R.string.app_share_link)));
                        }
                        break;
                }
                return true;
            }
        };

        if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){
            menu.add(0, ID_SAVE_IMAGE, 0, getString(R.string.context_saveImage)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_SHARE_IMAGE, 0, getString(R.string.context_shareImage)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_READ_LATER, 0, getString(R.string.context_readLater)).setOnMenuItemClickListener(handler);
        } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            menu.add(0, ID_COPY_LINK, 0, getString(R.string.menu_share_link_copy)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_SHARE_LINK, 0, getString(R.string.menu_share_link)).setOnMenuItemClickListener(handler);
            menu.add(0, ID_READ_LATER, 0, getString(R.string.context_readLater)).setOnMenuItemClickListener(handler);
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
        actionBar = getSupportActionBar();
        assert actionBar != null;
        if (scrollState == ScrollState.UP) {
            if (progressString.equals("loaded")) {
                imageButton.setVisibility(View.VISIBLE);
                imageButton_left.setVisibility(View.INVISIBLE);
                imageButton_right.setVisibility(View.INVISIBLE);
                if (actionBar.isShowing()) {
                    actionBar.hide();
                }
            }
        } else if (scrollState == ScrollState.DOWN) {
            if (progressString.equals("loaded")) {
                imageButton.setVisibility(View.INVISIBLE);
                editText.setText(mWebView.getTitle());
                if (!actionBar.isShowing()) {
                    actionBar.show();
                }
                setNavArrows();
            }
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
                            helper_webView.closeWebView(Browser.this, mWebView);
                        }
                    });
            snackbar.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Browser.this);
        mWebView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        mWebView.onResume();
        helper_main.isOpened(Browser.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Browser.this);
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem saveBookmark = menu.findItem(R.id.action_save_bookmark);
        MenuItem search = menu.findItem(R.id.action_search);
        MenuItem search2 = menu.findItem(R.id.action_search2);
        MenuItem search3 = menu.findItem(R.id.action_search3);
        MenuItem history = menu.findItem(R.id.action_history);
        MenuItem save = menu.findItem(R.id.action_save);
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem searchSite = menu.findItem(R.id.action_searchSite);
        MenuItem downloads = menu.findItem(R.id.action_downloads);
        MenuItem settings = menu.findItem(R.id.action_settings);
        MenuItem prev = menu.findItem(R.id.action_prev);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItem cancel = menu.findItem(R.id.action_cancel);
        MenuItem pass = menu.findItem(R.id.action_pass);
        MenuItem help = menu.findItem(R.id.action_help);
        MenuItem toggle = menu.findItem(R.id.action_toggle);

        if (sharedPref.getInt("keyboard", 0) == 0) { //could be button state or..?
            saveBookmark.setVisible(false);
            search.setVisible(true);
            search2.setVisible(false);
            search3.setVisible(false);
            history.setVisible(true);
            save.setVisible(true);
            share.setVisible(true);
            searchSite.setVisible(true);
            downloads.setVisible(true);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(false);
            pass.setVisible(true);
            help.setVisible(false);
            toggle.setVisible(true);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            saveBookmark.setVisible(false);
            search.setVisible(false);
            search2.setVisible(true);
            search3.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(true);
            next.setVisible(true);
            cancel.setVisible(true);
            pass.setVisible(false);
            help.setVisible(false);
            toggle.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            saveBookmark.setVisible(true);
            search.setVisible(false);
            search2.setVisible(false);
            search3.setVisible(false);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            help.setVisible(false);
            toggle.setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 3) {
            saveBookmark.setVisible(false);
            search.setVisible(true);
            search2.setVisible(false);
            search3.setVisible(true);
            history.setVisible(false);
            save.setVisible(false);
            share.setVisible(false);
            searchSite.setVisible(false);
            downloads.setVisible(false);
            settings.setVisible(false);
            prev.setVisible(false);
            next.setVisible(false);
            cancel.setVisible(true);
            pass.setVisible(false);
            help.setVisible(false);
            toggle.setVisible(false);
        }
        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_browser, menu);
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

            mWebView.stopLoading();
            String text = editText.getText().toString();
            String searchEngine = sharedPref.getString("searchEngine", "https://startpage.com/do/search?query=");
            String wikiLang = sharedPref.getString("wikiLang", "en");

            if (text.length() > 3) {
                subStr=text.substring(3);
            }

            if (text.equals(mWebView.getTitle()) || text.isEmpty()) {
                helper_editText.showKeyboard(Browser.this, editText, 3, "", getString(R.string.app_search_hint));

            } else {
                helper_editText.hideKeyboard(Browser.this, editText, 0, text, getString(R.string.app_search_hint));
                helper_editText.editText_EditorAction(editText, Browser.this, mWebView);

                if (text.startsWith("www")) {
                    mWebView.loadUrl("http://" + text);
                } else if (text.contains("http")) {
                    mWebView.loadUrl(text);
                } else if (text.contains(".w ")) {
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

        if (id == R.id.action_history) {
            helper_main.switchToActivity(Browser.this, Popup_history.class, "", false);
        }

        if (id == R.id.action_search3) {
            helper_editText.editText_searchWeb(editText, Browser.this);
        }

        if (id == R.id.action_pass) {
            helper_main.switchToActivity(Browser.this, Popup_pass.class, "", false);
            sharedPref.edit().putString("pass_copy_url", mWebView.getUrl()).apply();
            sharedPref.edit().putString("pass_copy_title", mWebView.getTitle()).apply();
        }

        if (id == R.id.action_toggle) {

            sharedPref.edit().putString("started", "yes").apply();
            String link = mWebView.getUrl();
            int domainInt = link.indexOf("//") + 2;
            final String domain = link.substring(domainInt, link.indexOf('/', domainInt));
            final String whiteList = sharedPref.getString("whiteList", "");

            AlertDialog.Builder builder = new AlertDialog.Builder(Browser.this);
            View dialogView = View.inflate(Browser.this, R.layout.dialog_toggle, null);

            Switch sw_java = (Switch) dialogView.findViewById(R.id.switch1);
            Switch sw_pictures = (Switch) dialogView.findViewById(R.id.switch2);
            Switch sw_location = (Switch) dialogView.findViewById(R.id.switch3);
            Switch sw_cookies = (Switch) dialogView.findViewById(R.id.switch4);
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
                        helper_main.grantPermissionsLoc(Browser.this);
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
                    sharedPref.edit().putString("lastActivity", "browser").apply();
                    helper_main.switchToActivity(Browser.this, Activity_settings.class, "", true);
                }
            });

            final AlertDialog dialog = builder.create();
            // Display the custom alert dialog on interface
            dialog.show();

        }

        if (id == R.id.action_save) {
            final CharSequence[] options = {
                    getString(R.string.menu_save_screenshot),
                    getString(R.string.menu_save_bookmark),
                    getString(R.string.menu_save_readLater),
                    getString(R.string.menu_save_pass),
                    getString(R.string.menu_createShortcut)};
            new AlertDialog.Builder(Browser.this)
                    .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.menu_save_bookmark))) {
                                helper_editText.editText_saveBookmark(editText, Browser.this, mWebView);
                            }
                            if (options[item].equals(getString(R.string.menu_save_pass))) {
                                helper_editText.editText_savePass(Browser.this, mWebView, mWebView.getTitle(), mWebView.getUrl());
                            }
                            if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                try {
                                    final Database_ReadLater db = new Database_ReadLater(Browser.this);
                                    db.addBookmark(mWebView.getTitle(), mWebView.getUrl());
                                    db.close();
                                    Snackbar.make(mWebView, R.string.readLater_added, Snackbar.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (options[item].equals(getString(R.string.menu_save_screenshot))) {
                                screenshot();
                            }
                            if (options[item].equals (getString(R.string.menu_createShortcut))) {
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_VIEW);
                                i.setClassName(Browser.this, "de.baumann.browser.Browser");
                                i.setData(Uri.parse(mWebView.getUrl()));

                                Intent shortcut = new Intent();
                                shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
                                shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, mWebView.getTitle());
                                shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(Browser.this.getApplicationContext(), R.mipmap.ic_launcher));
                                shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                Browser.this.sendBroadcast(shortcut);
                                Snackbar.make(mWebView, R.string.menu_createShortcut_success, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }).show();
        }

        if (id == R.id.action_share) {
            final CharSequence[] options = {
                    getString(R.string.menu_share_screenshot),
                    getString(R.string.menu_share_link),
                    getString(R.string.menu_share_link_copy)};
            new AlertDialog.Builder(Browser.this)
                    .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals(getString(R.string.menu_share_link))) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("text/plain");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                            }
                            if (options[item].equals(getString(R.string.menu_share_screenshot))) {
                                screenshot();

                                if (shareFile.exists()) {
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.setType("image/png");
                                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                                    sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                                    Uri bmpUri = Uri.fromFile(shareFile);
                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                    startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_screenshot))));
                                }
                            }
                            if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                String  url = mWebView.getUrl();
                                ClipboardManager clipboard = (ClipboardManager) Browser.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                                Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }).show();
        }

        if (id == R.id.action_downloads) {
            String startDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            helper_main.openFilePicker(Browser.this, mWebView, startDir);
        }

        if (id == R.id.action_searchSite) {
            mWebView.stopLoading();
            helper_editText.editText_FocusChange_searchSite(editText, Browser.this);
            helper_editText.editText_searchSite(editText, Browser.this, mWebView);
        }

        if (id == R.id.action_search2) {

            String text = editText.getText().toString();

            if (text.startsWith(getString(R.string.app_search))) {
                helper_editText.editText_searchSite(editText, Browser.this, mWebView);
            } else {
                mWebView.findAllAsync(text);
                helper_editText.hideKeyboard(Browser.this, editText, 1, getString(R.string.app_search) + " " + text, getString(R.string.app_search_hint_site));
            }

        }

        if (id == R.id.action_prev) {
            mWebView.findNext(false);
        }

        if (id == R.id.action_next) {
            mWebView.findNext(true);
        }

        if (id == R.id.action_cancel) {
            helper_editText.editText_FocusChange(editText, Browser.this);
            helper_editText.editText_EditorAction(editText, Browser.this, mWebView);
            helper_editText.hideKeyboard(Browser.this, editText, 0, mWebView.getTitle(), getString(R.string.app_search_hint));
        }

        if (id == R.id.action_save_bookmark) {
            helper_editText.editText_saveBookmark_save(editText, Browser.this, mWebView);
        }

        return super.onOptionsItemSelected(item);
    }



    class myWebChromeClient extends WebChromeClient {

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
            Log.i(TAG, "onGeolocationPermissionsShowPrompt()");

            final boolean remember = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(Browser.this);
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

            sharedPref.edit().putInt("keyboard", 0).apply();
            invalidateOptionsMenu();
            setNavArrows();
            progressString = "loading";
            imageButton.setVisibility(View.INVISIBLE);
            actionBar = getSupportActionBar();
            assert actionBar != null;
            if (!actionBar.isShowing()) {
                actionBar.show();
            }

            progressBar.setProgress(progress);
            progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);

            String url = mWebView.getUrl();
            int domainInt = mWebView.getUrl().indexOf("//") + 2;
            final  String domain = url.substring(domainInt, url.indexOf('/', domainInt));
            String whiteList = sharedPref.getString("whiteList", "");

            if (whiteList.contains(domain)) {
                mWebView.getSettings().setJavaScriptEnabled(true);
            } else {
                if (sharedPref.getString("started", "").equals("yes")) {
                    if (sharedPref.getString("java_string", "True").equals(Browser.this.getString(R.string.app_yes))){
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

            if (progress == 100) {
                progressString = "loaded";
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
                    photoFile = createImageFile();
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

        shareFile = helper_main.newFile();

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
                        .make(mWebView, getString(R.string.context_saveImage_toast) + " " + helper_main.newFileName() +
                                ". " + getString(R.string.app_open), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String startDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                                helper_main.openFilePicker(Browser.this, mWebView, startDir);
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

    private void setNavArrows() {
        if (sharedPref.getString ("nav", "2").equals("2") || sharedPref.getString ("nav", "2").equals("3")){
            if (mWebView.canGoBack()) {
                imageButton_left.setVisibility(View.VISIBLE);
            } else {
                imageButton_left.setVisibility(View.INVISIBLE);
            }
            imageButton_left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mWebView.goBack();
                }
            });

            if (mWebView.canGoForward()) {
                imageButton_right.setVisibility(View.VISIBLE);
            } else {
                imageButton_right.setVisibility(View.INVISIBLE);
            }
            imageButton_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mWebView.goForward();
                }
            });
        }
    }
}
