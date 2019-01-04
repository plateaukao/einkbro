package de.baumann.browser.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.VideoView;

import com.mobapphome.mahencryptorlib.MAHEncryptor;

import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import de.baumann.browser.Browser.AdBlock;
import de.baumann.browser.Browser.AlbumController;
import de.baumann.browser.Browser.BrowserContainer;
import de.baumann.browser.Browser.BrowserController;
import de.baumann.browser.Browser.Cookie;
import de.baumann.browser.Browser.Javascript;
import de.baumann.browser.Database.BookmarkList;
import de.baumann.browser.Database.Record;
import de.baumann.browser.Database.RecordAction;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Service.ClearService;
import de.baumann.browser.Service.HolderService;
import de.baumann.browser.Task.ScreenshotTask;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.HelperUnit;
import de.baumann.browser.Unit.IntentUnit;
import de.baumann.browser.Unit.ViewUnit;
import de.baumann.browser.View.CompleteAdapter;
import de.baumann.browser.View.FullscreenHolder;
import de.baumann.browser.View.GridAdapter;
import de.baumann.browser.View.GridItem;

import de.baumann.browser.View.NinjaToast;
import de.baumann.browser.View.NinjaWebView;
import de.baumann.browser.View.Adapter_Record;
import de.baumann.browser.View.SwipeTouchListener;

import static android.content.ContentValues.TAG;

@SuppressWarnings({"ResultOfMethodCallIgnored", "FieldCanBeLocal", "ApplySharedPref"})
public class BrowserActivity extends AppCompatActivity implements BrowserController, View.OnClickListener {

    // Menus

    private RelativeLayout menu_tabPreview;
    private LinearLayout menu_newTabOpen;
    private LinearLayout menu_closeTab;
    private LinearLayout menu_quit;

    private LinearLayout menu_shareScreenshot;
    private LinearLayout menu_shareLink;
    private LinearLayout menu_sharePDF;
    private LinearLayout menu_openWith;

    private LinearLayout menu_searchSite;
    private LinearLayout menu_settings;
    private LinearLayout menu_download;
    private LinearLayout menu_saveScreenshot;
    private LinearLayout menu_saveBookmark;
    private LinearLayout menu_savePDF;
    private LinearLayout menu_saveStart;
    private LinearLayout menu_help;

    private LinearLayout contextList_newTab;
    private LinearLayout contextList_newTabOpen;
    private LinearLayout contextList_edit;
    private LinearLayout contextList_delete;
    private LinearLayout contextList_fav;

    private View floatButton_tabView;
    private View floatButton_saveView;
    private View floatButton_shareView;
    private View floatButton_moreView;

    private ImageButton tab_next;
    private ImageButton tab_prev;

    private ImageButton fab_tab;
    private ImageButton fab_share;
    private ImageButton fab_save;
    private ImageButton fab_more;
    private ImageButton tab_plus;


    // Views

    private ImageButton searchUp;
    private ImageButton searchDown;
    private ImageButton searchCancel;
    private ImageButton omniboxRefresh;
    private ImageButton omniboxOverflow;
    private ImageButton omniboxOverview;

    private ImageButton open_startPage;
    private ImageButton open_bookmark;
    private ImageButton open_history;
    private ImageButton open_menu;

    private FloatingActionButton fab_imageButtonNav;
    private AutoCompleteTextView inputBox;
    private ProgressBar progressBar;
    private EditText searchBox;
    private BottomSheetDialog bottomSheetDialog;
    private BottomSheetDialog bottomSheetDialog_OverView;
    private NinjaWebView ninjaWebView;
    private ListView listView;
    private TextView omniboxTitle;
    private TextView dialogTitle;
    private View customView;
    private VideoView videoView;

    private HorizontalScrollView tab_ScrollView;

    // Layouts

    private RelativeLayout appBar;
    private RelativeLayout omnibox;
    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;


    // Others

    private String title;
    private String url;
    private String overViewTab;
    private BroadcastReceiver downloadReceiver;

    private SharedPreferences sp;
    private MAHEncryptor mahEncryptor;
    private Javascript javaHosts;
    private Javascript getJavaHosts() {
        return javaHosts;
    }
    private Cookie cookieHosts;
    private Cookie getCookieHosts () {return cookieHosts;}
    private AdBlock adBlock;
    private AdBlock getAdBlock() {
        return adBlock;
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    @SuppressWarnings("SameReturnValue")
    private boolean onKeyCodeBack() {
        hideSoftInput(inputBox);
        hideOverview();

        if (omnibox.getVisibility() == View.GONE) {
            showOmnibox();
        } else if (currentAlbumController == null) {
            finish();
        } else if (currentAlbumController instanceof NinjaWebView) {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack();
            } else if (BrowserContainer.size() <= 1) {
                doubleTapsQuit();
            } else {
                removeAlbum(currentAlbumController);
            }
        } else {
            finish();
        }
        return true;
    }

