package info.plateaukao.einkbro.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.ActionMode
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.AiConfig
import info.plateaukao.einkbro.preference.BrowserConfig
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TabConfig
import info.plateaukao.einkbro.preference.UiConfig
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.DisplayConfig
import info.plateaukao.einkbro.preference.TouchConfig
import info.plateaukao.einkbro.preference.TtsConfig
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.search.suggestion.SearchSuggestionViewModel
import info.plateaukao.einkbro.service.ClearService
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.BrowserUnit.createDownloadReceiver
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.disablePendingTransitions
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.preference.ShareLongPressAction
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.CenterExpandProgressBar
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.SendLinkDialog
import info.plateaukao.einkbro.view.dialog.compose.FastToggleDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.SiteSettingsDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.MenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.activity.delegates.ActionModeDelegate
import info.plateaukao.einkbro.activity.delegates.AiChatDelegate
import info.plateaukao.einkbro.activity.delegates.BookmarkActionsDelegate
import info.plateaukao.einkbro.activity.delegates.ChromeSetupDelegate
import info.plateaukao.einkbro.activity.delegates.ExternalSearchDelegate
import info.plateaukao.einkbro.task.TaskRunner
import info.plateaukao.einkbro.activity.delegates.ContextMenuDelegate
import info.plateaukao.einkbro.activity.delegates.DisplayConfigDelegate
import info.plateaukao.einkbro.activity.delegates.FileHandlingDelegate
import info.plateaukao.einkbro.activity.delegates.FullscreenDelegate
import info.plateaukao.einkbro.activity.delegates.InputBarDelegate
import info.plateaukao.einkbro.activity.delegates.InstapaperDelegate
import info.plateaukao.einkbro.activity.delegates.IntentDispatchDelegate
import info.plateaukao.einkbro.activity.delegates.SearchPanelDelegate
import info.plateaukao.einkbro.activity.delegates.TabManager
import info.plateaukao.einkbro.activity.delegates.TaskMenuDelegate
import info.plateaukao.einkbro.activity.delegates.TranslationDelegate
import info.plateaukao.einkbro.activity.delegates.TtsButtonDelegate
import info.plateaukao.einkbro.view.handlers.GestureHandler
import info.plateaukao.einkbro.view.handlers.MenuActionHandler
import info.plateaukao.einkbro.view.handlers.ToolbarActionHandler
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.AlbumViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModelFactory
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import info.plateaukao.einkbro.viewmodel.InstapaperViewModel
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import info.plateaukao.einkbro.viewmodel.SplitSearchViewModel
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel


open class BrowserActivity : FragmentActivity(), BrowserController {
    protected open var shouldRunClearService: Boolean = true

    // DI
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val recordDb: RecordRepository by inject()
    private val searchSuggestionViewModel: SearchSuggestionViewModel by inject()

    // ViewModels
    private val ttsViewModel: TtsViewModel by koinViewModel()
    private val translationViewModel: TranslationViewModel by koinViewModel()
    private val splitSearchViewModel: SplitSearchViewModel by viewModels()
    private val remoteConnViewModel: RemoteConnViewModel by koinViewModel()
    private val externalSearchViewModel: ExternalSearchViewModel by koinViewModel()
    private val instapaperViewModel: InstapaperViewModel by koinViewModel()
    private val albumViewModel: AlbumViewModel by viewModels()
    private val bookmarkViewModel: BookmarkViewModel by viewModels { BookmarkViewModelFactory(bookmarkManager) }
    private val actionModeMenuViewModel: ActionModeMenuViewModel by koinViewModel()

    // Controllers
    private lateinit var twoPaneController: TwoPaneController
    private var downloadReceiver: BroadcastReceiver? = null

    private val adFilter: AdFilter = AdFilter.get()
    private val filterViewModel: FilterViewModel = adFilter.viewModel
    private val browserContainer: BrowserContainer = BrowserContainer()

    // Fields delegated through browserState for delegate sharing
    protected var ebWebView: EBWebView
        get() = browserState.ebWebView
        set(value) { browserState.ebWebView = value }
    private var currentAlbumController: AlbumController?
        get() = browserState.currentAlbumController
        set(value) { browserState.currentAlbumController = value }
    private var searchOnSite: Boolean
        get() = browserState.searchOnSite
        set(value) { browserState.searchOnSite = value }
    private var longPressPoint: Point
        get() = browserState.longPressPoint
        set(value) { browserState.longPressPoint = value }
    private var binding: MainActivityLayout
        get() = browserState.binding
        set(value) { browserState.binding = value }
    private var mainContentLayout: FrameLayout
        get() = browserState.mainContentLayout
        set(value) { browserState.mainContentLayout = value }
    private var progressBar: ProgressBar
        get() = browserState.progressBar
        set(value) { browserState.progressBar = value }
    private var progressBarVertical: CenterExpandProgressBar
        get() = browserState.progressBarVertical
        set(value) { browserState.progressBarVertical = value }
    private var fabImageViewController: FabImageViewController
        get() = browserState.fabImageViewController
        set(value) { browserState.fabImageViewController = value }

    private val keyHandler: KeyHandler by lazy { KeyHandler(this, ebWebView, config) }
    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private val gestureHandler: GestureHandler by lazy { GestureHandler { dispatch(it) } }
    private val toolbarActionHandler: ToolbarActionHandler by lazy { ToolbarActionHandler(this) { dispatch(it) } }
    private val menuActionHandler: MenuActionHandler by lazy { MenuActionHandler(this, { dispatch(it) }) { ebWebView } }
    private val externalSearchWebView: WebView by lazy { BrowserUnit.createNaverDictWebView(this) }

