package de.baumann.browser.unit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import de.baumann.browser.Ninja.R;

public class IntentUnit {

    public static void share(Context context, String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
    }

    // activity holder
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    public static void setContext(Context holder) {
        context = holder;
    }
    public static Context getContext() {
        return context;
    }
}