    private boolean prepareRecord() {
        if (!(currentAlbumController instanceof NinjaWebView)) {
            return true;
        }

        NinjaWebView webView = (NinjaWebView) currentAlbumController;
        String title = webView.getTitle();
        String url = webView.getUrl();
        return (title == null
                || title.isEmpty()
                || url == null
                || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT));
    }

    private int originalOrientation;
    private int shortAnimTime = 0;
    private float dimen156dp;
    private float dimen144dp;
    private float dimen117dp;
    private float dimen108dp;

    private WebChromeClient.CustomViewCallback customViewCallback;
    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;


    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // Classes

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }
    }

    // Overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        WebView.enableSlowWholeDocumentDraw();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        HelperUnit.grantPermissionsStorage(this);
        HelperUnit.setTheme(this);

        setContentView(R.layout.activity_main);

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!§$%&/()=?;:_-.,+#*<>".toCharArray();
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 25; i++) {
                char c = chars[random.nextInt(chars.length)];
                sb.append(c);
            }
            if (Locale.getDefault().getCountry().equals("CN")) {
                sp.edit().putString(getString(R.string.sp_search_engine), "2").apply();
            }
            sp.edit().putString("saved_key", sb.toString()).apply();
            sp.edit().putString("saved_key_ok", "yes").apply();

            sp.edit().putString("setting_gesture_tb_up", "08").apply();
            sp.edit().putString("setting_gesture_tb_down", "01").apply();
            sp.edit().putString("setting_gesture_tb_left", "07").apply();
            sp.edit().putString("setting_gesture_tb_right", "06").apply();

            sp.edit().putString("setting_gesture_nav_up", "04").apply();
            sp.edit().putString("setting_gesture_nav_down", "05").apply();
            sp.edit().putString("setting_gesture_nav_left", "03").apply();
            sp.edit().putString("setting_gesture_nav_right", "02").apply();

            sp.edit().putBoolean(getString(R.string.sp_location), false).apply();
        }
        sp.edit().putInt("restart_changed", 0).apply();

        try {
            mahEncryptor = MAHEncryptor.newInstance(Objects.requireNonNull(sp.getString("saved_key", "")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        contentFrame = findViewById(R.id.main_content);
        appBar = findViewById(R.id.appBar);

        appBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                appBar.getWindowVisibleDisplayFrame(r);
                int screenHeight = appBar.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    omniboxTitle.setVisibility(View.GONE);
                } else {
                    omniboxTitle.setVisibility(View.VISIBLE);
                }
            }
        });

        shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        dimen156dp = getResources().getDimensionPixelSize(R.dimen.layout_width_156dp);
        dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        dimen117dp = getResources().getDimensionPixelSize(R.dimen.layout_height_117dp);
        dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);

        ninjaWebView = (NinjaWebView) currentAlbumController;

        initOmnibox();
        initSearchPanel();
        initOverview();

        if (sp.getBoolean("start_tabStart", true)){
            showOverview();
        }

        new AdBlock(this); // For AdBlock cold boot
        new Javascript(BrowserActivity.this);

        try {
            new Cookie(BrowserActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
            deleteDatabase("Ninja4.db");
            recreate();
        }

        dispatchIntent(getIntent());

        // show changelog

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            final String versionName = pInfo.versionName;
            String oldVersionName = sp.getString("oldVersionName", "0.0");

            if (!Objects.requireNonNull(oldVersionName).equals(versionName)) {

                bottomSheetDialog = new BottomSheetDialog(this);
                View dialogView = View.inflate(this, R.layout.dialog_text, null);

                TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
                dialog_title.setText(R.string.changelog_title);

                TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
                dialog_text.setText(HelperUnit.textSpannable(getString(R.string.changelog_dialog)));
                dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

                ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sp.edit().putString("oldVersionName", versionName).apply();
                        hideBottomSheetDialog ();
                    }
                });

                ImageButton fab_help = dialogView.findViewById(R.id.floatButton_help);
                fab_help.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHelpDialog();
                    }
                });

                ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
                fab_settings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(BrowserActivity.this, Settings_Activity.class);
                        startActivity(intent);
                        hideBottomSheetDialog ();
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(new Runnable() {
            public void run() {
                searchBox.requestFocus();
            }
        }, 500);

        downloadReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_downloadComplete);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                        hideBottomSheetDialog ();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideBottomSheetDialog ();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data == null) {
                // If there is not data, then we may have taken a photo
                if(mCameraPhotoPath != null) {
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
        return;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentUnit.setContext(this);
        dispatchIntent(getIntent());

        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply();
            finish();
        }

        if (sp.getBoolean("pdf_create", false)) {

            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);

            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    hideBottomSheetDialog ();
                }
            });
            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
            action_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideBottomSheetDialog ();
                }
            });
            bottomSheetDialog.setContentView(dialogView);

            final File pathFile = new File(sp.getString("pdf_path", ""));

            if (sp.getBoolean("pdf_share", false)) {

                if (pathFile.exists() && !sp.getBoolean("pdf_delete", false)) {
                    sp.edit().putBoolean("pdf_delete", true).commit();
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, pathFile.getName());
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, pathFile.getName());
                    sharingIntent.setType("*/pdf");
                    Uri bmpUri = Uri.fromFile(pathFile);
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.menu_share)));
                } else if (pathFile.exists() && sp.getBoolean("pdf_delete", false)){
                    pathFile.delete();
                    sp.edit().putBoolean("pdf_create", false).commit();
                    sp.edit().putBoolean("pdf_share", false).commit();
                    sp.edit().putBoolean("pdf_delete", false).commit();
                } else {
                    sp.edit().putBoolean("pdf_create", false).commit();
                    sp.edit().putBoolean("pdf_share", false).commit();
                    sp.edit().putBoolean("pdf_delete", false).commit();
                    textView.setText(R.string.menu_share_pdfToast);
                    bottomSheetDialog.show();
                }

            } else {
                textView.setText(R.string.toast_downloadComplete);
                bottomSheetDialog.show();
                sp.edit().putBoolean("pdf_share", false).commit();
                sp.edit().putBoolean("pdf_create", false).commit();
                sp.edit().putBoolean("pdf_delete", false).commit();
            }
        }

        if (sp.getBoolean("delete_screenshot", false)) {
            File pathFile = new File(sp.getString("screenshot_path", ""));

            if (pathFile.exists()) {
                pathFile.delete();
                sp.edit().putBoolean("delete_screenshot", false).commit();
            }
        }
    }

    @Override
    public void onPause() {
        Intent toHolderService = new Intent(this, HolderService.class);
        IntentUnit.setClear(false);
        stopService(toHolderService);
        inputBox.clearFocus();

        IntentUnit.setContext(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {

        boolean clearIndexedDB = sp.getBoolean(("sp_clearIndexedDB"), false);
        if (clearIndexedDB) {
            BrowserUnit.clearIndexedDB(this);
        }

        Intent toHolderService = new Intent(this, HolderService.class);
        IntentUnit.setClear(true);
        stopService(toHolderService);

        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            Intent toClearService = new Intent(this, ClearService.class);
            startService(toClearService);
        }

        BrowserContainer.clear();
        IntentUnit.setContext(null);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return false;
            case KeyEvent.KEYCODE_MENU:
                return showOverflow();
            case KeyEvent.KEYCODE_BACK:
                // When video fullscreen, first close it
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    return onHideCustomView();
                }
                return onKeyCodeBack();
        }

        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {

        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
            final View av = (View) controller;

            contentFrame.removeAllViews();
            contentFrame.addView(av);
        } else {
            contentFrame.removeAllViews();
            contentFrame.addView((View) controller);
        }

        currentAlbumController = controller;
        currentAlbumController.activate();
        updateOmnibox();
    }

    @Override
    public void updateAutoComplete() {
        RecordAction action = new RecordAction(this);
        action.open(false);
        List<Record> list = action.listBookmarks();
        list.addAll(action.listHistory());
        action.close();

        CompleteAdapter adapter = new CompleteAdapter(this, list);
        inputBox.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        inputBox.setDropDownWidth(ViewUnit.getWindowWidth(this));
        inputBox.setDropDownHorizontalOffset(16);
        inputBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = ((TextView) view.findViewById(R.id.record_item_url)).getText().toString();
                inputBox.setText(url);
                updateAlbum(url);
                hideSoftInput(inputBox);
            }
        });
    }

    @Override
    public void updateBookmarks() {
        RecordAction action = new RecordAction(this);
        action.open(false);
        action.close();
    }

    @Override
    public void updateInputBox(String query) {
        if (query != null) {
            inputBox.setText(query);
        } else {
            inputBox.setText(null);
        }
        inputBox.clearFocus();
    }

    private void showOverview() {
        if (currentAlbumController != null) {
            tab_ScrollView.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
        }
        bottomSheetDialog_OverView.show();
    }

    public void hideOverview () {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.cancel();
        }
    }

    private void hideBottomSheetDialog() {
        if (bottomSheetDialog != null) {
            bottomSheetDialog.cancel();
        }
    }


    @Override
    public void onClick(View v) {

        RecordAction action = new RecordAction(BrowserActivity.this);

        if (currentAlbumController instanceof NinjaWebView) {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            try {
                title = ninjaWebView.getTitle().trim();
                url = ninjaWebView.getUrl().trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        switch (v.getId()) {

            // Menu overflow

            case R.id.tab_prev:
                AlbumController controller = nextAlbumController(false);
                showAlbum(controller);
                updateOverflow();
                break;

            case R.id.tab_next:
                AlbumController controller2 = nextAlbumController(true);
                showAlbum(controller2);
                updateOverflow();
                break;

            case R.id.tab_plus:
                hideBottomSheetDialog();
                hideOverview();
                addAlbum(getString(R.string.album_untitled), sp.getString("favoriteURL", "https://www.startpage.com"), true);
                break;

            case R.id.menu_newTabOpen:
                hideBottomSheetDialog();
                hideOverview();
                addAlbum(getString(R.string.album_untitled), sp.getString("favoriteURL", "https://www.startpage.com"), true);
                break;

            case R.id.menu_closeTab:
                hideBottomSheetDialog ();
                removeAlbum(currentAlbumController);
                break;

            case R.id.menu_tabPreview:
                hideBottomSheetDialog ();
                showOverview();
                break;

            case R.id.menu_quit:
                hideBottomSheetDialog ();
                doubleTapsQuit();
                break;

            case R.id.menu_shareScreenshot:
                hideBottomSheetDialog ();
                sp.edit().putInt("screenshot", 1).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                break;

            case R.id.menu_shareLink:
                hideBottomSheetDialog ();
                if (prepareRecord()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_share_failed));
                } else {
                    IntentUnit.share(BrowserActivity.this, title, url);
                }
                break;

            case R.id.menu_sharePDF:
                hideBottomSheetDialog ();
                printPDF(true);
                break;

            case R.id.menu_openWith:
                hideBottomSheetDialog ();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                startActivity(chooser);
                break;

            case R.id.menu_saveScreenshot:
                hideBottomSheetDialog ();
                sp.edit().putInt("screenshot", 0).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                break;

            case R.id.menu_saveBookmark:
                hideBottomSheetDialog ();
                try {

                    MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(Objects.requireNonNull(sp.getString("saved_key", "")));
                    String encrypted_userName = mahEncryptor.encode("");
                    String encrypted_userPW = mahEncryptor.encode("");

                    BookmarkList db = new BookmarkList(BrowserActivity.this);
                    db.open();
                    if (db.isExist(url)){
                        NinjaToast.show(BrowserActivity.this, R.string.toast_newTitle);
                    } else {
                        db.insert(HelperUnit.secString(ninjaWebView.getTitle()), url, encrypted_userName, encrypted_userPW, "01");
                        NinjaToast.show(BrowserActivity.this, R.string.toast_edit_successful);
                        initBookmarkList();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                }
                break;

            case R.id.menu_saveStart:
                hideBottomSheetDialog ();
                action.open(true);
                if (action.checkGridItem(url)) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_already_exist_in_home));
                } else {
                    Bitmap bitmap = ViewUnit.capture(ninjaWebView, dimen156dp, dimen117dp, Bitmap.Config.ARGB_8888);
                    String filename = System.currentTimeMillis() + BrowserUnit.SUFFIX_PNG;
                    int ordinal = action.listGrid().size();
                    GridItem itemAlbum = new GridItem(title, url, filename, ordinal);

                    if (BrowserUnit.bitmap2File(BrowserActivity.this, bitmap, filename) && action.addGridItem(itemAlbum)) {
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_add_to_home_successful));
                    } else {
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_add_to_home_failed));
                    }
                }
                action.close();
                break;

                // Omnibox

            case R.id.menu_searchSite:
                hideBottomSheetDialog ();
                hideSoftInput(inputBox);
                showSearchPanel();
                break;

            case R.id.contextLink_saveAs:
                hideBottomSheetDialog ();
                printPDF(false);
                break;

            case R.id.menu_settings:
                hideBottomSheetDialog ();
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                break;

            case R.id.menu_help:
                showHelpDialog();
                break;

            case R.id.menu_download:
                hideBottomSheetDialog ();
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                break;

            case R.id.floatButton_tab:
                menu_newTabOpen.setVisibility(View.VISIBLE);
                menu_closeTab.setVisibility(View.VISIBLE);
                menu_tabPreview.setVisibility(View.VISIBLE);
                menu_quit.setVisibility(View.VISIBLE);

                menu_shareScreenshot.setVisibility(View.GONE);
                menu_shareLink.setVisibility(View.GONE);
                menu_sharePDF.setVisibility(View.GONE);
                menu_openWith.setVisibility(View.GONE);

                menu_saveScreenshot.setVisibility(View.GONE);
                menu_saveBookmark.setVisibility(View.GONE);
                menu_savePDF.setVisibility(View.GONE);
                menu_saveStart.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.VISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                menu_searchSite.setVisibility(View.GONE);
                menu_help.setVisibility(View.GONE);
                menu_settings.setVisibility(View.GONE);
                menu_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_share:
                menu_newTabOpen.setVisibility(View.GONE);
                menu_closeTab.setVisibility(View.GONE);
                menu_tabPreview.setVisibility(View.GONE);
                menu_quit.setVisibility(View.GONE);

                menu_shareScreenshot.setVisibility(View.VISIBLE);
                menu_shareLink.setVisibility(View.VISIBLE);
                menu_sharePDF.setVisibility(View.VISIBLE);
                menu_openWith.setVisibility(View.VISIBLE);

                menu_saveScreenshot.setVisibility(View.GONE);
                menu_saveBookmark.setVisibility(View.GONE);
                menu_savePDF.setVisibility(View.GONE);
                menu_saveStart.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.VISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                menu_searchSite.setVisibility(View.GONE);
                menu_help.setVisibility(View.GONE);
                menu_settings.setVisibility(View.GONE);
                menu_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_save:
                menu_newTabOpen.setVisibility(View.GONE);
                menu_closeTab.setVisibility(View.GONE);
                menu_tabPreview.setVisibility(View.GONE);
                menu_quit.setVisibility(View.GONE);

                menu_shareScreenshot.setVisibility(View.GONE);
                menu_shareLink.setVisibility(View.GONE);
                menu_sharePDF.setVisibility(View.GONE);
                menu_openWith.setVisibility(View.GONE);

                menu_saveScreenshot.setVisibility(View.VISIBLE);
                menu_saveBookmark.setVisibility(View.VISIBLE);
                menu_savePDF.setVisibility(View.VISIBLE);
                menu_saveStart.setVisibility(View.VISIBLE);

                menu_searchSite.setVisibility(View.GONE);
                menu_help.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.VISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                menu_settings.setVisibility(View.GONE);
                menu_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_more:
                menu_newTabOpen.setVisibility(View.GONE);
                menu_closeTab.setVisibility(View.GONE);
                menu_tabPreview.setVisibility(View.GONE);
                menu_quit.setVisibility(View.GONE);

                menu_shareScreenshot.setVisibility(View.GONE);
                menu_shareLink.setVisibility(View.GONE);
                menu_sharePDF.setVisibility(View.GONE);
                menu_openWith.setVisibility(View.GONE);

                menu_saveScreenshot.setVisibility(View.GONE);
                menu_saveBookmark.setVisibility(View.GONE);
                menu_savePDF.setVisibility(View.GONE);
                menu_saveStart.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.VISIBLE);

                menu_settings.setVisibility(View.VISIBLE);
                menu_searchSite.setVisibility(View.VISIBLE);
                menu_help.setVisibility(View.VISIBLE);
                menu_download.setVisibility(View.VISIBLE);

                break;

            // Buttons

            case R.id.omnibox_overview:
                showOverview();
                break;

            case R.id.omnibox_refresh:
                if (ninjaWebView.isLoadFinish()) {

                    if (!url.startsWith("https://")) {
                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(R.string.toast_unsecured);
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideBottomSheetDialog ();
                                ninjaWebView.loadUrl(url.replace("http://", "https://"));
                            }
                        });
                        Button action_cancel2 = dialogView.findViewById(R.id.action_cancel);
                        action_cancel2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideBottomSheetDialog ();
                                ninjaWebView.reload();
                            }
                        });
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                    } else {
                        ninjaWebView.reload();
                    }
                } else {
                    ninjaWebView.stopLoading();
                }
                break;

            default:
                break;
        }
    }

    // Methods

    private void printPDF (boolean share) {

        try {
            sp.edit().putBoolean("pdf_create", true).commit();

            if (share) {
                sp.edit().putBoolean("pdf_share", true).commit();
            } else {
                sp.edit().putBoolean("pdf_share", false).commit();
            }

            String title = HelperUnit.fileName(ninjaWebView.getUrl());
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File file = new File(dir, title + ".pdf");
            sp.edit().putString("pdf_path", file.getPath()).apply();

            String pdfTitle = file.getName().replace(".pdf", "");

            PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
            Objects.requireNonNull(printManager).print(pdfTitle, printAdapter, new PrintAttributes.Builder().build());

        } catch (Exception e) {
            sp.edit().putBoolean("pdf_create", false).commit();
            e.printStackTrace();
        }
    }


    private void dispatchIntent(Intent intent) {
        Intent toHolderService = new Intent(this, HolderService.class);
        IntentUnit.setClear(false);
        stopService(toHolderService);

        String action = intent.getAction();

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            // From ActionMode and some others
            pinAlbums(intent.getStringExtra(SearchManager.QUERY));
        } else if (filePathCallback != null) {
            filePathCallback = null;
        } else if ("sc_history".equals(action)) {
            pinAlbums(sp.getString("favoriteURL", "https://www.startpage.com"));
            showOverview();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    open_history.performClick();
                }
            }, 250);
        } else if ("sc_bookmark".equals(action)) {
            pinAlbums(sp.getString("favoriteURL", "https://www.startpage.com"));
            showOverview();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    open_bookmark.performClick();
                }
            }, 250);

        } else if ("sc_startPage".equals(action)) {
            pinAlbums(sp.getString("favoriteURL", "https://www.startpage.com"));
            showOverview();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    open_startPage.performClick();
                }
            }, 250);

        } else if (Intent.ACTION_SEND.equals(action)) {
            pinAlbums(intent.getStringExtra(Intent.EXTRA_TEXT));
        } else {
            pinAlbums(null);
        }
        getIntent().setAction("");
    }

    private void initRendering(View view) {

        if (currentAlbumController instanceof NinjaWebView && sp.getBoolean("sp_invert", false)) {
            Paint paint = new Paint();
            ColorMatrix matrix = new ColorMatrix();
            matrix.set(NEGATIVE_COLOR);
            ColorMatrix gcm = new ColorMatrix();
            gcm.setSaturation(0);
            ColorMatrix concat = new ColorMatrix();
            concat.setConcat(matrix, gcm);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
            paint.setColorFilter(filter);
            // maybe sometime LAYER_TYPE_NONE would better?
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOmnibox() {

        omnibox = findViewById(R.id.main_omnibox);
        inputBox = findViewById(R.id.main_omnibox_input);
        omniboxRefresh = findViewById(R.id.omnibox_refresh);
        omniboxOverview = findViewById(R.id.omnibox_overview);
        omniboxOverflow = findViewById(R.id.omnibox_overflow);
        omniboxTitle = findViewById(R.id.omnibox_title);
        progressBar = findViewById(R.id.main_progress_bar);


        int fab_position = Integer.parseInt(Objects.requireNonNull(sp.getString("nav_position", "0")));

        switch (fab_position) {
            case 0:
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_right);
                break;
            case 1:
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_left);
                break;
            case 2:
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_center);
                break;
            default:
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_right);
                break;
        }

        fab_imageButtonNav.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (currentAlbumController instanceof NinjaWebView) {
                    showFastToggle();
                }
                return false;
            }
        });

        omniboxOverflow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (currentAlbumController instanceof NinjaWebView) {
                    showFastToggle();
                }
                return false;
            }
        });

        if (sp.getBoolean("sp_gestures_use", true)) {
            fab_imageButtonNav.setOnTouchListener(new SwipeTouchListener(BrowserActivity.this) {
                public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
            });

            inputBox.setOnTouchListener(new SwipeTouchListener(BrowserActivity.this) {
                public void onSwipeTop() { performGesture("setting_gesture_tb_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_tb_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_tb_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_tb_left"); }
            });

            fab_imageButtonNav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOverflow();
                }
            });

            omniboxOverflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOverflow();
                }
            });
        }

        inputBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (currentAlbumController == null) { // || !(actionId == EditorInfo.IME_ACTION_DONE)
                    return false;
                }

                String query = inputBox.getText().toString().trim();
                if (query.isEmpty()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                    return true;
                }

                updateAlbum(query);
                hideSoftInput(inputBox);
                return false;
            }
        });

        updateBookmarks();
        updateAutoComplete();

        omniboxRefresh.setOnClickListener(this);
        omniboxOverview.setOnClickListener(this);
    }

    private void performGesture (String gesture) {
        String fab_position = Objects.requireNonNull(sp.getString(gesture, "0"));
        ninjaWebView = (NinjaWebView) currentAlbumController;

        switch (fab_position) {
            case "01":

                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.goForward();
                } else {
                    NinjaToast.show(BrowserActivity.this,R.string.toast_webview_forward);
                }
                break;
            case "03":
                if (ninjaWebView.canGoBack()) {
                    ninjaWebView.goBack();
                } else {
                    removeAlbum(currentAlbumController);
                }
                break;
            case "04":
                ninjaWebView.pageUp(true);
                break;
            case "05":
                ninjaWebView.pageDown(true);
                break;
            case "06":
                AlbumController controller = nextAlbumController(false);
                showAlbum(controller);
                break;
            case "07":
                AlbumController controller2 = nextAlbumController(true);
                showAlbum(controller2);
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.album_untitled), sp.getString("favoriteURL", "https://www.startpage.com"), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
        }
    }

    private void initOverview() {

        bottomSheetDialog_OverView = new BottomSheetDialog(this);
        View dialogView = View.inflate(this, R.layout.dialog_overiew, null);

        open_startPage = dialogView.findViewById(R.id.open_newTab_2);
        open_bookmark = dialogView.findViewById(R.id.open_bookmark_2);
        open_history = dialogView.findViewById(R.id.open_history_2);
        open_menu = dialogView.findViewById(R.id.open_menu);
        tab_container = dialogView.findViewById(R.id.tab_container);
        tab_plus = dialogView.findViewById(R.id.tab_plus);
        tab_ScrollView = dialogView.findViewById(R.id.tab_ScrollView);
        tab_plus.setOnClickListener(this);
        listView = dialogView.findViewById(R.id.home_list_2);

        final Button relayoutOK = dialogView.findViewById(R.id.relayout_ok);
        final DynamicGridView gridView = dialogView.findViewById(R.id.home_grid_2);
        final View open_startPageView = dialogView.findViewById(R.id.open_newTabView);
        final View open_bookmarkView = dialogView.findViewById(R.id.open_bookmarkView);
        final View open_historyView = dialogView.findViewById(R.id.open_historyView);
        final TextView overview_title = dialogView.findViewById(R.id.overview_title);

        final ImageButton overview_prev = dialogView.findViewById(R.id.overview_prev);
        final ImageButton overview_next = dialogView.findViewById(R.id.overview_next);

        gridView.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        open_startPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                open_startPageView.setVisibility(View.VISIBLE);
                open_bookmarkView.setVisibility(View.INVISIBLE);
                open_historyView.setVisibility(View.INVISIBLE);
                overview_title.setText(getString(R.string.album_title_home));
                overview_next.setImageResource(R.drawable.icon_bookmark);
                overview_prev.setImageResource(R.drawable.icon_history);
                overViewTab = getString(R.string.album_title_home);

                RecordAction action = new RecordAction(BrowserActivity.this);
                action.open(false);
                final List<GridItem> gridList = action.listGrid();
                action.close();

                GridAdapter gridAdapter = new de.baumann.browser.View.GridAdapter(BrowserActivity.this, gridList, 2);
                gridView.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();

                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateAlbum(gridList.get(position).getURL());
                        hideOverview();
                    }
                });

                gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        showGridMenu(gridList.get(position));
                        return true;
                    }
                });
            }
        });

        open_bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                open_startPageView.setVisibility(View.INVISIBLE);
                open_bookmarkView.setVisibility(View.VISIBLE);
                open_historyView.setVisibility(View.INVISIBLE);
                overview_title.setText(getString(R.string.album_title_bookmarks));
                overview_next.setImageResource(R.drawable.icon_history);
                overview_prev.setImageResource(R.drawable.icon_earth);
                overViewTab = getString(R.string.album_title_bookmarks);
                sp.edit().putString("filter_passBY", "00").apply();
                initBookmarkList();
            }
        });

        open_bookmark.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                open_bookmark.performClick();
                showFilterDialog();
                return false;
            }
        });

        open_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                open_startPageView.setVisibility(View.INVISIBLE);
                open_bookmarkView.setVisibility(View.INVISIBLE);
                open_historyView.setVisibility(View.VISIBLE);
                overview_title.setText(getString(R.string.album_title_history));
                overview_next.setImageResource(R.drawable.icon_earth);
                overview_prev.setImageResource(R.drawable.icon_bookmark);
                overViewTab = getString(R.string.album_title_history);

                RecordAction action = new RecordAction(BrowserActivity.this);
                action.open(false);
                final List<Record> list;
                list = action.listHistory();
                action.close();

                final Adapter_Record adapter = new Adapter_Record(BrowserActivity.this, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateAlbum(list.get(position).getURL());
                        hideOverview();
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        showListMenu(adapter, list, position);
                        return true;
                    }
                });
            }
        });

        open_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_overview, null);

                menu_settings = dialogView.findViewById(R.id.menu_settings);
                menu_settings.setOnClickListener(BrowserActivity.this);

                menu_quit = dialogView.findViewById(R.id.menu_quit);
                menu_quit.setOnClickListener(BrowserActivity.this);

                LinearLayout tv_relayout = dialogView.findViewById(R.id.tv_relayout);
                LinearLayout bookmark_sort = dialogView.findViewById(R.id.bookmark_sort);
                LinearLayout bookmark_filter = dialogView.findViewById(R.id.bookmark_filter);

                if (overViewTab.equals(getString(R.string.album_title_home))) {
                    tv_relayout.setVisibility(View.VISIBLE);
                } else {
                    tv_relayout.setVisibility(View.GONE);
                }

                if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    bookmark_filter.setVisibility(View.VISIBLE);
                    bookmark_sort.setVisibility(View.VISIBLE);
                } else {
                    bookmark_filter.setVisibility(View.GONE);
                    bookmark_sort.setVisibility(View.GONE);
                }

                bookmark_filter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFilterDialog();
                    }
                });

                bookmark_sort.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideBottomSheetDialog ();
                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_bookmark_sort, null);
                        LinearLayout dialog_sortName = dialogView.findViewById(R.id.dialog_sortName);
                        dialog_sortName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sp.edit().putString("sortDBB", "title").apply();
                                initBookmarkList();
                                hideBottomSheetDialog ();
                            }
                        });
                        LinearLayout dialog_sortIcon = dialogView.findViewById(R.id.dialog_sortIcon);
                        dialog_sortIcon.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sp.edit().putString("sortDBB", "icon").apply();
                                initBookmarkList();
                                hideBottomSheetDialog ();
                            }
                        });
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                    }
                });

                tv_relayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        hideBottomSheetDialog ();
                        omnibox.setVisibility(View.GONE);
                        appBar.setVisibility(View.GONE);
                        omniboxTitle.setVisibility(View.GONE);
                        relayoutOK.setVisibility(View.VISIBLE);

                        final List<GridItem> gridList = ((GridAdapter) gridView.getAdapter()).getList();

                        relayoutOK.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                relayoutOK.setVisibility(View.GONE);

                                RecordAction action = new RecordAction(BrowserActivity.this);
                                action.open(true);
                                action.clearGrid();
                                for (GridItem item : gridList) {
                                    action.addGridItem(item);
                                }
                                action.close();
                                gridView.stopEditMode();
                                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_relayout_successful));
                            }
                        });

                        gridView.setOnDragListener(new DynamicGridView.OnDragListener() {
                            private GridItem dragItem;

                            @Override
                            public void onDragStarted(int position) {
                                dragItem = gridList.get(position);
                            }

                            @Override
                            public void onDragPositionsChanged(int oldPosition, int newPosition) {
                                if (oldPosition < newPosition) {
                                    for (int i = newPosition; i > oldPosition; i--) {
                                        GridItem item = gridList.get(i);
                                        item.setOrdinal(i - 1);
                                    }
                                } else if (oldPosition > newPosition) {
                                    for (int i = newPosition; i < oldPosition; i++) {
                                        GridItem item = gridList.get(i);
                                        item.setOrdinal(i + 1);
                                    }
                                }
                                dragItem.setOrdinal(newPosition);

                                Collections.sort(gridList, new Comparator<GridItem>() {
                                    @Override
                                    public int compare(GridItem first, GridItem second) {
                                        return Integer.compare(first.getOrdinal(), second.getOrdinal());
                                    }
                                });
                            }
                        });
                        gridView.startEditMode();
                    }
                });

                menu_help = dialogView.findViewById(R.id.menu_help);
                menu_help.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHelpDialog();
                    }
                });

                LinearLayout tv_delete = dialogView.findViewById(R.id.tv_delete);
                tv_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideBottomSheetDialog ();
                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView3 = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                        TextView textView = dialogView3.findViewById(R.id.dialog_text);
                        textView.setText(R.string.hint_database);
                        Button action_ok = dialogView3.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                if (overViewTab.equals(getString(R.string.album_title_home))) {
                                    BrowserUnit.clearHome(BrowserActivity.this);
                                    open_startPage.performClick();
                                } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                    File data = Environment.getDataDirectory();
                                    String bookmarksPath_app = "//data//" + getPackageName() + "//databases//pass_DB_v01.db";
                                    final File bookmarkFile_app = new File(data, bookmarksPath_app);
                                    BrowserUnit.deleteDir(bookmarkFile_app);
                                    open_bookmark.performClick();
                                } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                    BrowserUnit.clearHistory(BrowserActivity.this);
                                    open_history.performClick();
                                }
                                hideBottomSheetDialog ();
                                omniboxRefresh.performClick();
                            }
                        });
                        Button action_cancel = dialogView3.findViewById(R.id.action_cancel);
                        action_cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideBottomSheetDialog ();
                            }
                        });
                        bottomSheetDialog.setContentView(dialogView3);
                        bottomSheetDialog.show();
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        bottomSheetDialog_OverView.setContentView(dialogView);

        overview_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overViewTab.equals(getString(R.string.album_title_home))) {
                    open_history.performClick();
                } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    open_startPage.performClick();
                } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                    open_bookmark.performClick();
                }
            }
        });

        overview_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overViewTab.equals(getString(R.string.album_title_home))) {
                    open_bookmark.performClick();
                } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    open_history.performClick();
                } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                    open_startPage.performClick();
                }
            }
        });

        switch (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            case "0":
                open_startPage.performClick();
                break;
            case "3":
                open_bookmark.performClick();
                break;
            case "4":
                open_history.performClick();
                break;
            default:
                open_startPage.performClick();
                break;
        }
    }

    private void initSearchPanel() {
        searchPanel = findViewById(R.id.main_search_panel);
        searchBox = findViewById(R.id.main_search_box);
        searchUp = findViewById(R.id.main_search_up);
        searchDown = findViewById(R.id.main_search_down);
        searchCancel = findViewById(R.id.main_search_cancel);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (currentAlbumController != null) {
                    ((NinjaWebView) currentAlbumController).findAllAsync(s.toString());
                }
            }
        });

        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }

                if (searchBox.getText().toString().isEmpty()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                    return true;
                }
                return false;
            }
        });

        searchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchBox.getText().toString();
                if (query.isEmpty()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                    return;
                }

                hideSoftInput(searchBox);
                if (currentAlbumController instanceof NinjaWebView) {
                    ((NinjaWebView) currentAlbumController).findNext(false);
                }
            }
        });

        searchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchBox.getText().toString();
                if (query.isEmpty()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                    return;
                }

                hideSoftInput(searchBox);
                if (currentAlbumController instanceof NinjaWebView) {
                    ((NinjaWebView) currentAlbumController).findNext(true);
                }
            }
        });

        searchCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSearchPanel();
            }
        });
    }



    private void initBookmarkList() {

        final BookmarkList db = new BookmarkList(this);
        final Cursor row;
        db.open();

        final int layoutStyle = R.layout.list_item_bookmark;
        int[] xml_id = new int[] {
                R.id.record_item_title
        };
        String[] column = new String[] {
                "pass_title",
        };

        String search = sp.getString("filter_passBY", "00");

        if (Objects.requireNonNull(search).equals("00")) {
            row = db.fetchAllData(BrowserActivity.this);
        } else {
            row = db.fetchDataByFilter(search, "pass_creation");
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutStyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                final String bookmarks_icon = row.getString(row.getColumnIndexOrThrow("pass_creation"));

                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = v.findViewById(R.id.ib_icon);
                HelperUnit.switchIcon(BrowserActivity.this, bookmarks_icon, "pass_creation", iv_icon);

                return v;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final String pass_content = row.getString(row.getColumnIndexOrThrow("pass_content"));
                final String pass_icon = row.getString(row.getColumnIndexOrThrow("pass_icon"));
                final String pass_attachment = row.getString(row.getColumnIndexOrThrow("pass_attachment"));
                updateAlbum(pass_content);
                toast_login (pass_icon, pass_attachment);
                hideOverview();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                final String _id = row.getString(row.getColumnIndexOrThrow("_id"));
                final String pass_title = row.getString(row.getColumnIndexOrThrow("pass_title"));
                final String pass_content = row.getString(row.getColumnIndexOrThrow("pass_content"));
                final String pass_icon = row.getString(row.getColumnIndexOrThrow("pass_icon"));
                final String pass_attachment = row.getString(row.getColumnIndexOrThrow("pass_attachment"));
                final String pass_creation = row.getString(row.getColumnIndexOrThrow("pass_creation"));

                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context_list, null);

                contextList_newTab = dialogView.findViewById(R.id.menu_contextList_newTab);
                contextList_newTab.setVisibility(View.VISIBLE);
                contextList_newTab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addAlbum(getString(R.string.album_untitled), pass_content, false);
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                        toast_login (pass_icon, pass_attachment);
                        hideBottomSheetDialog ();
                    }
                });

                contextList_newTabOpen = dialogView.findViewById(R.id.menu_contextList_newTabOpen);
                contextList_newTabOpen.setVisibility(View.VISIBLE);
                contextList_newTabOpen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addAlbum(getString(R.string.album_untitled), pass_content, true);
                        toast_login (pass_icon, pass_attachment);
                        hideBottomSheetDialog ();
                        hideOverview();
                    }
                });

                contextList_fav = dialogView.findViewById(R.id.menu_contextList_fav);
                contextList_fav.setVisibility(View.VISIBLE);
                contextList_fav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideBottomSheetDialog ();
                        sp.edit().putString("favoriteURL", pass_content).apply();
                        NinjaToast.show(BrowserActivity.this, R.string.toast_fav);

                    }
                });

                contextList_edit = dialogView.findViewById(R.id.menu_contextList_edit);
                contextList_edit.setVisibility(View.VISIBLE);
                contextList_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideBottomSheetDialog ();
                        try {

                            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);

                            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit_bookmark, null);

                            final EditText pass_titleET = dialogView.findViewById(R.id.pass_title);
                            final EditText pass_userNameET = dialogView.findViewById(R.id.pass_userName);
                            final EditText pass_userPWET = dialogView.findViewById(R.id.pass_userPW);
                            final EditText pass_URLET = dialogView.findViewById(R.id.pass_url);
                            final ImageView ib_icon = dialogView.findViewById(R.id.ib_icon);

                            final String decrypted_userName = mahEncryptor.decode(pass_icon);
                            final String decrypted_userPW = mahEncryptor.decode(pass_attachment);

                            pass_titleET.setText(pass_title);
                            pass_userNameET.setText(decrypted_userName);
                            pass_userPWET.setText(decrypted_userPW);
                            pass_URLET.setText(pass_content);

                            Button action_ok = dialogView.findViewById(R.id.action_ok);
                            action_ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    try {
                                        String input_pass_title = pass_titleET.getText().toString().trim();
                                        String input_pass_url = pass_URLET.getText().toString().trim();

                                        String encrypted_userName = mahEncryptor.encode(pass_userNameET.getText().toString().trim());
                                        String encrypted_userPW = mahEncryptor.encode(pass_userPWET.getText().toString().trim());

                                        db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), pass_creation);
                                        initBookmarkList();
                                        hideSoftInput(pass_titleET);
                                        NinjaToast.show(BrowserActivity.this, R.string.toast_edit_successful);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                                    }
                                    hideBottomSheetDialog ();
                                }
                            });
                            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                            action_cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    hideSoftInput(pass_titleET);
                                    hideBottomSheetDialog ();
                                }
                            });
                            HelperUnit.switchIcon(BrowserActivity.this, pass_creation, "pass_creation", ib_icon);
                            bottomSheetDialog.setContentView(dialogView);
                            bottomSheetDialog.show();

                            ib_icon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        final String input_pass_title = pass_titleET.getText().toString().trim();
                                        final String input_pass_url = pass_URLET.getText().toString().trim();
                                        final String encrypted_userName = mahEncryptor.encode(pass_userNameET.getText().toString().trim());
                                        final String encrypted_userPW = mahEncryptor.encode(pass_userPWET.getText().toString().trim());

                                        hideBottomSheetDialog ();
                                        hideSoftInput(pass_titleET);

                                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit_icon, null);

                                        LinearLayout icon_01 = dialogView.findViewById(R.id.icon_01);
                                        icon_01.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "01");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_02 = dialogView.findViewById(R.id.icon_02);
                                        icon_02.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "02");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_03 = dialogView.findViewById(R.id.icon_03);
                                        icon_03.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "03");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_04 = dialogView.findViewById(R.id.icon_04);
                                        icon_04.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "04");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_05 = dialogView.findViewById(R.id.icon_05);
                                        icon_05.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "05");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_06 = dialogView.findViewById(R.id.icon_06);
                                        icon_06.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "06");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_07 = dialogView.findViewById(R.id.icon_07);
                                        icon_07.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "07");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_08 = dialogView.findViewById(R.id.icon_08);
                                        icon_08.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "08");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_09 = dialogView.findViewById(R.id.icon_09);
                                        icon_09.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "09");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_10 = dialogView.findViewById(R.id.icon_10);
                                        icon_10.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "10");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });
                                        LinearLayout icon_11 = dialogView.findViewById(R.id.icon_11);
                                        icon_11.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url),  HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), "11");
                                                initBookmarkList();
                                                hideBottomSheetDialog ();
                                            }
                                        });

                                        bottomSheetDialog.setContentView(dialogView);
                                        bottomSheetDialog.show();


                                        NinjaToast.show(BrowserActivity.this, R.string.toast_edit_successful);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        hideBottomSheetDialog ();
                                        NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                                    }
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                        }
                    }
                });

                contextList_delete = dialogView.findViewById(R.id.menu_contextList_delete);
                contextList_delete.setVisibility(View.VISIBLE);
                contextList_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideBottomSheetDialog ();
                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(R.string.toast_titleConfirm_delete);
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                db.delete(Integer.parseInt(_id));
                                initBookmarkList();
                                hideBottomSheetDialog ();
                            }
                        });
                        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                        action_cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideBottomSheetDialog ();
                            }
                        });
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                return true;
            }
        });
    }

    private void showFastToggle() {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_toggle, null);

        CheckBox sw_java = dialogView.findViewById(R.id.switch_js);
        final ImageButton whiteList_js = dialogView.findViewById(R.id.imageButton_js);
        CheckBox sw_adBlock = dialogView.findViewById(R.id.switch_adBlock);
        final ImageButton whiteList_ab = dialogView.findViewById(R.id.imageButton_ab);
        CheckBox sw_image = dialogView.findViewById(R.id.switch_images);
        CheckBox sw_remote = dialogView.findViewById(R.id.switch_remote);
        CheckBox sw_cookie = dialogView.findViewById(R.id.switch_cookie);
        final ImageButton whitelist_cookie = dialogView.findViewById(R.id.imageButton_cookie);
        CheckBox sw_location = dialogView.findViewById(R.id.switch_location);
        CheckBox sw_invert = dialogView.findViewById(R.id.switch_invert);
        CheckBox sw_history = dialogView.findViewById(R.id.switch_history);

        javaHosts = new Javascript(BrowserActivity.this);
        javaHosts = getJavaHosts();

        cookieHosts = new Cookie(BrowserActivity.this);
        cookieHosts = getCookieHosts();

        adBlock = new AdBlock(BrowserActivity.this);
        adBlock = getAdBlock();

        ninjaWebView = (NinjaWebView) currentAlbumController;

        final String url = ninjaWebView.getUrl();

        if (javaHosts.isWhite(url)) {
            whiteList_js.setImageResource(R.drawable.check_green);
        } else {
            whiteList_js.setImageResource(R.drawable.ic_action_close_red);
        }

        if (cookieHosts.isWhite(url)) {
            whitelist_cookie.setImageResource(R.drawable.check_green);
        } else {
            whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
        }

        if (sp.getBoolean(getString(R.string.sp_javascript), true)){
            sw_java.setChecked(true);
        } else {
            sw_java.setChecked(false);
        }

        whiteList_js.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (javaHosts.isWhite(ninjaWebView.getUrl())) {
                    whiteList_js.setImageResource(R.drawable.ic_action_close_red);
                    javaHosts.removeDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                } else {
                    whiteList_js.setImageResource(R.drawable.check_green);
                    javaHosts.addDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                }
            }
        });

        whitelist_cookie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cookieHosts.isWhite(ninjaWebView.getUrl())) {
                    whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
                    cookieHosts.removeDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                } else {
                    whitelist_cookie.setImageResource(R.drawable.check_green);
                    cookieHosts.addDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                }
            }
        });

        sw_java.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_javascript), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_javascript), false).commit();
                }

            }
        });

        if (adBlock.isWhite(url)) {
            whiteList_ab.setImageResource(R.drawable.check_green);
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
        }

        if (sp.getBoolean(getString(R.string.sp_ad_block), true)){
            sw_adBlock.setChecked(true);
        } else {
            sw_adBlock.setChecked(false);
        }

        whiteList_ab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adBlock.isWhite(ninjaWebView.getUrl())) {
                    whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
                    adBlock.removeDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                } else {
                    whiteList_ab.setImageResource(R.drawable.check_green);
                    adBlock.addDomain(Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim());
                }
            }
        });

        sw_adBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), false).commit();
                }
            }
        });

        if (sp.getBoolean(getString(R.string.sp_images), true)){
            sw_image.setChecked(true);
        } else {
            sw_image.setChecked(false);
        }

        sw_image.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_images), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_images), false).commit();
                }
            }
        });

        if (sp.getBoolean(("sp_remote"), true)){
            sw_remote.setChecked(true);
        } else {
            sw_remote.setChecked(false);
        }

        sw_remote.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(("sp_remote"), true).commit();
                }else{
                    sp.edit().putBoolean(("sp_remote"), false).commit();
                }
            }
        });

        if (sp.getBoolean(getString(R.string.sp_cookies), true)){
            sw_cookie.setChecked(true);
        } else {
            sw_cookie.setChecked(false);
        }

        sw_cookie.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_cookies), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_cookies), false).commit();
                }
            }
        });

        if (sp.getBoolean("saveHistory", true)){
            sw_history.setChecked(true);
        } else {
            sw_history.setChecked(false);
        }

        sw_history.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean("saveHistory", true).commit();
                }else{
                    sp.edit().putBoolean("saveHistory", false).commit();
                }
            }
        });

        if (!sp.getBoolean(getString(R.string.sp_location), true)) {
            sw_location.setChecked(false);
        } else {
            sw_location.setChecked(true);
        }

        sw_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_location), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_location), false).commit();
                }
            }
        });

        if (!sp.getBoolean("sp_invert", false)) {
            sw_invert.setChecked(false);
        } else {
            sw_invert.setChecked(true);
        }

        sw_invert.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean("sp_invert", true).commit();
                    initRendering(contentFrame);
                }else{
                    sp.edit().putBoolean("sp_invert", false).commit();
                    initRendering(contentFrame);
                }
            }
        });

        final TextView font_text = dialogView.findViewById(R.id.font_text);
        font_text.setText(sp.getString("sp_fontSize", "100"));

        ImageButton font_minus = dialogView.findViewById(R.id.font_minus);
        font_minus.setImageResource(R.drawable.icon_minus);
        font_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (Objects.requireNonNull(sp.getString("sp_fontSize", "100"))) {
                    case "100":
                        Log.i(TAG, "Can not change font size");
                        break;
                    case "125":
                        sp.edit().putString("sp_fontSize", "100").commit();
                        break;
                    case "150":
                        sp.edit().putString("sp_fontSize", "125").commit();
                        break;
                    case "175":
                        sp.edit().putString("sp_fontSize", "150").commit();
                        break;
                }
                font_text.setText(sp.getString("sp_fontSize", "100"));
            }
        });

        ImageButton font_plus = dialogView.findViewById(R.id.font_plus);
        font_plus.setImageResource(R.drawable.icon_plus);
        font_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (Objects.requireNonNull(sp.getString("sp_fontSize", "100"))) {
                    case "100":
                        sp.edit().putString("sp_fontSize", "125").commit();
                        break;
                    case "125":
                        sp.edit().putString("sp_fontSize", "150").commit();
                        break;
                    case "150":
                        sp.edit().putString("sp_fontSize", "175").commit();
                        break;
                    case "175":
                        Log.i(TAG, "Can not change font size");
                        break;
                }
                font_text.setText(sp.getString("sp_fontSize", "100"));
            }
        });

        Button but_OK = dialogView.findViewById(R.id.action_ok);
        but_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ninjaWebView != null) {
                    hideBottomSheetDialog ();
                    ninjaWebView.initPreferences();
                    ninjaWebView.reload();
                }
            }
        });

        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideBottomSheetDialog ();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void toast_login (String userName, String passWord) {
        try {
            final String decrypted_userName = mahEncryptor.decode(userName);
            final String decrypted_userPW = mahEncryptor.decode(passWord);
            final ClipboardManager clipboard = (ClipboardManager) BrowserActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
            assert clipboard != null;

            final BroadcastReceiver unCopy = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ClipData clip = ClipData.newPlainText("text", decrypted_userName);
                    clipboard.setPrimaryClip(clip);
                    NinjaToast.show(BrowserActivity.this, R.string.toast_copy_successful);
                }
            };

            final BroadcastReceiver pwCopy = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ClipData clip = ClipData.newPlainText("text", decrypted_userPW);
                    clipboard.setPrimaryClip(clip);
                    NinjaToast.show(BrowserActivity.this, R.string.toast_copy_successful);
                }
            };

            IntentFilter intentFilter = new IntentFilter("unCopy");
            BrowserActivity.this.registerReceiver(unCopy, intentFilter);
            Intent copy = new Intent("unCopy");
            PendingIntent copyUN = PendingIntent.getBroadcast(BrowserActivity.this, 0, copy, PendingIntent.FLAG_CANCEL_CURRENT);

            IntentFilter intentFilter2 = new IntentFilter("pwCopy");
            BrowserActivity.this.registerReceiver(pwCopy, intentFilter2);
            Intent copy2 = new Intent("pwCopy");
            PendingIntent copyPW = PendingIntent.getBroadcast(BrowserActivity.this, 1, copy2, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder;

            NotificationManager mNotificationManager = (NotificationManager) BrowserActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
            assert mNotificationManager != null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "browser_not";// The id of the channel.
                CharSequence name = BrowserActivity.this.getString(R.string.app_name);// The user-visible name of the channel.
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
                mNotificationManager.createNotificationChannel(mChannel);
                builder = new NotificationCompat.Builder(BrowserActivity.this, CHANNEL_ID);
            } else {
                //noinspection deprecation
                builder = new NotificationCompat.Builder(BrowserActivity.this);
            }

            NotificationCompat.Action action_UN = new NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pasteUN), copyUN).build();
            NotificationCompat.Action action_PW = new NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pastePW), copyPW).build();

            @SuppressWarnings("deprecation")
            Notification n  = builder
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setSmallIcon(R.drawable.ic_notification_ninja)
                    .setContentTitle(BrowserActivity.this.getString(R.string.app_name))
                    .setContentText(BrowserActivity.this.getString(R.string.toast_titleConfirm_paste))
                    .setColor(ContextCompat.getColor(BrowserActivity.this,R.color.colorAccent))
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(new long[0])
                    .addAction(action_UN)
                    .addAction(action_PW)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;

            if (decrypted_userName.length() > 0 || decrypted_userPW.length() > 0 ) {
                notificationManager.notify(0, n);
            }





        } catch (Exception e) {
            e.printStackTrace();
            NinjaToast.show(BrowserActivity.this, R.string.toast_error);
        }
    }


    private synchronized void addAlbum(String title, final String url, final boolean foreground) {

        showOmnibox();
        ninjaWebView = new NinjaWebView(this);
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title);
        ViewUnit.bound(this, ninjaWebView);

        final View albumView = ninjaWebView.getAlbumView();
        if (currentAlbumController != null) {
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
            tab_container.addView(albumView, index, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        } else {
            BrowserContainer.add(ninjaWebView);
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        if (!foreground) {
            ViewUnit.bound(this, ninjaWebView);
            ninjaWebView.loadUrl(url);
            ninjaWebView.deactivate();
            return;
        }

        showAlbum(ninjaWebView);

        if (url != null && !url.isEmpty()) {
            ninjaWebView.loadUrl(url);
        }
    }

    private synchronized void pinAlbums(String url) {

        showOmnibox();
        hideSoftInput(inputBox);
        hideSearchPanel();
        tab_container.removeAllViews();

        ninjaWebView = new NinjaWebView(this);

        for (AlbumController controller : BrowserContainer.list()) {
            if (controller instanceof NinjaWebView) {
                ((NinjaWebView) controller).setBrowserController(this);
            }
            tab_container.addView(controller.getAlbumView(), LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            controller.getAlbumView().setVisibility(View.VISIBLE);
            controller.deactivate();
        }

        if (BrowserContainer.size() < 1 && url == null) {
            addAlbum("", sp.getString("favoriteURL", "https://www.startpage.com"), true);
        } else if (BrowserContainer.size() >= 1 && url == null) {
            if (currentAlbumController != null) {
                currentAlbumController.activate();
                return;
            }

            int index = BrowserContainer.size() - 1;
            currentAlbumController = BrowserContainer.get(index);
            contentFrame.removeAllViews();
            contentFrame.addView((View) currentAlbumController);
            currentAlbumController.activate();

            updateOmnibox();
        } else { // When url != null
            ninjaWebView.setBrowserController(this);
            ninjaWebView.setAlbumTitle(getString(R.string.album_untitled));
            ViewUnit.bound(this, ninjaWebView);
            ninjaWebView.loadUrl(url);

            BrowserContainer.add(ninjaWebView);
            final View albumView = ninjaWebView.getAlbumView();
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            contentFrame.removeAllViews();
            contentFrame.addView(ninjaWebView);

            if (currentAlbumController != null) {
                currentAlbumController.deactivate();
            }
            currentAlbumController = ninjaWebView;
            currentAlbumController.activate();

            updateOmnibox();
        }
    }

    private synchronized void updateAlbum(String url) {
        if (currentAlbumController == null) {
            return;
        }

        if (currentAlbumController instanceof NinjaWebView) {
            ((NinjaWebView) currentAlbumController).loadUrl(url);
            updateOmnibox();
        } else {
            NinjaToast.show(this, getString(R.string.toast_load_error));
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if(!sp.getBoolean("sp_close_tab_confirm", true)) {
            okAction.run();
        } else {
            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_close_tab);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    okAction.run();
                    hideBottomSheetDialog ();
                }
            });
            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
            action_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideBottomSheetDialog ();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }
    }

    @Override
    public synchronized void removeAlbum(final AlbumController controller) {

        if (BrowserContainer.size() <= 1) {
            doubleTapsQuit();
        } else {
            closeTabConfirmation( new Runnable() {
                @Override
                public void run() {
                    tab_container.removeView(controller.getAlbumView());
                    int index = BrowserContainer.indexOf(controller);
                    BrowserContainer.remove(controller);
                    if (index >= BrowserContainer.size()) {
                        index = BrowserContainer.size() - 1;
                    }
                    showAlbum(BrowserContainer.get(index));
                }
            });
        }
    }

    private void updateOmnibox() {

        initRendering(contentFrame);
        omniboxTitle.setText(currentAlbumController.getAlbumTitle());

        if (currentAlbumController == null) {
            return;
        }

        if (currentAlbumController instanceof NinjaWebView) {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            updateProgress(ninjaWebView.getProgress());
            updateBookmarks();
            scrollChange();
            if (ninjaWebView.getUrl() == null && ninjaWebView.getOriginalUrl() == null) {
                updateInputBox(null);
            } else if (ninjaWebView.getUrl() != null) {
                updateInputBox(ninjaWebView.getUrl());
            } else {
                updateInputBox(ninjaWebView.getOriginalUrl());
            }
        }
        contentFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                currentAlbumController.setAlbumCover(ViewUnit.capture(((View) currentAlbumController), dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            }
        }, shortAnimTime);

    }

    private void scrollChange () {

        if (Objects.requireNonNull(sp.getString("sp_hideToolbar", "0")).equals("0") ||
                Objects.requireNonNull(sp.getString("sp_hideToolbar", "0")).equals("1")) {

            ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(int scrollY, int oldScrollY) {

                    if (Objects.requireNonNull(sp.getString("sp_hideToolbar", "0")).equals("0")) {
                        if (scrollY > oldScrollY) {
                            hideOmnibox();
                        } else if (scrollY < oldScrollY){
                            showOmnibox();
                        }
                    } else if (Objects.requireNonNull(sp.getString("sp_hideToolbar", "0")).equals("1")) {
                        hideOmnibox();
                    }
                }
            });
        }
    }

    @Override
    public synchronized void updateProgress(int progress) {

        progressBar.setProgress(progress);

        updateBookmarks();
        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            updateRefresh(false);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateRefresh(boolean running) {
        if (running) {
            omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(this, R.drawable.ic_action_close));
        } else {
            if (currentAlbumController instanceof NinjaWebView) {
                try {
                    if (ninjaWebView.getUrl().contains("https://")) {
                        omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(this, R.drawable.ic_action_refresh));
                    } else {
                        omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(BrowserActivity.this, R.drawable.icon_alert));
                    }
                } catch (Exception e) {
                    omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(this, R.drawable.ic_action_refresh));
                }
            }
        }
    }


    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {

        if(mFilePathCallback != null) {
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
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Unable to create Image File", ex);
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
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if(takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        originalOrientation = getRequestedOrientation();

        fullscreenHolder = new FullscreenHolder(this);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        customView.setKeepScreenOn(true);
        ((View) currentAlbumController).setVisibility(View.GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
        customViewCallback = callback;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public boolean onHideCustomView() {
        if (customView == null || customViewCallback == null || currentAlbumController == null) {
            return false;
        }

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeView(fullscreenHolder);

        customView.setKeepScreenOn(false);
        ((View) currentAlbumController).setVisibility(View.VISIBLE);
        setCustomFullscreen(false);

        fullscreenHolder = null;
        customView = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null;
        }
        setRequestedOrientation(originalOrientation);
        ninjaWebView.reload();

        return true;
    }

    @Override
    public void onLongPress(final String url) {

        if (url != null) {

            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context_link, null);
            dialogTitle = dialogView.findViewById(R.id.dialog_title);
            dialogTitle.setText(url);

            LinearLayout contextLink_newTab = dialogView.findViewById(R.id.contextLink_newTab);
            contextLink_newTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAlbum(getString(R.string.album_untitled), url, false);
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                    hideBottomSheetDialog ();
                }
            });

            LinearLayout contextLink__shareLink = dialogView.findViewById(R.id.contextLink__shareLink);
            contextLink__shareLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (prepareRecord()) {
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_share_failed));
                    } else {
                        IntentUnit.share(BrowserActivity.this, "", url);
                    }
                    hideBottomSheetDialog ();
                }
            });

            LinearLayout contextLink_openWith = dialogView.findViewById(R.id.contextLink_openWith);
            contextLink_openWith.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                    startActivity(chooser);
                    hideBottomSheetDialog ();
                }
            });

            LinearLayout contextLink_newTabOpen = dialogView.findViewById(R.id.contextLink_newTabOpen);
            contextLink_newTabOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAlbum(getString(R.string.album_untitled), url, true);
                    hideBottomSheetDialog ();
                }
            });

            LinearLayout contextLink_saveAs = dialogView.findViewById(R.id.contextLink_saveAs);
            contextLink_saveAs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        hideBottomSheetDialog ();

                        AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit_extension, null);

                        final EditText editTitle = dialogView.findViewById(R.id.dialog_edit);
                        final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_extension);

                        editTitle.setHint(R.string.dialog_title_hint);
                        editTitle.setText(HelperUnit.fileName(ninjaWebView.getUrl()));

                        builder.setView(dialogView);
                        builder.setTitle(R.string.menu_edit);
                        builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                String title = editTitle.getText().toString().trim();
                                String extension = editExtension.getText().toString().trim();
                                String  filename = title + extension;

                                if (title.isEmpty() || extension.isEmpty() || !extension.startsWith(".")) {
                                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                                } else {

                                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                                        int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                            NinjaToast.show(BrowserActivity.this, R.string.toast_permission_sdCard_sec);
                                        } else {
                                            Uri source = Uri.parse(url);
                                            DownloadManager.Request request = new DownloadManager.Request(source);
                                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                            request.allowScanningByMediaScanner();
                                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                            DownloadManager dm = (DownloadManager) BrowserActivity.this.getSystemService(DOWNLOAD_SERVICE);
                                            assert dm != null;
                                            dm.enqueue(request);
                                            hideSoftInput(editTitle);
                                        }
                                    } else {
                                        Uri source = Uri.parse(url);
                                        DownloadManager.Request request = new DownloadManager.Request(source);
                                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                                        request.allowScanningByMediaScanner();
                                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                        DownloadManager dm = (DownloadManager) BrowserActivity.this.getSystemService(DOWNLOAD_SERVICE);
                                        assert dm != null;
                                        dm.enqueue(request);
                                        hideSoftInput(editTitle);
                                    }
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                                hideSoftInput(editTitle);
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        showSoftInput(editTitle);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }
    }

    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) {
            finish();
        } else {
            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_quit);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
            action_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideBottomSheetDialog ();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }
    }

    private void hideSoftInput(final EditText view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showSoftInput(final EditText view) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }, 250);
    }

    @SuppressLint("RestrictedApi")
    private void showOmnibox() {
        if (omnibox.getVisibility() == View.GONE && searchPanel.getVisibility()  == View.GONE) {

            searchPanel.setVisibility(View.GONE);
            omnibox.setVisibility(View.VISIBLE);
            appBar.setVisibility(View.VISIBLE);

            if (Objects.requireNonNull(sp.getString("sp_hideNav", "0")).equals("0")) {
                fab_imageButtonNav.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void hideOmnibox() {
        if (omnibox.getVisibility() == View.VISIBLE) {

            omnibox.setVisibility(View.GONE);
            searchPanel.setVisibility(View.GONE);
            appBar.setVisibility(View.GONE);

            if (Objects.requireNonNull(sp.getString("sp_hideNav", "0")).equals("0") || Objects.requireNonNull(sp.getString("sp_hideNav", "0")).equals("2")) {
                fab_imageButtonNav.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideSearchPanel() {
        hideSoftInput(searchBox);
        omniboxTitle.setVisibility(View.VISIBLE);
        searchBox.setText("");
        searchPanel.setVisibility(View.GONE);
        omnibox.setVisibility(View.VISIBLE);
    }

    private void showSearchPanel() {
        showOmnibox();
        omnibox.setVisibility(View.GONE);
        omniboxTitle.setVisibility(View.GONE);
        searchPanel.setVisibility(View.VISIBLE);
        showSoftInput(searchBox);
    }

    private void  updateOverflow () {

        new Handler().postDelayed(new Runnable() {
            public void run() {
                dialogTitle.setText(ninjaWebView.getTitle());
            }
        }, 500);

        if (currentAlbumController == null || BrowserContainer.size() <= 1) {
            tab_next.setVisibility(View.GONE);
            tab_prev.setVisibility(View.GONE);
        } else {
            tab_next.setVisibility(View.VISIBLE);
            tab_prev.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("SameReturnValue")
    private boolean showOverflow() {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);

        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu, null);

        fab_tab = dialogView.findViewById(R.id.floatButton_tab);
        fab_tab.setOnClickListener(BrowserActivity.this);
        fab_share = dialogView.findViewById(R.id.floatButton_share);
        fab_share.setOnClickListener(BrowserActivity.this);
        fab_save = dialogView.findViewById(R.id.floatButton_save);
        fab_save.setOnClickListener(BrowserActivity.this);
        fab_more = dialogView.findViewById(R.id.floatButton_more);
        fab_more.setOnClickListener(BrowserActivity.this);

        tab_prev = dialogView.findViewById(R.id.tab_prev);
        tab_prev.setOnClickListener(BrowserActivity.this);
        tab_next = dialogView.findViewById(R.id.tab_next);
        tab_next.setOnClickListener(BrowserActivity.this);

        floatButton_tabView = dialogView.findViewById(R.id.floatButton_tabView);
        floatButton_saveView = dialogView.findViewById(R.id.floatButton_saveView);
        floatButton_shareView = dialogView.findViewById(R.id.floatButton_shareView);
        floatButton_moreView = dialogView.findViewById(R.id.floatButton_moreView);

        dialogTitle = dialogView.findViewById(R.id.dialog_title);

        menu_newTabOpen = dialogView.findViewById(R.id.menu_newTabOpen);
        menu_newTabOpen.setOnClickListener(BrowserActivity.this);
        menu_closeTab = dialogView.findViewById(R.id.menu_closeTab);
        menu_closeTab.setOnClickListener(BrowserActivity.this);
        menu_tabPreview = dialogView.findViewById(R.id.menu_tabPreview);
        menu_tabPreview.setOnClickListener(BrowserActivity.this);
        menu_quit = dialogView.findViewById(R.id.menu_quit);
        menu_quit.setOnClickListener(BrowserActivity.this);

        menu_shareScreenshot = dialogView.findViewById(R.id.menu_shareScreenshot);
        menu_shareScreenshot.setOnClickListener(BrowserActivity.this);
        menu_shareLink = dialogView.findViewById(R.id.menu_shareLink);
        menu_shareLink.setOnClickListener(BrowserActivity.this);
        menu_sharePDF = dialogView.findViewById(R.id.menu_sharePDF);
        menu_sharePDF.setOnClickListener(BrowserActivity.this);
        menu_openWith = dialogView.findViewById(R.id.menu_openWith);
        menu_openWith.setOnClickListener(BrowserActivity.this);

        menu_saveScreenshot = dialogView.findViewById(R.id.menu_saveScreenshot);
        menu_saveScreenshot.setOnClickListener(BrowserActivity.this);
        menu_saveBookmark = dialogView.findViewById(R.id.menu_saveBookmark);
        menu_saveBookmark.setOnClickListener(BrowserActivity.this);
        menu_savePDF = dialogView.findViewById(R.id.contextLink_saveAs);
        menu_savePDF.setOnClickListener(BrowserActivity.this);
        menu_saveStart = dialogView.findViewById(R.id.menu_saveStart);
        menu_saveStart.setOnClickListener(BrowserActivity.this);

        menu_searchSite = dialogView.findViewById(R.id.menu_searchSite);
        menu_searchSite.setOnClickListener(BrowserActivity.this);
        menu_settings = dialogView.findViewById(R.id.menu_settings);
        menu_settings.setOnClickListener(BrowserActivity.this);
        menu_download = dialogView.findViewById(R.id.menu_download);
        menu_download.setOnClickListener(BrowserActivity.this);
        menu_help = dialogView.findViewById(R.id.menu_help);
        menu_help.setOnClickListener(BrowserActivity.this);

        updateOverflow();

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        return true;
    }

    private void showGridMenu(final GridItem gridItem) {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context_list, null);

        contextList_newTab = dialogView.findViewById(R.id.menu_contextList_newTab);
        contextList_newTab.setVisibility(View.VISIBLE);
        contextList_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), gridItem.getURL(), false);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                hideBottomSheetDialog ();
            }
        });

        contextList_newTabOpen = dialogView.findViewById(R.id.menu_contextList_newTabOpen);
        contextList_newTabOpen.setVisibility(View.VISIBLE);
        contextList_newTabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), gridItem.getURL(), true);
                hideBottomSheetDialog ();
                hideOverview();
            }
        });

        contextList_delete = dialogView.findViewById(R.id.menu_contextList_delete);
        contextList_delete.setVisibility(View.VISIBLE);
        contextList_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomSheetDialog ();
                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_titleConfirm_delete);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        RecordAction action = new RecordAction(BrowserActivity.this);
                        action.open(true);
                        action.deleteGridItem(gridItem);
                        action.close();
                        BrowserActivity.this.deleteFile(gridItem.getFilename());
                        open_startPage.performClick();
                        hideBottomSheetDialog ();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideBottomSheetDialog ();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        contextList_edit = dialogView.findViewById(R.id.menu_contextList_edit);
        contextList_edit.setVisibility(View.VISIBLE);
        contextList_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomSheetDialog ();

                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit_title, null);

                final EditText editText = dialogView.findViewById(R.id.dialog_edit);

                editText.setHint(R.string.dialog_title_hint);
                editText.setText(gridItem.getTitle());

                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String text = editText.getText().toString().trim();
                        if (text.isEmpty()) {
                            NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                        } else {
                            RecordAction action = new RecordAction(BrowserActivity.this);
                            action.open(true);
                            gridItem.setTitle(text);
                            action.updateGridItem(gridItem);
                            action.close();
                            hideSoftInput(editText);
                            open_startPage.performClick();
                        }
                        hideBottomSheetDialog ();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideSoftInput(editText);
                        hideBottomSheetDialog ();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }


    private void showListMenu(final Adapter_Record adapterRecord, final List<Record> recordList, final int location) {

        final Record record = recordList.get(location);

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context_list, null);

        contextList_newTab = dialogView.findViewById(R.id.menu_contextList_newTab);
        contextList_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), record.getURL(), false);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                hideBottomSheetDialog ();
            }
        });

        contextList_newTabOpen = dialogView.findViewById(R.id.menu_contextList_newTabOpen);
        contextList_newTabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), record.getURL(), true);
                hideBottomSheetDialog ();
                hideOverview();
            }
        });

        contextList_delete = dialogView.findViewById(R.id.menu_contextList_delete);
        contextList_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomSheetDialog ();
                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_titleConfirm_delete);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        RecordAction action = new RecordAction(BrowserActivity.this);
                        action.open(true);
                        action.deleteHistory(record);
                        action.close();
                        recordList.remove(location);
                        adapterRecord.notifyDataSetChanged();
                        updateBookmarks();
                        updateAutoComplete();
                        hideBottomSheetDialog ();
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_delete_successful));
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideBottomSheetDialog ();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        contextList_edit = dialogView.findViewById(R.id.menu_contextList_edit);
        contextList_fav = dialogView.findViewById(R.id.menu_contextList_fav);

        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            contextList_fav.setVisibility(View.VISIBLE);
            contextList_edit.setVisibility(View.VISIBLE);
        } else {
            contextList_fav.setVisibility(View.GONE);
            contextList_edit.setVisibility(View.GONE);
        }



        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void showFilterDialog () {
        hideBottomSheetDialog();

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit_icon, null);

        LinearLayout icon_01 = dialogView.findViewById(R.id.icon_01);
        icon_01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "01").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_02 = dialogView.findViewById(R.id.icon_02);
        icon_02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "02").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_03 = dialogView.findViewById(R.id.icon_03);
        icon_03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "03").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_04 = dialogView.findViewById(R.id.icon_04);
        icon_04.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "04").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_05 = dialogView.findViewById(R.id.icon_05);
        icon_05.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "05").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_06 = dialogView.findViewById(R.id.icon_06);
        icon_06.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "06").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_07 = dialogView.findViewById(R.id.icon_07);
        icon_07.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "07").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_08 = dialogView.findViewById(R.id.icon_08);
        icon_08.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "08").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_09 = dialogView.findViewById(R.id.icon_09);
        icon_09.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "09").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_10 = dialogView.findViewById(R.id.icon_10);
        icon_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "10").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });
        LinearLayout icon_11 = dialogView.findViewById(R.id.icon_11);
        icon_11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().putString("filter_passBY", "11").apply();
                initBookmarkList();
                hideBottomSheetDialog ();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void showHelpDialog () {

        hideBottomSheetDialog ();
        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_help, null);

        ImageView help_not = dialogView.findViewById(R.id.cardView_help_not);
        ImageView help_nav = dialogView.findViewById(R.id.cardView_help_nav);
        ImageView help_set = dialogView.findViewById(R.id.cardView_help_set);
        ImageView help_start = dialogView.findViewById(R.id.cardView_help_startpage);
        ImageView help_menu = dialogView.findViewById(R.id.cardView_help_menu);
        ImageView help_fastToggle = dialogView.findViewById(R.id.cardView_help_fastToggle);

        help_not.setImageResource(R.drawable.help_not);
        help_nav.setImageResource(R.drawable.help_nav);
        help_set.setImageResource(R.drawable.help_set);
        help_start.setImageResource(R.drawable.help_start);
        help_menu.setImageResource(R.drawable.help_menu);
        help_fastToggle.setImageResource(R.drawable.help_toggle);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(getString(R.string.menu_other_help));

        ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomSheetDialog ();
            }
        });

        ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
        fab_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(intent);
                hideBottomSheetDialog ();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void setCustomFullscreen(boolean fullscreen) {
        View decorView = getWindow().getDecorView();
        if (fullscreen) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) {
            return currentAlbumController;
        }

        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) {
                index = 0;
            }
        } else {
            index--;
            if (index < 0) {
                index = list.size() - 1;
            }
        }

        return list.get(index);
    }
}
