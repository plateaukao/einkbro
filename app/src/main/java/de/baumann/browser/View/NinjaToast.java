package de.baumann.browser.View;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
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

        try {
            // Create the Snackbar
            View view = activity.findViewById(R.id.main_content);
            Snackbar snackbar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT);
            // Get the Snackbar's layout view
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
            // Hide the text
            TextView textView2 = layout.findViewById(android.support.design.R.id.snackbar_text);
            textView2.setVisibility(View.INVISIBLE);
            // Inflate our custom view
            LayoutInflater mInflater = new LayoutInflater(activity ) {
                @Override
                public LayoutInflater cloneInContext(Context newContext) {
                    return null;
                }
            };
            View snackView = mInflater.inflate(R.layout.dialog_bottom_snackbar, null);
            TextView textView = snackView.findViewById(R.id.dialog_text);
            textView.setText(text);
            // Add the view to the Snackbar's layout
            layout.addView(snackView, 0);
            // Show the Snackbar
            snackbar.show();
        } catch (Exception e) {
            final BottomSheetDialog dialog = new BottomSheetDialog(activity);

            View dialogView = View.inflate(activity, R.layout.dialog_bottom_dialog, null);
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
}
