package de.baumann.browser.browser;

import android.graphics.Bitmap;
import android.view.View;

@SuppressWarnings("unused")
public interface AlbumController {

    View getAlbumView();

    void setAlbumCover(Bitmap bitmap);

    String getAlbumTitle();

    void setAlbumTitle(String title);

    void activate();

    void deactivate();
}
