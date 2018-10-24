package de.baumann.browser.Database;

public class Record {
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

    private long time;
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    public Record() {
        this.title = null;
        this.url = null;
        this.time = 0L;
    }

    public Record(String title, String url, long time) {
        this.title = title;
        this.url = url;
        this.time = time;
    }
}
