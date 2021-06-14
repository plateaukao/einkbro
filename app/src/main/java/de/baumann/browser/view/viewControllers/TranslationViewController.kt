package de.baumann.browser.view.viewControllers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.LinkAddress
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.size
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.TranslationPageIndexBinding
import de.baumann.browser.Ninja.databinding.TranslationPanelBinding
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TranslationMode
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import kotlinx.coroutines.delay

class TranslationViewController(
    private val activity: Activity,
    private val translationViewBinding: TranslationPanelBinding,
    private val showTranslationAction: () -> Unit
) {
    private val config: ConfigManager by lazy { ConfigManager(activity) }
    private val webView: NinjaWebView by lazy {
        NinjaWebView(activity, null).apply {
            shouldHideTranslateContext = true
        }
    }
    private val pageContainer: ViewGroup = translationViewBinding.pageContainer

    private var isWebViewAdded: Boolean = false

    private var pageTextList: List<String> = listOf()

    init {
        translationViewBinding.translationClose.setOnClickListener {
            toggleTranslationWindow(false)
        }
    }
    suspend fun showTranslation(context: Context, webView: NinjaWebView) {
        //activity.lifecycleScope.launch(Dispatchers.Main) {
            if (!webView.isReaderModeOn) {
                webView.toggleReaderMode()
                delay(700)
            }

            val text = webView.getRawText()
                .replace("\\u003C", "<")
                .replace("\\n", "\n")
                .replace("\\t", "  ")
            if (text == "null") {
                NinjaToast.showShort(context, "null string")
            } else {
                try {
                    launchTranslateWindow(text)
                } catch (ignored: ClassNotFoundException) {
                    //Log.e(BrowserActivity.TAG, "translation activity not found.")
                }
            }
        //}
    }

    fun launchTranslateWindow(text: String) {
        // onyx case
        if (config.translationMode == TranslationMode.ONYX) {
            ViewUnit.toggleMultiWindow(activity, true)
            launchOnyxDictTranslation(text)
            return
        }

        // webview cases: google, papago
        if (!isWebViewAdded) {
            addWebView()
            isWebViewAdded = true
        }

        translationViewBinding.root.visibility = VISIBLE

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
        val buttonText = if (!isTranslationModeOn()) "Enable" else "Disable"
        AlertDialog.Builder(activity, R.style.TouchAreaDialog).apply{
            setTitle("Translation Mode")
            setSingleChoiceItems(translationModeArray, selected) { dialog, which ->
                config.translationMode = enumValues[which]
                if (isTranslationModeOn()) showTranslationAction.invoke()
                dialog.dismiss()
            }
        }
            .setPositiveButton(buttonText) { d, _ -> d.dismiss() ; toggleTranslationWindow(!isTranslationModeOn()) }
            .setNegativeButton(android.R.string.cancel)  { d, _ -> d.dismiss() }
            .create().also {
                it.show()
                it.window?.setLayout(ViewUnit.dpToPixel(activity, 200).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
    }

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
            val params = LinearLayout.LayoutParams(
                ViewUnit.dpToPixel(activity, 40).toInt(),
                ViewUnit.dpToPixel(activity, 40).toInt()
            )
            textView.setOnClickListener { translatePage(index) }
            pageContainer.addView(textView, params)
        }
        pageContainer.requestLayout()
    }

    private fun translatePage(selectedPageIndex: Int) {
        val text = pageTextList[selectedPageIndex]
        webView.loadUrl(
            if (config.translationMode == TranslationMode.GOOGLE) buildGTranslateUrl(text)
            else buildPTranslateUrl(text)
        )

        pageContainer.children.iterator().forEach{ pageIndexView ->
            pageIndexView.setBackgroundResource(
                if (selectedPageIndex == pageIndexView.tag) R.drawable.selected_border_bg else R.drawable.dialog_border_bg
            )
        }
    }

    private fun parseTextToSegments(text: String): List<String> {
        return text.chunked(TRANSLATION_TEXT_THRESHOLD).take(6)
    }

    private fun addWebView(): NinjaWebView {
        val separator = translationViewBinding.separator
        val params: RelativeLayout.LayoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT).apply {
                //addRule(RelativeLayout.ABOVE, pageScroller.id)
                addRule(RelativeLayout.RIGHT_OF, separator.id)
            }
        translationViewBinding.root.addView(webView, params)
        translationViewBinding.pageScroller.bringToFront()
        translationViewBinding.translationClose.bringToFront()

        return webView
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

    fun isTranslationModeOn(): Boolean =
        (config.translationMode == TranslationMode.ONYX && ViewUnit.isMultiWindowEnabled(activity)) ||
                translationViewBinding.root.visibility == View.VISIBLE

    fun toggleTranslationWindow(isEnabled: Boolean) {
        if (config.translationMode == TranslationMode.ONYX) {
            ViewUnit.toggleMultiWindow(activity, isEnabled)
        } else {
            // all other translation types, should remove sub webviews
            if (!isEnabled) {
                webView.loadUrl("about:blank")
                translationViewBinding.root.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val TRANSLATION_TEXT_THRESHOLD = 300
    }
}