package de.baumann.browser.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.Objects;

public class HolderActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String url = Objects.requireNonNull(getIntent().getData()).toString();

        final Intent toActivity = new Intent(HolderActivity.this, BrowserActivity.class);
        toActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        toActivity.setAction(Intent.ACTION_SEND);
        toActivity.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(toActivity);
        finish();
    }
}
