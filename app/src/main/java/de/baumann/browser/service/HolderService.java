package de.baumann.browser.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.*;
import de.baumann.browser.view.NinjaContextWrapper;
import de.baumann.browser.view.NinjaWebView;

public class HolderService extends Service implements BrowserController {

    @Override
    public void updateAutoComplete() {}

    @Override
    public void hideOverview() {}

    @Override
    public void updateBookmarks() {}

    @Override
    public void updateProgress(int progress) {}

    @Override
    public void showAlbum(AlbumController albumController) {}

    @Override
    public void removeAlbum(AlbumController albumController) {}


    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {}

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {}

    @Override
    public boolean onHideCustomView() {
        return true;
    }

    @Override
    public void onLongPress(String url) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("sp_background", true)) {
            WebView.enableSlowWholeDocumentDraw();
            NinjaWebView webView = new NinjaWebView(new NinjaContextWrapper(this));

            webView.setBrowserController(this);
            webView.setAlbumCover(null);
            webView.setAlbumTitle(getString(R.string.app_name));
            ViewUnit.bound(this, webView);

            webView.loadUrl(RecordUnit.getHolder().getURL());
            webView.deactivate();

            BrowserContainer.add(webView);
            updateNotification();
        } else {
            Intent toActivity = new Intent(HolderService.this, BrowserActivity.class);
            toActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            toActivity.setAction(Intent.ACTION_SEND);
            toActivity.putExtra(Intent.EXTRA_TEXT, RecordUnit.getHolder().getURL());
            startActivity(toActivity);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (IntentUnit.isClear()) {
            BrowserContainer.clear();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification() {
        Notification notification = NotificationUnit.getHBuilder(this).build();
        startForeground(NotificationUnit.HOLDER_ID, notification);
        Toast.makeText(this, R.string.toast_load_in_background, Toast.LENGTH_LONG).show();
    }
}
