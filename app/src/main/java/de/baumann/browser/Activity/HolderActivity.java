package de.baumann.browser.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.baumann.browser.Database.Record;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Service.HolderService;
import de.baumann.browser.Unit.BrowserUnit;
import de.baumann.browser.Unit.IntentUnit;
import de.baumann.browser.Unit.RecordUnit;
import de.baumann.browser.View.DialogAdapter;
import de.baumann.browser.View.NinjaContextWrapper;

public class HolderActivity extends Activity {
    private static final int TIMER_SCHEDULE_DEFAULT = 512;

    private Record first = null;
    private Record second = null;
    private Timer timer = null;
    private boolean background = false;

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
                if (first != null && second == null) {
                    Intent toService = new Intent(HolderActivity.this, HolderService.class);
                    RecordUnit.setHolder(first);
                    startService(toService);
                    background = true;
                }
                HolderActivity.this.finish();
            }
        };
        timer = new Timer();
        timer.schedule(task, TIMER_SCHEDULE_DEFAULT);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null || intent.getData() == null || first == null) {
            finish();
            return;
        }

        if (timer != null) {
            timer.cancel();
        }

        second = new Record();
        second.setTitle(getString(R.string.album_untitled));
        second.setURL(intent.getData().toString());
        second.setTime(System.currentTimeMillis());

        if (first.getURL().equals(second.getURL())) {
            showHolderDialog();
        } else {
            Intent toService = new Intent(HolderActivity.this, HolderService.class);
            RecordUnit.setHolder(second);
            startService(toService);
            background = true;
            finish();
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }

        if (background) {
            Toast.makeText(this, R.string.toast_load_in_background, Toast.LENGTH_LONG).show();
        }

        first = null;
        second = null;
        timer = null;
        background = false;
        super.onDestroy();
    }

    private void showHolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new NinjaContextWrapper(this));
        builder.setCancelable(true);

        @SuppressLint("InflateParams")
        FrameLayout linearLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.dialog_list, null, false);
        builder.setView(linearLayout);

        String[] strings = getResources().getStringArray(R.array.holder_menu);
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(strings));

        ListView listView = linearLayout.findViewById(R.id.dialog_list);
        DialogAdapter adapter = new DialogAdapter(this, list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        final AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                HolderActivity.this.finish();
            }
        });
        dialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Intent toActivity = new Intent(HolderActivity.this, BrowserActivity.class);
                        toActivity.putExtra(IntentUnit.OPEN, first.getURL());
                        startActivity(toActivity);
                        break;
                    case 1:
                        BrowserUnit.copyURL(HolderActivity.this, first.getURL());
                        break;
                    case 2:
                        IntentUnit.share(HolderActivity.this, first.getTitle(), first.getURL());
                        break;
                    default:
                        break;
                }
                dialog.hide();
                dialog.dismiss();
                finish();
            }
        });
    }
}
