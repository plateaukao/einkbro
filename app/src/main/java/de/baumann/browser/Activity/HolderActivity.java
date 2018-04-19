package de.baumann.browser.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

import de.baumann.browser.Database.Record;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Service.HolderService;
import de.baumann.browser.Unit.RecordUnit;

public class HolderActivity extends Activity {
    private static final int TIMER_SCHEDULE_DEFAULT = 512;

    private Record first = null;
    private Timer timer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }

        first = new Record();
        first.setTitle(getString(R.string.album_untitled));
        first.setURL(getIntent().getData().toString());
        first.setTime(System.currentTimeMillis());

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (first != null) {
                    Intent toService = new Intent(HolderActivity.this, HolderService.class);
                    toService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    RecordUnit.setHolder(first);
                    startService(toService);
                }
                HolderActivity.this.finish();
            }
        };
        timer = new Timer();
        timer.schedule(task, TIMER_SCHEDULE_DEFAULT);
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }

        first = null;
        timer = null;
        super.onDestroy();
    }
}
