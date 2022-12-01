package info.plateaukao.einkbro.browser;

import android.view.GestureDetector;
import android.view.MotionEvent;

import info.plateaukao.einkbro.view.NinjaWebView;

public class NinjaGestureListener extends GestureDetector.SimpleOnGestureListener {
    private NinjaWebView webView;
    private boolean longPress = true;

    public NinjaGestureListener(NinjaWebView webView) {
        super();
        this.webView = webView;
    }

    public void setWebView(NinjaWebView webView) {
        this.webView = webView;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (longPress) {
            webView.onLongPress(e);
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        longPress = false;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        longPress = true;
    }
}
