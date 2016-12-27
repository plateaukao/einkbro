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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;

import java.util.Random;

import de.baumann.browser.R;
import de.baumann.browser.popups.Popup_readLater;

public class Activity_intent extends Activity {

    private String linkIntent2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());
        android.content.Intent intent = getIntent();

        Uri data = intent.getData();
        String linkIntent = data.toString();

        if (linkIntent.contains("https://")) {
            linkIntent2 = linkIntent.replace("https://", "|");
        } else if (linkIntent.contains("http://")){
            linkIntent2 = linkIntent.replace("http://", "|");
        }

        String linkIntent3;
        if (linkIntent2.contains("www.")) {
            linkIntent3 = linkIntent2.replace("www.", "").toUpperCase();
        } else {
            linkIntent3 = linkIntent2.toUpperCase();
        }

        String domain;
        if (linkIntent3.contains("/")) {
            domain = linkIntent3.substring(linkIntent3.indexOf('|')+1, linkIntent3.indexOf('/'));
        } else {
            domain = linkIntent3.substring(linkIntent3.indexOf('|')+1, linkIntent3.lastIndexOf('.'));
        }

        String domain2 = domain.substring(0,1).toUpperCase() + domain.substring(1).toLowerCase();

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("add_readLater_link", linkIntent).apply();
        sharedPref.edit().putString("add_readLater_domain", domain2).apply();

        Random rand = new Random();
        int n = rand.nextInt(100000); // Gives n such that 0 <= n < 20

        android.content.Intent iMain = new android.content.Intent();
        iMain.putExtra("url", linkIntent);
        iMain.setClassName(Activity_intent.this, "de.baumann.browser.Browser");

        android.content.Intent iAction = new android.content.Intent(this, Popup_readLater.class);
        iAction.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        android.content.Intent iAction_2 = new android.content.Intent(this, Activity_intent_add.class);
        iAction_2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent piMain = PendingIntent.getActivity(this, n, iMain, 0);
        PendingIntent piAction = PendingIntent.getActivity(this, n, iAction, 0);
        PendingIntent piAction_2 = PendingIntent.getActivity(this, n, iAction_2, 0);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder
                (R.drawable.format_list_bulleted, getString(R.string.readLater_action), piAction).build();
        NotificationCompat.Action action_2 = new NotificationCompat.Action.Builder
                (R.drawable.format_list_bulleted, getString(R.string.readLater_action2), piAction_2).build();

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.earth)
                .setContentTitle(getString(R.string.readLater_title))
                .setContentText(linkIntent)
                .setContentIntent(piMain)
                .setAutoCancel(true)
                .addAction(action)
                .addAction(action_2)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[0])
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        finish();
    }
}
