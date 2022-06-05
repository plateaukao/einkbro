package de.baumann.browser.browser;

import android.view.View;

@SuppressWarnings("unused")
public interface AlbumController {

    View getAlbumView();

    String getAlbumTitle();

    void setAlbumTitle(String title);

    void activate();

    void deactivate();

    String getAlbumUrl();

    void pauseWebView();

    void resumeWebView();
}
