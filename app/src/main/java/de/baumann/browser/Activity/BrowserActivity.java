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
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import com.bumptech.glide.Glide;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.mobapphome.mahencryptorlib.MAHEncryptor;

import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.baumann.browser.Browser.AdBlock;
import de.baumann.browser.Browser.AlbumController;
import de.baumann.browser.Browser.BrowserContainer;
import de.baumann.browser.Browser.BrowserController;
import de.baumann.browser.Browser.Javascript;
import de.baumann.browser.Database.Files;
import de.baumann.browser.Database.Pass;
import de.baumann.browser.Database.Record;
import de.baumann.browser.Database.RecordAction;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Service.ClearService;
import de.baumann.browser.Service.HolderService;
import de.baumann.browser.Task.ScreenshotTask;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.IntentUnit;
import de.baumann.browser.Unit.ViewUnit;
import de.baumann.browser.View.CompleteAdapter;
import de.baumann.browser.View.FullscreenHolder;
import de.baumann.browser.View.GridAdapter;
import de.baumann.browser.View.GridItem;
import de.baumann.browser.View.NinjaRelativeLayout;
import de.baumann.browser.View.NinjaToast;
import de.baumann.browser.View.NinjaWebView;
import de.baumann.browser.View.RecordAdapter;
import de.baumann.browser.View.SwipeToBoundListener;
import de.baumann.browser.View.SwitcherPanel;

import static android.content.ContentValues.TAG;
import static java.lang.String.valueOf;

@SuppressWarnings({"ResultOfMethodCallIgnored", "FieldCanBeLocal"})
public class BrowserActivity extends Activity implements BrowserController, View.OnClickListener {

    // Menus

    private TextView dialogTitle;
    private TextView tv_new_tabOpen;
    private TextView tv_closeTab;
    private TextView tv_tabPreview;
    private TextView tv_quit;

    private TextView tv_shareScreenshot;
    private TextView tv_shareLink;
    private TextView tv_shareClipboard;

    private TextView tv_relayout;
    private TextView tv_searchSite;
    private TextView tv_settings;
    private TextView tv_help;

    private TextView tv_saveScreenshot;
    private TextView tv_saveBookmark;
    private TextView tv_saveStart;
    private TextView tv_saveLogin;


    private TextView tv2_menu_newTab;
    private TextView tv2_menu_newTab_open;
    private TextView tv2_menu_edit;
    private TextView tv2_menu_delete;
    private TextView tv2_menu_notification;
    private TextView tv2_menu_share;

    private FloatingActionButton fab_tab;
    private FloatingActionButton fab_share;
    private FloatingActionButton fab_save;
    private FloatingActionButton fab_more;
    private FloatingActionButton fab_imageButtonNav;

    // Views

    private ImageButton searchUp;
    private ImageButton searchDown;
    private ImageButton searchCancel;
    private ImageButton omniboxRefresh;
    private ImageButton omniboxOverflow;

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

    private SharedPreferences sp;
    private MAHEncryptor mahEncryptor;
    private Javascript javaHosts;
    private Javascript getJavaHosts() {
        return javaHosts;
    }
    private AdBlock adBlock;
    private AdBlock getAdBlock() {
        return adBlock;
    }

