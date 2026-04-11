package info.plateaukao.einkbro.activity.delegates

import android.content.Intent
import android.graphics.Point
import android.view.ActionMode
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionDisplay
import info.plateaukao.einkbro.preference.GptActionScope
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.ActionModeMenu
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.*
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.SplitSearchViewModel
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Highlight
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ActionModeDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val browserState: BrowserState,
    private val actionModeMenuViewModel: ActionModeMenuViewModel,
    private val translationViewModel: TranslationViewModel,
    private val ttsViewModel: TtsViewModel,
    private val splitSearchViewModel: SplitSearchViewModel,
    private val remoteConnViewModel: info.plateaukao.einkbro.viewmodel.RemoteConnViewModel,
    private val externalSearchViewModel: info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel,
    private val bookmarkManager: BookmarkManager,
    private val getFocusedWebView: () -> EBWebView,
    private val showTranslationDialog: (isWholePageMode: Boolean) -> Unit,
    private val updateTranslationInput: suspend () -> Unit,
    private val toggleSplitScreen: (url: String?) -> Unit,
    private val chatWithWeb: (useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) -> Unit,
) {
    private var actionModeView: View? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    fun initActionModeViewModel() {
        activity.lifecycleScope.launch {
            actionModeMenuViewModel.actionModeMenuState.collect { state ->
                when (state) {
                    is HighlightText -> {
                        activity.lifecycleScope.launch {
                            highlightText(state.highlightStyle)
                            actionModeMenuViewModel.finish()
                        }
                    }

                    GoogleTranslate, DeeplTranslate, Papago, Naver -> {
                        val api =
                            if (GoogleTranslate == state) TRANSLATE_API.GOOGLE
                            else if (Papago == state) TRANSLATE_API.PAPAGO
                            else if (DeeplTranslate == state) TRANSLATE_API.DEEPL
                            else TRANSLATE_API.NAVER
                        translationViewModel.updateTranslateMethod(api)

                        activity.lifecycleScope.launch {
                            updateTranslationInput()
                            showTranslationDialog(false)
                            actionModeMenuViewModel.finish()
                        }
                    }

                    is ActionModeMenuState.ReadFromHere -> readFromThisSentence()

                    is Gpt -> {
                        val gptAction = config.ai.gptActionList[state.gptActionIndex]
                        activity.lifecycleScope.launch {
                            updateTranslationInput()
                            if (translationViewModel.hasOpenAiApiKey()) {
                                translationViewModel.setupGptAction(gptAction)
                                translationViewModel.url = getFocusedWebView().url.orEmpty()

                                when (gptAction.display) {
                                    GptActionDisplay.Popup -> showTranslationDialog(false)
                                    GptActionDisplay.NewTab -> {
                                        chatWithWeb(false, actionModeMenuViewModel.selectedText.value, gptAction)
                                    }
                                    GptActionDisplay.SplitScreen -> {
                                        chatWithWeb(true, actionModeMenuViewModel.selectedText.value, gptAction)
                                    }
                                }
                            } else {
                                EBToast.show(activity, R.string.gpt_api_key_not_set)
                            }
                            actionModeMenuViewModel.finish()
                        }
                    }

                    is SplitSearch -> {
                        splitSearchViewModel.state = state
                        val selectedText = actionModeMenuViewModel.selectedText.value
                        toggleSplitScreen(splitSearchViewModel.getUrl(selectedText))
                        actionModeMenuViewModel.finish()
                    }

                    is Tts -> {
                        IntentUnit.tts(activity, state.text)
                        actionModeMenuViewModel.finish()
                    }

                    is SelectSentence -> getFocusedWebView().selectSentence(browserState.longPressPoint)
                    is SelectParagraph -> getFocusedWebView().selectParagraph(browserState.longPressPoint)
                    Idle -> Unit
                }
            }
        }

        activity.lifecycleScope.launch {
            actionModeMenuViewModel.clickedPoint.collect { point ->
                val view = actionModeView ?: return@collect
                ViewUnit.updateViewPosition(view, point)
            }
        }

        activity.lifecycleScope.launch {
            actionModeMenuViewModel.shouldShow.collect { shouldShow ->
                val view = actionModeView ?: return@collect
                if (shouldShow) {
                    val point = actionModeMenuViewModel.clickedPoint.value
                    if (view.width == 0 || view.height == 0) {
                        view.post {
                            ViewUnit.updateViewPosition(view, point)
                            view.visibility = VISIBLE
                        }
                    } else {
                        ViewUnit.updateViewPosition(view, point)
                        view.visibility = VISIBLE
                    }
                } else {
                    view.visibility = INVISIBLE
                }
            }
        }
    }

    private fun readFromThisSentence() {
        activity.lifecycleScope.launch {
            val ebWebView = browserState.ebWebView
            val selectedSentence = ebWebView.getSelectedText()
            val fullText = ebWebView.getRawText()
            val startIndex = fullText.indexOf(selectedSentence)
            ttsViewModel.readArticle(fullText.substring(startIndex), ebWebView.title.orEmpty())
        }
    }

    private suspend fun highlightText(highlightStyle: HighlightStyle) {
        val focusedWebView = getFocusedWebView()
        focusedWebView.highlightTextSelection(highlightStyle)

        val url = focusedWebView.url.orEmpty()
        val title = focusedWebView.title.orEmpty()
        val article = Article(title, url, System.currentTimeMillis(), "")

        val articleInDb =
            bookmarkManager.getArticleByUrl(url) ?: bookmarkManager.insertArticle(article)

        val selectedText = actionModeMenuViewModel.selectedText.value
        val highlight = Highlight(articleInDb.id, selectedText)
        bookmarkManager.insertHighlight(highlight)
    }

    fun onActionModeStarted(mode: ActionMode) {
        val isTextEditMode = ViewUnit.isTextEditMode(activity, mode.menu)

        if (remoteConnViewModel.isSendingTextSearch && !isTextEditMode) {
            mode.hide(1000000)
            mode.menu.clear()
            mode.finish()

            activity.lifecycleScope.launch {
                val keyword = getFocusedWebView().getSelectedText()
                val keywordWithContext = getFocusedWebView().getSelectedTextWithContext()
                val action = config.ai.gptActionList.firstOrNull { it.name == config.ai.remoteQueryActionName }
                val constructedUrlString =
                    if (action != null) {
                        val actionString = json.encodeToString(serializer = ChatGPTActionInfo.serializer(), action)
                        "action|||$keywordWithContext|||$actionString"
                    } else {
                        externalSearchViewModel.generateSearchUrl(keyword)
                    }
                remoteConnViewModel.sendTextSearch(constructedUrlString)
            }
            return
        }

        if (!config.showDefaultActionMenu && !isTextEditMode && isInSplitSearchMode()) {
            mode.hide(1000000)
            mode.menu.clear()

            activity.lifecycleScope.launch {
                toggleSplitScreen(splitSearchViewModel.getUrl(browserState.ebWebView.getSelectedText()))
            }

            mode.finish()
            return
        }

        if (!actionModeMenuViewModel.isInActionMode()) {
            actionModeMenuViewModel.updateActionMode(mode)

            if (!config.showDefaultActionMenu && !isTextEditMode) {
                mode.hide(1000000)
                mode.menu.clear()
                mode.finish()

                activity.lifecycleScope.launch {
                    actionModeMenuViewModel.updateSelectedText(HelperUnit.unescapeJava(getFocusedWebView().getSelectedText()))
                    showActionModeView(translationViewModel) {
                        getFocusedWebView().removeTextSelection()
                    }
                }
            }
        }

        if (!config.showDefaultActionMenu && !isTextEditMode) {
            mode.menu.clear()
        }
    }

    private fun isInSplitSearchMode(): Boolean =
        splitSearchViewModel.state != null && isTwoPaneSecondPaneDisplayed()

    private var isTwoPaneSecondPaneDisplayed: () -> Boolean = { false }

    fun setTwoPaneChecker(checker: () -> Boolean) {
        isTwoPaneSecondPaneDisplayed = checker
    }

    private fun showActionModeView(
        translationViewModel: TranslationViewModel,
        clearSelectionAction: () -> Unit,
    ) {
        actionModeMenuViewModel.updateMenuInfos(activity, translationViewModel)
        if (actionModeView == null) {
            actionModeView = ComposeView(activity).apply {
                id = View.generateViewId()
                setContent {
                    val text by actionModeMenuViewModel.selectedText.collectAsState()
                    MyTheme {
                        ActionModeMenu(
                            actionModeMenuViewModel.menuInfos,
                            actionModeMenuViewModel.showIcons,
                        ) { intent ->
                            if (intent != null) {
                                context.startActivity(intent.apply {
                                    putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                                    putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                                })
                            }
                            clearSelectionAction()
                            actionModeMenuViewModel.updateActionMode(null)
                        }
                    }
                }
            }
            actionModeView?.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            actionModeView?.visibility = INVISIBLE
            browserState.binding.root.addView(actionModeView)
        }

        actionModeMenuViewModel.show()
    }

    fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float) {
        val newPoint = Point(
            ViewUnit.dpToPixel(right.toInt()).toInt(),
            ViewUnit.dpToPixel(bottom.toInt() + 16).toInt()
        )
        if (kotlin.math.abs(newPoint.x - actionModeMenuViewModel.clickedPoint.value.x) > ViewUnit.dpToPixel(15) ||
            kotlin.math.abs(newPoint.y - actionModeMenuViewModel.clickedPoint.value.y) > ViewUnit.dpToPixel(15)
        ) {
            actionModeView?.visibility = INVISIBLE
        }
        actionModeMenuViewModel.updateClickedPoint(newPoint)

        browserState.longPressPoint = Point(
            ViewUnit.dpToPixel(left.toInt() - 1).toInt(),
            ViewUnit.dpToPixel(top.toInt() + 1).toInt()
        )
    }

    fun onActionModeFinished(mode: ActionMode?) {
        mode?.hide(1000000)
        actionModeMenuViewModel.updateActionMode(null)
    }

    fun isActionModeActive(): Boolean = actionModeMenuViewModel.isInActionMode()

    fun dismissActionMode() {
        browserState.ebWebView.removeTextSelection()
        actionModeMenuViewModel.updateActionMode(null)
    }

    fun onPause() {
        actionModeMenuViewModel.finish()
    }

    val actionModeViewRef: View?
        get() = actionModeView
}
