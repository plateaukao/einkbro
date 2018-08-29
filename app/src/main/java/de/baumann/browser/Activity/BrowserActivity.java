package de.baumann.browser.Activity;

import android.Manifest;
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.baumann.browser.Browser.AdBlock;
import de.baumann.browser.Browser.AlbumController;
import de.baumann.browser.Browser.BrowserContainer;
import de.baumann.browser.Browser.BrowserController;
import de.baumann.browser.Browser.Cookie;
import de.baumann.browser.Browser.Javascript;
import de.baumann.browser.Database.Pass;
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
import de.baumann.browser.View.NinjaRelativeLayout;
import de.baumann.browser.View.NinjaToast;
import de.baumann.browser.View.NinjaWebView;
import de.baumann.browser.View.Adapter_Record;
import de.baumann.browser.View.SwipeTouchListener;
import de.baumann.browser.View.SwipeToBoundListener;
import de.baumann.browser.View.SwitcherPanel;

import static android.content.ContentValues.TAG;

@SuppressWarnings({"ResultOfMethodCallIgnored", "FieldCanBeLocal", "ApplySharedPref"})
public class BrowserActivity extends AppCompatActivity implements BrowserController, View.OnClickListener {

    // Menus

    private TextView dialogTitle;

    private LinearLayout tv_new_tabOpen;
    private LinearLayout tv_closeTab;
    private RelativeLayout tv_tabPreview;
    private LinearLayout tv_quit;

    private LinearLayout tv_shareScreenshot;
    private LinearLayout tv_shareLink;
    private LinearLayout tv_menu_save_as;
    private LinearLayout tv_openWith;

    private LinearLayout tv_relayout;
    private LinearLayout tv_searchSite;
    private LinearLayout tv_settings;
    private LinearLayout tv_help;
    private LinearLayout tv_download;
    private LinearLayout tv_placeHolder;
    private LinearLayout tv_placeHolder_2;
    private LinearLayout tv_delete;

    private LinearLayout tv_saveScreenshot;
    private LinearLayout tv_saveBookmark;
    private LinearLayout tv3_menu_save_as;
    private LinearLayout tv_saveStart;
    private LinearLayout tv_saveLogin;

    private LinearLayout tv2_menu_newTab;
    private LinearLayout tv2_menu_newTab_open;
    private LinearLayout tv2_menu_edit;
    private LinearLayout tv2_menu_delete;
    private LinearLayout tv2_menu_notification;

    private LinearLayout floatButton_saveLayout;
    private LinearLayout floatButton_shareLayout;

    private View floatButton_tabView;
    private View floatButton_saveView;
    private View floatButton_shareView;
    private View floatButton_moreView;

    private ImageButton web_next;
    private ImageButton web_prev;
    private ImageButton tab_next;
    private ImageButton tab_prev;

    private ImageButton fab_tab;
    private ImageButton fab_share;
    private ImageButton fab_save;
    private ImageButton fab_more;


    // Views

    private ImageButton searchUp;
    private ImageButton searchDown;
    private ImageButton searchCancel;
    private ImageButton omniboxRefresh;
    private ImageButton omniboxOverflow;
    private ImageButton switcherPlus;

    private FloatingActionButton fab_imageButtonNav;
    private Button relayoutOK;
    private AutoCompleteTextView inputBox;
    private ProgressBar progressBar;
    private EditText searchBox;
    private SwitcherPanel switcherPanel;
    private BottomSheetDialog bottomSheetDialog;
    private HorizontalScrollView switcherScroller;
    private NinjaWebView ninjaWebView;
    private ListView listView;
    private TextView omniboxTitle;
    private View customView;
    private VideoView videoView;

    // Layouts

    private RelativeLayout omnibox;
    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout switcherContainer;
    private NinjaRelativeLayout ninjaRelativeLayout;
    private FrameLayout fullscreenHolder;
    private final ViewGroup nullParent = null;

    // Others

