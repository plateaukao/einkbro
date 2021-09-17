package de.baumann.browser.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.fragment.UiSettingsFragment;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.util.Constants;

public class Settings_UIActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.applyTheme(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UiSettingsFragment fragment = new UiSettingsFragment();
        if(getIntent().getBooleanExtra(Constants.ARG_LAUNCH_TOOLBAR_SETTING, false)) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.ARG_LAUNCH_TOOLBAR_SETTING, true);
            fragment.setArguments(bundle);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        overridePendingTransition(0, 0);
    }
}
