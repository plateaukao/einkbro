package de.baumann.browser.browser;

import de.baumann.browser.view.Album;

@SuppressWarnings("unused")
public interface AlbumController {

    Album getAlbum();

    String getAlbumTitle();

    void setAlbumTitle(String title);

    void activate();

    void deactivate();

    String getAlbumUrl();

    void pauseWebView();

    void resumeWebView();
}
