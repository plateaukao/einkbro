package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.compose.ListScaffold
import info.plateaukao.einkbro.view.dialog.compose.DEFAULT_DESKTOP_VIEWPORT_WIDTH
import info.plateaukao.einkbro.view.dialog.compose.SiteSettingsContent
import info.plateaukao.einkbro.view.dialog.compose.TextEditorDialogFragment
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Full-screen host for the per-site settings on phones, where the centered
 * dialog is too cramped; tablets keep using SiteSettingsDialogFragment.
 * FragmentActivity (not LocaleAwareComponentActivity) because the custom
 * CSS/JS editors are DialogFragments.
 */
class SiteSettingsActivity : FragmentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val adBlock: AdBlock by inject()
    private val javascript: Javascript by inject()
    private val cookie: Cookie by inject()

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(LocaleManager.setLocale(newBase, config.uiLocaleLanguage))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(KEY_URL).orEmpty()
        val host = Uri.parse(url)?.host.orEmpty()

        setContent {
            ListScaffold(
                title = getString(R.string.site_settings),
                onBack = { finish() },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    SiteSettingsContent(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxHeight(),
                        host = host,
                        domainConfig = config.getDomainConfig(url),
                        globalFontSize = config.display.fontSize,
                        globalFontType = config.display.fontType,
                        globalBoldFont = config.display.boldFontStyle,
                        globalBlackFont = config.display.blackFontStyle,
                        globalFontBoldness = config.display.fontBoldness,
                        globalDesktopMode = config.browser.desktop,
                        defaultViewportWidth = DEFAULT_DESKTOP_VIEWPORT_WIDTH,
                        globalJavascript = config.browser.enableJavascript || javascript.isWhite(url),
                        globalAdBlock = config.browser.adBlock && !adBlock.isWhite(url),
                        globalCookies = config.browser.cookies || cookie.isWhite(url),
                        globalTranslationMode = config.translation.translationMode,
                        onEditText = { title, initial, onResult ->
                            TextEditorDialogFragment(title, initial, onResult)
                                .show(supportFragmentManager, "text_editor")
                        },
                        onSave = { updatedConfig ->
                            config.updateDomainConfig(updatedConfig)
                            setResult(RESULT_OK)
                            finish()
                        },
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_URL = "url"

        fun createIntent(context: Context, url: String) =
            Intent(context, SiteSettingsActivity::class.java).apply {
                putExtra(KEY_URL, url)
            }
    }
}
