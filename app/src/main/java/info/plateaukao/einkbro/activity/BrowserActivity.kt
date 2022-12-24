package info.plateaukao.einkbro.activity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.SearchManager
import android.content.*
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.View.*
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView.HitTestResult
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.databinding.ActivityMainBinding
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.*
import info.plateaukao.einkbro.service.ClearService
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.task.SaveScreenshotTask
import info.plateaukao.einkbro.unit.*
import info.plateaukao.einkbro.unit.BrowserUnit.downloadFileId
import info.plateaukao.einkbro.unit.HelperUnit.toNormalScheme
import info.plateaukao.einkbro.util.DebugT
import info.plateaukao.einkbro.view.*
import info.plateaukao.einkbro.view.GestureType.*
import info.plateaukao.einkbro.view.GestureType.CloseTab
import info.plateaukao.einkbro.view.GestureType.Menu
import info.plateaukao.einkbro.view.compose.SearchBarView
import info.plateaukao.einkbro.view.dialog.*
import info.plateaukao.einkbro.view.dialog.compose.*
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.*
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.viewControllers.*
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.system.exitProcess


open class BrowserActivity : FragmentActivity(), BrowserController {
    private lateinit var progressBar: ProgressBar
    protected lateinit var ninjaWebView: NinjaWebView

    private var videoView: VideoView? = null
    private var customView: View? = null

    // Layouts
    private lateinit var searchPanel: SearchBarView
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var subContainer: RelativeLayout

    private var fullscreenHolder: FrameLayout? = null

    // Others
    private var downloadReceiver: BroadcastReceiver? = null
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()
    private val ttsManager: TtsManager by inject()

