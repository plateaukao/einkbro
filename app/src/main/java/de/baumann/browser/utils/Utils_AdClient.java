package de.baumann.browser.utils;

/*
* MIT License

Copyright (c) 2016 Aman tonk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
* */
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.HashMap;
import java.util.Map;
/**
 * Created by Ozymandias on 5/3/2017.
 * Abstraction of https://github.com/AmniX/AdBlockedWebView-Android
 */

public class Utils_AdClient extends WebViewClient {
    @Override
    public void onPageFinished(WebView view, String url) {

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.endsWith(".mp4")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);

            return true;
        } else if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:")
                || url.startsWith("mms:") || url.startsWith("mmsto:")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.getContext().startActivity(intent);

            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private Map<String, Boolean> loadedUrls = new HashMap<>();
    //could simply place this section inside and if/else statement
    //inside the activities webview client.  but this way there is a class
    // available to call that is separate from the activity and easier for
    // others to incorporate into their activities as well.
    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        boolean ad;
        if (!loadedUrls.containsKey(url)) {
            ad = Utils_AdBlocker.isAd(url);
            loadedUrls.put(url, ad);
        } else {
            ad = loadedUrls.get(url);
        }
        return ad ? Utils_AdBlocker.createEmptyResource() :
                super.shouldInterceptRequest(view, url);
    }
}
