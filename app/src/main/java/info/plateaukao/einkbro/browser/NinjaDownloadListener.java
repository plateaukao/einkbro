package info.plateaukao.einkbro.browser;

import android.app.Activity;
import android.content.Context;
import android.webkit.DownloadListener;

import info.plateaukao.einkbro.unit.BrowserUnit;
import info.plateaukao.einkbro.unit.IntentUnit;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;

    public NinjaDownloadListener(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        if (context instanceof Activity) {
            BrowserUnit.download(context, url, contentDisposition, mimeType);
        }
    }
}
