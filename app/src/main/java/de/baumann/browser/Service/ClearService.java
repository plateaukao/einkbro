package de.baumann.browser.Service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.BrowserUnit;

public class ClearService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        System.exit(0); // For remove all WebView thread
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        clear();
        stopSelf();
        return START_STICKY;
    }

    private void clear() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean clearBookmarks = sp.getBoolean(getString(R.string.sp_clear_bookmarks), false);
        boolean clearCache = sp.getBoolean(getString(R.string.sp_clear_cache), false);
        boolean clearCookie = sp.getBoolean(getString(R.string.sp_clear_cookie), false);
        boolean clearHistory = sp.getBoolean(getString(R.string.sp_clear_history), false);

        if (clearBookmarks) {
            BrowserUnit.clearBookmarks(this);
        }
        if (clearCache) {
            BrowserUnit.clearCache(this);
        }
        if (clearCookie) {
            BrowserUnit.clearCookie();
        }
        if (clearHistory) {
            BrowserUnit.clearHistory(this);
        }
    }
}
