package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class FullscreenHolder extends FrameLayout {
    public FullscreenHolder(Context context) {
        super(context);
        this.setBackgroundColor(ContextCompat.getColor(context,(android.R.color.black)));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
