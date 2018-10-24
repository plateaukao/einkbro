package de.baumann.browser.View;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.baumann.browser.Ninja.R;


public class NinjaToast {

    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }

    public static void show(Context context, String text) {

        Activity activity = (Activity) context;

        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_bottom, (ViewGroup) activity.findViewById(R.id.dialog_toast));

        TextView dialog_text = layout.findViewById(R.id.dialog_text);
        dialog_text.setText(text);

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM|Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();

        //Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
