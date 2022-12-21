package info.plateaukao.einkbro.activity

import android.app.Activity
import android.content.Intent
import info.plateaukao.einkbro.unit.HelperUnit.applyTheme
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.view.dialog.DialogManager

class SettingsActivity : AppCompatActivity() {
    private val backupUnit: BackupUnit by lazy { BackupUnit(this, this) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        applyTheme(this)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount >= 1) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
        //set correct title
        if (supportFragmentManager.backStackEntryCount == 1) {
            setTitle(R.string.settings)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DialogManager.EXPORT_BOOKMARKS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            backupUnit.exportBookmarks(lifecycleScope, uri)
        } else if (requestCode == DialogManager.IMPORT_BOOKMARKS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            backupUnit.importBookmarks(lifecycleScope, uri)
        }
    }
}