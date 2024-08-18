package info.plateaukao.einkbro.activity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.Request
import android.app.PictureInPictureParams
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.databinding.ActivityMainBinding
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.AlbumInfo
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.NewTabBehavior
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.service.ClearService
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.BrowserUnit.createDownloadReceiver
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.HelperUnit.toNormalScheme
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.Constants.Companion.ACTION_DICT
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.MultitouchListener
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.SwipeTouchListener
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.SendLinkDialog
import info.plateaukao.einkbro.view.dialog.ShortcutEditDialog
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
import info.plateaukao.einkbro.view.dialog.compose.BookmarksDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType
import info.plateaukao.einkbro.view.dialog.compose.FastToggleDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.LanguageSettingDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.MenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ReaderFontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ShowEditGptActionDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TranslateDialogFragment
import info.plateaukao.einkbro.view.handlers.GestureHandler
import info.plateaukao.einkbro.view.handlers.MenuActionHandler
import info.plateaukao.einkbro.view.handlers.ToolbarActionHandler
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.view.viewControllers.TouchAreaViewController
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.DeeplTranslate
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.GoogleTranslate
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Gpt
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.HighlightText
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Idle
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Naver
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Papago
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.SplitSearch
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Tts
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.AlbumViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModelFactory
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import info.plateaukao.einkbro.viewmodel.PocketShareState
import info.plateaukao.einkbro.viewmodel.PocketViewModel
import info.plateaukao.einkbro.viewmodel.PocketViewModelFactory
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import info.plateaukao.einkbro.viewmodel.SplitSearchViewModel
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


open class BrowserActivity : FragmentActivity(), BrowserController {
    private lateinit var progressBar: ProgressBar
    protected lateinit var ninjaWebView: NinjaWebView
    protected open var shouldRunClearService: Boolean = true

    private var videoView: VideoView? = null
    private var customView: View? = null
    private var languageLabelView: TextView? = null

    // Layouts
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var subContainer: RelativeLayout

    private var fullscreenHolder: FrameLayout? = null

    // Others
    private var downloadReceiver: BroadcastReceiver? = null
    private val config: ConfigManager by inject()
    private val ttsManager: TtsManager by inject()
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    private val ttsViewModel: TtsViewModel by viewModels()
    private val translationViewModel: TranslationViewModel by viewModels()

    private val splitSearchViewModel: SplitSearchViewModel by viewModels()

    private val remoteConnViewModel: RemoteConnViewModel by viewModels()

    private val externalSearchViewModel: ExternalSearchViewModel by viewModels()

