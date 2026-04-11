package info.plateaukao.einkbro.activity.delegates

import android.graphics.Point
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.WebView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.TranslationPanelView
import info.plateaukao.einkbro.view.TwoPaneLayout
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.view.dialog.compose.LanguageSettingDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ShowEditGptActionDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TranslateDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TranslationConfigDlgFragment
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranslationDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val translationViewModel: TranslationViewModel,
    private val actionModeMenuViewModel: ActionModeMenuViewModel,
    private val focusedWebViewProvider: () -> EBWebView,
    private val externalSearchWebViewProvider: () -> WebView,
    private val twoPaneControllerProvider: () -> TwoPaneController,
    private val isTwoPaneControllerInitialized: () -> Boolean,
    private val maybeInitTwoPaneController: () -> Unit,
    private val addAlbum: () -> Unit,
) {
    var showSiteSettingsAction: (() -> Unit)? = null
    private var languageLabelView: TextView? = null

    fun initTranslationViewModel() {
        activity.lifecycleScope.launch {
            translationViewModel.showEditDialogWithIndex.collect { index ->
                if (index == -1) return@collect
                ShowEditGptActionDialogFragment(index)
                    .showNow(activity.supportFragmentManager, "editGptAction")
                translationViewModel.resetEditDialogIndex()
            }
        }
    }

    fun initLanguageLabel() {
        languageLabelView = activity.findViewById(R.id.translation_language)
        activity.lifecycleScope.launch {
            translationViewModel.translationLanguage.collect {
                ViewUnit.updateLanguageLabel(languageLabelView!!, it)
            }
        }

        languageLabelView?.setOnClickListener {
            activity.lifecycleScope.launch {
                val translationLanguage =
                    TranslationLanguageDialog(activity).show() ?: return@launch
                translationViewModel.updateTranslationLanguage(translationLanguage)
                state.ebWebView.clearTranslationElements()
                translateByParagraph(state.ebWebView.translateApi)
            }
        }
        languageLabelView?.setOnLongClickListener {
            languageLabelView?.visibility = GONE
            true
        }
    }

    suspend fun updateTranslationInput() {
        with(translationViewModel) {
            updateInputMessage(actionModeMenuViewModel.selectedText.value)
            updateMessageWithContext(focusedWebViewProvider().getSelectedTextWithContext())
            url = focusedWebViewProvider().url.orEmpty()
        }
    }

    fun translate(translationMode: TranslationMode) {
        when (translationMode) {
            TranslationMode.TRANSLATE_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.GOOGLE)
            TranslationMode.DEEPL_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.DEEPL)
            TranslationMode.OPENAI_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.OPENAI)
            TranslationMode.GEMINI_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.GEMINI)
            TranslationMode.PAPAGO_TRANSLATE_BY_SCREEN -> translateWebView()
            TranslationMode.GOOGLE_IN_PLACE -> state.ebWebView.addGoogleTranslation()
            TranslationMode.OPENAI_IN_PLACE -> translateInPlaceReplace(TRANSLATE_API.OPENAI)
            TranslationMode.GEMINI_IN_PLACE -> translateInPlaceReplace(TRANSLATE_API.GEMINI)
            TranslationMode.GOOGLE_URL -> Unit
        }
    }

    fun resetTranslateUI() {
        languageLabelView?.visibility = GONE
    }

    fun configureTranslationLanguage(translateApi: TRANSLATE_API) {
        LanguageSettingDialogFragment(translateApi, translationViewModel) {
            if (translateApi == TRANSLATE_API.GOOGLE) {
                translateByParagraph(TRANSLATE_API.GOOGLE)
            } else if (translateApi == TRANSLATE_API.PAPAGO) {
                translateByParagraph(TRANSLATE_API.PAPAGO)
            } else if (translateApi == TRANSLATE_API.DEEPL) {
                translateByParagraph(TRANSLATE_API.DEEPL)
            }
        }
            .show(activity.supportFragmentManager, "LanguageSettingDialog")
    }

    fun showTranslationDialog(isWholePageMode: Boolean = false) {
        activity.supportFragmentManager.findFragmentByTag("translateDialog")?.let {
            activity.supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
        TranslateDialogFragment(
            translationViewModel,
            externalSearchWebViewProvider(),
            actionModeMenuViewModel.clickedPoint.value,
            isWholePageMode = isWholePageMode,
        )
            .show(activity.supportFragmentManager, "translateDialog")
    }

    fun showTranslation(webView: EBWebView? = null) {
        maybeInitTwoPaneController()

        activity.lifecycleScope.launch(Dispatchers.Main) {
            twoPaneControllerProvider().showTranslation(webView ?: state.ebWebView)
        }
    }

    fun showTranslationConfigDialog(translateDirectly: Boolean) {
        maybeInitTwoPaneController()
        val url = state.ebWebView.url.orEmpty()
        TranslationConfigDlgFragment(
            url,
            translateDirectly,
            onToggledAction = { shouldTranslate ->
                if (shouldTranslate) {
                    translate(config.getTranslationMode(url))
                } else {
                    state.ebWebView.reload()
                }
            },
            onShowSiteSettings = {
                showSiteSettingsAction?.invoke()
            },
        ).show(activity.supportFragmentManager, "TranslationConfigDialog")
    }

    fun translateByParagraph(
        translateApi: TRANSLATE_API,
        webView: EBWebView = state.ebWebView,
    ) {
        translateByParagraphInPlace(translateApi, webView)
    }

    private fun translateByParagraphInPlace(
        translateApi: TRANSLATE_API,
        webView: EBWebView = state.ebWebView,
    ) {
        activity.lifecycleScope.launch {
            webView.translateApi = translateApi
            webView.translateByParagraphInPlace()
            if (webView == state.ebWebView) {
                languageLabelView?.visibility = VISIBLE
            }
        }
    }

    fun translateInPlaceReplace(
        translateApi: TRANSLATE_API,
        webView: EBWebView = state.ebWebView,
    ) {
        activity.lifecycleScope.launch {
            webView.translateApi = translateApi
            webView.translateByParagraphInPlaceReplace()
            if (webView == state.ebWebView) {
                languageLabelView?.visibility = VISIBLE
            }
        }
    }

    fun translateWebView() {
        activity.lifecycleScope.launch {
            val base64String = translationViewModel.translateWebView(
                state.ebWebView,
                config.translation.sourceLanguage,
                config.translation.translationLanguage,
            )
            if (base64String != null) {
                val translatedImageHtml = HelperUnit.loadAssetFileToString(
                    activity, "translated_image.html"
                ).replace("%%", base64String)
                if (config.translation.showTranslatedImageToSecondPanel) {
                    maybeInitTwoPaneController()
                    twoPaneControllerProvider().showSecondPaneWithData(translatedImageHtml)
                } else {
                    addAlbum()
                    state.ebWebView.isTranslatePage = true
                    state.ebWebView.loadData(translatedImageHtml, "text/html", "utf-8")
                }
            } else {
                EBToast.show(activity, "Failed to translate image")
            }
        }
    }

    fun translateImage(url: String) {
        activity.lifecycleScope.launch {
            translateImageSuspend(url)
        }
    }

    suspend fun translateImageSuspend(url: String): Boolean {
        val result = translationViewModel.translateImage(
            state.ebWebView.url.orEmpty(),
            url,
            TranslationLanguage.KO,
            config.translation.translationLanguage,
        )
        if (result != null) {
            val escapedUrl = url.replace("\\", "\\\\").replace("'", "\\'")
            val js = HelperUnit.loadAssetFileToString(
                activity, "translate_image_overlay.js"
            ).replace("%%IMAGE_URL%%", escapedUrl)
                .replace("%%BASE64_DATA%%", result.renderedImage)
            state.ebWebView.evaluateJavascript(js, null)
            return result.imageId == "cached"
        }
        return false
    }

    fun translateAllImages(imageUrl: String) {
        activity.lifecycleScope.launch {
            val escapedUrl = imageUrl.replace("\\", "\\\\").replace("'", "\\'")
            val js = HelperUnit.loadAssetFileToString(
                activity, "get_remaining_images.js"
            ).replace("%%IMAGE_URL%%", escapedUrl)

            state.ebWebView.evaluateJavascript(js) { result ->
                val urlsJson = result?.trim('"')?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\") ?: return@evaluateJavascript
                try {
                    val urls = org.json.JSONArray(urlsJson)
                    val imageUrls = mutableListOf<String>()
                    for (i in 0 until urls.length()) {
                        imageUrls.add(urls.getString(i))
                    }
                    if (imageUrls.isEmpty()) return@evaluateJavascript

                    EBToast.show(
                        activity,
                        "Translating ${imageUrls.size} images..."
                    )
                    activity.lifecycleScope.launch {
                        for ((index, url) in imageUrls.withIndex()) {
                            val wasCached = translateImageSuspend(url)
                            if (index < imageUrls.size - 1 && !wasCached) {
                                val base = config.ai.imageTranslateIntervalSeconds
                                val delayMs =
                                    ((base - 2).coerceAtLeast(1)..(base + 2)).random() * 1000L
                                kotlinx.coroutines.delay(delayMs)
                            }
                        }
                    }
                } catch (e: Exception) {
                    EBToast.show(activity, "Failed to get image list")
                }
            }
        }
    }

    fun updateLanguageLabel() {
        val ebWebView = state.ebWebView
        languageLabelView?.visibility =
            if (ebWebView.isTranslatePage || ebWebView.isTranslateByParagraph) VISIBLE else GONE
    }
}
