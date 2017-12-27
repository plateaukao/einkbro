package de.baumann.browser.View;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import de.baumann.browser.Ninja.R;


public class NinjaToast {

    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }

    public static void show(Context context, String text) {

        Activity activity = (Activity) context;
        final BottomSheetDialog dialog = new BottomSheetDialog(activity);

        View dialogView = View.inflate(activity, R.layout.dialog_bottom, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(text);
        dialog.setContentView(dialogView);
        //noinspection ConstantConditions
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
            }
        }, 2000);
    }
}
