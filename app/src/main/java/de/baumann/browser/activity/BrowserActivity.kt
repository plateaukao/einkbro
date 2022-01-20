package de.baumann.browser.activity

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView.HitTestResult
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.*
import de.baumann.browser.browser.*
import de.baumann.browser.database.*
import de.baumann.browser.database.Bookmark
import de.baumann.browser.epub.EpubFileInfo
import de.baumann.browser.epub.EpubManager
import de.baumann.browser.preference.*
import de.baumann.browser.service.ClearService
import de.baumann.browser.task.SaveScreenshotTask
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.HelperUnit.openUri
import de.baumann.browser.unit.HelperUnit.toNormalScheme
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.unit.ViewUnit.dp
import de.baumann.browser.util.Constants
import de.baumann.browser.util.DebugT
import de.baumann.browser.view.*
import de.baumann.browser.view.GestureType.*
import de.baumann.browser.view.adapter.*
import de.baumann.browser.view.dialog.*
import de.baumann.browser.view.viewControllers.OverviewDialogController
import de.baumann.browser.view.viewControllers.ToolbarViewController
import de.baumann.browser.view.viewControllers.TouchAreaViewController
import de.baumann.browser.view.viewControllers.TwoPaneController
import de.baumann.browser.viewmodel.BookmarkViewModel
import de.baumann.browser.viewmodel.BookmarkViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.*
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.system.exitProcess


open class BrowserActivity : ComponentActivity(), BrowserController, OnClickListener {
    private lateinit var fabImageButtonNav: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var searchBox: EditText
    private lateinit var ninjaWebView: NinjaWebView
    private lateinit var omniboxTitle: TextView

    private var bottomSheetDialog: Dialog? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Layouts
    private lateinit var mainToolbar: RelativeLayout
    private lateinit var searchPanel: ViewGroup
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var subContainer: RelativeLayout

    private var fullscreenHolder: FrameLayout? = null

    // Others
    private var title: String? = null
    private var url: String? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()
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

    private val toolbarViewController: ToolbarViewController by lazy { ToolbarViewController(this, binding.toolbarScroller) }

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