    private val displayConfigDelegate: DisplayConfigDelegate by lazy {
        DisplayConfigDelegate(
            activity = this,
            state = browserState,
            config = config,
            onOrientationChanged = { newOrientation ->
                composeToolbarViewController.updateIcons()
                if (config.ui.fabPosition == FabPosition.Custom) {
                    fabImageViewController.updateImagePosition(newOrientation)
                }
            },
            onLocaleApplied = { composeToolbarViewController.updateIcons() },
        )
    }

    private var isRunning = false

    // ── Shared State ─────────────────────────────────────────────────────
    val browserState = BrowserState()

    // ── Delegates ──────────────────────────────────────────────────────────

    private val fullscreenDelegate: FullscreenDelegate by lazy {
        FullscreenDelegate(
            activity = this,
            config = config,
            state = browserState,
            searchPanelHideAction = { searchOnSite = false; ViewUnit.hideKeyboard(this) },
        )
    }

    private val aiChatDelegate: AiChatDelegate by lazy {
        AiChatDelegate(
            activity = this,
            config = config,
            state = browserState,
            translationViewModel = translationViewModel,
            ttsViewModel = ttsViewModel,
            twoPaneControllerProvider = { twoPaneController },
            isTwoPaneControllerInitialized = { isTwoPaneControllerInitialized() },
            maybeInitTwoPaneController = { maybeInitTwoPaneController() },
            addAlbum = { title, _ -> addAlbum(title) },
            showTranslationDialog = { isWholePageMode -> translationDelegate.showTranslationDialog(isWholePageMode) },
            showTaskMenu = { dispatch(BrowserAction.ShowTaskMenu) },
        )
    }

    private val taskRunner: TaskRunner by lazy {
        TaskRunner(
            activity = this,
            context = this,
            webViewCallback = this,
            browserState = browserState,
            config = config,
            ttsViewModel = ttsViewModel,
        )
    }

    private val contextMenuDelegate: ContextMenuDelegate by lazy {
        ContextMenuDelegate(
            activity = this,
            config = config,
            state = browserState,
            ttsViewModel = ttsViewModel,
            addAlbum = { title, url, foreground -> addAlbum(title, url, foreground) },
            prepareRecord = { prepareRecord() },
            saveBookmark = { url, title -> saveBookmark(url, title) },
            toggleSplitScreen = { url -> toggleSplitScreen(url) },
            summarizeLinkContent = { url -> aiChatDelegate.summarizeLinkContent(url) },
            translateImage = { url -> translationDelegate.translateImage(url) },
            translateAllImages = { url -> translationDelegate.translateAllImages(url) },
            saveFile = { url, fileName ->
                info.plateaukao.einkbro.unit.DownloadHelper.saveFileWithName(
                    this, url, fileName, fileHandlingDelegate.saveImageFilePickerLauncher,
                )
            },
        )
    }

    private val inputBarDelegate: InputBarDelegate by lazy {
        InputBarDelegate(
            activity = this,
            config = config,
            state = browserState,
            bookmarkManager = bookmarkManager,
            searchSuggestionViewModel = searchSuggestionViewModel,
            updateAlbum = { url -> updateAlbum(url) },
            showToolbar = { fullscreenDelegate.showToolbar() },
            toggleFullscreen = { toggleFullscreen() },
        )
    }

    private val intentDispatchDelegate: IntentDispatchDelegate by lazy {
        IntentDispatchDelegate(
            activity = this,
            config = config,
            state = browserState,
            externalSearchViewModel = externalSearchViewModel,
            remoteConnViewModel = remoteConnViewModel,
            translationViewModel = translationViewModel,
            overviewDialogControllerProvider = { chromeSetupDelegate.overviewDialogController },
            addAlbum = { title, url, foreground -> addAlbum(title, url, foreground) },
            updateAlbum = { url -> updateAlbum(url) },
            showAlbum = { controller -> showAlbum(controller) },
            getUrlMatchedBrowser = { url -> tabManager.getUrlMatchedBrowser(url) },
            openHistoryPage = { openHistoryPage() },
            openBookmarkPage = { openBookmarkPage() },
            focusOnInput = { focusOnInput() },
            readArticle = { readArticle() },
            chatWithWeb = { useSplitScreen, content, action -> chatWithWeb(useSplitScreen, content, action) },
            showTranslationDialog = { isWholePageMode -> translationDelegate.showTranslationDialog(isWholePageMode) },
        )
    }

    private val actionModeDelegate: ActionModeDelegate by lazy {
        ActionModeDelegate(
            activity = this,
            config = config,
            browserState = browserState,
            actionModeMenuViewModel = actionModeMenuViewModel,
            translationViewModel = translationViewModel,
            ttsViewModel = ttsViewModel,
            splitSearchViewModel = splitSearchViewModel,
            remoteConnViewModel = remoteConnViewModel,
            externalSearchViewModel = externalSearchViewModel,
            bookmarkManager = bookmarkManager,
            getFocusedWebView = { getFocusedWebView() },
            showTranslationDialog = { isWholePageMode -> translationDelegate.showTranslationDialog(isWholePageMode) },
            updateTranslationInput = { translationDelegate.updateTranslationInput() },
            toggleSplitScreen = { url -> toggleSplitScreen(url) },
            chatWithWeb = { useSplitScreen, content, action -> chatWithWeb(useSplitScreen, content, action) },
        )
    }

    protected val translationDelegate: TranslationDelegate by lazy {
        TranslationDelegate(
            activity = this,
            config = config,
            state = browserState,
            translationViewModel = translationViewModel,
            actionModeMenuViewModel = actionModeMenuViewModel,
            focusedWebViewProvider = { getFocusedWebView() },
            externalSearchWebViewProvider = { externalSearchWebView },
            twoPaneControllerProvider = { twoPaneController },
            isTwoPaneControllerInitialized = { isTwoPaneControllerInitialized() },
            maybeInitTwoPaneController = { maybeInitTwoPaneController() },
            addAlbum = { tabManager.addAlbum() },
        ).also {
            it.showSiteSettingsAction = { showSiteSettingsDialog() }
        }
    }