    private fun prepareRecord(): Boolean {
        val webView = currentAlbumController as NinjaWebView
        val title = webView.title
        val url = webView.url
        return (title == null || title.isEmpty()
                || url == null || url.isEmpty()
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

    protected val composeToolbarViewController: ComposeToolbarViewController by lazy {
        ComposeToolbarViewController(
            binding.composeIconBar,
            this::onToolActionClick,
            this::onToolActionLongClick,
            onTabClick = { it.show() },
            onTabLongClick = { it.remove() },
        )
    }


    private fun onToolActionLongClick(toolbarAction: ToolbarAction) {
        when (toolbarAction) {
            ToolbarAction.Back -> openHistoryPage(5)
            ToolbarAction.Refresh -> fullscreen()
            ToolbarAction.Touch -> TouchAreaDialogFragment().show(
                supportFragmentManager,
                "TouchAreaDialog"
            )

            ToolbarAction.PageUp -> ninjaWebView.jumpToTop()
            ToolbarAction.PageDown -> ninjaWebView.jumpToBottom()
            ToolbarAction.TabCount -> config::isIncognitoMode.toggle()
            ToolbarAction.Settings -> showFastToggleDialog()
            ToolbarAction.Bookmark -> saveBookmark()
            ToolbarAction.Translation -> showTranslationConfigDialog()
            ToolbarAction.NewTab -> launchNewBrowser()
            ToolbarAction.Tts -> TtsSettingDialogFragment(
                this::gotoSystemTtsSettings
            ).show(supportFragmentManager, "TtsSettingDialog")

            ToolbarAction.Font -> ninjaWebView.toggleReaderMode()

            else -> {}
        }
    }

    private fun gotoSystemTtsSettings() {
        val intent = Intent().apply {
            action = "com.android.settings.TTS_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            NinjaToast.show(this, "No Text to Speech settings found")
        }
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
            ToolbarAction.BoldFont -> config::boldFontStyle.toggle()
            ToolbarAction.IncreaseFont -> increaseFontSize()
            ToolbarAction.DecreaseFont -> decreaseFontSize()
            ToolbarAction.FullScreen -> fullscreen()
            ToolbarAction.Forward -> if (ninjaWebView.canGoForward()) ninjaWebView.goForward()
            ToolbarAction.RotateScreen -> rotateScreen()
            ToolbarAction.Translation -> showTranslation()
            ToolbarAction.CloseTab -> removeAlbum(currentAlbumController!!)
            ToolbarAction.InputUrl -> focusOnInput()
            ToolbarAction.NewTab -> newATab()
            ToolbarAction.Desktop -> config::desktop.toggle()
            ToolbarAction.Search -> showSearchPanel()
            ToolbarAction.DuplicateTab -> duplicateTab()
            ToolbarAction.Tts -> toggleTtsRead()
            else -> {}
        }
    }

    private fun newATab() {
        when (config.newTabBehavior) {
            NewTabBehavior.START_INPUT -> {
                addAlbum(getString(R.string.app_name), "")
                focusOnInput()
            }

            NewTabBehavior.SHOW_HOME -> {
                addAlbum("", config.favoriteUrl)
            }

            NewTabBehavior.SHOW_RECENT_BOOKMARKS -> {
                addAlbum("", "")
                showRecentlyUsedBookmarks(ninjaWebView)
            }
        }
    }

    private fun duplicateTab() {
        val webView = currentAlbumController as NinjaWebView
        val title = webView.title ?: ""
        val url = webView.url ?: return
        addAlbum(title, url)
    }

    private fun refreshAction() {
        if (ninjaWebView.isLoadFinish && ninjaWebView.url?.isNotEmpty() == true) {
            ninjaWebView.reload()
        } else {
            ninjaWebView.stopLoading()
        }
    }

    private lateinit var overviewDialogController: OverviewDialogController

    private val browserContainer: BrowserContainer = BrowserContainer()

    private val touchController: TouchAreaViewController by lazy {
        TouchAreaViewController(
            rootView = binding.root,
            pageUpAction = { ninjaWebView.pageUpWithNoAnimation() },
            pageTopAction = { ninjaWebView.jumpToTop() },
            pageDownAction = { ninjaWebView.pageDownWithNoAnimation() },
            pageBottomAction = { ninjaWebView.jumpToBottom() },
        )
    }

    private lateinit var twoPaneController: TwoPaneController

    private val dialogManager: DialogManager by lazy { DialogManager(this) }

    private val recordDb: RecordDb by lazy { RecordDb(this).apply { open(false) } }

    private val customFontResultLauncher: ActivityResultLauncher<Intent> =
        BrowserUnit.registerCustomFontSelectionResult(this)
    private val saveImageFilePickerLauncher: ActivityResultLauncher<Intent> =
        BrowserUnit.registerSaveImageFilePickerResult(this) { uri ->
            // action to show the downloaded image
            val fileIntent = Intent(ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            dialogManager.showOkCancelDialog(
                messageResId = R.string.toast_downloadComplete,
                okAction = { startActivity(fileIntent) }
            )
        }

    // Classes
    private inner class VideoCompletionListener : OnCompletionListener,
        MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            return false
        }

        override fun onCompletion(mp: MediaPlayer) {
            onHideCustomView()
        }
    }

    // Overrides
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
        initToolbar()
        initSearchPanel()
        initInputBar()
        initOverview()
        initTouchArea()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (this@BrowserActivity.isFinishing || downloadFileId == -1L) return

