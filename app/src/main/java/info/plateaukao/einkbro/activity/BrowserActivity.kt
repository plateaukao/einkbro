package info.plateaukao.einkbro.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.Request
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
import android.graphics.PorterDuff
import android.graphics.Rect
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
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.search.suggestion.SearchSuggestionViewModel
import info.plateaukao.einkbro.service.ClearService
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.BrowserUnit.createDownloadReceiver
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.pruneWebTitle
import info.plateaukao.einkbro.view.TranslationPanelView
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MultitouchListener
import info.plateaukao.einkbro.view.SwipeTouchListener
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.SendLinkDialog
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
import info.plateaukao.einkbro.view.compose.ComposedSearchBar
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.BookmarksDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FastToggleDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontBoldnessDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontBrowserDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.FontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.MenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ReaderFontDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.activity.delegates.ActionModeDelegate
import info.plateaukao.einkbro.activity.delegates.AiChatDelegate
import info.plateaukao.einkbro.activity.delegates.ContextMenuDelegate
import info.plateaukao.einkbro.activity.delegates.FileHandlingDelegate
import info.plateaukao.einkbro.activity.delegates.FullscreenDelegate
import info.plateaukao.einkbro.activity.delegates.InputBarDelegate
import info.plateaukao.einkbro.activity.delegates.IntentDispatchDelegate
import info.plateaukao.einkbro.activity.delegates.TabManager
import info.plateaukao.einkbro.activity.delegates.TranslationDelegate
import info.plateaukao.einkbro.view.handlers.GestureHandler
import info.plateaukao.einkbro.view.handlers.MenuActionHandler
import info.plateaukao.einkbro.view.handlers.ToolbarActionHandler
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.view.viewControllers.TouchAreaViewController
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
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt


open class BrowserActivity : FragmentActivity(), BrowserController {
    private lateinit var progressBar: ProgressBar
    protected lateinit var ebWebView: EBWebView
    protected open var shouldRunClearService: Boolean = true

    // Layouts
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var translationPanelView: TranslationPanelView

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
    private lateinit var touchController: TouchAreaViewController
    private lateinit var twoPaneController: TwoPaneController
    private lateinit var overviewDialogController: OverviewDialogController
    private lateinit var fabImageViewController: FabImageViewController
    private var currentAlbumController: AlbumController? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private var searchOnSite = false
    private var longPressPoint: Point = Point(0, 0)

    private val adFilter: AdFilter = AdFilter.get()
    private val filterViewModel: FilterViewModel = adFilter.viewModel
    private val browserContainer: BrowserContainer = BrowserContainer()
    private lateinit var binding: MainActivityLayout

    private val keyHandler: KeyHandler by lazy { KeyHandler(this, ebWebView, config) }
    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private val gestureHandler: GestureHandler by lazy { GestureHandler { dispatch(it) } }
    private val toolbarActionHandler: ToolbarActionHandler by lazy { ToolbarActionHandler(this) { dispatch(it) } }
    private val menuActionHandler: MenuActionHandler by lazy { MenuActionHandler(this, { dispatch(it) }) { ebWebView } }
    private val externalSearchWebView: WebView by lazy { BrowserUnit.createNaverDictWebView(this) }

    private var uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED
    private var orientation: Int = 0
    private var isRunning = false
    private var fabImagePositionChanged = false

    // ── Delegates ──────────────────────────────────────────────────────────

    private val fullscreenDelegate: FullscreenDelegate by lazy {
        FullscreenDelegate(
            activity = this,
            config = config,
            bindingProvider = { binding },
            webViewProvider = { ebWebView },
            currentAlbumControllerProvider = { currentAlbumController },
            composeToolbarViewControllerProvider = { composeToolbarViewController },
            fabImageViewControllerProvider = { fabImageViewController },
            searchOnSiteProvider = { searchOnSite },
            searchPanelHideAction = { searchOnSite = false; ViewUnit.hideKeyboard(this) },
        )
    }

    private val aiChatDelegate: AiChatDelegate by lazy {
        AiChatDelegate(
            activity = this,
            config = config,
            translationViewModel = translationViewModel,
            webViewProvider = { ebWebView },
            twoPaneControllerProvider = { twoPaneController },
            isTwoPaneControllerInitialized = { isTwoPaneControllerInitialized() },
            maybeInitTwoPaneController = { maybeInitTwoPaneController() },
            addAlbum = { title, _ -> addAlbum(title) },
            showTranslationDialog = { isWholePageMode -> translationDelegate.showTranslationDialog(isWholePageMode) },
        )
    }

