/*
    activity file is part of the Browser WebApp.

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.baumann.browser.databases.DbAdapter_ReadLater;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;
import de.baumann.browser.helper.helper_webView;
import de.baumann.browser.lists.List_bookmarks;
import de.baumann.browser.lists.List_files;
import de.baumann.browser.lists.List_history;
import de.baumann.browser.lists.List_pass;
import de.baumann.browser.lists.List_readLater;
import de.baumann.browser.utils.Utils_AdBlocker;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Browser_5 extends AppCompatActivity implements ObservableScrollViewCallbacks {

    // Views

    private ObservableWebView mWebView;
    private ProgressBar progressBar;
    private ImageButton imageButton;
    private ImageButton imageButton_left;
    private ImageButton imageButton_right;
    private TextView urlBar;
    private FrameLayout customViewContainer;
    private View mCustomView;
    private EditText editText;
    private RelativeLayout relativeLayout;
    private HorizontalScrollView scrollTabs;
    private Toolbar toolbar;


    // Strings

    private String mCameraPhotoPath;
    private final String TAG = "Browser";
    private String sharePath;


    // Others

    private Activity activity;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private myWebChromeClient mWebChromeClient;
    private SharedPreferences sharedPref;
    private File shareFile;
    private Bitmap bitmap;
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final int REQUEST_CODE_LOLLIPOP = 1;


    // Booleans

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
        
        activity = Browser_5.this;

        WebView.enableSlowWholeDocumentDraw();
        setContentView(R.layout.activity_browser);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark_1));
        helper_main.checkPin(activity);
        helper_main.onStart(activity);
        helper_main.grantPermissionsStorage(activity);

        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedPref.edit().putInt("actualTab", 2).apply();


        // find Views

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mWebView = (ObservableWebView) findViewById(R.id.webView);
        relativeLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        editText = (EditText) findViewById(R.id.editText);
        urlBar = (TextView) findViewById(R.id.urlBar);
        imageButton_left = (ImageButton) findViewById(R.id.imageButton_left);
        imageButton_right = (ImageButton) findViewById(R.id.imageButton_right);
        imageButton = (ImageButton) findViewById(R.id.imageButton);
        scrollTabs  = (HorizontalScrollView) activity.findViewById(R.id.scrollTabs);


        // setupViews

        helper_browser.setupViews(activity, toolbar, mWebView, urlBar, editText, imageButton, imageButton_left, imageButton_right, relativeLayout);
        toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorPrimary_1));
        setSupportActionBar(toolbar);


        // setup WebView

        helper_webView.webView_Settings(activity, mWebView);
        helper_webView.webView_WebViewClient(activity, mWebView, urlBar);

        mWebChromeClient = new myWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setScrollViewCallbacks(this);
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

                registerReceiver(onComplete_download, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

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

        Utils_AdBlocker.init(activity);
        onNewIntent(getIntent());
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final WebView.HitTestResult result = mWebView.getHitTestResult();
        final String url = result.getExtra();

        if(url != null) {

            if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_context, null);

                builder.setView(dialogView);
                builder.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        dialog.cancel();
                    }
                });

                final AlertDialog dialog = builder.create();
                // Display the custom alert dialog on interface
                dialog.show();

                if (url.endsWith(".gif") || url.endsWith(".bmp") || url.endsWith(".tiff") ||
                        url.endsWith(".svg") || url.endsWith(".png") || url.endsWith(".jpg") ||
                        url.endsWith(".JPG") || url.endsWith(".jpeg")) {
                    sharePath = mWebView.getUrl().substring(mWebView.getUrl().lastIndexOf("/")+1);
                } else {
                    sharePath = helper_webView.getDomain(activity, url) + ".png";
                }

                shareFile = helper_main.newFile(sharePath);

                TextView menu_share_link_copy = (TextView) dialogView.findViewById(R.id.menu_share_link_copy);
                menu_share_link_copy.setText(R.string.context_saveImage);
                LinearLayout menu_share_link_copy_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_share_link_copy_Layout);
                menu_share_link_copy_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Uri source = Uri.parse(url);
                            DownloadManager.Request request = new DownloadManager.Request(source);
                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sharePath);
                            DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);
                            Snackbar.make(mWebView, activity.getString(R.string.context_saveImage_toast) + " " + sharePath , Snackbar.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                        }
                        activity.registerReceiver(onComplete_download, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                        dialog.cancel();
                    }
                });

                TextView menu_share_link = (TextView) dialogView.findViewById(R.id.menu_share_link);
                menu_share_link.setText(R.string.context_shareImage);
                LinearLayout menu_share_link_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_share_link_Layout);
                menu_share_link_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Uri source = Uri.parse(url);
                            DownloadManager.Request request = new DownloadManager.Request(source);
                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sharePath);
                            DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                            dm.enqueue(request);

                            Snackbar.make(mWebView, activity.getString(R.string.context_saveImage_toast) + " " + sharePath , Snackbar.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                        }
                        activity.registerReceiver(onComplete_share, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                        dialog.cancel();
                    }
                });

                TextView menu_save_readLater = (TextView) dialogView.findViewById(R.id.menu_save_readLater);
                menu_save_readLater.setText(R.string.menu_save_readLater);
                LinearLayout menu_save_readLater_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_save_readLater_Layout);
                menu_save_readLater_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DbAdapter_ReadLater db = new DbAdapter_ReadLater(activity);
                        db.open();
                        if(db.isExist(mWebView.getUrl())){
                            Snackbar.make(editText, activity.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                        }else{
                            db.insert(helper_webView.getDomain(activity, url), url, "", "", helper_main.createDate());
                            Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                        }
                        dialog.cancel();
                    }
                });

                TextView context_1 = (TextView) dialogView.findViewById(R.id.scrollView_1);
                ImageView context_1_preView = (ImageView) dialogView.findViewById(R.id.context_1_preView);
                CardView context_1_Layout = (CardView) dialogView.findViewById(R.id.scrollView_1_Layout);
                helper_toolbar.toolbarContext(activity, context_1, context_1_preView, context_1_Layout, url,
                        1, helper_browser.tab_1(activity), "/tab_1.jpg", dialog, Browser_1.class);

                TextView context_2 = (TextView) dialogView.findViewById(R.id.scrollView_2);
                ImageView context_2_preView = (ImageView) dialogView.findViewById(R.id.context_2_preView);
                CardView context_2_Layout = (CardView) dialogView.findViewById(R.id.scrollView_2_Layout);
                helper_toolbar.toolbarContext(activity, context_2, context_2_preView, context_2_Layout, url,
                        2, helper_browser.tab_2(activity), "/tab_2.jpg", dialog, Browser_2.class);

                TextView context_3 = (TextView) dialogView.findViewById(R.id.scrollView_3);
                ImageView context_3_preView = (ImageView) dialogView.findViewById(R.id.context_3_preView);
                CardView context_3_Layout = (CardView) dialogView.findViewById(R.id.scrollView_3_Layout);
                helper_toolbar.toolbarContext(activity, context_3, context_3_preView, context_3_Layout, url,
                        3, helper_browser.tab_3(activity), "/tab_3.jpg", dialog, Browser_3.class);

                TextView context_4 = (TextView) dialogView.findViewById(R.id.scrollView_4);
                ImageView context_4_preView = (ImageView) dialogView.findViewById(R.id.context_4_preView);
                CardView context_4_Layout = (CardView) dialogView.findViewById(R.id.scrollView_4_Layout);
                helper_toolbar.toolbarContext(activity, context_4, context_4_preView, context_4_Layout, url,
                        4, helper_browser.tab_4(activity), "/tab_4.jpg", dialog, Browser_4.class);

                TextView context_5 = (TextView) dialogView.findViewById(R.id.scrollView_5);
                ImageView context_5_preView = (ImageView) dialogView.findViewById(R.id.context_5_preView);
                CardView context_5_Layout = (CardView) dialogView.findViewById(R.id.scrollView_5_Layout);
                helper_toolbar.toolbarContext(activity, context_5, context_5_preView, context_5_Layout, url,
                        5, helper_browser.tab_5(activity), "/tab_5.jpg", dialog, Browser_5.class);

            } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                final  View dialogView = View.inflate(activity, R.layout.dialog_context, null);

                builder.setView(dialogView);
                builder.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        dialog.cancel();
                    }
                });

                final AlertDialog dialog = builder.create();
                // Display the custom alert dialog on interface
                dialog.show();

                LinearLayout context_save_Layout = (LinearLayout) dialogView.findViewById(R.id.context_save_Layout);
                context_save_Layout.setVisibility(View.VISIBLE);
                context_save_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String filename = url.substring(url.lastIndexOf("/")+1);
                        dialog.cancel();
                        Snackbar snackbar = Snackbar
                                .make(mWebView, getString(R.string.toast_download_1) + " " + filename, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            Uri source = Uri.parse(url);
                                            DownloadManager.Request request = new DownloadManager.Request(source);
                                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                            request.allowScanningByMediaScanner();
                                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                            DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                                            dm.enqueue(request);
                                            Snackbar.make(mWebView, getString(R.string.toast_download) + " " + filename , Snackbar.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Snackbar.make(mWebView, R.string.toast_perm , Snackbar.LENGTH_SHORT).show();
                                        }
                                        activity.registerReceiver(onComplete_download, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                                    }
                                });
                        snackbar.show();
                    }
                });

                LinearLayout menu_share_link_copy_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_share_link_copy_Layout);
                menu_share_link_copy_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
                        Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });

                LinearLayout menu_share_link_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_share_link_Layout);
                menu_share_link_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                        sendIntent.setType("text/plain");
                        activity.startActivity(Intent.createChooser(sendIntent, activity.getResources()
                                .getString(R.string.app_share_link)));
                        dialog.cancel();
                    }
                });

                LinearLayout menu_save_readLater_Layout = (LinearLayout) dialogView.findViewById(R.id.menu_save_readLater_Layout);
                menu_save_readLater_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DbAdapter_ReadLater db = new DbAdapter_ReadLater(activity);
                        db.open();
                        if(db.isExist(mWebView.getUrl())){
                            Snackbar.make(editText, activity.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                        }else{
                            db.insert(helper_webView.getDomain(activity, url), url, "", "", helper_main.createDate());
                            Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                        }
                        dialog.cancel();
                    }
                });

                TextView context_1 = (TextView) dialogView.findViewById(R.id.scrollView_1);
                ImageView context_1_preView = (ImageView) dialogView.findViewById(R.id.context_1_preView);
                CardView context_1_Layout = (CardView) dialogView.findViewById(R.id.scrollView_1_Layout);
                helper_toolbar.toolbarContext(activity, context_1, context_1_preView, context_1_Layout, url,
                        1, helper_browser.tab_1(activity), "/tab_1.jpg", dialog, Browser_1.class);

                TextView context_2 = (TextView) dialogView.findViewById(R.id.scrollView_2);
                ImageView context_2_preView = (ImageView) dialogView.findViewById(R.id.context_2_preView);
                CardView context_2_Layout = (CardView) dialogView.findViewById(R.id.scrollView_2_Layout);
                helper_toolbar.toolbarContext(activity, context_2, context_2_preView, context_2_Layout, url,
                        2, helper_browser.tab_2(activity), "/tab_2.jpg", dialog, Browser_2.class);

                TextView context_3 = (TextView) dialogView.findViewById(R.id.scrollView_3);
                ImageView context_3_preView = (ImageView) dialogView.findViewById(R.id.context_3_preView);
                CardView context_3_Layout = (CardView) dialogView.findViewById(R.id.scrollView_3_Layout);
                helper_toolbar.toolbarContext(activity, context_3, context_3_preView, context_3_Layout, url,
                        3, helper_browser.tab_3(activity), "/tab_3.jpg", dialog, Browser_3.class);

                TextView context_4 = (TextView) dialogView.findViewById(R.id.scrollView_4);
                ImageView context_4_preView = (ImageView) dialogView.findViewById(R.id.context_4_preView);
                CardView context_4_Layout = (CardView) dialogView.findViewById(R.id.scrollView_4_Layout);
                helper_toolbar.toolbarContext(activity, context_4, context_4_preView, context_4_Layout, url,
                        4, helper_browser.tab_4(activity), "/tab_4.jpg", dialog, Browser_4.class);

                TextView context_5 = (TextView) dialogView.findViewById(R.id.scrollView_5);
                ImageView context_5_preView = (ImageView) dialogView.findViewById(R.id.context_5_preView);
                CardView context_5_Layout = (CardView) dialogView.findViewById(R.id.scrollView_5_Layout);
                helper_toolbar.toolbarContext(activity, context_5, context_5_preView, context_5_Layout, url,
                        5, helper_browser.tab_5(activity), "/tab_5.jpg", dialog, Browser_5.class);
            }
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
        helper_browser.scroll(activity, scrollState, relativeLayout, toolbar, imageButton, imageButton_left, imageButton_right,
                urlBar, mWebView, scrollTabs);
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void onBackPressed() {

        if (scrollTabs.getVisibility() == View.VISIBLE) {
            scrollTabs.setVisibility(View.GONE);
        } else if (inCustomView()) {
            hideCustomView();
        } else if ((mCustomView == null) && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            Snackbar snackbar = Snackbar
                    .make(mWebView, getString(R.string.toast_exit), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            helper_main.closeApp(activity, mWebView);
                        }
                    });
            snackbar.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        if (sharedPref.getInt("closeApp", 0) == 1) {
            helper_main.closeApp(activity, mWebView);
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                helper_editText.hideKeyboard(activity, editText, 0, helper_webView.getTitle (activity, mWebView), getString(R.string.app_search_hint));
            }
        }, 100);
        sharedPref.edit().putInt("actualTab", 5).apply();
        mWebView.onResume();
        final String URL = sharedPref.getString("openURL","https://github.com/scoute-dich/browser/");
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (URL.equals(mWebView.getUrl()) || URL.equals("")) {
                    Log.i(TAG, "Tab switched");
                } else if (URL.equals("settings")) {
                    mWebView.reload();
                } else if (URL.equals("copyLogin")) {
                    Snackbar snackbar = Snackbar
                            .make(mWebView, R.string.pass_copy_userName, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyUN", "")));

                                    Snackbar snackbar = Snackbar
                                            .make(mWebView, R.string.pass_copy_userPW, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyPW", "")));
                                                }
                                            });
                                    snackbar.show();
                                }
                            });
                    snackbar.show();
                } else if (URL.contains("openLogin")) {
                    Snackbar snackbar = Snackbar
                            .make(mWebView, R.string.pass_copy_userName, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyUN", "")));

                                    Snackbar snackbar = Snackbar
                                            .make(mWebView, R.string.pass_copy_userPW, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("userName", sharedPref.getString("copyPW", "")));
                                                }
                                            });
                                    snackbar.show();
                                }
                            });
                    snackbar.show();
                    mWebView.loadUrl(URL.replace("openLogin", ""));
                } else if (URL.contains("exit")) {
                    sharedPref.edit().putString("openURL", "").apply();
                    sharedPref.edit().putString("tab_5", "").apply();
                    File tab_5 = new File(activity.getFilesDir() + "/tab_5.jpg");
                    tab_5.delete();
                    finish();
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
        helper_browser.prepareMenu(activity, menu);
        return true; // activity is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; activity adds items to the action bar if it is present.
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

        if (id == R.id.action_history) {
            helper_main.switchToActivity(activity, List_history.class, "", false);
        }

        if (id == R.id.action_search_chooseWebsite) {
            helper_editText.editText_searchWeb(editText, activity);
        }

        if (id == R.id.action_pass) {
            helper_main.switchToActivity(activity, List_pass.class, "", false);
            sharedPref.edit().putString("pass_copy_url", mWebView.getUrl()).apply();
            sharedPref.edit().putString("pass_copy_title", helper_webView.getTitle (activity, mWebView)).apply();
        }

        if (id == R.id.action_toggle) {
            helper_browser.switcher(activity, mWebView, urlBar);
        }

        if (id == R.id.menu_save_screenshot) {
            screenshot();
        }

        if (id == R.id.menu_save_bookmark) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.editText_saveBookmark(editText, activity, mWebView);
        }

        if (id == R.id.menu_save_readLater) {
            DbAdapter_ReadLater db = new DbAdapter_ReadLater(activity);
            db.open();
            if(db.isExist(mWebView.getUrl())){
                Snackbar.make(editText, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
            }else{
                db.insert(helper_webView.getTitle (activity, mWebView), mWebView.getUrl(), "", "", helper_main.createDate());
                Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
            }
        }

        if (id == R.id.menu_save_pass) {
            helper_editText.editText_savePass(activity, mWebView, helper_webView.getTitle (activity, mWebView), mWebView.getUrl());
        }

        if (id == R.id.menu_createShortcut) {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setClassName(activity, "de.baumann.browser.Browser_1");
            i.setData(Uri.parse(mWebView.getUrl()));

            Intent shortcut = new Intent();
            shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
            shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, helper_webView.getTitle (activity, mWebView));
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(activity.getApplicationContext(), R.mipmap.ic_launcher));
            shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            activity.sendBroadcast(shortcut);
            Snackbar.make(mWebView, R.string.menu_createShortcut_success, Snackbar.LENGTH_SHORT).show();
        }

        if (id == R.id.menu_share_screenshot) {
            screenshot();
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, helper_webView.getTitle (activity, mWebView));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            Uri bmpUri = Uri.fromFile(shareFile);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_screenshot))));
        }

        if (id == R.id.menu_share_link) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, helper_webView.getTitle (activity, mWebView));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
        }

        if (id == R.id.menu_share_link_copy) {
            String  url = mWebView.getUrl();
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("text", url));
            Snackbar.make(mWebView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
        }

        if (id == R.id.action_downloads) {
            helper_main.switchToActivity(activity, List_files.class, "", false);
        }

        if (id == R.id.action_search_go) {

            String text = editText.getText().toString();
            helper_webView.openURL(activity, mWebView, editText);
            helper_editText.hideKeyboard(activity, editText, 0, text, getString(R.string.app_search_hint));
            helper_editText.editText_EditorAction(editText, activity, mWebView, urlBar);
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        if (id == R.id.action_search_onSite) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.showKeyboard(activity, editText, 1, "", getString(R.string.app_search_hint));
            helper_editText.editText_FocusChange_searchSite(editText, activity);
            helper_editText.editText_searchSite(editText, activity, mWebView, urlBar);
        }

        if (id == R.id.action_search_onSite_go) {

            String text = editText.getText().toString();

            if (text.startsWith(getString(R.string.app_search))) {
                helper_editText.editText_searchSite(editText, activity, mWebView, urlBar);
            } else {
                mWebView.findAllAsync(text);
                helper_editText.hideKeyboard(activity, editText, 1, getString(R.string.app_search) + " " + text, getString(R.string.app_search_hint_site));
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
            urlBar.setText(helper_webView.getTitle (activity, mWebView));
            editText.setVisibility(View.GONE);
            mWebView.findAllAsync("");
            helper_editText.editText_FocusChange(editText, activity);
            helper_editText.editText_EditorAction(editText, activity, mWebView, urlBar);
            helper_editText.hideKeyboard(activity, editText, 0, helper_webView.getTitle (activity, mWebView), getString(R.string.app_search_hint));
        }

        if (id == R.id.action_save_bookmark) {
            helper_editText.editText_saveBookmark_save(editText, activity, mWebView);
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        if (id == R.id.action_reload) {
            mWebView.reload();
        }

        return super.onOptionsItemSelected(item);
    }

    private class myWebChromeClient extends WebChromeClient {

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
            Log.i(TAG, "onGeolocationPermissionsShowPrompt()");

            final boolean remember = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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

            sharedPref.edit().putString("tab_5", helper_webView.getTitle(activity, mWebView)).apply();
            progressBar.setProgress(progress);
            progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);

            try {
                String whiteList = sharedPref.getString("whiteList", "");

                if (whiteList.contains(helper_webView.getDomain(activity, mWebView.getUrl()))) {
                    mWebView.getSettings().setJavaScriptEnabled(true);
                } else {
                    if (sharedPref.getString("started", "").equals("yes")) {
                        if (sharedPref.getString("java_string", "True").equals(activity.getString(R.string.app_yes))){
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
            } catch (Exception e) {
                // Error occurred while creating the File
                Log.e(TAG, "Browser Error", e);
            }

            if (progress == 100) {

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int width = mWebView.getWidth();
                            int high = (width/175) * 100;
                            Bitmap bitmap = Bitmap.createBitmap(width, high , Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            mWebView.draw(canvas);

                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
                            File file = new File(activity.getFilesDir() + "/tab_5.jpg");
                            file.createNewFile();
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(bytes.toByteArray());
                            outputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 100);

                if (imageButton.getVisibility() != View.VISIBLE) {
                    helper_browser.setNavArrows(mWebView, imageButton_left, imageButton_right);
                }
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

            if (sharedPref.getString ("fullscreen", "2").equals("2") || sharedPref.getString ("fullscreen", "2").equals("4")){
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

            if (sharedPref.getString ("fullscreen", "2").equals("2") || sharedPref.getString ("fullscreen", "2").equals("4")){
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }


    // Methods

    private void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    protected void onNewIntent(final Intent intent) {

        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    String searchEngine = sharedPref.getString("searchEngine", "https://duckduckgo.com/?q=");
                    mWebView.loadUrl(searchEngine + sharedText);
                }
            }, 300);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    Uri data = intent.getData();
                    String link = data.toString();
                    mWebView.loadUrl(link);
                }
            }, 300);
        } else if ("readLater".equals(action)) {
            helper_main.switchToActivity(activity, List_readLater.class, "", false);
        } else if ("bookmarks".equals(action)) {
            helper_main.switchToActivity(activity, List_bookmarks.class, "", false);
        } else if ("history".equals(action)) {
            helper_main.switchToActivity(activity, List_history.class, "", false);
        } else if ("pass".equals(action)) {
            helper_main.switchToActivity(activity, List_pass.class, "", false);
        } else {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    mWebView.loadUrl(intent.getStringExtra("URL"));
                }
            }, 300);
        }
    }

    private void screenshot() {

        sharePath = helper_webView.getDomain(activity, mWebView.getUrl()) + "_" + helper_main.createDate_Second() + ".jpg";
        shareFile = helper_main.newFile(sharePath);

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

                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
                fOut.flush();
                fOut.close();
                bitmap.recycle();

                Snackbar snackbar = Snackbar
                        .make(mWebView, getString(R.string.context_saveImage_toast) + " " + shareFile.getName() +
                                ". " + getString(R.string.app_open), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.toast_yes), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                helper_main.switchToActivity(activity, List_files.class, "", false);
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


    // Receivers

    private final BroadcastReceiver onComplete_download = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Snackbar snackbar = Snackbar
                    .make(mWebView, activity.getString(R.string.app_open), Snackbar.LENGTH_LONG)
                    .setAction(activity.getString(R.string.toast_yes), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            helper_main.switchToActivity(activity, List_files.class, "", false);
                        }
                    });
            snackbar.show();
            activity.unregisterReceiver(onComplete_download);
        }
    };

    private final BroadcastReceiver onComplete_share = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            Uri myUri= Uri.fromFile(shareFile);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/*");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, myUri);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, helper_webView.getTitle (activity, mWebView));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            activity.startActivity(Intent.createChooser(sharingIntent, (activity.getString(R.string.app_share_image))));
            activity.unregisterReceiver(onComplete_share);
        }
    };
}