    private fun prepareRecord(): Boolean {
        val webView = currentAlbumController as NinjaWebView
        val title = webView.title
        val url = webView.url
        return (title.isNullOrEmpty() || url.isNullOrEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT))
    }

    private var originalOrientation = 0
    private var searchOnSite = false
    private var customViewCallback: CustomViewCallback? = null
    private var currentAlbumController: AlbumController? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var binding: ActivityMainBinding

    private val bookmarkManager: BookmarkManager by inject()

    private val epubManager: EpubManager by lazy { EpubManager(this) }

    private var uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED

    private var shouldLoadTabState: Boolean = false

    private val toolbarActionHandler: ToolbarActionHandler by lazy {
        ToolbarActionHandler(this)
    }

    private val albumViewModel: AlbumViewModel by viewModels()

    private val externalSearchWebView: WebView by lazy {
        BrowserUnit.createNaverDictWebView(this)
    }

    protected val composeToolbarViewController: ComposeToolbarViewController by lazy {
        ComposeToolbarViewController(
            binding.composeIconBar,
            albumViewModel.albums,
            { toolbarActionHandler.handleClick(it) },
            { toolbarActionHandler.handleLongClick(it) },
            onTabClick = { it.showOrJumpToTop() },
            onTabLongClick = { it.remove() },
        )
    }

    override fun newATab() = when (config.newTabBehavior) {
        NewTabBehavior.START_INPUT -> {
            addAlbum(getString(R.string.app_name), "")
            focusOnInput()
        }

        NewTabBehavior.SHOW_HOME -> addAlbum("", config.favoriteUrl)
        NewTabBehavior.SHOW_RECENT_BOOKMARKS -> {
            addAlbum("", "")
            showRecentlyUsedBookmarks(ninjaWebView)
        }
    }

    override fun duplicateTab() {
        val webView = currentAlbumController as NinjaWebView
        val title = webView.title.orEmpty()
        val url = webView.url ?: return
        addAlbum(title, url)
    }

    override fun refreshAction() {
        if (ninjaWebView.isLoadFinish && ninjaWebView.url?.isNotEmpty() == true) {
            ninjaWebView.reload()
        } else {
            ninjaWebView.stopLoading()
        }
    }

    private lateinit var overviewDialogController: OverviewDialogController

    private val browserContainer: BrowserContainer = BrowserContainer()

    private lateinit var touchController: TouchAreaViewController

    private lateinit var twoPaneController: TwoPaneController

    private val dialogManager: DialogManager by lazy { DialogManager(this) }

    private val recordDb: RecordDb by inject()

    private lateinit var customFontResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var saveImageFilePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var writeEpubFilePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var createWebArchivePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var openBookmarkFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var createBookmarkFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    // Classes
    private inner class VideoCompletionListener : OnCompletionListener,
        MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

        override fun onCompletion(mp: MediaPlayer) {
            onHideCustomView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // workaround for crash issue
        // Caused by java.lang.NoSuchMethodException:
        super.onCreate(null)

        binding = ActivityMainBinding.inflate(layoutInflater)

        savedInstanceState?.let {
            shouldLoadTabState = it.getBoolean(K_SHOULD_LOAD_TAB_STATE)
        }

        config.restartChanged = false
        HelperUnit.applyTheme(this)
        setContentView(binding.root)

        orientation = resources.configuration.orientation

        mainContentLayout = findViewById(R.id.main_content)
        subContainer = findViewById(R.id.sub_container)
        updateAppbarPosition()
        initLaunchers()
        initToolbar()
        initSearchPanel()
        initInputBar()
        initOverview()
        initTouchArea()
        initActionModeViewModel()

        downloadReceiver = createDownloadReceiver(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
        }

        dispatchIntent(intent)
        // after dispatching intent, the value should be reset to false
        shouldLoadTabState = false

        if (config.keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        initLanguageLabel()
        initTouchAreaViewController()
        initTextSearchButton()
        initExternalSearchCloseButton()
        initTranslationViewModel()

        if (config.hideStatusbar) {
            hideStatusBar()
        }
        handleWindowInsets()
        listenKeyboardShowHide()
    }

    private fun initTranslationViewModel() {
        lifecycleScope.launch {
            translationViewModel.showEditDialogWithIndex.collect { index ->
                if (index == -1) return@collect
                ShowEditGptActionDialogFragment(index)
                    .showNow(supportFragmentManager, "editGptAction")
                translationViewModel.resetEditDialogIndex()
            }
        }

    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { view, windowInsets ->
            val insetsNavigationBar: Insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val insetsKeyboard: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as FrameLayout.LayoutParams

            if (config.hideStatusbar) {
                if (isKeyboardDisplaying()) {
                    params.bottomMargin = insetsKeyboard.bottom
                } else {
                    if (insetsNavigationBar.bottom > 0) {
                        params.bottomMargin = insetsNavigationBar.bottom
                    } else {
                        params.bottomMargin = 0
                    }
                }
                view.layoutParams = params
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initExternalSearchCloseButton() {
        binding.activityMainContent.externalSearchClose.setOnClickListener {
            moveTaskToBack(true)
            externalSearchViewModel.setButtonVisibility(false)
        }
        val externalSearchContainer = binding.activityMainContent.externalSearchActionContainer
        externalSearchViewModel.searchActions.forEach { action ->
            val button = TextView(this).apply {
                height = ViewUnit.dpToPixel(this@BrowserActivity, 40).toInt()
                textSize = ViewUnit.dpToPixel(this@BrowserActivity, 10)
                gravity = Gravity.CENTER
                background = getDrawable(R.drawable.background_with_border)
                text = action.title.take(2).uppercase(Locale.getDefault())
                setOnClickListener {
                    externalSearchViewModel.currentSearchAction = action
                    ninjaWebView.loadUrl(
                        externalSearchViewModel.generateSearchUrl(
                            splitSearchItemInfo = action
                        )
                    )
                }
            }
            externalSearchContainer.addView(button, 0)
        }
        lifecycleScope.launch {
            externalSearchViewModel.showButton.collect { show ->
                externalSearchContainer.visibility = if (show) VISIBLE else INVISIBLE
            }
        }
    }

    private fun initTextSearchButton() {
        val remoteTextSearch = findViewById<ImageButton>(R.id.remote_text_search)
        remoteTextSearch.setOnClickListener {
            remoteConnViewModel.reset()
        }
        lifecycleScope.launch {
            remoteConnViewModel.remoteConnected.collect { connected ->
                remoteTextSearch.setImageResource(
                    if (remoteConnViewModel.isSendingTextSearch) R.drawable.ic_send
                    else R.drawable.ic_receive
                )
                remoteTextSearch.isVisible = connected
            }
        }
    }

    private fun initTouchAreaViewController() {
        touchController = TouchAreaViewController(binding.root) { ninjaWebView }
    }

    private fun initActionModeViewModel() {
        lifecycleScope.launch {
            actionModeMenuViewModel.actionModeMenuState.collect { state ->
                val point = actionModeMenuViewModel.clickedPoint.value
                when (state) {
                    is HighlightText -> {
                        lifecycleScope.launch {
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

                        translationViewModel.viewModelScope.launch {
                            translationViewModel.updateInputMessage(actionModeMenuViewModel.selectedText.value)
                            val selectedTextWithContext = ninjaWebView.getSelectedTextWithContext()
                            translationViewModel.updateMessageWithContext(selectedTextWithContext)
                            translationViewModel.url = ninjaWebView.url.orEmpty()
                            TranslateDialogFragment(
                                translationViewModel,
                                externalSearchWebView,
                                point
                            )
                                .show(supportFragmentManager, "translateDialog")
                            actionModeMenuViewModel.finish()
                        }
                    }

                    is Gpt -> {
                        translationViewModel.viewModelScope.launch {
                            translationViewModel.updateInputMessage(actionModeMenuViewModel.selectedText.value)
                            val selectedTextWithContext = ninjaWebView.getSelectedTextWithContext()
                            translationViewModel.updateMessageWithContext(selectedTextWithContext)
                            if (translationViewModel.hasOpenAiApiKey()) {
                                translationViewModel.updateTranslateMethod(TRANSLATE_API.GPT)
                                translationViewModel.gptActionInfo = state.gptAction
                                translationViewModel.url = ninjaWebView.url.orEmpty()
                                TranslateDialogFragment(
                                    translationViewModel,
                                    externalSearchWebView,
                                    actionModeMenuViewModel.clickedPoint.value,
                                )
                                    .show(supportFragmentManager, "contextMenu")
                            } else {
                                NinjaToast.show(this@BrowserActivity, R.string.gpt_api_key_not_set)
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
                        IntentUnit.tts(this@BrowserActivity, state.text)
                        actionModeMenuViewModel.finish()
                    }

                    Idle -> Unit
                }
            }
        }
    }

    private suspend fun highlightText(highlightStyle: HighlightStyle) {
        // work on UI first
        ninjaWebView.highlightTextSelection(highlightStyle)

        // work on db saving
        val url = ninjaWebView.url.orEmpty()
        val title = ninjaWebView.title.orEmpty()
        val article = Article(title, url, System.currentTimeMillis(), "")

        val articleInDb =
            bookmarkManager.getArticleByUrl(url) ?: bookmarkManager.insertArticle(article)

        val selectedText = actionModeMenuViewModel.selectedText.value
        val highlight = Highlight(articleInDb.id, selectedText)
        bookmarkManager.insertHighlight(highlight)
    }

    private fun isInSplitSearchMode(): Boolean =
        splitSearchViewModel.state != null && twoPaneController.isSecondPaneDisplayed()

    private fun initLanguageLabel() {
        languageLabelView = findViewById(R.id.translation_language)
        lifecycleScope.launch {
            translationViewModel.translationLanguage.collect {
                ViewUnit.updateLanguageLabel(languageLabelView!!, it)
            }
        }

        languageLabelView?.setOnClickListener {
            lifecycleScope.launch {
                val translationLanguage =
                    TranslationLanguageDialog(this@BrowserActivity).show() ?: return@launch
                translationViewModel.updateTranslationLanguage(translationLanguage)
                ninjaWebView.clearTranslationElements()
                translateByParagraph(ninjaWebView.translateApi)
            }
        }
    }

    override fun isAtTop(): Boolean = ninjaWebView.isAtTop()
    override fun jumpToTop() = ninjaWebView.jumpToTop()
    override fun jumpToBottom() = ninjaWebView.jumpToBottom()
    override fun pageDown() = ninjaWebView.pageDownWithNoAnimation()
    override fun pageUp() = ninjaWebView.pageUpWithNoAnimation()
    override fun toggleReaderMode() = ninjaWebView.toggleReaderMode()
    override fun toggleVerticalRead() = ninjaWebView.toggleVerticalRead()
    override fun updatePageInfo(info: String) = composeToolbarViewController.updatePageInfo(info)

    override fun addToPocket(url: String) {
        lifecycleScope.launch {
            when (val sharedState =
                pocketViewModel.shareToPocketWithLoginCheck(this@BrowserActivity, url)) {
                is PocketShareState.SharedByEinkBro -> {
                    Snackbar.make(binding.root, "Added", Snackbar.LENGTH_LONG).apply {
                        setAction("Go to Pocket article url") {
                            addNewTab(sharedState.pocketUrl)
                        }
                    }.show()
                }

                is PocketShareState.NeedLogin -> addNewTab(sharedState.authUrl)
                PocketShareState.Failed -> NinjaToast.showShort(this@BrowserActivity, "Failed")
                PocketShareState.SharedByPocketApp -> Unit // done by pocket app
            }
        }
    }

    override fun handlePocketRequestToken(requestToken: String) {
        lifecycleScope.launch {
            pocketViewModel.getAndSaveAccessToken()
            addToPocket(pocketViewModel.urlToBeAdded)
        }
    }

    override fun translate(translationMode: TranslationMode) {
        when (translationMode) {
            TranslationMode.TRANSLATE_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.GOOGLE)
            TranslationMode.PAPAGO_TRANSLATE_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.PAPAGO)
            TranslationMode.PAPAGO_TRANSLATE_BY_SCREEN -> translateWebView()
            TranslationMode.GOOGLE_IN_PLACE -> ninjaWebView.addGoogleTranslation()
            else -> Unit
        }
    }

    override fun resetTranslateUI() {
        languageLabelView?.visibility = GONE
    }

    override fun configureTranslationLanguage(translateApi: TRANSLATE_API) {
        LanguageSettingDialogFragment(translateApi, translationViewModel) {
            if (translateApi == TRANSLATE_API.GOOGLE) {
                translateByParagraph(TRANSLATE_API.GOOGLE)
            } else {
                translateByParagraph(TRANSLATE_API.PAPAGO)
            }
        }
            .show(supportFragmentManager, "LanguageSettingDialog")
    }

    override fun toggleTouchPagination() = toggleTouchTurnPageFeature()

    override fun toggleTextSearch() {
        remoteConnViewModel.toggleTextSearch()
    }

    override fun sendToRemote(text: String) {
        if (remoteConnViewModel.isSendingTextSearch) {
            remoteConnViewModel.toggleTextSearch()
            NinjaToast.show(this, R.string.send_to_remote_terminate)
            return
        }

        SendLinkDialog(this, lifecycleScope).show(text)
    }

    override fun summarizeContent() {
        if (config.gptApiKey.isNotBlank()) {
            lifecycleScope.launch {
                with(translationViewModel) {
                    updateInputMessage(ninjaWebView.getRawText())
                    updateTranslateMethod(TRANSLATE_API.GPT)
                    gptActionInfo =
                        ChatGPTActionInfo(systemMessage = config.gptUserPromptForWebPage)
                }
                translationViewModel.url = ninjaWebView.url.orEmpty()
                TranslateDialogFragment(
                    translationViewModel,
                    externalSearchWebView,
                    actionModeMenuViewModel.clickedPoint.value,
                )
                    .show(supportFragmentManager, "contextMenu")

            }
        }
    }

    override fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float) {
        //Log.d("touch", "updateSelectionRect: $left, $top, $right, $bottom")
        // 10 for the selection indicator height
        val newPoint = Point(
            ViewUnit.dpToPixel(this, right.toInt()).toInt(),
            ViewUnit.dpToPixel(this, bottom.toInt() + 16).toInt()
        )
        if (abs(newPoint.x - actionModeMenuViewModel.clickedPoint.value.x) > ViewUnit.dpToPixel(
                this@BrowserActivity,
                15
            ) ||
            abs(newPoint.y - actionModeMenuViewModel.clickedPoint.value.y) > ViewUnit.dpToPixel(
                this@BrowserActivity,
                15
            )
        ) {
            actionModeMenuViewModel.hide()
        }
        actionModeMenuViewModel.updateClickedPoint(newPoint)
    }

    override fun toggleReceiveTextSearch() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
        } else {
            remoteConnViewModel.toggleReceiveLink { ninjaWebView.loadUrl(it) }
        }
    }

    override fun toggleReceiveLink() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
            NinjaToast.show(this, R.string.receive_link_terminate)
            return
        }

        ReceiveDataDialog(this@BrowserActivity, lifecycleScope).show {
            ShareUtil.startReceiving(lifecycleScope) { url ->
                if (url.isNotBlank()) {
                    ninjaWebView.loadUrl(url)
                    ShareUtil.stopBroadcast()
                }
            }
        }
    }

    private fun initLaunchers() {
        saveImageFilePickerLauncher = IntentUnit.createSaveImageFilePickerLauncher(this)
        customFontResultLauncher =
            IntentUnit.createResultLauncher(this) { handleFontSelectionResult(it) }
        openBookmarkFileLauncher = backupUnit.createOpenBookmarkFileLauncher(this)
        createBookmarkFileLauncher = backupUnit.createCreateBookmarkFileLauncher(this)
        createWebArchivePickerLauncher =
            IntentUnit.createResultLauncher(this) { saveWebArchive(it) }
        writeEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(this) {
                val uri = backupUnit.preprocessActivityResult(it) ?: return@createResultLauncher
                saveEpub(uri)
            }
        fileChooserLauncher =
            IntentUnit.createResultLauncher(this) { handleWebViewFileChooser(it) }
    }

    private fun handleFontSelectionResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        BrowserUnit.handleFontSelectionResult(this, result, ninjaWebView.shouldUseReaderFont())
    }

    private fun handleWebViewFileChooser(result: ActivityResult) {
        if (result.resultCode != RESULT_OK || filePathCallback == null) {
            filePathCallback = null
            return
        }
        var results: Array<Uri>?
        // Check that the response is a good one
        val data = result.data
        if (data != null) {
            // If there is not data, then we may have taken a photo
            val dataString = data.dataString
            results = arrayOf(Uri.parse(dataString))
            filePathCallback?.onReceiveValue(results)
        }
        filePathCallback = null
    }

    private fun saveEpub(uri: Uri) {
        val progressDialog =
            dialogManager.createProgressDialog(R.string.saving_epub).apply { show() }
        epubManager.saveEpub(
            this,
            uri,
            ninjaWebView,
            {
                progressDialog.progress = it
                if (it == 100) {
                    progressDialog.dismiss()
                }
            },
            {
                progressDialog.dismiss()
                dialogManager.showOkCancelDialog(
                    messageResId = R.string.cannot_save_epub,
                    okAction = {},
                    showInCenter = true,
                    showNegativeButton = false,
                )
            }
        )
    }

    private fun saveWebArchive(result: ActivityResult) {
        val uri = backupUnit.preprocessActivityResult(result) ?: return
        saveWebArchiveToUri(uri)
    }

    private fun saveWebArchiveToUri(uri: Uri) {
        // get archive from webview
        val filePath = File(filesDir.absolutePath + "/temp.mht").absolutePath
        ninjaWebView.saveWebArchive(filePath, false) {
            val tempFile = File(filePath)
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (isMeetPipCriteria()) {
            enterPipMode()
        }
    }

    private fun isMeetPipCriteria() = config.enableVideoPip &&
            fullscreenHolder != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipMode() {
        val params = PictureInPictureParams.Builder().build();
        enterPictureInPictureMode(params)
    }

    private fun initInputBar() {
        binding.inputUrl.apply {
            focusRequester = FocusRequester()
            onTextSubmit = { updateAlbum(it.trim()); showToolbar() }
            onPasteClick = { updateAlbum(getClipboardText()); showToolbar() }
            closeAction = { showToolbar() }
            onRecordClick = {
                updateAlbum(it.url)
                showToolbar()
            }
        }
        binding.inputUrl.bookmarkManager = bookmarkManager
    }

    private fun isKeyboardDisplaying(): Boolean {
        val rect = Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val heightDiff: Int = binding.root.rootView.height - rect.bottom
        return heightDiff > binding.root.rootView.height * 0.15
    }

    private fun listenKeyboardShowHide() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isKeyboardDisplaying()) { // Value should be less than keyboard's height
                touchController.maybeDisableTemporarily()
            } else {
                touchController.maybeEnableAgain()
            }
        }
    }

    private var orientation: Int = 0
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags != uiMode && config.darkMode == DarkMode.SYSTEM) {
                recreate()
            }
        }
        if (newConfig.orientation != orientation) {
            composeToolbarViewController.updateIcons()
            orientation = newConfig.orientation

            if (config.fabPosition == FabPosition.Custom) {
                fabImageViewController.updateImagePosition(orientation)
            }
        }
    }

    private fun initTouchArea() = updateTouchView()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (config.restartChanged) {
            config.restartChanged = false
            dialogManager.showRestartConfirmDialog()
        }

        updateTitle()
        overridePendingTransition(0, 0)
        uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (config.customFontChanged &&
            (config.fontType == FontType.CUSTOM || config.readerFontType == FontType.CUSTOM)
        ) {
            if (!ninjaWebView.shouldUseReaderFont()) {
                ninjaWebView.reload()
            } else {
                ninjaWebView.updateCssStyle()
            }
            config.customFontChanged = false
        }
        if (!config.continueMedia) {
            if (this::ninjaWebView.isInitialized) {
                ninjaWebView.resumeTimers()
            }
        }
    }

    override fun onDestroy() {
        ttsManager.stopReading()

        updateSavedAlbumInfo()

        if (config.clearWhenQuit && shouldRunClearService) {
            startService(Intent(this, ClearService::class.java))
        }

        browserContainer.clear()
        unregisterReceiver(downloadReceiver)

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (config.useUpDownPageTurn) ninjaWebView.pageDownWithNoAnimation()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (config.useUpDownPageTurn) ninjaWebView.pageUpWithNoAnimation()
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> return handleVolumeDownKey()
            KeyEvent.KEYCODE_VOLUME_UP -> return handleVolumeUpKey()
            KeyEvent.KEYCODE_MENU -> {
                showMenuDialog(); return true
            }

            KeyEvent.KEYCODE_BACK -> {
                handleBackKey(); return true
            }
        }
        return false
    }

    private fun handleVolumeDownKey(): Boolean {
        return if (config.volumePageTurn) {
            if (ninjaWebView.isVerticalRead) {
                ninjaWebView.pageUpWithNoAnimation()
            } else {
                ninjaWebView.pageDownWithNoAnimation()
            }
            true
        } else {
            false
        }
    }

    private fun handleVolumeUpKey(): Boolean {
        return if (config.volumePageTurn) {
            if (ninjaWebView.isVerticalRead) {
                ninjaWebView.pageDownWithNoAnimation()
            } else {
                ninjaWebView.pageUpWithNoAnimation()
            }
            true
        } else {
            false
        }
    }

    override fun handleBackKey() {
        hideKeyboard()
        if (overviewDialogController.isVisible()) {
            hideOverview()
        }
        if (fullscreenHolder != null || customView != null || videoView != null) {
            onHideCustomView()
        } else if (!binding.appBar.isVisible && config.showToolbarFirst) {
            showToolbar()
        } else if (!composeToolbarViewController.isDisplayed()) {
            composeToolbarViewController.show()
        } else {
            // disable back key when it's translate mode web page
            if (!ninjaWebView.isTranslatePage && ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                if (config.closeTabWhenNoMoreBackHistory) {
                    removeAlbum()
                } else {
                    NinjaToast.show(this, getString(R.string.no_previous_page))
                }
            }
        }
    }

    override fun isCurrentAlbum(albumController: AlbumController): Boolean =
        currentAlbumController == albumController

    override fun showAlbum(controller: AlbumController) {
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
                // if it's the same controller, just scroll to top
                if (ninjaWebView.isAtTop()) {
                    ninjaWebView.reload()
                } else {
                    jumpToTop()
                }
                return
            }
            currentAlbumController?.deactivate()
        }

        // remove current view from the container first
        val controllerView = controller as View
        if (mainContentLayout.childCount > 0) {
            for (i in 0 until mainContentLayout.childCount) {
                if (mainContentLayout.getChildAt(i) == controllerView) {
                    mainContentLayout.removeView(controllerView)
                    break
                }
            }
        }

        mainContentLayout.addView(
            controller as View,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        currentAlbumController = controller
        currentAlbumController?.activate()

        updateSavedAlbumInfo()
        updateWebViewCount()

        progressBar.visibility = GONE
        ninjaWebView = controller as NinjaWebView

        updateTitle()
        ninjaWebView.updatePageInfo()

        languageLabelView?.visibility =
            (if (ninjaWebView.isTranslatePage) VISIBLE else GONE)

        // when showing a new album, should turn off externalSearch button visibility
        externalSearchViewModel.setButtonVisibility(false)
        runOnUiThread {
            composeToolbarViewController.updateFocusIndex(
                albumViewModel.albums.value.indexOfFirst { it.isActivated }
            )
        }
    }

    private fun openCustomFontPicker() = BrowserUnit.openFontFilePicker(customFontResultLauncher)

    override fun showOverview() = overviewDialogController.show()

    override fun hideOverview() = overviewDialogController.hide()

    override fun rotateScreen() = IntentUnit.rotateScreen(this)

    override fun saveBookmark(url: String?, title: String?) {
        val currentUrl = url ?: ninjaWebView.url ?: return
        val nonNullTitle = title ?: HelperUnit.secString(ninjaWebView.title)
        try {
            lifecycleScope.launch {
                BookmarkEditDialog(
                    this@BrowserActivity,
                    bookmarkManager,
                    Bookmark(nonNullTitle, currentUrl),
                    {
                        handleBookmarkSync(true)
                        hideKeyboard()
                        NinjaToast.show(this@BrowserActivity, R.string.toast_edit_successful)
                    },
                    { hideKeyboard() }
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(this, R.string.toast_error)
        }
    }

    override fun createShortcut() {
        val currentUrl = ninjaWebView.url ?: return
        ShortcutEditDialog(
            this@BrowserActivity,
            HelperUnit.secString(ninjaWebView.title),
            currentUrl,
            ninjaWebView.favicon,
            {
                hideKeyboard()
                NinjaToast.show(this@BrowserActivity, R.string.toast_edit_successful)
            },
            { hideKeyboard() }
        ).show()
    }

    override fun toggleTouchTurnPageFeature() = config::enableTouchTurn.toggle()

    override fun toggleSwitchTouchAreaAction() = config::switchTouchAreaAction.toggle()

    private fun updateTouchView() = composeToolbarViewController.updateIcons()

    // Methods
    override fun showFontSizeChangeDialog() {
        if (ninjaWebView.shouldUseReaderFont()) {
            ReaderFontDialogFragment { openCustomFontPicker() }.show(
                supportFragmentManager,
                "font_dialog"
            )
        } else {
            FontDialogFragment { openCustomFontPicker() }.show(
                supportFragmentManager,
                "font_dialog"
            )
        }
    }

    private fun changeFontSize(size: Int) {
        if (ninjaWebView.shouldUseReaderFont()) {
            config.readerFontSize = size
        } else {
            config.fontSize = size
        }
    }

    override fun increaseFontSize() {
        val fontSize =
            if (ninjaWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
        changeFontSize(fontSize + 20)
    }

    override fun decreaseFontSize() {
        val fontSize =
            if (ninjaWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
        if (fontSize > 50) changeFontSize(fontSize - 20)
    }

    private fun maybeInitTwoPaneController() {
        if (!isTwoPaneControllerInitialized()) {
            twoPaneController = TwoPaneController(
                this,
                lifecycleScope,
                binding.subContainer,
                binding.twoPanelLayout,
                { showTranslation() },
                { if (ninjaWebView.isReaderModeOn) ninjaWebView.toggleReaderMode() },
                { url -> ninjaWebView.loadUrl(url) },
                this::translateByParagraph,
                this::translateWebView
            )
        }
    }

    private fun translateByParagraph(translateApi: TRANSLATE_API) {
        if (config.enableInplaceParagraphTranslate) {
            translateByParagraphInPlace(translateApi)
        } else {
            translateByParagraphInReaderMode(translateApi)
        }
    }

    private fun translateByParagraphInPlace(translateApi: TRANSLATE_API) {
        lifecycleScope.launch {
            ninjaWebView.translateApi = translateApi
            ninjaWebView.translateByParagraphInPlace()
            languageLabelView?.visibility = VISIBLE
        }
    }

    private fun translateByParagraphInReaderMode(translateApi: TRANSLATE_API) {
        lifecycleScope.launch {
            val currentUrl = ninjaWebView.url

            // assume it's current one
            val translateModeWebView = if (ninjaWebView.isTranslatePage) {
                ninjaWebView
            } else {
                // get html from original WebView
                val htmlCache = ninjaWebView.getRawReaderHtml()
                // create a new WebView
                addAlbum("", "")
                // set it to translate mode
                ninjaWebView.isTranslatePage = true
                ninjaWebView.translateApi = translateApi
                // set its raw html to be the same as original WebView
                ninjaWebView.rawHtmlCache = htmlCache
                // show the language label
                languageLabelView?.visibility = VISIBLE
                ninjaWebView
            }

            val translatedHtml = translationViewModel
                .translateByParagraph(translateModeWebView.rawHtmlCache ?: return@launch)
            if (translateModeWebView.isAttachedToWindow) {
                translateModeWebView.loadDataWithBaseURL(
                    if (!ninjaWebView.isPlainText) currentUrl else null,
                    translatedHtml,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    }

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    override fun showTranslation() {
        maybeInitTwoPaneController()

        lifecycleScope.launch(Dispatchers.Main) {
            twoPaneController.showTranslation(ninjaWebView)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(K_SHOULD_LOAD_TAB_STATE, true)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("InlinedApi")
    open fun dispatchIntent(intent: Intent) {
        if (overviewDialogController.isVisible()) {
            overviewDialogController.hide()
        }

        when (intent.action) {
            "", Intent.ACTION_MAIN -> {
                initSavedTabs { addAlbum() }
            }

            ACTION_VIEW -> {
                initSavedTabs()
                // if webview for that url already exists, show the original tab, otherwise, create new
                val viewUri = intent.data?.toNormalScheme() ?: return
                if (viewUri.scheme == "content") {
                    val (filename, mimetype) = HelperUnit.getFileInfoFromContentUri(this, viewUri)
                    val mimeType = contentResolver.getType(viewUri)
                    //Log.d(TAG, "mimeType: $mimeType")
                    if (filename?.endsWith(".srt") == true ||
                        mimeType.equals("application/x-subrip")
                    ) {
                        // srt
                        addAlbum()
                        val stringList =
                            HelperUnit.readContentAsStringList(contentResolver, viewUri)
                        val htmlContent = HelperUnit.srtToHtml(stringList)
                        ninjaWebView.isPlainText = true
                        ninjaWebView.rawHtmlCache = htmlContent
                        ninjaWebView.loadData(htmlContent, "text/html", "utf-8")

                    } else if (mimeType.equals("application/octet-stream")) {
                        HelperUnit.getCachedPathFromURI(this, viewUri).let {
                            addAlbum(url = "file://$it")
                        }
                    } else if (filename?.endsWith(".mht") == true) {
                        // mht
                        HelperUnit.getCachedPathFromURI(this, viewUri).let {
                            addAlbum(url = "file://$it")
                        }
                    } else if (filename?.endsWith(".html") == true || mimeType.equals("text/html")) {
                        // local html
                        updateAlbum(url = viewUri.toString())
                    } else {
                        // epub
                        epubManager.showEpubReader(viewUri)
                        finish()
                    }
                } else {
                    val url = viewUri.toString()
                    getUrlMatchedBrowser(url)?.let { showAlbum(it) } ?: addAlbum(url = url)
                }
            }

            Intent.ACTION_WEB_SEARCH -> {
                initSavedTabs()
                val searchedKeyword = intent.getStringExtra(SearchManager.QUERY).orEmpty()
                if (currentAlbumController != null && config.isExternalSearchInSameTab) {
                    ninjaWebView.loadUrl(searchedKeyword)
                } else {
                    addAlbum(url = searchedKeyword)
                }
            }

            "sc_history" -> {
                addAlbum(); openHistoryPage()
            }

            "sc_home" -> {
                addAlbum(config.favoriteUrl)
            }

            "sc_bookmark" -> {
                addAlbum(); openBookmarkPage()
            }

            Intent.ACTION_SEND -> {
                initSavedTabs()
                val sentKeyword = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val url =
                    if (BrowserUnit.isURL(sentKeyword)) sentKeyword else externalSearchViewModel.generateSearchUrl(
                        sentKeyword
                    )
                if (currentAlbumController != null && config.isExternalSearchInSameTab) {
                    ninjaWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                initSavedTabs()
                val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: return

                if (remoteConnViewModel.isSendingTextSearch) {
                    remoteConnViewModel.sendTextSearch(
                        externalSearchViewModel.generateSearchUrl(
                            text
                        )
                    )
                    moveTaskToBack(true)
                    return
                }

                val url =
                    if (BrowserUnit.isURL(text)) text else externalSearchViewModel.generateSearchUrl(
                        text
                    )
                if (currentAlbumController != null && config.isExternalSearchInSameTab) {
                    ninjaWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
                // set minimize button visible
                externalSearchViewModel.setButtonVisibility(true)
            }

            ACTION_DICT -> {
                val text = intent.getStringExtra("EXTRA_QUERY") ?: return

                if (remoteConnViewModel.isSendingTextSearch) {
                    remoteConnViewModel.sendTextSearch(
                        externalSearchViewModel.generateSearchUrl(
                            text
                        )
                    )
                    moveTaskToBack(true)
                    return
                }

                initSavedTabs()
                val url = externalSearchViewModel.generateSearchUrl(text)
                if (currentAlbumController != null && config.isExternalSearchInSameTab) {
                    ninjaWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
                // set minimize button visible
                externalSearchViewModel.setButtonVisibility(true)
            }

            null -> {}
            else -> addAlbum()
        }
        getIntent().action = ""
    }

    private fun initSavedTabs(whenNoSavedTabs: (() -> Unit)? = null) {
        if (currentAlbumController == null) { // newly opened Activity
            if ((shouldLoadTabState || config.shouldSaveTabs) &&
                config.savedAlbumInfoList.isNotEmpty()
            ) {
                // fix current album index is larger than album size
                if (config.currentAlbumIndex >= config.savedAlbumInfoList.size) {
                    config.currentAlbumIndex = config.savedAlbumInfoList.size - 1
                }
                val albumList = config.savedAlbumInfoList.toList()
                var savedIndex = config.currentAlbumIndex
                // fix issue
                if (savedIndex == -1) savedIndex = 0
                albumList.forEachIndexed { index, albumInfo ->
                    addAlbum(
                        title = albumInfo.title,
                        url = albumInfo.url,
                        foreground = (index == savedIndex)
                    )
                }
            } else {
                whenNoSavedTabs?.invoke()
            }
        }
    }

    private fun updateAppbarPosition() {
        if (config.isToolbarOnTop) {
            moveAppbarToTop()
        } else {
            moveAppbarToBottom()
        }
        binding.inputUrl.shouldReverse = config.isToolbarOnTop
    }

    private fun moveAppbarToBottom() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            connect(
                binding.appBar.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            connect(
                binding.inputUrl.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.BOTTOM,
                binding.appBar.id,
                ConstraintSet.TOP
            )

            clear(binding.contentSeparator.id, ConstraintSet.TOP)
            connect(
                binding.contentSeparator.id,
                ConstraintSet.BOTTOM,
                binding.appBar.id,
                ConstraintSet.TOP
            )
        }
        constraintSet.applyTo(binding.root)
    }

    private fun moveAppbarToTop() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(binding.appBar.id, ConstraintSet.BOTTOM)
            clear(binding.inputUrl.id, ConstraintSet.BOTTOM)

            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.TOP,
                binding.appBar.id,
                ConstraintSet.BOTTOM
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )

            clear(binding.contentSeparator.id, ConstraintSet.BOTTOM)
            connect(
                binding.contentSeparator.id,
                ConstraintSet.TOP,
                binding.appBar.id,
                ConstraintSet.BOTTOM
            )
        }
        constraintSet.applyTo(binding.root)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initToolbar() {
        progressBar = findViewById(R.id.main_progress_bar)
        if (config.darkMode == DarkMode.FORCE_ON) {
            val nightModeFlags: Int =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                progressBar.progressTintMode = PorterDuff.Mode.LIGHTEN
            }
        }
        initFAB()
        if (config.enableNavButtonGesture) {
            val onNavButtonTouchListener = object : SwipeTouchListener(this@BrowserActivity) {
                override fun onSwipeTop() = gestureHandler.handle(config.navGestureUp)
                override fun onSwipeBottom() = gestureHandler.handle(config.navGestureDown)
                override fun onSwipeRight() = gestureHandler.handle(config.navGestureRight)
                override fun onSwipeLeft() = gestureHandler.handle(config.navGestureLeft)
            }
            fabImageViewController.defaultTouchListener = onNavButtonTouchListener
        }

        composeToolbarViewController.updateIcons()
        // strange crash on my device. register later
        runOnUiThread {
            config.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }

    override fun showTouchAreaDialog() = TouchAreaDialogFragment(ninjaWebView.url.orEmpty())
        .show(supportFragmentManager, "TouchAreaDialog")

    override fun showTranslationConfigDialog(translateDirectly: Boolean) {
        maybeInitTwoPaneController()
        twoPaneController.showTranslationConfigDialog(translateDirectly)
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ConfigManager.K_HIDE_STATUSBAR -> {
                    if (config.hideStatusbar) {
                        hideStatusBar()
                    } else {
                        showStatusBar()
                    }
                }

                ConfigManager.K_TOOLBAR_ICONS_FOR_LARGE,
                ConfigManager.K_TOOLBAR_ICONS,
                -> {
                    composeToolbarViewController.updateIcons()
                }

                ConfigManager.K_SHOW_TAB_BAR -> {
                    composeToolbarViewController.showTabbar(config.shouldShowTabBar)
                }

                ConfigManager.K_FONT_TYPE -> {
                    if (config.fontType == FontType.SYSTEM_DEFAULT) {
                        ninjaWebView.reload()
                    } else {
                        ninjaWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_READER_FONT_TYPE -> {
                    if (config.readerFontType == FontType.SYSTEM_DEFAULT) {
                        ninjaWebView.reload()
                    } else {
                        ninjaWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_FONT_SIZE -> {
                    ninjaWebView.settings.textZoom = config.fontSize
                }

                ConfigManager.K_READER_FONT_SIZE -> {
                    if (ninjaWebView.shouldUseReaderFont()) {
                        ninjaWebView.settings.textZoom = config.readerFontSize
                    }
                }

                ConfigManager.K_BOLD_FONT -> {
                    composeToolbarViewController.updateIcons()
                    if (config.boldFontStyle) {
                        ninjaWebView.updateCssStyle()
                    } else {
                        ninjaWebView.reload()
                    }
                }

                ConfigManager.K_BLACK_FONT -> {
                    composeToolbarViewController.updateIcons()
                    if (config.blackFontStyle) {
                        ninjaWebView.updateCssStyle()
                    } else {
                        ninjaWebView.reload()
                    }
                }

                ConfigManager.K_ENABLE_IMAGE_ADJUSTMENT -> ninjaWebView.reload()

                ConfigManager.K_WHITE_BACKGROUND_LIST -> {
                    if (config.whiteBackground(ninjaWebView.url.orEmpty())) {
                        ninjaWebView.updateCssStyle()
                    } else {
                        ninjaWebView.reload()
                    }
                }

                ConfigManager.K_CUSTOM_FONT -> {
                    if (config.fontType == FontType.CUSTOM) {
                        ninjaWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_READER_CUSTOM_FONT -> {
                    if (config.readerFontType == FontType.CUSTOM && ninjaWebView.shouldUseReaderFont()) {
                        ninjaWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_IS_INCOGNITO_MODE -> {
                    ninjaWebView.incognito = config.isIncognitoMode
                    composeToolbarViewController.updateIcons()
                    NinjaToast.showShort(
                        this,
                        "Incognito mode is " + if (config.isIncognitoMode) "enabled." else "disabled."
                    )
                }

                ConfigManager.K_KEEP_AWAKE -> {
                    if (config.keepAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                ConfigManager.K_DESKTOP -> {
                    ninjaWebView.updateUserAgentString()
                    ninjaWebView.reload()
                    composeToolbarViewController.updateIcons()
                }

                ConfigManager.K_DARK_MODE -> config.restartChanged = true
                ConfigManager.K_TOOLBAR_TOP -> updateAppbarPosition()

                ConfigManager.K_NAV_POSITION -> fabImageViewController.initialize()
                ConfigManager.K_TTS_SPEED_VALUE ->
                    ttsManager.setSpeechRate(config.ttsSpeedValue / 100f)

                ConfigManager.K_CUSTOM_USER_AGENT,
                ConfigManager.K_ENABLE_CUSTOM_USER_AGENT,
                -> {
                    ninjaWebView.updateUserAgentString()
                    ninjaWebView.reload()
                }

                ConfigManager.K_ENABLE_TOUCH -> {
                    updateTouchView()
                    touchController.toggleTouchPageTurn(config.enableTouchTurn)
                }

                ConfigManager.K_TOUCH_AREA_ACTION_SWITCH -> {
                    updateTouchView()
                }
            }
        }

    private lateinit var fabImageViewController: FabImageViewController
    private fun initFAB() {
        fabImageViewController = FabImageViewController(
            orientation,
            findViewById(R.id.fab_imageButtonNav),
            this::showToolbar,
            this::showFastToggleDialog
        )
    }

    private val gestureHandler: GestureHandler by lazy { GestureHandler(this) }

    override fun goForward() {
        if (ninjaWebView.canGoForward()) {
            ninjaWebView.goForward()
        } else {
            NinjaToast.show(this, R.string.toast_webview_forward)
        }
    }

    override fun gotoLeftTab() {
        nextAlbumController(false)?.let { showAlbum(it) }
    }

    override fun gotoRightTab() {
        nextAlbumController(true)?.let { showAlbum(it) }
    }

    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager.bookmarkDao)
    }

    private val pocketViewModel: PocketViewModel by viewModels {
        PocketViewModelFactory()
    }

    private val actionModeMenuViewModel: ActionModeMenuViewModel by viewModels()

    private fun initOverview() {
        overviewDialogController = OverviewDialogController(
            this,
            albumViewModel.albums,
            albumViewModel.focusIndex,
            binding.layoutOverview,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            addIncognitoTabAction = {
                hideOverview()
                addAlbum(getString(R.string.app_name), "", incognito = true)
                focusOnInput()
            },
            onHistoryChanged = { },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            addEmptyTabAction = { newATab() }
        )
    }

    override fun openHistoryPage(amount: Int) = overviewDialogController.openHistoryPage(amount)

    override fun openBookmarkPage() =
        BookmarksDialogFragment(
            lifecycleScope,
            bookmarkViewModel,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            syncBookmarksAction = this::handleBookmarkSync
        ).show(supportFragmentManager, "bookmarks dialog")

    private fun handleBookmarkSync(forceUpload: Boolean = false) {
        backupUnit.handleBookmarkSync(
            forceUpload,
            dialogManager,
            createBookmarkFileLauncher,
            openBookmarkFileLauncher
        )
    }

    private fun initSearchPanel() {
        with(binding.mainSearchPanel) {
            onTextChanged = { (currentAlbumController as NinjaWebView?)?.findAllAsync(it) }
            onCloseClick = { hideSearchPanel() }
            onUpClick = { searchUp(it) }
            onDownClick = { searchDown(it) }
        }
    }

    private fun searchUp(text: String) {
        if (text.isEmpty()) {
            NinjaToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        hideKeyboard()
        (currentAlbumController as NinjaWebView).findNext(false)
    }

    private fun searchDown(text: String) {
        if (text.isEmpty()) {
            NinjaToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        hideKeyboard()
        (currentAlbumController as NinjaWebView).findNext(true)
    }

    override fun showFastToggleDialog() {
        if (!this::ninjaWebView.isInitialized) return

        FastToggleDialogFragment {
            ninjaWebView.initPreferences()
            ninjaWebView.reload()
        }.show(supportFragmentManager, "fast_toggle_dialog")
    }

    override fun addNewTab(url: String) = addAlbum(url = url)

    private fun getUrlMatchedBrowser(url: String): NinjaWebView? {
        return browserContainer.list().firstOrNull { it.albumUrl == url } as NinjaWebView?
    }

    private var preloadedWebView: NinjaWebView? = null

    open fun createNinjaWebView(): NinjaWebView = NinjaWebView(this, this).apply {
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun addAlbum(
        title: String = "",
        url: String = config.favoriteUrl,
        foreground: Boolean = true,
        incognito: Boolean = false,
        enablePreloadWebView: Boolean = true,
    ) {
        val newWebView = (preloadedWebView ?: createNinjaWebView()).apply {
            this.albumTitle = title
            this.incognito = incognito
            setOnTouchListener(createMultiTouchTouchListener(this))
        }

        maybeCreateNewPreloadWebView(enablePreloadWebView, newWebView)

        updateTabPreview(newWebView, url)
        updateWebViewCount()

        loadUrlInWebView(foreground, newWebView, url)

        updateSavedAlbumInfo()
    }

    private fun maybeCreateNewPreloadWebView(
        enablePreloadWebView: Boolean,
        newWebView: NinjaWebView,
    ) {
        preloadedWebView = null
        if (enablePreloadWebView) {
            newWebView.postDelayed({
                if (preloadedWebView == null) {
                    preloadedWebView = createNinjaWebView()
                }
            }, 2000)
        }
    }

    private fun updateTabPreview(newWebView: NinjaWebView, url: String) {
        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            newWebView.setAlbumCover(it)
        }

        val album = newWebView.album
        if (currentAlbumController != null) {
            val index = browserContainer.indexOf(currentAlbumController) + 1
            browserContainer.add(newWebView, index)
            albumViewModel.addAlbum(album, index)
        } else {
            browserContainer.add(newWebView)
            albumViewModel.addAlbum(album, browserContainer.size() - 1)
        }
    }

    private fun loadUrlInWebView(foreground: Boolean, webView: NinjaWebView, url: String) {
        //ViewUnit.bound(this, webView)
        if (!foreground) {
            webView.deactivate()
            if (config.enableWebBkgndLoad) {
                webView.loadUrl(url)
            } else {
                webView.initAlbumUrl = url
            }
        } else {
            showToolbar()
            showAlbum(webView)
            if (url.isNotEmpty() && url != BrowserUnit.URL_ABOUT_BLANK) {
                webView.loadUrl(url)
            } else if (url == BrowserUnit.URL_ABOUT_BLANK) {
            }
        }
    }

    private fun showRecentlyUsedBookmarks(webView: NinjaWebView) {
        val html = BrowserUnit.getRecentBookmarksContent()
        if (html.isNotBlank()) {
            webView.loadDataWithBaseURL(
                null,
                BrowserUnit.getRecentBookmarksContent(),
                "text/html",
                "utf-8",
                null
            )
            webView.albumTitle = getString(R.string.recently_used_bookmarks)
        }
    }

    private fun createMultiTouchTouchListener(ninjaWebView: NinjaWebView): MultitouchListener =
        object : MultitouchListener(this@BrowserActivity, ninjaWebView) {
            private var longPressStartPoint: Point? = null
            override fun onSwipeTop() = gestureHandler.handle(config.multitouchUp)
            override fun onSwipeBottom() = gestureHandler.handle(config.multitouchDown)
            override fun onSwipeRight() = gestureHandler.handle(config.multitouchRight)
            override fun onSwipeLeft() = gestureHandler.handle(config.multitouchLeft)
            override fun onLongPressMove(motionEvent: MotionEvent) {
                super.onLongPressMove(motionEvent)
                if (longPressStartPoint == null) {
                    longPressStartPoint = Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                    return
                }
                //Log.d("touch", "onLongPress: ${motionEvent.x}, ${motionEvent.y} \n ${actionModeMenuViewModel.clickedPoint.value.x}, ${actionModeMenuViewModel.clickedPoint.value.y}")
                if (abs(motionEvent.x - (longPressStartPoint?.x ?: 0)) > ViewUnit.dpToPixel(
                        this@BrowserActivity,
                        15
                    ) ||
                    abs(motionEvent.y - (longPressStartPoint?.y ?: 0)) > ViewUnit.dpToPixel(
                        this@BrowserActivity,
                        15
                    )
                ) {
                    actionModeMenuViewModel.hide()
                    longPressStartPoint = null
                    //Log.d("touch", "onLongPress: hide")
                }
            }

            override fun onMoveDone(motionEvent: MotionEvent) {
                //Log.d("touch", "onMoveDone")
            }
        }.apply { lifecycle.addObserver(this) }

    private fun updateSavedAlbumInfo() {
        val albumControllers = browserContainer.list()
        val albumInfoList = albumControllers
            .filter { !it.isTranslatePage }
            .filter { !it.albumUrl.startsWith("data") }
            .filter {
                (it.albumUrl.isNotBlank() && it.albumUrl != BrowserUnit.URL_ABOUT_BLANK) ||
                        it.initAlbumUrl.isNotBlank()
            }
            .map { controller ->
                AlbumInfo(
                    controller.albumTitle,
                    controller.albumUrl.ifBlank { controller.initAlbumUrl },
                )
            }
        config.savedAlbumInfoList = albumInfoList
        config.currentAlbumIndex = browserContainer.indexOf(currentAlbumController)
        // fix if current album is still with null url
        if (albumInfoList.isNotEmpty() && config.currentAlbumIndex >= albumInfoList.size) {
            config.currentAlbumIndex = albumInfoList.size - 1
        }
    }

    private fun createWebViewCountString(superScript: Int, subScript: Int): String {
        if (subScript == 0 || superScript == 0) return "1"
        if (subScript >= 10) return subScript.toString()

        if (subScript == superScript) return subScript.toString()

        val superScripts = listOf("¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹")
        val subScripts = listOf("₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉")
        val separator = "⁄"
        return "${superScripts[superScript - 1]}$separator${subScripts[subScript - 1]}"
    }

    private fun updateWebViewCount() {
        val subScript = browserContainer.size()
        val superScript = browserContainer.indexOf(currentAlbumController) + 1
        val countString = createWebViewCountString(superScript, subScript)
        composeToolbarViewController.updateTabCount(countString)
        fabImageViewController.updateTabCount(countString)
    }

    override fun updateAlbum(url: String?) {
        if (url == null) return
        (currentAlbumController as NinjaWebView).loadUrl(url)
        updateTitle()

        updateSavedAlbumInfo()
    }

    private fun closeTabConfirmation(okAction: () -> Unit) {
        if (!config.confirmTabClose) {
            okAction()
        } else {
            dialogManager.showOkCancelDialog(
                messageResId = R.string.toast_close_tab,
                okAction = okAction,
            )
        }
    }

    override fun removeAlbum(albumController: AlbumController, showHome: Boolean) {
        closeTabConfirmation {
            if (config.isSaveHistoryWhenClose()) {
                addHistory(albumController.albumTitle, albumController.albumUrl)
            }

            albumViewModel.removeAlbum(albumController.album)
            val removeIndex = browserContainer.indexOf(albumController)
            val currentIndex = browserContainer.indexOf(currentAlbumController)
            browserContainer.remove(albumController)

            updateSavedAlbumInfo()
            updateWebViewCount()

            if (browserContainer.isEmpty()) {
                if (!showHome) {
                    finish()
                } else {
                    ninjaWebView.loadUrl(config.favoriteUrl)
                }
            } else {
                // only refresh album when the delete one is current one
                if (removeIndex == currentIndex) {
                    showAlbum(browserContainer[getNextAlbumIndexAfterRemoval(removeIndex)])
                }
            }
        }
    }

    private fun getNextAlbumIndexAfterRemoval(removeIndex: Int): Int =
        if (config.shouldShowNextAfterRemoveTab) min(browserContainer.size() - 1, removeIndex)
        else max(0, removeIndex - 1)

    private fun updateTitle() {
        if (!this::ninjaWebView.isInitialized) return

        if (this::ninjaWebView.isInitialized && ninjaWebView === currentAlbumController) {
            composeToolbarViewController.updateTitle(ninjaWebView.title.orEmpty())
        }
    }

    private fun scrollChange() {
        ninjaWebView.setScrollChangeListener(object : NinjaWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                ninjaWebView.updatePageInfo()

                if (::twoPaneController.isInitialized) {
                    twoPaneController.scrollChange(scrollY - oldScrollY)
                }

                if (!config.shouldHideToolbar) return

                val height =
                    floor(x = ninjaWebView.contentHeight * ninjaWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ninjaWebView.height
                val cutoff =
                    height - webViewHeight - 112 * resources.displayMetrics.density.roundToInt()
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    if (binding.appBar.visibility == VISIBLE) toggleFullscreen()
                }
            }
        })

    }

    override fun updateTitle(title: String?) = updateTitle()

    override fun addHistory(title: String, url: String) {
        lifecycleScope.launch {
            recordDb.addHistory(Record(title, url, System.currentTimeMillis()))
        }
    }

    override fun updateProgress(progress: Int) {
        progressBar.progress = progress

        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true)
            progressBar.visibility = VISIBLE
        } else { // web page loading complete
            updateRefresh(false)
            progressBar.visibility = GONE

            scrollChange()

            updateSavedAlbumInfo()
        }
    }

    private var inputTextOrUrl = mutableStateOf(TextFieldValue(""))
    private var focusRequester = FocusRequester()
    override fun focusOnInput() {
        composeToolbarViewController.hide()

        val textOrUrl = if (ninjaWebView.url?.startsWith("data:") != true) {
            val url = ninjaWebView.url.orEmpty()
            TextFieldValue(url, selection = TextRange(0, url.length))
        } else {
            TextFieldValue("")
        }

        binding.inputUrl.apply {
            inputTextOrUrl.value = textOrUrl
            isWideLayout = ViewUnit.isWideLayout(this@BrowserActivity)
            shouldReverse = !config.isToolbarOnTop
            hasCopiedText = getClipboardText().isNotEmpty()
            lifecycleScope.launch {
                binding.inputUrl.recordList.value =
                    recordDb.listEntries(config.showBookmarksInInputBar)
            }
        }


        composeToolbarViewController.hide()
        binding.appBar.visibility = INVISIBLE
        binding.contentSeparator.visibility = INVISIBLE
        binding.inputUrl.visibility = VISIBLE
        showKeyboard()
        binding.inputUrl.getFocus()
    }

    private fun getClipboardText(): String =
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

    private var isRunning = false
    private fun updateRefresh(running: Boolean) {
        if (!isRunning && running) {
            isRunning = true
        } else if (isRunning && !running) {
            isRunning = false
        }
        composeToolbarViewController.updateRefresh(isRunning)
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        fileChooserLauncher.launch(chooserIntent)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null) {
            return
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        originalOrientation = requestedOrientation
        fullscreenHolder = FrameLayout(this).apply {
            addView(
                customView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        ninjaWebView.toggleInvertColor(view!!)

        val decorView = window.decorView as FrameLayout
        decorView.addView(
            fullscreenHolder,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        customView?.keepScreenOn = true
        (currentAlbumController as View?)?.visibility = INVISIBLE
        ViewUnit.setCustomFullscreen(
            window,
            true,
            config.hideStatusbar,
            ViewUnit.isEdgeToEdgeEnabled(resources)
        )
        if (view is FrameLayout) {
            if (view.focusedChild is VideoView) {
                videoView = view.focusedChild as VideoView
                videoView?.setOnErrorListener(VideoCompletionListener())
                videoView?.setOnCompletionListener(VideoCompletionListener())
            }
        }
        customViewCallback = callback
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    override fun onHideCustomView(): Boolean {
        if (customView == null || customViewCallback == null || currentAlbumController == null) {
            return false
        }

        // fix when pressing back, the screen is whole black.
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        fullscreenHolder?.visibility = GONE
        customView?.visibility = GONE
        (window.decorView as FrameLayout).removeView(fullscreenHolder)
        customView?.keepScreenOn = false
        (currentAlbumController as View).visibility = VISIBLE
        ViewUnit.setCustomFullscreen(
            window,
            false,
            config.hideStatusbar,
            ViewUnit.isEdgeToEdgeEnabled(resources)
        )
        fullscreenHolder = null
        customView = null

        if (videoView != null) {
            videoView?.visibility = GONE
            videoView?.setOnErrorListener(null)
            videoView?.setOnCompletionListener(null)
            videoView = null
        }
        requestedOrientation = originalOrientation

        return true
    }

    private var previousKeyEvent: KeyEvent? = null
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != ACTION_DOWN) return false
        if (ninjaWebView.hitTestResult.type == HitTestResult.EDIT_TEXT_TYPE) return false

        // process dpad navigation
        if (config.useUpDownPageTurn) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    ninjaWebView.pageDownWithNoAnimation()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    ninjaWebView.pageUpWithNoAnimation()
                    return true
                }
            }
        }

        if (!config.enableViBinding) return false
        // vim bindings
        if (event.isShiftPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_J -> {
                    val controller = nextAlbumController(true) ?: return true
                    showAlbum(controller)
                }

                KeyEvent.KEYCODE_K -> {
                    val controller = nextAlbumController(false) ?: return true
                    showAlbum(controller)
                }

                KeyEvent.KEYCODE_G -> ninjaWebView.jumpToBottom()
                else -> return false
            }
        } else { // non-capital
            when (event.keyCode) {
                KeyEvent.KEYCODE_B -> openBookmarkPage()
                KeyEvent.KEYCODE_O -> {
                    if (previousKeyEvent?.keyCode == KeyEvent.KEYCODE_V) {
                        decreaseFontSize()
                        previousKeyEvent = null
                    } else {
                        focusOnInput()
                    }
                }

                KeyEvent.KEYCODE_J -> ninjaWebView.pageDownWithNoAnimation()
                KeyEvent.KEYCODE_K -> ninjaWebView.pageUpWithNoAnimation()
                KeyEvent.KEYCODE_H -> ninjaWebView.goBack()
                KeyEvent.KEYCODE_L -> ninjaWebView.goForward()
                KeyEvent.KEYCODE_R -> showTranslation()
                KeyEvent.KEYCODE_D -> removeAlbum()
                KeyEvent.KEYCODE_T -> {
                    addAlbum(getString(R.string.app_name), "")
                    focusOnInput()
                }

                KeyEvent.KEYCODE_SLASH -> showSearchPanel()
                KeyEvent.KEYCODE_G -> {
                    previousKeyEvent = when {
                        previousKeyEvent == null -> event
                        previousKeyEvent?.keyCode == KeyEvent.KEYCODE_G -> {
                            // gg
                            ninjaWebView.jumpToTop()
                            null
                        }

                        else -> null
                    }
                }

                KeyEvent.KEYCODE_V -> {
                    previousKeyEvent = if (previousKeyEvent == null) event else null
                }

                KeyEvent.KEYCODE_I -> {
                    if (previousKeyEvent?.keyCode == KeyEvent.KEYCODE_V) {
                        increaseFontSize()
                        previousKeyEvent = null
                    }
                }

                KeyEvent.KEYCODE_F -> toggleFullscreen()

                else -> return false
            }
        }
        return true
    }

    override fun loadInSecondPane(url: String): Boolean =
        if (config.twoPanelLinkHere &&
            isTwoPaneControllerInitialized() &&
            twoPaneController.isSecondPaneDisplayed()
        ) {
            toggleSplitScreen(url)
            true
        } else {
            false
        }

    private fun confirmAdSiteAddition(url: String) {
        val host = Uri.parse(url).host.orEmpty()
        if (config.adSites.contains(host)) {
            confirmRemoveAdSite(host)
        } else {
            lifecycleScope.launch {
                val domain = TextInputDialog(
                    this@BrowserActivity,
                    "Ad domain to be blocked",
                    "",
                    host,
                ).show().orEmpty()

                if (domain.isNotBlank()) {
                    config.adSites = config.adSites.apply { add(domain) }
                    ninjaWebView.reload()
                }
            }
        }
    }

    private fun confirmRemoveAdSite(url: String) {
        dialogManager.showOkCancelDialog(
            title = "remove this url from blacklist?",
            okAction = {
                config.adSites = config.adSites.apply { remove(url) }
                ninjaWebView.reload()
            }
        )
    }

    private var motionEvent: MotionEvent? = null
    private var point: Point = Point(0, 0)
    override fun onLongPress(message: Message, event: MotionEvent?) {
        //Log.d("touch", "onLongPress")
        if (ninjaWebView.isSelectingText) return

        motionEvent = event
        point = Point(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
        val url = BrowserUnit.getWebViewLinkUrl(ninjaWebView, message)
        if (url.isBlank()) {
        } else {
            // case: image or link
            val linkImageUrl = BrowserUnit.getWebViewLinkImageUrl(ninjaWebView, message)
            BrowserUnit.getWebViewLinkTitle(ninjaWebView) { linkTitle ->
                val titleText = linkTitle.ifBlank { url }.toString()
                ContextMenuDialogFragment(
                    url,
                    linkImageUrl.isNotBlank(),
                    config.imageApiKey.isNotBlank(),
                    motionEvent!!.toRawPoint(),
                ) {
                    this@BrowserActivity.handleContextMenuItem(it, titleText, url, linkImageUrl)
                }.show(supportFragmentManager, "contextMenu")
            }
        }
    }

    private fun handleContextMenuItem(
        contextMenuItemType: ContextMenuItemType,
        title: String,
        url: String,
        imageUrl: String,
    ) {
        when (contextMenuItemType) {
            ContextMenuItemType.NewTabForeground -> addAlbum(title, url)
            ContextMenuItemType.NewTabBackground -> addAlbum(title, url, false)
            ContextMenuItemType.ShareLink -> {
                if (prepareRecord()) NinjaToast.show(this, getString(R.string.toast_share_failed))
                else IntentUnit.share(this, title, url)
            }

            ContextMenuItemType.CopyLink -> ShareUtil.copyToClipboard(
                this,
                BrowserUnit.stripUrlQuery(url)
            )

            ContextMenuItemType.SelectText -> ninjaWebView.post {
                //actionModeMenuViewModel.updateClickedPoint(motionEvent!!.toPoint())
                ninjaWebView.selectLinkText(point)
            }

            ContextMenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                this,
                url,
                getString(R.string.menu_open_with)
            )

            ContextMenuItemType.SaveBookmark -> saveBookmark(url, title)
            ContextMenuItemType.SplitScreen -> toggleSplitScreen(url)
            ContextMenuItemType.AdBlock -> confirmAdSiteAddition(imageUrl)

            ContextMenuItemType.TranslateImage -> translateImage(imageUrl)
            ContextMenuItemType.SaveAs -> {
                if (url.startsWith("data:image")) {
                    saveFile(url)
                } else {
                    if (imageUrl.isNotBlank()) {
                        dialogManager.showSaveFileDialog(url = imageUrl, saveFile = this::saveFile)
                    } else {
                        dialogManager.showSaveFileDialog(url = url, saveFile = this::saveFile)
                    }
                }
            }
        }
    }

    private fun translateWebView() {
        lifecycleScope.launch {
            val base64String = translationViewModel.translateWebView(
                ninjaWebView,
                config.sourceLanguage,
                config.translationLanguage,
            )
            if (base64String != null) {
                val translatedImageHtml = HelperUnit.loadAssetFileToString(
                    this@BrowserActivity, "translated_image.html"
                ).replace("%%", base64String)
                if (config.showTranslatedImageToSecondPanel) {
                    maybeInitTwoPaneController()
                    twoPaneController.showSecondPaneWithData(translatedImageHtml)
                } else {
                    addAlbum()
                    ninjaWebView.isTranslatePage = true
                    ninjaWebView.loadData(translatedImageHtml, "text/html", "utf-8")
                }
            } else {
                NinjaToast.show(this@BrowserActivity, "Failed to translate image")
            }
        }
    }

    private fun translateImage(url: String) {
        lifecycleScope.launch {
            val base64String = translationViewModel.translateImage(
                ninjaWebView.url.orEmpty(),
                url,
                TranslationLanguage.KO,
                config.translationLanguage,
            )
            if (base64String != null) {
                //addAlbum(url = "data:image/png;base64,$it")
                val translatedImageHtml = HelperUnit.loadAssetFileToString(
                    this@BrowserActivity, "translated_image.html"
                ).replace("%%", base64String)
                if (config.showTranslatedImageToSecondPanel) {
                    maybeInitTwoPaneController()
                    twoPaneController.showSecondPaneWithData(translatedImageHtml)
                } else {
                    addAlbum()
                    ninjaWebView.isTranslatePage = true
                    ninjaWebView.loadData(translatedImageHtml, "text/html", "utf-8")
                }
            } else {
                NinjaToast.show(this@BrowserActivity, "Failed to translate image")
            }
        }
    }

    private fun saveFile(url: String, fileName: String = "") {
        // handle data url case
        if (url.startsWith("data:image")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BrowserUnit.saveImageFromUrl(url, saveImageFilePickerLauncher)
            } else {
                NinjaToast.show(this, "Not supported dataUrl")
            }
            return
        }

        if (HelperUnit.needGrantStoragePermission(this)) {
            return
        }

        val source = Uri.parse(url)
        val request = Request(source).apply {
            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
            setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val dm = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
        dm.enqueue(request)
        hideKeyboard()
    }

    @SuppressLint("RestrictedApi")
    private fun showToolbar() {
        if (searchOnSite) return

        showStatusBar()
        fabImageViewController.hide()
        binding.mainSearchPanel.visibility = INVISIBLE
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        binding.inputUrl.visibility = INVISIBLE
        composeToolbarViewController.show()
        hideKeyboard()
    }

    override fun toggleFullscreen() {
        if (searchOnSite) return

        if (binding.appBar.visibility == VISIBLE) {
            if (config.fabPosition != FabPosition.NotShow) {
                fabImageViewController.show()
            }
            binding.mainSearchPanel.visibility = INVISIBLE
            binding.appBar.visibility = GONE
            binding.contentSeparator.visibility = GONE
            hideStatusBar()
        } else {
            showToolbar()
        }
    }

    private fun hideSearchPanel() {
        if (this::ninjaWebView.isInitialized) {
            ninjaWebView.clearMatches()
        }
        searchOnSite = false
        hideKeyboard()
        showToolbar()
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            if (ViewUnit.isEdgeToEdgeEnabled(resources))
                window.insetsController?.hide(WindowInsets.Type.navigationBars())
            binding.root.setPadding(0, 0, 0, 0)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun showStatusBar() {
        if (config.hideStatusbar) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    override fun showSearchPanel() {
        searchOnSite = true
        fabImageViewController.hide()
        binding.mainSearchPanel.visibility = VISIBLE
        binding.mainSearchPanel.getFocus()
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        showKeyboard()
    }

    override fun showSaveEpubDialog() = dialogManager.showSaveEpubDialog { uri ->
        if (uri == null) {
            epubManager.showEpubFilePicker(writeEpubFilePickerLauncher)
        } else {
            saveEpub(uri)
        }
    }

    private fun readArticle() {
        if (config.useOpenAiTts && config.gptApiKey.isNotBlank()) {
            if (ttsViewModel.isSpeaking()) {
                ttsViewModel.stop()
            } else {
                lifecycleScope.launch {
                    ttsViewModel.readText(this@BrowserActivity, ninjaWebView.getRawText());
                    //ttsViewModel.readText(this@BrowserActivity, "one. two. three. four. five. six.")
                }
            }
            return
        }

        if (Build.MODEL.startsWith("Pixel 8")) {
            lifecycleScope.launch {
                IntentUnit.tts(this@BrowserActivity, ninjaWebView.getRawText())
            }
            return
        }


        lifecycleScope.launch {
            ttsManager.readText(ninjaWebView.getRawText())
            delay(2000)
            composeToolbarViewController.updateIcons()

            if (ttsManager.isSpeaking()) {
                speakingCompleteDetector()
            }
        }
    }

    private val menuActionHandler: MenuActionHandler by lazy { MenuActionHandler(this) }

    override fun showMenuDialog() =
        MenuDialogFragment(
            ninjaWebView.url.orEmpty(),
            { menuActionHandler.handle(it, ninjaWebView) },
            { menuActionHandler.handleLongClick(it, ninjaWebView) }
        ).show(supportFragmentManager, "menu_dialog")

    override fun showWebArchiveFilePicker() {
        val fileName = "${ninjaWebView.title}.mht"
        BrowserUnit.createFilePicker(createWebArchivePickerLauncher, fileName)
    }

    override fun toggleTtsRead() {
        if (ttsManager.isSpeaking()) {
            ttsManager.stopReading()
        } else {
            readArticle()
        }
    }

    override fun showTtsLanguageDialog() {
        TtsLanguageDialog(this).show(ttsManager.getAvailableLanguages()) {
            config.ttsLocale = it
        }
    }

    override fun removeAlbum() {
        currentAlbumController?.let { removeAlbum(it) }
    }

    private fun speakingCompleteDetector() {
        lifecycleScope.launch {
            while (ttsManager.isSpeaking()) {
                delay(1000L)
                if (!ttsManager.isSpeaking()) {
                    composeToolbarViewController.updateIcons()
                }
            }
        }
    }

    override fun toggleSplitScreen(url: String?) {
        maybeInitTwoPaneController()
        if (twoPaneController.isSecondPaneDisplayed() && url == null) {
            twoPaneController.hideSecondPane()
            splitSearchViewModel.reset()
            return
        }

        twoPaneController.showSecondPaneWithUrl(url ?: ninjaWebView.url.orEmpty())
    }

    private fun nextAlbumController(next: Boolean): AlbumController? {
        if (browserContainer.size() <= 1) {
            return currentAlbumController
        }

        val list = browserContainer.list()
        var index = list.indexOf(currentAlbumController)
        if (next) {
            index++
            if (index >= list.size) {
                return list.first()
            }
        } else {
            index--
            if (index < 0) {
                return list.last()
            }
        }
        return list[index]
    }

    private fun showKeyboard() = ViewUnit.showKeyboard(this)

    private fun hideKeyboard() = ViewUnit.hideKeyboard(this)

    // - action mode handling
    override fun onActionModeStarted(mode: ActionMode) {
        //Log.d("touch", "onActionModeStarted")

        // check isSendingLink
        if (remoteConnViewModel.isSendingTextSearch &&
            !isTextEditMode(mode.menu)
        ) {
            mode.hide(1000000)
            mode.menu.clear()
            mode.finish()

            lifecycleScope.launch {
                val keyword = ninjaWebView.getSelectedText()
                remoteConnViewModel.sendTextSearch(externalSearchViewModel.generateSearchUrl(keyword))
            }
            return
        }

        if (!config.showDefaultActionMenu && !isTextEditMode(mode.menu) && isInSplitSearchMode()) {
            mode.hide(1000000)
            mode.menu.clear()

            lifecycleScope.launch {
                toggleSplitScreen(splitSearchViewModel.getUrl(ninjaWebView.getSelectedText()))
            }

            mode.finish()
            return
        }

        if (!actionModeMenuViewModel.isInActionMode()) {
            actionModeMenuViewModel.updateActionMode(mode)

            if (!config.showDefaultActionMenu && !isTextEditMode(mode.menu)) {
                mode.hide(1000000)
                mode.menu.clear()
                mode.finish()

                lifecycleScope.launch {
                    actionModeMenuViewModel.updateSelectedText(ninjaWebView.getSelectedText())
                    actionModeMenuViewModel.showActionModeView(
                        this@BrowserActivity,
                        binding.root
                    ) {
                        ninjaWebView.removeTextSelection()
                    }
                }
            }
        }

        if (!config.showDefaultActionMenu && !isTextEditMode(mode.menu)) {
            mode.menu.clear()
        }

        super.onActionModeStarted(mode)
    }


    private fun isTextEditMode(menu: Menu): Boolean {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.title == getString(android.R.string.paste)) {
                return true
            }
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        actionModeMenuViewModel.finish()
        if (!config.continueMedia && !isMeetPipCriteria()) {
            if (this::ninjaWebView.isInitialized) {
                ninjaWebView.pauseTimers()
            }
        }
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        //Log.d("touch", "onActionModeFinished")
        super.onActionModeFinished(mode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mode?.hide(1000000)
        }
        actionModeMenuViewModel.updateActionMode(null)
    }

    // - action mode handling

    companion object {
        private const val TAG = "BrowserActivity"
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
    }
}

//private fun MotionEvent.toPoint(): Point = Point(x.toInt(), y.toInt())
private fun MotionEvent.toRawPoint(): Point = Point(rawX.toInt(), rawY.toInt())
