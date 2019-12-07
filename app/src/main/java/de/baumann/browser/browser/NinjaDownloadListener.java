package de.baumann.browser.browser;

import android.app.Activity;
import android.content.Context;
import android.webkit.DownloadListener;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.IntentUnit;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;

    public NinjaDownloadListener(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        final Context holder = IntentUnit.getContext();
        if (!(holder instanceof Activity)) {
            BrowserUnit.download(context, url, contentDisposition, mimeType);
            return;
        }
    }
}