    val tabManager: TabManager by lazy {
        TabManager(
            activity = this,
            config = config,
            state = browserState,
            browserContainer = browserContainer,
            albumViewModel = albumViewModel,
            bookmarkManager = bookmarkManager,
            externalSearchViewModel = externalSearchViewModel,
            createWebView = { createebWebView() },
            createTouchListener = { chromeSetupDelegate.createMultiTouchTouchListener(it) },
            keyHandlerSetWebView = { keyHandler.setWebView(it) },
            addHistoryAction = { title, url -> addHistory(title, url) },
            adFilterProvider = { adFilter },
            updateLanguageLabel = { translationDelegate.updateLanguageLabel() },
        )
    }

    val fileHandlingDelegate: FileHandlingDelegate by lazy {
        FileHandlingDelegate(
            activity = this,
            state = browserState,
            bookmarkManager = bookmarkManager,
        )
    }

    private val bookmarkActionsDelegate: BookmarkActionsDelegate by lazy {
        BookmarkActionsDelegate(
            activity = this,
            state = browserState,
            bookmarkViewModel = bookmarkViewModel,
            overviewDialogControllerProvider = { chromeSetupDelegate.overviewDialogController },
            updateAlbum = { url -> updateAlbum(url) },
            addAlbum = { title, url, foreground -> addAlbum(title, url, foreground) },
            toggleSplitScreen = { url -> toggleSplitScreen(url) },
        )
    }

    private val taskMenuDelegate: TaskMenuDelegate by lazy {
        TaskMenuDelegate(
            activity = this,
            state = browserState,
            config = config,
            taskRunner = taskRunner,
            translationViewModel = translationViewModel,
            dispatch = { dispatch(it) },
            showTranslationDialog = { isWholePage -> translationDelegate.showTranslationDialog(isWholePage) },
            chatWithWebAgent = { prompt, snapshot -> aiChatDelegate.chatWithWebAgent(prompt, snapshot) },
        )
    }

    private val ttsButtonDelegate: TtsButtonDelegate by lazy {
        TtsButtonDelegate(
            activity = this,
            state = browserState,
            ttsViewModel = ttsViewModel,
            composeToolbarViewControllerProvider = { composeToolbarViewController },
        )
    }

    private val externalSearchDelegate: ExternalSearchDelegate by lazy {
        ExternalSearchDelegate(
            activity = this,
            state = browserState,
            externalSearchViewModel = externalSearchViewModel,
        )
    }

    private val instapaperDelegate: InstapaperDelegate by lazy {
        InstapaperDelegate(
            activity = this,
            state = browserState,
            config = config,
            instapaperViewModel = instapaperViewModel,
            dialogManager = dialogManager,
        )
    }

    private val searchPanelDelegate: SearchPanelDelegate by lazy {
        SearchPanelDelegate(
            activity = this,
            state = browserState,
            remoteConnViewModel = remoteConnViewModel,
            translationViewModel = translationViewModel,
            fabImageViewControllerProvider = { fabImageViewController },
            showTranslationDialog = { isWholePageMode -> translationDelegate.showTranslationDialog(isWholePageMode) },
            chatWithWeb = { useSplitScreen, content, action -> chatWithWeb(useSplitScreen, content, action) },
            hideSearchPanel = { hideSearchPanel() },
        )
    }

