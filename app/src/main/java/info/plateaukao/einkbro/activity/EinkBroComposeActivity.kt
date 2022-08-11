package info.plateaukao.einkbro.activity

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.*
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.model.ToolbarViewModel
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.databinding.EinkbroMainLayoutBinding
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.AlbumInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.unit.*
import info.plateaukao.einkbro.unit.HelperUnit.toNormalScheme
import info.plateaukao.einkbro.unit.ViewUnit.hideKeyboard
import info.plateaukao.einkbro.view.MultitouchListener
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.TwoPaneLayout
import info.plateaukao.einkbro.view.compose.ComposedToolbar
import info.plateaukao.einkbro.view.compose.HistoryAndTabsView
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.SendLinkDialog
import info.plateaukao.einkbro.view.dialog.compose.*
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.toolbaricons.ToolbarActionInfo
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModelFactory
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

open class EinkBroActivity : FragmentActivity(), BrowserController {
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()
    private val epubManager: EpubManager by lazy { EpubManager(this) }
    private val bookmarkManager: BookmarkManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(this) }
    private val recordDb: RecordDb by lazy { RecordDb(this).apply { open(false) } }

    private val toolbarViewModel: ToolbarViewModel by viewModels()
    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager.bookmarkDao)
    }

    private lateinit var overviewDialogController: OverviewDialogController

    private lateinit var _einkBroMainLayoutBinding: EinkbroMainLayoutBinding

    protected lateinit var ninjaWebView: NinjaWebView
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var layoutOverview: View

    private var currentAlbumController: AlbumController? = null
    private var shouldLoadTabState: Boolean = false

    private var keepToolbar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _einkBroMainLayoutBinding = EinkbroMainLayoutBinding.inflate(layoutInflater)
        mainContentLayout = _einkBroMainLayoutBinding.activityMainContent.mainContent
        layoutOverview = HistoryAndTabsView(this)

        setContent {
            MyTheme {
                val toolbarInfoList by toolbarViewModel.toolbarActionInfoListLiveData.observeAsState()
                EinkBroScreen(
                    modifier = Modifier.fillMaxWidth(),
                    _einkBroMainLayoutBinding.root,
                    layoutOverview,
                    toolbarInfoList ?: emptyList(),
                    onToolbarItemClick = this::onToolActionClick
                )
            }
        }

        initOverview()

        dispatchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatchIntent(intent)
    }

    open fun dispatchIntent(intent: Intent) {
//        if (overviewDialogController.isVisible()) {
//            overviewDialogController.hide()
//        }

        when (intent.action) {
            "", Intent.ACTION_MAIN -> { // initial case
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
                        Log.w(TAG, "savedIndex:$savedIndex")
                        Log.w(TAG, "albumList:$albumList")
                        albumList.forEachIndexed { index, albumInfo ->
                            addAlbum(
                                title = albumInfo.title,
                                url = albumInfo.url,
                                foreground = (index == savedIndex)
                            )
                        }
                    } else {
                        addAlbum()
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                // if webview for that url already exists, show the original tab, otherwise, create new
                val viewUri = intent.data?.toNormalScheme() ?: return
                if (viewUri.scheme == "content") {
                    epubManager.showEpubReader(viewUri)
                    finish()
                } else {
                    val url = viewUri.toString()
                    getUrlMatchedBrowser(url)?.let { showAlbum(it) } ?: addAlbum(url = url)
                }
            }
            Intent.ACTION_WEB_SEARCH -> addAlbum(
                url = intent.getStringExtra(SearchManager.QUERY) ?: ""
            )
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
                addAlbum(url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: return
                val url = config.customProcessTextUrl + text
                if (this::ninjaWebView.isInitialized) {
                    ninjaWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
            }
            "colordict.intent.action.PICK_RESULT",
            "colordict.intent.action.SEARCH" -> {
                val text = intent.getStringExtra("EXTRA_QUERY") ?: return
                val url = config.customProcessTextUrl + text
                if (this::ninjaWebView.isInitialized) {
                    ninjaWebView.loadUrl(url)
                } else {
                    addAlbum(url = url)
                }
            }
            else -> {
                addAlbum()
            }
        }
        getIntent().action = ""
    }

    protected open fun onToolActionClick(toolbarAction: ToolbarAction) {
        when (toolbarAction) {
            ToolbarAction.Title -> focusOnInput()
            ToolbarAction.Back -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                NinjaToast.show(this, getString(R.string.no_previous_page))
            }
            ToolbarAction.Refresh -> refreshAction()
            ToolbarAction.Touch -> toggleTouchTurnPageFeature()
            ToolbarAction.PageUp -> ninjaWebView.pageUpWithNoAnimation()
            ToolbarAction.PageDown -> {
                keepToolbar = true
                ninjaWebView.pageDownWithNoAnimation()
            }
            ToolbarAction.TabCount -> showOverview()
            ToolbarAction.Font -> showFontSizeChangeDialog()
            ToolbarAction.Settings -> showMenuDialog()
            ToolbarAction.Bookmark -> openBookmarkPage()
            ToolbarAction.IconSetting -> ToolbarConfigDialogFragment().show(
                supportFragmentManager,
                "toolbar_config"
            )
            ToolbarAction.VerticalLayout -> ninjaWebView.toggleVerticalRead()
            ToolbarAction.ReaderMode -> ninjaWebView.toggleReaderMode()
            ToolbarAction.BoldFont -> config.boldFontStyle = !config.boldFontStyle
            ToolbarAction.IncreaseFont -> increaseFontSize()
            ToolbarAction.DecreaseFont -> decreaseFontSize()
            ToolbarAction.FullScreen -> fullscreen()
            ToolbarAction.Forward -> if (ninjaWebView.canGoForward()) ninjaWebView.goForward()
            ToolbarAction.RotateScreen -> rotateScreen()
            ToolbarAction.Translation -> showTranslation()
            ToolbarAction.CloseTab -> removeAlbum(currentAlbumController!!)
            ToolbarAction.InputUrl -> focusOnInput()
            ToolbarAction.NewTab -> {
                addAlbum(getString(R.string.app_name), "", true)
                focusOnInput()
            }
            ToolbarAction.Desktop -> config.desktop = !config.desktop
            ToolbarAction.Search -> showSearchPanel()
            else -> {}
        }
    }

    private fun initOverview() {
        overviewDialogController = OverviewDialogController(
            this,
            layoutOverview as HistoryAndTabsView,
            recordDb,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            addIncognitoTabAction = {
                hideOverview()
                addAlbum(getString(R.string.app_name), "", incognito = true)
                focusOnInput()
            },
            onHistoryChanged = { },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            addEmptyTabAction = { addAlbum(getString(R.string.app_name), ""); focusOnInput() }
        )
        overviewDialogController.hide()
    }

    private fun showSearchPanel() {
        TODO("Not yet implemented")
    }

    private fun showTranslation() {
        TODO("Not yet implemented")
    }

    private var isRotated: Boolean = false
    private fun rotateScreen() {
        isRotated = !isRotated
        requestedOrientation = if (!isRotated) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun fullscreen() {
//        if (!searchOnSite) {
//            if (config.fabPosition != FabPosition.NotShow) {
//                fabImageButtonNav.visibility = View.VISIBLE
//            }
//            searchPanel.visibility = View.INVISIBLE
//            binding.appBar.visibility = View.GONE
//            hideStatusBar()
//        }
        TODO("Not yet implemented")
    }

    private fun changeFontSize(size: Int) {
        config.fontSize = size
    }

    private fun increaseFontSize() = changeFontSize(config.fontSize + 20)

    private fun decreaseFontSize() {
        if (config.fontSize > 50) changeFontSize(config.fontSize - 20)
    }

    private fun showMenuDialog() {
        MenuDialogFragment { handleMenuItem(it) }.show(supportFragmentManager, "menu_dialog")
    }

    private fun handleMenuItem(menuItemType: MenuItemType) {
        when (menuItemType) {
            MenuItemType.QuickToggle -> showFastToggleDialog()
            MenuItemType.OpenHome -> updateAlbum(
                sp.getString(
                    "favoriteURL",
                    "https://github.com/plateaukao/browser"
                )
            )
            MenuItemType.CloseTab -> currentAlbumController?.let { removeAlbum(it) }
            MenuItemType.Quit -> finishAndRemoveTask()

            MenuItemType.SplitScreen -> toggleSplitScreen()
            MenuItemType.Translate -> showTranslation()
            MenuItemType.VerticalRead -> ninjaWebView.toggleVerticalRead()
            MenuItemType.ReaderMode -> ninjaWebView.toggleReaderMode()
            MenuItemType.TouchSetting -> TouchAreaDialogFragment().show(
                supportFragmentManager,
                "TouchAreaDialog"
            )
            MenuItemType.ToolbarSetting -> ToolbarConfigDialogFragment().show(
                supportFragmentManager,
                "toolbar_config"
            )

            MenuItemType.ReceiveData -> showReceiveDataDialog()
            MenuItemType.SendLink -> SendLinkDialog(
                this,
                lifecycleScope
            ).show(ninjaWebView.url.orEmpty())
            MenuItemType.ShareLink -> IntentUnit.share(this, ninjaWebView.title, ninjaWebView.url)
            MenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                this,
                ninjaWebView.url,
                getString(R.string.menu_open_with)
            )
            MenuItemType.CopyLink -> ShareUtil.copyToClipboard(this, ninjaWebView.url ?: "")
            MenuItemType.Shortcut -> HelperUnit.createShortcut(
                this,
                ninjaWebView.title,
                ninjaWebView.url,
                ninjaWebView.favicon
            )

            MenuItemType.SetHome -> config.favoriteUrl = ninjaWebView.url.orEmpty()
            MenuItemType.SaveBookmark -> saveBookmark()
            MenuItemType.OpenEpub -> openSavedEpub()
            MenuItemType.SaveEpub -> showSaveEpubDialog()
            MenuItemType.SavePdf -> printPDF()

            MenuItemType.FontSize -> showFontSizeChangeDialog()
            MenuItemType.WhiteBknd -> config.whiteBackground = !config.whiteBackground
            MenuItemType.BoldFont -> config.boldFontStyle = !config.boldFontStyle
            MenuItemType.Search -> showSearchPanel()
            MenuItemType.Download -> BrowserUnit.openDownloadFolder(this)
            MenuItemType.Settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun printPDF() {
    }

    private fun showSaveEpubDialog() {
    }

    private fun openSavedEpub() = if (config.savedEpubFileInfos.isEmpty()) {
        NinjaToast.show(this, "no saved epub!")
    } else {
        dialogManager.showSaveEpubDialog(shouldAddNewEpub = false) { uri ->
            HelperUnit.openFile(this@EinkBroActivity, uri ?: return@showSaveEpubDialog)
        }
    }

    private fun saveBookmark(url: String? = null, title: String? = null) {
        val currentUrl = url ?: ninjaWebView.url ?: return
        val nonNullTitle = title ?: HelperUnit.secString(ninjaWebView.title)
        try {
            lifecycleScope.launch {
                BookmarkEditDialog(
                    this@EinkBroActivity,
                    bookmarkManager,
                    Bookmark(nonNullTitle, currentUrl),
                    {
                        hideKeyboard()
                        NinjaToast.show(this@EinkBroActivity, R.string.toast_edit_successful)
                    },
                    { hideKeyboard() }
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(this, R.string.toast_error)
        }
    }

    private fun hideKeyboard() = ViewUnit.hideKeyboard(this)

    private fun showReceiveDataDialog() {
        ReceiveDataDialog(this, lifecycleScope).show { text ->
            if (text.startsWith("http")) ninjaWebView.loadUrl(text)
            else {
                val clip = ClipData.newPlainText("Copied Text", text)
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    clip
                )
                NinjaToast.show(this, "String is Copied!")
            }
        }
    }

    private fun showFastToggleDialog() {
        if (!this::ninjaWebView.isInitialized) return

        FastToggleDialogFragment {
            ninjaWebView.initPreferences()
            ninjaWebView.reload()
        }.show(supportFragmentManager, "fast_toggle_dialog")
    }

    private fun showFontSizeChangeDialog() =
        FontDialogFragment { openCustomFontPicker() }.show(supportFragmentManager, "font_dialog")

    private val customFontResultLauncher: ActivityResultLauncher<Intent> =
        BrowserUnit.registerCustomFontSelectionResult(this)

    private fun openCustomFontPicker() = BrowserUnit.openFontFilePicker(customFontResultLauncher)

    private fun toggleTouchTurnPageFeature() {
        config.enableTouchTurn = !config.enableTouchTurn
        //updateTouchView()
    }

    private fun refreshAction() {
        TODO("Not yet implemented")
    }

    private fun focusOnInput() {
        TODO("Not yet implemented")
    }

    private fun createMultiTouchTouchListener(ninjaWebView: NinjaWebView): MultitouchListener =
        object : MultitouchListener(this@EinkBroActivity, ninjaWebView) {
            override fun onSwipeTop() = performGesture("setting_multitouch_up")
            override fun onSwipeBottom() = performGesture("setting_multitouch_down")
            override fun onSwipeRight() = performGesture("setting_multitouch_right")
            override fun onSwipeLeft() = performGesture("setting_multitouch_left")
        }

    private fun performGesture(str: String) {

    }

    open fun createNinjaWebView(): NinjaWebView = NinjaWebView(this, this)

    private var preloadedWebView: NinjaWebView? = null

    @SuppressLint("ClickableViewAccessibility")
    protected fun addAlbum(
        title: String = "",
        url: String = config.favoriteUrl,
        foreground: Boolean = true,
        incognito: Boolean = false,
        enablePreloadWebView: Boolean = true
    ) {
        val newWebView = (preloadedWebView ?: createNinjaWebView()).apply {
            this.albumTitle = title
            this.incognito = incognito
            setOnTouchListener(createMultiTouchTouchListener(this))
        }

        maybeCreateNewPreloadWebView(enablePreloadWebView, newWebView)

        ViewUnit.bound(this, newWebView)

        updateTabPreview(newWebView, url)
        updateWebViewCount()

        loadUrlInWebView(foreground, newWebView, url)

        updateSavedAlbumInfo()
    }

    private val browserContainer: BrowserContainer = BrowserContainer()
    private fun updateSavedAlbumInfo() {
        val albumControllers = browserContainer.list()
        val albumInfoList = albumControllers
            .filter { it.albumUrl.isNotBlank() && it.albumUrl != BrowserUnit.URL_ABOUT_BLANK }
            .map { controller -> AlbumInfo(controller.albumTitle, controller.albumUrl) }
        config.savedAlbumInfoList = albumInfoList
        config.currentAlbumIndex = browserContainer.indexOf(currentAlbumController)
        // fix if current album is still with null url
        if (albumInfoList.isNotEmpty() && config.currentAlbumIndex >= albumInfoList.size) {
            config.currentAlbumIndex = albumInfoList.size - 1
        }
    }

    private fun loadUrlInWebView(foreground: Boolean, webView: NinjaWebView, url: String) {
        if (!foreground) {
            ViewUnit.bound(this, webView)
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
            } else if (config.showRecentBookmarks) {
                showRecentlyUsedBookmarks(webView)
            }
        }
    }

    private fun showRecentlyUsedBookmarks(webView: NinjaWebView) {

    }

    private fun showToolbar() {
    }

    private fun updateWebViewCount() {
        val subScript = browserContainer.size()
        val superScript = browserContainer.indexOf(currentAlbumController) + 1
//        composeToolbarViewController.updateTabCount(
//            createWebViewCountString(
//                superScript,
//                subScript
//            )
//        )
    }

    private fun updateTabPreview(newWebView: NinjaWebView, url: String) {
        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            newWebView.setAlbumCover(it)
        }

        val album = newWebView.album
        if (currentAlbumController != null) {
            val index = browserContainer.indexOf(currentAlbumController) + 1
            browserContainer.add(newWebView, index)
            overviewDialogController.addTabPreview(album, index)
        } else {
            browserContainer.add(newWebView)
            overviewDialogController.addTabPreview(album, browserContainer.size() - 1)
        }
    }

    private fun maybeCreateNewPreloadWebView(
        enablePreloadWebView: Boolean,
        newWebView: NinjaWebView
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

    override fun updateProgress(progress: Int) {
    }

    override fun updateTitle(title: String?) = updateTitle()

    private fun updateTitle() {
        if (!this::ninjaWebView.isInitialized) return

        if (this::ninjaWebView.isInitialized && ninjaWebView === currentAlbumController) {
            //TODO
            //composeToolbarViewController.updateTitle(ninjaWebView.title.orEmpty())
        }
    }

    override fun addNewTab(url: String?) {
        TODO("Not yet implemented")
    }

    override fun showAlbum(controller: AlbumController) {
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
                return
            }

            currentAlbumController?.deactivate()
        }

        mainContentLayout.removeAllViews()
        mainContentLayout.addView(controller as View)

        currentAlbumController = controller
        currentAlbumController?.activate()

        updateSavedAlbumInfo()
        updateWebViewCount()

        ninjaWebView = controller as NinjaWebView

        updateTitle()
    }

    override fun removeAlbum(albumController: AlbumController?) {
        TODO("Not yet implemented")
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>?) {
        TODO("Not yet implemented")
    }

    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        TODO("Not yet implemented")
    }

    override fun onLongPress(message: Message?) {
        TODO("Not yet implemented")
    }

    private fun showOverview() = overviewDialogController.show()

    override fun hideOverview() = overviewDialogController.hide()

    override fun addHistory(title: String?, url: String?) {
    }

    override fun onHideCustomView(): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleKeyEvent(event: KeyEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadInSecondPane(url: String?): Boolean {
        return false
    }

    private fun getUrlMatchedBrowser(url: String): NinjaWebView? {
        return browserContainer.list().firstOrNull { it.albumUrl == url } as NinjaWebView?
    }

    private fun openHistoryPage(amount: Int = 0) {}

    private fun openBookmarkPage() = BookmarksDialogFragment(
        lifecycleScope,
        bookmarkViewModel,
        gotoUrlAction = { url -> updateAlbum(url) },
        addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
        splitScreenAction = { url -> toggleSplitScreen(url) }
    ).show(supportFragmentManager, "bookmarks dialog")

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    private lateinit var twoPaneController: TwoPaneController
    private fun maybeInitTwoPaneController() {
        if (!isTwoPaneControllerInitialized()) {
            twoPaneController = TwoPaneController(
                this,
                _einkBroMainLayoutBinding.subContainer,
                _einkBroMainLayoutBinding.twoPanelLayout,
                { showTranslation() },
                { if (ninjaWebView.isReaderModeOn) ninjaWebView.toggleReaderMode() },
                { url -> ninjaWebView.loadUrl(url) },
            )
        }
    }

    private fun toggleSplitScreen(url: String? = null) {
        maybeInitTwoPaneController()
        if (twoPaneController.isSecondPaneDisplayed() && url == null) {
            twoPaneController.hideSecondPane()
            return
        }

        twoPaneController.showSecondPane(url ?: config.favoriteUrl)
    }

    private fun updateAlbum(url: String?) {

    }


    companion object {
        private const val TAG = "EinkBroActivity"
        private const val INPUT_FILE_REQUEST_CODE = 1
        const val WRITE_EPUB_REQUEST_CODE = 2
        const val GRANT_PERMISSION_REQUEST_CODE = 4
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun EinkBroScreen(
    modifier: Modifier,
    twoPaneLayout: TwoPaneLayout,
    layoutOverview: View,
    toolbarActionInfoList: List<ToolbarActionInfo>,
    onToolbarItemClick: (ToolbarAction) -> Unit = {},
    onToolbarItemLongClick: (ToolbarAction) -> Unit = {}
) {

    var title by mutableStateOf("")
    var tabCount by mutableStateOf("")
    var isIncognito by mutableStateOf(false)

    Box {
        Column {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                factory = { twoPaneLayout },
            )
            ComposedToolbar(
                toolbarActionInfoList,
                title = title,
                tabCount = tabCount,
                isIncognito = isIncognito,
                onClick = onToolbarItemClick,
                onLongClick = onToolbarItemLongClick
            )
        }
        AndroidView(factory = { layoutOverview })
    }
}

