package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionDisplay
import info.plateaukao.einkbro.preference.GptActionScope
import info.plateaukao.einkbro.task.InitialPageSnapshot
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.compose.PageAiActionDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ShowEditGptActionDialogFragment
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiChatDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val translationViewModel: TranslationViewModel,
    private val ttsViewModel: TtsViewModel,
    private val twoPaneControllerProvider: () -> TwoPaneController,
    private val isTwoPaneControllerInitialized: () -> Boolean,
    private val maybeInitTwoPaneController: () -> Unit,
    private val addAlbum: (title: String, url: String) -> Unit,
    private val showTranslationDialog: (isWholePageMode: Boolean) -> Unit,
    private val showTaskMenu: () -> Unit,
) {
    private val linkContentWebView: EBWebView by lazy {
        EBWebView(activity, activity as info.plateaukao.einkbro.browser.WebViewCallback).apply {
            setOnPageFinishedAction {
                activity.lifecycleScope.launch {
                    if (!translationViewModel.hasOpenAiApiKey()) {
                        loadUrl("about:blank")
                        EBToast.show(activity, R.string.gpt_api_key_not_set)
                        return@launch
                    }
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
        if (!translationViewModel.hasOpenAiApiKey()) {
            EBToast.show(activity, R.string.gpt_api_key_not_set)
            return
        }
        translationViewModel.url = url
        linkContentWebView.loadUrl(url)
    }

    fun summarizeContent() {
        if (!translationViewModel.hasOpenAiApiKey()) {
            EBToast.show(activity, R.string.gpt_api_key_not_set)
            return
        }
        activity.lifecycleScope.launch {
            val ebWebView = state.ebWebView
            translationViewModel.url = ebWebView.url.orEmpty()
            val isSuccess = translationViewModel.setupTextSummary(ebWebView.getRawText())

            if (!isSuccess) {
                EBToast.show(activity, R.string.gpt_api_key_not_set)
                return@launch
            }

            showTranslationDialog(false)
        }
    }

    fun chatWithWeb(useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) {
        activity.lifecycleScope.launch {
            val ebWebView = state.ebWebView
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
                    val newWebView = state.ebWebView
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

    /**
     * Open a new chat tab that runs the free-form LLM agent with tool access. The chat
     * tab's [ChatWebInterface] is configured in agent mode and the user's [prompt] is
     * fired as the first message once `chat.html` finishes loading.
     *
     * The [snapshot] captures the page the user was viewing at the moment they triggered
     * the task — it's exposed to the agent as the "originating page" so the user can say
     * things like "summarize this" or "open the top 3 stories" without extra fetches.
     */
    fun chatWithWebAgent(prompt: String, snapshot: InitialPageSnapshot) {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val scope = activity.lifecycleScope
                addAlbum("Agent Chat", "")
                val newWebView = state.ebWebView
                newWebView.setOnPageFinishedAction {
                    newWebView.setOnPageFinishedAction {}
                    newWebView.runAgentPrompt(prompt)
                }
                newWebView.setupAiPage(
                    lifecycleScope = scope,
                    webContent = snapshot.text,
                    webTitle = snapshot.title.ifBlank { "Agent Chat" },
                    webUrl = snapshot.url,
                    agentMode = true,
                    initialSnapshot = snapshot,
                    agentContext = activity,
                    agentBrowserState = state,
                    agentTtsViewModel = ttsViewModel,
                )
            }
        }
    }

    fun showPageAiActionMenu() {
        val pageActions =
            config.ai.gptActionList.filter { it.scope == GptActionScope.WholePage }

        PageAiActionDialogFragment(
            actions = pageActions,
            onActionClicked = { runPageAiAction(it) },
            onActionLongClicked = { action ->
                val index = config.ai.gptActionList.indexOf(action)
                if (index >= 0) {
                    ShowEditGptActionDialogFragment(index)
                        .showNow(activity.supportFragmentManager, "editGptAction")
                }
            },
            onChatWithWebClicked = { chatWithWeb(useSplitScreen = false, content = null, runWithAction = null) },
            onChatWithWebLongClicked = { chatWithWeb(useSplitScreen = true, content = null, runWithAction = null) },
            onTaskRunnerClicked = { showTaskMenu() },
        ).showNow(activity.supportFragmentManager, "pageAiAction")
    }

    fun runPageAiAction(action: ChatGPTActionInfo) {
        activity.lifecycleScope.launch {
            if (!translationViewModel.hasOpenAiApiKey()) {
                EBToast.show(activity, R.string.gpt_api_key_not_set)
                return@launch
            }

            val ebWebView = state.ebWebView
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