    // Classes
    private inner class VideoCompletionListener : OnCompletionListener, MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            return false
        }

        override fun onCompletion(mp: MediaPlayer) {
            onHideCustomView()
        }
    }

    // Overrides
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            bookmarkManager.migrateOldData()
        }

        savedInstanceState?.let {
            shouldLoadTabState = it.getBoolean(K_SHOULD_LOAD_TAB_STATE)
        }

        // root cause of slow drawing
        //WebView.enableSlowWholeDocumentDraw()

        sp.edit().putInt("restart_changed", 0).apply()
        HelperUnit.applyTheme(this)
        setContentView(binding.root)
        if (sp.getString("saved_key_ok", "no") == "no") {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!§$%&/()=?;:_-.,+#*<>".toCharArray()
            val sb = StringBuilder()
            val random = Random()
            for (i in 0..24) {
                val c = chars[random.nextInt(chars.size)]
                sb.append(c)
            }
            if (Locale.getDefault().country == "CN") {
                sp.edit().putString(getString(R.string.sp_search_engine), "2").apply()
            }
            sp.edit {
                putString("saved_key", sb.toString())
                putString("saved_key_ok", "yes")
                putString("setting_gesture_tb_up", "08")
                putString("setting_gesture_tb_down", "01")
                putString("setting_gesture_tb_left", "07")
                putString("setting_gesture_tb_right", "06")
                putString("setting_gesture_nav_up", "04")
                putString("setting_gesture_nav_down", "05")
                putString("setting_gesture_nav_left", "03")
                putString("setting_gesture_nav_right", "02")
                putBoolean(getString(R.string.sp_location), false)
            }
        }
        mainContentLayout = findViewById(R.id.main_content)
        subContainer = findViewById(R.id.sub_container)
        initToolbar()
        initSearchPanel()
        initOverview()
        initTouchArea()
        updateWebViewCountUI()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dialogManager.showOkCancelDialog(
                        messageResId = R.string.toast_downloadComplete,
                        okAction = { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                )
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, filter)
        dispatchIntent(intent)
        // after dispatching intent, the value should be reset to false
        shouldLoadTabState = false

        if (config.keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        listenKeyboardShowHide()

        orientation = resources.configuration.orientation
    }

    private fun listenKeyboardShowHide() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff: Int = binding.root.rootView.height - binding.root.height
            if (heightDiff > 300) { // Value should be less than keyboard's height
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
            toolbarViewController.reorderIcons()
            orientation = newConfig.orientation
        }
    }

    private fun initTouchArea() {
        binding.omniboxTouch.setOnLongClickListener {
            TouchAreaDialog(this@BrowserActivity).show()
            true
        }

        updateTouchView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WRITE_EPUB_REQUEST_CODE && resultCode == RESULT_OK) {
            val nonNullData = data?.data ?: return
            saveEpub(nonNullData)

            return
        }

        if (requestCode == WRITE_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { printPDF() }

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

    public override fun onResume() {
        super.onResume()
        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply()
            showRestartConfirmDialog()
        }

        updateTitle()
        overridePendingTransition(0, 0)
        uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
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
                okAction = { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
        )
    }

    public override fun onDestroy() {
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            val toClearService = Intent(this, ClearService::class.java)
            startService(toClearService)
        }
        browserContainer.clear()
        IntentUnit.setContext(null)
        unregisterReceiver(downloadReceiver)
        recordDb.close()

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                return if (config.volumePageTurn) {
                    ninjaWebView.pageDownWithNoAnimation()
                    true
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                return if (config.volumePageTurn) {
                    ninjaWebView.pageUpWithNoAnimation()
                    true
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_MENU -> return showMenuDialog()
            KeyEvent.KEYCODE_BACK -> {
                hideKeyboard()
                if (overviewDialogController.isVisible()) {
                    hideOverview()
                    return true
                }
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    return onHideCustomView()
                } else if (!binding.appBar.isVisible && sp.getBoolean("sp_toolbarShow", true)) {
                    showToolbar()
                } else if (!toolbarViewController.isDisplayed()) {
                    toolbarViewController.show()
                } else {
                    if (ninjaWebView.canGoBack()) {
                        ninjaWebView.goBack()
                    } else {
                        removeAlbum(currentAlbumController!!)
                    }
                }
                return true
            }
            // vim bindings
            KeyEvent.KEYCODE_O -> {
                binding.omniboxInput.performClick()
            }
        }
        return false
    }

    override fun showAlbum(controller: AlbumController) {
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
                return
            }

            currentAlbumController?.deactivate()
        }

        mainContentLayout.removeAllViews()
        val av = controller as View
        mainContentLayout.addView(av)

        currentAlbumController = controller
        currentAlbumController?.activate()

        updateSavedAlbumInfo()
        updateWebViewCountUI()

        progressBar.visibility = GONE
        ninjaWebView = controller as NinjaWebView

        updateTitle()
    }

    override fun updateAutoComplete() {
        lifecycleScope.launch {
            val activity = this@BrowserActivity
            val list = recordDb.listEntries(true)

            val adapter = CompleteAdapter(activity, R.layout.complete_item, list) { record ->
                updateAlbum(record.url)
                hideKeyboard()
                binding.omniboxInput.clearFocus()
                showToolbar()
            }

            with(binding.omniboxInput) {
                setAdapter(adapter)
                threshold = 1
                dropDownWidth = ViewUnit.getWindowWidth(activity)
            }
        }
    }

    private fun showOverview() = overviewDialogController.show()

    override fun hideOverview() = overviewDialogController.hide()

    private fun hideBottomSheetDialog() {
        bottomSheetDialog?.cancel()
    }

    @SuppressLint("RestrictedApi")
    override fun onClick(v: View) {
        ninjaWebView = currentAlbumController as NinjaWebView
        try {
            title = ninjaWebView.title?.trim { it <= ' ' }
            url = ninjaWebView.url?.trim { it <= ' ' }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        hideBottomSheetDialog()
        when (v.id) {
            R.id.button_font_size -> showFontSizeChangeDialog()
            R.id.omnibox_title -> {
                focusOnInput()
            }
            R.id.omnibox_input_clear -> binding.omniboxInput.text.clear()
            R.id.omnibox_input_close -> showToolbar()
            R.id.tab_plus_incognito -> {
                hideOverview()
                addAlbum(getString(R.string.app_name), "", incognito = true)
                focusOnInput()
            }
            R.id.new_window -> {
                hideOverview()
                launchNewBrowser()
            }
            R.id.tab_plus_bottom -> {
                hideOverview()
                addAlbum(getString(R.string.app_name), "")
                focusOnInput()
            }
            R.id.menu_save_pdf -> showPdfFilePicker()

            // --- tool bar handling
            R.id.omnibox_tabcount -> showOverview()
            R.id.omnibox_touch -> toggleTouchTurnPageFeature()
            R.id.omnibox_font -> showFontSizeChangeDialog()
            R.id.omnibox_reader -> ninjaWebView.toggleReaderMode()
            R.id.omnibox_bold_font -> {
                config.boldFontStyle = !config.boldFontStyle
            }
            R.id.omnibox_back -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                NinjaToast.show(this, getString(R.string.no_previous_page))
            }
            R.id.toolbar_forward -> if (ninjaWebView.canGoForward()) {
                ninjaWebView.goForward()
            }
            R.id.omnibox_page_up -> ninjaWebView.pageUpWithNoAnimation()
            R.id.omnibox_page_down -> {
                keepToolbar = true
                ninjaWebView.pageDownWithNoAnimation()
            }
            R.id.omnibox_vertical_read -> ninjaWebView.toggleVerticalRead()

            R.id.omnibox_refresh -> if (url != null && ninjaWebView.isLoadFinish) {
                ninjaWebView.reload()
            } else if (url == null) {
                val text = getString(R.string.toast_load_error) + ": " + url
                NinjaToast.show(this, text)
            } else {
                ninjaWebView.stopLoading()
            }
            R.id.toolbar_setting -> ToolbarConfigDialog(this).show()
            R.id.toolbar_increase_font -> increaseFontSize()
            R.id.toolbar_decrease_font -> decreaseFontSize()
            R.id.toolbar_fullscreen -> fullscreen()
            R.id.toolbar_rotate -> rotateScreen()
            R.id.toolbar_translate -> showTranslation()
            R.id.toolbar_close_tab -> removeAlbum(currentAlbumController!!)
            R.id.toolbar_input_url -> focusOnInput()
            R.id.toolbar_new_tab -> {
                addAlbum(getString(R.string.app_name), "", true)
                focusOnInput()
            }
            else -> {
            }
        }
    }

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
        val context = this
        try {
            lifecycleScope.launch {
                if (bookmarkManager.existsUrl(currentUrl)) {
                    NinjaToast.show(context, R.string.toast_newTitle)
                } else {
                    BookmarkEditDialog(
                            this@BrowserActivity,
                            bookmarkManager,
                            Bookmark(nonNullTitle, currentUrl),
                            {
                                hideKeyboard()
                                NinjaToast.show(this@BrowserActivity, R.string.toast_edit_successful)
                                isAutoCompleteOutdated = true
                            },
                            { hideKeyboard() }
                    ).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(this, R.string.toast_error)
        }
    }

    private fun toggleTouchTurnPageFeature() {
        config.enableTouchTurn = !config.enableTouchTurn
        updateTouchView()
    }

    private fun updateTouchView() {
        val fabResourceId = if (config.enableTouchTurn) R.drawable.icon_overflow_fab else R.drawable.ic_touch_disabled
        fabImageButtonNav.setImageResource(fabResourceId)
        val touchResourceId = if (config.enableTouchTurn) R.drawable.ic_touch_enabled else R.drawable.ic_touch_disabled
        binding.omniboxTouch.setImageResource(touchResourceId)
    }

    // Methods
    private fun showFontSizeChangeDialog() =
            dialogManager.showFontSizeChangeDialog { changeFontSize(it) }

    private fun changeFontSize(size: Int) {
        config.fontSize = size
        ninjaWebView.settings.textZoom = size
    }

    private fun increaseFontSize() = changeFontSize(config.fontSize + 20)

    private fun decreaseFontSize() {
        if (config.fontSize <= 50) return

        changeFontSize(config.fontSize - 20)
    }

    private fun showPdfFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_PDF
        intent.putExtra(Intent.EXTRA_TITLE, "einkbro.pdf")
        startActivityForResult(intent, WRITE_PDF_REQUEST_CODE)
    }

    private fun saveEpub(fileUri: Uri) {
        lifecycleScope.launch(Dispatchers.Main) {
            val isNewFile = (DocumentFile.fromSingleUri(this@BrowserActivity, fileUri)?.length()
                    ?: 0).toInt() == 0

            val bookName = if (isNewFile) epubManager.getBookName() else ""
            val chapterName = epubManager.getChapterName(ninjaWebView.title)

            if (bookName != null && chapterName != null) {

                val progressDialog = ProgressDialog(this@BrowserActivity, R.style.TouchAreaDialog).apply {
                    setTitle(R.string.saving_epub)
                    show()
                }

                val rawHtml = ninjaWebView.getRawHtml()
                epubManager.saveEpub(
                        isNewFile,
                        fileUri,
                        rawHtml,
                        bookName,
                        chapterName,
                        ninjaWebView.url ?: "") { savedBookName ->
                    progressDialog.dismiss()
                    HelperUnit.openFile(this@BrowserActivity, fileUri, Constants.MIME_TYPE_EPUB)

                    // save epub file info to preference
                    val bookUri = fileUri.toString()
                    if (config.savedEpubFileInfos.none { it.uri == bookUri }) {
                        config.addSavedEpubFile(EpubFileInfo(savedBookName, bookUri))
                    }
                }
            }
        }
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
    private fun dispatchIntent(intent: Intent) {
        if (overviewDialogController.isVisible()) {
            overviewDialogController.hide()
        }

        when (intent.action) {
            "", Intent.ACTION_MAIN -> { // initial case
                if (currentAlbumController == null) { // newly opened Activity
                    if ((shouldLoadTabState || config.shouldSaveTabs) &&
                            config.savedAlbumInfoList.isNotEmpty()) {
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
                                    foreground = (index == savedIndex))
                        }
                    } else {
                        addAlbum()
                    }
                }
            }
            ACTION_VIEW -> {
                // if webview for that url already exists, show the original tab, otherwise, create new
                val url = intent.data?.toNormalScheme()?.toString() ?: return
                getUrlMatchedBrowser(url)?.let { showAlbum(it) } ?: addAlbum(url = url)
            }
            Intent.ACTION_WEB_SEARCH -> addAlbum(url = intent.getStringExtra(SearchManager.QUERY))
            "sc_history" -> {
                addAlbum()
                openHistoryPage()
            }
            "sc_home" -> {
                addAlbum(config.favoriteUrl)
            }
            "sc_bookmark" -> {
                addAlbum()
                openBookmarkPage()
            }
            Intent.ACTION_SEND -> {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT)
                addAlbum(url = url)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun initToolbar() {
        mainToolbar = findViewById(R.id.main_toolbar)
        omniboxTitle = findViewById(R.id.omnibox_title)
        progressBar = findViewById(R.id.main_progress_bar)
        if (config.darkMode == DarkMode.FORCE_ON) {
            val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                progressBar.progressTintMode = PorterDuff.Mode.LIGHTEN
            }
        }
        initFAB()
        binding.omniboxSetting.setOnLongClickListener {
            showFastToggleDialog()
            false
        }
        binding.omniboxSetting.setOnClickListener { showMenuDialog() }
        if (sp.getBoolean("sp_gestures_use", true)) {
            val onNavButtonTouchListener = object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture("setting_gesture_nav_up")
                override fun onSwipeBottom() = performGesture("setting_gesture_nav_down")
                override fun onSwipeRight() = performGesture("setting_gesture_nav_right")
                override fun onSwipeLeft() = performGesture("setting_gesture_nav_left")
            }
            fabImageButtonNav.setOnTouchListener(onNavButtonTouchListener)
            binding.omniboxSetting.setOnTouchListener(onNavButtonTouchListener)
            val onTitleTouchListener = object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture("setting_gesture_tb_up")
                override fun onSwipeBottom() = performGesture("setting_gesture_tb_down")
                override fun onSwipeRight() = performGesture("setting_gesture_tb_right")
                override fun onSwipeLeft() = performGesture("setting_gesture_tb_left")
            }
            binding.omniboxTitle.setOnTouchListener(onTitleTouchListener)
        }
        binding.omniboxInput.setOnEditorActionListener(OnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent.action == ACTION_DOWN && keyEvent.keyCode == KEYCODE_ENTER)
            ) {
                binding.omniboxInput.dismissDropDown()
                isAutoCompleteOutdated = true

                val query = binding.omniboxInput.text.toString().trim { it <= ' ' }
                if (query.isEmpty()) {
                    NinjaToast.show(this, getString(R.string.toast_input_empty))
                    return@OnEditorActionListener true
                }
                updateAlbum(query)
                showToolbar()
            }
            false
        })
        binding.omniboxInput.onFocusChangeListener = OnFocusChangeListener { _, _ ->
            if (!this::ninjaWebView.isInitialized) return@OnFocusChangeListener

            if (binding.omniboxInput.hasFocus()) {
                binding.omniboxInput.setText(ninjaWebView.url)
                binding.omniboxInput.setSelection(0, binding.omniboxInput.text.toString().length)
                toolbarViewController.hide()
            } else {
                toolbarViewController.show()
                omniboxTitle.text = ninjaWebView.title
                hideKeyboard()
            }
        }

        // long click on overview, show bookmark
        binding.omniboxTabcount.setOnLongClickListener {
            config.isIncognitoMode = !config.isIncognitoMode
            true
        }

        // scroll to top
        binding.omniboxPageUp.setOnLongClickListener {
            ninjaWebView.jumpToTop()
            true
        }

        // hide bottom bar when refresh button is long pressed.
        binding.omniboxRefresh.setOnLongClickListener {
            fullscreen()
            true
        }

        binding.omniboxBookmark.setOnClickListener { openBookmarkPage() }
        binding.omniboxBookmark.setOnLongClickListener { saveBookmark(); true }
        binding.toolbarTranslate.setOnLongClickListener { showTranslationConfigDialog(); true }

        binding.omniboxBack.setOnLongClickListener { openHistoryPage(5); true }

        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        toolbarViewController.reorderIcons()
    }

    private fun showTranslationConfigDialog() {
        maybeInitTwoPaneController()
        twoPaneController.showTranslationConfigDialog()
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            ConfigManager.K_TOOLBAR_ICONS_FOR_LARGE,
            ConfigManager.K_TOOLBAR_ICONS -> {
                toolbarViewController.reorderIcons()
            }
            ConfigManager.K_CUSTOM_FONT_ENABLE -> {
                if(config.enableCustomFont) {
                    ninjaWebView.updateCssStyle()
                } else {
                    ninjaWebView.reload()
                }
            }
            ConfigManager.K_BOLD_FONT -> {
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
            ConfigManager.K_FONT_STYLE_SERIF -> {
                if (config.fontStyleSerif) {
                    ninjaWebView.updateCssStyle()
                } else {
                    ninjaWebView.reload()
                }
            }
            ConfigManager.K_CUSTOM_FONT -> {
                if (config.enableCustomFont) {
                    ninjaWebView.updateCssStyle()
                }
            }
            ConfigManager.K_IS_INCOGNITO_MODE -> {
                ninjaWebView.incognito = config.isIncognitoMode
                updateWebViewCountUI()
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
            ConfigManager.K_DESKTOP -> ninjaWebView.updateDesktopMode()
            ConfigManager.K_DARK_MODE -> showRestartConfirmDialog()
        }
    }

    private fun initFAB() {
        fabImageButtonNav = findViewById(R.id.fab_imageButtonNav)
        val params = RelativeLayout.LayoutParams(fabImageButtonNav.layoutParams.width, fabImageButtonNav.layoutParams.height)
        when (config.fabPosition) {
            FabPosition.Left -> {
                fabImageButtonNav.layoutParams = params.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }
            FabPosition.Right -> {
                fabImageButtonNav.layoutParams = params.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }
            FabPosition.Center -> {
                fabImageButtonNav.layoutParams = params.apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }
        }

        ViewUnit.expandViewTouchArea(fabImageButtonNav, 20.dp(this))
        fabImageButtonNav.setOnClickListener { showToolbar() }
        fabImageButtonNav.setOnLongClickListener {
            showFastToggleDialog()
            false
        }
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
            CloseTab -> removeAlbum(currentAlbumController!!)
            PageUp -> ninjaWebView.pageUpWithNoAnimation()
            PageDown -> ninjaWebView.pageDownWithNoAnimation()
            GestureType.Bookmark -> openBookmarkPage()
        }
    }

    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager.bookmarkDao)
    }

    private fun initOverview() {
        overviewDialogController = OverviewDialogController(
                this,
                bookmarkViewModel,
                binding.layoutOverview,
                recordDb,
                gotoUrlAction = { url -> updateAlbum(url) },
                addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
                onBookmarksChanged = { isAutoCompleteOutdated = true },
                onHistoryChanged = { isAutoCompleteOutdated = true },
                splitScreenAction = { url -> toggleSplitScreen(url) }
        )
    }

    private fun openHistoryPage(amount: Int = 0) = overviewDialogController.openHistoryPage(amount)

    private fun openBookmarkPage() = overviewDialogController.openBookmarkPage()

    private fun initSearchPanel() {
        searchPanel = binding.mainSearchPanel
        searchBox = binding.mainSearchBox
        searchBox.addTextChangedListener(searchBoxTextChangeListener)
        searchBox.setOnEditorActionListener(searchBoxEditorActionListener)
        binding.mainSearchUp.setOnClickListener { searchUp() }
        binding.mainSearchDown.setOnClickListener { searchDown() }
        binding.mainSearchCancel.setOnClickListener { hideSearchPanel() }
    }

    private val searchBoxEditorActionListener = object : OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false
            }
            if (searchBox.text.toString().isEmpty()) {
                NinjaToast.show(this@BrowserActivity, getString(R.string.toast_input_empty))
                return true
            }
            return false
        }
    }

    private val searchBoxTextChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            (currentAlbumController as NinjaWebView?)?.findAllAsync(s.toString())
        }
    }

    private fun searchUp() {
        val query = searchBox.text.toString()
        if (query.isEmpty()) {
            NinjaToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        hideKeyboard()
        (currentAlbumController as NinjaWebView).findNext(false)
    }

    private fun searchDown() {
        val query = searchBox.text.toString()
        if (query.isEmpty()) {
            NinjaToast.show(this, getString(R.string.toast_input_empty))
            return
        }
        hideKeyboard()
        (currentAlbumController as NinjaWebView).findNext(true)
    }

    private fun showFastToggleDialog() {
        if (!this::ninjaWebView.isInitialized) return

        FastToggleDialog(this, ninjaWebView.url ?: "") {
            ninjaWebView.initPreferences()
            ninjaWebView.reload()
        }.show()
    }

    override fun addNewTab(url: String?) = addAlbum(url = url)

    private fun getUrlMatchedBrowser(url: String): NinjaWebView? {
        return browserContainer.list().firstOrNull { it.albumUrl == url } as NinjaWebView?
    }

    private var preloadedWebView: NinjaWebView? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun addAlbum(
            title: String = "",
            url: String? = config.favoriteUrl,
            foreground: Boolean = true,
            incognito: Boolean = false
    ) {
        if (url == null) return

        val newWebView = (preloadedWebView ?: NinjaWebView(this, this)).apply {
            this.albumTitle = title
            this.incognito = incognito
        }
        preloadedWebView = null
        newWebView.postDelayed({
            if (preloadedWebView == null) {
                preloadedWebView = NinjaWebView(this, this)
            }
        }, 2000)

        ViewUnit.bound(this, newWebView)

        val albumView = newWebView.albumView
        if (currentAlbumController != null) {
            val index = browserContainer.indexOf(currentAlbumController) + 1
            browserContainer.add(newWebView, index)
            overviewDialogController.addTabPreview(albumView, index)
        } else {
            browserContainer.add(newWebView)
            overviewDialogController.addTabPreview(albumView, browserContainer.size() - 1)
        }
        updateWebViewCount()

        if (config.isMultitouchEnabled) {
            val multiTouchTouchListener = object: MultitouchListener(this@BrowserActivity, newWebView) {
                override fun onSwipeTop() = performGesture("setting_multitouch_up")
                override fun onSwipeBottom() = performGesture("setting_multitouch_down")
                override fun onSwipeRight() = performGesture("setting_multitouch_right")
                override fun onSwipeLeft() = performGesture("setting_multitouch_left")
            }

            newWebView.setOnTouchListener(multiTouchTouchListener)
        }

        if (!foreground) {
            ViewUnit.bound(this, newWebView)
            newWebView.loadUrl(url)
            newWebView.deactivate()
        } else {
            showToolbar()
            showAlbum(newWebView)
            if (url.isNotEmpty()) {
                newWebView.loadUrl(url)
            }
        }

        updateSavedAlbumInfo()
    }

    private fun updateSavedAlbumInfo() {
        val albumControllers = browserContainer.list()
        val albumInfoList = albumControllers
                .filter { it.albumUrl.isNotBlank() }
                .map { controller -> AlbumInfo(controller.albumTitle, controller.albumUrl) }
        config.savedAlbumInfoList = albumInfoList
        config.currentAlbumIndex = browserContainer.indexOf(currentAlbumController)
        // fix if current album is still with null url
        if (albumInfoList.isNotEmpty() && config.currentAlbumIndex >= albumInfoList.size) {
            config.currentAlbumIndex = albumInfoList.size - 1
        }
    }

    private fun updateWebViewCount() {
        binding.omniboxTabcount.text = browserContainer.size().toString()
        updateWebViewCountUI()
    }

    private fun updateWebViewCountUI() {
        binding.omniboxTabcount.setBackgroundResource(
                if (config.isIncognitoMode
                        || (this::ninjaWebView.isInitialized && ninjaWebView.incognito)) R.drawable.button_border_bg_dash else R.drawable.button_border_bg
        )
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
        updateSavedAlbumInfo()

        if (browserContainer.size() <= 1) {
            finish()
        } else {
            closeTabConfirmation {
                overviewDialogController.removeTabView(controller.albumView)
                val removeIndex = browserContainer.indexOf(controller)
                val currentIndex = browserContainer.indexOf(currentAlbumController)
                browserContainer.remove(controller)
                // only refresh album when the delete one is current one
                if (removeIndex == currentIndex) {
                    val newIndex = max(0, removeIndex - 1)
                    showAlbum(browserContainer[newIndex])
                }
                updateWebViewCount()
            }
        }
    }

    private fun updateTitle() {
        if (!this::ninjaWebView.isInitialized) return

        if (this::ninjaWebView.isInitialized && ninjaWebView === currentAlbumController) {
            omniboxTitle.text = ninjaWebView.title
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

                val height = floor(x = ninjaWebView.contentHeight * ninjaWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ninjaWebView.height
                val cutoff = height - webViewHeight - 112 * resources.displayMetrics.density.roundToInt()
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

    override fun addHistory(url: String) {
        recordDb.addHistory(Record(ninjaWebView.albumTitle, url, System.currentTimeMillis()))
    }

    override fun updateProgress(progress: Int) {
        DebugT("updateProgress:$progress")
        progressBar.progress = progress

        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true)
            progressBar.visibility = VISIBLE
        } else {
            updateRefresh(false)
            progressBar.visibility = GONE

            scrollChange()
        }
    }

    // to keep track of whether data is changed for auto completion
    private var isAutoCompleteOutdated = true
    private fun focusOnInput() {
        toolbarViewController.hide()
        binding.omniboxInput.requestFocus()
        binding.omniboxInput.selectAll()

        if (isAutoCompleteOutdated) {
            updateAutoComplete()
            isAutoCompleteOutdated = false
        }
        showKeyboard()
    }

    private var isRunning = false
    private fun updateRefresh(running: Boolean) {
        if (!isRunning && running) {
            isRunning = true
            binding.omniboxRefresh.setImageResource(R.drawable.ic_stop)
        } else if (isRunning && !running) {
            isRunning = false
            binding.omniboxRefresh.setImageResource(R.drawable.icon_refresh)
        }
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
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )

        }
        val decorView = window.decorView as FrameLayout
        decorView.addView(
                fullscreenHolder,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
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

        // prevent inputBox to get the focus
        binding.omniboxInput.isEnabled = false

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

        // re-enable inputBox after fullscreen view is removed.
        binding.omniboxInput.isEnabled = true
        return true
    }

    private var previousKeyEvent: KeyEvent? = null
    override fun handleKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action != ACTION_DOWN) return false
        if (ninjaWebView.hitTestResult.type == HitTestResult.EDIT_TEXT_TYPE) return false

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
                // vim bindings
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
                KeyEvent.KEYCODE_D -> removeAlbum(currentAlbumController!!)
                KeyEvent.KEYCODE_T -> {
                    addAlbum(getString(R.string.app_name), "", true)
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
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (config.useUpDownPageTurn) {
                        ninjaWebView.pageDownWithNoAnimation()
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (config.useUpDownPageTurn) {
                        ninjaWebView.pageUpWithNoAnimation()
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

    private fun showContextMenuLinkDialog(url: String?, hitTestResult: HitTestResult) {
        if (url == null &&
                !listOf(HitTestResult.IMAGE_TYPE, HitTestResult.IMAGE_ANCHOR_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE, HitTestResult.ANCHOR_TYPE)
                        .contains(hitTestResult.type)) return

        val nonNullUrl = url ?: hitTestResult.extra ?: ""
        val dialogView = DialogMenuContextLinkBinding.inflate(layoutInflater)
        val dialog = dialogManager.showOptionDialog(dialogView.root)
        dialogView.contextLinkNewTab.setOnClickListener {
            dialog.dismissWithAction {
                addAlbum(getString(R.string.app_name), nonNullUrl, false)
                NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            }
        }
        dialogView.contextLinkSplitScreen.setOnClickListener {
            dialog.dismissWithAction { toggleSplitScreen(nonNullUrl) }
        }
        dialogView.contextLinkShareLink.setOnClickListener {
            dialog.dismissWithAction {
                if (prepareRecord()) NinjaToast.show(this, getString(R.string.toast_share_failed))
                else IntentUnit.share(this, "", nonNullUrl)
            }
        }
        dialogView.contextLinkOpenWith.setOnClickListener {
            dialog.dismissWithAction { HelperUnit.showBrowserChooser(this@BrowserActivity, nonNullUrl, getString(R.string.menu_open_with)) }
        }
        dialogView.contextLinkSaveBookmark.setOnClickListener {
            dialog.dismissWithAction { saveBookmark(nonNullUrl, title = "") }
        }
        dialogView.contextLinkNewTabOpen.setOnClickListener {
            dialog.dismissWithAction { addAlbum(getString(R.string.app_name), nonNullUrl) }
        }
        dialogView.menuSavePdf.setOnClickListener {
            dialog.dismissWithAction { showSavePdfDialog(nonNullUrl) }
        }

        if (hitTestResult.extra != null) {
            dialogView.menuRemoveAd.visibility = VISIBLE
            dialogView.menuRemoveAd.setOnClickListener {
                dialog.dismissWithAction { confirmAdSiteAddition(hitTestResult.extra ?: "") }
            }
        }
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

    override fun onLongPress(url: String?) =
            showContextMenuLinkDialog(url, ninjaWebView.hitTestResult)

    private fun showSavePdfDialog(url: String) {
        dialogManager.showSavePdfDialog(
                url = url,
                savePdf = { pdfUrl, fileName -> savePdf(pdfUrl, fileName) },
        )
    }

    private fun savePdf(url: String, fileName: String) {
        if (HelperUnit.needGrantStoragePermission(this)) {
            return
        }

        val source = Uri.parse(url)
        val request = DownloadManager.Request(source).apply {
            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val dm = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
        dm.enqueue(request)
        hideKeyboard()
    }

    @SuppressLint("RestrictedApi")
    private fun showToolbar() {
        if (!searchOnSite) {
            fabImageButtonNav.visibility = INVISIBLE
            searchPanel.visibility = GONE
            mainToolbar.visibility = VISIBLE
            binding.appBar.visibility = VISIBLE
            toolbarViewController.show()
            hideKeyboard()
            showStatusBar()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun fullscreen() {
        if (!searchOnSite) {
            if (config.fabPosition != FabPosition.NotShow) {
                fabImageButtonNav.visibility = VISIBLE
            }
            searchPanel.visibility = GONE
            binding.appBar.visibility = GONE
            hideStatusBar()
        }
    }

    private fun hideSearchPanel() {
        searchOnSite = false
        searchBox.setText("")
        showToolbar()
    }

    private fun hideStatusBar() = window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    private fun showStatusBar() = window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    @SuppressLint("RestrictedApi")
    private fun showSearchPanel() {
        searchOnSite = true
        fabImageButtonNav.visibility = INVISIBLE
        mainToolbar.visibility = GONE
        searchPanel.visibility = VISIBLE
        omniboxTitle.visibility = GONE
        binding.appBar.visibility = VISIBLE
        searchBox.requestFocus()
        showKeyboard()
    }

    private fun showSaveEpubDialog() = dialogManager.showSaveEpubDialog { uri ->
        if (uri == null) {
            epubManager.showEpubFilePicker()
        } else {
            saveEpub(uri)
        }
    }

    private fun openSavedEpub() = if (config.savedEpubFileInfos.isEmpty()) {
        NinjaToast.show(this, "no saved epub!")
    } else {
        dialogManager.showSaveEpubDialog(shouldAddNewEpub = false) {
            openUri(this@BrowserActivity, it)
        }
    }

    private fun showMenuDialog(): Boolean {
        MenuDialog(
                this,
                ninjaWebView,
                { updateAlbum(sp.getString("favoriteURL", "https://github.com/plateaukao/browser")) },
                { removeAlbum(currentAlbumController!!) },
                this::saveBookmark,
                this::showSearchPanel,
                this::showSaveEpubDialog,
                this::openSavedEpub,
                this::printPDF,
                this::showFontSizeChangeDialog,
                this::saveScreenshot,
                this::toggleSplitScreen,
                this::toggleTouchTurnPageFeature,
                this::showTranslation,
                this::showTranslationConfigDialog,
        ).show()
        return true
    }

    private fun toggleSplitScreen(url: String? = null) {
        maybeInitTwoPaneController()
        if(twoPaneController.isSecondPaneDisplayed() && url == null) {
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

    private fun showKeyboard() {
        ViewUnit.showKeyboard(this)
    }

    private fun hideKeyboard() {
        ViewUnit.hideKeyboard(this)
    }

    private var mActionMode: ActionMode? = null
    override fun onActionModeStarted(mode: ActionMode) {
        if (mActionMode == null) {
            var isNaverDictExist = false
            mActionMode = mode
            val menu = mode.menu
            val toBeAddedLaterList: MutableList<MenuItem> = mutableListOf()
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                if (item.intent?.component?.packageName == "info.plateaukao.naverdict") {
                    isNaverDictExist = true
                    break
                }
            }

            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                if (item.intent?.component?.packageName == "info.plateaukao.naverdict") {
                    continue
                }
                if (isNaverDictExist && item.intent?.component?.packageName == "com.onyx.dict") {
                    continue
                }

                toBeAddedLaterList.add(item)
            }

            if (isNaverDictExist) {
                for (item in toBeAddedLaterList) {
                    menu.removeItem(item.itemId)
                }
                val subMenu = menu.addSubMenu("Others")
                for (item in toBeAddedLaterList) {
                    if (item.title.equals("Copy")) {
                        menu.add(item.groupId, item.itemId, Menu.NONE, item.title)
                    } else {
                        subMenu.add(item.groupId, item.itemId, Menu.NONE, item.title)
                    }
                }
            }
        }
        super.onActionModeStarted(mode)
    }

    override fun onPause() {
        super.onPause()
        mActionMode?.finish()
        mActionMode = null
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        mActionMode = null
    }

    companion object {
        private const val TAG = "BrowserActivity"
        private const val INPUT_FILE_REQUEST_CODE = 1
        const val WRITE_EPUB_REQUEST_CODE = 2
        private const val WRITE_PDF_REQUEST_CODE = 3
        private const val K_SHOULD_LOAD_TAB_STATE = "k_should_load_tab_state"
    }
}

private fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}