package de.baumann.browser.browser;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;

import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.IntentUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {

    private final NinjaWebView ninjaWebView;
    private final Context context;
    private final SharedPreferences sp;
    private final AdBlock adBlock;
    private final Cookie cookie;

    private boolean white;
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

    boolean isReaderJsInjected = false;
    @Override
    public void onPageFinished(WebView view, String url) {
        if (sp.getBoolean("saveHistory", true)) {
            RecordAction action = new RecordAction(context);
            action.open(true);
            if (action.checkHistory(url)) {
                action.deleteHistoryItemByURL(url);
                action.addHistory(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis()));
            } else {
                action.addHistory(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis()));
            }
            action.close();
        }
        /* Daniel testing codes for reader mode
        if (!isReaderJsInjected) {
            injectScriptFile(view, "readability.js");
            isReaderJsInjected = true;
        } else {
            isReaderJsInjected = false;
        }
         */
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

        String url = uri.toString();
        Uri parsedUri = Uri.parse(url);
        PackageManager packageManager = context.getPackageManager();
        Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);

        if (url.startsWith("http")) {
            webView.loadUrl(url, ninjaWebView.getRequestHeaders());
            return true;
        }

        if (browseIntent.resolveActivity(packageManager) != null) {
            context.startActivity(browseIntent);
            return true;
        }

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
        return true;//do nothing in other cases
    }

    private WebResourceResponse webResourceResponse = new WebResourceResponse(
            BrowserUnit.MIME_TYPE_TEXT_PLAIN,
            BrowserUnit.URL_ENCODING,
            new ByteArrayInputStream("".getBytes())
    );

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (enable && !white && adBlock.isAd(url)) { return webResourceResponse; }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {
            if (cookie.isWhite(url)) {
                CookieManager manager = CookieManager.getInstance();
                manager.getCookie(url);
                manager.setAcceptCookie(true);
            } else {
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
            } else {
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
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

        String message = "\"SSL Certificate error.\"";
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                message = "\"Certificate authority is not trusted.\"";
                break;
            case SslError.SSL_EXPIRED:
                message = "\"Certificate has expired.\"";
                break;
            case SslError.SSL_IDMISMATCH:
                message = "\"Certificate Hostname mismatch.\"";
                break;
            case SslError.SSL_NOTYETVALID:
                message = "\"Certificate is not yet valid.\"";
                break;
            case SslError.SSL_DATE_INVALID:
                message = "\"Certificate date is invalid.\"";
                break;
            case SslError.SSL_INVALID:
                message = "\"Certificate is invalid.\"";
                break;
        }
        String text = message + " - " + context.getString(R.string.dialog_content_ssl_error);

        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(text);
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

    public void injectScriptFile(WebView view, String scriptFile) {
        InputStream input;
        try {
            input = view.getContext().getAssets().open(scriptFile);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            // String-ify the script byte-array using BASE64 encoding !!!
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            view.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
