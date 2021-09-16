package de.baumann.browser.browser;

import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

public interface BrowserController {
    void updateAutoComplete();
    void updateProgress(int progress);
    void updateTitle(String title);
    void addNewTab(String url);
    void showAlbum(AlbumController albumController);
    void removeAlbum(AlbumController albumController);
    void showFileChooser(ValueCallback<Uri[]> filePathCallback);
    void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
    void onLongPress(String url);
    void hideOverview ();
    void addHistory(String url);
    boolean onHideCustomView();
    boolean handleKeyEvent(KeyEvent event);
}