    private val chromeSetupDelegate: ChromeSetupDelegate by lazy {
        ChromeSetupDelegate(
            activity = this,
            state = browserState,
            config = config,
            displayConfigDelegate = displayConfigDelegate,
            fullscreenDelegate = fullscreenDelegate,
            gestureHandler = gestureHandler,
            contextMenuDelegate = contextMenuDelegate,
            actionModeDelegate = actionModeDelegate,
            inputBarDelegate = inputBarDelegate,
            albumViewModel = albumViewModel,
            composeToolbarViewControllerProvider = { composeToolbarViewController },
            twoPaneControllerProvider = { if (isTwoPaneControllerInitialized()) twoPaneController else null },
            dispatch = { dispatch(it) },
            updateAlbum = { url -> updateAlbum(url) },
            addAlbum = { title, url, foreground -> addAlbum(title, url, foreground) },
            addIncognitoAlbum = {
                hideOverview()
                addAlbum(getString(R.string.app_name), "", incognito = true)
                focusOnInput()
            },
            newATab = { newATab() },
            toggleSplitScreen = { url -> toggleSplitScreen(url) },
            focusOnInput = { focusOnInput() },
            showFastToggleDialog = { showFastToggleDialog() },
            toggleFullscreen = { toggleFullscreen() },
        )
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
            isAudioOnlyMode = { if (browserState.isWebViewInitialized) ebWebView.isAudioOnlyMode else false },
        ).also { browserState.composeToolbarViewController = it }
    }

    protected val statusbarViewController: info.plateaukao.einkbro.view.viewControllers.StatusbarViewController by lazy {
        info.plateaukao.einkbro.view.viewControllers.StatusbarViewController(
            composeView = binding.statusBar,
            applyConstraints = { position -> chromeSetupDelegate.applyStatusbarConstraints(position) },
        ).also { browserState.statusbarViewController = it }
    }

    // ── BrowserAction dispatch ─────────────────────────────────────────────

    fun dispatch(action: BrowserAction) = when (action) {
        is BrowserAction.Noop -> Unit
        is BrowserAction.NewATab -> newATab()
        is BrowserAction.DuplicateTab -> duplicateTab()
        is BrowserAction.RemoveAlbum -> removeAlbum()
        is BrowserAction.GotoLeftTab -> gotoLeftTab()
        is BrowserAction.GotoRightTab -> gotoRightTab()
        is BrowserAction.AddNewTab -> addNewTab(action.url)
        is BrowserAction.UpdateAlbum -> updateAlbum(action.url)
        is BrowserAction.GoForward -> goForward()
        is BrowserAction.HandleBackKey -> handleBackKey()
        is BrowserAction.RefreshAction -> refreshAction()
        is BrowserAction.JumpToTop -> jumpToTop()
        is BrowserAction.JumpToBottom -> jumpToBottom()
        is BrowserAction.PageUp -> pageUp()
        is BrowserAction.PageDown -> pageDown()
        is BrowserAction.SendPageUpKey -> sendPageUpKey()
        is BrowserAction.SendPageDownKey -> sendPageDownKey()
        is BrowserAction.SendLeftKey -> sendLeftKey()
        is BrowserAction.SendRightKey -> sendRightKey()
        is BrowserAction.ToggleReaderMode -> toggleReaderMode()
        is BrowserAction.ToggleVerticalRead -> toggleVerticalRead()
        is BrowserAction.IncreaseFontSize -> increaseFontSize()
        is BrowserAction.DecreaseFontSize -> decreaseFontSize()
        is BrowserAction.ShowFontSizeChangeDialog -> showFontSizeChangeDialog()
        is BrowserAction.ShowFontBoldnessDialog -> showFontBoldnessDialog()
        is BrowserAction.InvertColors -> invertColors()
        is BrowserAction.ShowOverview -> showOverview()
        is BrowserAction.ToggleFullscreen -> toggleFullscreen()
        is BrowserAction.ToggleSplitScreen -> toggleSplitScreen(action.url)
        is BrowserAction.ShowTranslation -> showTranslation()
        is BrowserAction.ShowTranslationConfigDialog -> showTranslationConfigDialog(action.translateDirectly)
        is BrowserAction.Translate -> translate(action.mode)
        is BrowserAction.ConfigureTranslationLanguage -> configureTranslationLanguage(action.api)
        is BrowserAction.HandleTtsButton -> handleTtsButton()
        is BrowserAction.OpenBookmarkPage -> openBookmarkPage()
        is BrowserAction.OpenHistoryPage -> openHistoryPage(action.amount)
        is BrowserAction.SaveBookmark -> saveBookmark(action.url, action.title)
        is BrowserAction.ShowSearchPanel -> showSearchPanel()
        is BrowserAction.ToggleTextSearch -> toggleTextSearch()
        is BrowserAction.ToggleReceiveTextSearch -> toggleReceiveTextSearch()
        is BrowserAction.CreateShortcut -> createShortcut()
        is BrowserAction.ShareLink -> shareLink()
        is BrowserAction.ShareLinkToLastTarget -> shareLinkToLastTarget()
        is BrowserAction.ShareLinkLongPress -> handleShareLongPress()
        is BrowserAction.SendToRemote -> sendToRemote(action.text)
        is BrowserAction.AddToInstapaper -> addToInstapaper()
        is BrowserAction.ConfigureInstapaper -> configureInstapaper()
        is BrowserAction.ToggleReceiveLink -> toggleReceiveLink()
        is BrowserAction.ToggleTouchTurnPage -> toggleTouchTurnPageFeature()
        is BrowserAction.ToggleSwitchTouchAreaAction -> toggleSwitchTouchAreaAction()
        is BrowserAction.ShowTouchAreaDialog -> showTouchAreaDialog()
        is BrowserAction.ToggleTouchPagination -> toggleTouchPagination()
        is BrowserAction.SummarizeContent -> summarizeContent()
        is BrowserAction.ChatWithWeb -> chatWithWeb(action.useSplitScreen, action.content, action.runWithAction)
        is BrowserAction.ShowPageAiActionMenu -> showPageAiActionMenu()
        is BrowserAction.ShowTaskMenu -> showTaskMenu()
        is BrowserAction.RunTask -> runTaskById(action.taskId)
        is BrowserAction.RunCustomTask -> runCustomTask(action.prompt)
        is BrowserAction.ShowEpubDialog -> showEpubDialog()
        is BrowserAction.SavePageForLater -> savePageForLater()
        is BrowserAction.ShowSavedPages -> showSavedPages()
        is BrowserAction.SaveWebArchive -> showWebArchiveFilePicker()
        is BrowserAction.SavePdf -> showPdfFilePicker()
        is BrowserAction.FocusOnInput -> focusOnInput()
        is BrowserAction.ShowMenuDialog -> showMenuDialog()
        is BrowserAction.ShowFastToggleDialog -> showFastToggleDialog()
        is BrowserAction.ShowTocDialog -> showTocDialog()
        is BrowserAction.RotateScreen -> rotateScreen()
        is BrowserAction.ToggleAudioOnlyMode -> toggleAudioOnlyMode()
        is BrowserAction.ShowSiteSettingsDialog -> showSiteSettingsDialog()
    }

    // ── BrowserController implementation ──────────────────────────────────

    override fun newATab() = tabManager.newATab(searchOnSite, { hideSearchPanel() }, { focusOnInput() })
    override fun duplicateTab() = tabManager.duplicateTab()
    override fun refreshAction() {
        if (ebWebView.isLoadFinish && ebWebView.url?.isNotEmpty() == true) ebWebView.reload()
        else ebWebView.stopLoading()
    }

    override fun isAtTop(): Boolean = ebWebView.isAtTop()
    override fun jumpToTop() = ebWebView.jumpToTop()
    override fun jumpToBottom() = ebWebView.jumpToBottom()
    override fun pageDown() = ebWebView.pageDownWithNoAnimation()
    override fun pageUp() = ebWebView.pageUpWithNoAnimation()
    override fun toggleReaderMode() = ebWebView.toggleReaderMode()
    override fun toggleVerticalRead() = ebWebView.toggleVerticalRead()
    override fun toggleAudioOnlyMode() = ebWebView.toggleAudioOnlyMode()
    override fun updatePageInfo(info: String) {
        composeToolbarViewController.updatePageInfo(info)
        statusbarViewController.updatePageInfo(info)
    }
    override fun sendPageUpKey() = ebWebView.sendPageUpKey()
    override fun sendPageDownKey() = ebWebView.sendPageDownKey()
    override fun sendLeftKey() { ebWebView.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)) }
    override fun sendRightKey() { ebWebView.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)) }

    override fun translate(translationMode: TranslationMode) = translationDelegate.translate(translationMode)
    override fun resetTranslateUI() = translationDelegate.resetTranslateUI()
    override fun configureTranslationLanguage(translateApi: TRANSLATE_API) = translationDelegate.configureTranslationLanguage(translateApi)
    override fun showTranslation(webView: EBWebView?) = translationDelegate.showTranslation(webView)
    override fun showTranslationConfigDialog(translateDirectly: Boolean) = translationDelegate.showTranslationConfigDialog(translateDirectly)

    override fun toggleTouchPagination() = toggleTouchTurnPageFeature()
    override fun toggleTextSearch() { remoteConnViewModel.toggleTextSearch() }
    override fun toggleTouchTurnPageFeature() = config.touch::enableTouchTurn.toggle()
    override fun toggleSwitchTouchAreaAction() = config.touch::switchTouchAreaAction.toggle()

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) = fullscreenDelegate.onShowCustomView(view, callback)
    override fun onHideCustomView(): Boolean = fullscreenDelegate.onHideCustomView()
    override fun toggleFullscreen() = fullscreenDelegate.toggleFullscreen()
    override fun showOverview() = chromeSetupDelegate.overviewDialogController.show()
    override fun hideOverview() = chromeSetupDelegate.overviewDialogController.hide()
    override fun rotateScreen() = IntentUnit.rotateScreen(this)

    override fun addHistory(title: String, url: String) {
        lifecycleScope.launch { recordDb.addHistory(Record(title, url, System.currentTimeMillis())) }
    }

    override fun isCurrentAlbum(albumController: AlbumController): Boolean = tabManager.isCurrentAlbum(albumController)
    override fun showAlbum(controller: AlbumController) = tabManager.showAlbum(controller)
    override fun addNewTab(url: String) = tabManager.addNewTab(url)
    override fun removeAlbum(albumController: AlbumController, showHome: Boolean) = tabManager.removeAlbum(albumController, showHome)
    override fun removeAlbum() = tabManager.removeCurrentAlbum()
    override fun gotoLeftTab() = tabManager.gotoLeftTab()
    override fun gotoRightTab() = tabManager.gotoRightTab()
    override fun updateAlbum(url: String?) = tabManager.updateAlbum(url)

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) = fileHandlingDelegate.showFileChooser(filePathCallback)
    override fun showEpubDialog() = fileHandlingDelegate.showEpubDialog()
    override fun showWebArchiveFilePicker() = fileHandlingDelegate.showWebArchiveFilePicker()
    override fun showPdfFilePicker() = fileHandlingDelegate.showPdfFilePicker()
    override fun showOpenEpubFilePicker() = fileHandlingDelegate.showOpenEpubFilePicker()
    override fun savePageForLater() = fileHandlingDelegate.savePageForLater()
    override fun showSavedPages() = fileHandlingDelegate.showSavedPages()

    override fun onLongPress(message: Message, event: MotionEvent?) = contextMenuDelegate.onLongPress(message, event)
    override fun handleKeyEvent(event: KeyEvent): Boolean = keyHandler.handleKeyEvent(event)
    override fun focusOnInput() = inputBarDelegate.focusOnInput()

    override fun summarizeContent() = aiChatDelegate.summarizeContent()
    override fun chatWithWeb(useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) = aiChatDelegate.chatWithWeb(useSplitScreen, content, runWithAction)
    override fun showPageAiActionMenu() = aiChatDelegate.showPageAiActionMenu()

    private fun showTaskMenu() = taskMenuDelegate.showTaskMenu()

    private fun runTaskById(taskId: String) = taskMenuDelegate.runTaskById(taskId)

    private fun runCustomTask(prompt: String) = taskMenuDelegate.runCustomTask(prompt)

    override fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float) = actionModeDelegate.updateSelectionRect(left, top, right, bottom)
    override fun isActionModeActive(): Boolean = actionModeDelegate.isActionModeActive()
    override fun dismissActionMode() = actionModeDelegate.dismissActionMode()

    override fun handleBackKey() {
        ViewUnit.hideKeyboard(this)
        if (chromeSetupDelegate.overviewDialogController.isVisible()) hideOverview()
        if (fullscreenDelegate.fullscreenHolder != null) {
            onHideCustomView()
        } else if (!binding.appBar.isVisible && config.ui.showToolbarFirst) {
            fullscreenDelegate.showToolbar()
        } else if (!composeToolbarViewController.isDisplayed()) {
            composeToolbarViewController.show()
        } else {
            if (!ebWebView.isTranslatePage && ebWebView.canGoBack()) {
                ebWebView.goBack()
            } else {
                if (config.tab.closeTabWhenNoMoreBackHistory) removeAlbum()
                else EBToast.show(this, R.string.no_previous_page)
            }
        }
    }

    override fun goForward() {
        if (ebWebView.canGoForward()) ebWebView.goForward()
        else EBToast.show(this, R.string.toast_webview_forward)
    }

    override fun saveBookmark(url: String?, title: String?) = bookmarkActionsDelegate.saveBookmark(url, title)

    override fun createShortcut() = bookmarkActionsDelegate.createShortcut()

    override fun showSearchPanel() = searchPanelDelegate.showSearchPanel()

    override fun showFontSizeChangeDialog() = displayConfigDelegate.showFontSizeChangeDialog()

    override fun showFontBoldnessDialog() = displayConfigDelegate.showFontBoldnessDialog()

    override fun increaseFontSize() = displayConfigDelegate.increaseFontSize()

    override fun decreaseFontSize() = displayConfigDelegate.decreaseFontSize()

    override fun invertColors() = displayConfigDelegate.invertColors()

    override fun shareLink() = bookmarkActionsDelegate.shareLink()

    private fun shareLinkToLastTarget() {
        val webView = ebWebView
        IntentUnit.shareToLastTarget(this, webView.title, webView.url)
    }

    private fun handleShareLongPress() {
        when (config.browser.shareLongPressAction) {
            ShareLongPressAction.COPY_LINK -> ShareUtil.copyToClipboard(
                this,
                BrowserUnit.stripUrlQuery(ebWebView.url.orEmpty())
            )
            ShareLongPressAction.LAST_SHARE_TARGET -> shareLinkToLastTarget()
        }
    }

    override fun sendToRemote(text: String) {
        if (remoteConnViewModel.isSendingTextSearch) {
            remoteConnViewModel.toggleTextSearch()
            EBToast.show(this, R.string.send_to_remote_terminate)
            return
        }
        SendLinkDialog(this, lifecycleScope).show(text)
    }

    override fun toggleReceiveTextSearch() = searchPanelDelegate.toggleReceiveTextSearch()

    override fun toggleReceiveLink() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
            EBToast.show(this, R.string.receive_link_terminate)
            return
        }
        ReceiveDataDialog(this, lifecycleScope).show {
            ShareUtil.startReceiving(lifecycleScope) { url ->
                if (url.isNotBlank()) { ebWebView.loadUrl(url); ShareUtil.stopBroadcast() }
            }
        }
    }

    override fun addToInstapaper() = instapaperDelegate.addToInstapaper()

    override fun configureInstapaper() = instapaperDelegate.configureInstapaper()

    override fun handleTtsButton() = ttsButtonDelegate.handleTtsButton()

    override fun showTtsLanguageDialog() = ttsButtonDelegate.showTtsLanguageDialog()

    override fun toggleSplitScreen(url: String?) {
        maybeInitTwoPaneController()
        if (twoPaneController.isSecondPaneDisplayed() && url == null) {
            twoPaneController.hideSecondPane()
            splitSearchViewModel.reset()
            return
        }
        twoPaneController.showSecondPaneWithUrl(url ?: ebWebView.url.orEmpty())
    }

    override fun loadInSecondPane(url: String): Boolean =
        if (config.translation.twoPanelLinkHere && isTwoPaneControllerInitialized() && twoPaneController.isSecondPaneDisplayed()) {
            toggleSplitScreen(url); true
        } else false

    override fun showFastToggleDialog() {
        if (!browserState.isWebViewInitialized) return
        FastToggleDialogFragment { ebWebView.initPreferences(); ebWebView.reload() }.show(supportFragmentManager, "fast_toggle_dialog")
    }

    override fun showMenuDialog() = MenuDialogFragment(
        ebWebView.url.orEmpty(), ttsViewModel.isReading(), ebWebView.isAudioOnlyMode,
        ebWebView.hasVideo, config.touch.enableTouchTurn,
        { menuActionHandler.handle(it) }, { menuActionHandler.handleLongClick(it) }
    ).show(supportFragmentManager, "menu_dialog")

    private fun showSiteSettingsDialog() {
        if (!browserState.isWebViewInitialized) return
        SiteSettingsDialogFragment(
            url = ebWebView.url.orEmpty(),
            onDismissAction = { ebWebView.initPreferences(); ebWebView.reload() },
        ).show(supportFragmentManager, "site_settings_dialog")
    }

    override fun showTouchAreaDialog() = TouchAreaDialogFragment().show(supportFragmentManager, "TouchAreaDialog")

    override fun openHistoryPage(amount: Int) = bookmarkActionsDelegate.openHistoryPage(amount)

    override fun openBookmarkPage() = bookmarkActionsDelegate.openBookmarkPage()

    override fun updateTitle(title: String?) = updateTitle()
    override fun updateProgress(progress: Int) {
        progressBar.progress = progress
        progressBarVertical.progress = progress
        val isVertical = config.ui.isVerticalToolbar
        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true)
            progressBar.visibility = if (isVertical) GONE else VISIBLE
            progressBarVertical.visibility = if (isVertical) VISIBLE else GONE
        } else {
            updateRefresh(false)
            progressBar.visibility = GONE
            progressBarVertical.visibility = GONE
            browserState.swipeRefreshLayout.isRefreshing = false
            chromeSetupDelegate.scrollChange()
            tabManager.updateSavedAlbumInfo()
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        binding = MainActivityLayout.create(this)

        savedInstanceState?.let {
            intentDispatchDelegate.shouldLoadTabState = it.getBoolean(K_SHOULD_LOAD_TAB_STATE)
        }

        config.restartChanged = false
        displayConfigDelegate.onCreate()
        HelperUnit.applyTheme(this)
        setContentView(binding.root)

        chromeSetupDelegate.initContentViews()
        initLaunchers()
        chromeSetupDelegate.initToolbar()
        runOnUiThread { config.registerOnSharedPreferenceChangeListener(preferenceChangeListener) }
        searchPanelDelegate.initSearchPanel()
        inputBarDelegate.initInputBar()
        chromeSetupDelegate.initOverview()
        chromeSetupDelegate.initTouchArea()
        actionModeDelegate.initActionModeViewModel()
        actionModeDelegate.setTwoPaneChecker { isTwoPaneControllerInitialized() && twoPaneController.isSecondPaneDisplayed() }
        instapaperDelegate.init()

        initDownloadReceiver()

        // ViewModel survives activity recreation (e.g. keyboard config change + recreate()).
        // Clear stale album entries so initSavedTabs() doesn't append duplicates
        albumViewModel.clearAlbums();

        dispatchIntent(intent)
        intentDispatchDelegate.shouldLoadTabState = false

        if (config.ui.keepAwake) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        translationDelegate.initLanguageLabel()
        chromeSetupDelegate.initTouchAreaViewController()
        searchPanelDelegate.initTextSearchButton()
        externalSearchDelegate.init()
        translationDelegate.initTranslationViewModel()
        ttsButtonDelegate.init()

        if (config.ui.hideStatusbar) fullscreenDelegate.hideStatusBar()

        // Initialize statusbar controller; show it when toolbar is configured hidden at launch
        statusbarViewController
        if (config.ui.shouldHideToolbar) statusbarViewController.show()

        chromeSetupDelegate.handleWindowInsets()
        chromeSetupDelegate.listenKeyboardShowHide()

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission()
        } else {
            binding.root.postDelayed({ checkAdBlockerList() }, 1000)
        }

        binding.root.postDelayed({ MenuDialogFragment.prewarm(this) }, 1500)
    }

    private fun initDownloadReceiver() {
        downloadReceiver = createDownloadReceiver(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        displayConfigDelegate.onResume()
        if (config.restartChanged) { config.restartChanged = false; dialogManager.showRestartConfirmDialog() }
        statusbarViewController.refresh()
        if (!binding.appBar.isVisible) statusbarViewController.show() else statusbarViewController.hide()
        updateTitle()
        disablePendingTransitions()
        if (config.display.customFontChanged && (config.display.fontType == FontType.CUSTOM || config.display.readerFontType == FontType.CUSTOM)) {
            if (!ebWebView.shouldUseReaderFont()) ebWebView.reload() else ebWebView.updateCssStyle()
            config.display.customFontChanged = false
        }
        if (!config.browser.continueMedia && browserState.isWebViewInitialized) ebWebView.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        actionModeDelegate.onPause()
        if (!config.browser.continueMedia && !isMeetPipCriteria() && browserState.isWebViewInitialized) ebWebView.pauseTimers()
    }

    override fun onDestroy() {
        ttsViewModel.reset()
        tabManager.updateSavedAlbumInfo()
        if (config.clearWhenQuit && shouldRunClearService) startService(Intent(this, ClearService::class.java))
        browserContainer.clear()
        unregisterReceiver(downloadReceiver)
        config.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        chromeSetupDelegate.dispose()
        keyHandler.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(K_SHOULD_LOAD_TAB_STATE, true)
        super.onSaveInstanceState(outState)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            for (controller in browserContainer.list()) {
                if (controller != currentAlbumController) controller.pauseWebView()
            }
            tabManager.destroyPreloadedWebView()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        displayConfigDelegate.onConfigurationChanged(newConfig)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isMeetPipCriteria()) enterPipMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = keyHandler.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean = keyHandler.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = keyHandler.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

    override fun onActionModeStarted(mode: ActionMode) {
        actionModeDelegate.onActionModeStarted(mode)
        super.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        actionModeDelegate.onActionModeFinished(mode)
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) super.attachBaseContext(LocaleManager.setLocale(newBase, config.uiLocaleLanguage))
        else super.attachBaseContext(newBase)
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun prepareRecord(): Boolean = bookmarkActionsDelegate.prepareRecord()

    private fun getFocusedWebView(): EBWebView = when {
        ebWebView.hasFocus() -> ebWebView
        isTwoPaneControllerInitialized() && twoPaneController.getSecondWebView().hasFocus() -> twoPaneController.getSecondWebView()
        else -> ebWebView
    }

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    private fun maybeInitTwoPaneController() {
        if (!isTwoPaneControllerInitialized()) {
            twoPaneController = TwoPaneController(
                this, lifecycleScope, browserState.translationPanelView, binding.twoPanelLayout,
                { showTranslation() },
                { if (ebWebView.isReaderModeOn) ebWebView.toggleReaderMode() },
                { url -> ebWebView.loadUrl(url) },
                { api, webView -> translationDelegate.translateByParagraph(api, webView) },
                this::translateWebView
            )
        }
    }

    private fun translateWebView() = translationDelegate.translateWebView()

    private fun hideSearchPanel() = fullscreenDelegate.hideSearchPanel()


    private fun updateTitle() {
        if (!browserState.isWebViewInitialized) return
        tabManager.updateTitle()
    }

    private fun updateRefresh(running: Boolean) {
        isRunning = running
        composeToolbarViewController.updateRefresh(isRunning)
    }

    private fun isMeetPipCriteria() = config.browser.enableVideoPip && fullscreenDelegate.fullscreenHolder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipMode() { enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }

    private fun initLaunchers() = fileHandlingDelegate.initLaunchers()

    protected fun readArticle() = ttsButtonDelegate.readArticle()

    @SuppressLint("ClickableViewAccessibility")
    protected fun addAlbum(
        title: String = "",
        url: String = config.favoriteUrl,
        foreground: Boolean = true,
        incognito: Boolean = false,
        enablePreloadWebView: Boolean = true,
    ) = tabManager.addAlbum(title, url, foreground, incognito, enablePreloadWebView)

    open fun createebWebView(): EBWebView = EBWebView(this, this).apply { overScrollMode = View.OVER_SCROLL_NEVER }

    @SuppressLint("InlinedApi")
    open fun dispatchIntent(intent: Intent) = intentDispatchDelegate.dispatchIntent(intent)

    private fun checkAdBlockerList() {
        if (!adFilter.hasInstallation) {
            val map = mapOf("AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt")
            for ((key, value) in map) filterViewModel.addFilter(key, value)
            val filters = filterViewModel.filters.value
            for ((key, _) in filters) filterViewModel.download(key)
        }
    }

    private fun requestNotificationPermission() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) checkAdBlockerList()
        }
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            UiConfig.K_HIDE_STATUSBAR -> { if (config.ui.hideStatusbar) fullscreenDelegate.hideStatusBar() else fullscreenDelegate.showStatusBar() }
            UiConfig.K_STATUSBAR_ENABLED,
            UiConfig.K_STATUSBAR_POSITION,
            UiConfig.K_STATUSBAR_ITEMS -> {
                statusbarViewController.refresh()
                if (!binding.appBar.isVisible && config.ui.statusbarEnabled) statusbarViewController.show()
                else statusbarViewController.hide()
            }
            UiConfig.K_TOOLBAR_ICONS_FOR_LARGE, UiConfig.K_TOOLBAR_ICONS -> composeToolbarViewController.updateIcons()
            TabConfig.K_SHOW_TAB_BAR -> composeToolbarViewController.showTabbar(config.tab.shouldShowTabBar)
            DisplayConfig.K_FONT_TYPE -> { if (config.display.fontType == FontType.SYSTEM_DEFAULT) ebWebView.reload() else ebWebView.updateCssStyle() }
            DisplayConfig.K_READER_FONT_TYPE -> { if (config.display.readerFontType == FontType.SYSTEM_DEFAULT) ebWebView.reload() else ebWebView.updateCssStyle() }
            DisplayConfig.K_FONT_SIZE -> ebWebView.settings.textZoom = config.display.fontSize
            DisplayConfig.K_READER_FONT_SIZE -> { if (ebWebView.shouldUseReaderFont()) ebWebView.settings.textZoom = config.display.readerFontSize }
            DisplayConfig.K_BOLD_FONT -> { composeToolbarViewController.updateIcons(); if (config.display.boldFontStyle) ebWebView.updateCssStyle() else ebWebView.reload() }
            DisplayConfig.K_BLACK_FONT -> { composeToolbarViewController.updateIcons(); if (config.display.blackFontStyle) ebWebView.updateCssStyle() else ebWebView.reload() }
            DisplayConfig.K_ENABLE_IMAGE_ADJUSTMENT -> ebWebView.reload()
            DisplayConfig.K_CUSTOM_FONT -> { if (config.display.fontType == FontType.CUSTOM) ebWebView.updateCssStyle() }
            DisplayConfig.K_READER_CUSTOM_FONT -> { if (config.display.readerFontType == FontType.CUSTOM && ebWebView.shouldUseReaderFont()) ebWebView.updateCssStyle() }
            ConfigManager.K_IS_INCOGNITO_MODE -> {
                ebWebView.incognito = config.isIncognitoMode
                composeToolbarViewController.updateIcons()
                EBToast.showShort(this, "Incognito mode is " + if (config.isIncognitoMode) "enabled." else "disabled.")
            }
            UiConfig.K_KEEP_AWAKE -> { if (config.ui.keepAwake) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            BrowserConfig.K_DESKTOP -> { ebWebView.updateUserAgentString(); ebWebView.reload(); composeToolbarViewController.updateIcons() }
            DisplayConfig.K_DARK_MODE -> {
                AppCompatDelegate.setDefaultNightMode(
                    when (config.display.darkMode) {
                        DarkMode.FORCE_ON -> AppCompatDelegate.MODE_NIGHT_YES
                        DarkMode.DISABLED -> AppCompatDelegate.MODE_NIGHT_NO
                        DarkMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                browserContainer.list().forEach { (it as? EBWebView)?.updateDarkMode() }
                config.restartChanged = true
            }
            UiConfig.K_TOOLBAR_TOP -> ViewUnit.updateAppbarPosition(binding)
            UiConfig.K_TOOLBAR_POSITION -> { composeToolbarViewController.updateIcons(); ViewUnit.updateAppbarPosition(binding) }
            UiConfig.K_NAV_POSITION -> fabImageViewController.applyFabPosition()
            TtsConfig.K_TTS_SPEED_VALUE -> ttsViewModel.setSpeechRate(config.tts.ttsSpeedValue / 100f)
            BrowserConfig.K_CUSTOM_USER_AGENT, BrowserConfig.K_ENABLE_CUSTOM_USER_AGENT -> { ebWebView.updateUserAgentString(); ebWebView.reload() }
            BrowserConfig.K_ENABLE_PULL_TO_REFRESH -> { browserState.swipeRefreshLayout.isEnabled = config.browser.enablePullToRefresh }
            TouchConfig.K_ENABLE_TOUCH -> { composeToolbarViewController.updateIcons(); chromeSetupDelegate.touchController?.toggleTouchPageTurn(config.touch.enableTouchTurn) }
            TouchConfig.K_TOUCH_AREA_ACTION_SWITCH -> composeToolbarViewController.updateIcons()
            AiConfig.K_GPT_ACTION_ITEMS -> actionModeMenuViewModel.updateMenuInfos(this, translationViewModel)
        }
    }

    companion object {
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
        const val ACTION_READ_ALOUD = "action_read_aloud"
    }
}
