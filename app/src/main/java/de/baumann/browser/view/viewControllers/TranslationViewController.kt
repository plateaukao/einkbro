package de.baumann.browser.view.viewControllers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TranslationMode
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import kotlinx.coroutines.delay

class TranslationViewController(
    private val activity: Activity,
    private val translationViewContainer: ViewGroup,
    private val showTranslationAction: () -> Unit
) {
    private val config: ConfigManager by lazy { ConfigManager(activity) }

    suspend fun showTranslation(context: Context, webView: NinjaWebView) {
        //activity.lifecycleScope.launch(Dispatchers.Main) {
            if (!webView.isReaderModeOn) {
                webView.toggleReaderMode()
                delay(500)
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
        val translateWebView = if (!isTranslationModeOn()) {
            val ninjaWebView = NinjaWebView(activity, null)
            translationViewContainer.addView(ninjaWebView)
            translationViewContainer.visibility = View.VISIBLE
            ninjaWebView.shouldHideTranslateContext = true
            ninjaWebView
        } else {
            translationViewContainer[0] as NinjaWebView
        }

        translateWebView.loadUrl(
            if (config.translationMode == TranslationMode.GOOGLE) buildGTranslateUrl(text)
            else buildPTranslateUrl(text)
        )
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


    private fun buildPTranslateUrl(text: String): String {
        val translationTextLength = 1000
        val shortenedText: String = if (text.length > translationTextLength) text.substring(0, translationTextLength) else text
        val uri = Uri.Builder()
            .scheme("https")
            .authority("papago.naver.com")
            .appendQueryParameter("st", shortenedText)
            .build()
        return uri.toString()
    }

    private fun buildGTranslateUrl(text: String): String {
        val translationTextLength = 1800
        val shortenedText: String = if (text.length > translationTextLength) text.substring(0, translationTextLength) else text
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
                translationViewContainer.visibility == View.VISIBLE

    fun toggleTranslationWindow(isEnabled: Boolean) {
        if (config.translationMode == TranslationMode.ONYX) {
            ViewUnit.toggleMultiWindow(activity, isEnabled)
        } else {
            // all other translation types, should remove sub webviews
            if (!isEnabled) {
                translationViewContainer.removeAllViews()
                translationViewContainer.visibility = View.GONE
            }
        }
    }
}