    private val contextMenuDelegate: ContextMenuDelegate by lazy {
        ContextMenuDelegate(
            activity = this,
            config = config,
            ttsViewModel = ttsViewModel,
            webViewProvider = { ebWebView },
            addAlbum = { title, url, foreground -> addAlbum(title, url, foreground) },
            prepareRecord = { prepareRecord() },
            saveBookmark = { url, title -> saveBookmark(url, title) },
            toggleSplitScreen = { url -> toggleSplitScreen(url) },
            summarizeLinkContent = { url -> aiChatDelegate.summarizeLinkContent(url) },
            translateImage = { url -> translationDelegate.translateImage(url) },
            translateAllImages = { url -> translationDelegate.translateAllImages(url) },
            saveFile = { url, fileName -> saveFile(url, fileName) },
        )
    }

    private val inputBarDelegate: InputBarDelegate by lazy {
        InputBarDelegate(
            activity = this,
            config = config,
            bookmarkManager = bookmarkManager,
            searchSuggestionViewModel = searchSuggestionViewModel,
            bindingProvider = { binding },
            webViewProvider = { ebWebView },
            composeToolbarViewControllerProvider = { composeToolbarViewController },
            updateAlbum = { url -> updateAlbum(url) },
            showToolbar = { fullscreenDelegate.showToolbar() },
            toggleFullscreen = { toggleFullscreen() },
        )
    }

    private val intentDispatchDelegate: IntentDispatchDelegate by lazy {
        IntentDispatchDelegate(
            activity = this,
            config = config,
            externalSearchViewModel = externalSearchViewModel,
            remoteConnViewModel = remoteConnViewModel,
            translationViewModel = translationViewModel,
            webViewProvider = { ebWebView },
            currentAlbumControllerProvider = { currentAlbumController },
            overviewDialogControllerProvider = { overviewDialogController },
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
            actionModeMenuViewModel = actionModeMenuViewModel,
            translationViewModel = translationViewModel,
            ttsViewModel = ttsViewModel,
            splitSearchViewModel = splitSearchViewModel,
            remoteConnViewModel = remoteConnViewModel,
            externalSearchViewModel = externalSearchViewModel,
            bookmarkManager = bookmarkManager,
            bindingProvider = { binding },
            webViewProvider = { ebWebView },
            getFocusedWebView = { getFocusedWebView() },
            longPressPointProvider = { longPressPoint },
            updateLongPressPoint = { longPressPoint = it },
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
            translationViewModel = translationViewModel,
            actionModeMenuViewModel = actionModeMenuViewModel,
            webViewProvider = { ebWebView },
            focusedWebViewProvider = { getFocusedWebView() },
            externalSearchWebViewProvider = { externalSearchWebView },
            twoPaneControllerProvider = { twoPaneController },
            isTwoPaneControllerInitialized = { isTwoPaneControllerInitialized() },
            maybeInitTwoPaneController = { maybeInitTwoPaneController() },
            addAlbum = { tabManager.addAlbum() },
        )
    }

    val tabManager: TabManager by lazy {
        TabManager(
            activity = this,
            config = config,
            browserContainer = browserContainer,
            albumViewModel = albumViewModel,
            bookmarkManager = bookmarkManager,
            externalSearchViewModel = externalSearchViewModel,
            webViewProvider = { ebWebView },
            currentAlbumControllerProvider = { currentAlbumController },
            setCurrentAlbumController = { currentAlbumController = it },
            setEbWebView = { ebWebView = it },
            mainContentLayoutProvider = { mainContentLayout },
            progressBarProvider = { progressBar },
            composeToolbarViewControllerProvider = { composeToolbarViewController },
            fabImageViewControllerProvider = { fabImageViewController },
            createWebView = { createebWebView() },
            createTouchListener = { createMultiTouchTouchListener(it) },
            keyHandlerSetWebView = { keyHandler.setWebView(it) },
            addHistoryAction = { title, url -> addHistory(title, url) },
            adFilterProvider = { adFilter },
            updateLanguageLabel = { translationDelegate.updateLanguageLabel() },
        )
    }

    val fileHandlingDelegate: FileHandlingDelegate by lazy {
        FileHandlingDelegate(
            activity = this,
            webViewProvider = { ebWebView },
            bookmarkManager = bookmarkManager,
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
            isAudioOnlyMode = { if (::ebWebView.isInitialized) ebWebView.isAudioOnlyMode else false },
        )
    }

