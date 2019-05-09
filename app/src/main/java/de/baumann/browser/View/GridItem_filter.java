package de.baumann.browser.View;

import android.graphics.drawable.Drawable;

public class GridItem_filter {
    private String title;
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    private String url;
    public String getURL() {
        return url;
    }
    public void setURL(String url) {
        this.url = url;
    }

    private final Drawable icon;
    public Drawable getIcon() {
        return icon;
    }

    private final String ordinal;
    public String getOrdinal() {
        return ordinal;
    }

    public GridItem_filter(String title, String url, Drawable icon, String ordinal) {
        this.title = title;
        this.url = url;
        this.icon = icon;
        this.ordinal = ordinal;
    }
}
