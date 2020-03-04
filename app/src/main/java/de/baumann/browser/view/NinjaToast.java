package de.baumann.browser.view;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;


public class NinjaToast {

    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }

    public static void show(Context context, String text) {
        Activity activity = (Activity) context;
        Toast toast = new Toast(activity.getApplicationContext());
        toast.setText(text);
        toast.show();
    }
}