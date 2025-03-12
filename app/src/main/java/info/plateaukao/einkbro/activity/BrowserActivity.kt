package info.plateaukao.einkbro.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.Request
import android.app.PictureInPictureParams
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.NewTabBehavior
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.service.ClearService
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.BrowserUnit.createDownloadReceiver
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.HelperUnit.toNormalScheme
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.pruneWebTitle
import info.plateaukao.einkbro.unit.toRawPoint
import info.plateaukao.einkbro.util.Constants.Companion.ACTION_DICT
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MultitouchListener
import info.plateaukao.einkbro.view.SwipeTouchListener
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.SendLinkDialog
import info.plateaukao.einkbro.view.dialog.ShortcutEditDialog
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
import info.plateaukao.einkbro.view.dialog.compose.ActionModeView
import info.plateaukao.einkbro.view.dialog.compose.BookmarksDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType
import info.plateaukao.einkbro.view.dialog.compose.FastToggleDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontBoldnessDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.LanguageSettingDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.MenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ReaderFontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ShowEditGptActionDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TranslateDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TranslationConfigDlgFragment
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.view.handlers.GestureHandler
import info.plateaukao.einkbro.view.handlers.MenuActionHandler
import info.plateaukao.einkbro.view.handlers.ToolbarActionHandler
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.view.viewControllers.TouchAreaViewController
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.DeeplTranslate
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.GoogleTranslate
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Gpt
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.HighlightText
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Idle
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Naver
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.Papago
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.SelectParagraph
import info.plateaukao.einkbro.viewmodel.ActionModeMenuState.SelectSentence
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
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel
import kotlinx.coroutines.Dispatchers
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
    protected lateinit var ebWebView: EBWebView
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
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }

    private val ttsViewModel: TtsViewModel by viewModels()

    private val translationViewModel: TranslationViewModel by viewModels()

    private val splitSearchViewModel: SplitSearchViewModel by viewModels()

    private val remoteConnViewModel: RemoteConnViewModel by viewModels()

    private val externalSearchViewModel: ExternalSearchViewModel by viewModels()

    private fun prepareRecord(): Boolean {
        val webView = currentAlbumController as EBWebView
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
            ttsViewModel,
            { toolbarActionHandler.handleClick(it) },
            { toolbarActionHandler.handleLongClick(it) },
            onTabClick = { it.showOrJumpToTop() },
            onTabLongClick = { it.remove() },
        )
    }

    override fun newATab() {
        // fix: https://github.com/plateaukao/einkbro/issues/343
        if (searchOnSite) {
            hideSearchPanel()
        }

        when (config.newTabBehavior) {
            NewTabBehavior.START_INPUT -> {
                addAlbum(getString(R.string.app_name), "")
                focusOnInput()
            }

            NewTabBehavior.SHOW_HOME -> addAlbum("", config.favoriteUrl)
            NewTabBehavior.SHOW_RECENT_BOOKMARKS -> {
                addAlbum("", "")
                BrowserUnit.loadRecentlyUsedBookmarks(ebWebView)
            }
        }
    }

    override fun duplicateTab() {
        val webView = currentAlbumController as EBWebView
        val title = webView.title.orEmpty()
        val url = webView.url ?: return
        addAlbum(title, url)
    }

    override fun refreshAction() {
        if (ebWebView.isLoadFinish && ebWebView.url?.isNotEmpty() == true) {
            ebWebView.reload()
        } else {
            ebWebView.stopLoading()
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
    private lateinit var openEpubFilePickerLauncher: ActivityResultLauncher<Intent>

    // Classes
    private inner class VideoCompletionListener : OnCompletionListener,
        MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

        override fun onCompletion(mp: MediaPlayer) {
            onHideCustomView()
        }
    }

    private val adFilter: AdFilter = AdFilter.get()
    private val filterViewModel: FilterViewModel = adFilter.viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // workaround for crash issue
        // Caused by java.lang.NoSuchMethodException:
        super.onCreate(null)

        //android.os.Debug.waitForDebugger()

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
        ViewUnit.updateAppbarPosition(binding)
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
        initTtsViewModel()

        if (config.hideStatusbar) {
            hideStatusBar()
        }

        handleWindowInsets()
        listenKeyboardShowHide()

        // post delay to update filter list
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission()
        } else {
            binding.root.postDelayed({
                checkAdBlockerList()
            }, 1000)
        }
    }

    private fun checkAdBlockerList() {
        if (!adFilter.hasInstallation) {
            val map = mapOf(
                "AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt",
//                "EasyPrivacy Lite" to "https://filters.adtidy.org/extension/chromium/filters/118_optimized.txt",
//                "AdGuard Tracking Protection" to "https://filters.adtidy.org/extension/chromium/filters/3.txt",
//                "AdGuard Annoyances" to "https://filters.adtidy.org/extension/chromium/filters/14.txt",
//                "AdGuard Chinese" to "https://filters.adtidy.org/extension/chromium/filters/224.txt",
//                "NoCoin Filter List" to "https://filters.adtidy.org/extension/chromium/filters/242.txt"
            )
            for ((key, value) in map) {
                filterViewModel.addFilter(key, value)
            }
            val filters = filterViewModel.filters.value
            for ((key, _) in filters) {
                filterViewModel.download(key)
            }
        }
    }

    private fun requestNotificationPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    checkAdBlockerList()
                } else {
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun initTtsViewModel() {
        lifecycleScope.launch {
            ttsViewModel.readingState.collect { _ ->
                composeToolbarViewController.updateIcons()
            }
        }
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
                height = 40.dp.value.toInt()
                textSize = 10.sp.value
                gravity = Gravity.CENTER
                background = getDrawable(R.drawable.background_with_border)
                text = action.title.take(2).uppercase(Locale.getDefault())
                setOnClickListener {
                    externalSearchViewModel.currentSearchAction = action
                    ebWebView.loadUrl(
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
        touchController = TouchAreaViewController(binding.activityMainContent, this)
    }

    private fun initActionModeViewModel() {
        lifecycleScope.launch {
            actionModeMenuViewModel.actionModeMenuState.collect { state ->
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

                        lifecycleScope.launch {
                            updateTranslationInput()
                            showTranslationDialog()

                            actionModeMenuViewModel.finish()
                        }
                    }

                    is ActionModeMenuState.ReadFromHere -> readFromThisSentence()

                    is Gpt -> {
                        val gptAction = config.gptActionList[state.gptActionIndex]
                        lifecycleScope.launch {
                            updateTranslationInput()
                            if (translationViewModel.hasOpenAiApiKey()) {
                                translationViewModel.setupGptAction(gptAction)
                                translationViewModel.url = getFocusedWebView().url.orEmpty()

                                showTranslationDialog()
                            } else {
                                EBToast.show(this@BrowserActivity, R.string.gpt_api_key_not_set)
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

                    is SelectSentence -> getFocusedWebView().selectSentence(longPressPoint)
                    is SelectParagraph -> getFocusedWebView().selectParagraph(longPressPoint)

                    Idle -> Unit
                }
            }
        }

        lifecycleScope.launch {
            actionModeMenuViewModel.clickedPoint.collect { point ->
                val view = actionModeView ?: return@collect
                ViewUnit.updateViewPosition(view, point)
            }
        }

        lifecycleScope.launch {
            actionModeMenuViewModel.shouldShow.collect { shouldShow ->
                val view = actionModeView ?: return@collect
                if (shouldShow) {
                    val point = actionModeMenuViewModel.clickedPoint.value
                    // when it's first time to show action mode view
                    // need to wait until width and height is available
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
        lifecycleScope.launch {
            val selectedSentence = ebWebView.getSelectedText()
            val fullText = ebWebView.getRawText()
            // read from selected sentence to the end of the article
            val startIndex = fullText.indexOf(selectedSentence)
            ttsViewModel.readArticle(fullText.substring(startIndex))
        }
    }

    private suspend fun updateTranslationInput() {
        // need to handle where data is from: ebWebView or twoPaneController.getSecondWebView()
        with(translationViewModel) {
            updateInputMessage(actionModeMenuViewModel.selectedText.value)
            updateMessageWithContext(getFocusedWebView().getSelectedTextWithContext())
            url = getFocusedWebView().url.orEmpty()
        }
    }

    private suspend fun highlightText(highlightStyle: HighlightStyle) {
        val focusedWebView = getFocusedWebView()
        // work on UI first
        focusedWebView.highlightTextSelection(highlightStyle)

        // work on db saving
        val url = focusedWebView.url.orEmpty()
        val title = focusedWebView.title.orEmpty()
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
                ebWebView.clearTranslationElements()
                translateByParagraph(ebWebView.translateApi)
            }
        }
        languageLabelView?.setOnLongClickListener {
            languageLabelView?.visibility = GONE
            true
        }
    }

    override fun isAtTop(): Boolean = ebWebView.isAtTop()
    override fun jumpToTop() = ebWebView.jumpToTop()
    override fun jumpToBottom() = ebWebView.jumpToBottom()
    override fun pageDown() = ebWebView.pageDownWithNoAnimation()
    override fun pageUp() = ebWebView.pageUpWithNoAnimation()
    override fun toggleReaderMode() = ebWebView.toggleReaderMode()
    override fun toggleVerticalRead() = ebWebView.toggleVerticalRead()
    override fun updatePageInfo(info: String) = composeToolbarViewController.updatePageInfo(info)

    override fun sendPageUpKey() = ebWebView.sendPageUpKey()
    override fun sendPageDownKey() = ebWebView.sendPageDownKey()
    override fun sendLeftKey() {
        ebWebView.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
    }

    override fun sendRightKey() {
        ebWebView.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
    }

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
                PocketShareState.Failed -> EBToast.showShort(this@BrowserActivity, "Failed")
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
            TranslationMode.DEEPL_BY_PARAGRAPH -> translateByParagraph(TRANSLATE_API.DEEPL)

            TranslationMode.PAPAGO_TRANSLATE_BY_SCREEN -> translateWebView()
            TranslationMode.GOOGLE_IN_PLACE -> ebWebView.addGoogleTranslation()
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
            } else if (translateApi == TRANSLATE_API.PAPAGO) {
                translateByParagraph(TRANSLATE_API.PAPAGO)
            } else if (translateApi == TRANSLATE_API.DEEPL) {
                translateByParagraph(TRANSLATE_API.DEEPL)
            }
        }
            .show(supportFragmentManager, "LanguageSettingDialog")
    }

    override fun toggleTouchPagination() = toggleTouchTurnPageFeature()

    override fun showFontBoldnessDialog() {
        FontBoldnessDialogFragment(
            config.fontBoldness,
            okAction = { changedBoldness ->
                config.fontBoldness = changedBoldness
                ebWebView.applyFontBoldness()
            }
        ).show(supportFragmentManager, "FontBoldnessDialog")
    }

    override fun toggleTextSearch() {
        remoteConnViewModel.toggleTextSearch()
    }

    override fun sendToRemote(text: String) {
        if (remoteConnViewModel.isSendingTextSearch) {
            remoteConnViewModel.toggleTextSearch()
            EBToast.show(this, R.string.send_to_remote_terminate)
            return
        }

        SendLinkDialog(this, lifecycleScope).show(text)
    }

    override fun summarizeContent() {
        if (translationViewModel.hasOpenAiApiKey()) {
            lifecycleScope.launch {
                translationViewModel.url = ebWebView.url.orEmpty()
                val isSuccess = translationViewModel.setupTextSummary(ebWebView.getRawText())

                if (!isSuccess) {
                    EBToast.show(this@BrowserActivity, R.string.gpt_api_key_not_set)
                    return@launch
                }

                showTranslationDialog()
            }
        }
    }

    private fun showTranslationDialog() {
        TranslateDialogFragment(
            translationViewModel,
            externalSearchWebView,
            actionModeMenuViewModel.clickedPoint.value,
        )
            .show(supportFragmentManager, "contextMenu")
    }

    override fun invertColors() {
        val hasInvertedColor = config.toggleInvertedColor(ebWebView.url.orEmpty())
        ViewUnit.invertColor(ebWebView, hasInvertedColor)
    }

    override fun shareLink() {
        IntentUnit.share(this, ebWebView.title, ebWebView.url)
    }

    override fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float) {
        //Log.d("touch", "updateSelectionRect: $left, $top, $right, $bottom")
        // 10 for the selection indicator height
        val newPoint = Point(
            ViewUnit.dpToPixel(right.toInt()).toInt(),
            ViewUnit.dpToPixel(bottom.toInt() + 16).toInt()
        )
        if (abs(newPoint.x - actionModeMenuViewModel.clickedPoint.value.x) > ViewUnit.dpToPixel(15) ||
            abs(newPoint.y - actionModeMenuViewModel.clickedPoint.value.y) > ViewUnit.dpToPixel(15)
        ) {
            actionModeView?.visibility = INVISIBLE
        }
        actionModeMenuViewModel.updateClickedPoint(newPoint)

        // update the long press point so that it can be used for selecting sentence
        longPressPoint = Point(
            ViewUnit.dpToPixel(left.toInt() - 1).toInt(),
            ViewUnit.dpToPixel(top.toInt() + 1).toInt()
        )
    }

    override fun toggleReceiveTextSearch() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
        } else {
            remoteConnViewModel.toggleReceiveLink { ebWebView.loadUrl(it) }
        }
    }

    override fun toggleReceiveLink() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
            EBToast.show(this, R.string.receive_link_terminate)
            return
        }

        ReceiveDataDialog(this@BrowserActivity, lifecycleScope).show {
            ShareUtil.startReceiving(lifecycleScope) { url ->
                if (url.isNotBlank()) {
                    ebWebView.loadUrl(url)
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
        openEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(this) { handleEpubUri(it) }
    }

    private fun handleEpubUri(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val uri = result.data?.data ?: return
        HelperUnit.openFile(this, uri)
    }

    private fun handleFontSelectionResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        BrowserUnit.handleFontSelectionResult(this, result, ebWebView.shouldUseReaderFont())
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
            ebWebView,
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
        ebWebView.saveWebArchive(filePath, false) {
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
            if (!ebWebView.shouldUseReaderFont()) {
                ebWebView.reload()
            } else {
                ebWebView.updateCssStyle()
            }
            config.customFontChanged = false
        }
        if (!config.continueMedia) {
            if (this::ebWebView.isInitialized) {
                ebWebView.resumeTimers()
            }
        }
    }

    override fun onDestroy() {
        ttsViewModel.reset()

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
                if (config.useUpDownPageTurn) ebWebView.pageDownWithNoAnimation()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (config.useUpDownPageTurn) ebWebView.pageUpWithNoAnimation()
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
            if (ebWebView.isVerticalRead) {
                ebWebView.pageUpWithNoAnimation()
            } else {
                ebWebView.pageDownWithNoAnimation()
            }
            true
        } else {
            false
        }
    }

    private fun handleVolumeUpKey(): Boolean {
        return if (config.volumePageTurn) {
            if (ebWebView.isVerticalRead) {
                ebWebView.pageDownWithNoAnimation()
            } else {
                ebWebView.pageUpWithNoAnimation()
            }
            true
        } else {
            false
        }
    }

    override fun handleBackKey() {
        ViewUnit.hideKeyboard(this)
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
            if (!ebWebView.isTranslatePage && ebWebView.canGoBack()) {
                ebWebView.goBack()
            } else {
                if (config.closeTabWhenNoMoreBackHistory) {
                    removeAlbum()
                } else {
                    EBToast.show(this, getString(R.string.no_previous_page))
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
                if (ebWebView.isAtTop()) {
                    ebWebView.reload()
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
        ebWebView = controller as EBWebView

        updateTitle()
        ebWebView.updatePageInfo()

        // when showing a new album, should turn off externalSearch button visibility
        externalSearchViewModel.setButtonVisibility(false)
        runOnUiThread {
            composeToolbarViewController.updateFocusIndex(
                albumViewModel.albums.value.indexOfFirst { it.isActivated }
            )
        }
        updateLanguageLabel()
    }

    private fun updateLanguageLabel() {
        languageLabelView?.visibility =
            if (ebWebView.isTranslatePage || ebWebView.isTranslateByParagraph) VISIBLE else GONE
    }

    private fun openCustomFontPicker() = BrowserUnit.openFontFilePicker(customFontResultLauncher)

    override fun showOverview() = overviewDialogController.show()

    override fun hideOverview() = overviewDialogController.hide()

    override fun rotateScreen() = IntentUnit.rotateScreen(this)

    override fun saveBookmark(url: String?, title: String?) {
        val currentUrl = url ?: ebWebView.url ?: return
        var nonNullTitle = title ?: HelperUnit.secString(ebWebView.title)
        try {
            lifecycleScope.launch {
                BookmarkEditDialog(
                    this@BrowserActivity,
                    bookmarkViewModel,
                    Bookmark(
                        nonNullTitle.pruneWebTitle(),
                        currentUrl, order = if (ViewUnit.isWideLayout(this@BrowserActivity)) 999 else 0),
                    {
                        handleBookmarkSync(true)
                        ViewUnit.hideKeyboard(this@BrowserActivity)
                        EBToast.show(this@BrowserActivity, R.string.toast_edit_successful)
                    },
                    { ViewUnit.hideKeyboard(this@BrowserActivity) }
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            EBToast.show(this, R.string.toast_error)
        }
    }

    override fun createShortcut() {
        val currentUrl = ebWebView.url ?: return
        ShortcutEditDialog(
            this@BrowserActivity,
            HelperUnit.secString(ebWebView.title),
            currentUrl,
            ebWebView.favicon,
            {
                ViewUnit.hideKeyboard(this)
                EBToast.show(this@BrowserActivity, R.string.toast_edit_successful)
            },
            { ViewUnit.hideKeyboard(this) }
        ).show()
    }

    override fun toggleTouchTurnPageFeature() = config::enableTouchTurn.toggle()

    override fun toggleSwitchTouchAreaAction() = config::switchTouchAreaAction.toggle()

    private fun updateTouchView() = composeToolbarViewController.updateIcons()

    // Methods
    override fun showFontSizeChangeDialog() {
        if (ebWebView.shouldUseReaderFont()) {
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
        if (ebWebView.shouldUseReaderFont()) {
            config.readerFontSize = size
        } else {
            config.fontSize = size
        }
    }

    override fun increaseFontSize() {
        val fontSize =
            if (ebWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
        changeFontSize(fontSize + 20)
    }

    override fun decreaseFontSize() {
        val fontSize =
            if (ebWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
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
                { if (ebWebView.isReaderModeOn) ebWebView.toggleReaderMode() },
                { url -> ebWebView.loadUrl(url) },
                { api, webView -> translateByParagraph(api, webView) },
                this::translateWebView
            )
        }
    }

    private fun translateByParagraph(
        translateApi: TRANSLATE_API,
        webView: EBWebView = ebWebView,
    ) {
        translateByParagraphInPlace(translateApi, webView)
    }

    private fun translateByParagraphInPlace(
        translateApi: TRANSLATE_API,
        webView: EBWebView = ebWebView,
    ) {
        lifecycleScope.launch {
            webView.translateApi = translateApi
            webView.translateByParagraphInPlace()
            if (webView == ebWebView) {
                languageLabelView?.visibility = VISIBLE
            }
        }
    }

//    private fun translateByParagraphInReaderMode(translateApi: TRANSLATE_API) {
//        lifecycleScope.launch {
//            val currentUrl = ebWebView.url
//
//            // assume it's current one
//            val translateModeWebView = if (ebWebView.isTranslatePage) {
//                ebWebView
//            } else {
//                // get html from original WebView
//                val htmlCache = ebWebView.getRawReaderHtml()
//                // create a new WebView
//                addAlbum("", "")
//                // set it to translate mode
//                ebWebView.isTranslatePage = true
//                ebWebView.translateApi = translateApi
//                // set its raw html to be the same as original WebView
//                ebWebView.rawHtmlCache = htmlCache
//                // show the language label
//                languageLabelView?.visibility = VISIBLE
//                ebWebView
//            }
//
//            val translatedHtml = translationViewModel
//                .translateByParagraph(translateModeWebView.rawHtmlCache ?: return@launch)
//            if (translateModeWebView.isAttachedToWindow) {
//                translateModeWebView.loadDataWithBaseURL(
//                    if (!ebWebView.isPlainText) currentUrl else null,
//                    translatedHtml,
//                    "text/html",
//                    "utf-8",
//                    null
//                )
//            }
//        }
//    }

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    override fun showTranslation(webView: EBWebView?) {
        maybeInitTwoPaneController()

        lifecycleScope.launch(Dispatchers.Main) {
            twoPaneController.showTranslation(webView ?: ebWebView)
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
                    if (filename?.endsWith(".srt") == true ||
                        mimeType.equals("application/x-subrip")
                    ) {
                        // srt
                        addAlbum()
                        val stringList =
                            HelperUnit.readContentAsStringList(contentResolver, viewUri)
                        val htmlContent = HelperUnit.srtToHtml(stringList)
                        ebWebView.isPlainText = true
                        ebWebView.rawHtmlCache = htmlContent
                        ebWebView.loadData(htmlContent, "text/html", "utf-8")

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
                    ebWebView.loadUrl(searchedKeyword)
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
                    ebWebView.loadUrl(url)
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
                    ebWebView.loadUrl(url)
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
                    ebWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
                // set minimize button visible
                externalSearchViewModel.setButtonVisibility(true)
            }

            ACTION_READ_ALOUD -> readArticle()

            null -> {
                if (browserContainer.isEmpty()) {
                    initSavedTabs { addAlbum() }
                } else {
                    return
                }
            }

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

    override fun showTouchAreaDialog() = TouchAreaDialogFragment(ebWebView.url.orEmpty())
        .show(supportFragmentManager, "TouchAreaDialog")

    override fun showTranslationConfigDialog(translateDirectly: Boolean) {
        maybeInitTwoPaneController()
        TranslationConfigDlgFragment(ebWebView.url.orEmpty()) { shouldTranslate ->
            if (shouldTranslate) {
                translate(config.translationMode)
            } else {
                ebWebView.reload()
            }
        }
            .show(supportFragmentManager, "TranslationConfigDialog")
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(
                LocaleManager.setLocale(newBase, config.uiLocaleLanguage)
            )
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private fun updateLocale(context: Context, languageCode: String) {
        val newContext = LocaleManager.setLocale(context, languageCode)
        val intent = Intent(newContext, BrowserActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
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
                        ebWebView.reload()
                    } else {
                        ebWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_READER_FONT_TYPE -> {
                    if (config.readerFontType == FontType.SYSTEM_DEFAULT) {
                        ebWebView.reload()
                    } else {
                        ebWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_FONT_SIZE -> {
                    ebWebView.settings.textZoom = config.fontSize
                }

                ConfigManager.K_READER_FONT_SIZE -> {
                    if (ebWebView.shouldUseReaderFont()) {
                        ebWebView.settings.textZoom = config.readerFontSize
                    }
                }

                ConfigManager.K_BOLD_FONT -> {
                    composeToolbarViewController.updateIcons()
                    if (config.boldFontStyle) {
                        ebWebView.updateCssStyle()
                    } else {
                        ebWebView.reload()
                    }
                }

                ConfigManager.K_BLACK_FONT -> {
                    composeToolbarViewController.updateIcons()
                    if (config.blackFontStyle) {
                        ebWebView.updateCssStyle()
                    } else {
                        ebWebView.reload()
                    }
                }

                ConfigManager.K_ENABLE_IMAGE_ADJUSTMENT -> ebWebView.reload()

                ConfigManager.K_CUSTOM_FONT -> {
                    if (config.fontType == FontType.CUSTOM) {
                        ebWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_READER_CUSTOM_FONT -> {
                    if (config.readerFontType == FontType.CUSTOM && ebWebView.shouldUseReaderFont()) {
                        ebWebView.updateCssStyle()
                    }
                }

                ConfigManager.K_IS_INCOGNITO_MODE -> {
                    ebWebView.incognito = config.isIncognitoMode
                    composeToolbarViewController.updateIcons()
                    EBToast.showShort(
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
                    ebWebView.updateUserAgentString()
                    ebWebView.reload()
                    composeToolbarViewController.updateIcons()
                }

                ConfigManager.K_DARK_MODE -> config.restartChanged = true
                ConfigManager.K_TOOLBAR_TOP -> ViewUnit.updateAppbarPosition(binding)

                ConfigManager.K_NAV_POSITION -> fabImageViewController.initialize()
                ConfigManager.K_TTS_SPEED_VALUE ->
                    ttsViewModel.setSpeechRate(config.ttsSpeedValue / 100f)

                ConfigManager.K_CUSTOM_USER_AGENT,
                ConfigManager.K_ENABLE_CUSTOM_USER_AGENT,
                    -> {
                    ebWebView.updateUserAgentString()
                    ebWebView.reload()
                }

                ConfigManager.K_ENABLE_TOUCH -> {
                    updateTouchView()
                    touchController.toggleTouchPageTurn(config.enableTouchTurn)
                }

                ConfigManager.K_TOUCH_AREA_ACTION_SWITCH -> {
                    updateTouchView()
                }

                ConfigManager.K_GPT_ACTION_ITEMS ->
                    actionModeMenuViewModel.updateMenuInfos(this, translationViewModel)
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
        if (ebWebView.canGoForward()) {
            ebWebView.goForward()
        } else {
            EBToast.show(this, R.string.toast_webview_forward)
        }
    }

    override fun gotoLeftTab() {
        nextAlbumController(false)?.let { showAlbum(it) }
    }

    override fun gotoRightTab() {
        nextAlbumController(true)?.let { showAlbum(it) }
    }

    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager)
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
            bookmarkIconClickAction = { title, url, isForeground ->
                addAlbum(
                    title,
                    url,
                    isForeground
                )
            },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            syncBookmarksAction = this::handleBookmarkSync,
            linkBookmarksAction = this::linkBookmarkSync
        ).show(supportFragmentManager, "bookmarks dialog")

    private fun handleBookmarkSync(forceUpload: Boolean = false) {
        if (config.bookmarkSyncUrl.isNotEmpty()) backupUnit.handleBookmarkSync(forceUpload)
    }

    private fun linkBookmarkSync() {
        backupUnit.linkBookmarkSync(
            dialogManager,
            createBookmarkFileLauncher,
            openBookmarkFileLauncher
        )
    }

    private fun initSearchPanel() {
        with(binding.mainSearchPanel) {
            onTextChanged = { (currentAlbumController as EBWebView?)?.findAllAsync(it) }
            onCloseClick = { hideSearchPanel() }
            onUpClick = { searchUp(it) }
            onDownClick = { searchDown(it) }
        }
    }

    private fun searchUp(text: String) {
        if (text.isEmpty()) {
            EBToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        ViewUnit.hideKeyboard(this)
        (currentAlbumController as EBWebView).findNext(false)
    }

    private fun searchDown(text: String) {
        if (text.isEmpty()) {
            EBToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        ViewUnit.hideKeyboard(this)
        (currentAlbumController as EBWebView).findNext(true)
    }

    override fun showFastToggleDialog() {
        if (!this::ebWebView.isInitialized) return

        FastToggleDialogFragment {
            ebWebView.initPreferences()
            ebWebView.reload()
        }.show(supportFragmentManager, "fast_toggle_dialog")
    }

    override fun addNewTab(url: String) = addAlbum(url = url)

    private fun getUrlMatchedBrowser(url: String): EBWebView? {
        return browserContainer.list().firstOrNull { it.albumUrl == url } as EBWebView?
    }

    private var preloadedWebView: EBWebView? = null

    open fun createebWebView(): EBWebView = EBWebView(this, this).apply {
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
        val newWebView = (preloadedWebView ?: createebWebView()).apply {
            this.albumTitle = title
            this.incognito = incognito
            setOnTouchListener(createMultiTouchTouchListener(this))
        }

        maybeCreateNewPreloadWebView(enablePreloadWebView, newWebView)

        updateTabPreview(newWebView, url)
        updateWebViewCount()

        loadUrlInWebView(foreground, newWebView, url)

        updateSavedAlbumInfo()

        if (config.adBlock) {
            adFilter.setupWebView(newWebView)
        }
    }

    private fun maybeCreateNewPreloadWebView(
        enablePreloadWebView: Boolean,
        newWebView: EBWebView,
    ) {
        preloadedWebView = null
        if (enablePreloadWebView) {
            newWebView.postDelayed({
                if (preloadedWebView == null) {
                    preloadedWebView = createebWebView()
                }
            }, 2000)
        }
    }

    private fun updateTabPreview(newWebView: EBWebView, url: String) {
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

    private fun loadUrlInWebView(foreground: Boolean, webView: EBWebView, url: String) {
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

    private fun createMultiTouchTouchListener(ebWebView: EBWebView): MultitouchListener =
        object : MultitouchListener(this@BrowserActivity, ebWebView) {
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
                if (abs(motionEvent.x - (longPressStartPoint?.x ?: 0)) > ViewUnit.dpToPixel(15) ||
                    abs(motionEvent.y - (longPressStartPoint?.y ?: 0)) > ViewUnit.dpToPixel(15)
                ) {
                    actionModeView?.visibility = INVISIBLE
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

    private fun updateWebViewCount() {
        val subScript = browserContainer.size()
        val superScript = browserContainer.indexOf(currentAlbumController) + 1
        val countString = ViewUnit.createCountString(superScript, subScript)
        composeToolbarViewController.updateTabCount(countString)
        fabImageViewController.updateTabCount(countString)
    }

    override fun updateAlbum(url: String?) {
        if (url == null) return
        (currentAlbumController as EBWebView).loadUrl(url)
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
                    ebWebView.loadUrl(config.favoriteUrl)
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
        if (!this::ebWebView.isInitialized) return

        if (this::ebWebView.isInitialized && ebWebView === currentAlbumController) {
            composeToolbarViewController.updateTitle(ebWebView.title.orEmpty())
        }
    }

    private fun scrollChange() {
        ebWebView.setScrollChangeListener(object : EBWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                ebWebView.updatePageInfo()

                if (::twoPaneController.isInitialized) {
                    twoPaneController.scrollChange(scrollY - oldScrollY)
                }

                if (!config.shouldHideToolbar) return

                val height =
                    floor(x = ebWebView.contentHeight * ebWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ebWebView.height
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

        val textOrUrl = if (ebWebView.url?.startsWith("data:") != true) {
            val url = ebWebView.url.orEmpty()
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
        ViewUnit.invertColor(view, config.hasInvertedColor(ebWebView.url.orEmpty()))

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
        if (ebWebView.hitTestResult.type == HitTestResult.EDIT_TEXT_TYPE) return false

        // process dpad navigation
        if (config.useUpDownPageTurn) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    ebWebView.pageDownWithNoAnimation()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    ebWebView.pageUpWithNoAnimation()
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

                KeyEvent.KEYCODE_G -> ebWebView.jumpToBottom()
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

                KeyEvent.KEYCODE_J -> ebWebView.pageDownWithNoAnimation()
                KeyEvent.KEYCODE_K -> ebWebView.pageUpWithNoAnimation()
                KeyEvent.KEYCODE_H -> ebWebView.goBack()
                KeyEvent.KEYCODE_L -> ebWebView.goForward()
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
                            jumpToTop()
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
                    ebWebView.reload()
                }
            }
        }
    }

    private fun confirmRemoveAdSite(url: String) {
        dialogManager.showOkCancelDialog(
            title = "remove this url from blacklist?",
            okAction = {
                config.adSites = config.adSites.apply { remove(url) }
                ebWebView.reload()
            }
        )
    }

    private var motionEvent: MotionEvent? = null
    private var longPressPoint: Point = Point(0, 0)
    override fun onLongPress(message: Message, event: MotionEvent?) {
        if (ebWebView.isSelectingText) return

        motionEvent = event
        longPressPoint = Point(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
        val url = BrowserUnit.getWebViewLinkUrl(ebWebView, message)
        if (url.isNotBlank()) {
            // case: image or link
            val linkImageUrl = BrowserUnit.getWebViewLinkImageUrl(ebWebView, message)
            BrowserUnit.getWebViewLinkTitle(ebWebView) { linkTitle ->
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
                if (prepareRecord()) EBToast.show(this, getString(R.string.toast_share_failed))
                else IntentUnit.share(this, title, url)
            }

            ContextMenuItemType.CopyLink -> ShareUtil.copyToClipboard(
                this,
                BrowserUnit.stripUrlQuery(url)
            )

            ContextMenuItemType.SelectText -> ebWebView.post {
                ebWebView.selectLinkText(longPressPoint)
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
            ContextMenuItemType.Tts -> addContentToReadList(url)
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

            else -> Unit
        }
    }

    private val headlessWebView: EBWebView by lazy {
        EBWebView(this, this).apply {
            setOnPageFinishedAction {
                lifecycleScope.launch {
                    val content = headlessWebView.getRawText()
                    if (content.isNotEmpty()) {
                        ttsViewModel.readArticle(content)
                    }
                    // remove self
                    if (toBeReadProcessUrlList.isNotEmpty()) {
                        toBeReadProcessUrlList.removeAt(0)
                    }

                    if (toBeReadProcessUrlList.isNotEmpty()) {
                        headlessWebView.loadUrl(toBeReadProcessUrlList.removeAt(0))
                    } else {
                        headlessWebView.loadUrl("about:blank")
                    }
                }
            }
        }
    }

    private var toBeReadProcessUrlList: MutableList<String> = mutableListOf()
    private fun addContentToReadList(url: String) {
        toBeReadProcessUrlList.add(url)
        if (toBeReadProcessUrlList.size == 1) {
            headlessWebView.loadUrl(url)
        }
        EBToast.show(this, R.string.added_to_read_list)
    }

    private fun translateWebView() {
        lifecycleScope.launch {
            val base64String = translationViewModel.translateWebView(
                ebWebView,
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
                    ebWebView.isTranslatePage = true
                    ebWebView.loadData(translatedImageHtml, "text/html", "utf-8")
                }
            } else {
                EBToast.show(this@BrowserActivity, "Failed to translate image")
            }
        }
    }

    private fun translateImage(url: String) {
        lifecycleScope.launch {
            val base64String = translationViewModel.translateImage(
                ebWebView.url.orEmpty(),
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
                    ebWebView.isTranslatePage = true
                    ebWebView.loadData(translatedImageHtml, "text/html", "utf-8")
                }
            } else {
                EBToast.show(this@BrowserActivity, "Failed to translate image")
            }
        }
    }

    private fun saveFile(url: String, fileName: String = "") {
        // handle data url case
        if (url.startsWith("data:image")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BrowserUnit.saveImageFromUrl(url, saveImageFilePickerLauncher)
            } else {
                EBToast.show(this, "Not supported dataUrl")
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
        ViewUnit.hideKeyboard(this)
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
        ViewUnit.hideKeyboard(this)
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
        if (this::ebWebView.isInitialized) {
            ebWebView.clearMatches()
        }
        searchOnSite = false
        ViewUnit.hideKeyboard(this)
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
        ViewUnit.showKeyboard(this)
    }

    override fun showSaveEpubDialog() = dialogManager.showSaveEpubDialog { uri ->
        if (uri == null) {
            epubManager.showWriteEpubFilePicker(writeEpubFilePickerLauncher, ebWebView.title ?: "einkbro")
        } else {
            saveEpub(uri)
        }
    }

    protected fun readArticle() {
        lifecycleScope.launch {
            ttsViewModel.readArticle(ebWebView.getRawText())
        }
    }

    private val menuActionHandler: MenuActionHandler by lazy { MenuActionHandler(this) }

    override fun showMenuDialog() =
        MenuDialogFragment(
            ebWebView.url.orEmpty(),
            ttsViewModel.isReading(),
            { menuActionHandler.handle(it, ebWebView) },
            { menuActionHandler.handleLongClick(it) }
        ).show(supportFragmentManager, "menu_dialog")

    override fun showWebArchiveFilePicker() {
        val fileName = "${ebWebView.title}.mht"
        BrowserUnit.createFilePicker(createWebArchivePickerLauncher, fileName)
    }

    override fun showOpenEpubFilePicker() =
        epubManager.showOpenEpubFilePicker(openEpubFilePickerLauncher)

    override fun handleTtsButton() {
        if (ttsViewModel.isReading()) {
            TtsSettingDialogFragment().show(supportFragmentManager, "TtsSettingDialog")
        } else {
            readArticle()
        }
    }

    override fun showTtsLanguageDialog() {
        TtsLanguageDialog(this).show(ttsViewModel.getAvailableLanguages())
    }

    override fun removeAlbum() {
        currentAlbumController?.let { removeAlbum(it) }
    }

    override fun toggleSplitScreen(url: String?) {
        maybeInitTwoPaneController()
        if (twoPaneController.isSecondPaneDisplayed() && url == null) {
            twoPaneController.hideSecondPane()
            splitSearchViewModel.reset()
            return
        }

        twoPaneController.showSecondPaneWithUrl(url ?: ebWebView.url.orEmpty())
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

    private fun getFocusedWebView(): EBWebView = when {
        ebWebView.hasFocus() -> ebWebView
        isTwoPaneControllerInitialized() && twoPaneController.getSecondWebView().hasFocus() -> {
            twoPaneController.getSecondWebView()
        }

        else -> ebWebView
    }

    // - action mode handling
    override fun onActionModeStarted(mode: ActionMode) {
        val isTextEditMode = ViewUnit.isTextEditMode(this, mode.menu)

        // check isSendingLink
        if (remoteConnViewModel.isSendingTextSearch && !isTextEditMode) {
            mode.hide(1000000)
            mode.menu.clear()
            mode.finish()

            lifecycleScope.launch {
                val keyword = getFocusedWebView().getSelectedText()
                remoteConnViewModel.sendTextSearch(externalSearchViewModel.generateSearchUrl(keyword))
            }
            return
        }

        if (!config.showDefaultActionMenu && !isTextEditMode && isInSplitSearchMode()) {
            mode.hide(1000000)
            mode.menu.clear()

            lifecycleScope.launch {
                toggleSplitScreen(splitSearchViewModel.getUrl(ebWebView.getSelectedText()))
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

                lifecycleScope.launch {
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

        super.onActionModeStarted(mode)
    }

    private var actionModeView: View? = null
    private fun showActionModeView(
        translationViewModel: TranslationViewModel,
        clearSelectionAction: () -> Unit,
    ) {
        actionModeMenuViewModel.updateMenuInfos(this, translationViewModel)
        if (actionModeView == null) {
            actionModeView = ActionModeView(this).apply {
                init(
                    actionModeMenuViewModel = actionModeMenuViewModel,
                    clearSelectionAction = { clearSelectionAction() }
                )
            }
            actionModeView?.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            actionModeView?.visibility = INVISIBLE
            binding.root.addView(actionModeView)
        }

        actionModeMenuViewModel.show()
    }

    override fun onPause() {
        super.onPause()
        actionModeMenuViewModel.finish()
        if (!config.continueMedia && !isMeetPipCriteria()) {
            if (this::ebWebView.isInitialized) {
                ebWebView.pauseTimers()
            }
        }
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mode?.hide(1000000)
        }
        actionModeMenuViewModel.updateActionMode(null)
    }

    // - action mode handling

    companion object {
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
        const val ACTION_READ_ALOUD = "action_read_aloud"
    }
}