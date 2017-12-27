package de.baumann.browser.View;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.MailTo;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import de.baumann.browser.Activity.helper_main;
import de.baumann.browser.Browser.*;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.IntentUnit;
import de.baumann.browser.Unit.ViewUnit;

import java.net.URISyntaxException;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;


    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(int scrollY, int oldScrollY);
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    private Context context;
    private int flag = BrowserUnit.FLAG_NINJA;
    private int dimen144dp;
    private int dimen108dp;
    private int animTime;

    private Album album;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;
    private NinjaClickHandler clickHandler;
    private GestureDetector gestureDetector;

    private AdBlock adBlock;
    public AdBlock getAdBlock() {
        return adBlock;
    }

    private Javascript javaHosts;

    private SharedPreferences sp;
    private WebSettings webSettings;

    private boolean foreground;
    public boolean isForeground() {
        return foreground;
    }

    private String userAgentOriginal;

    private BrowserController browserController = null;
    public BrowserController getBrowserController() {
        return browserController;
    }
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an Activity

        this.context = context;
        this.dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        this.dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        this.animTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.foreground = false;

        this.adBlock = new AdBlock(this.context);
        this.javaHosts = new Javascript(this.context);
        this.album = new Album(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        this.clickHandler = new NinjaClickHandler(this);
        this.gestureDetector = new GestureDetector(context, new NinjaGestureListener(this));

        initWebView();
        initWebSettings();
        initPreferences();
        initAlbum();
    }

    private synchronized void initWebView() {
        setDrawingCacheBackgroundColor(0x00000000);
        setDrawingCacheEnabled(true);
        setWillNotCacheDrawing(false);
        setSaveEnabled(true);

        setBackground(null);
        getRootView().setBackground(null);
        setBackgroundColor(ContextCompat.getColor(context,R.color.light));

        setFocusable(true);
        setFocusableInTouchMode(true);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setScrollbarFadingEnabled(true);

        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
    }

    private synchronized void initWebSettings() {
        webSettings = getSettings();
        userAgentOriginal = webSettings.getUserAgentString();

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setSupportMultipleWindows(false);

        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(context.getCacheDir().toString());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webSettings.setDefaultTextEncodingName(BrowserUnit.URL_ENCODING);
        webSettings.setLoadsImagesAutomatically(true);

        webSettings.setLoadWithOverviewMode(true);
        webSettings.setTextZoom(100);
        webSettings.setUseWideViewPort(true);
    }

    public synchronized void initPreferences() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        webSettings = getSettings();

        webSettings.setBlockNetworkImage(!sp.getBoolean(context.getString(R.string.sp_images), true));
        webSettings.setJavaScriptEnabled(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(context.getString(R.string.sp_javascript), true));

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        try {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        } catch (Exception e) {
            Log.w("Browser", "Error setting TEXT_AUTOSIZING", e);
        }

        int userAgent = Integer.valueOf(sp.getString(context.getString(R.string.sp_user_agent), "0"));
        if (userAgent == 1) {
            webSettings.setUserAgentString(BrowserUnit.UA_DESKTOP);
        } else if (userAgent == 2) {
            webSettings.setUserAgentString(sp.getString(context.getString(R.string.sp_user_agent_custom), userAgentOriginal));
        } else {
            webSettings.setUserAgentString(userAgentOriginal);
        }

        int mode = Integer.valueOf(sp.getString(context.getString(R.string.sp_rendering), "0"));
        initRendering(mode);

        webViewClient.enableAdBlock(sp.getBoolean(context.getString(R.string.sp_ad_block), true));

        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(sp.getBoolean(context.getString(R.string.sp_cookies), true));

        if (sp.getBoolean(context.getString(R.string.sp_location), true)) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                int hasACCESS_FINE_LOCATION = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                    Activity activity = (Activity) context;
                    helper_main.grantPermissionsLoc(activity);
                } else {
                    webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), true));
                }
            } else {
                webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), true));
            }
        }
    }

    private synchronized void initAlbum() {
        album.setAlbumCover(null);
        album.setAlbumTitle(context.getString(R.string.album_untitled));
        album.setBrowserController(browserController);
    }

    private void initRendering(int mode) {
        Paint paint = new Paint();

        switch (mode) {
            case 0: { // Default
                paint.setColorFilter(null);
                break;
            } case 1: { // Grayscale
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                paint.setColorFilter(filter);
                break;
            } case 2: { // Inverted
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(NEGATIVE_COLOR);
                paint.setColorFilter(filter);
                break;
            } case 3: { // Inverted grayscale
                ColorMatrix matrix = new ColorMatrix();
                matrix.set(NEGATIVE_COLOR);

                ColorMatrix gcm = new ColorMatrix();
                gcm.setSaturation(0);

                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, gcm);

                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
                paint.setColorFilter(filter);

                break;
            } default: {
                paint.setColorFilter(null);
                break;
            }
        }

        // maybe sometime LAYER_TYPE_NONE would better?
        setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public synchronized void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error);
            return;
        }

        url = BrowserUnit.queryWrapper(context, url.trim());
        if (url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)) {
            Intent intent = IntentUnit.getEmailIntent(MailTo.parse(url));
            context.startActivity(intent);
            reload();

            return;
        } else if (url.startsWith(BrowserUnit.URL_SCHEME_INTENT)) {
            Intent intent;
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                context.startActivity(intent);
            } catch (URISyntaxException u) {
                Log.w("Browser", "Error parsing URL");
            }

            return;
        }

        if (!sp.getBoolean(context.getString(R.string.sp_javascript), true)) {

            if (javaHosts.isWhite(url)) {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webSettings.setJavaScriptEnabled(true);
            } else {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
                webSettings.setJavaScriptEnabled(false);
            }
        }

        webViewClient.updateWhite(adBlock.isWhite(url));
        super.loadUrl(url);
        if (browserController != null && foreground) {
            browserController.updateBookmarks();
        }
    }

    @Override
    public void reload() {
        webViewClient.updateWhite(adBlock.isWhite(getUrl()));
        super.reload();
    }

    @Override
    public int getFlag() {
        return flag;
    }

    @Override
    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    @Override
    public void setAlbumCover(Bitmap bitmap) {
        album.setAlbumCover(bitmap);
    }

    @Override
    public String getAlbumTitle() {
        return album.getAlbumTitle();
    }

    @Override
    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void update(int progress) {
        if (foreground) {
            browserController.updateProgress(progress);
        }

        setAlbumCover(ViewUnit.capture(this, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
        if (isLoadFinish()) {
            setHorizontalScrollBarEnabled(true);
            setVerticalScrollBarEnabled(true);
            setScrollbarFadingEnabled(true);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAlbumCover(ViewUnit.capture(NinjaWebView.this, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                }
            }, animTime);

            if (prepareRecord()) {
                browserController.updateAutoComplete();
            }
        }
    }

    public synchronized void update(String title, String url) {
        album.setAlbumTitle(title);
        if (foreground) {
            browserController.updateBookmarks();
            browserController.updateInputBox(url);
        }

        try {
            TextView omniTitle = this.getRootView().findViewById(R.id.main_omnibox_title);
            omniTitle.setText(NinjaWebView.this.getTitle());
        } catch (Exception e) {
            Log.w("Browser", "Error updating");
        }
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        destroyDrawingCache();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        if (click != null) {
            click.setTarget(clickHandler);
        }
        requestFocusNodeHref(click);
    }

    private boolean prepareRecord() {
        String title = getTitle();
        String url = getUrl();

        return !(title == null
                || title.isEmpty()
                || url == null
                || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT));
    }
}
