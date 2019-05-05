package de.baumann.browser.View;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;

import java.util.Objects;

import de.baumann.browser.Browser.AlbumController;
import de.baumann.browser.Browser.BrowserController;
import de.baumann.browser.Ninja.R;

@SuppressWarnings({"WeakerAccess"})
class Album {
    private final Context context;

    private View albumView;
    public View getAlbumView() {
        return albumView;
    }

    private ImageView albumCover;
    public void setAlbumCover(Bitmap bitmap) {
        albumCover.setImageBitmap(bitmap);
    }

    private TextView albumTitle;
    public String getAlbumTitle() {
        return albumTitle.getText().toString();
    }
    public void setAlbumTitle(String title) {
        albumTitle.setText(title);
    }

    private final AlbumController albumController;

    private BrowserController browserController;
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    public Album(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }


    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.album, null, false);

        albumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                browserController.showAlbum(albumController);
                browserController.hideOverview();
            }
        });

        albumView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                browserController.removeAlbum(albumController);
                return true;
            }
        });

        ImageView albumClose = albumView.findViewById(R.id.album_close);
        albumCover = albumView.findViewById(R.id.album_cover);
        albumTitle = albumView.findViewById(R.id.album_title);
        albumTitle.setText(context.getString(R.string.album_untitled));

        albumClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                browserController.removeAlbum(albumController);
            }
        });
    }

    public void activate() {
        albumView.setBackgroundResource(R.drawable.album_shape_accent);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int color = sp.getInt("vibrantColor", 0);

        Drawable shape = context.getDrawable(R.drawable.album_shape_accent);
        Objects.requireNonNull(shape).setTint(color);
        shape.setTintMode(PorterDuff.Mode.SRC_OVER);
    }

    public void deactivate() {
        albumView.setBackgroundResource(R.drawable.album_shape_transparent);
    }
}