                val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val mostRecentDownload: Uri =
                    downloadManager.getUriForDownloadedFile(downloadFileId) ?: return
                val mimeType: String = downloadManager.getMimeTypeForDownloadedFile(downloadFileId)
                val fileIntent = Intent(ACTION_VIEW).apply {
                    setDataAndType(mostRecentDownload, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                downloadFileId = -1L
                dialogManager.showOkCancelDialog(
                    messageResId = R.string.toast_downloadComplete,
                    okAction = {
                        try {
                            startActivity(fileIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            NinjaToast.show(this@BrowserActivity, R.string.toast_error)
                        }
                    }
                )
            }
        }

        registerReceiver(downloadReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
        dispatchIntent(intent)
        // after dispatching intent, the value should be reset to false
        shouldLoadTabState = false

        if (config.keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        listenKeyboardShowHide()

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

    private fun listenKeyboardShowHide() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff: Int = binding.root.rootView.height - binding.root.height
            if (heightDiff > 200) { // Value should be less than keyboard's height
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
                //restartApp()
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

    private fun initTouchArea() {
        updateTouchView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WRITE_EPUB_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            epubManager.saveEpub(this, uri, ninjaWebView)

            return
        }

        if (requestCode == GRANT_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            HelperUnit.openFile(this@BrowserActivity, uri)
            return
        }

        if (requestCode == INPUT_FILE_REQUEST_CODE && filePathCallback != null) {
            var results: Array<Uri>? = null
            // Check that the response is a good one
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // If there is not data, then we may have taken a photo
                    val dataString = data.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
            return
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (config.restartChanged) {
            config.restartChanged = false
            showRestartConfirmDialog()
        }

        updateTitle()
        overridePendingTransition(0, 0)
        uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (config.customFontChanged) {
            dialogManager.showOkCancelDialog(
                title = getString(R.string.reload_font_change),
                okAction = { ninjaWebView.reload() }
            )
            config.customFontChanged = false
        }
        if (!config.continueMedia) {
            if (this::ninjaWebView.isInitialized) {
                ninjaWebView.resumeTimers()
            }
        }
    }

    private fun showRestartConfirmDialog() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.toast_restart,
            okAction = { restartApp() }
        )
    }

    private fun restartApp() {
        finishAffinity() // Finishes all activities.
        startActivity(packageManager.getLaunchIntentForPackage(packageName))    // Start the launch activity
        overridePendingTransition(0, 0)
        exitProcess(0)
    }

    private fun showFileListConfirmDialog() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.toast_downloadComplete,
            okAction = { BrowserUnit.openDownloadFolder(this@BrowserActivity) }
        )
    }

    override fun onDestroy() {
        ttsManager.stopReading()

        customFontResultLauncher.unregister()

        updateSavedAlbumInfo()

        if (config.clearWhenQuit) {
            val toClearService = Intent(this, ClearService::class.java)
            startService(toClearService)
        }
        browserContainer.clear()
        unregisterReceiver(downloadReceiver)
        recordDb.close()

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

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                return handleVolumeDownKey()
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                return handleVolumeUpKey()
            }

            KeyEvent.KEYCODE_MENU -> {
                showMenuDialog(); return true
            }

