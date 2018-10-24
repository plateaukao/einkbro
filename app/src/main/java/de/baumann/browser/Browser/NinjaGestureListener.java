package de.baumann.browser.Browser;

import android.view.GestureDetector;
import android.view.MotionEvent;
import de.baumann.browser.View.NinjaWebView;

public class NinjaGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final NinjaWebView webView;
    private boolean longPress = true;

    public NinjaGestureListener(NinjaWebView webView) {
        super();
        this.webView = webView;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (longPress) {
            webView.onLongPress();
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
