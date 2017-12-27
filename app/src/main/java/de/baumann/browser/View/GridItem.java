package de.baumann.browser.View;

public class GridItem {
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

    private String filename;
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    private int ordinal;
    public int getOrdinal() {
        return ordinal;
    }
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public GridItem() {
        this.title = null;
        this.url = null;
        this.filename = null;
        this.ordinal = -1;
    }

    public GridItem(String title, String url, String filename, int ordinal) {
        this.title = title;
        this.url = url;
        this.filename = filename;
        this.ordinal = ordinal;
    }
}
