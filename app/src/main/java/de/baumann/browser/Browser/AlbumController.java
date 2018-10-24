package de.baumann.browser.Browser;

import android.graphics.Bitmap;
import android.view.View;

@SuppressWarnings("unused")
public interface AlbumController {
    int getFlag();

    void setFlag(int flag);

    View getAlbumView();

    void setAlbumCover(Bitmap bitmap);

    String getAlbumTitle();

    void setAlbumTitle(String title);

    void activate();

    void deactivate();
}
