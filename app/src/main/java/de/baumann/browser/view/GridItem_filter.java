package de.baumann.browser.view;

import android.graphics.drawable.Drawable;

public class GridItem_filter {
    private final String title;
    public String getTitle() {
        return title;
    }

    private final String url;
    public String getURL() {
        return url;
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
