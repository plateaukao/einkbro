package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionDisplay
import info.plateaukao.einkbro.preference.GptActionScope
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.compose.PageAiActionDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ShowEditGptActionDialogFragment
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiChatDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val translationViewModel: TranslationViewModel,
    private val webViewProvider: () -> EBWebView,
    private val twoPaneControllerProvider: () -> TwoPaneController,
    private val isTwoPaneControllerInitialized: () -> Boolean,
    private val maybeInitTwoPaneController: () -> Unit,
    private val addAlbum: (title: String, url: String) -> Unit,
    private val showTranslationDialog: (isWholePageMode: Boolean) -> Unit,
) {
    private val linkContentWebView: EBWebView by lazy {
        EBWebView(activity, activity as info.plateaukao.einkbro.browser.WebViewCallback).apply {
            setOnPageFinishedAction {
                activity.lifecycleScope.launch {
                    val content = linkContentWebView.getRawText()
                    loadUrl("about:blank")
                    if (content.isNotEmpty()) {
                        val isSuccess = translationViewModel.setupTextSummary(content)
                        if (!isSuccess) {
                            EBToast.show(activity, R.string.gpt_api_key_not_set)
                            return@launch
                        }
                        showTranslationDialog(false)
                    }
                }
            }
        }
    }

    fun summarizeLinkContent(url: String) {
        if (translationViewModel.hasOpenAiApiKey()) {
            translationViewModel.url = url
            linkContentWebView.loadUrl(url)
        }
    }

    fun summarizeContent() {
        if (translationViewModel.hasOpenAiApiKey()) {
            activity.lifecycleScope.launch {
                val ebWebView = webViewProvider()
                translationViewModel.url = ebWebView.url.orEmpty()
                val isSuccess = translationViewModel.setupTextSummary(ebWebView.getRawText())

                if (!isSuccess) {
                    EBToast.show(activity, R.string.gpt_api_key_not_set)
                    return@launch
                }

                showTranslationDialog(false)
            }
        }
    }

    fun chatWithWeb(useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) {
        activity.lifecycleScope.launch {
            val ebWebView = webViewProvider()
            val rawText = content ?: ebWebView.getRawText()
            withContext(Dispatchers.Main) {
                val scope = activity.lifecycleScope
                if (useSplitScreen) {
                    maybeInitTwoPaneController()
                    val webTitle = ebWebView.title ?: "No Title"
                    val webUrl = ebWebView.url.orEmpty()
                    twoPaneControllerProvider().showSecondPaneAsAi(rawText, webTitle, webUrl)
                    runWithAction?.let { twoPaneControllerProvider().runGptAction(it) }
                } else {
                    val webTitle = ebWebView.title ?: "No Title"
                    val webUrl = ebWebView.url.orEmpty()
                    addAlbum("Chat With Web", "")
                    val newWebView = webViewProvider()
                    runWithAction?.let { action ->
                        newWebView.setOnPageFinishedAction {
                            newWebView.setOnPageFinishedAction {}
                            newWebView.runGptAction(action)
                        }
                    }
                    newWebView.setupAiPage(scope, rawText, webTitle, webUrl)
                }
            }
        }
    }

    fun showPageAiActionMenu() {
        val pageActions =
            config.gptActionList.filter { it.scope == GptActionScope.WholePage }
        if (pageActions.isEmpty()) {
            EBToast.show(activity, R.string.page_ai_action_empty)
            return
        }

        PageAiActionDialogFragment(
            actions = pageActions,
            onActionClicked = { runPageAiAction(it) },
            onActionLongClicked = { action ->
                val index = config.gptActionList.indexOf(action)
                if (index >= 0) {
                    ShowEditGptActionDialogFragment(index)
                        .showNow(activity.supportFragmentManager, "editGptAction")
                }
            }
        ).showNow(activity.supportFragmentManager, "pageAiAction")
    }

    fun runPageAiAction(action: ChatGPTActionInfo) {
        activity.lifecycleScope.launch {
            if (!translationViewModel.hasOpenAiApiKey()) {
                EBToast.show(activity, R.string.gpt_api_key_not_set)
                return@launch
            }

            val ebWebView = webViewProvider()
            val content = ebWebView.getRawText()
            translationViewModel.setupGptAction(action)
            translationViewModel.url = ebWebView.url.orEmpty()
            translationViewModel.pageTitle = ebWebView.title.orEmpty()

            when (action.display) {
                GptActionDisplay.Popup -> {
                    translationViewModel.updateInputMessage(content)
                    translationViewModel.updateMessageWithContext(content)
                    showTranslationDialog(true)
                }

                GptActionDisplay.NewTab -> chatWithWeb(false, content, action)
                GptActionDisplay.SplitScreen -> chatWithWeb(true, content, action)
            }
        }
    }
}
