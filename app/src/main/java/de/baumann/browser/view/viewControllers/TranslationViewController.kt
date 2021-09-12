package de.baumann.browser.view.viewControllers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View.*
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.TranslationPageIndexBinding
import de.baumann.browser.Ninja.databinding.TranslationPanelBinding
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TranslationMode
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.unit.ViewUnit.dp
import de.baumann.browser.util.TranslationLanguage
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import de.baumann.browser.view.NinjaWebView.OnScrollChangeListener
import de.baumann.browser.view.Orientation
import de.baumann.browser.view.TwoPaneLayout
import de.baumann.browser.view.dialog.TranslationLanguageDialog
import java.lang.Math.abs

class TranslationViewController(
    private val activity: Activity,
    private val translationViewBinding: TranslationPanelBinding,
    private val twoPaneLayout: TwoPaneLayout,
    private val showTranslationAction: () -> Unit,
    private val onTranslationClosed: () -> Unit,
    private val onScrollChangeListener: OnScrollChangeListener
) {
    private val config: ConfigManager by lazy { ConfigManager(activity) }
    private val webView: NinjaWebView by lazy {
        NinjaWebView(activity, null).apply {
            shouldHideTranslateContext = true
            setScrollChangeListener(object: OnScrollChangeListener {
                override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                    if (isScrollSynced) onScrollChangeListener.onScrollChange(scrollY, oldScrollY)
                    if (abs(scrollY - oldScrollY) > 10) {
                        hideControlButtons()
                    }
                }
            })
        }
    }
    private val pageContainer: ViewGroup = translationViewBinding.pageContainer

    private var isWebViewAdded: Boolean = false

    private var pageTextList: List<String> = listOf()

    private var isScrollSynced = false

    init {
        twoPaneLayout.setOrientation(config.translationOrientation)

        translationViewBinding.translationFontPlus.setOnClickListener { increaseFontSize() }
        translationViewBinding.translationFontMinus.setOnClickListener { decreaseFontSize() }

        translationViewBinding.translationClose.setOnClickListener { toggleTranslationWindow(false, onTranslationClosed) }
        translationViewBinding.translationClose.setOnLongClickListener{ hideControlButtons(); true }

        translationViewBinding.translationOrientation.setOnClickListener {
            val orientation = if (twoPaneLayout.getOrientation() == Orientation.Vertical) Orientation.Horizontal else Orientation.Vertical
            setOrientation(orientation)
        }

        translationViewBinding.translationOrientation.setOnLongClickListener{ twoPaneLayout.switchPanels() ; true }

        translationViewBinding.syncScroll.setOnClickListener { toggleSyncScroll(!isScrollSynced) }

        translationViewBinding.expandedButton.setOnClickListener { showControlButtons() }

        translationViewBinding.translationLanguage.text = config.translationLanguage.value
        translationViewBinding.translationLanguage.setOnClickListener {
            TranslationLanguageDialog(activity).show { translationLanguage ->
                translationViewBinding.translationLanguage.text = translationLanguage.value
                changeTranslationLanguage(translationLanguage)
            }
        }
    }

    private fun hideControlButtons() {
        translationViewBinding.pageScroller.visibility = INVISIBLE
        translationViewBinding.controlsContainer.visibility = INVISIBLE
        translationViewBinding.expandedButton.visibility = VISIBLE
    }

    private fun showControlButtons() {
        translationViewBinding.pageScroller.visibility = VISIBLE
        translationViewBinding.controlsContainer.visibility = VISIBLE
        translationViewBinding.expandedButton.visibility = INVISIBLE
    }

    private fun changeTranslationLanguage(translationLanguage: TranslationLanguage) {
        val uri = Uri.parse(webView.url)
        val newUri = uri.removeQueryParam("_x_tr_tl").buildUpon()
                .appendQueryParameter("_x_tr_tl", translationLanguage.value) // source language
                .build()
        webView.loadUrl(newUri.toString())
    }

    fun Uri.removeQueryParam(key: String): Uri {
        val builder = buildUpon().clearQuery()

        queryParameterNames.filter { it != key }
                .onEach { builder.appendQueryParameter(it, getQueryParameter(it)) }

        return builder.build()
    }



    suspend fun showTranslation(webView: NinjaWebView) {

        when(config.translationMode) {
            TranslationMode.PAPAGO_DUAL -> webView.loadUrl(buildPUrlTranslateUrl(webView.url.toString()))
            TranslationMode.PAPAGO_URL,
            TranslationMode.GOOGLE_URL -> launchTranslateWindow(webView.url.toString())
            TranslationMode.PAPAGO,
            TranslationMode.GOOGLE -> {
                if (!webView.isReaderModeOn) {
                    webView.toggleReaderMode { launchTranslateWindow(it.purify()) }
                } else {
                    launchTranslateWindow(webView.getRawText().purify())
                }
            }
            TranslationMode.ONYX -> launchTranslateWindow(webView.getRawText().purify())
        }
    }

    //fun showTranslation(text: String) = launchTranslateWindow(text)

    fun setOrientation(orientation: Orientation) {
        config.translationOrientation = orientation
        twoPaneLayout.setOrientation(orientation)
    }

    private fun toggleSyncScroll(shouldSyncScroll: Boolean = false) {
        isScrollSynced = shouldSyncScroll
        val drawable = if (isScrollSynced) R.drawable.selected_border_bg else R.drawable.backgound_with_border
        translationViewBinding.syncScroll.setBackgroundResource(drawable)
    }

    private fun launchTranslateWindow(text: String) {
        if (text == "null") {
            NinjaToast.showShort(activity, "Translation does not work for this page.")
            return
        }
            // onyx case
        if (config.translationMode == TranslationMode.ONYX) {
            ViewUnit.toggleMultiWindow(activity, true)
            try {
                launchOnyxDictTranslation(text)
            } catch (ignored: ClassNotFoundException) {}
            return
        }

        // webview cases: google, papago
        if (!isWebViewAdded) {
            addWebView()
            isWebViewAdded = true
        }

        translationViewBinding.translationLanguage.visibility =
                if (config.translationMode == TranslationMode.GOOGLE_URL) VISIBLE else GONE

        twoPaneLayout.shouldShowSecondPane = true

        // handle translate url
        if (config.translationMode == TranslationMode.PAPAGO_URL) {
            updatePageViews(1)
            translateUrl(buildPUrlTranslateUrl(text))
            return
        } else if (config.translationMode == TranslationMode.GOOGLE_URL) {
            updatePageViews(1)
            translateUrl(buildGUrlTranslateUrl(text))
            return
        }

        pageTextList = parseTextToSegments(text)
        updatePageViews(pageTextList.size)
        translatePage(0)
    }

    fun showTranslationConfigDialog() {
        val enumValues: List<TranslationMode> = if (Build.MANUFACTURER != "ONYX") {
            TranslationMode.values().toMutableList().apply {  remove(TranslationMode.ONYX) }
        } else {
            TranslationMode.values().toList()
        }

        val translationModeArray = enumValues.map { it.name }.toTypedArray()
        val valueArray = enumValues.map { it.ordinal }
        val selected = valueArray.indexOf(config.translationMode.ordinal)
        AlertDialog.Builder(activity, R.style.TouchAreaDialog).apply{
            setTitle("Translation Mode")
            setSingleChoiceItems(translationModeArray, selected) { dialog, which ->
                dialog.dismiss()
                config.translationMode = enumValues[which]
                if (isTranslationModeOn()) showTranslationAction.invoke()
            }
        }
            .setPositiveButton(R.string.close) { _, _ ->
                toggleTranslationWindow(false) { onTranslationClosed() }
            }
            .create().also {
                it.show()
                it.window?.setLayout(300.dp(activity), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
    }

    private fun String.purify(): String =
        this.replace("\\u003C", "<")
            .replace("\\n", "\n")
            .replace("\\t", "  ")
            .replace("\\\"", "\"")

    private fun updatePageViews(size: Int) {
        if (size == 1) {
            pageContainer.visibility = GONE
            return
        }

        pageContainer.visibility = VISIBLE
        pageContainer.removeAllViews()

        (pageTextList.indices).forEach { index ->
            val textView = TranslationPageIndexBinding.inflate(LayoutInflater.from(activity)).root
            textView.text = (index + 1).toString()
            textView.tag = index
            val params = LinearLayout.LayoutParams(40.dp(activity), 40.dp(activity))
            textView.setOnClickListener { translatePage(index) }
            pageContainer.addView(textView, params)
        }
        pageContainer.requestLayout()
    }

    private fun translateUrl(url: String) {
        isScrollSynced = false
        webView.loadUrl(url)
    }

    private fun translatePage(selectedPageIndex: Int) {
        // disable scroll sync first
        toggleSyncScroll(false)

        val text = pageTextList[selectedPageIndex]
        val url = when (config.translationMode) {
            TranslationMode.GOOGLE -> buildGTranslateUrl(text)
            TranslationMode.PAPAGO -> buildPTranslateUrl(text)
            else -> ""
        }

        webView.loadUrl(url)

        pageContainer.children.iterator().forEach{ pageIndexView ->
            pageIndexView.setBackgroundResource(
                if (selectedPageIndex == pageIndexView.tag) R.drawable.selected_border_bg else R.drawable.backgound_with_border
            )
        }
    }

    private fun parseTextToSegments(text: String): List<String> =
        text.chunked(TRANSLATION_TEXT_THRESHOLD)

    private fun addWebView(): NinjaWebView {
        val params: RelativeLayout.LayoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        translationViewBinding.root.addView(webView, 0, params)

        return webView
    }

    private fun buildPUrlTranslateUrl(url: String): String {
        val uri = Uri.Builder()
            .scheme("https")
            .authority("papago.naver.net")
            .path("website")
            .appendQueryParameter("locale", "en")
            .appendQueryParameter("source", "auto")
            .appendQueryParameter("target", "ja")
            .appendQueryParameter("url", url)
            .build()
        return uri.toString()
    }

    private fun buildPTranslateUrl(text: String): String {
        val shortenedText: String = if (text.length > TRANSLATION_TEXT_THRESHOLD) text.substring(0, TRANSLATION_TEXT_THRESHOLD) else text
        val uri = Uri.Builder()
            .scheme("https")
            .authority("papago.naver.com")
            .appendQueryParameter("st", shortenedText)
            .build()
        return uri.toString()
    }

    private fun buildGUrlTranslateUrl(url: String): String {
        val uri = Uri.parse(url)
        val newUri = uri.buildUpon()
                .scheme("https")
                .authority(uri.authority?.replace(".", "-") + ".translate.goog")
                .appendQueryParameter("_x_tr_sl", "auto")
                .appendQueryParameter("_x_tr_tl", config.translationLanguage.value) // source language
                .appendQueryParameter("_x_tr_pto", "ajax,elem") // target language
                .build()
        return newUri.toString()
    }

   private fun oldBuildGUrlTranslateUrl(url: String): String {
        val uri = Uri.Builder()
            .scheme("https")
            .authority("translate.google.com")
            .appendPath("translate")
            .appendQueryParameter("u", url)
            .appendQueryParameter("sl", "auto") // source language
            .appendQueryParameter("tl", "jp") // target language
            .build()
       return uri.toString()
   }

    private fun buildGTranslateUrl(text: String): String {
        val shortenedText: String = if (text.length > TRANSLATION_TEXT_THRESHOLD) text.substring(0, TRANSLATION_TEXT_THRESHOLD) else text
        val uri = Uri.Builder()
            .scheme("https")
            .authority("translate.google.com")
            .appendQueryParameter("text", shortenedText)
            .appendQueryParameter("sl", "auto") // source language
            .appendQueryParameter("tl", "jp") // target language
            .build()
        return uri.toString()
    }

    private fun launchOnyxDictTranslation(text: String) {
        val intent = Intent().apply {
            action = "com.onyx.intent.ACTION_DICT_TRANSLATION"
            putExtra("translation", "{\"type\": \"page\", \"content\": \"$text\"}")
        }
        activity.startActivity(intent)
    }

    private fun isTranslationModeOn(): Boolean =
        (config.translationMode == TranslationMode.ONYX && ViewUnit.isMultiWindowEnabled(activity)) ||
                twoPaneLayout.shouldShowSecondPane

    private fun toggleTranslationWindow(
        isEnabled: Boolean,
        onTranslationClosed: () -> Unit
    ) {
        if (config.translationMode == TranslationMode.ONYX) {
            ViewUnit.toggleMultiWindow(activity, isEnabled)
        } else {
            // all other translation types, should remove sub webviews
            if (!isEnabled) {
                webView.loadUrl("about:blank")
                twoPaneLayout.shouldShowSecondPane = false
            }
        }

        if (!isEnabled) {
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