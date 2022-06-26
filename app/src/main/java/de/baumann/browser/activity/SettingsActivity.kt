package de.baumann.browser.activity

import de.baumann.browser.unit.HelperUnit.applyTheme
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.Ninja.R
import de.baumann.browser.fragment.SettingsComposeFragment

class SettingsActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        applyTheme(this)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, SettingsComposeFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount > 1) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
            //set correct title
            if (supportFragmentManager.backStackEntryCount == 2) {
                setTitle(R.string.settings)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
    }
}