    // ── BrowserAction dispatch ─────────────────────────────────────────────

    fun dispatch(action: BrowserAction) = when (action) {
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
        is BrowserAction.ShowEpubDialog -> showEpubDialog()
        is BrowserAction.SavePageForLater -> savePageForLater()
        is BrowserAction.ShowSavedPages -> showSavedPages()
        is BrowserAction.SaveWebArchive -> showWebArchiveFilePicker()
        is BrowserAction.FocusOnInput -> focusOnInput()
        is BrowserAction.ShowMenuDialog -> showMenuDialog()
        is BrowserAction.ShowFastToggleDialog -> showFastToggleDialog()
        is BrowserAction.ShowTocDialog -> showTocDialog()
        is BrowserAction.RotateScreen -> rotateScreen()
        is BrowserAction.ToggleAudioOnlyMode -> toggleAudioOnlyMode()
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
    override fun updatePageInfo(info: String) = composeToolbarViewController.updatePageInfo(info)
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
    override fun toggleTouchTurnPageFeature() = config::enableTouchTurn.toggle()
    override fun toggleSwitchTouchAreaAction() = config::switchTouchAreaAction.toggle()

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) = fullscreenDelegate.onShowCustomView(view, callback)
    override fun onHideCustomView(): Boolean = fullscreenDelegate.onHideCustomView()
    override fun toggleFullscreen() = fullscreenDelegate.toggleFullscreen()
    override fun showOverview() = overviewDialogController.show()
    override fun hideOverview() = overviewDialogController.hide()
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
    override fun showOpenEpubFilePicker() = fileHandlingDelegate.showOpenEpubFilePicker()
    override fun savePageForLater() = fileHandlingDelegate.savePageForLater()
    override fun showSavedPages() = fileHandlingDelegate.showSavedPages()

    override fun onLongPress(message: Message, event: MotionEvent?) = contextMenuDelegate.onLongPress(message, event)
    override fun handleKeyEvent(event: KeyEvent): Boolean = keyHandler.handleKeyEvent(event)
    override fun focusOnInput() = inputBarDelegate.focusOnInput()

    override fun summarizeContent() = aiChatDelegate.summarizeContent()
    override fun chatWithWeb(useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) = aiChatDelegate.chatWithWeb(useSplitScreen, content, runWithAction)
    override fun showPageAiActionMenu() = aiChatDelegate.showPageAiActionMenu()

