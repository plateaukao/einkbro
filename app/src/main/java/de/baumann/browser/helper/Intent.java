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
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

import java.util.Random;

import de.baumann.browser.R;
import de.baumann.browser.databases.Database_ReadLater;
import de.baumann.browser.popups.Popup_readLater;

public class Intent extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());
        android.content.Intent intent = getIntent();

        Uri data = intent.getData();
        String link = data.toString();
        int domainInt = link.indexOf("//") + 2;
        final  String domain = link.substring(domainInt, link.indexOf('/', domainInt));

        try {
            final Database_ReadLater db = new Database_ReadLater(Intent.this);
            db.addBookmark(domain, link);
            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Random rand = new Random();
        int n = rand.nextInt(100000); // Gives n such that 0 <= n < 20

        android.content.Intent iMain = new android.content.Intent();
        iMain.setAction(android.content.Intent.ACTION_VIEW);
        iMain.putExtra("url", link);
        iMain.putExtra("title", domain);
        iMain.setClassName(Intent.this, "de.baumann.browser.Browser");

        android.content.Intent iAction = new android.content.Intent(this, Popup_readLater.class);
        iAction.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent piMain = PendingIntent.getActivity(this, n, iMain, 0);
        PendingIntent piAction = PendingIntent.getActivity(this, n, iAction, 0);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder
                (R.drawable.format_list_bulleted, getString(R.string.readLater_action), piAction).build();

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.earth)
                .setContentTitle(getString(R.string.readLater_title))
                .setContentText(link)
                .setContentIntent(piMain)
                .setAutoCancel(true)
                .addAction(action)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[0])
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        finish();

    }
}
