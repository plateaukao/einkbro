package de.baumann.browser.View;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

import de.baumann.browser.Unit.HelperUnit;

public class NinjaContextWrapper extends ContextWrapper {
    private final Context context;

    public NinjaContextWrapper(Context context) {
        super(context);
        this.context = context;
        HelperUnit.setTheme(context);
    }

    @Override
    public Resources.Theme getTheme() {
        return context.getTheme();
    }
}