    override fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float) = actionModeDelegate.updateSelectionRect(left, top, right, bottom)
    override fun isActionModeActive(): Boolean = actionModeDelegate.isActionModeActive()
    override fun dismissActionMode() = actionModeDelegate.dismissActionMode()

    override fun handleBackKey() {
        ViewUnit.hideKeyboard(this)
        if (overviewDialogController.isVisible()) hideOverview()
        if (fullscreenDelegate.fullscreenHolder != null) {
            onHideCustomView()
        } else if (!binding.appBar.isVisible && config.showToolbarFirst) {
            fullscreenDelegate.showToolbar()
        } else if (!composeToolbarViewController.isDisplayed()) {
            composeToolbarViewController.show()
        } else {
            if (!ebWebView.isTranslatePage && ebWebView.canGoBack()) {
                ebWebView.goBack()
            } else {
                if (config.closeTabWhenNoMoreBackHistory) removeAlbum()
                else EBToast.show(this, R.string.no_previous_page)
            }
        }
    }

    override fun goForward() {
        if (ebWebView.canGoForward()) ebWebView.goForward()
        else EBToast.show(this, R.string.toast_webview_forward)
    }

    override fun saveBookmark(url: String?, title: String?) {
        val currentUrl = url ?: ebWebView.url ?: return
        val nonNullTitle = title ?: HelperUnit.secString(ebWebView.title)
        try {
            BookmarkEditDialog(
                bookmarkViewModel,
                Bookmark(nonNullTitle.pruneWebTitle(), currentUrl, order = if (ViewUnit.isWideLayout(this)) 999 else 0),
                { ViewUnit.hideKeyboard(this); EBToast.show(this, R.string.toast_edit_successful) },
                { ViewUnit.hideKeyboard(this) }
            ).show(supportFragmentManager, "bookmark_edit")
        } catch (e: Exception) {
            e.printStackTrace()
            EBToast.show(this, R.string.toast_error)
        }
    }

    override fun createShortcut() = BrowserUnit.createShortcut(this, ebWebView)

    override fun showSearchPanel() {
        searchOnSite = true
        fabImageViewController.hide()
        binding.mainSearchPanel.visibility = VISIBLE
        binding.mainSearchPanel.post { searchPanelFocusRequester.requestFocus(); ViewUnit.showKeyboard(this) }
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        ViewUnit.showKeyboard(this)
    }

    override fun showFontSizeChangeDialog() {
        if (ebWebView.shouldUseReaderFont()) {
            ReaderFontDialogFragment { openCustomFontPicker() }.show(supportFragmentManager, "font_dialog")
        } else {
            FontDialogFragment { openCustomFontPicker() }.show(supportFragmentManager, "font_dialog")
        }
    }

    override fun showFontBoldnessDialog() {
        FontBoldnessDialogFragment(
            config.fontBoldness,
            okAction = { changedBoldness ->
                config.fontBoldness = changedBoldness
                ebWebView.applyFontBoldness()
            }
        ).show(supportFragmentManager, "FontBoldnessDialog")
    }

    override fun increaseFontSize() {
        val fontSize = if (ebWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
        changeFontSize(fontSize + 20)
    }

    override fun decreaseFontSize() {
        val fontSize = if (ebWebView.shouldUseReaderFont()) config.readerFontSize else config.fontSize
        if (fontSize > 50) changeFontSize(fontSize - 20)
    }

    override fun invertColors() {
        val hasInvertedColor = config.toggleInvertedColor(ebWebView.url.orEmpty())
        ViewUnit.invertColor(ebWebView, hasInvertedColor)
    }

    override fun shareLink() = IntentUnit.share(this, ebWebView.title, ebWebView.url)

    override fun sendToRemote(text: String) {
        if (remoteConnViewModel.isSendingTextSearch) {
            remoteConnViewModel.toggleTextSearch()
            EBToast.show(this, R.string.send_to_remote_terminate)
            return
        }
        SendLinkDialog(this, lifecycleScope).show(text)
    }

    override fun toggleReceiveTextSearch() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
        } else {
            remoteConnViewModel.toggleReceiveLink {
                if (it.startsWith("action")) {
                    val (_, content, actionString) = it.split("|||")
                    val action = Json.decodeFromString<ChatGPTActionInfo>(actionString)
                    if (action.display == info.plateaukao.einkbro.preference.GptActionDisplay.Popup) {
                        translationViewModel.setupGptAction(action)
                        translationViewModel.updateMessageWithContext(content)
                        translationViewModel.updateInputMessage(content)
                        translationDelegate.showTranslationDialog(action.scope == info.plateaukao.einkbro.preference.GptActionScope.WholePage)
                    } else {
                        chatWithWeb(false, content, action)
                    }
                } else {
                    ebWebView.loadUrl(it)
                }
            }
        }
    }

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

    override fun addToInstapaper() {
        val url = ebWebView.url.orEmpty()
        if (url.isEmpty()) { EBToast.show(this, R.string.url_empty); return }
        instapaperViewModel.addUrl(url = url, username = config.instapaperUsername, password = config.instapaperPassword, title = ebWebView.title)
    }

    override fun configureInstapaper() { dialogManager.showInstapaperCredentialsDialog { _, _ -> Unit } }

    override fun handleTtsButton() {
        if (ttsViewModel.isReading()) TtsSettingDialogFragment().show(supportFragmentManager, "TtsSettingDialog")
        else readArticle()
    }

    override fun showTtsLanguageDialog() { TtsLanguageDialog(this).show(ttsViewModel.getAvailableLanguages()) }

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
        if (config.twoPanelLinkHere && isTwoPaneControllerInitialized() && twoPaneController.isSecondPaneDisplayed()) {
            toggleSplitScreen(url); true
        } else false

    override fun showFastToggleDialog() {
        if (!this::ebWebView.isInitialized) return
        FastToggleDialogFragment { ebWebView.initPreferences(); ebWebView.reload() }.show(supportFragmentManager, "fast_toggle_dialog")
    }

    override fun showMenuDialog() = MenuDialogFragment(
        ebWebView.url.orEmpty(), ttsViewModel.isReading(), ebWebView.isAudioOnlyMode,
        ebWebView.hasVideo, config.enableTouchTurn,
        { menuActionHandler.handle(it) }, { menuActionHandler.handleLongClick(it) }
    ).show(supportFragmentManager, "menu_dialog")

    override fun showTouchAreaDialog() = TouchAreaDialogFragment(ebWebView.url.orEmpty()).show(supportFragmentManager, "TouchAreaDialog")

    override fun openHistoryPage(amount: Int) = overviewDialogController.openHistoryPage(amount)

    override fun openBookmarkPage() = BookmarksDialogFragment(
        lifecycleScope, bookmarkViewModel,
        gotoUrlAction = { url -> updateAlbum(url) },
        bookmarkIconClickAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
        splitScreenAction = { url -> toggleSplitScreen(url) },
    ).show(supportFragmentManager, "bookmarks dialog")

    override fun updateTitle(title: String?) = updateTitle()
    override fun updateProgress(progress: Int) {
        progressBar.progress = progress
        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true)
            progressBar.visibility = VISIBLE
        } else {
            updateRefresh(false)
            progressBar.visibility = GONE
            swipeRefreshLayout.isRefreshing = false
            scrollChange()
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
        HelperUnit.applyTheme(this)
        setContentView(binding.root)

        orientation = resources.configuration.orientation

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        mainContentLayout = findViewById(R.id.main_content)
        translationPanelView = TranslationPanelView(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        binding.twoPanelLayout.addView(translationPanelView)

        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            !ebWebView.wasAtTopOnTouchStart || ebWebView.scrollY > 0 || !ebWebView.isInnerScrollAtTop
        }
        swipeRefreshLayout.setOnRefreshListener {
            if (currentAlbumController != null) ebWebView.reload()
            else swipeRefreshLayout.isRefreshing = false
        }
        ViewUnit.updateAppbarPosition(binding)
        initLaunchers()
        initToolbar()
        initSearchPanel()
        inputBarDelegate.initInputBar()
        initOverview()
        initTouchArea()
        actionModeDelegate.initActionModeViewModel()
        actionModeDelegate.setTwoPaneChecker { isTwoPaneControllerInitialized() && twoPaneController.isSecondPaneDisplayed() }
        initInstapaperViewModel()

        downloadReceiver = createDownloadReceiver(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
        }

        intentDispatchDelegate.dispatchIntent(intent)
        intentDispatchDelegate.shouldLoadTabState = false

        if (config.keepAwake) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        translationDelegate.initLanguageLabel()
        initTouchAreaViewController()
        initTextSearchButton()
        initExternalSearchCloseButton()
        translationDelegate.initTranslationViewModel()
        initTtsViewModel()

        if (config.hideStatusbar) fullscreenDelegate.hideStatusBar()

        handleWindowInsets()
        listenKeyboardShowHide()

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission()
        } else {
            binding.root.postDelayed({ checkAdBlockerList() }, 1000)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentDispatchDelegate.dispatchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (config.restartChanged) { config.restartChanged = false; dialogManager.showRestartConfirmDialog() }
        updateTitle()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (config.customFontChanged && (config.fontType == FontType.CUSTOM || config.readerFontType == FontType.CUSTOM)) {
            if (!ebWebView.shouldUseReaderFont()) ebWebView.reload() else ebWebView.updateCssStyle()
            config.customFontChanged = false
        }
        if (!config.continueMedia && this::ebWebView.isInitialized) ebWebView.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        actionModeDelegate.onPause()
        if (!config.continueMedia && !isMeetPipCriteria() && this::ebWebView.isInitialized) ebWebView.pauseTimers()
    }

    override fun onDestroy() {
        ttsViewModel.reset()
        tabManager.updateSavedAlbumInfo()
        if (config.clearWhenQuit && shouldRunClearService) startService(Intent(this, ClearService::class.java))
        browserContainer.clear()
        unregisterReceiver(downloadReceiver)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags != uiMode && config.darkMode == DarkMode.SYSTEM) recreate()
        }
        if (newConfig.orientation != orientation) {
            composeToolbarViewController.updateIcons()
            orientation = newConfig.orientation
            if (config.fabPosition == FabPosition.Custom) fabImageViewController.updateImagePosition(orientation)
        }
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

    private fun prepareRecord(): Boolean {
        val webView = currentAlbumController as EBWebView
        val title = webView.title
        val url = webView.url
        return (title.isNullOrEmpty() || url.isNullOrEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT))
    }

    private fun getFocusedWebView(): EBWebView = when {
        ebWebView.hasFocus() -> ebWebView
        isTwoPaneControllerInitialized() && twoPaneController.getSecondWebView().hasFocus() -> twoPaneController.getSecondWebView()
        else -> ebWebView
    }

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    private fun maybeInitTwoPaneController() {
        if (!isTwoPaneControllerInitialized()) {
            twoPaneController = TwoPaneController(
                this, lifecycleScope, translationPanelView, binding.twoPanelLayout,
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

    private fun changeFontSize(size: Int) {
        if (ebWebView.shouldUseReaderFont()) config.readerFontSize = size else config.fontSize = size
    }

    private fun openCustomFontPicker() {
        FontBrowserDialogFragment(isReaderMode = ebWebView.shouldUseReaderFont()).show(supportFragmentManager, "font_browser_dialog")
    }

    private fun updateTitle() {
        if (!this::ebWebView.isInitialized) return
        tabManager.updateTitle()
    }

    private fun updateRefresh(running: Boolean) {
        if (!isRunning && running) isRunning = true
        else if (isRunning && !running) isRunning = false
        composeToolbarViewController.updateRefresh(isRunning)
    }

    private fun isMeetPipCriteria() = config.enableVideoPip && fullscreenDelegate.fullscreenHolder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipMode() { enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }

    private fun initLaunchers() = fileHandlingDelegate.initLaunchers()
    private fun initTouchArea() = composeToolbarViewController.updateIcons()
    private fun initTouchAreaViewController() { touchController = TouchAreaViewController(binding.activityMainContent) { dispatch(it) } }

    protected fun readArticle() {
        lifecycleScope.launch { ttsViewModel.readArticle(ebWebView.getRawText(), ebWebView.title.orEmpty()) }
    }

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

    private fun saveFile(url: String, fileName: String = "") {
        if (url.startsWith("data:image")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BrowserUnit.saveImageFromUrl(url, fileHandlingDelegate.saveImageFilePickerLauncher)
            } else {
                EBToast.show(this, "Not supported dataUrl")
            }
            return
        }
        if (HelperUnit.needGrantStoragePermission(this)) return
        val source = Uri.parse(url)
        val request = DownloadManager.Request(source).apply {
            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
            addRequestHeader("User-Agent", WebSettings.getDefaultUserAgent(this@BrowserActivity))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            try { setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName) }
            catch (e: Exception) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                setDestinationUri(Uri.fromFile(File(downloadsDir, fileName)))
            }
        }
        (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        ViewUnit.hideKeyboard(this)
    }

    private fun scrollChange() {
        ebWebView.setScrollChangeListener(object : EBWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                ebWebView.updatePageInfo()
                if (::twoPaneController.isInitialized) twoPaneController.scrollChange(scrollY - oldScrollY)
                if (!config.shouldHideToolbar) return
                val height = floor(ebWebView.contentHeight * ebWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ebWebView.height
                val cutoff = height - webViewHeight - 112 * resources.displayMetrics.density.roundToInt()
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    if (binding.appBar.visibility == VISIBLE) toggleFullscreen()
                }
            }
        })
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
                if (config.enableDragUrlToAction && contextMenuDelegate.isInLongPressMode && contextMenuDelegate.activeContextMenuDialog != null) {
                    contextMenuDelegate.activeContextMenuDialog?.updateHoveredItem(motionEvent.rawX, motionEvent.rawY)
                    return
                }
                if (longPressStartPoint == null) { longPressStartPoint = Point(motionEvent.x.toInt(), motionEvent.y.toInt()); return }
                if (abs(motionEvent.x - (longPressStartPoint?.x ?: 0)) > ViewUnit.dpToPixel(15) ||
                    abs(motionEvent.y - (longPressStartPoint?.y ?: 0)) > ViewUnit.dpToPixel(15)) {
                    actionModeDelegate.actionModeViewRef?.visibility = INVISIBLE
                    longPressStartPoint = null
                }
            }
            override fun onMoveDone(motionEvent: MotionEvent) {
                if (contextMenuDelegate.isInLongPressMode && contextMenuDelegate.activeContextMenuDialog != null) {
                    contextMenuDelegate.activeContextMenuDialog?.onFingerLifted()
                    contextMenuDelegate.activeContextMenuDialog = null
                    contextMenuDelegate.isInLongPressMode = false
                    return
                }
            }
        }.apply { lifecycle.addObserver(this) }

    // ── Init methods ──────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun initToolbar() {
        progressBar = findViewById(R.id.main_progress_bar)
        if (config.darkMode == DarkMode.FORCE_ON) {
            val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) progressBar.progressTintMode = PorterDuff.Mode.LIGHTEN
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
        runOnUiThread { config.registerOnSharedPreferenceChangeListener(preferenceChangeListener) }
    }

    private fun initFAB() {
        fabImageViewController = FabImageViewController(
            orientation, findViewById(R.id.fab_imageButtonNav),
            { fullscreenDelegate.showToolbar() },
            longClickAction = {
                if (config.enableNavButtonGesture) gestureHandler.handle(config.navButtonLongClickGesture)
                else showFastToggleDialog()
            }
        )
    }

    private fun initOverview() {
        overviewDialogController = OverviewDialogController(
            this, albumViewModel.albums, albumViewModel.focusIndex, binding.layoutOverview,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            addIncognitoTabAction = { hideOverview(); addAlbum(getString(R.string.app_name), "", incognito = true); focusOnInput() },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            addEmptyTabAction = { newATab() }
        )
    }

    private val searchPanelFocusRequester = FocusRequester()
    private fun initSearchPanel() {
        binding.mainSearchPanel.apply {
            visibility = INVISIBLE
            setContent {
                MyTheme {
                    ComposedSearchBar(
                        focusRequester = searchPanelFocusRequester,
                        onTextChanged = { (currentAlbumController as EBWebView?)?.findAllAsync(it) },
                        onCloseClick = { hideSearchPanel() },
                        onUpClick = { searchUp(it) },
                        onDownClick = { searchDown(it) },
                    )
                }
            }
        }
    }

    private fun searchUp(text: String) {
        if (text.isEmpty()) { EBToast.show(this, getString(R.string.toast_input_empty)); return }
        ViewUnit.hideKeyboard(this); (currentAlbumController as EBWebView).findNext(false)
    }

    private fun searchDown(text: String) {
        if (text.isEmpty()) { EBToast.show(this, getString(R.string.toast_input_empty)); return }
        ViewUnit.hideKeyboard(this); (currentAlbumController as EBWebView).findNext(true)
    }

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

    private fun initTtsViewModel() {
        lifecycleScope.launch { ttsViewModel.readingState.collect { composeToolbarViewController.updateIcons() } }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initExternalSearchCloseButton() {
        binding.activityMainContent.externalSearchClose.setOnClickListener {
            moveTaskToBack(true); externalSearchViewModel.setButtonVisibility(false)
        }
        val externalSearchContainer = binding.activityMainContent.externalSearchActionContainer
        externalSearchViewModel.searchActions.forEach { action ->
            val button = TextView(this).apply {
                height = 40.dp.value.toInt(); textSize = 10.sp.value
                gravity = Gravity.CENTER
                background = getDrawable(R.drawable.background_with_border)
                text = action.title.take(2).uppercase(Locale.getDefault())
                setOnClickListener {
                    externalSearchViewModel.currentSearchAction = action
                    ebWebView.loadUrl(externalSearchViewModel.generateSearchUrl(splitSearchItemInfo = action))
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
        remoteTextSearch.setOnClickListener { remoteConnViewModel.reset() }
        lifecycleScope.launch {
            remoteConnViewModel.remoteConnected.collect { connected ->
                remoteTextSearch.setImageResource(
                    if (remoteConnViewModel.isSendingTextSearch) R.drawable.ic_send else R.drawable.ic_receive
                )
                remoteTextSearch.isVisible = connected
            }
        }
    }

    private fun initInstapaperViewModel() {
        lifecycleScope.launch {
            instapaperViewModel.uiState.collect { state ->
                if (state.showConfigureDialog) configureInstapaper()
                else if (state.successMessage != null) EBToast.show(this@BrowserActivity, state.successMessage)
                else if (state.errorMessage != null) EBToast.show(this@BrowserActivity, state.errorMessage)
                instapaperViewModel.resetState()
            }
        }
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insetsNavigationBar: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val insetsKeyboard: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as FrameLayout.LayoutParams
            if (config.hideStatusbar) {
                params.bottomMargin = when {
                    insetsKeyboard.bottom > 0 -> insetsKeyboard.bottom
                    insetsNavigationBar.bottom > 0 -> insetsNavigationBar.bottom
                    else -> 0
                }
                view.layoutParams = params
            }
            windowInsets
        }
    }

    private fun listenKeyboardShowHide() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (inputBarDelegate.isKeyboardDisplaying()) touchController.maybeDisableTemporarily()
            else touchController.maybeEnableAgain()

            @Suppress("DEPRECATION")
            val isFullscreen = (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && isFullscreen) {
                val rect = Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val params = binding.root.layoutParams as FrameLayout.LayoutParams
                if (keypadHeight > screenHeight * 0.15) {
                    if (params.bottomMargin != keypadHeight) { params.bottomMargin = keypadHeight; binding.root.layoutParams = params }
                } else {
                    if (params.bottomMargin != 0) { params.bottomMargin = 0; binding.root.layoutParams = params }
                }
            }
        }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            ConfigManager.K_HIDE_STATUSBAR -> { if (config.hideStatusbar) fullscreenDelegate.hideStatusBar() else fullscreenDelegate.showStatusBar() }
            ConfigManager.K_TOOLBAR_ICONS_FOR_LARGE, ConfigManager.K_TOOLBAR_ICONS -> composeToolbarViewController.updateIcons()
            ConfigManager.K_SHOW_TAB_BAR -> composeToolbarViewController.showTabbar(config.shouldShowTabBar)
            ConfigManager.K_FONT_TYPE -> { if (config.fontType == FontType.SYSTEM_DEFAULT) ebWebView.reload() else ebWebView.updateCssStyle() }
            ConfigManager.K_READER_FONT_TYPE -> { if (config.readerFontType == FontType.SYSTEM_DEFAULT) ebWebView.reload() else ebWebView.updateCssStyle() }
            ConfigManager.K_FONT_SIZE -> ebWebView.settings.textZoom = config.fontSize
            ConfigManager.K_READER_FONT_SIZE -> { if (ebWebView.shouldUseReaderFont()) ebWebView.settings.textZoom = config.readerFontSize }
            ConfigManager.K_BOLD_FONT -> { composeToolbarViewController.updateIcons(); if (config.boldFontStyle) ebWebView.updateCssStyle() else ebWebView.reload() }
            ConfigManager.K_BLACK_FONT -> { composeToolbarViewController.updateIcons(); if (config.blackFontStyle) ebWebView.updateCssStyle() else ebWebView.reload() }
            ConfigManager.K_ENABLE_IMAGE_ADJUSTMENT -> ebWebView.reload()
            ConfigManager.K_CUSTOM_FONT -> { if (config.fontType == FontType.CUSTOM) ebWebView.updateCssStyle() }
            ConfigManager.K_READER_CUSTOM_FONT -> { if (config.readerFontType == FontType.CUSTOM && ebWebView.shouldUseReaderFont()) ebWebView.updateCssStyle() }
            ConfigManager.K_IS_INCOGNITO_MODE -> {
                ebWebView.incognito = config.isIncognitoMode
                composeToolbarViewController.updateIcons()
                EBToast.showShort(this, "Incognito mode is " + if (config.isIncognitoMode) "enabled." else "disabled.")
            }
            ConfigManager.K_KEEP_AWAKE -> { if (config.keepAwake) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            ConfigManager.K_DESKTOP -> { ebWebView.updateUserAgentString(); ebWebView.reload(); composeToolbarViewController.updateIcons() }
            ConfigManager.K_DARK_MODE -> config.restartChanged = true
            ConfigManager.K_TOOLBAR_TOP -> ViewUnit.updateAppbarPosition(binding)
            ConfigManager.K_TOOLBAR_POSITION -> { composeToolbarViewController.updateIcons(); ViewUnit.updateAppbarPosition(binding) }
            ConfigManager.K_NAV_POSITION -> config.restartChanged = true
            ConfigManager.K_TTS_SPEED_VALUE -> ttsViewModel.setSpeechRate(config.ttsSpeedValue / 100f)
            ConfigManager.K_CUSTOM_USER_AGENT, ConfigManager.K_ENABLE_CUSTOM_USER_AGENT -> { ebWebView.updateUserAgentString(); ebWebView.reload() }
            ConfigManager.K_ENABLE_TOUCH -> { composeToolbarViewController.updateIcons(); touchController.toggleTouchPageTurn(config.enableTouchTurn) }
            ConfigManager.K_TOUCH_AREA_ACTION_SWITCH -> composeToolbarViewController.updateIcons()
            ConfigManager.K_GPT_ACTION_ITEMS -> actionModeMenuViewModel.updateMenuInfos(this, translationViewModel)
        }
    }

    companion object {
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
        const val ACTION_READ_ALOUD = "action_read_aloud"
    }
}