            KeyEvent.KEYCODE_BACK -> {
                return handleBackKey()
            }
        }
        return false
    }

    private fun handleVolumeDownKey(): Boolean {
        return if (config.volumePageTurn) {
            ninjaWebView.pageDownWithNoAnimation()
            true
        } else {
            false
        }
    }

    private fun handleVolumeUpKey(): Boolean {
        return if (config.volumePageTurn) {
            ninjaWebView.pageUpWithNoAnimation()
            true
        } else {
            false
        }
    }

    private fun handleBackKey(): Boolean {
        hideKeyboard()
        if (overviewDialogController.isVisible()) {
            hideOverview()
            return true
        }
        if (fullscreenHolder != null || customView != null || videoView != null) {
            return onHideCustomView()
        } else if (!binding.appBar.isVisible && sp.getBoolean("sp_toolbarShow", true)) {
            showToolbar()
        } else if (!composeToolbarViewController.isDisplayed()) {
            composeToolbarViewController.show()
        } else {
            if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                removeAlbum(currentAlbumController!!)
            }
        }
        return true
    }

    override fun showAlbum(controller: AlbumController) {
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
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

        mainContentLayout.addView(controller as View)

        currentAlbumController = controller
        currentAlbumController?.activate()

        updateSavedAlbumInfo()
        updateWebViewCount()

        progressBar.visibility = GONE
        ninjaWebView = controller as NinjaWebView

        updateTitle()

        updateTabBar()
    }

    override fun onUpdateAlbum(album: Album?) {
        updateTabBar()
    }

    private fun openCustomFontPicker() = BrowserUnit.openFontFilePicker(customFontResultLauncher)

    private fun showOverview() = overviewDialogController.show()

    override fun hideOverview() = overviewDialogController.hide()

    private fun launchNewBrowser() {
        val intent = Intent(this, ExtraBrowserActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            action = ACTION_VIEW
            data = Uri.parse(config.favoriteUrl)
        }

        startActivity(intent)
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

    private fun saveBookmark(url: String? = null, title: String? = null) {
        val currentUrl = url ?: ninjaWebView.url ?: return
        val nonNullTitle = title ?: HelperUnit.secString(ninjaWebView.title)
        try {
            lifecycleScope.launch {
                BookmarkEditDialog(
                    this@BrowserActivity,
                    bookmarkManager,
                    Bookmark(nonNullTitle, currentUrl),
                    {
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

    private fun toggleTouchTurnPageFeature() {
        config::enableTouchTurn.toggle()
        updateTouchView()
    }

    private fun updateTouchView() {
        composeToolbarViewController.updateIcons()
    }

    // Methods
    //private fun showFontSizeChangeDialog() = dialogManager.showFontSizeChangeDialog()
    private fun showFontSizeChangeDialog() =
        FontDialogFragment { openCustomFontPicker() }.show(supportFragmentManager, "font_dialog")

    private fun changeFontSize(size: Int) {
        config.fontSize = size
    }

    private fun increaseFontSize() = changeFontSize(config.fontSize + 20)

    private fun decreaseFontSize() {
        if (config.fontSize > 50) changeFontSize(config.fontSize - 20)
    }

    private fun maybeInitTwoPaneController() {
        if (!isTwoPaneControllerInitialized()) {
            twoPaneController = TwoPaneController(
                this,
                binding.subContainer,
                binding.twoPanelLayout,
                { showTranslation() },
                { if (ninjaWebView.isReaderModeOn) ninjaWebView.toggleReaderMode() },
                { url -> ninjaWebView.loadUrl(url) },
            )
        }
    }

    private fun isTwoPaneControllerInitialized(): Boolean = ::twoPaneController.isInitialized

    private fun showTranslation() {
        maybeInitTwoPaneController()

        lifecycleScope.launch(Dispatchers.Main) {
            twoPaneController.showTranslation(ninjaWebView)
        }
    }

    private fun printPDF() {
        try {
            val title = HelperUnit.fileName(ninjaWebView.url)
            val printManager = getSystemService(PRINT_SERVICE) as PrintManager
            val printAdapter = ninjaWebView.createPrintDocumentAdapter(title) {
                showFileListConfirmDialog()
            }
            printManager.print(title, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
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

            ACTION_VIEW -> {
                // if webview for that url already exists, show the original tab, otherwise, create new
                val viewUri = intent.data?.toNormalScheme() ?: Uri.parse(config.favoriteUrl)
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
            val onNavButtonTouchListener = object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture(ConfigManager.K_GESTURE_NAV_UP)
                override fun onSwipeBottom() = performGesture(ConfigManager.K_GESTURE_NAV_DOWN)
                override fun onSwipeRight() = performGesture(ConfigManager.K_GESTURE_NAV_RIGHT)
                override fun onSwipeLeft() = performGesture(ConfigManager.K_GESTURE_NAV_LEFT)
            }
            fabImageViewController.defaultTouchListener = onNavButtonTouchListener
        }

        composeToolbarViewController.updateIcons()
        // strange crash on my device. register later
        runOnUiThread {
            config.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }

    private fun showTranslationConfigDialog() {
        maybeInitTwoPaneController()
        twoPaneController.showTranslationConfigDialog()
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ConfigManager.K_TOOLBAR_ICONS_FOR_LARGE,
                ConfigManager.K_TOOLBAR_ICONS -> {
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

                ConfigManager.K_FONT_SIZE -> {
                    ninjaWebView.settings.textZoom = config.fontSize
                }

                ConfigManager.K_BOLD_FONT -> {
                    composeToolbarViewController.updateIcons()
                    if (config.boldFontStyle) {
                        ninjaWebView.updateCssStyle()
                    } else {
                        ninjaWebView.reload()
                    }
                }

                ConfigManager.K_WHITE_BACKGROUND -> {
                    if (config.whiteBackground) {
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
                    ninjaWebView.updateDesktopMode()
                    ninjaWebView.reload()
                    composeToolbarViewController.updateIcons()
                }

                ConfigManager.K_DARK_MODE -> config.restartChanged = true
                ConfigManager.K_TOOLBAR_TOP -> updateAppbarPosition()

                ConfigManager.K_NAV_POSITION -> fabImageViewController.initialize()
                ConfigManager.K_TTS_SPEED_VALUE ->
                    ttsManager.setSpeechRate(config.ttsSpeedValue / 100f)
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

    private fun performGesture(gestureString: String) {
        val gesture = GestureType.from(sp.getString(gestureString, "01") ?: "01")
        val controller: AlbumController?
        ninjaWebView = currentAlbumController as NinjaWebView
        when (gesture) {
            NothingHappen -> Unit
            Forward -> if (ninjaWebView.canGoForward()) {
                ninjaWebView.goForward()
            } else {
                NinjaToast.show(this, R.string.toast_webview_forward)
            }

            Backward -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                NinjaToast.show(this, getString(R.string.no_previous_page))
            }

            ScrollToTop -> ninjaWebView.jumpToTop()
            ScrollToBottom -> ninjaWebView.pageDownWithNoAnimation()
            ToLeftTab -> {
                controller = nextAlbumController(false)
                showAlbum(controller!!)
            }

            ToRightTab -> {
                controller = nextAlbumController(true)
                showAlbum(controller!!)
            }

            Overview -> showOverview()
            OpenNewTab -> {
                addAlbum(getString(R.string.app_name), "", true)
                focusOnInput()
            }

            CloseTab -> runOnUiThread { removeAlbum(currentAlbumController!!) }
            PageUp -> ninjaWebView.pageUpWithNoAnimation()
            PageDown -> ninjaWebView.pageDownWithNoAnimation()
            Bookmark -> openBookmarkPage()
            Back -> handleBackKey()
            Fullscreen -> fullscreen()
            Refresh -> ninjaWebView.reload()
            Menu -> showMenuDialog()
        }
    }

    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager.bookmarkDao)
    }

    private fun initOverview() {
        overviewDialogController = OverviewDialogController(
            this,
            binding.layoutOverview,
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
    }

    private fun openHistoryPage(amount: Int = 0) = overviewDialogController.openHistoryPage(amount)

    private fun openBookmarkPage() =
        BookmarksDialogFragment(
            lifecycleScope,
            bookmarkViewModel,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            splitScreenAction = { url -> toggleSplitScreen(url) }
        ).show(supportFragmentManager, "bookmarks dialog")

    private fun initSearchPanel() {
        searchPanel = binding.mainSearchPanel
        searchPanel.apply {
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

    private fun showFastToggleDialog() {
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

    open fun createNinjaWebView(): NinjaWebView = NinjaWebView(this, this)

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
        updateTabBar()
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
            override fun onSwipeTop() = performGesture("setting_multitouch_up")
            override fun onSwipeBottom() = performGesture("setting_multitouch_down")
            override fun onSwipeRight() = performGesture("setting_multitouch_right")
            override fun onSwipeLeft() = performGesture("setting_multitouch_left")
        }

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

    private fun updateAlbum(url: String?) {
        if (url == null) return
        (currentAlbumController as NinjaWebView).loadUrl(url)
        updateTitle()

        updateSavedAlbumInfo()
    }

    private fun closeTabConfirmation(okAction: () -> Unit) {
        if (!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction()
        } else {
            dialogManager.showOkCancelDialog(
                messageResId = R.string.toast_close_tab,
                okAction = okAction,
            )
        }
    }

    override fun removeAlbum(controller: AlbumController) {
        if (browserContainer.size() <= 1) {
            finish()
        } else {
            closeTabConfirmation {
                overviewDialogController.removeTabView(controller.album)
                val removeIndex = browserContainer.indexOf(controller)
                val currentIndex = browserContainer.indexOf(currentAlbumController)
                browserContainer.remove(controller)
                // only refresh album when the delete one is current one
                if (removeIndex == currentIndex) {
                    val newIndex = max(0, removeIndex - 1)
                    showAlbum(browserContainer[newIndex])
                }
                updateWebViewCount()
                updateTabBar()
            }
        }
        updateSavedAlbumInfo()
    }

    private fun updateTabBar() {
        if (config.shouldShowTabBar) {
            composeToolbarViewController.updateTabView(overviewDialogController.currentAlbumList.toList())
        }
    }

    private fun updateTitle() {
        if (!this::ninjaWebView.isInitialized) return

        if (this::ninjaWebView.isInitialized && ninjaWebView === currentAlbumController) {
            composeToolbarViewController.updateTitle(ninjaWebView.title.orEmpty())
        }
    }

    private var keepToolbar = false
    private fun scrollChange() {
        ninjaWebView.setScrollChangeListener(object : NinjaWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                if (::twoPaneController.isInitialized) {
                    twoPaneController.scrollChange(scrollY - oldScrollY)
                }

                if (!sp.getBoolean("hideToolbar", false)) return

                val height =
                    floor(x = ninjaWebView.contentHeight * ninjaWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ninjaWebView.height
                val cutoff =
                    height - webViewHeight - 112 * resources.displayMetrics.density.roundToInt()
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    if (!keepToolbar) {
                        fullscreen()
                    } else {
                        keepToolbar = false
                    }
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
        DebugT("updateProgress:$progress")
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
    private fun focusOnInput() {
        composeToolbarViewController.hide()

        val textOrUrl = if (ninjaWebView.url?.startsWith("data:") != true) {
            val url = ninjaWebView.url ?: ""
            TextFieldValue(url, selection = TextRange(0, url.length))
        } else {
            TextFieldValue("")
        }

        binding.inputUrl.apply {
            inputTextOrUrl.value = textOrUrl
            isWideLayout = ViewUnit.isWideLayout(this@BrowserActivity)
            shouldReverse = !config.isToolbarOnTop
            hasCopiedText = getClipboardText().isNotEmpty()
            lifecycleScope.launch { binding.inputUrl.recordList.value = recordDb.listEntries(true) }
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
            .primaryClip?.getItemAt(0)?.text?.toString() ?: ""

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
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
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
        val decorView = window.decorView as FrameLayout
        decorView.addView(
            fullscreenHolder,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        customView?.keepScreenOn = true
        (currentAlbumController as View?)?.visibility = GONE
        ViewUnit.setCustomFullscreen(window, true)
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


        (window.decorView as FrameLayout).removeView(fullscreenHolder)
        customView?.keepScreenOn = false
        (currentAlbumController as View).visibility = VISIBLE
        ViewUnit.setCustomFullscreen(window, false)
        fullscreenHolder = null
        customView = null

        if (videoView != null) {
            videoView?.setOnErrorListener(null)
            videoView?.setOnCompletionListener(null)
            videoView = null
        }
        requestedOrientation = originalOrientation

        return true
    }

    private var previousKeyEvent: KeyEvent? = null
    override fun handleKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action != ACTION_DOWN) return false
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
                KeyEvent.KEYCODE_D -> removeAlbum(currentAlbumController!!)
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
        val host = Uri.parse(url).host ?: ""
        if (config.adSites.contains(host)) {
            confirmRemoveAdSite(host)
        } else {
            lifecycleScope.launch {
                val domain = TextInputDialog(
                    this@BrowserActivity,
                    "Ad Url to be blocked",
                    "",
                    url,
                ).show() ?: ""

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

    override fun onLongPress(message: Message, event: MotionEvent) {
        val url = BrowserUnit.getWebViewLinkUrl(ninjaWebView, message).ifBlank { return }
        val linkImageUrl = BrowserUnit.getWebViewLinkImageUrl(ninjaWebView, message)
        BrowserUnit.getWebViewLinkTitle(ninjaWebView) { linkTitle ->
            val titleText = linkTitle.ifBlank { url }.toString()
            ContextMenuDialogFragment(
                url,
                linkImageUrl.isNotBlank(),
                Point(event.x.toInt(), event.y.toInt())
            ) {
                this@BrowserActivity.handleContextMenuItem(it, titleText, url, linkImageUrl)
            }.show(supportFragmentManager, "contextMenu")
        }
    }

    private fun handleContextMenuItem(
        contextMenuItemType: ContextMenuItemType,
        title: String,
        url: String,
        imageUrl: String
    ) {
        when (contextMenuItemType) {
            ContextMenuItemType.NewTabForeground -> addAlbum(title, url)
            ContextMenuItemType.NewTabBackground -> addAlbum(title, url, false)
            ContextMenuItemType.ShareLink -> {
                if (prepareRecord()) NinjaToast.show(this, getString(R.string.toast_share_failed))
                else IntentUnit.share(this, title, url)
            }

            ContextMenuItemType.CopyLink -> ShareUtil.copyToClipboard(this, url)
            ContextMenuItemType.CopyText -> ShareUtil.copyToClipboard(this, title)
            ContextMenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                this,
                url,
                getString(R.string.menu_open_with)
            )

            ContextMenuItemType.SaveBookmark -> saveBookmark(url, title)
            ContextMenuItemType.SplitScreen -> toggleSplitScreen(url)
            ContextMenuItemType.AdBlock -> confirmAdSiteAddition(imageUrl)
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
        if (!searchOnSite) {
            fabImageViewController.hide()
            searchPanel.visibility = INVISIBLE
            binding.appBar.visibility = VISIBLE
            binding.contentSeparator.visibility = VISIBLE
            binding.inputUrl.visibility = INVISIBLE
            composeToolbarViewController.show()
            hideKeyboard()
            showStatusBar()
        }
    }

    private fun fullscreen() {
        if (!searchOnSite) {
            if (config.fabPosition != FabPosition.NotShow) {
                fabImageViewController.show()
            }
            searchPanel.visibility = INVISIBLE
            binding.appBar.visibility = GONE
            binding.contentSeparator.visibility = GONE
            hideStatusBar()
        }
    }

    private fun hideSearchPanel() {
        ninjaWebView.clearMatches()
        searchOnSite = false
        hideKeyboard()
        showToolbar()
    }

    private fun hideStatusBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.setDecorFitsSystemWindows(false)
            binding.root.setPadding(0, 0, 0, 0)
        }
    }

    private fun showStatusBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.systemBars())
        }
    }

    private fun showSearchPanel() {
        searchOnSite = true
        fabImageViewController.hide()
        searchPanel.visibility = VISIBLE
        searchPanel.getFocus()
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        showKeyboard()
    }

    private fun showSaveEpubDialog() = dialogManager.showSaveEpubDialog { uri ->
        if (uri == null) {
            epubManager.showEpubFilePicker()
        } else {
            epubManager.saveEpub(this, uri, ninjaWebView)
        }
    }

    private fun readArticle() {
        TtsLanguageDialog(this).show { ttsLanguage ->
            lifecycleScope.launch {
                ttsManager.readText(ttsLanguage, ninjaWebView.getRawText())
                delay(2000)
                // TODO: use real status to do update
                // wait for tts preparation
                composeToolbarViewController.updateIcons()

                if (ttsManager.isSpeaking()) {
                    speakingCompleteDetector()
                }
            }
        }
    }

    private fun openSavedEpub() = if (config.savedEpubFileInfos.isEmpty()) {
        NinjaToast.show(this, "no saved epub!")
    } else {
        dialogManager.showSaveEpubDialog(shouldAddNewEpub = false) { uri ->
            HelperUnit.openFile(this@BrowserActivity, uri ?: return@showSaveEpubDialog)
        }
    }

    private fun showMenuDialog() {
        MenuDialogFragment { handleMenuItem(it) }.show(supportFragmentManager, "menu_dialog")
    }

    private fun handleMenuItem(menuItemType: MenuItemType) {
        when (menuItemType) {
            Tts -> toggleTtsRead()
            QuickToggle -> showFastToggleDialog()
            OpenHome -> updateAlbum(
                sp.getString(
                    "favoriteURL",
                    "https://github.com/plateaukao/browser"
                )
            )

            MenuItemType.CloseTab -> currentAlbumController?.let { removeAlbum(it) }
            Quit -> finishAndRemoveTask()

            SplitScreen -> toggleSplitScreen()
            Translate -> showTranslation()
            VerticalRead -> ninjaWebView.toggleVerticalRead()
            ReaderMode -> ninjaWebView.toggleReaderMode()
            TouchSetting -> TouchAreaDialogFragment().show(
                supportFragmentManager,
                "TouchAreaDialog"
            )

            ToolbarSetting -> ToolbarConfigDialogFragment().show(
                supportFragmentManager,
                "toolbar_config"
            )

            ReceiveData -> showReceiveDataDialog()
            SendLink -> SendLinkDialog(this, lifecycleScope).show(ninjaWebView.url.orEmpty())
            ShareLink -> IntentUnit.share(this, ninjaWebView.title, ninjaWebView.url)
            OpenWith -> HelperUnit.showBrowserChooser(
                this,
                ninjaWebView.url,
                getString(R.string.menu_open_with)
            )

            CopyLink -> ShareUtil.copyToClipboard(this, ninjaWebView.url ?: "")
            Shortcut -> HelperUnit.createShortcut(
                this,
                ninjaWebView.title,
                ninjaWebView.url,
                ninjaWebView.favicon
            )

            SetHome -> config.favoriteUrl = ninjaWebView.url.orEmpty()
            SaveBookmark -> saveBookmark()
            OpenEpub -> openSavedEpub()
            SaveEpub -> showSaveEpubDialog()
            SavePdf -> printPDF()

            FontSize -> showFontSizeChangeDialog()
            WhiteBknd -> config::whiteBackground.toggle()
            BoldFont -> config::boldFontStyle.toggle()
            Search -> showSearchPanel()
            Download -> BrowserUnit.openDownloadFolder(this)
            Settings -> startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    private fun toggleTtsRead() {
        if (ttsManager.isSpeaking()) {
            ttsManager.stopReading()
        } else {
            readArticle()
        }
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

    private fun toggleSplitScreen(url: String? = null) {
        maybeInitTwoPaneController()
        if (twoPaneController.isSecondPaneDisplayed() && url == null) {
            twoPaneController.hideSecondPane()
            return
        }

        twoPaneController.showSecondPane(url ?: config.favoriteUrl)
    }

    private fun saveScreenshot() {
        lifecycleScope.launch(Dispatchers.Main) {
            SaveScreenshotTask(this@BrowserActivity, ninjaWebView).execute()
        }
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
    private var mActionMode: ActionMode? = null
    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (mActionMode == null) {
            mActionMode = mode
        }
    }

    override fun onPause() {
        super.onPause()
        mActionMode?.finish()
        mActionMode = null
        if (!config.continueMedia) {
            if (this::ninjaWebView.isInitialized) {
                ninjaWebView.pauseTimers()
            }
        }
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        mActionMode = null
    }
    // - action mode handling

    companion object {
        private const val TAG = "BrowserActivity"
        private const val INPUT_FILE_REQUEST_CODE = 1
        const val WRITE_EPUB_REQUEST_CODE = 2
        const val GRANT_PERMISSION_REQUEST_CODE = 4
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
    }
}