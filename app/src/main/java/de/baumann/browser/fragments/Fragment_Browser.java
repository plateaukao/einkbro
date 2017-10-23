package de.baumann.browser.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_ReadLater;
import de.baumann.browser.helper.Activity_settings;
import de.baumann.browser.helper.class_CustomViewPager;
import de.baumann.browser.helper.helper_browser;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;
import de.baumann.browser.helper.helper_toolbar;
import de.baumann.browser.helper.helper_webView;
import de.baumann.browser.utils.Utils_AdBlocker;

import static android.content.Context.DOWNLOAD_SERVICE;

public class Fragment_Browser extends Fragment implements ObservableScrollViewCallbacks {


    // Views

    private ObservableWebView mWebView;
    private ProgressBar progressBar;
    private ImageButton imageButton_up;
    private ImageButton imageButton_down;
    private ImageButton imageButton_left;
    private ImageButton imageButton_right;
    private TextView urlBar;
    private FrameLayout customViewContainer;
    private View mCustomView;
    private EditText editText;
    private HorizontalScrollView scrollTabs;
    private class_CustomViewPager viewPager;
    private AppBarLayout appBarLayout;


    // Strings

    private String mCameraPhotoPath;
    private final String TAG = "Browser";
    private String sharePath;
    private String tab_number;


    // Others

    private Activity activity;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private myWebChromeClient mWebChromeClient;
    private SharedPreferences sharedPref;
    private File shareFile;
    private Bitmap bitmap;
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final int REQUEST_CODE_LOLLIPOP = 1;
    private HorizontalScrollView horizontalScrollView;


    // Booleans

    private boolean inCustomView() {
        return (mCustomView != null);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_browser, container, false);

