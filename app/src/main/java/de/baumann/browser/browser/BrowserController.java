package de.baumann.browser.browser;

import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

public interface BrowserController {
    void updateAutoComplete();

    void updateBookmarks();

    void updateProgress(int progress);

    void showAlbum(AlbumController albumController);

    void removeAlbum(AlbumController albumController);

    void showFileChooser(ValueCallback<Uri[]> filePathCallback);

    void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);

    boolean onHideCustomView();

    void onLongPress(String url);

    void hideOverview ();
}
