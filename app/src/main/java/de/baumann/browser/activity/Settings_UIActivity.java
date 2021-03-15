package de.baumann.browser.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceDialogFragmentCompat;

import android.view.MenuItem;

import de.baumann.browser.fragment.Fragment_settings_UI;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.HelperUnit;

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

        Fragment_settings_UI fragment = new Fragment_settings_UI();
        if(getIntent().getBooleanExtra("launch_toolbar_setting", false)) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("launch_toolbar_setting", true);
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
}
