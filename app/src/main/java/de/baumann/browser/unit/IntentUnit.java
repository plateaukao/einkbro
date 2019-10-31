package de.baumann.browser.unit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;

import de.baumann.browser.Ninja.R;

public class IntentUnit {

    private static final String INTENT_TYPE_MESSAGE_RFC822 = "message/rfc822";

    public static Intent getEmailIntent(MailTo mailTo) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { mailTo.getTo() });
        intent.putExtra(Intent.EXTRA_TEXT, mailTo.getBody());
        intent.putExtra(Intent.EXTRA_SUBJECT, mailTo.getSubject());
        intent.putExtra(Intent.EXTRA_CC, mailTo.getCc());
        intent.setType(INTENT_TYPE_MESSAGE_RFC822);

        return intent;
    }

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