        setHasOptionsMenu(true);
        activity = getActivity();

        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_search, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_app, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_close, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_start, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_search_main, false);
        PreferenceManager.setDefaultValues(activity, R.xml.user_settings_data, false);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedPref.edit().putInt("tab_" + tab_number + "_exit", 0).apply();


        // find Views

        mWebView = (ObservableWebView) rootView.findViewById(R.id.webView);
        customViewContainer = (FrameLayout) rootView.findViewById(R.id.customViewContainer);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        editText = (EditText) activity.findViewById(R.id.editText);
        urlBar = (TextView) activity.findViewById(R.id.urlBar);
        imageButton_left = (ImageButton) rootView.findViewById(R.id.imageButton_left);
        imageButton_right = (ImageButton) rootView.findViewById(R.id.imageButton_right);
        imageButton_up = (ImageButton) rootView.findViewById(R.id.imageButton);
        imageButton_down = (ImageButton) rootView.findViewById(R.id.imageButton_down);
        scrollTabs  = (HorizontalScrollView) activity.findViewById(R.id.scrollTabs);
        viewPager = (class_CustomViewPager) activity.findViewById(R.id.viewpager);
        appBarLayout = (AppBarLayout) activity.findViewById(R.id.appBarLayout);
        horizontalScrollView = (HorizontalScrollView) getActivity().findViewById(R.id.scrollTabs);


        // setupViews

        helper_browser.setupViews(activity, viewPager, mWebView, editText, imageButton_up, imageButton_down, imageButton_left,
                imageButton_right, appBarLayout, horizontalScrollView);
        helper_webView.webView_Settings(activity, mWebView);
        helper_webView.webView_WebViewClient(activity, mWebView, urlBar);

        mWebChromeClient = new myWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setScrollViewCallbacks(this);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(final String url, String userAgent,
                                        final String contentDisposition, final String mimetype,
                                        long contentLength) {

                activity.registerReceiver(onComplete_download, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

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
                                DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);

                                Snackbar.make(mWebView, getString(R.string.toast_download) + " " + filename , Snackbar.LENGTH_LONG).show();
                            }
                        });
                snackbar.show();
            }
        });


        // other stuff

        Utils_AdBlocker.init(activity);
        registerForContextMenu(mWebView);

        return rootView;
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

        WebView.HitTestResult result = mWebView.getHitTestResult();
        final String url = result.getExtra();
        final AlertDialog dialog;
        final View dialogView = View.inflate(activity, R.layout.dialog_context, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if(url != null) {

            if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){

                builder.setView(dialogView);
                builder.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                dialog = builder.create();
                dialog.show();

                try {
                    sharePath = URLUtil.guessFileName(url, null, null);
                } catch (Exception e) {
                    sharePath = helper_webView.getDomain(activity, url) + url.substring(url.lastIndexOf('/') + 1);
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
                        if(db.isExist(helper_main.secString(mWebView.getUrl()))){
                            Snackbar.make(editText, activity.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                        }else{
                            db.insert(helper_main.secString(helper_webView.getDomain(activity, url)), helper_main.secString(url), "", "", helper_main.createDate_norm());
                            Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                        }
                        dialog.cancel();
                    }
                });

                contextMenu(dialogView, url, dialog);

            } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {

                builder.setView(dialogView);
                builder.setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        dialog.cancel();
                    }
                });

                dialog = builder.create();
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
                        if(db.isExist(helper_main.secString(mWebView.getUrl()))){
                            Snackbar.make(editText, activity.getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                        }else{
                            db.insert(helper_main.secString(helper_webView.getDomain(activity, url)), helper_main.secString(url), "", "", helper_main.createDate_norm());
                            Snackbar.make(mWebView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                        }
                        dialog.cancel();
                    }
                });

                contextMenu(dialogView, url, dialog);
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

        if (scrollState == ScrollState.UP) {

            imageButton_up.setVisibility(View.VISIBLE);
            imageButton_down.setVisibility(View.VISIBLE);
            imageButton_left.setVisibility(View.GONE);
            imageButton_right.setVisibility(View.GONE);

            if (sharedPref.getString ("fullscreen", "2").equals("2") || sharedPref.getString ("fullscreen", "2").equals("3")){
                appBarLayout.setVisibility(View.GONE);
            }

        } else if (scrollState == ScrollState.DOWN) {

            urlBar.setText(mWebView.getTitle());
            helper_browser.setNavArrows(mWebView, imageButton_left, imageButton_right);
            imageButton_up.setVisibility(View.GONE);
            imageButton_down.setVisibility(View.GONE);
            appBarLayout.setVisibility(View.VISIBLE);

        } else {
            imageButton_up.setVisibility(View.GONE);
            imageButton_down.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

            sharedPref.edit().putString("tab_" + tab_number, helper_webView.getTitle(activity, mWebView)).apply();
            urlBar.setText(helper_webView.getTitle(activity, mWebView));
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

            if (progress < 10) {
                if (scrollTabs.getVisibility() == View.VISIBLE) {
                    scrollTabs.setVisibility(View.GONE);
                }
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
                            File file = new File(activity.getFilesDir() + "/tab_" + tab_number + ".jpg");
                            file.createNewFile();
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(bytes.toByteArray());
                            outputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 100);

                if (imageButton_up.getVisibility() != View.VISIBLE) {
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
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
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
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            mCustomView = view;
            mWebView.setVisibility(View.GONE);
            appBarLayout.setVisibility(View.GONE);
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
            appBarLayout.setVisibility(View.VISIBLE);

            if (sharedPref.getString ("fullscreen", "2").equals("2") || sharedPref.getString ("fullscreen", "2").equals("4")){
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

    private void contextMenu (View dialogView, String url, Dialog dialog) {
        HorizontalScrollView scrollTabs = (HorizontalScrollView) dialogView.findViewById(R.id.scrollTabs);

        TextView context_1 = (TextView) dialogView.findViewById(R.id.context_1);
        ImageView context_1_preView = (ImageView) dialogView.findViewById(R.id.context_1_preView);
        CardView context_1_Layout = (CardView) dialogView.findViewById(R.id.context_1_Layout);
        ImageView close_1 = (ImageView) dialogView.findViewById(R.id.close_1);
        helper_toolbar.cardViewClickMenu(getActivity(), context_1_Layout, scrollTabs, 0, close_1, viewPager, url, dialog, "0");
        helper_toolbar.toolBarPreview(getActivity(), context_1,context_1_preView, 0, helper_browser.tab_1(getActivity()), "/tab_0.jpg", close_1);

        TextView context_2 = (TextView) dialogView.findViewById(R.id.context_2);
        ImageView context_2_preView = (ImageView) dialogView.findViewById(R.id.context_2_preView);
        CardView context_2_Layout = (CardView) dialogView.findViewById(R.id.context_2_Layout);
        ImageView close_2 = (ImageView) dialogView.findViewById(R.id.close_2);
        helper_toolbar.cardViewClickMenu(getActivity(), context_2_Layout, scrollTabs, 1, close_2, viewPager, url, dialog, "1");
        helper_toolbar.toolBarPreview(getActivity(), context_2,context_2_preView, 1, helper_browser.tab_2(getActivity()), "/tab_1.jpg", close_2);

        TextView context_3 = (TextView) dialogView.findViewById(R.id.context_3);
        ImageView context_3_preView = (ImageView) dialogView.findViewById(R.id.context_3_preView);
        CardView context_3_Layout = (CardView) dialogView.findViewById(R.id.context_3_Layout);
        ImageView close_3 = (ImageView) dialogView.findViewById(R.id.close_3);
        helper_toolbar.cardViewClickMenu(getActivity(), context_3_Layout, scrollTabs, 2, close_3, viewPager, url, dialog, "2");
        helper_toolbar.toolBarPreview(getActivity(), context_3,context_3_preView, 2, helper_browser.tab_3(getActivity()), "/tab_2.jpg", close_3);

        TextView context_4 = (TextView) dialogView.findViewById(R.id.context_4);
        ImageView context_4_preView = (ImageView) dialogView.findViewById(R.id.context_4_preView);
        CardView context_4_Layout = (CardView) dialogView.findViewById(R.id.context_4_Layout);
        ImageView close_4 = (ImageView) dialogView.findViewById(R.id.close_4);
        helper_toolbar.cardViewClickMenu(getActivity(), context_4_Layout, scrollTabs, 3, close_4, viewPager, url, dialog, "3");
        helper_toolbar.toolBarPreview(getActivity(), context_4,context_4_preView, 3, helper_browser.tab_4(getActivity()), "/tab_3.jpg", close_4);

        TextView context_5 = (TextView) dialogView.findViewById(R.id.context_5);
        ImageView context_5_preView = (ImageView) dialogView.findViewById(R.id.context_5_preView);
        CardView context_5_Layout = (CardView) dialogView.findViewById(R.id.context_5_Layout);
        ImageView close_5 = (ImageView) dialogView.findViewById(R.id.close_5);
        helper_toolbar.cardViewClickMenu(getActivity(), context_5_Layout, scrollTabs, 4, close_5, viewPager, url, dialog, "4");
        helper_toolbar.toolBarPreview(getActivity(), context_5,context_5_preView, 4, helper_browser.tab_5(getActivity()), "/tab_4.jpg", close_5);

    }

    public void doBack() {
        //BackPressed in activity will call this;
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

    public void fragmentAction () {

        if (sharedPref.getInt("closeApp", 1) == 1) {
            helper_main.closeApp(activity, mWebView);
        }

        editText.setVisibility(View.GONE);

        setTitle();
        mWebView.findAllAsync("");
        tab_number = String.valueOf(viewPager.getCurrentItem());
        sharedPref.edit().putInt("tab", viewPager.getCurrentItem()).apply();
        sharedPref.edit().putInt("keyboard", 0).apply();

        final String URL = sharedPref.getString("openURL","https://github.com/scoute-dich/browser/");

        if (URL.equals(mWebView.getUrl()) || URL.equals("") && sharedPref.getString("tab_" + tab_number, "").length() > 0) {
            Log.i(TAG, "Tab switched");
        } else if (URL.equals("settings")) {
            mWebView.reload();
        } else if (URL.equals("settings_recreate")) {
            getActivity().recreate();
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
        } else if (sharedPref.getString("tab_" + tab_number, "").isEmpty() && URL.length() == 0) {
            mWebView.loadUrl(sharedPref.getString("startURL", "https://github.com/scoute-dich/browser/"));
        } else {
            mWebView.loadUrl(URL);
        }
    }

    private void setTitle () {

        String title = helper_webView.getTitle (activity, mWebView);
        sharedPref.edit().putString("webView_url", mWebView.getUrl()).apply();

        if (title.isEmpty()) {
            urlBar.setText(getString(R.string.app_name));
        } else {
            urlBar.setText(title);
        }
    }

    private void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    private void screenshot() {

        sharePath = helper_webView.getDomain(activity, mWebView.getUrl()) + "_" + helper_main.createDate_sec() + ".jpg";
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

            OutputStream os;
            try {
                os = new FileOutputStream(shareFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
            }


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
                                viewPager.setCurrentItem(9);
                            }
                        });
                snackbar.show();

                Uri uri = Uri.fromFile(shareFile);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                activity.sendBroadcast(intent);

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
                            viewPager.setCurrentItem(9);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = getActivity().findViewById(R.id.action_history);
                if (v != null) {
                    v.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            mWebView.reload();
                            return true;
                        }
                    });
                }
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = getActivity().findViewById(R.id.action_toggle);
                if (v != null) {
                    v.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            helper_main.switchToActivity(activity, Activity_settings.class);
                            return true;
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onPrepareOptionsMenu(menu);
        helper_browser.prepareMenu(activity, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        horizontalScrollView.setVisibility(View.GONE);

        int id = item.getItemId();

        if (id == R.id.action_history) {
            viewPager.setCurrentItem(7);
            scrollTabs.setVisibility(View.GONE);
        }

        if (id == R.id.action_search_chooseWebsite) {
            helper_editText.editText_searchWeb(editText, activity);
        }

        if (id == R.id.action_pass) {
            viewPager.setCurrentItem(8);
            scrollTabs.setVisibility(View.GONE);
            sharedPref.edit().putString("pass_copy_url", mWebView.getUrl()).apply();
            sharedPref.edit().putString("pass_copy_title", helper_webView.getTitle (activity, mWebView)).apply();
        }

        if (id == R.id.action_toggle) {
            helper_browser.switcher(activity, mWebView, urlBar, viewPager);
        }

        if (id == R.id.menu_save_screenshot) {
            screenshot();
        }

        if (id == R.id.menu_save_bookmark) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.showKeyboard(activity, editText, 2, helper_webView.getTitle(activity, mWebView), getString(R.string.app_search_hint_bookmark));editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        activity.findViewById(R.id.action_save_bookmark).performClick();
                    }
                    return false;
                }
            });

        }

        if (id == R.id.menu_save_readLater) {
            helper_main.save_readLater(getActivity(), helper_webView.getTitle(activity, mWebView), mWebView.getUrl(), mWebView);
        }

        if (id == R.id.menu_save_pass) {
            helper_editText.editText_savePass(activity, mWebView, helper_webView.getTitle (activity, mWebView), mWebView.getUrl());
        }

        if (id == R.id.menu_createShortcut) {
            helper_main.installShortcut(getActivity(), helper_webView.getTitle(activity, mWebView), mWebView.getUrl(), mWebView);
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
            viewPager.setCurrentItem(9);
            scrollTabs.setVisibility(View.GONE);
        }

        if (id == R.id.action_search_go) {

            String text = editText.getText().toString();
            helper_webView.openURL(activity, mWebView, editText);
            helper_editText.hideKeyboard(activity, editText, 0, text, getString(R.string.app_search_hint));
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        if (id == R.id.action_search_onSite) {
            urlBar.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            helper_editText.showKeyboard(activity, editText, 1, "", getString(R.string.app_search_hint_site));
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        activity.findViewById(R.id.action_search_onSite_go).performClick();
                    }
                    return false;
                }
            });
        }

        if (id == R.id.action_search_onSite_go) {
            String text = editText.getText().toString();
            helper_editText.hideKeyboard(activity, editText, 4, getString(R.string.app_search) + " " + text, getString(R.string.app_search_hint_site));
            mWebView.findAllAsync(text);
            editText.setVisibility(View.GONE);
            urlBar.setVisibility(View.VISIBLE);
            urlBar.setText(getString(R.string.app_search) + " " + text);
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
            helper_editText.hideKeyboard(activity, editText, 0, helper_webView.getTitle (activity, mWebView), getString(R.string.app_search_hint));
        }

        if (id == R.id.action_save_bookmark) {
            String inputTag = editText.getText().toString().trim();
            helper_main.save_bookmark(activity, inputTag, mWebView.getUrl(), mWebView);
            helper_editText.hideKeyboard(activity, editText, 0, mWebView.getTitle(), getString(R.string.app_search_hint));
            urlBar.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }

        if (id == R.id.action_reload) {
            mWebView.reload();
        }

        if (id == R.id.action_help) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View dialogView = View.inflate(getActivity(), R.layout.dialog_help, null);

            TextView help_lists_title = (TextView) dialogView.findViewById(R.id.help_lists_title);
            TextView help_lists = (TextView) dialogView.findViewById(R.id.help_lists);

            TextView help_toolbar_title = (TextView) dialogView.findViewById(R.id.help_toolbar_title);
            TextView help_toolbar = (TextView) dialogView.findViewById(R.id.help_toolbar);

            TextView help_tabs_title = (TextView) dialogView.findViewById(R.id.help_tabs_title);
            TextView help_tabs = (TextView) dialogView.findViewById(R.id.help_tabs);

            TextView help_menu_title = (TextView) dialogView.findViewById(R.id.help_menu_title);
            TextView help_menu = (TextView) dialogView.findViewById(R.id.help_menu);

            TextView help_search_title = (TextView) dialogView.findViewById(R.id.help_search_title);
            TextView help_search = (TextView) dialogView.findViewById(R.id.help_search);

            help_lists_title.setText(helper_main.textSpannable(getString(R.string.help_lists_title)));
            help_lists.setText(helper_main.textSpannable(getString(R.string.help_lists)));

            help_toolbar_title.setText(helper_main.textSpannable(getString(R.string.help_toolbar_title)));
            help_toolbar.setText(helper_main.textSpannable(getString(R.string.help_toolbar)));

            help_tabs_title.setText(helper_main.textSpannable(getString(R.string.help_tabs_title)));
            help_tabs.setText(helper_main.textSpannable(getString(R.string.help_tabs)));

            help_menu_title.setText(helper_main.textSpannable(getString(R.string.help_menu_title)));
            help_menu.setText(helper_main.textSpannable(getString(R.string.help_menu)));

            help_search_title.setText(helper_main.textSpannable(getString(R.string.help_search_title)));
            help_search.setText(helper_main.textSpannable(getString(R.string.help_search)));

            builder.setView(dialogView);
            builder.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }
}