package info.plateaukao.einkbro.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.util.Constants.Companion.ACTION_DICT
import info.plateaukao.einkbro.view.dialog.compose.TranslateDialogFragment
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import org.koin.android.ext.android.inject

class DictActivity : AppCompatActivity() {
    private val config: ConfigManager by inject()
    private val translationViewModel: TranslationViewModel by viewModels()
    private val webView: WebView by lazy {
        BrowserUnit.createNaverDictWebView(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dict)

        hideStatusBar()

        if (intent.action != null) {
            onNewIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            in listOf("colordict.intent.action.PICK_RESULT", "colordict.intent.action.SEARCH") -> {
                if (!config.externalSearchWithGpt) {
                    forwardDictIntentAndFinish()
                } else {
                    val text = intent.getStringExtra("EXTRA_QUERY") ?: return
                    searchWithPopup(text)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                if (!config.externalSearchWithGpt && !config.externalSearchWithPopUp) {
                    forwardProcessTextIntentAndFinish()
                } else {
                    val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: return
                    searchWithPopup(text)
                }
            }
        }
    }

    private fun searchWithPopup(text: String) {
        translationViewModel.updateInputMessage(text)
        val fragment = TranslateDialogFragment(
            translationViewModel,
            webView,
            Point(50, 50),
        ) {
            finish()
//            supportFragmentManager.popBackStack()
//            moveTaskToBack(true)
        }
        supportFragmentManager.beginTransaction().add(fragment, "contextMenu").addToBackStack(null)
            .commit()

    }

    private fun forwardDictIntentAndFinish() {
        val newIntent = Intent(this, BrowserActivity::class.java).apply {
            action = ACTION_DICT
            putExtra("EXTRA_QUERY", intent.getStringExtra("EXTRA_QUERY"))
        }
        startActivity(newIntent)
        finish()
    }

    private fun forwardProcessTextIntentAndFinish() {
        val newIntent = Intent(this, BrowserActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            putExtra(Intent.EXTRA_PROCESS_TEXT, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
        }
        startActivity(newIntent)
        finish()
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars())
            controller?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            window.setDecorFitsSystemWindows(false)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }
}