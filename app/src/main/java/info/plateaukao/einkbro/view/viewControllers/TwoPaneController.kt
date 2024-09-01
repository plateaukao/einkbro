package info.plateaukao.einkbro.view.viewControllers

import android.app.Activity
import android.net.Uri
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.TranslationPanelBinding
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.NinjaWebView.OnScrollChangeListener
import info.plateaukao.einkbro.view.Orientation
import info.plateaukao.einkbro.view.TwoPaneLayout
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Math.abs

class TwoPaneController(
    private val activity: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val translationViewBinding: TranslationPanelBinding,
    private val twoPaneLayout: TwoPaneLayout,
    private val showTranslationAction: () -> Unit,
    private val onTranslationClosed: () -> Unit,
    private val loadTranslationUrl: (String) -> Unit,
    private val translateByParagraph: (TRANSLATE_API, NinjaWebView) -> Unit,
    private val translateByScreen: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val webView: NinjaWebView by lazy {
        NinjaWebView(activity, null).apply {
            shouldHideTranslateContext = true
            setScrollChangeListener(object : OnScrollChangeListener {
                override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                    if (abs(scrollY - oldScrollY) > 10) {
                        hideControlButtons()
                    }
                }
            })
        }
    }

    private var isWebViewAdded: Boolean = false

    init {
        twoPaneLayout.setOrientation(config.translationOrientation)
        if (config.translationPanelSwitched) twoPaneLayout.post {
            twoPaneLayout.switchPanels()
        }


        translationViewBinding.translationFontPlus.setOnClickListener { increaseFontSize() }
        translationViewBinding.translationFontMinus.setOnClickListener { decreaseFontSize() }

        translationViewBinding.translationClose.setOnClickListener {
            toggleTranslationWindow(
                false, onTranslationClosed
            )
        }
        translationViewBinding.translationClose.setOnLongClickListener {
            hideControlButtons()
            true
        }

        translationViewBinding.translationOrientation.setImageResource(
            if (twoPaneLayout.getOrientation() == Orientation.Vertical) R.drawable.ic_split_screen
            else R.drawable.ic_split_screen_vertical
        )

        translationViewBinding.translationOrientation.setOnClickListener {
            val orientation =
                if (twoPaneLayout.getOrientation() == Orientation.Vertical) Orientation.Horizontal else Orientation.Vertical
            setOrientation(orientation)
        }

        translationViewBinding.translationOrientation.setOnLongClickListener {
            config::translationPanelSwitched.toggle()
            twoPaneLayout.switchPanels()
            true
        }

        translationViewBinding.linkHere.setOnClickListener {
            config::twoPanelLinkHere.toggle()
            updateLinkHereView(config.twoPanelLinkHere)
        }
        updateLinkHereView(config.twoPanelLinkHere)

        translationViewBinding.syncScroll.setOnClickListener {
            config::translationScrollSync.toggle()
            updateSyncScrollView(config.translationScrollSync)
        }
        updateSyncScrollView(config.translationScrollSync)

        translationViewBinding.expandedButton.setOnClickListener { showControlButtons() }

        val languageView = translationViewBinding.translationLanguage
        ViewUnit.updateLanguageLabel(languageView, config.translationLanguage)
        translationViewBinding.translationLanguage.setOnClickListener {
            lifecycleScope.launch {
                val translationLanguage =
                    TranslationLanguageDialog(activity).show() ?: return@launch
                ViewUnit.updateLanguageLabel(languageView, translationLanguage)
                translateWithNewLanguage(translationLanguage)
            }
        }
    }

    private fun hideControlButtons() {
        translationViewBinding.controlsContainer.visibility = INVISIBLE
        translationViewBinding.expandedButton.visibility = VISIBLE
    }

    private fun showControlButtons() {
        translationViewBinding.controlsContainer.visibility = VISIBLE
        translationViewBinding.expandedButton.visibility = INVISIBLE
    }

    private fun translateWithNewLanguage(translationLanguage: TranslationLanguage) {
        val uri = Uri.parse(webView.url)
        val newUri = uri.removeQueryParam("_x_tr_tl").buildUpon()
            .appendQueryParameter("_x_tr_tl", translationLanguage.value) // source language
            .build()
        webView.loadUrl(newUri.toString())
    }

    private fun Uri.removeQueryParam(key: String): Uri {
        val builder = buildUpon().clearQuery()

        queryParameterNames.filter { it != key }
            .onEach { builder.appendQueryParameter(it, getQueryParameter(it)) }

        return builder.build()
    }

    private fun showSecondPane() {
        if (!isWebViewAdded) {
            addWebView()
            isWebViewAdded = true
        }

        translationViewBinding.translationLanguage.visibility = GONE
        translationViewBinding.linkHere.visibility = VISIBLE
        twoPaneLayout.shouldShowSecondPane = true
    }

    fun showSecondPaneWithData(data: String) {
        showSecondPane()
        webView.loadData(data, "text/html", "utf-8")
    }

    fun showSecondPaneWithUrl(url: String) {
        showSecondPane()
        webView.loadUrl(url)
    }

    fun hideSecondPane() {
        toggleTranslationWindow(false)
    }

    fun isSecondPaneDisplayed(): Boolean = twoPaneLayout.shouldShowSecondPane

    fun showTranslation(webView: NinjaWebView) {
        when (config.translationMode) {
            TranslationMode.PAPAGO_DUAL -> webView.loadUrl(buildPUrlTranslateUrl(webView.url.toString()))
            TranslationMode.PAPAGO_URL, TranslationMode.GOOGLE_URL -> launchTranslateWindow(webView.url.toString())
            TranslationMode.ONYX, TranslationMode.PAPAGO, TranslationMode.GOOGLE ->
                NinjaToast.showShort(activity, "No more supported")

            TranslationMode.GOOGLE_IN_PLACE -> webView.addGoogleTranslation()
            TranslationMode.TRANSLATE_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.GOOGLE, webView)
            TranslationMode.PAPAGO_TRANSLATE_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.PAPAGO, webView)
            TranslationMode.PAPAGO_TRANSLATE_BY_SCREEN -> translateByScreen()
        }
    }

    fun scrollChange(offset: Int) {
        if (config.translationScrollSync) {
            webView.scrollBy(0, offset)
            webView.scrollY = kotlin.math.max(0, webView.scrollY)
        }
    }

    private fun setOrientation(orientation: Orientation) {
        config.translationOrientation = orientation
        twoPaneLayout.setOrientation(orientation)
        translationViewBinding.translationOrientation.setImageResource(
            if (twoPaneLayout.getOrientation() == Orientation.Vertical) R.drawable.ic_split_screen
            else R.drawable.ic_split_screen_vertical
        )
    }

    private fun updateSyncScrollView(shouldSyncScroll: Boolean = false) {
        val drawable =
            if (shouldSyncScroll) R.drawable.selected_border_bg else R.drawable.background_with_border
        translationViewBinding.syncScroll.setBackgroundResource(drawable)
    }

    private fun updateLinkHereView(shouldLinkHere: Boolean = false) {
        val drawable =
            if (shouldLinkHere) R.drawable.selected_border_bg else R.drawable.background_with_border
        translationViewBinding.linkHere.setBackgroundResource(drawable)
    }

    private fun launchTranslateWindow(text: String) {
        if (text == "null") {
            NinjaToast.showShort(activity, "Translation does not work for this page.")
            return
        }

        // webview cases: google, papago
        if (!isWebViewAdded) {
            addWebView()
            isWebViewAdded = true
        }

        translationViewBinding.linkHere.visibility = GONE
        translationViewBinding.translationLanguage.visibility =
            if (config.translationMode == TranslationMode.GOOGLE_URL) VISIBLE else GONE

        twoPaneLayout.shouldShowSecondPane = true

        // handle translate url
        if (config.translationMode == TranslationMode.PAPAGO_URL) {
            translateUrl(buildPUrlTranslateUrl(text))
            return
        } else if (config.translationMode == TranslationMode.GOOGLE_URL) {
            translateUrl(buildGUrlTranslateUrl(text))
            return
        }

    }

    fun showTranslationConfigDialog(translateDirectly: Boolean) {
        val enumValues: List<TranslationMode> = TranslationMode.entries.toMutableList().apply {
            // remove not supported translation modes
            remove(TranslationMode.ONYX)
            remove(TranslationMode.PAPAGO)
            remove(TranslationMode.GOOGLE)
            if (config.papagoApiSecret.isBlank()) {
                remove(TranslationMode.PAPAGO_TRANSLATE_BY_PARAGRAPH)
                remove(TranslationMode.PAPAGO_TRANSLATE_BY_SCREEN)
            }
        }

        val translationModeArray =
            enumValues.map { activity.getString(it.labelResId) }.toTypedArray()
        val valueArray = enumValues.map { it.ordinal }
        val selected = valueArray.indexOf(config.translationMode.ordinal)
        AlertDialog.Builder(activity, R.style.TouchAreaDialog).apply {
            setTitle(context.getString(R.string.translation_mode))
            setSingleChoiceItems(translationModeArray, selected) { dialog, which ->
                dialog.dismiss()
                config.translationMode = enumValues[which]
                if (translateDirectly) showTranslationAction.invoke()
            }
        }.create().also {
            it.show()
            it.window?.setLayout(300.dp(activity), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun String.purify(): String =
        this.replace("\\u003C", "<").replace("\\n", "\n").replace("\\t", "  ").replace("\\\"", "\"")

    private fun translateUrl(url: String) {
        webView.loadUrl(url)
    }

    private fun addWebView(): NinjaWebView {
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
        )
        translationViewBinding.root.addView(webView, 0, params)

        return webView
    }

    private fun buildPUrlTranslateUrl(url: String): String {
        val uri = Uri.Builder().scheme("https").authority("papago.naver.net").path("website")
            .appendQueryParameter("locale", "en").appendQueryParameter("source", "auto")
            .appendQueryParameter("target", "ja").appendQueryParameter("url", url).build()
        return uri.toString()
    }

    private fun buildPTranslateUrl(text: String): String {
        val shortenedText: String = if (text.length > TRANSLATION_TEXT_THRESHOLD) text.substring(
            0, TRANSLATION_TEXT_THRESHOLD
        ) else text
        val uri = Uri.Builder().scheme("https").authority("papago.naver.com")
            .appendQueryParameter("st", shortenedText).build()
        return uri.toString()
    }

    private fun buildGUrlTranslateUrl(url: String): String {
        val uri = Uri.parse(url)
        val newUri = uri.buildUpon().scheme("https")
            .authority(uri.authority?.replace(".", "-") + ".translate.goog")
            .appendQueryParameter("_x_tr_sl", "auto")
            .appendQueryParameter("_x_tr_tl", config.translationLanguage.value) // source language
            .appendQueryParameter("_x_tr_pto", "ajax,elem") // target language
            .build()
        return newUri.toString()
    }

    private fun buildGTranslateUrl(text: String): String {
        val shortenedText: String = if (text.length > TRANSLATION_TEXT_THRESHOLD) text.substring(
            0, TRANSLATION_TEXT_THRESHOLD
        ) else text
        val uri = Uri.Builder().scheme("https").authority("translate.google.com")
            .appendQueryParameter("text", shortenedText)
            .appendQueryParameter("sl", "auto") // source language
            .appendQueryParameter("tl", "jp") // target language
            .build()
        return uri.toString()
    }


    private fun toggleTranslationWindow(
        isEnabled: Boolean, onTranslationClosed: () -> Unit = {}
    ) {
        if (!isEnabled) {
            webView.loadUrl(BrowserUnit.URL_ABOUT_BLANK)
            twoPaneLayout.shouldShowSecondPane = false
            onTranslationClosed()
        }
    }

    private fun increaseFontSize() {
        webView.settings.textZoom += 20
    }

    private fun decreaseFontSize() {
        if (webView.settings.textZoom > 20) webView.settings.textZoom -= 20
    }

    companion object {
        private const val TRANSLATION_TEXT_THRESHOLD = 800
    }
}