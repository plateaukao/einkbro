package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.baumann.browser.Ninja.BuildConfig;
import de.baumann.browser.browser.*;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.ViewUnit;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class NinjaWebView extends WebView implements AlbumController {


    private static final String verticalLayoutCss = "body {\n" +
            "-webkit-writing-mode: vertical-rl;\n" +
            "writing-mode: vertical-rl;\n" +
            "}";

    private static final String horizontalLayoutCss = "body {\n" +
            "-webkit-writing-mode: horizontal-tb;\n" +
            "writing-mode: horizontal-tb;\n" +
            "}";


    private Context context;
    private int dimen144dp;
    private int dimen108dp;

    private OnScrollChangeListener onScrollChangeListener;

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
    private Cookie cookieHosts;

    public Cookie getCookieHosts() {
        return cookieHosts;
    }

    public Javascript getJavaHosts() {
        return javaHosts;
    }

    private SharedPreferences sp;
    private WebSettings webSettings;

    private String defaultUserAgent;

    private boolean foreground;

    private boolean isVerticalRead = false;

    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;

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
        void onScrollChange(int scrollY, int oldScrollY);
    }


    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an activity

        this.context = context;
        this.dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        this.dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        this.foreground = false;

        this.adBlock = new AdBlock(this.context);
        this.javaHosts = new Javascript(this.context);
        this.cookieHosts = new Cookie(this.context);
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

    private void initWebView() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void initWebSettings() {
        webSettings = getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }
    }

    public void initPreferences() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String userAgent = sp.getString("userAgent", "");
        webSettings = getSettings();
        defaultUserAgent = webSettings.getUserAgentString();

        if (!userAgent.isEmpty()) {
            webSettings.setUserAgentString(userAgent);
        }

        Boolean isDesktopMode = sp.getBoolean("sp_desktop", false);
        if (isDesktopMode) {
            webSettings.setUserAgentString(BrowserUnit.UA_DESKTOP);
        } else {
            webSettings.setUserAgentString(defaultUserAgent.replace("wv", ""));
        }
        webSettings.setUseWideViewPort(isDesktopMode);
        webSettings.setLoadWithOverviewMode(isDesktopMode);

        webViewClient.enableAdBlock(sp.getBoolean(context.getString(R.string.sp_ad_block), true));
        webSettings = getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setTextZoom(Integer.parseInt(sp.getString("sp_fontSize", "100")));
        webSettings.setAllowFileAccessFromFileURLs(sp.getBoolean(("sp_remote"), true));
        webSettings.setAllowUniversalAccessFromFileURLs(sp.getBoolean(("sp_remote"), true));
        webSettings.setDomStorageEnabled(sp.getBoolean(("sp_remote"), true));
        webSettings.setDatabaseEnabled(true);
        webSettings.setBlockNetworkImage(!sp.getBoolean(context.getString(R.string.sp_images), true));
        webSettings.setJavaScriptEnabled(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), false));
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(sp.getBoolean(context.getString(R.string.sp_cookies), true));
    }

    private void initAlbum() {
        album.setAlbumCover(null);
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        if (sp.getBoolean(context.getString(R.string.sp_savedata), false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    /* continue playing if preference is set */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (sp.getBoolean("sp_media_continue", false)) {
            if (visibility != View.GONE && visibility != View.INVISIBLE)
                super.onWindowVisibilityChanged(View.VISIBLE);
        } else {
            super.onWindowVisibilityChanged(visibility);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error);
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

        if(url.startsWith("javascript")) {
            // Daniel
            super.loadUrl(url);
            return;
        }

        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
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
        if (isLoadFinish()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAlbumCover(ViewUnit.capture(NinjaWebView.this, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                }
            }, 250);

            if (prepareRecord()) {
                browserController.updateAutoComplete();
            }
        }
    }

    public synchronized void update(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        click.setTarget(clickHandler);
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

    public void jumpToTop() {
        scrollTo(0, 0);
    }

    public void jumpToBottom() {
        if (getVerticalRead()) {
            scrollTo(computeHorizontalScrollRange(), 0);
        } else {
            scrollTo(0, computeVerticalScrollRange());
        }
    }

    public void pageDownWithNoAnimation() {
        if (getVerticalRead()) {
            scrollBy(shiftOffset(), 0);
        } else {
            scrollBy(0, shiftOffset());
        }
    }

    public void pageUpWithNoAnimation() {
        if (getVerticalRead()) {
            scrollBy(-shiftOffset(), 0);
        } else {
            scrollBy(0, -shiftOffset());
        }
    }

    public int shiftOffset() {
        if (getVerticalRead()) {
            return getWidth() - (int) ViewUnit.dpToPixel(getContext(), 40);
        } else {
            return getHeight() - (int) ViewUnit.dpToPixel(getContext(), 80);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!browserController.handleKeyEvent(event)) {
            return super.dispatchKeyEvent(event);
        } else {
            return true;
        }
    }

    public void applyVerticalRead() {
        injectJavascript(this, verticalLayoutCss.getBytes());
    }

    public void applyHorizontalRead() {
        injectJavascript(this, horizontalLayoutCss.getBytes());
    }

    private void injectScriptFile(WebView view, String scriptFile) {
        InputStream input;
        try {
            input = view.getContext().getAssets().open(scriptFile);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            injectJavascript(view, buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void injectJavascript(WebView view, byte[] bytes) {
        try {
            String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
            view.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean getVerticalRead() {
        return sp.getBoolean("sp_vertical_read", false);
    }
}
