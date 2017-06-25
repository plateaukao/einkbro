/*
    This file is part of the Browser WebApp.

    Browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.helper;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.util.Random;

import de.baumann.browser.Browser_1;
import de.baumann.browser.R;

public class Activity_intent extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());
        android.content.Intent intent = getIntent();

        Uri data = intent.getData();
        String url = data.toString();
        String domain = helper_webView.getDomain(this, url);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("add_readLater_link", url).apply();
        sharedPref.edit().putString("add_readLater_domain", domain).apply();

        Random rand = new Random();
        int n = rand.nextInt(100000);

        android.content.Intent iMain = new android.content.Intent();
        iMain.setData(data);
        iMain.setAction(Intent.ACTION_VIEW);
        iMain.setClassName(Activity_intent.this, "de.baumann.browser.Browser_1");

        android.content.Intent iAction = new android.content.Intent(this, Browser_1.class);
        iAction.setAction("readLater");

        android.content.Intent iAction_2 = new android.content.Intent(this, Activity_intent_add.class);
        iAction_2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent piMain = PendingIntent.getActivity(this, n, iMain, 0);
        PendingIntent piLL_1 = PendingIntent.getActivity(this, n, iAction, 0);
        PendingIntent piLL_2 = PendingIntent.getActivity(this, n, iAction_2, 0);

        PendingIntent piAction = PendingIntent.getActivity(this, n, iAction, 0);
        PendingIntent piAction_2 = PendingIntent.getActivity(this, n, iAction_2, 0);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder
                (R.drawable.format_list_bulleted, getString(R.string.readLater_action), piAction).build();
        NotificationCompat.Action action_2 = new NotificationCompat.Action.Builder
                (R.drawable.format_list_bulleted, getString(R.string.app_title_readLater), piAction_2).build();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.support.v4.app.NotificationCompat.Builder builderSummary =
                    new android.support.v4.app.NotificationCompat.Builder(Activity_intent.this)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.earth)
                            .setColor(ContextCompat.getColor(Activity_intent.this, R.color.colorPrimary_1))
                            .setGroup("Browser")
                            .setGroupSummary(true)
                            .setContentIntent(piMain);

            Notification notification = new NotificationCompat.Builder(Activity_intent.this)
                    .setColor(ContextCompat.getColor(Activity_intent.this, R.color.colorPrimary_1))
                    .setSmallIcon(R.drawable.earth)
                    .setContentTitle(getString(R.string.readLater_title) + " " + domain)
                    .setContentText(url)
                    .setContentIntent(piMain)
                    .setAutoCancel(true)
                    .setGroup("Browser")
                    .addAction(action)
                    .addAction(action_2)
                    .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText(data.toString()))
                    .setPriority(Notification.PRIORITY_MAX)
                    .setVibrate(new long[0])
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(n, notification);
            notificationManager.notify(0, builderSummary.build());
        } else {
            Notification notification = new NotificationCompat.Builder(this)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setContentTitle(getString(R.string.readLater_title) + " " + domain)
                    .setContentText(url)
                    .setSmallIcon(R.drawable.earth)
                    .setAutoCancel(true)
                    .addAction(R.drawable.earth, getString(R.string.readLater_action), piLL_1)
                    .addAction(R.drawable.format_list_bulleted, getString(R.string.app_title_readLater), piLL_2)
                    .setContentIntent(piMain)
                    .setPriority(Notification.PRIORITY_HIGH).build();
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(n, notification);
        }
        finish();
    }
}
