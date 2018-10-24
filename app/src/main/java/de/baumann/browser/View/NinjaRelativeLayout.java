package de.baumann.browser.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import de.baumann.browser.Browser.AlbumController;
import de.baumann.browser.Browser.BrowserController;
import de.baumann.browser.Ninja.R;

public class NinjaRelativeLayout extends RelativeLayout implements AlbumController {
    private final Context context;
    private final Album album;
    private int flag = 0;

    private BrowserController controller;
    public void setBrowserController(BrowserController controller) {
        this.controller = controller;
        this.album.setBrowserController(controller);
    }

    public NinjaRelativeLayout(Context context) {
        this(context, null);
    }

    public NinjaRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NinjaRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.album = new Album(context, this, this.controller);
        initUI();
    }

    private void initUI() {
        album.setAlbumCover(null);
        album.setAlbumTitle(context.getString(R.string.album_untitled));
        album.setBrowserController(controller);
    }

    @Override
    public int getFlag() {
        return flag;
    }

    @Override
    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    @Override
    public void setAlbumCover(Bitmap bitmap) {
        album.setAlbumCover(bitmap);
    }

    @Override
    public String getAlbumTitle() {
        return album.getAlbumTitle();
    }

    @Override
    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public void activate() {
        album.activate();
    }

    @Override
    public void deactivate() {
        album.deactivate();
    }
}
