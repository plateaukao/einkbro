package de.baumann.browser.Unit;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import de.baumann.browser.Activity.BrowserActivity;
import de.baumann.browser.Browser.BrowserContainer;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Service.HolderService;

@SuppressWarnings("deprecation")
public class NotificationUnit {

    public static final int HOLDER_ID = 0x65536;

    public static NotificationCompat.Builder getHBuilder(Context context) {

        NotificationCompat.Builder builder;

        final BroadcastReceiver stopNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent toHolderService = new Intent(context, HolderService.class);
                toHolderService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                IntentUnit.setClear(false);
                context.stopService(toHolderService);
                BrowserContainer.clear();
            }
        };

        IntentFilter intentFilter = new IntentFilter("stopNotification");
        context.registerReceiver(stopNotificationReceiver, intentFilter);
        Intent stopNotification = new Intent("stopNotification");
        PendingIntent stopNotificationPI = PendingIntent.getBroadcast(context, 0, stopNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Action action_UN = new NotificationCompat.Action.Builder(R.drawable.icon_earth, context.getString(R.string.toast_closeNotification), stopNotificationPI).build();

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "browser_not";// The id of the channel.
            CharSequence name = context.getString(R.string.app_name);// The user-visible name of the channel.
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(mChannel);
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }

        builder.setCategory(Notification.CATEGORY_MESSAGE);
        builder.setSmallIcon(R.drawable.ic_notification_ninja);
        builder.setContentTitle(context.getString(R.string.notification_content_holderTitle));
        builder.setContentText(context.getString(R.string.notification_content_holder));
        builder.setColor(ContextCompat.getColor(context,R.color.colorAccent));
        builder.setAutoCancel(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setVibrate(new long[0]);
        builder.addAction(action_UN);

        Intent toActivity = new Intent(context, BrowserActivity.class);
        toActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pin = PendingIntent.getActivity(context, 0, toActivity, 0);
        builder.setContentIntent(pin);

        return builder;
    }
}
