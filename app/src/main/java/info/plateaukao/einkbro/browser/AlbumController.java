package info.plateaukao.einkbro.browser;

import info.plateaukao.einkbro.view.Album;

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
