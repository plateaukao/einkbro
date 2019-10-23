package de.baumann.browser.browser;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;

import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.IntentUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

import static android.content.ContentValues.TAG;

public class NinjaWebViewClient extends WebViewClient {
    private final NinjaWebView ninjaWebView;
    private final Context context;
    private final SharedPreferences sp;

    private final AdBlock adBlock;
    private final Cookie cookie;

    private boolean white;
    public void updateWhite(boolean white) {
        this.white = white;
    }

    private boolean enable;
    public void enableAdBlock(boolean enable) {
        this.enable = enable;
    }

    public NinjaWebViewClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
        this.context = ninjaWebView.getContext();
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.adBlock = ninjaWebView.getAdBlock();
        this.cookie = ninjaWebView.getCookieHosts();
        this.white = false;
        this.enable = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        ShortcutManager shortcutManager = null;
        String title = ninjaWebView.getTitle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager = context.getSystemService(ShortcutManager.class);
        }

        if (sp.getBoolean("saveHistory", true)) {
            RecordAction action = new RecordAction(context);
            action.open(true);

            if (action.checkHistory(url)) {
                action.deleteHistoryOld(url);
                action.addHistory(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis()));
            } else {
                action.addHistory(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis()));
            }
            action.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1  && !title.isEmpty() && !url.isEmpty()) {
                try {
                    if (sp.getInt("shortcut_number", 0) == 0) {
                        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "0")
                                .setShortLabel(title)
                                .setLongLabel(title)
                                .setIcon(Icon.createWithResource(context, R.drawable.qc_history))
                                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                .build();
                        Objects.requireNonNull(shortcutManager).addDynamicShortcuts(Collections.singletonList(shortcut));
                        sp.edit().putInt("shortcut_number", 1).apply();
                    } else {
                        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "1")
                                .setShortLabel(title)
                                .setLongLabel(title)
                                .setIcon(Icon.createWithResource(context, R.drawable.qc_history))
                                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                .build();
                        Objects.requireNonNull(shortcutManager).addDynamicShortcuts(Collections.singletonList(shortcut));
                        sp.edit().putInt("shortcut_number", 0).apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Objects.requireNonNull(shortcutManager).removeAllDynamicShortcuts();
            }
        }

        if (ninjaWebView.isForeground()) {
            ninjaWebView.invalidate();
        } else {
            ninjaWebView.postInvalidate();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final Uri uri = Uri.parse(url);
        return handleUri(view, uri);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        return handleUri(view, uri);
    }

    private boolean handleUri(WebView webView, final Uri uri) {

        Log.i(TAG, "Uri =" + uri);
        final String url = uri.toString();
        // Based on some condition you need to determine if you are going to load the url
        // in your web view itself or in a browser.
        // You can use `host` or `scheme` or any part of the `uri` to decide.
        // open web links as usual

        if (url.startsWith("http")) {
            webView.loadUrl(url, ninjaWebView.getRequestHeaders());
            return true;
        }

        //try to find browse activity to handle uri
        Uri parsedUri = Uri.parse(url);
        PackageManager packageManager = context.getPackageManager();
        Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
        if (browseIntent.resolveActivity(packageManager) != null) {
            context.startActivity(browseIntent);
            return true;
        }
        //if not activity found, try to parse intent://
        if (url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        NinjaToast.show(context, R.string.toast_load_error);
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
                Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()));
                if (marketIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(marketIntent);
                    return true;
                }
            } catch (URISyntaxException e) {
                //not an intent uri
                return false;
            }
        }
        white = adBlock.isWhite(url);
        return true;//do nothing in other cases
    }

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (enable && !white && adBlock.isAd(url)) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {

            if (cookie.isWhite(url)) {
                CookieManager manager = CookieManager.getInstance();
                manager.getCookie(url);
                manager.setAcceptCookie(true);
            }  else {
                CookieManager manager = CookieManager.getInstance();
                manager.setAcceptCookie(false);
            }
        }

        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (enable && !white && adBlock.isAd(request.getUrl().toString())) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {

            if (cookie.isWhite(request.getUrl().toString())) {
                CookieManager manager = CookieManager.getInstance();
                manager.getCookie(request.getUrl().toString());
                manager.setAcceptCookie(true);
            }  else {
                CookieManager manager = CookieManager.getInstance();
                manager.setAcceptCookie(false);
            }
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {
        Context holder = IntentUnit.getContext();
        if (!(holder instanceof Activity)) {
            return;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(holder);
        View dialogView = View.inflate(holder, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(R.string.dialog_content_resubmission);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resend.sendToTarget();
                dialog.cancel();
            }
        });
        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doNotResend.sendToTarget();
                dialog.cancel();
            }
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, SslError error) {
        Context holder = IntentUnit.getContext();
        if (!(holder instanceof Activity)) {
            return;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(holder);
        View dialogView = View.inflate(holder, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(R.string.dialog_content_ssl_error);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.proceed();
                dialog.cancel();
            }
        });
        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.cancel();
                dialog.cancel();
            }
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {
        Context holder = IntentUnit.getContext();
        if (!(holder instanceof Activity)) {
            return;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(holder);
        View dialogView = View.inflate(holder, R.layout.dialog_edit_bookmark, null);


        final EditText pass_userNameET = dialogView.findViewById(R.id.pass_userName);
        final EditText pass_userPWET = dialogView.findViewById(R.id.pass_userPW);

        TextInputLayout login_title = dialogView.findViewById(R.id.login_title);
        login_title.setVisibility(View.GONE);

        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = pass_userNameET.getText().toString().trim();
                String pass = pass_userPWET.getText().toString().trim();
                handler.proceed(user, pass);
                dialog.cancel();
            }
        });
        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.cancel();
                dialog.cancel();
            }
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