    private static String getReadableFileSize(long size) {
        final int BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                } else {
                    suffix = MEGABYTES;
                }
            }
        }
        return valueOf(dec.format(fileSize) + suffix);
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
                showOmnibox();
                updateAlbum();
            }
        } else if (currentAlbumController instanceof NinjaRelativeLayout) {
            switch (currentAlbumController.getFlag()) {
                case BrowserUnit.FLAG_BOOKMARKS:
                    updateAlbum();
                    break;
                case BrowserUnit.FLAG_HISTORY:
                    updateAlbum();
                    break;
                case BrowserUnit.FLAG_FILES:
                    doubleTapsQuit();
                    break;
                case BrowserUnit.FLAG_PASS:
                    doubleTapsQuit();
                    break;
                case BrowserUnit.FLAG_HOME:
                    doubleTapsQuit();
                    break;
                default:
                    finish();
                    break;
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
    private float dimen156dp;
    private float dimen144dp;
    private float dimen117dp;
    private float dimen108dp;
    private float dimen56dp;
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

        WebView.enableSlowWholeDocumentDraw();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        helper_main.grantPermissionsStorage(this);
        helper_main.setTheme(this);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getString("saved_key_ok", "no").equals("no")) {
            char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!§$%&/()=?;:_-.,+#*<>".toCharArray();
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 25; i++) {
                char c = chars[random.nextInt(chars.length)];
                sb.append(c);
            }

            if (Locale.getDefault().getLanguage().equals("zh")) {
                sp.edit().putString(getString(R.string.sp_search_engine), "4").apply();
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

        setContentView(R.layout.main);

        contentFrame = findViewById(R.id.main_content);
        create = true;
        shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        switcherPanel = findViewById(R.id.switcher_panel);
        switcherPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = switcherPanel.getRootView().getHeight() - switcherPanel.getHeight();
                if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                    if (heightDiff > 100) {
                        omniboxTitle.setVisibility(View.GONE);
                    } else {
                        omniboxTitle.setVisibility(View.VISIBLE);
                    }
                } else {
                    omniboxTitle.setVisibility(View.GONE);
                }
            }
        });
        switcherPanel.setStatusListener(new SwitcherPanel.StatusListener() {
            @Override
            public void onCollapsed() {
                inputBox.clearFocus();
            }
        });

        dimen156dp = getResources().getDimensionPixelSize(R.dimen.layout_width_156dp);
        dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        dimen117dp = getResources().getDimensionPixelSize(R.dimen.layout_height_117dp);
        dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        dimen56dp = getResources().getDimensionPixelSize(R.dimen.layout_height_56dp);
        dimen16dp = getResources().getDimensionPixelOffset(R.dimen.layout_margin_16dp);

        initSwitcherView();
        initOmnibox();
        initSearchPanel();
        relayoutOK = findViewById(R.id.main_relayout_ok);

        new AdBlock(this); // For AdBlock cold boot
        new Javascript(BrowserActivity.this);

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
                dialog_text.setText(helper_main.textSpannable(getString(R.string.changelog_dialog)));
                dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

                FloatingActionButton fab = dialogView.findViewById(R.id.floatButton_ok);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sp.edit().putString("oldVersionName", versionName).apply();
                        bottomSheetDialog.cancel();
                    }
                });

                FloatingActionButton fab_help = dialogView.findViewById(R.id.floatButton_help);
                fab_help.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHelpDialog();
                    }
                });

                FloatingActionButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
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

        initRendering(contentFrame);
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

        dispatchIntent(getIntent());

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

        initRendering(contentFrame);
        fab_imageButtonNav.setVisibility(View.GONE);
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
        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
            if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_HOME) {
                DynamicGridView gridView = ninjaRelativeLayout.findViewById(R.id.home_grid);
                if (gridView.isEditMode()) {
                    gridView.stopEditMode();
                    relayoutOK.setVisibility(View.GONE);
                    omnibox.setVisibility(View.VISIBLE);
                }
            }
        }

        hideSoftInput(inputBox);
        hideSearchPanel();
        if (switcherPanel.getStatus() != SwitcherPanel.Status.EXPANDED) {
            switcherPanel.expanded();
        }

        super.onConfigurationChanged(newConfig);

        float coverHeight = ViewUnit.getWindowHeight(this) - ViewUnit.getStatusBarHeight(this) - dimen108dp - dimen16dp - dimen56dp;
        switcherPanel.setCoverHeight(coverHeight);
        switcherPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                switcherPanel.fixKeyBoardShowing(switcherPanel.getHeight());
                switcherPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
            if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_HOME) {
                initHomeGrid(ninjaRelativeLayout);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return showOverflow();
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When video fullscreen, first close it
            if (fullscreenHolder != null || customView != null || videoView != null) {
                return onHideCustomView();
            }
            return onKeyCodeBack();
        }

        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller, final boolean expand, final boolean capture) {
        if (controller == null || controller == currentAlbumController) {
            switcherPanel.expanded();
            return;
        }

        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
            final View rv = (View) currentAlbumController;
            final View av = (View) controller;

            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.album_fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {}

                @Override
                public void onAnimationStart(Animation animation) {
                    contentFrame.removeAllViews();
                    contentFrame.addView(av);
                }
            });
            rv.startAnimation(fadeOut);
        } else {
            contentFrame.removeAllViews();
            contentFrame.addView((View) controller);
        }

        currentAlbumController = controller;
        currentAlbumController.activate();
        switcherScroller.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
        updateOmnibox();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (expand) {
                    switcherPanel.expanded();
                }

                if (capture) {
                    currentAlbumController.setAlbumCover(ViewUnit.capture(((View) currentAlbumController), dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
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

        final CompleteAdapter adapter = new CompleteAdapter(this, list);
        inputBox.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        inputBox.setDropDownWidth(ViewUnit.getWindowWidth(this));
        inputBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = ((TextView) view.findViewById(R.id.complete_item_url)).getText().toString();
                inputBox.setText(url);
                inputBox.setSelection(url.length());
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

        String title = "";
        String url = "";

        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
        } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
            try {
                title = ninjaWebView.getTitle().trim();
                url = ninjaWebView.getUrl().trim();
                ninjaWebView = (NinjaWebView) currentAlbumController;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        switch (v.getId()) {

            // Menu overflow

            case R.id.tv_new_tabOpen:
                addAlbum(BrowserUnit.FLAG_HOME);
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_closeTab:
                removeAlbum(currentAlbumController);
                if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
                    omniboxRefresh.performClick();
                }
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_tabPreview:
                switcherPanel.collapsed();
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_quit:
                finish();
                break;

            case R.id.tv_shareScreenshot:
                sp.edit().putInt("screenshot", 1).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_shareLink:
                if (prepareRecord()) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_share_failed));
                } else {
                    IntentUnit.share(BrowserActivity.this, ninjaWebView.getTitle(), ninjaWebView.getUrl());
                }
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_shareClipboard:
                BrowserUnit.copyURL(BrowserActivity.this, ninjaWebView.getUrl());
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_saveScreenshot:
                sp.edit().putInt("screenshot", 0).apply();
                new ScreenshotTask(BrowserActivity.this, ninjaWebView).execute();
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_saveBookmark:
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
                bottomSheetDialog.cancel();

                break;

            case R.id.tv_saveStart:
                action.open(true);
                if (action.checkGridItem(ninjaWebView.getUrl())) {
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
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_saveLogin:
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
                            if (db.isExist(helper_main.secString(input_pass_title))){
                                NinjaToast.show(BrowserActivity.this, R.string.toast_newTitle);
                            } else {
                                db.insert(input_pass_title, ninjaWebView.getUrl(), encrypted_userName, helper_main.secString(encrypted_userPW), String.valueOf(System.currentTimeMillis()));
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

                bottomSheetDialog.cancel();
                AlertDialog dialog = builder.create();
                dialog.show();
                break;

                // Omnibox

            case R.id.tv_relayout:
                final DynamicGridView gridView = ninjaRelativeLayout.findViewById(R.id.home_grid);
                final List<GridItem> gridList = ((GridAdapter) gridView.getAdapter()).getList();

                omnibox.setVisibility(View.GONE);
                relayoutOK.setVisibility(View.VISIBLE);

                relayoutOK.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            relayoutOK.setTextColor(helper_main.colorAccent(BrowserActivity.this));
                        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                            relayoutOK.setTextColor(ContextCompat.getColor(BrowserActivity.this, (R.color.light)));
                        }
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
                                if (first.getOrdinal() < second.getOrdinal()) {
                                    return -1;
                                } else if (first.getOrdinal() > second.getOrdinal()) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                    }
                });
                gridView.startEditMode();
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_searchSite:
                hideSoftInput(inputBox);
                showSearchPanel();
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_settings:
                Intent intent = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(intent);
                bottomSheetDialog.cancel();
                break;

            case R.id.tv_help:
                bottomSheetDialog.cancel();
                showHelpDialog();
                break;

            case R.id.floatButton_tab:
                dialogTitle.setText(getString(R.string.main_menu));
                tv_new_tabOpen.setVisibility(View.VISIBLE);
                tv_closeTab.setVisibility(View.VISIBLE);
                tv_tabPreview.setVisibility(View.VISIBLE);
                tv_quit.setVisibility(View.VISIBLE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_shareClipboard.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);
                tv_saveLogin.setVisibility(View.GONE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                break;

            case R.id.floatButton_share:
                dialogTitle.setText(getString(R.string.menu_share));
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.VISIBLE);
                tv_shareLink.setVisibility(View.VISIBLE);
                tv_shareClipboard.setVisibility(View.VISIBLE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);
                tv_saveLogin.setVisibility(View.GONE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                break;

            case R.id.floatButton_save:
                dialogTitle.setText(getString(R.string.menu_save));
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_shareClipboard.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.VISIBLE);
                tv_saveBookmark.setVisibility(View.VISIBLE);
                tv_saveStart.setVisibility(View.VISIBLE);
                tv_saveLogin.setVisibility(View.VISIBLE);

                tv_relayout.setVisibility(View.GONE);
                tv_searchSite.setVisibility(View.GONE);
                tv_settings.setVisibility(View.GONE);
                tv_help.setVisibility(View.GONE);
                break;

            case R.id.floatButton_more:
                dialogTitle.setText(getString(R.string.menu_other));
                tv_new_tabOpen.setVisibility(View.GONE);
                tv_closeTab.setVisibility(View.GONE);
                tv_tabPreview.setVisibility(View.GONE);
                tv_quit.setVisibility(View.GONE);

                tv_shareScreenshot.setVisibility(View.GONE);
                tv_shareLink.setVisibility(View.GONE);
                tv_shareClipboard.setVisibility(View.GONE);

                tv_saveScreenshot.setVisibility(View.GONE);
                tv_saveBookmark.setVisibility(View.GONE);
                tv_saveStart.setVisibility(View.GONE);
                tv_saveLogin.setVisibility(View.GONE);

                tv_settings.setVisibility(View.VISIBLE);
                tv_help.setVisibility(View.VISIBLE);

                if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
                    tv_searchSite.setVisibility(View.GONE);
                    if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_HOME) {
                        tv_relayout.setVisibility(View.VISIBLE);
                    } else {
                        tv_relayout.setVisibility(View.GONE);
                    }
                } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
                    tv_searchSite.setVisibility(View.VISIBLE);
                    tv_relayout.setVisibility(View.GONE);
                }

                break;

            // Buttons

            case R.id.fab_imageButtonNav_center:
            case R.id.fab_imageButtonNav_left:
            case R.id.fab_imageButtonNav_right:
                doubleTapsHide();
                break;

            case R.id.omnibox_refresh:
                if (currentAlbumController == null) {
                    NinjaToast.show(BrowserActivity.this, getString(R.string.toast_refresh_failed));
                    return;
                }

                if (currentAlbumController instanceof NinjaWebView) {
                    ninjaWebView = (NinjaWebView) currentAlbumController;
                    if (ninjaWebView.isLoadFinish()) {
                        ninjaWebView.reload();
                    } else {
                        ninjaWebView.stopLoading();
                    }
                } else if (currentAlbumController instanceof NinjaRelativeLayout) {
                    ninjaRelativeLayout = (NinjaRelativeLayout) currentAlbumController;
                    if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_HOME) {
                        initHomeGrid(ninjaRelativeLayout);
                        return;
                    }
                    if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_FILES) {
                        sp.edit().putString("files_startFolder", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()).apply();
                        initFEList(ninjaRelativeLayout);
                        return;
                    }
                    if (ninjaRelativeLayout.getFlag() == BrowserUnit.FLAG_PASS) {
                        initPSList(ninjaRelativeLayout);
                        return;
                    }
                    initBHList(ninjaRelativeLayout, true);

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

    private void dispatchIntent(Intent intent) {
        Intent toHolderService = new Intent(this, HolderService.class);
        IntentUnit.setClear(false);
        stopService(toHolderService);

        String action = intent.getAction();

        if (intent.hasExtra(IntentUnit.OPEN)) { // From HolderActivity's menu
            pinAlbums(intent.getStringExtra(IntentUnit.OPEN));
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) { // From ActionMode and some others
            pinAlbums(intent.getStringExtra(SearchManager.QUERY));
        } else if (filePathCallback != null) {
            filePathCallback = null;
        } else if ("sc_history".equals(action)) {
            addAlbum(BrowserUnit.FLAG_HISTORY);
        } else if ("sc_bookmark".equals(action)) {
            addAlbum(BrowserUnit.FLAG_BOOKMARKS);
        } else if ("sc_login".equals(action)) {
            addAlbum(BrowserUnit.FLAG_PASS);
        } else if ("sc_files".equals(action)) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    NinjaToast.show(BrowserActivity.this, R.string.toast_permission_sdCard_sec);
                } else {
                    addAlbum(BrowserUnit.FLAG_FILES);
                }
            } else {
                addAlbum(BrowserUnit.FLAG_FILES);
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            pinAlbums(intent.getStringExtra(Intent.EXTRA_TEXT));
        } else {
            pinAlbums(null);
        }
    }

    private void initRendering(View view) {

        if (sp.getBoolean("sp_invert", false)) {

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
        fab_imageButtonNav.setOnTouchListener(new OnSwipeTouchListener(BrowserActivity.this) {

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
                    NinjaToast.show(BrowserActivity.this,R.string.toast_webview_back);
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
                    showAlbum(controller, false, true);
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

        if (sp.getBoolean("sp_exit", true)) {
            omniboxRefresh.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    finish();
                    return false;
                }
            });
        }

        omniboxOverflow.setOnClickListener(this);

        if (sp.getBoolean("sp_toggle", true)) {
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

        ImageButton menu_newTab = findViewById(R.id.open_newTab);
        menu_newTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlbum(BrowserUnit.FLAG_HOME);
            }
        });
        ImageButton menu_files = findViewById(R.id.open_files);
        menu_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        NinjaToast.show(BrowserActivity.this, R.string.toast_permission_sdCard_sec);
                    } else {
                        addAlbum(BrowserUnit.FLAG_FILES);
                    }
                } else {
                    addAlbum(BrowserUnit.FLAG_FILES);
                }
            }
        });
        ImageButton menu_pass = findViewById(R.id.open_pass);
        menu_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlbum(BrowserUnit.FLAG_PASS);
            }
        });
        ImageButton menu_bookmarks = findViewById(R.id.open_bookmark);
        menu_bookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlbum(BrowserUnit.FLAG_BOOKMARKS);
            }
        });
        ImageButton menu_history = findViewById(R.id.open_history);
        menu_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlbum(BrowserUnit.FLAG_HISTORY);
            }
        });
    }

    private void initHomeGrid(final NinjaRelativeLayout layout) {

        updateProgress(BrowserUnit.PROGRESS_MIN);
        RecordAction action = new RecordAction(this);
        action.open(false);
        final List<GridItem> gridList = action.listGrid();
        action.close();

        DynamicGridView gridView = layout.findViewById(R.id.home_grid);
        TextView aboutBlank = layout.findViewById(R.id.home_about_blank);
        gridView.setEmptyView(aboutBlank);

        final de.baumann.browser.View.GridAdapter gridAdapter;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridAdapter = new de.baumann.browser.View.GridAdapter(this, gridList, 3);
        } else {
            gridAdapter = new de.baumann.browser.View.GridAdapter(this, gridList, 2);
        }
        gridView.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        /* Wait for gridAdapter.notifyDataSetChanged() */
        gridView.postDelayed(new Runnable() {
            @Override
            public void run() {
                layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                updateProgress(BrowserUnit.PROGRESS_MAX);
            }
        }, shortAnimTime);

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

    private void initBHList(final NinjaRelativeLayout layout, boolean update) {
        if (update) {
            updateProgress(BrowserUnit.PROGRESS_MIN);
        }

        RecordAction action = new RecordAction(BrowserActivity.this);
        action.open(false);
        final List<Record> list;
        if (layout.getFlag() == BrowserUnit.FLAG_BOOKMARKS) {
            list = action.listBookmarks();
            Collections.sort(list, new Comparator<Record>() {
                @Override
                public int compare(Record first, Record second) {
                    return first.getTitle().compareTo(second.getTitle());
                }
            });
        } else if (layout.getFlag() == BrowserUnit.FLAG_HISTORY) {
            list = action.listHistory();
        } else {
            list = new ArrayList<>();
        }
        action.close();

        listView = layout.findViewById(R.id.record_list);
        TextView textView = layout.findViewById(R.id.record_list_empty);
        listView.setEmptyView(textView);

        final RecordAdapter adapter = new RecordAdapter(BrowserActivity.this, list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        /* Wait for adapter.notifyDataSetChanged() */
        if (update) {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                    updateProgress(BrowserUnit.PROGRESS_MAX);
                }
            }, shortAnimTime);
        }

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

    private void initFEList(final NinjaRelativeLayout layout) {

        deleteDatabase("files_DB_v01.db");

        Files db = new Files(this);
        db.open();

        String path = sp.getString("files_startFolder",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        File f = new File(path);
        File[] files = f.listFiles();

        // looping through all items <item>
        if (files == null || files.length == 0) {
            NinjaToast.show(BrowserActivity.this, getString(R.string.toast_noFile));
            files = (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).
                    getAbsolutePath())).listFiles();
        }

        for (File file : files) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            String file_Name = helper_main.secString(file.getName());
            String file_Size = getReadableFileSize(file.length());
            String file_date = formatter.format(new Date(file.lastModified()));
            String file_path = file.getAbsolutePath();

            String file_ext;
            if (file.isDirectory()) {
                file_ext = ".";
            } else {
                try {
                    file_ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                } catch (Exception e) {
                    file_ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/"));
                }
            }

            db.open();
            if(db.isExist(file_Name)) {
                Log.i(TAG, "Entry exists" + file_Name);
            } else {
                db.insert(file_Name, file_Size, file_ext, file_path, file_date);
            }
        }

        try {
            db.insert("...", "", "", "", "");
        } catch (Exception e) {
            Log.i(TAG, "Browser something went wrong");
        }

        //display data

        listView = layout.findViewById(R.id.record_list);

        final int layoutstyle= R.layout.record_item;
        int[] xml_id = new int[] {
                R.id.record_item_title,
                R.id.record_item_url,
                R.id.record_item_time
        };
        String[] column = new String[] {
                "files_title",
                "files_creation",
                "files_content"
        };
        final Cursor row = db.fetchAllData();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutstyle,row,column, xml_id, 0) {
            @Override
            public View getView (final int position, View convertView, ViewGroup parent) {

                View v = super.getView(position, convertView, parent);

                try {
                    Cursor row = (Cursor) listView.getItemAtPosition(position);
                    String files_icon = row.getString(row.getColumnIndexOrThrow("files_icon"));
                    String files_attachment = row.getString(row.getColumnIndexOrThrow("files_attachment"));

                    ImageView iv = v.findViewById(R.id.icon);
                    iv.setVisibility(View.VISIBLE);

                    if (files_icon.matches("")) {
                        iv.setImageResource(R.drawable.icon_arrow_up_dark);
                    } else if (files_icon.matches("(.)")) {
                        iv.setImageResource(R.drawable.file_folder);
                    } else if (files_icon.matches("(.m3u8|.mp3|.wma|.midi|.wav|.aac|.aif|.amp3|.weba|.ogg)")) {
                        iv.setImageResource(R.drawable.file_music);
                    } else if (files_icon.matches("(.mpeg|.mp4|.webm|.qt|.3gp|.3g2|.avi|.flv|.h261|.h263|.h264|.asf|.wmv)")) {
                        try {
                            Glide.with(BrowserActivity.this).load(files_attachment).into(iv);
                        } catch (Exception e) {
                            Log.w("Browser", "Error load thumbnail", e);
                            iv.setImageResource(R.drawable.file_video);
                        }
                    } else if(files_icon.matches("(.gif|.bmp|.tiff|.svg|.png|.jpg|.JPG|.jpeg)")) {
                        try {
                            Glide.with(BrowserActivity.this).load(files_attachment).into(iv);
                        } catch (Exception e) {
                            Log.w("Browser", "Error load thumbnail", e);
                            iv.setImageResource(R.drawable.file_image);
                        }
                    } else if (files_icon.matches("(.vcs|.vcf|.css|.ics|.conf|.config|.java|.html)")) {
                        iv.setImageResource(R.drawable.file_xml);
                    } else if (files_icon.matches("(.apk)")) {
                        iv.setImageResource(R.drawable.file_android);
                    } else if (files_icon.matches("(.pdf)")) {
                        iv.setImageResource(R.drawable.file_pdf);
                    } else if (files_icon.matches("(.rtf|.csv|.txt|.doc|.xls|.ppt|.docx|.pptx|.xlsx|.odt|.ods|.odp)")) {
                        iv.setImageResource(R.drawable.file_document);
                    } else if (files_icon.matches("(.zip|.rar)")) {
                        iv.setImageResource(R.drawable.file_zip_box);
                    } else {
                        iv.setImageResource(R.drawable.file);
                    }
                } catch (Exception e) {
                    NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                }

                return v;
            }
        };

        listView.setAdapter(adapter);

        //onClick function
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                try {
                    Cursor row = (Cursor) listView.getItemAtPosition(position);
                    final String files_icon = row.getString(row.getColumnIndexOrThrow("files_icon"));
                    final String files_attachment = row.getString(row.getColumnIndexOrThrow("files_attachment"));
                    final File pathFile = new File(files_attachment);

                    if(pathFile.isDirectory()) {
                        String preFolder = sp.getString("files_startFolder",
                                Environment.getExternalStorageDirectory().getPath());
                        try {
                            sp.edit().putString("files_startFolder", files_attachment).apply();
                            initFEList(layout);
                        } catch (Exception e) {
                            NinjaToast.show(BrowserActivity.this, getString(R.string.toast_directory));
                            //set files_startFolder preFolder if has Exception
                            sp.edit().putString("files_startFolder", preFolder).apply();
                        }
                    } else if(files_attachment.equals("")) {
                        try {
                            final File pathActual = new File(sp.getString("files_startFolder",
                                    Environment.getExternalStorageDirectory().getPath()));
                            sp.edit().putString("files_startFolder", pathActual.getParent()).apply();
                            initFEList(layout);
                        } catch (Exception e) {
                            NinjaToast.show(BrowserActivity.this, getString(R.string.toast_directory));
                        }
                    } else {
                        helper_main.open(files_icon, BrowserActivity.this, pathFile, listView);
                    }
                } catch (Exception e) {
                    NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                try {
                    Cursor row = (Cursor) listView.getItemAtPosition(position);
                    final String files_title = row.getString(row.getColumnIndexOrThrow("files_title"));
                    final String files_attachment = row.getString(row.getColumnIndexOrThrow("files_attachment"));
                    final File pathFile = new File(files_attachment);

                    if (pathFile.isDirectory()) {

                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(R.string.toast_titleConfirm_delete);
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                sp.edit().putString("files_startFolder", pathFile.getParent()).apply();
                                deleteRecursive(pathFile);
                                initFEList(layout);
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

                    } else {

                        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
                        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu_context, null);

                        tv2_menu_share = dialogView.findViewById(R.id.tv2_menu_share);
                        tv2_menu_share.setVisibility(View.VISIBLE);
                        tv2_menu_share.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("image/png");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, files_title);
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, files_title);
                                Uri bmpUri = Uri.fromFile(pathFile);
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                startActivity(Intent.createChooser(sharingIntent, getString(R.string.menu_share)));
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
                                        pathFile.delete();
                                        initFEList(layout);
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
                    }
                } catch (Exception e) {
                    NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                }
                return true;
            }
        });
    }

    private void initPSList(final NinjaRelativeLayout layout) {

        final Pass db = new Pass(this);
        db.open();

        final int layoutstyle = R.layout.record_item;
        int[] xml_id = new int[] {
                R.id.record_item_title,
                R.id.record_item_url,
                R.id.record_item_time
        };
        String[] column = new String[] {
                "pass_title",
                "pass_content",
                "pass_creation"
        };

        final Cursor row = db.fetchAllData();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, layoutstyle,row,column, xml_id, 0) {
            @Override
            public View getView (final int position, View convertView, ViewGroup parent) {

                View v = super.getView(position, convertView, parent);

                try {
                    Cursor row = (Cursor) listView.getItemAtPosition(position);
                    String pass_creation = row.getString(row.getColumnIndexOrThrow("pass_creation"));
                    RelativeTimeTextView tv = v.findViewById(R.id.record_item_time);
                    tv.setReferenceTime(Long.parseLong(pass_creation));
                } catch (Exception e) {
                    NinjaToast.show(BrowserActivity.this, R.string.toast_error);
                }
                return v;
            }
        };

        listView = layout.findViewById(R.id.record_list);
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

                                        db.update(Integer.parseInt(_id), helper_main.secString(input_pass_title), helper_main.secString(pass_content), helper_main.secString(encrypted_userName), helper_main.secString(encrypted_userPW), String.valueOf(System.currentTimeMillis()));
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

        CheckBox sw_java = dialogView.findViewById(R.id.switch1);
        final ImageButton whiteList_js = dialogView.findViewById(R.id.imageButton_js);
        CheckBox sw_adBlock = dialogView.findViewById(R.id.switch2);
        final ImageButton whiteList_ab = dialogView.findViewById(R.id.imageButton_ab);
        CheckBox sw_image = dialogView.findViewById(R.id.switch4);
        CheckBox sw_cookie = dialogView.findViewById(R.id.switch5);
        CheckBox sw_location = dialogView.findViewById(R.id.switch6);
        CheckBox sw_invert = dialogView.findViewById(R.id.switch7);
        CheckBox sw_history = dialogView.findViewById(R.id.switch3);

        javaHosts = new Javascript(BrowserActivity.this);
        javaHosts = getJavaHosts();

        adBlock = new AdBlock(BrowserActivity.this);
        adBlock = getAdBlock();

        ninjaWebView = (NinjaWebView) currentAlbumController;

        final String url = ninjaWebView.getUrl();

        if (javaHosts.isWhite(url)) {
            whiteList_js.setImageResource(R.drawable.check_green);
        } else {
            whiteList_js.setImageResource(R.drawable.ic_action_close_red);
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

        sw_java.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @SuppressLint("ApplySharedPref")
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

            @SuppressLint("ApplySharedPref")
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

            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_images), true).commit();
                    IntentUnit.setSPChange(true);
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_images), false).commit();
                    IntentUnit.setSPChange(true);
                }
            }
        });

        if (sp.getBoolean(getString(R.string.sp_cookies), true)){
            sw_cookie.setChecked(true);
        } else {
            sw_cookie.setChecked(false);
        }

        sw_cookie.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @SuppressLint("ApplySharedPref")
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

            @SuppressLint("ApplySharedPref")
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

            @SuppressLint("ApplySharedPref")
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

            @SuppressLint("ApplySharedPref")
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

        Button but_OK = dialogView.findViewById(R.id.action_ok);
        but_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (switcherPanel.getStatus() != SwitcherPanel.Status.EXPANDED) {
                    switcherPanel.expanded();
                }

                if (ninjaWebView != null) {
                    String url = ninjaWebView.getUrl();
                    addAlbum(getString(R.string.album_untitled), url, true, null);
                    removeAlbum(currentAlbumController);
                }

                bottomSheetDialog.cancel();
            }
        });

        Button but_set = dialogView.findViewById(R.id.action_settings);
        but_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (switcherPanel.getStatus() != SwitcherPanel.Status.EXPANDED) {
                    switcherPanel.expanded();
                }
                Intent intent = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(intent);
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

            BroadcastReceiver unCopy = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ClipData clip = ClipData.newPlainText("text", decrypted_userName);
                    clipboard.setPrimaryClip(clip);
                    NinjaToast.show(BrowserActivity.this, R.string.toast_copy_successful);
                }
            };

            BroadcastReceiver pwCopy = new BroadcastReceiver() {
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

            String theme = sp.getString("theme", "0");
            int color;

            switch (theme) {
                case "5":case "6":case "8":
                    color = ContextCompat.getColor(BrowserActivity.this,R.color.colorAccent_grey);
                    break;
                case "7":
                    color = ContextCompat.getColor(BrowserActivity.this,R.color.colorAccent_brown);
                    break;
                case "9":
                    color = ContextCompat.getColor(BrowserActivity.this,R.color.colorAccent_darkGrey);
                    break;
                default:
                    color = ContextCompat.getColor(BrowserActivity.this,R.color.colorAccent);
            }

            NotificationCompat.Action action_UN = new NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pasteUN), copyUN).build();
            NotificationCompat.Action action_PW = new NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pastePW), copyPW).build();

            @SuppressWarnings("deprecation")
            Notification n  = builder
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setSmallIcon(R.drawable.ic_notification_ninja)
                    .setContentTitle(BrowserActivity.this.getString(R.string.app_name))
                    .setContentText(BrowserActivity.this.getString(R.string.toast_titleConfirm_paste))
                    .setColor(color)
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



    private void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private synchronized void addAlbum(int flag) {
        final AlbumController holder;
        if (flag == BrowserUnit.FLAG_BOOKMARKS) {
            NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.record_list, nullParent, false);
            layout.setBrowserController(this);
            layout.setFlag(BrowserUnit.FLAG_BOOKMARKS);
            layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            layout.setAlbumTitle(getString(R.string.album_title_bookmarks));
            holder = layout;
            initBHList(layout, false);
            showOmnibox();
        } else if (flag == BrowserUnit.FLAG_HISTORY) {
            NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.record_list, nullParent, false);
            layout.setBrowserController(this);
            layout.setFlag(BrowserUnit.FLAG_HISTORY);
            layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            layout.setAlbumTitle(getString(R.string.album_title_history));
            holder = layout;
            initBHList(layout, false);
            showOmnibox();
        } else if (flag == BrowserUnit.FLAG_HOME) {
            NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.home, nullParent, false);
            layout.setBrowserController(this);
            layout.setFlag(BrowserUnit.FLAG_HOME);
            layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            layout.setAlbumTitle(getString(R.string.album_title_home));
            holder = layout;
            initHomeGrid(layout);
            showOmnibox();
        }  else if (flag == BrowserUnit.FLAG_FILES) {
            NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.record_list, nullParent, false);
            layout.setBrowserController(this);
            layout.setFlag(BrowserUnit.FLAG_FILES);
            layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            layout.setAlbumTitle(getString(R.string.album_title_files));
            holder = layout;
            initFEList(layout);
            showOmnibox();
        } else if (flag == BrowserUnit.FLAG_PASS) {
            NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.record_list, nullParent, false);
            layout.setBrowserController(this);
            layout.setFlag(BrowserUnit.FLAG_PASS);
            layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            layout.setAlbumTitle(getString(R.string.album_title_pass));
            holder = layout;
            initPSList(layout);
            showOmnibox();
        } else {
            return;
        }

        final View albumView = holder.getAlbumView();
        albumView.setVisibility(View.INVISIBLE);

        BrowserContainer.add(holder);
        switcherContainer.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.album_slide_in_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                albumView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showAlbum(holder, true, true);
            }
        });
        albumView.startAnimation(animation);
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground, final Message resultMsg) {
        ninjaWebView = new NinjaWebView(this);

        showOmnibox();
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setFlag(BrowserUnit.FLAG_NINJA);
        ninjaWebView.setAlbumCover(ViewUnit.capture(ninjaWebView, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
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

            albumView.setVisibility(View.VISIBLE);
            if (currentAlbumController != null) {
                switcherScroller.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
            }
            return;
        }

        albumView.setVisibility(View.INVISIBLE);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.album_slide_in_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                albumView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showAlbum(ninjaWebView, true, false);

                if (url != null && !url.isEmpty()) {
                    ninjaWebView.loadUrl(url);
                } else if (resultMsg != null) {
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(ninjaWebView);
                    resultMsg.sendToTarget();
                }
            }
        });
        albumView.startAnimation(animation);
    }

    private synchronized void pinAlbums(String url) {
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
            addAlbum(BrowserUnit.FLAG_HOME);
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    switcherScroller.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
                    currentAlbumController.setAlbumCover(ViewUnit.capture(((View) currentAlbumController), dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                }
            }, shortAnimTime);
        } else { // When url != null
            ninjaWebView.setBrowserController(this);
            ninjaWebView.setFlag(BrowserUnit.FLAG_NINJA);
            ninjaWebView.setAlbumCover(ViewUnit.capture(ninjaWebView, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
            ninjaWebView.setAlbumTitle(getString(R.string.album_untitled));
            ViewUnit.bound(this, ninjaWebView);
            ninjaWebView.loadUrl(url);

            BrowserContainer.add(ninjaWebView);
            final View albumView = ninjaWebView.getAlbumView();
            albumView.setVisibility(View.VISIBLE);
            switcherContainer.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            contentFrame.removeAllViews();
            contentFrame.addView(ninjaWebView);

            if (currentAlbumController != null) {
                currentAlbumController.deactivate();
            }
            currentAlbumController = ninjaWebView;
            currentAlbumController.activate();

            updateOmnibox();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    switcherScroller.smoothScrollTo(currentAlbumController.getAlbumView().getLeft(), 0);
                    currentAlbumController.setAlbumCover(ViewUnit.capture(((View) currentAlbumController), dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                }
            }, shortAnimTime);
        }
    }

    private synchronized void updateAlbum() {
        if (currentAlbumController == null) {
            return;
        }

        NinjaRelativeLayout layout = (NinjaRelativeLayout) getLayoutInflater().inflate(R.layout.home, nullParent, false);
        layout.setBrowserController(this);
        layout.setFlag(BrowserUnit.FLAG_HOME);
        layout.setAlbumCover(ViewUnit.capture(layout, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
        layout.setAlbumTitle(getString(R.string.album_title_home));
        initHomeGrid(layout);

        int index = switcherContainer.indexOfChild(currentAlbumController.getAlbumView());
        currentAlbumController.deactivate();
        switcherContainer.removeView(currentAlbumController.getAlbumView());
        contentFrame.removeAllViews(); ///

        switcherContainer.addView(layout.getAlbumView(), index, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        contentFrame.addView(layout);
        BrowserContainer.set(layout, index);
        currentAlbumController = layout;
        updateOmnibox();
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
            ninjaWebView.setAlbumCover(ViewUnit.capture(ninjaWebView, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
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

    @Override
    public synchronized void removeAlbum(AlbumController controller) {
        if (currentAlbumController == null || BrowserContainer.size() <= 1) {
            switcherContainer.removeView(controller.getAlbumView());
            BrowserContainer.remove(controller);
            addAlbum(BrowserUnit.FLAG_HOME);
            return;
        }

        if (controller != currentAlbumController) {
            switcherContainer.removeView(controller.getAlbumView());
            BrowserContainer.remove(controller);
        } else {
            switcherContainer.removeView(controller.getAlbumView());
            int index = BrowserContainer.indexOf(controller);
            BrowserContainer.remove(controller);
            if (index >= BrowserContainer.size()) {
                index = BrowserContainer.size() - 1;
            }
            showAlbum(BrowserContainer.get(index), false, false);
        }
    }

    private void updateOmnibox() {
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
            String title = ninjaWebView.getTitle();
            updateProgress(ninjaWebView.getProgress());
            updateBookmarks();
            scrollChange();
            if (ninjaWebView.getUrl() == null && ninjaWebView.getOriginalUrl() == null) {
                updateInputBox(null);
            } else if (ninjaWebView.getUrl() != null) {
                updateInputBox(ninjaWebView.getUrl());
                omniboxTitle.setText(title);
            } else {
                updateInputBox(ninjaWebView.getOriginalUrl());
                omniboxTitle.setText(title);
            }
        }
    }

    private void scrollChange () {
        if (sp.getBoolean("sp_hideOmni", true)) {
            ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(int scrollY, int oldScrollY) {
                    if (scrollY > oldScrollY) {
                        hideOmnibox();
                        ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                            @Override
                            public void onScrollChange(int scrollY, int oldScrollY) {}
                        });
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollChange();
                            }
                        }, 250);
                    } else if (scrollY < oldScrollY){
                        showOmnibox();
                        ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                            @Override
                            public void onScrollChange(int scrollY, int oldScrollY) {}
                        });
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollChange();
                            }
                        }, 250);
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
            omniboxRefresh.setImageDrawable(ViewUnit.getDrawable(this, R.drawable.ic_action_refresh));
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

        TextView tv3_main_menu_new_tab = dialogView.findViewById(R.id.tv3_main_menu_new_tab);
        tv3_main_menu_new_tab.setVisibility(View.VISIBLE);
        tv3_main_menu_new_tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlbum(getString(R.string.album_untitled), target, false, null);
                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_new_tab_successful));
                bottomSheetDialog.cancel();
            }
        });

        TextView tv3_main_menu_new_tabOpen = dialogView.findViewById(R.id.tv3_main_menu_new_tabOpen);
        tv3_main_menu_new_tabOpen.setVisibility(View.VISIBLE);
        tv3_main_menu_new_tabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinAlbums(target);
                bottomSheetDialog.cancel();
            }
        });

        TextView tv3_main_menu_copy_link = dialogView.findViewById(R.id.tv3_main_menu_copy_link);
        tv3_main_menu_copy_link.setVisibility(View.VISIBLE);
        tv3_main_menu_copy_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserUnit.copyURL(BrowserActivity.this, target);
                bottomSheetDialog.cancel();
            }
        });

        TextView tv3_menu_save_as = dialogView.findViewById(R.id.tv3_menu_save_as);
        tv3_menu_save_as.setVisibility(View.VISIBLE);
        tv3_menu_save_as.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    assert target != null;
                    String filename = target.substring(target.lastIndexOf("/")+1);

                    AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                    View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit, null);

                    final EditText editText = dialogView.findViewById(R.id.dialog_edit);

                    editText.setHint(R.string.dialog_title_hint);
                    editText.setText(filename);
                    editText.setSelection(filename.length());

                    builder.setView(dialogView);
                    builder.setTitle(R.string.menu_edit);
                    builder.setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {

                            String text = editText.getText().toString().trim();
                            if (text.isEmpty()) {
                                NinjaToast.show(BrowserActivity.this, getString(R.string.toast_input_empty));
                            } else {
                                Uri source = Uri.parse(target);
                                DownloadManager.Request request = new DownloadManager.Request(source);
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(target));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, text);
                                DownloadManager dm = (DownloadManager) BrowserActivity.this.getSystemService(DOWNLOAD_SERVICE);
                                assert dm != null;
                                dm.enqueue(request);
                                BrowserUnit.download(BrowserActivity.this, target, text, BrowserUnit.MIME_TYPE_IMAGE);
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
                    bottomSheetDialog.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (result != null && (result.getType() == WebView.HitTestResult.IMAGE_TYPE || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
            TextView tv3_main_menu_save = dialogView.findViewById(R.id.tv3_main_menu_save);
            tv3_main_menu_save.setVisibility(View.VISIBLE);
            tv3_main_menu_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserUnit.download(BrowserActivity.this, target, target, BrowserUnit.MIME_TYPE_IMAGE);
                    bottomSheetDialog.cancel();
                }
            });
        }
    }

    private void doubleTapsQuit() {
        final Timer timer = new Timer();
        if (!quit) {
            quit = true;
            NinjaToast.show(this, getString(R.string.toast_double_taps_quit));
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    quit = false;
                    timer.cancel();
                }
            }, 2000);
        } else {
            timer.cancel();
            finish();
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

    private void hideSoftInput(final View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showSoftInput(final View view) {
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

            if (sp.getBoolean("nav_show", true)) {
                fab_imageButtonNav.setVisibility(View.GONE);
            }

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            onConfigurationChanged(null);
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
            fab_imageButtonNav.setVisibility(View.VISIBLE);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

    private boolean showOverflow() {

        bottomSheetDialog = new BottomSheetDialog(BrowserActivity.this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_menu, null);

        fab_tab = dialogView.findViewById(R.id.floatButton_tab);
        fab_tab.setOnClickListener(this);
        fab_share = dialogView.findViewById(R.id.floatButton_share);
        fab_share.setOnClickListener(this);
        fab_save = dialogView.findViewById(R.id.floatButton_save);
        fab_save.setOnClickListener(this);
        fab_more = dialogView.findViewById(R.id.floatButton_more);
        fab_more.setOnClickListener(this);

        if (currentAlbumController != null && currentAlbumController instanceof NinjaRelativeLayout) {
            fab_share.setVisibility(View.GONE);
            fab_save.setVisibility(View.GONE);
        } else if (currentAlbumController != null && currentAlbumController instanceof NinjaWebView) {
            fab_share.setVisibility(View.VISIBLE);
            fab_save.setVisibility(View.VISIBLE);
        }

        dialogTitle = dialogView.findViewById(R.id.dialog_title);
        dialogTitle.setText(getString(R.string.main_menu));

        tv_new_tabOpen = dialogView.findViewById(R.id.tv_new_tabOpen);
        tv_new_tabOpen.setOnClickListener(this);
        tv_closeTab = dialogView.findViewById(R.id.tv_closeTab);
        tv_closeTab.setOnClickListener(this);
        tv_tabPreview = dialogView.findViewById(R.id.tv_tabPreview);
        tv_tabPreview.setOnClickListener(this);
        tv_quit = dialogView.findViewById(R.id.tv_quit);
        tv_quit.setOnClickListener(this);

        tv_shareScreenshot = dialogView.findViewById(R.id.tv_shareScreenshot);
        tv_shareScreenshot.setOnClickListener(this);
        tv_shareLink = dialogView.findViewById(R.id.tv_shareLink);
        tv_shareLink.setOnClickListener(this);
        tv_shareClipboard = dialogView.findViewById(R.id.tv_shareClipboard);
        tv_shareClipboard.setOnClickListener(this);

        tv_saveScreenshot = dialogView.findViewById(R.id.tv_saveScreenshot);
        tv_saveScreenshot.setOnClickListener(this);
        tv_saveBookmark = dialogView.findViewById(R.id.tv_saveBookmark);
        tv_saveBookmark.setOnClickListener(this);
        tv_saveStart = dialogView.findViewById(R.id.tv_saveStart);
        tv_saveStart.setOnClickListener(this);
        tv_saveLogin = dialogView.findViewById(R.id.tv_saveLogin);
        tv_saveLogin.setOnClickListener(this);

        tv_relayout = dialogView.findViewById(R.id.tv_relayout);
        tv_relayout.setOnClickListener(this);
        tv_searchSite = dialogView.findViewById(R.id.tv_searchSite);
        tv_searchSite.setOnClickListener(this);
        tv_settings = dialogView.findViewById(R.id.tv_settings);
        tv_settings.setOnClickListener(this);
        tv_help = dialogView.findViewById(R.id.tv_help);
        tv_help.setOnClickListener(this);

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


    private void showListMenu(final RecordAdapter recordAdapter, final List<Record> recordList, final int location) {

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
                        recordAdapter.notifyDataSetChanged();
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
                    showEditDialog(recordAdapter, recordList, location);
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

        help_logo.setImageResource(R.drawable.help_toolbar);
        help_tabs.setImageResource(R.drawable.help_tabs);
        help_not.setImageResource(R.drawable.help_not);
        help_nav.setImageResource(R.drawable.help_nav);
        help_set.setImageResource(R.drawable.help_settings);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(getString(R.string.menu_other_help));

        FloatingActionButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
            }
        });

        FloatingActionButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
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
        editText.setSelection(gridItem.getTitle().length());

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

        final AlertDialog dialog = builder.create();
        // Display the custom alert dialog on interface
        dialog.show();
        showSoftInput(editText);
    }

    private void showEditDialog(final RecordAdapter recordAdapter, List<Record> recordList, int location) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = View.inflate(BrowserActivity.this, R.layout.dialog_edit, null);

        final Record record = recordList.get(location);
        final EditText editText = dialogView.findViewById(R.id.dialog_edit);

        editText.setHint(R.string.dialog_title_hint);
        editText.setText(record.getTitle());
        editText.setSelection(record.getTitle().length());

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

                recordAdapter.notifyDataSetChanged();
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
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        /*
         * Can not use View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION,
         * so we can not hide NavigationBar :(
         */
        int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;

        if (fullscreen) {
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
            if (customView != null) {
                customView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                contentFrame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        getWindow().setAttributes(layoutParams);
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
