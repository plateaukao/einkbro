package de.baumann.browser.Unit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import de.baumann.browser.Activity.BrowserActivity;
import de.baumann.browser.Ninja.R;

@SuppressWarnings("deprecation")
public class NotificationUnit {

    public static final int HOLDER_ID = 0x65536;

    public static Notification.Builder getHBuilder(Context context) {

        Notification.Builder builder;

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "browser_not";// The id of the channel.
            CharSequence name = context.getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_MAX;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String theme = sp.getString("theme", "0");
        int color;

        switch (theme) {
            case "5":case "6":case "8":
                color = ContextCompat.getColor(context,R.color.colorAccent_grey);
                break;
            case "7":
                color = ContextCompat.getColor(context,R.color.colorAccent_brown);
                break;
            case "9":
                color = ContextCompat.getColor(context,R.color.colorAccent_darkGrey);
                break;
            default:
                color = ContextCompat.getColor(context,R.color.colorAccent);
        }

        builder.setCategory(Notification.CATEGORY_MESSAGE);
        builder.setSmallIcon(R.drawable.ic_notification_ninja);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.notification_content_holder));
        builder.setColor(color);
        builder.setAutoCancel(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setVibrate(new long[0]);

        Intent toActivity = new Intent(context, BrowserActivity.class);
        PendingIntent pin = PendingIntent.getActivity(context, 0, toActivity, 0);
        builder.setContentIntent(pin);

        return builder;
    }
}