    private String title;
    private String url;
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
        if (switcherPanel.getStatus() != SwitcherPanel.Status.EXPANDED) {
            switcherPanel.expanded();
        } else if (omnibox.getVisibility() == View.GONE) {
            showOmnibox();
        } else if (currentAlbumController == null) {
            finish();
        } else if (currentAlbumController instanceof NinjaWebView) {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack();
            } else {
                removeAlbum(currentAlbumController);
            }
        } else if (currentAlbumController instanceof NinjaRelativeLayout) {
            if (BrowserContainer.size() <= 1) {
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
        if (currentAlbumController == null || !(currentAlbumController instanceof NinjaWebView)) {
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
    private int start_tab;
    private float dimen156dp;
    private float dimen144dp;
    private float dimen117dp;
    private float dimen108dp;
    private float dimen16dp;
    private boolean quit = false;
    private boolean create = true;

    private WebChromeClient.CustomViewCallback customViewCallback;
    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;

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

        HelperUnit.grantPermissionsStorage(this);
        HelperUnit.setTheme(this);

        setContentView(R.layout.main);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        switch (sp.getString("start_tab", "0")) {
            case "0":
                start_tab = BrowserUnit.FLAG_HOME;
                break;
            case "1":
                start_tab = BrowserUnit.FLAG_HOME;
                break;
            case "2":
                start_tab = BrowserUnit.FLAG_PASS;
                break;
            case "3":
                start_tab = BrowserUnit.FLAG_BOOKMARKS;
                break;
            default:
                start_tab = BrowserUnit.FLAG_HISTORY;
                break;
        }

        if (sp.getString("saved_key_ok", "no").equals("no")) {
            char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!§$%&/()=?;:_-.,+#*<>".toCharArray();
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 25; i++) {
                char c = chars[random.nextInt(chars.length)];
                sb.append(c);
            }

            if (Locale.getDefault().getLanguage().equals("zh")) {
                sp.edit().putString(getString(R.string.sp_search_engine), "2").apply();
            }

            sp.edit().putString("saved_key", sb.toString()).apply();
            sp.edit().putString("saved_key_ok", "yes").apply();
            sp.edit().putBoolean(getString(R.string.sp_location), false).apply();
        }

        sp.edit().putInt("restart_changed", 0).apply();

        try {
            mahEncryptor = MAHEncryptor.newInstance(sp.getString("saved_key", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }

        contentFrame = findViewById(R.id.main_content);
        create = true;
        shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        switcherPanel = findViewById(R.id.switcher_panel);
        switcherPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = switcherPanel.getRootView().getHeight() - switcherPanel.getHeight();
                if (currentAlbumController != null) {
                    if (heightDiff > 100) {
                        omniboxTitle.setVisibility(View.GONE);
                    } else {
                        omniboxTitle.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        switcherPanel.setStatusListener(new SwitcherPanel.StatusListener() {
            @Override
            public void onCollapsed() {
                inputBox.clearFocus();
                if (currentAlbumController != null) {
                    switcherScroller.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
                }
            }
        });

        dimen156dp = getResources().getDimensionPixelSize(R.dimen.layout_width_156dp);
        dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        dimen117dp = getResources().getDimensionPixelSize(R.dimen.layout_height_117dp);
        dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        dimen16dp = getResources().getDimensionPixelOffset(R.dimen.layout_margin_16dp);

        initSwitcherView();
        initOmnibox();
        initSearchPanel();
        relayoutOK = findViewById(R.id.main_relayout_ok);

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

            if (!oldVersionName.equals(versionName)) {

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
                        bottomSheetDialog.cancel();
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
                        bottomSheetDialog.cancel();
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
                        bottomSheetDialog.cancel();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
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
        if (create) {
            return;
        }

        if (IntentUnit.isDBChange()) {
            updateBookmarks();
            updateAutoComplete();
            IntentUnit.setDBChange(false);
        }

        if (IntentUnit.isSPChange()) {
            for (AlbumController controller : BrowserContainer.list()) {
                if (controller instanceof NinjaWebView) {
                    ((NinjaWebView) controller).initPreferences();
                }
            }
            IntentUnit.setSPChange(false);
        }

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
                    bottomSheetDialog.cancel();
                }
            });
            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
            action_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetDialog.cancel();
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

        dispatchIntent(getIntent());
    }

    @Override
    public void onPause() {
        Intent toHolderService = new Intent(this, HolderService.class);
        IntentUnit.setClear(false);
        stopService(toHolderService);

        create = false;
        inputBox.clearFocus();
        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
            if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_HOME) {
                DynamicGridView gridView = ninjaRelativeLayout.findViewById(R.id.home_grid);
                if (gridView.isEditMode()) {
                    gridView.stopEditMode();
                    relayoutOK.setVisibility(View.GONE);
                    omnibox.setVisibility(View.VISIBLE);
                    initHomeGrid(ninjaRelativeLayout);
                }
            }
        }

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

        boolean exit = true;
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            Intent toClearService = new Intent(this, ClearService.class);
            startService(toClearService);
            exit = false;
        }

        BrowserContainer.clear();
        IntentUnit.setContext(null);
        super.onDestroy();
        if (exit) {
            System.exit(0); // For remove all WebView thread
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        float coverHeight = ViewUnit.getWindowHeight(this) - ViewUnit.getStatusBarHeight(this) - dimen108dp - dimen16dp;
        hideSoftInput(inputBox);
        hideSearchPanel();
        switcherPanel.expanded();
        switcherPanel.setCoverHeight(coverHeight);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
                    omniboxRefresh.performClick();
                }
            }
        }, shortAnimTime);

        super.onConfigurationChanged(newConfig);
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
    public synchronized void showAlbum(AlbumController controller, final boolean expand) {
        if (controller == null || controller == currentAlbumController) {
            switcherPanel.expanded();
            return;
        }

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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (expand) {
                    switcherPanel.expanded();
                }
            }
        }, shortAnimTime);
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

    @Override
    public void onClick(View v) {

        RecordAction action = new RecordAction(BrowserActivity.this);

        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
        } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
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
                showAlbum(controller, false);
                updateOverflow();
                break;

            case R.id.tab_next:
                AlbumController controller2 = nextAlbumController(true);
                showAlbum(controller2, false);
                updateOverflow();
                break;

            case R.id.web_prev:
                if (ninjaWebView.canGoBack()) {
                    ninjaWebView.goBack();
                    updateOverflow();
                } else {
                    bottomSheetDialog.cancel();
                    removeAlbum(currentAlbumController);
                }
                break;

            case R.id.web_next:
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.goForward();
                    updateOverflow();
                } else {
                    bottomSheetDialog.cancel();
                    NinjaToast.show(BrowserActivity.this,R.string.toast_webview_forward);
                }
                break;

            case R.id.tv_new_tabOpen:
                bottomSheetDialog.cancel();
                addAlbum(start_tab);
                break;

            case R.id.tv_closeTab:
                bottomSheetDialog.cancel();
                removeAlbum(currentAlbumController);
                break;

            case R.id.tv_tabPreview:
                bottomSheetDialog.cancel();
                showOmnibox();
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        switcherPanel.collapsed();
                    }
                }, 500);

                break;

            case R.id.tv_quit:
                bottomSheetDialog.cancel();
                doubleTapsQuit();
                break;

            case R.id.tv_shareScreenshot:
                bottomSheetDialog.cancel();
                sp.edit().putInt("screenshot", 1).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                break;

            case R.id.tv_shareLink:
                bottomSheetDialog.cancel();
                if (prepareRecord()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_share_failed));
                } else {
                    IntentUnit.share(BrowserActivity.this, title, url);
                }
                break;

            case R.id.tv_menu_save_as:
                bottomSheetDialog.cancel();
                printPDF(true);
                break;

            case R.id.tv_openWith:
                bottomSheetDialog.cancel();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                startActivity(chooser);
                break;

            case R.id.tv_saveScreenshot:
                bottomSheetDialog.cancel();
                sp.edit().putInt("screenshot", 0).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                break;

            case R.id.tv_saveBookmark:
                bottomSheetDialog.cancel();
                action.open(true);
                if (action.checkBookmark(url)) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_entry_exists));
                } else {
                    action.addBookmark(new Record(title, url, System.currentTimeMillis()));
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_add_bookmark_successful));
                }
                action.close();
                updateBookmarks();
                updateAutoComplete();
                break;

            case R.id.tv_saveStart:
                bottomSheetDialog.cancel();
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

            case R.id.tv_saveLogin:
                bottomSheetDialog.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_login, null);

                final EditText pass_title = dialogView.findViewById(R.id.pass_title);
                final EditText pass_userName = dialogView.findViewById(R.id.pass_userName);
                final EditText pass_userPW = dialogView.findViewById(R.id.pass_userPW);

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        pass_title.setText(ninjaWebView.getTitle());
                        showSoftInput(pass_title);
                    }
                }, 100);

                builder.setView(dialogView);
                builder.setTitle(R.string.menu_edit);
                builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        String input_pass_title = pass_title.getText().toString().trim();

                        try {

                            MAHEncryptor mahEncryptor = MAHEncryptor.newInstance(sp.getString("saved_key", ""));
                            String encrypted_userName = mahEncryptor.encode(pass_userName.getText().toString().trim());
                            String encrypted_userPW = mahEncryptor.encode(pass_userPW.getText().toString().trim());

                            Pass db = new Pass(BrowserActivity.this);
                            db.open();
                            if (db.isExist(HelperUnit.secString(input_pass_title))){
                                NinjaToast.show(BrowserActivity.this, R.string.toast_newTitle);
                            } else {
                                db.insert(input_pass_title, url, encrypted_userName, HelperUnit.secString(encrypted_userPW), String.valueOf(System.currentTimeMillis()));
                                NinjaToast.show(BrowserActivity.this, R.string.toast_edit_successful);
                                hideSoftInput(pass_title);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                        }
                    }
                });
                builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;

                // Omnibox

            case R.id.tv_relayout:
                bottomSheetDialog.cancel();
                omnibox.setVisibility(View.GONE);
                relayoutOK.setVisibility(View.VISIBLE);

                final DynamicGridView gridView = ninjaRelativeLayout.findViewById(R.id.home_grid);
                final List<GridItem> gridList = ((GridAdapter) gridView.getAdapter()).getList();

                relayoutOK.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        relayoutOK.setTextColor(ContextCompat.getColor(BrowserActivity.this, (R.color.colorAccent)));
                        return false;
                    }
                });

                relayoutOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gridView.stopEditMode();
                        relayoutOK.setVisibility(View.GONE);
                        omnibox.setVisibility(View.VISIBLE);

                        RecordAction action = new RecordAction(BrowserActivity.this);
                        action.open(true);
                        action.clearGrid();
                        for (GridItem item : gridList) {
                            action.addGridItem(item);
                        }
                        action.close();
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
                break;

            case R.id.tv_searchSite:
                bottomSheetDialog.cancel();
                hideSoftInput(inputBox);
                showSearchPanel();
                break;

            case R.id.tv3_menu_save_as:
                bottomSheetDialog.cancel();
                printPDF(false);
                break;

            case R.id.tv_settings:
                bottomSheetDialog.cancel();
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                break;

            case R.id.tv_delete:
                bottomSheetDialog.cancel();

                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView3 = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                TextView textView = dialogView3.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                Button action_ok = dialogView3.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
                            tv_searchSite.setVisibility(View.GONE);

                            switch (ninjaRelativeLayout.getFlag()) {
                                case BrowserUnit.FLAG_HOME:
                                    BrowserUnit.clearHome(BrowserActivity.this);
                                    break;
                                case BrowserUnit.FLAG_BOOKMARKS:
                                    BrowserUnit.clearBookmarks(BrowserActivity.this);
                                    break;
                                case BrowserUnit.FLAG_HISTORY:
                                    BrowserUnit.clearHistory(BrowserActivity.this);
                                    break;
                                case BrowserUnit.FLAG_PASS:
                                    deleteDatabase("pass_DB_v01.db");
                                    break;
                            }
                        }
                        bottomSheetDialog.cancel();
                        omniboxRefresh.performClick();
                    }
                });
                Button action_cancel = dialogView3.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView3);
                bottomSheetDialog.show();

                break;

            case R.id.tv_help:
                bottomSheetDialog.cancel();
                showHelpDialog();
                break;

            case R.id.tv_download:
                bottomSheetDialog.cancel();
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                break;

            case R.id.floatButton_tab:
                tv_new_tabOpen.setVisibility(View.VISIBLE);
                tv_closeTab.setVisibility(View.VISIBLE);
                tv_tabPreview.setVisibility(View.VISIBLE);
                tv_quit.setVisibility(View.VISIBLE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_menu_save_as.setVisibility(View.GONE);
                tv_openWith.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv3_menu_save_as.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);
                tv_saveLogin.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.VISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);
                tv_placeHolder.setVisibility(View.GONE);
                tv_placeHolder_2.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_delete.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                tv_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_share:
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.VISIBLE);
                tv_shareLink.setVisibility(View.VISIBLE);
                tv_menu_save_as.setVisibility(View.VISIBLE);
                tv_openWith.setVisibility(View.VISIBLE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv3_menu_save_as.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);
                tv_saveLogin.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.VISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);
                tv_placeHolder.setVisibility(View.GONE);
                tv_placeHolder_2.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_delete.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                tv_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_save:
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_menu_save_as.setVisibility(View.GONE);
                tv_openWith.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.VISIBLE);
                tv_saveBookmark.setVisibility(View.VISIBLE);
                tv3_menu_save_as.setVisibility(View.VISIBLE);
                tv_saveStart.setVisibility(View.VISIBLE);
                tv_saveLogin.setVisibility(View.GONE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.VISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.INVISIBLE);

                tv_placeHolder.setVisibility(View.GONE);
                tv_placeHolder_2.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_delete.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                tv_download.setVisibility(View.GONE);
                break;

            case R.id.floatButton_more:
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_menu_save_as.setVisibility(View.GONE);
                tv_openWith.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv3_menu_save_as.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.INVISIBLE);
                floatButton_saveView.setVisibility(View.INVISIBLE);
                floatButton_shareView.setVisibility(View.INVISIBLE);
                floatButton_moreView.setVisibility(View.VISIBLE);

                tv_settings.setVisibility(View.VISIBLE);
                tv_delete.setVisibility(View.VISIBLE);

                if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
                    tv_searchSite.setVisibility(View.GONE);
                    tv_saveLogin.setVisibility(View.GONE);
                    tv_help.setVisibility(View.VISIBLE);
                    tv_download.setVisibility(View.GONE);

                    if (ninjaRelativeLayout.getFlag() != BrowserUnit.FLAG_PASS) {
                        tv_relayout.setVisibility(View.VISIBLE);
                        tv_placeHolder_2.setVisibility(View.GONE);
                    } else {
                        tv_relayout.setVisibility(View.GONE);
                        tv_placeHolder_2.setVisibility(View.VISIBLE);
                    }
                } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                    tv_searchSite.setVisibility(View.VISIBLE);
                    tv_relayout.setVisibility(View.GONE);
                    tv_placeHolder_2.setVisibility(View.GONE);
                    tv_delete.setVisibility(View.GONE);
                    tv_saveLogin.setVisibility(View.VISIBLE);
                    tv_help.setVisibility(View.GONE);
                    tv_download.setVisibility(View.VISIBLE);
                }

                break;

            // Buttons

            case R.id.fab_imageButtonNav_center:
            case R.id.fab_imageButtonNav_left:
            case R.id.fab_imageButtonNav_right:
                doubleTapsHide();
                break;

            case R.id.switcher_plus:
                addAlbum(start_tab);
                break;

            case R.id.omnibox_refresh:
                if (currentAlbumController == null) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_refresh_failed));
                    return;
                }

                if (currentAlbumController instanceof NinjaWebView) {
                    ninjaWebView = (NinjaWebView) currentAlbumController;
                    ninjaWebView.stopLoading();
                    if (ninjaWebView.isLoadFinish()) {

                        if (!url.startsWith("https://")) {
                            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                            View dialogView2 = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                            TextView textView2 = dialogView2.findViewById(R.id.dialog_text);
                            textView2.setText(R.string.toast_unsecured);
                            Button action_ok2 = dialogView2.findViewById(R.id.action_ok);
                            action_ok2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    bottomSheetDialog.cancel();
                                    ninjaWebView.loadUrl(url.replace("http://", "https://"));
                                }
                            });
                            Button action_cancel2 = dialogView2.findViewById(R.id.action_cancel);
                            action_cancel2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    bottomSheetDialog.cancel();
                                    ninjaWebView.reload();
                                }
                            });
                            bottomSheetDialog.setContentView(dialogView2);
                            bottomSheetDialog.show();
                        } else {
                            ninjaWebView.reload();
                        }


                    } else {
                        ninjaWebView.stopLoading();
                    }
                } else if (currentAlbumController instanceof NinjaRelativeLayout) {
                    ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
                    initHomeGrid(ninjaRelativeLayout);
                } else {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_refresh_failed));
                }
                break;

            case R.id.omnibox_overflow:
                showOverflow();
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
            addAlbum(BrowserUnit.FLAG_HISTORY);
        } else if ("sc_bookmark".equals(action)) {
            addAlbum(BrowserUnit.FLAG_BOOKMARKS);
        } else if ("sc_login".equals(action)) {
            addAlbum(BrowserUnit.FLAG_PASS);
        } else if ("sc_startPage".equals(action)) {
            addAlbum(BrowserUnit.FLAG_HOME);
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

    private void initSwitcherView() {
        switcherScroller = findViewById(R.id.switcher_scroller);
        switcherContainer = findViewById(R.id.switcher_container);
        switcherPlus = findViewById(R.id.switcher_plus);
        switcherPlus.setOnClickListener(this);
        ninjaWebView = (NinjaWebView) currentAlbumController;
    }

    private void initOmnibox() {

        omnibox = findViewById(R.id.main_omnibox);
        inputBox = findViewById(R.id.main_omnibox_input);
        omniboxRefresh = findViewById(R.id.omnibox_refresh);
        omniboxOverflow = findViewById(R.id.omnibox_overflow);
        omniboxTitle = findViewById(R.id.omnibox_title);
        progressBar = findViewById(R.id.main_progress_bar);

        int fab_position = Integer.parseInt(sp.getString("nav_position", "0"));

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
                    showSwitcher();
                }
                return false;
            }
        });

        fab_imageButtonNav.setOnClickListener(this);
        fab_imageButtonNav.setOnTouchListener(new SwipeTouchListener(BrowserActivity.this) {

            public void onSwipeTop() {
                ninjaWebView = (NinjaWebView) currentAlbumController;
                ninjaWebView.pageUp(true);
            }

            public void onSwipeBottom() {
                ninjaWebView = (NinjaWebView) currentAlbumController;
                ninjaWebView.pageDown(true);
            }

            public void onSwipeRight() {
                ninjaWebView = (NinjaWebView) currentAlbumController;
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.goForward();
                } else {
                    NinjaToast.show(BrowserActivity.this,R.string.toast_webview_forward);
                }
            }

            public void onSwipeLeft() {
                if (ninjaWebView.canGoBack()) {
                    ninjaWebView.goBack();
                } else {
                    removeAlbum(currentAlbumController);
                }
            }
        });

        inputBox.setOnTouchListener(new SwipeToBoundListener(omnibox, new SwipeToBoundListener.BoundCallback() {
            private final KeyListener keyListener = inputBox.getKeyListener();

            @Override
            public boolean canSwipe() {
                boolean ob = sp.getBoolean(getString(R.string.sp_omnibox_control), true);
                return switcherPanel.isKeyBoardShowing() && ob;
            }

            @Override
            public void onSwipe() {
                inputBox.setKeyListener(null);
                inputBox.setFocusable(false);
                inputBox.setFocusableInTouchMode(false);
                inputBox.clearFocus();
            }

            @Override
            public void onBound(boolean canSwitch, boolean left) {
                inputBox.setKeyListener(keyListener);
                inputBox.setFocusable(true);
                inputBox.setFocusableInTouchMode(true);
                inputBox.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                inputBox.clearFocus();

                if (canSwitch) {
                    AlbumController controller = nextAlbumController(left);
                    showAlbum(controller, false);
                }
            }
        }));

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
        omniboxOverflow.setOnClickListener(this);

        omniboxOverflow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                    showSwitcher();
                }
                return false;
            }
        });
    }

    private void initHomeGrid(final NinjaRelativeLayout layout) {

        final int current_tab = layout.getFlag();

        if (currentAlbumController != null) {
            AlbumController holder;
            holder = layout;
            showAlbum(holder, true);
            updateOmnibox();
        }

        final DynamicGridView gridView = layout.findViewById(R.id.home_grid);
        final ListView home_list = layout.findViewById(R.id.home_list);
        final View open_newTabView = layout.findViewById(R.id.open_newTabView);
        final View open_passView = layout.findViewById(R.id.open_passView);
        final View open_bookmarkView = layout.findViewById(R.id.open_bookmarkView);
        final View open_historyView = layout.findViewById(R.id.open_historyView);

        ImageButton open_pass = layout.findViewById(R.id.open_pass);
        ImageButton open_newTab = layout.findViewById(R.id.open_newTab);
        ImageButton open_bookmark = layout.findViewById(R.id.open_bookmark);
        ImageButton open_history = layout.findViewById(R.id.open_history);

        if (current_tab == BrowserUnit.FLAG_HOME) {

            open_newTabView.setVisibility(View.VISIBLE);
            open_passView.setVisibility(View.INVISIBLE);
            open_bookmarkView.setVisibility(View.INVISIBLE);
            open_historyView.setVisibility(View.INVISIBLE);

            layout.setAlbumTitle(getString(R.string.album_title_home));
            gridView.setVisibility(View.VISIBLE);
            home_list.setVisibility(View.GONE);
            updateProgress(BrowserUnit.PROGRESS_MAX);
            RecordAction action = new RecordAction(this);
            action.open(false);
            final List<GridItem> gridList = action.listGrid();
            action.close();

            GridAdapter gridAdapter = new de.baumann.browser.View.GridAdapter(this, gridList, 2);
            gridView.setAdapter(gridAdapter);
            gridAdapter.notifyDataSetChanged();

            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    updateAlbum(gridList.get(position).getURL());
                }
            });

            gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    showGridMenu(gridList.get(position));
                    return true;
                }
            });
        } else {

            gridView.setVisibility(View.GONE);
            home_list.setVisibility(View.VISIBLE);

            switch (current_tab) {
                case BrowserUnit.FLAG_BOOKMARKS:
                    open_newTabView.setVisibility(View.INVISIBLE);
                    open_passView.setVisibility(View.INVISIBLE);
                    open_bookmarkView.setVisibility(View.VISIBLE);
                    open_historyView.setVisibility(View.INVISIBLE);
                    layout.setAlbumTitle(getString(R.string.album_title_bookmarks));
                    layout.setFlag(BrowserUnit.FLAG_BOOKMARKS);
                    initBHList(layout);
                    break;
                case BrowserUnit.FLAG_HISTORY:
                    open_newTabView.setVisibility(View.INVISIBLE);
                    open_passView.setVisibility(View.INVISIBLE);
                    open_bookmarkView.setVisibility(View.INVISIBLE);
                    open_historyView.setVisibility(View.VISIBLE);
                    layout.setAlbumTitle(getString(R.string.album_title_history));
                    layout.setFlag(BrowserUnit.FLAG_HISTORY);
                    initBHList(layout);
                    break;
                case BrowserUnit.FLAG_PASS:
                    open_newTabView.setVisibility(View.INVISIBLE);
                    open_passView.setVisibility(View.VISIBLE);
                    open_bookmarkView.setVisibility(View.INVISIBLE);
                    open_historyView.setVisibility(View.INVISIBLE);
                    layout.setAlbumTitle(getString(R.string.album_title_pass));
                    layout.setFlag(BrowserUnit.FLAG_PASS);
                    initPSList(layout);
                    break;
            }
        }

        open_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.setFlag(BrowserUnit.FLAG_PASS);
                layout.setAlbumTitle(getString(R.string.album_title_pass));
                initHomeGrid(layout);
            }
        });

        open_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.setFlag(BrowserUnit.FLAG_HOME);
                layout.setAlbumTitle(getString(R.string.album_title_home));
                initHomeGrid(layout);
            }
        });

        open_bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.setFlag(BrowserUnit.FLAG_BOOKMARKS);
                layout.setAlbumTitle(getString(R.string.album_title_bookmarks));
                initHomeGrid(layout);
            }
        });

        open_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.setFlag(BrowserUnit.FLAG_HISTORY);
                layout.setAlbumTitle(getString(R.string.album_title_history));
                initHomeGrid(layout);
            }
        });
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
                if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
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

    private void initBHList(final NinjaRelativeLayout layout) {

        RecordAction action = new RecordAction(BrowserActivity.this);
        action.open(false);
        final List<Record> list;
        switch (layout.getFlag()) {
            case BrowserUnit.FLAG_BOOKMARKS:
                list = action.listBookmarks();
                Collections.sort(list, new Comparator<Record>() {
                    @Override
                    public int compare(Record first, Record second) {
                        return first.getTitle().compareToIgnoreCase(second.getTitle());
                    }
                });
                break;
            case BrowserUnit.FLAG_HISTORY:
                list = action.listHistory();
                break;
            default:
                list = new ArrayList<>();
                break;
        }
        action.close();

        listView = layout.findViewById(R.id.home_list);

        final Adapter_Record adapter = new Adapter_Record(BrowserActivity.this, list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateAlbum(list.get(position).getURL());
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

    private void initPSList(final NinjaRelativeLayout layout) {

        final Pass db = new Pass(this);
        db.open();

        final int layoutstyle = R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.record_item_title,
                R.id.record_item_url,
        };
        String[] column = new String[] {
                "pass_title",
                "pass_content",
        };

        final Cursor row = db.fetchAllData();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutstyle,row,column, xml_id, 0);

        listView = layout.findViewById(R.id.home_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                final String pass_content = row.getString(row.getColumnIndexOrThrow("pass_content"));
                final String pass_icon = row.getString(row.getColumnIndexOrThrow("pass_icon"));
                final String pass_attachment = row.getString(row.getColumnIndexOrThrow("pass_attachment"));
                updateAlbum(pass_content);
                toast_login (pass_icon, pass_attachment);
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

                bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context, null);

                tv2_menu_newTab = dialogView.findViewById(R.id.tv2_menu_newTab);
                tv2_menu_newTab.setVisibility(View.VISIBLE);
                tv2_menu_newTab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.cancel();
                        addAlbum(getString(R.string.album_untitled), pass_content, false, null);
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                        toast_login (pass_icon, pass_attachment);
                    }
                });

                tv2_menu_newTab_open = dialogView.findViewById(R.id.tv2_menu_newTab_open);
                tv2_menu_newTab_open.setVisibility(View.VISIBLE);
                tv2_menu_newTab_open.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.cancel();
                        pinAlbums(pass_content);
                        toast_login (pass_icon, pass_attachment);
                    }
                });

                tv2_menu_notification = dialogView.findViewById(R.id.tv2_menu_notification);
                tv2_menu_notification.setVisibility(View.VISIBLE);
                tv2_menu_notification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.cancel();
                        toast_login (pass_icon, pass_attachment);
                    }
                });

                tv2_menu_edit = dialogView.findViewById(R.id.tv2_menu_edit);
                tv2_menu_edit.setVisibility(View.VISIBLE);
                tv2_menu_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.cancel();
                        try {

                            AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_login, null);

                            final EditText pass_titleET = dialogView.findViewById(R.id.pass_title);
                            final EditText pass_userNameET = dialogView.findViewById(R.id.pass_userName);
                            final EditText pass_userPWET = dialogView.findViewById(R.id.pass_userPW);

                            final String decrypted_userName = mahEncryptor.decode(pass_icon);
                            final String decrypted_userPW = mahEncryptor.decode(pass_attachment);

                            pass_titleET.setText(pass_title);
                            pass_userNameET.setText(decrypted_userName);
                            pass_userPWET.setText(decrypted_userPW);

                            builder.setView(dialogView);
                            builder.setTitle(R.string.menu_edit);
                            builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {

                                    try {
                                        String input_pass_title = pass_titleET.getText().toString().trim();
                                        String encrypted_userName = mahEncryptor.encode(pass_userNameET.getText().toString().trim());
                                        String encrypted_userPW = mahEncryptor.encode(pass_userPWET.getText().toString().trim());

                                        db.update(Integer.parseInt(_id), HelperUnit.secString(input_pass_title), HelperUnit.secString(pass_content), HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), String.valueOf(System.currentTimeMillis()));
                                        initPSList(layout);
                                        hideSoftInput(pass_titleET);
                                        NinjaToast.show(BrowserActivity.this, R.string.toast_edit_successful);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                    hideSoftInput(pass_titleET);
                                }
                            });

                            final AlertDialog dialog = builder.create();
                            dialog.show();
                            showSoftInput(pass_titleET);

                        } catch (Exception e) {
                            e.printStackTrace();
                            NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                        }
                    }
                });

                tv2_menu_delete = dialogView.findViewById(R.id.tv2_menu_delete);
                tv2_menu_delete.setVisibility(View.VISIBLE);
                tv2_menu_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.cancel();
                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(R.string.toast_titleConfirm_delete);
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                db.delete(Integer.parseInt(_id));
                                initPSList(layout);
                                bottomSheetDialog.cancel();
                            }
                        });
                        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                        action_cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                bottomSheetDialog.cancel();
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

    private void showSwitcher () {

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
                    javaHosts.removeDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                } else {
                    whiteList_js.setImageResource(R.drawable.check_green);
                    javaHosts.addDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                }
            }
        });

        whitelist_cookie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cookieHosts.isWhite(ninjaWebView.getUrl())) {
                    whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
                    cookieHosts.removeDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                } else {
                    whitelist_cookie.setImageResource(R.drawable.check_green);
                    cookieHosts.addDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                }
            }
        });

        sw_java.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_javascript), true).commit();
                    IntentUnit.setSPChange(true);
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_javascript), false).commit();
                    IntentUnit.setSPChange(true);
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
                    adBlock.removeDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                } else {
                    whiteList_ab.setImageResource(R.drawable.check_green);
                    adBlock.addDomain(Uri.parse(url).getHost().replace("www.", "").trim());
                }
            }
        });

        sw_adBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), true).commit();
                    IntentUnit.setSPChange(true);
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), false).commit();
                    IntentUnit.setSPChange(true);
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
                    IntentUnit.setSPChange(true);
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_cookies), false).commit();
                    IntentUnit.setSPChange(false);
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
                    IntentUnit.setSPChange(true);
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_location), false).commit();
                    IntentUnit.setSPChange(true);
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
                switch (sp.getString("sp_fontSize", "100")) {
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
                switch (sp.getString("sp_fontSize", "100")) {
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
                    bottomSheetDialog.cancel();
                    ninjaWebView.reload();
                }
            }
        });

        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
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
            notificationManager.notify(0, n);

        } catch (Exception e) {
            e.printStackTrace();
            NinjaToast.show(BrowserActivity.this, R.string.toast_error);
        }
    }

    private synchronized void addAlbum(int flag) {

        showOmnibox();

        final AlbumController holder;
        NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.main_home, nullParent, false);
        layout.setBrowserController(this);
        layout.setFlag(flag);
        layout.setAlbumTitle(getString(R.string.app_name));
        holder = layout;

        View albumView = holder.getAlbumView();
        BrowserContainer.add(holder);
        switcherContainer.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        showAlbum(holder, true);
        initHomeGrid(layout);
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground, final Message resultMsg) {

        showOmnibox();
        ninjaWebView = new NinjaWebView(this);
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setFlag(BrowserUnit.FLAG_NINJA);
        ninjaWebView.setAlbumTitle(title);
        ViewUnit.bound(this, ninjaWebView);

        final View albumView = ninjaWebView.getAlbumView();
        if (currentAlbumController != null && (currentAlbumController instanceof NinjaWebView) && resultMsg != null) {
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
            switcherContainer.addView(albumView, index, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        } else {
            BrowserContainer.add(ninjaWebView);
            switcherContainer.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        if (!foreground) {
            ViewUnit.bound(this, ninjaWebView);
            ninjaWebView.loadUrl(url);
            ninjaWebView.deactivate();
            return;
        }

        showAlbum(ninjaWebView, true);

        if (url != null && !url.isEmpty()) {
            ninjaWebView.loadUrl(url);
        } else if (resultMsg != null) {
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(ninjaWebView);
            resultMsg.sendToTarget();
        }
    }

    private synchronized void pinAlbums(String url) {
        showOmnibox();
        hideSoftInput(inputBox);
        hideSearchPanel();
        switcherContainer.removeAllViews();

        ninjaWebView = new NinjaWebView(this);

        for (AlbumController controller : BrowserContainer.list()) {
            if (controller instanceof NinjaWebView) {
                ((NinjaWebView) controller).setBrowserController(this);
            } else if (controller instanceof NinjaRelativeLayout) {
                ((NinjaRelativeLayout) controller).setBrowserController(this);
            }
            switcherContainer.addView(controller.getAlbumView(), LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            controller.getAlbumView().setVisibility(View.VISIBLE);
            controller.deactivate();
        }

        if (BrowserContainer.size() < 1 && url == null) {
            addAlbum(start_tab);
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
            ninjaWebView.setFlag(BrowserUnit.FLAG_NINJA);
            ninjaWebView.setAlbumTitle(getString(R.string.album_untitled));
            ViewUnit.bound(this, ninjaWebView);
            ninjaWebView.loadUrl(url);

            BrowserContainer.add(ninjaWebView);
            final View albumView = ninjaWebView.getAlbumView();
            switcherContainer.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        } else if (currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaWebView = new NinjaWebView(this);

            ninjaWebView.setBrowserController(this);
            ninjaWebView.setFlag(BrowserUnit.FLAG_NINJA);
            ninjaWebView.setAlbumTitle(getString(R.string.album_untitled));
            ViewUnit.bound(this, ninjaWebView);

            int index = switcherContainer.indexOfChild(currentAlbumController.getAlbumView());
            currentAlbumController.deactivate();
            switcherContainer.removeView(currentAlbumController.getAlbumView());
            contentFrame.removeAllViews(); ///

            switcherContainer.addView(ninjaWebView.getAlbumView(), index, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            contentFrame.addView(ninjaWebView);
            BrowserContainer.set(ninjaWebView, index);
            currentAlbumController = ninjaWebView;
            ninjaWebView.activate();
            ninjaWebView.loadUrl(url);
            updateOmnibox();

        } else {
            NinjaToast.show(this, getString(R.string.toast_load_error));
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if(!sp.getBoolean("sp_close_tab_confirm", true)) {
            okAction.run();
        } else {
            switcherPanel.expanded();
            bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
            View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_close_tab);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    okAction.run();
                    bottomSheetDialog.cancel();
                }
            });
            Button action_cancel = dialogView.findViewById(R.id.action_cancel);
            action_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetDialog.cancel();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }
    }

    @Override
    public synchronized void removeAlbum(final AlbumController controller) {
        if (currentAlbumController == null || BrowserContainer.size() <= 1) {

            if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                closeTabConfirmation( new Runnable() {
                    @Override
                    public void run() {
                        switcherContainer.removeView(controller.getAlbumView());
                        BrowserContainer.remove(controller);
                        pinAlbums(null);
                    }
                });
            } else {
                doubleTapsQuit();
            }
            return;
        }

        if (controller != currentAlbumController) {
            if (currentAlbumController instanceof NinjaWebView) {
                closeTabConfirmation( new Runnable() {
                    @Override
                    public void run() {
                        switcherContainer.removeView(controller.getAlbumView());
                        BrowserContainer.remove(controller);
                    }
                });
            } else {
                switcherContainer.removeView(controller.getAlbumView());
                BrowserContainer.remove(controller);
            }


        } else {
            if (currentAlbumController instanceof NinjaWebView) {
                closeTabConfirmation( new Runnable() {
                    @Override
                    public void run() {
                        switcherContainer.removeView(controller.getAlbumView());
                        int index = BrowserContainer.indexOf(controller);
                        BrowserContainer.remove(controller);
                        if (index >= BrowserContainer.size()) {
                            index = BrowserContainer.size() - 1;
                        }
                        showAlbum(BrowserContainer.get(index), false);
                    }
                });
            } else {
                switcherContainer.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if (index >= BrowserContainer.size()) {
                    index = BrowserContainer.size() - 1;
                }
                showAlbum(BrowserContainer.get(index), false);
            }
        }
        showOmnibox();
    }

    private void updateOmnibox() {

        initRendering(contentFrame);
        omniboxTitle.setText(currentAlbumController.getAlbumTitle());

        if (currentAlbumController == null) {
            return;
        }

        if (currentAlbumController instanceof NinjaRelativeLayout) {
            fab_imageButtonNav.setVisibility(View.GONE);
            updateProgress(BrowserUnit.PROGRESS_MAX);
            updateBookmarks();
            updateInputBox(null);
        } else if (currentAlbumController instanceof NinjaWebView) {
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
        if (sp.getString("sp_hideToolbar", "0").equals("0") ||
                sp.getString("sp_hideToolbar", "0").equals("1")) {

            ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(int scrollY, int oldScrollY) {

                    if (sp.getString("sp_hideToolbar", "0").equals("0")) {
                        if (scrollY > oldScrollY + 25) {
                            hideOmnibox();
                        } else if (scrollY < oldScrollY){
                            showOmnibox();
                        }
                    } else if (sp.getString("sp_hideToolbar", "0").equals("1")) {
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
            } else if (currentAlbumController instanceof NinjaRelativeLayout) {
                omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(this, R.drawable.ic_action_refresh));
            }
        }
    }


    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        this.filePathCallback = filePathCallback;
        try {
            Intent intent = fileChooserParams.createIntent();
            startActivityForResult(intent, IntentUnit.REQUEST_FILE_21);
        } catch (Exception e) {
            NinjaToast.show(this, getString(R.string.toast_open_file_manager_failed));
        }
    }

    @Override
    public void onCreateView(WebView view, final Message resultMsg) {
        if (resultMsg == null) {
            return;
        }
        switcherPanel.collapsed();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                addAlbum(getString(R.string.album_untitled), null, true, resultMsg);
            }
        }, shortAnimTime);
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // Auto landscape when video shows

    }

    @Override
    public boolean onHideCustomView() {
        if (customView == null || customViewCallback == null || currentAlbumController == null) {
            return false;
        }

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        if (decorView != null) {
            decorView.removeView(fullscreenHolder);
        }

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

        return true;
    }

    @Override
    public void onLongPress(String url) {
        WebView.HitTestResult result;
        if (!(currentAlbumController instanceof NinjaWebView)) {
            return;
        }
        result = ((NinjaWebView) currentAlbumController).getHitTestResult();

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_link, null);

        if (url != null || (result != null && result.getExtra() != null)) {
            if (url == null) {
                url = result.getExtra();
            }
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }

        final String target = url;

        TextView tv_title = dialogView.findViewById(R.id.dialog_title);
        tv_title.setText(url);

        LinearLayout tv3_main_menu_new_tab = dialogView.findViewById(R.id.tv3_main_menu_new_tab);
        tv3_main_menu_new_tab.setVisibility(View.VISIBLE);
        tv3_main_menu_new_tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), target, false, null);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                bottomSheetDialog.cancel();
            }
        });

        LinearLayout tv3_main_menu_share_link = dialogView.findViewById(R.id.tv3_main_menu_share_link);
        tv3_main_menu_share_link.setVisibility(View.VISIBLE);
        tv3_main_menu_share_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prepareRecord()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_share_failed));
                } else {
                    IntentUnit.share(BrowserActivity.this, "", target);
                }
                bottomSheetDialog.cancel();
            }
        });

        LinearLayout tv3_main_menu_open_link = dialogView.findViewById(R.id.tv3_main_menu_open_link);
        tv3_main_menu_open_link.setVisibility(View.VISIBLE);
        tv3_main_menu_open_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(target));
                Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                startActivity(chooser);
                bottomSheetDialog.cancel();
            }
        });

        LinearLayout tv3_main_menu_new_tabOpen = dialogView.findViewById(R.id.tv3_main_menu_new_tabOpen);
        tv3_main_menu_new_tabOpen.setVisibility(View.VISIBLE);
        tv3_main_menu_new_tabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinAlbums(target);
                bottomSheetDialog.cancel();
            }
        });

        tv3_menu_save_as = dialogView.findViewById(R.id.tv3_menu_save_as);
        tv3_menu_save_as.setVisibility(View.VISIBLE);
        tv3_menu_save_as.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    assert target != null;

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
                                        Uri source = Uri.parse(target);
                                        DownloadManager.Request request = new DownloadManager.Request(source);
                                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(target));
                                        request.allowScanningByMediaScanner();
                                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                                        DownloadManager dm = (DownloadManager) BrowserActivity.this.getSystemService(DOWNLOAD_SERVICE);
                                        assert dm != null;
                                        dm.enqueue(request);
                                        hideSoftInput(editTitle);
                                    }
                                } else {
                                    Uri source = Uri.parse(target);
                                    DownloadManager.Request request = new DownloadManager.Request(source);
                                    request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(target));
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
                    bottomSheetDialog.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
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
                    bottomSheetDialog.cancel();
                    switcherPanel.expanded();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
        }
    }

    private void doubleTapsHide() {
        final Timer timer = new Timer();
        if (!quit) {
            quit = true;
            showOverflow();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    quit = false;
                    timer.cancel();
                }
            }, 500);
        } else {
            bottomSheetDialog.cancel();
            fab_imageButtonNav.setVisibility(View.GONE);
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

    private void showOmnibox() {
        if (omnibox.getVisibility() == View.GONE && searchPanel.getVisibility()  == View.GONE) {

            int dpValue = 56; // margin in dips
            float d = getResources().getDisplayMetrics().density;
            int margin = (int)(dpValue * d); // margin in pixels

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentFrame.getLayoutParams();
            params.topMargin = margin;
            params.setMargins(0, margin, 0, 0); //substitute parameters for left, top, right, bottom
            contentFrame.setLayoutParams(params);

            searchPanel.setVisibility(View.GONE);
            omnibox.setVisibility(View.VISIBLE);

            if (sp.getString("sp_hideNav", "0").equals("0")) {
                fab_imageButtonNav.setVisibility(View.GONE);
            }
        }
    }

    private void hideOmnibox() {
        if (omnibox.getVisibility() == View.VISIBLE) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentFrame.getLayoutParams();
            params.topMargin = 0;
            params.setMargins(0, 0, 0, 0); //substitute parameters for left, top, right, bottom
            contentFrame.setLayoutParams(params);

            omnibox.setVisibility(View.GONE);
            searchPanel.setVisibility(View.GONE);

            if (sp.getString("sp_hideNav", "0").equals("0") || sp.getString("sp_hideNav", "0").equals("2")) {
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

        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            floatButton_shareLayout.setVisibility(View.GONE);
            floatButton_saveLayout.setVisibility(View.GONE);
            web_next.setVisibility(View.GONE);
            web_prev.setVisibility(View.GONE);
            showOmnibox();
            dialogTitle.setText(currentAlbumController.getAlbumTitle());
        } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
            floatButton_shareLayout.setVisibility(View.VISIBLE);
            floatButton_saveLayout.setVisibility(View.VISIBLE);

            if (ninjaWebView.canGoBack()) {
                web_prev.setVisibility(View.VISIBLE);
            } else {
                web_prev.setVisibility(View.INVISIBLE);
            }

            if (ninjaWebView.canGoForward()) {
                web_next.setVisibility(View.VISIBLE);
            } else {
                web_next.setVisibility(View.INVISIBLE);
            }

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                        dialogTitle.setText(ninjaWebView.getTitle());
                    }
                }
            }, 500);

        }

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

        floatButton_saveLayout = dialogView.findViewById(R.id.floatButton_saveLayout);
        floatButton_shareLayout = dialogView.findViewById(R.id.floatButton_shareLayout);

        fab_tab = dialogView.findViewById(R.id.floatButton_tab);
        fab_tab.setOnClickListener(BrowserActivity.this);
        fab_share = dialogView.findViewById(R.id.floatButton_share);
        fab_share.setOnClickListener(BrowserActivity.this);
        fab_save = dialogView.findViewById(R.id.floatButton_save);
        fab_save.setOnClickListener(BrowserActivity.this);
        fab_more = dialogView.findViewById(R.id.floatButton_more);
        fab_more.setOnClickListener(BrowserActivity.this);

        web_prev = dialogView.findViewById(R.id.web_prev);
        web_prev.setOnClickListener(BrowserActivity.this);
        web_next = dialogView.findViewById(R.id.web_next);
        web_next.setOnClickListener(BrowserActivity.this);

        tab_prev = dialogView.findViewById(R.id.tab_prev);
        tab_prev.setOnClickListener(BrowserActivity.this);
        tab_next = dialogView.findViewById(R.id.tab_next);
        tab_next.setOnClickListener(BrowserActivity.this);

        floatButton_tabView = dialogView.findViewById(R.id.floatButton_tabView);
        floatButton_saveView = dialogView.findViewById(R.id.floatButton_saveView);
        floatButton_shareView = dialogView.findViewById(R.id.floatButton_shareView);
        floatButton_moreView = dialogView.findViewById(R.id.floatButton_moreView);

        dialogTitle = dialogView.findViewById(R.id.dialog_title);

        tv_new_tabOpen = dialogView.findViewById(R.id.tv_new_tabOpen);
        tv_new_tabOpen.setOnClickListener(BrowserActivity.this);
        tv_closeTab = dialogView.findViewById(R.id.tv_closeTab);
        tv_closeTab.setOnClickListener(BrowserActivity.this);
        tv_tabPreview = dialogView.findViewById(R.id.tv_tabPreview);
        tv_tabPreview.setOnClickListener(BrowserActivity.this);
        tv_quit = dialogView.findViewById(R.id.tv_quit);
        tv_quit.setOnClickListener(BrowserActivity.this);

        tv_shareScreenshot = dialogView.findViewById(R.id.tv_shareScreenshot);
        tv_shareScreenshot.setOnClickListener(BrowserActivity.this);
        tv_shareLink = dialogView.findViewById(R.id.tv_shareLink);
        tv_shareLink.setOnClickListener(BrowserActivity.this);
        tv_menu_save_as = dialogView.findViewById(R.id.tv_menu_save_as);
        tv_menu_save_as.setOnClickListener(BrowserActivity.this);
        tv_openWith = dialogView.findViewById(R.id.tv_openWith);
        tv_openWith.setOnClickListener(BrowserActivity.this);

        tv_saveScreenshot = dialogView.findViewById(R.id.tv_saveScreenshot);
        tv_saveScreenshot.setOnClickListener(BrowserActivity.this);
        tv_saveBookmark = dialogView.findViewById(R.id.tv_saveBookmark);
        tv_saveBookmark.setOnClickListener(BrowserActivity.this);
        tv3_menu_save_as = dialogView.findViewById(R.id.tv3_menu_save_as);
        tv3_menu_save_as.setOnClickListener(BrowserActivity.this);
        tv_saveStart = dialogView.findViewById(R.id.tv_saveStart);
        tv_saveStart.setOnClickListener(BrowserActivity.this);
        tv_saveLogin = dialogView.findViewById(R.id.tv_saveLogin);
        tv_saveLogin.setOnClickListener(BrowserActivity.this);

        tv_relayout = dialogView.findViewById(R.id.tv_relayout);
        tv_relayout.setOnClickListener(BrowserActivity.this);
        tv_searchSite = dialogView.findViewById(R.id.tv_searchSite);
        tv_searchSite.setOnClickListener(BrowserActivity.this);
        tv_settings = dialogView.findViewById(R.id.tv_settings);
        tv_settings.setOnClickListener(BrowserActivity.this);
        tv_delete = dialogView.findViewById(R.id.tv_delete);
        tv_delete.setOnClickListener(BrowserActivity.this);
        tv_help = dialogView.findViewById(R.id.tv_help);
        tv_help.setOnClickListener(BrowserActivity.this);
        tv_download = dialogView.findViewById(R.id.tv_download);
        tv_download.setOnClickListener(BrowserActivity.this);
        tv_placeHolder = dialogView.findViewById(R.id.tv_placeholder);
        tv_placeHolder_2 = dialogView.findViewById(R.id.tv_placeholder_2);

        updateOverflow();

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();

        return true;
    }

    private void showGridMenu(final GridItem gridItem) {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context, null);

        tv2_menu_newTab = dialogView.findViewById(R.id.tv2_menu_newTab);
        tv2_menu_newTab.setVisibility(View.VISIBLE);
        tv2_menu_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), gridItem.getURL(), false, null);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                bottomSheetDialog.cancel();
            }
        });

        tv2_menu_newTab_open = dialogView.findViewById(R.id.tv2_menu_newTab_open);
        tv2_menu_newTab_open.setVisibility(View.VISIBLE);
        tv2_menu_newTab_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinAlbums(gridItem.getURL());
                bottomSheetDialog.cancel();
            }
        });

        tv2_menu_delete = dialogView.findViewById(R.id.tv2_menu_delete);
        tv2_menu_delete.setVisibility(View.VISIBLE);
        tv2_menu_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
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
                        initHomeGrid((NinjaRelativeLayout) currentAlbumController);
                        bottomSheetDialog.cancel();
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_delete_successful));
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        tv2_menu_edit = dialogView.findViewById(R.id.tv2_menu_edit);
        tv2_menu_edit.setVisibility(View.VISIBLE);
        tv2_menu_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
                showEditDialog(gridItem);
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }


    private void showListMenu(final Adapter_Record adapterRecord, final List<Record> recordList, final int location) {

        final Record record = recordList.get(location);

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context, null);

        ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;

        tv2_menu_newTab = dialogView.findViewById(R.id.tv2_menu_newTab);
        tv2_menu_newTab.setVisibility(View.VISIBLE);
        tv2_menu_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), record.getURL(), false, null);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                bottomSheetDialog.cancel();
            }
        });

        tv2_menu_newTab_open = dialogView.findViewById(R.id.tv2_menu_newTab_open);
        tv2_menu_newTab_open.setVisibility(View.VISIBLE);
        tv2_menu_newTab_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinAlbums(record.getURL());
                bottomSheetDialog.cancel();
            }
        });

        tv2_menu_delete = dialogView.findViewById(R.id.tv2_menu_delete);
        tv2_menu_delete.setVisibility(View.VISIBLE);
        tv2_menu_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
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
                        if (currentAlbumController.getFlag() == BrowserUnit.FLAG_BOOKMARKS) {
                            action.deleteBookmark(record);
                        } else if (currentAlbumController.getFlag() == BrowserUnit.FLAG_HISTORY) {
                            action.deleteHistory(record);
                        }
                        action.close();
                        recordList.remove(location);
                        adapterRecord.notifyDataSetChanged();
                        updateBookmarks();
                        updateAutoComplete();
                        bottomSheetDialog.cancel();
                        NinjaToast.show(BrowserActivity.this, getString(R.string.toast_delete_successful));
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
            }
        });

        tv2_menu_edit = dialogView.findViewById(R.id.tv2_menu_edit);
        if (ninjaRelativeLayout.getFlag() != BrowserUnit.FLAG_HISTORY) {
            tv2_menu_edit.setVisibility(View.VISIBLE);
            tv2_menu_edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetDialog.cancel();
                    showEditDialog(adapterRecord, recordList, location);
                }
            });
        }

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void showHelpDialog () {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_help, null);

        ImageView help_logo = dialogView.findViewById(R.id.cardView_help_logo);
        ImageView help_tabs = dialogView.findViewById(R.id.cardView_help_tabs);
        ImageView help_not = dialogView.findViewById(R.id.cardView_help_not);
        ImageView help_nav = dialogView.findViewById(R.id.cardView_help_nav);
        ImageView help_set = dialogView.findViewById(R.id.cardView_help_set);
        ImageView help_start = dialogView.findViewById(R.id.cardView_help_startpage);
        ImageView help_menu = dialogView.findViewById(R.id.cardView_help_menu);
        ImageView help_fastToggle = dialogView.findViewById(R.id.cardView_help_fastToggle);

        help_logo.setImageResource(R.drawable.help_toolbar);
        help_tabs.setImageResource(R.drawable.help_tabs);
        help_not.setImageResource(R.drawable.help_not);
        help_nav.setImageResource(R.drawable.help_nav);
        help_set.setImageResource(R.drawable.help_settings);
        help_start.setImageResource(R.drawable.help_start);
        help_menu.setImageResource(R.drawable.help_menu);
        help_fastToggle.setImageResource(R.drawable.help_toggle);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(getString(R.string.menu_other_help));

        ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
            }
        });

        ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
        fab_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(intent);
                bottomSheetDialog.cancel();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void showEditDialog(final GridItem gridItem) {

        AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit, null);

        final EditText editText = dialogView.findViewById(R.id.dialog_edit);

        editText.setHint(R.string.dialog_title_hint);
        editText.setText(gridItem.getTitle());

        builder.setView(dialogView);
        builder.setTitle(R.string.menu_edit);
        builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

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
                }
            }
        });
        builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
                hideSoftInput(editText);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        showSoftInput(editText);
    }

    private void showEditDialog(final Adapter_Record adapterRecord, List<Record> recordList, int location) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit, null);

        final Record record = recordList.get(location);
        final EditText editText = dialogView.findViewById(R.id.dialog_edit);

        editText.setHint(R.string.dialog_title_hint);
        editText.setText(record.getTitle());

        builder.setView(dialogView);
        builder.setTitle(R.string.menu_edit);
        builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                String text = editText.getText().toString().trim();
                if (text.isEmpty()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                }

                RecordAction action = new RecordAction(BrowserActivity.this);
                action.open(true);
                record.setTitle(text);
                action.updateBookmark(record);
                action.close();

                adapterRecord.notifyDataSetChanged();
                hideSoftInput(editText);
            }
        });
        builder.setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
                hideSoftInput(editText);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        showSoftInput(editText);
    }

    private void setCustomFullscreen(boolean fullscreen) {

        View decorView = getWindow().getDecorView();

        if (fullscreen) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            showOmnibox();
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
