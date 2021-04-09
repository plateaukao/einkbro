package de.baumann.browser.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.app.SearchManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView.HitTestResult
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.ActivityMainBinding
import de.baumann.browser.browser.*
import de.baumann.browser.database.BookmarkList
import de.baumann.browser.database.Record
import de.baumann.browser.database.RecordAction
import de.baumann.browser.epub.EpubManager
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.FabPosition
import de.baumann.browser.preference.TouchAreaType
import de.baumann.browser.service.ClearService
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.util.Constants
import de.baumann.browser.view.*
import de.baumann.browser.view.adapter.*
import de.baumann.browser.view.dialog.FastToggleDialog
import de.baumann.browser.view.dialog.MenuDialog
import de.baumann.browser.view.dialog.TextInputDialog
import de.baumann.browser.view.dialog.TouchAreaDialog
import de.baumann.browser.view.toolbaricons.ToolbarAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.system.exitProcess


class BrowserActivity : AppCompatActivity(), BrowserController, View.OnClickListener {
    private lateinit var adapter: RecordAdapter

    private lateinit var btnOpenStartPage: ImageButton
    private lateinit var btnOpenBookmark: ImageButton
    private lateinit var btnOpenHistory: ImageButton
    private lateinit var btnOpenMenu: ImageButton
    private lateinit var fabImagebuttonnav: ImageButton
    private lateinit var inputBox: AutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchBox: EditText
    private lateinit var overviewLayout: ViewGroup
    private lateinit var ninjaWebView: NinjaWebView
    private lateinit var recyclerView: RecyclerView
    private lateinit var omniboxTitle: TextView
    private lateinit var tabScrollview: HorizontalScrollView
    private lateinit var overviewPreview: LinearLayout
    private lateinit var touchAreaPageUp: View
    private lateinit var touchAreaPageDown: View

    private var bottomSheetDialog: Dialog? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Layouts
    private lateinit var mainToolbar: RelativeLayout
    private lateinit var searchPanel: ViewGroup
    private lateinit var mainContentLayout: FrameLayout
    private lateinit var tab_container: LinearLayout
    private lateinit var open_startPageView: View
    private lateinit var open_bookmarkView: View
    private lateinit var open_historyView: View

    private var fullscreenHolder: FrameLayout? = null

    // Others
    private var title: String? = null
    private var url: String? = null
    private var overViewTab: String? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val config: ConfigManager by lazy { ConfigManager(this) }
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

    private lateinit var bookmarkDB: BookmarkList

    private val epubManager: EpubManager by lazy { EpubManager(this) }

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

        bookmarkDB = BookmarkList(this).apply { open() }

        WebView.enableSlowWholeDocumentDraw()

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
            sp.edit().putString("saved_key", sb.toString()).apply()
            sp.edit().putString("saved_key_ok", "yes").apply()
            sp.edit().putString("setting_gesture_tb_up", "08").apply()
            sp.edit().putString("setting_gesture_tb_down", "01").apply()
            sp.edit().putString("setting_gesture_tb_left", "07").apply()
            sp.edit().putString("setting_gesture_tb_right", "06").apply()
            sp.edit().putString("setting_gesture_nav_up", "04").apply()
            sp.edit().putString("setting_gesture_nav_down", "05").apply()
            sp.edit().putString("setting_gesture_nav_left", "03").apply()
            sp.edit().putString("setting_gesture_nav_right", "02").apply()
            sp.edit().putBoolean(getString(R.string.sp_location), false).apply()
        }
        mainContentLayout = findViewById(R.id.main_content)
        initToolbar()
        initSearchPanel()
        initOverview()
        initTouchArea()
        AdBlock(this) // For AdBlock cold boot
        Javascript(this)
        Cookie(this)
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                bottomSheetDialog = BottomSheetDialog(this@BrowserActivity, R.style.BottomSheetDialog)
                val dialogView = View.inflate(this@BrowserActivity, R.layout.dialog_action, null)
                dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.toast_downloadComplete)
                dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    hideBottomSheetDialog()
                }
                dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
                bottomSheetDialog?.setContentView(dialogView)
                bottomSheetDialog?.show()
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, filter)
        dispatchIntent(intent)
    }

    private fun initTouchArea() {
        updateTouchAreaType()
        binding.omniboxTouch.setOnLongClickListener {
            TouchAreaDialog(BrowserActivity@ this).show()
            true
        }
        sp.registerOnSharedPreferenceChangeListener(touchAreaChangeListener)

        val isEnabled = sp.getBoolean("sp_enable_touch", false)
        if (isEnabled) {
            enableTouch()
        } else {
            disableTouch()
        }
    }

    private val touchAreaChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key.equals("sp_touch_area_hint")) {
            if (config.touchAreaHint) {
                showTouchAreaHint()
            } else {
                hideTouchAreaHint()
            }
        }
        if (key.equals("sp_touch_area_type")) {
            updateTouchAreaType()
        }
    }

    private fun updateTouchAreaType() {
        // hide current one, and reset listener
        if (this::touchAreaPageUp.isInitialized) {
            with(touchAreaPageUp) {
                visibility = INVISIBLE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
            with(touchAreaPageDown) {
                visibility = INVISIBLE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
        }

        when(TouchAreaType.values()[sp.getInt("sp_touch_area_type", 0)]) {
            TouchAreaType.BottomLeftRight -> {
                touchAreaPageUp = findViewById(R.id.touch_area_bottom_left)
                touchAreaPageDown = findViewById(R.id.touch_area_bottom_right)
            }
            TouchAreaType.Left -> {
                touchAreaPageUp = findViewById(R.id.touch_area_left_1)
                touchAreaPageDown = findViewById(R.id.touch_area_left_2)
            }
            TouchAreaType.Right -> {
                touchAreaPageUp = findViewById(R.id.touch_area_right_1)
                touchAreaPageDown = findViewById(R.id.touch_area_right_2)
            }
            }

        val isTouchEnabled = sp.getBoolean("sp_enable_touch", false)
        with(touchAreaPageUp) {
            if (isTouchEnabled) visibility = VISIBLE
            setOnClickListener { ninjaWebView.pageUpWithNoAnimation() }
            setOnLongClickListener { ninjaWebView.jumpToTop(); true }
        }
        with(touchAreaPageDown) {
            if (isTouchEnabled) visibility = VISIBLE
            setOnClickListener { ninjaWebView.pageDownWithNoAnimation() }
            setOnLongClickListener { ninjaWebView.jumpToBottom(); true }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == WRITE_EPUB_REQUEST_CODE && resultCode == RESULT_OK) {
            val nonNullData = data?.data ?: return
            saveEpub(nonNullData)

            return
        }

        if (requestCode == WRITE_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            val nonNullData = data?.data ?: return
            printPDF()

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

        super.onActivityResult(requestCode, resultCode, data)
        return
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

        updateOmnibox()
        overridePendingTransition(0, 0)
    }

    private fun showRestartConfirmDialog() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_action, null)
        dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.toast_restart)
        dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
            dialog.dismiss()
            restartApp()
        }

        dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { dialog.cancel() }
        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun restartApp() {
        finishAffinity(); // Finishes all activities.
        startActivity(packageManager.getLaunchIntentForPackage(packageName));    // Start the launch activity
        exitProcess(0)
    }

    private fun showFileListConfirmDialog() {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_action, null)
        dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.toast_downloadComplete)
        dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            hideBottomSheetDialog()
        }
        dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
    }

    public override fun onDestroy() {
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            val toClearService = Intent(this, ClearService::class.java)
            startService(toClearService)
        }
        BrowserContainer.clear()
        IntentUnit.setContext(null)
        unregisterReceiver(downloadReceiver)
        bookmarkDB.close()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                ninjaWebView.pageDownWithNoAnimation()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                ninjaWebView.pageUpWithNoAnimation()
                return true
            }
            KeyEvent.KEYCODE_MENU -> return showMenuDialog()
            KeyEvent.KEYCODE_BACK -> {
                hideKeyboard()
                if (overviewLayout.visibility == VISIBLE) {
                    hideOverview()
                    return true
                }
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    return onHideCustomView()
                } else if (mainToolbar.visibility == GONE && sp.getBoolean("sp_toolbarShow", true)) {
                    showToolbar()
                } else if (binding.iconBar.visibility == GONE) {
                    inputBox.clearFocus()
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
                inputBox.performClick()
            }
        }
        return false
    }

    @Synchronized
    override fun showAlbum(controller: AlbumController) {
        if (currentAlbumController != null) {
            currentAlbumController?.deactivate()
            val av = controller as View
            mainContentLayout.removeAllViews()
            mainContentLayout.addView(av)
        } else {
            mainContentLayout.removeAllViews()
            mainContentLayout.addView(controller as View)
        }
        currentAlbumController = controller
        currentAlbumController?.activate()
        updateOmnibox()
    }

    override fun updateAutoComplete() {
        val action = RecordAction(this)
        action.open(false)
        val list = action.listEntries(this, true)
        action.close()
        val adapter = CompleteAdapter(this, R.layout.complete_item, list)
        inputBox.setAdapter(adapter)
        adapter.notifyDataSetChanged()
        inputBox.threshold = 1
        inputBox.dropDownVerticalOffset = -16
        inputBox.dropDownWidth = ViewUnit.getWindowWidth(this)
        inputBox.onItemClickListener = OnItemClickListener { _, view, _, _ ->
            val url = (view.findViewById<View>(R.id.complete_item_url) as TextView).text.toString()
            updateAlbum(url)
            hideKeyboard()
        }
    }

    private fun showOverview() {
        showCurrentTabInOverview()
        overviewLayout.visibility = VISIBLE

        currentAlbumController?.deactivate()
        currentAlbumController?.activate()
        binding.root.postDelayed({
            tabScrollview.scrollTo(currentAlbumController?.albumView?.left ?: 0, 0)
        }, 250)
    }

    override fun hideOverview() {
        overviewLayout.visibility = INVISIBLE
    }

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
            R.id.button_size -> showFontSizeChangeDialog()
            R.id.tab_plus_bottom -> {
                hideOverview()
                addAlbum(getString(R.string.app_name), "", true)
                inputBox.requestFocus()
                showKeyboard()
            }
            R.id.menu_save_pdf -> showPdfFilePicker()

            //R.id.menu_save_epub -> showEpubFilePicker()

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
                removeAlbum(currentAlbumController!!)
            }
            R.id.omnibox_page_up -> ninjaWebView.pageUpWithNoAnimation()
            R.id.omnibox_page_down -> {
                keepToolbar = true
                ninjaWebView.pageDownWithNoAnimation()
            }
            R.id.omnibox_vertical_read -> ninjaWebView.toggleVerticalRead()

            R.id.omnibox_refresh -> if (url != null && ninjaWebView.isLoadFinish) {
                if (url?.startsWith("https://") != true) {
                    bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                    val dialogView = View.inflate(this, R.layout.dialog_action, null)
                    val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
                    textView.setText(R.string.toast_unsecured)
                    dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                        hideBottomSheetDialog()
                        ninjaWebView.loadUrl(url?.replace("http://", "https://") ?: "")
                    }
                    dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                        hideBottomSheetDialog()
                        ninjaWebView.reload()
                    }
                    bottomSheetDialog?.setContentView(dialogView)
                    bottomSheetDialog?.show()
                } else {
                    ninjaWebView.reload()
                }
            } else if (url == null) {
                val text = getString(R.string.toast_load_error) + ": " + url
                NinjaToast.show(this, text)
            } else {
                ninjaWebView.stopLoading()
            }
            R.id.omnibox_bar_setting -> openToolbarSetting()
            else -> {
            }
        }
    }

    private fun openToolbarSetting() {
        val intent = Intent(this, Settings_UIActivity::class.java)
                .putExtra(Constants.ARG_LAUNCH_TOOLBAR_SETTING, true)
        startActivity(intent)
        overridePendingTransition(0, 0);
    }
    private fun saveBookmark() {
        val currentUrl = ninjaWebView.url
        try {
            if (bookmarkDB.isExist(currentUrl)) {
                NinjaToast.show(this, R.string.toast_newTitle)
            } else {
                bookmarkDB.insert(HelperUnit.secString(ninjaWebView.title), currentUrl, "", "", "01")
                NinjaToast.show(this, R.string.toast_edit_successful)
                initBookmarkList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(this, R.string.toast_error)
        }
    }

    private fun showBrowserChooser(url: String, title: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(Intent.createChooser(intent, title))
    }

    private fun  toggleTouchTurnPageFeature() {
        // off: turn on
        //if (sp.getBoolean("sp_enable_touch", false)) {
        if(binding.omniboxTouch.alpha != 1.0F) {
            enableTouch()
            sp.edit(commit = true) { putBoolean("sp_enable_touch", true) }
        } else { // turn off
            disableTouch()
            sp.edit(commit = true) { putBoolean("sp_enable_touch", false) }
        }
    }
    private fun enableTouch() {
        binding.omniboxTouch.alpha = 1.0F

        touchAreaPageUp.visibility = VISIBLE
        touchAreaPageDown.visibility = VISIBLE

        fabImagebuttonnav.setImageResource(R.drawable.ic_touch_enabled)
        binding.omniboxTouch.setImageResource(R.drawable.ic_touch_enabled)
        showTouchAreaHint()
    }

    private fun disableTouch() {
        binding.omniboxTouch.alpha = 0.99F
        touchAreaPageUp.visibility = INVISIBLE
        touchAreaPageDown.visibility = INVISIBLE
        fabImagebuttonnav.setImageResource(R.drawable.icon_overflow_fab)
        binding.omniboxTouch.setImageResource(R.drawable.ic_touch_disabled)
    }

    private fun hideTouchAreaHint() {
        touchAreaPageUp.setBackgroundColor(Color.TRANSPARENT)
        touchAreaPageDown.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun showTouchAreaHint() {
        touchAreaPageUp.setBackgroundResource(R.drawable.touch_area_border)
        touchAreaPageDown.setBackgroundResource(R.drawable.touch_area_border)
        if (!config.touchAreaHint) {
            Timer("showTouchAreaHint", false)
                    .schedule(object : TimerTask() {
                        override fun run() {
                            hideTouchAreaHint()
                        }
                    }, 500)
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    // Methods
    private fun showFontSizeChangeDialog() {
        val fontArray = resources.getStringArray(R.array.setting_entries_font)
        val valueArray = resources.getStringArray(R.array.setting_values_font)
        val selected = valueArray.indexOf(sp.getString("sp_fontSize", "100")!!)
        AlertDialog.Builder(this, R.style.TouchAreaDialog).apply{
            setTitle("Choose Font Size")
            setSingleChoiceItems(fontArray, selected) { dialog, which ->
                sp.edit().putString("sp_fontSize", valueArray[which]).apply()
                changeFontSize(Integer.parseInt(sp.getString("sp_fontSize", "100") ?: "100"))
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun changeFontSize(size: Int) {
        ninjaWebView.settings.textZoom = size
    }

    private fun increaseFontSize() {
        ninjaWebView.settings.textZoom += 20
    }

    private fun decreaseFontSize() {
        if (ninjaWebView.settings.textZoom <= 20) return
        ninjaWebView.settings.textZoom -= 20
    }

    private fun showEpubFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_EPUB
        intent.putExtra(Intent.EXTRA_TITLE, "einkbro.epub")
        startActivityForResult(intent, WRITE_EPUB_REQUEST_CODE)
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
            val bookName = if (isNewEpubFile) getBookName() else ""
            val chapterName = getChapterName()
            val rawHtml = ninjaWebView.getRawHtml()
            epubManager.saveEpub(
                    isNewEpubFile,
                    fileUri,
                    rawHtml,
                    bookName,
                    chapterName,
                    ninjaWebView.url ?: "") {
                openFile(fileUri, Constants.MIME_TYPE_EPUB)
                isNewEpubFile = false
            }
        }
    }

    private suspend fun getChapterName(): String {
        var chapterName = ninjaWebView.title ?: "no title"
        return TextInputDialog(
                this@BrowserActivity,
                getString(R.string.title),
                getString(R.string.title_in_toc),
                chapterName
        ).show() ?: chapterName
    }

    private suspend fun getBookName(): String {
        return TextInputDialog(
                this@BrowserActivity,
                getString(R.string.book_name),
                getString(R.string.book_name_description),
                "einkbro book"
        ).show() ?: "einkbro book"
    }

    private fun openFile(uri: Uri, mimeType: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(intent, "Open file with"))
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

    private fun dispatchIntent(intent: Intent) {
        val action = intent.action
        when(action) {
            Intent.ACTION_MAIN -> { // initial case
                addAlbum("", config.favoriteUrl, true)
            }
            Intent.ACTION_VIEW -> {
                addAlbum("", intent.data?.toString(), true)
            }
            Intent.ACTION_WEB_SEARCH -> addAlbum("", intent.getStringExtra(SearchManager.QUERY), true)
            "sc_history" -> {
                addAlbum("", config.favoriteUrl, true)
                showOverview()
                ninjaWebView.postDelayed({ openHistoryPage() }, 250)
            }
            "sc_bookmark" -> {
                addAlbum("", config.favoriteUrl, true)
                showOverview()
                ninjaWebView.postDelayed({ openBookmarkPage() }, 250)
            }
            Intent.ACTION_SEND -> {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT)
                addAlbum("", url, true)
            }
            else -> { }
        }
        getIntent().action = ""
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initToolbar() {
        mainToolbar = findViewById(R.id.main_toolbar)
        inputBox = findViewById(R.id.main_omnibox_input)
        omniboxTitle = findViewById(R.id.omnibox_title)
        progressBar = findViewById(R.id.main_progress_bar)
        initFAB()
        binding.omniboxSetting.setOnLongClickListener {
            showFastToggleDialog()
            false
        }
        binding.omniboxSetting.setOnClickListener { showMenuDialog() }
        if (sp.getBoolean("sp_gestures_use", true)) {
            val onTouchListener = object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture("setting_gesture_nav_up")
                override fun onSwipeBottom() = performGesture("setting_gesture_nav_down")
                override fun onSwipeRight() = performGesture("setting_gesture_nav_right")
                override fun onSwipeLeft() = performGesture("setting_gesture_nav_left")
            }
            fabImagebuttonnav.setOnTouchListener(onTouchListener)
            binding.omniboxSetting.setOnTouchListener(onTouchListener)
            inputBox.setOnTouchListener(object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture("setting_gesture_tb_up")
                override fun onSwipeBottom() = performGesture("setting_gesture_tb_down")
                override fun onSwipeRight() = performGesture("setting_gesture_tb_right")
                override fun onSwipeLeft() = performGesture("setting_gesture_tb_left")
            })
        }
        inputBox.setOnEditorActionListener(OnEditorActionListener { _, _, _ ->
            val query = inputBox.text.toString().trim { it <= ' ' }
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnEditorActionListener true
            }
            updateAlbum(query)
            showToolbar()
            false
        })
        inputBox.onFocusChangeListener = OnFocusChangeListener { _, _ ->
            if (inputBox.hasFocus()) {
                ninjaWebView.stopLoading()
                inputBox.setText(ninjaWebView.url)
                Handler().postDelayed({
                    toggleIconsOnOmnibox(true)
                    inputBox.requestFocus()
                    inputBox.setSelection(0, inputBox.text.toString().length)
                    showKeyboard()
                }, 250)
            } else {
                toggleIconsOnOmnibox(false)
                omniboxTitle.text = ninjaWebView.title
                hideKeyboard()
            }
        }
        updateAutoComplete()

        // long click on overview, show bookmark
        binding.omniboxTabcount.setOnLongClickListener {
            openBookmarkPage()
            true
        }

        // scroll to top
        binding.omniboxPageUp.setOnLongClickListener {
            ninjaWebView.jumpToTop()
            true
        }

        // hide bottom bar when refresh button is long pressed.
        binding.omniboxRefresh.setOnLongClickListener {
            hideToolbar()
            true
        }

        binding.omniboxBookmark.setOnClickListener { openBookmarkPage() }
        binding.omniboxBookmark.setOnLongClickListener { saveBookmark(); true }

        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        reorderToolbarIcons()
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when {
            key.equals(ConfigManager.K_TOOLBAR_ICONS) -> { reorderToolbarIcons() }
            key.equals(ConfigManager.K_BOLD_FONT) -> {
                if (config.boldFontStyle) {
                    ninjaWebView.updateCssStyle()
                } else {
                    ninjaWebView.reload()
                }
            }
            key.equals((ConfigManager.K_FONT_STYLE_SERIF)) -> {
                if (config.fontStyleSerif) {
                    ninjaWebView.updateCssStyle()
                } else {
                    ninjaWebView.reload()
                }
            }
        }
    }

    private val toolbarActionViews: List<View> by lazy {
        val childCount = binding.iconBar.childCount
        val children = mutableListOf<View>()
        for (i in 0 until childCount) {
            children.add(binding.iconBar.getChildAt(i))
        }

        children
    }

    private fun reorderToolbarIcons() {
        toolbarActionViews.size

        val iconEnums = config.toolbarIcons
        if (iconEnums.isNotEmpty()) {
            binding.iconBar.removeAllViews()
            iconEnums.forEach { actionEnum ->
                binding.iconBar.addView(toolbarActionViews[actionEnum.ordinal])
            }
            binding.iconBar.requestLayout()
        }
    }

    private fun initFAB() {
        fabImagebuttonnav = findViewById(R.id.fab_imageButtonNav)
        val params = RelativeLayout.LayoutParams(fabImagebuttonnav.layoutParams.width, fabImagebuttonnav.layoutParams.height)
        when (config.fabPosition) {
            FabPosition.Left -> {
                fabImagebuttonnav.layoutParams = params.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }
            FabPosition.Center -> {
                fabImagebuttonnav.layoutParams = params.apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }
        }

        expandViewTouchArea(fabImagebuttonnav, ViewUnit.dpToPixel(this, 20).toInt())
        fabImagebuttonnav.setOnClickListener { showToolbar() }
        fabImagebuttonnav.setOnLongClickListener {
            showFastToggleDialog()
            false
        }
    }

    private fun expandViewTouchArea(view: View, size: Int) {
        val parent = view.parent as View // button: the view you want to enlarge hit area

        parent.post {
            val rect = Rect()
            view.getHitRect(rect)
            rect.top -= size
            rect.left -= size
            rect.bottom += size
            rect.right += size
            parent.touchDelegate = TouchDelegate(rect, view)
        }
    }

    private fun toggleIconsOnOmnibox(shouldHide: Boolean) {
        val visibility = if (shouldHide) GONE else VISIBLE
        binding.iconBar.visibility = visibility
        omniboxTitle.visibility = visibility
    }

    private fun performGesture(gesture: String) {
        val gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"))
        val controller: AlbumController?
        ninjaWebView = currentAlbumController as NinjaWebView
        when (gestureAction) {
            "01" -> {
            }
            "02" -> if (ninjaWebView.canGoForward()) {
                ninjaWebView.goForward()
            } else {
                NinjaToast.show(this, R.string.toast_webview_forward)
            }
            "03" -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                removeAlbum(currentAlbumController!!)
            }
            "04" -> ninjaWebView.jumpToTop()
            "05" -> ninjaWebView.pageDownWithNoAnimation()
            "06" -> {
                controller = nextAlbumController(false)
                showAlbum(controller!!)
            }
            "07" -> {
                controller = nextAlbumController(true)
                showAlbum(controller!!)
            }
            "08" -> showOverview()
            "09" -> addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", Constants.DEFAULT_HOME_URL), true)
            "10" -> removeAlbum(currentAlbumController!!)
            // page up
            "11" -> ninjaWebView.pageUpWithNoAnimation()
            // page down
            "12" -> ninjaWebView.pageDownWithNoAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverview() {
        overviewLayout = findViewById(R.id.layout_overview)
        btnOpenStartPage = findViewById(R.id.open_newTab_2)
        btnOpenBookmark = findViewById(R.id.open_bookmark_2)
        btnOpenHistory = findViewById(R.id.open_history_2)
        btnOpenMenu = findViewById(R.id.open_menu)
        tab_container = findViewById(R.id.tab_container)
        tabScrollview = findViewById(R.id.tab_ScrollView)
        overviewPreview = findViewById(R.id.overview_preview)
        recyclerView = findViewById(R.id.home_list_2)
        open_startPageView = findViewById(R.id.open_newTabView)
        open_bookmarkView = findViewById(R.id.open_bookmarkView)
        open_historyView = findViewById(R.id.open_historyView)

        overviewLayout.setOnTouchListener { _, _ ->
            hideOverview()
            true
        }

        // allow scrolling in listView without closing the bottomSheetDialog
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
        }
        recyclerView.setOnTouchListener { v, event ->
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) { // Disallow NestedScrollView to intercept touch events.
                if (recyclerView.canScrollVertically(-1)) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event)
            true
        }
        btnOpenMenu.setOnClickListener {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = inflate(this, R.layout.dialog_menu_overview, null)
            val bookmark_sort = dialogView.findViewById<LinearLayout>(R.id.bookmark_sort)
            when (overViewTab) {
                getString(R.string.album_title_bookmarks) -> bookmark_sort.visibility = VISIBLE
                getString(R.string.album_title_home) -> bookmark_sort.visibility = VISIBLE
                getString(R.string.album_title_history) -> bookmark_sort.visibility = GONE
            }
            bookmark_sort.setOnClickListener {
                hideBottomSheetDialog()
                bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                val sortView = View.inflate(this, R.layout.dialog_bookmark_sort, null)
                if (overViewTab == getString(R.string.album_title_bookmarks)) {
                    sortView.findViewById<TextView>(R.id.bookmark_sort_tv).text = resources.getString(R.string.dialog_sortIcon)
                } else if (overViewTab == getString(R.string.album_title_home)) {
                    sortView.findViewById<TextView>(R.id.bookmark_sort_tv).text = resources.getString(R.string.dialog_sortDate)
                }
                sortView.findViewById<LinearLayout>(R.id.dialog_sortName).setOnClickListener {
                    if (overViewTab == getString(R.string.album_title_bookmarks)) {
                        sp.edit().putString("sortDBB", "title").apply()
                        initBookmarkList()
                        hideBottomSheetDialog()
                    } else if (overViewTab == getString(R.string.album_title_home)) {
                        sp.edit().putString("sort_startSite", "title").apply()
                        btnOpenStartPage.performClick()
                        hideBottomSheetDialog()
                    }
                }
                sortView.findViewById<LinearLayout>(R.id.dialog_sortIcon).setOnClickListener {
                    if (overViewTab == getString(R.string.album_title_bookmarks)) {
                        sp.edit().putString("sortDBB", "icon").apply()
                        initBookmarkList()
                        hideBottomSheetDialog()
                    } else if (overViewTab == getString(R.string.album_title_home)) {
                        sp.edit().putString("sort_startSite", "ordinal").apply()
                        btnOpenStartPage.performClick()
                        hideBottomSheetDialog()
                    }
                }
                bottomSheetDialog?.setContentView(sortView)
                bottomSheetDialog?.show()
            }
            dialogView.findViewById<LinearLayout>(R.id.tv_delete).setOnClickListener {
                hideBottomSheetDialog()
                bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                val dialogView3 = View.inflate(this, R.layout.dialog_action, null)
                dialogView3.findViewById<TextView>(R.id.dialog_text).setText(R.string.hint_database)
                dialogView3.findViewById<Button>(R.id.action_ok).setOnClickListener {
                    when (overViewTab) {
                        getString(R.string.album_title_home) -> {
                            BrowserUnit.clearHome(this)
                            btnOpenStartPage.performClick()
                        }
                        getString(R.string.album_title_bookmarks) -> {
                            val data = Environment.getDataDirectory()
                            val bookmarksPath_app = "//data//$packageName//databases//pass_DB_v01.db"
                            val bookmarkFile_app = File(data, bookmarksPath_app)
                            BrowserUnit.deleteDir(bookmarkFile_app)
                            btnOpenBookmark.performClick()
                        }
                        getString(R.string.album_title_history) -> {
                            BrowserUnit.clearHistory(this)
                            openHistoryPage()
                        }
                    }
                    hideBottomSheetDialog()
                }
                dialogView3.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
                bottomSheetDialog?.setContentView(dialogView3)
                bottomSheetDialog?.show()
            }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
        }
        btnOpenStartPage.setOnClickListener {
            overviewPreview.visibility = VISIBLE
            recyclerView.visibility = GONE
            toggleOverviewFocus(open_startPageView)
            overViewTab = getString(R.string.album_title_home)
        }
        btnOpenBookmark.setOnClickListener { openBookmarkPage() }
        btnOpenHistory.setOnClickListener { openHistoryPage() }

        findViewById<View>(R.id.button_close_overview).setOnClickListener { hideOverview() }
        showCurrentTabInOverview()
    }

    private fun openHistoryPage() {
        overviewLayout.visibility = VISIBLE

        overviewPreview.visibility = INVISIBLE
        recyclerView.visibility = VISIBLE
        toggleOverviewFocus(open_historyView)

        overViewTab = getString(R.string.album_title_history)

        val action = RecordAction(this)
        action.open(false)
        val list: MutableList<Record> = action.listEntries(this, false)
        action.close()
        adapter = RecordAdapter(
                list,
                { position ->
                    updateAlbum(list[position].url)
                    hideOverview()
                },
                { position ->
                    showHistoryContextMenu(list[position].title, list[position].url, adapter, list, position)
                }
        )
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun openBookmarkPage() {
        overviewLayout.visibility = VISIBLE

        overviewPreview.visibility = INVISIBLE
        recyclerView.visibility = VISIBLE
        toggleOverviewFocus(open_bookmarkView)
        overViewTab = getString(R.string.album_title_bookmarks)
        initBookmarkList()
    }

    private fun toggleOverviewFocus(view: View) {
        open_startPageView.visibility = if (open_startPageView == view) VISIBLE else INVISIBLE
        open_bookmarkView.visibility = if (open_bookmarkView== view) VISIBLE else INVISIBLE
        open_historyView.visibility = if (open_historyView== view) VISIBLE else INVISIBLE
    }

    private fun showCurrentTabInOverview() {
        when (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            "3" -> openBookmarkPage()
            "4" -> openHistoryPage()
            else -> btnOpenStartPage.performClick()
        }
    }

    private fun initSearchPanel() {
        searchPanel = findViewById(R.id.main_search_panel)
        searchBox = findViewById(R.id.main_search_box)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                (currentAlbumController as NinjaWebView?)?.findAllAsync(s.toString())
            }
        })
        searchBox.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@OnEditorActionListener false
            }
            if (searchBox.text.toString().isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnEditorActionListener true
            }
            false
        })
        findViewById<ImageButton?>(R.id.main_search_up).setOnClickListener {
            val query = searchBox.text.toString()
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@setOnClickListener
            }
            hideKeyboard()
            (currentAlbumController as NinjaWebView).findNext(false)
        }
        findViewById<ImageButton?>(R.id.main_search_down).setOnClickListener {
            val query = searchBox.text.toString()
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@setOnClickListener
            }
            hideKeyboard()
            (currentAlbumController as NinjaWebView).findNext(true)
        }
        findViewById<ImageButton?>(R.id.main_search_cancel).setOnClickListener { hideSearchPanel() }
    }

    private fun initBookmarkList() {
        val adapter = object: SimpleCursorRecyclerAdapter(
                R.layout.list_item_bookmark,
                bookmarkDB.fetchAllData(this),
                arrayOf("pass_title"),
                intArrayOf(R.id.record_item_title)
        ) {
            override fun onBindViewHolder(holder: SimpleViewHolder, cursor: Cursor) {
                super.onBindViewHolder(holder, cursor)
                holder.itemView.setOnClickListener {
                    val position = holder.adapterPosition
                    cursor.moveToPosition(position)
                    val passContent = cursor.getString(cursor.getColumnIndexOrThrow("pass_content"))
                    updateAlbum(passContent)
                    hideOverview()
                }
                holder.itemView.setOnLongClickListener {
                    val position = holder.adapterPosition
                    cursor.moveToPosition(position)
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                    val passTitle = cursor.getString(cursor.getColumnIndexOrThrow("pass_title"))
                    val passContent = cursor.getString(cursor.getColumnIndexOrThrow("pass_content"))
                    val passIcon = cursor.getString(cursor.getColumnIndexOrThrow("pass_icon"))
                    val passAttachment = cursor.getString(cursor.getColumnIndexOrThrow("pass_attachment"))
                    val passCreation = cursor.getString(cursor.getColumnIndexOrThrow("pass_creation"))
                    showBookmarkContextMenu(passTitle, passContent, passIcon, passAttachment, id, passCreation)
                    true
                }
            }
        }
        recyclerView.adapter = adapter
    }

    private fun showFastToggleDialog() {
        FastToggleDialog(this, ninjaWebView.title ?: "", ninjaWebView.url ?: "") {
            if (ninjaWebView != null) {
                ninjaWebView.initPreferences()
                ninjaWebView.reload()
            }
        }.apply { show() }
    }

    override fun addNewTab(url: String?) = addAlbum("", url, true)

    @Synchronized
    private fun addAlbum(title: String, url: String?, foreground: Boolean) {
        if (url == null) return

        ninjaWebView = NinjaWebView(this, this)
        ninjaWebView.albumTitle = title
        ViewUnit.bound(this, ninjaWebView)
        val albumView = ninjaWebView.albumView
        if (currentAlbumController != null) {
            val index = BrowserContainer.indexOf(currentAlbumController) + 1
            BrowserContainer.add(ninjaWebView, index)
            updateWebViewCount()
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        } else {
            BrowserContainer.add(ninjaWebView)
            updateWebViewCount()
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        if (!foreground) {
            ViewUnit.bound(this, ninjaWebView)
            ninjaWebView.loadUrl(url)
            ninjaWebView.deactivate()
            return
        } else {
            showToolbar()
            showAlbum(ninjaWebView)
        }
        if (url.isNotEmpty()) {
            ninjaWebView.loadUrl(url)
        }
    }

    private fun updateWebViewCount() {
        binding.omniboxTabcount.text = BrowserContainer.size().toString()
    }

    @Synchronized
    private fun updateAlbum(url: String?) {
        if (url == null) return
        (currentAlbumController as NinjaWebView).loadUrl(url)
        updateOmnibox()
    }

    private fun closeTabConfirmation(okAction: () -> Unit) {
        if (!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction()
        } else {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.toast_close_tab)
            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                okAction()
                hideBottomSheetDialog()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
        }
    }

    @Synchronized
    override fun removeAlbum(controller: AlbumController) {
        if (BrowserContainer.size() <= 1) {
            if (!sp.getBoolean("sp_reopenLastTab", false)) {
                finish()
            } else {
                updateAlbum(sp.getString("favoriteURL", "https://github.com/plateaukao/browser"))
                hideOverview()
            }
        } else {
            closeTabConfirmation {
                tab_container.removeView(controller.albumView)
                var index = BrowserContainer.indexOf(controller)
                BrowserContainer.remove(controller)
                updateWebViewCount()
                if (index >= BrowserContainer.size()) {
                    index = BrowserContainer.size() - 1
                }
                showAlbum(BrowserContainer.get(index))
            }
        }
    }

    private fun updateOmnibox() {
        if(!this::ninjaWebView.isInitialized) return

        if (this::ninjaWebView.isInitialized && ninjaWebView === currentAlbumController) {
            omniboxTitle.text = ninjaWebView.title
        } else {
            ninjaWebView = currentAlbumController as NinjaWebView
            updateProgress(ninjaWebView.progress)
        }
    }

    var keepToolbar = false
    private fun scrollChange() {
        ninjaWebView.setOnScrollChangeListener(object : NinjaWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                if (!sp.getBoolean("hideToolbar", true)) return

                val height = floor(x = ninjaWebView.contentHeight * ninjaWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ninjaWebView.height
                val cutoff = height - webViewHeight - 112 * resources.displayMetrics.density.roundToInt()
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    if (!keepToolbar) {
                        // Daniel
                        hideToolbar();
                    } else {
                        keepToolbar = false
                    }
                } else if (scrollY < oldScrollY) {
                    //showOmnibox()
                }
            }
        })
    }

    @Synchronized
    override fun updateProgress(progress: Int) {
        progressBar.progress = progress
        updateOmnibox()
        updateAutoComplete()
        scrollChange()
        HelperUnit.initRendering(mainContentLayout)
        ninjaWebView.requestFocus()
        if (progress < BrowserUnit.PROGRESS_MAX) {
            updateRefresh(true)
            progressBar.visibility = View.VISIBLE
        } else {
            updateRefresh(false)
            progressBar.visibility = View.GONE
        }
    }

    private fun updateRefresh(running: Boolean) {
        if (running) {
            binding.omniboxRefresh.setImageResource(R.drawable.icon_close)
        } else {
            try {
                if (ninjaWebView.url?.contains("https://") == true) {
                    binding.omniboxRefresh.setImageResource(R.drawable.icon_refresh)
                } else {
                    binding.omniboxRefresh.setImageResource(R.drawable.icon_alert)
                }
            } catch (e: Exception) {
                binding.omniboxRefresh.setImageResource(R.drawable.icon_refresh)
            }
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

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (view == null) {
            return
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        originalOrientation = requestedOrientation
        fullscreenHolder = FrameLayout(this).apply{
            addView(
                    customView,
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    ))

        }
        val decorView = window.decorView as FrameLayout
        decorView.addView(
                fullscreenHolder,
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ))
        customView?.keepScreenOn = true
        (currentAlbumController as View?)?.visibility = View.GONE
        setCustomFullscreen(true)
        if (view is FrameLayout) {
            if (view.focusedChild is VideoView) {
                videoView = view.focusedChild as VideoView
                videoView?.setOnErrorListener(VideoCompletionListener())
                videoView?.setOnCompletionListener(VideoCompletionListener())
            }
        }
        customViewCallback = callback
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onHideCustomView(): Boolean {
        if (customView == null || customViewCallback == null || currentAlbumController == null) {
            return false
        }
        val decorView = window.decorView as FrameLayout
        decorView.removeView(fullscreenHolder)
        customView?.keepScreenOn = false
        (currentAlbumController as View).visibility = View.VISIBLE
        setCustomFullscreen(false)
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
        if (event?.action != KeyEvent.ACTION_DOWN) return false
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
                        inputBox.requestFocus()
                    }
                }
                KeyEvent.KEYCODE_J -> ninjaWebView.pageDownWithNoAnimation()
                KeyEvent.KEYCODE_K -> ninjaWebView.pageUpWithNoAnimation()
                KeyEvent.KEYCODE_H -> ninjaWebView.goBack()
                KeyEvent.KEYCODE_L -> ninjaWebView.goForward()
                KeyEvent.KEYCODE_D -> removeAlbum(currentAlbumController!!)
                KeyEvent.KEYCODE_T -> {
                    addAlbum(getString(R.string.app_name), "", true)
                    inputBox.requestFocus()
                }
                KeyEvent.KEYCODE_SLASH -> showSearchPanel()
                KeyEvent.KEYCODE_G -> {
                    when {
                        previousKeyEvent == null -> {
                            previousKeyEvent = event
                        }
                        previousKeyEvent?.keyCode == KeyEvent.KEYCODE_G -> {
                            // gg
                            ninjaWebView.jumpToTop()
                            previousKeyEvent = null
                        }
                        else -> {
                            previousKeyEvent = null
                        }
                    }
                }
                KeyEvent.KEYCODE_V -> {
                    if (previousKeyEvent == null) {
                        previousKeyEvent = event
                    } else {
                        previousKeyEvent = null
                    }
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

    private fun show_contextMenu_link(url: String?) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_link, null)
        dialogView.findViewById<LinearLayout>(R.id.contextLink_newTab).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.contextLink__shareLink).setOnClickListener {
            if (prepareRecord()) {
                NinjaToast.show(this, getString(R.string.toast_share_failed))
            } else {
                IntentUnit.share(this, "", url)
            }
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.contextLink_openWith).setOnClickListener {
            url?.let { showBrowserChooser(it, getString(R.string.menu_open_with)) }
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.contextLink_newTabOpen).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_save_pdf).setOnClickListener {
            try {
                hideBottomSheetDialog()
                val builder = AlertDialog.Builder(this@BrowserActivity)
                val menuView = inflate(this, R.layout.dialog_edit_extension, null)
                val editTitle = menuView.findViewById<EditText>(R.id.dialog_edit)
                val editExtension = menuView.findViewById<EditText>(R.id.dialog_edit_extension)
                val filename = URLUtil.guessFileName(url, null, null)
                editTitle.setHint(R.string.dialog_title_hint)
                editTitle.setText(HelperUnit.fileName(ninjaWebView.url))
                val extension = filename.substring(filename.lastIndexOf("."))
                if (extension.length <= 8) {
                    editExtension.setText(extension)
                }
                builder.setView(menuView)
                builder.setTitle(R.string.menu_edit)
                builder.setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    val title = editTitle.text.toString().trim { it <= ' ' }
                    val extension = editExtension.text.toString().trim { it <= ' ' }
                    val filename = title + extension
                    if (title.isEmpty() || extension.isEmpty() || !extension.startsWith(".")) {
                        NinjaToast.show(this, getString(R.string.toast_input_empty))
                    } else {
                        if (Build.VERSION.SDK_INT in 23..28) {
                            val hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                HelperUnit.grantPermissionsStorage(this)
                            } else {
                                val source = Uri.parse(url)
                                val request = DownloadManager.Request(source)
                                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                val dm = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
                                dm.enqueue(request)
                                hideKeyboard()
                            }
                        } else {
                            val source = Uri.parse(url)
                            val request = DownloadManager.Request(source)
                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            val dm = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
                            dm.enqueue(request)
                            hideKeyboard()
                        }
                    }
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, whichButton ->
                    dialog.cancel()
                    hideKeyboard()
                }
                val dialog = builder.create()
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
    }

    override fun onLongPress(url: String?) {
        val result = ninjaWebView.hitTestResult
        if (url != null) {
            show_contextMenu_link(url)
        } else if (result.type == HitTestResult.IMAGE_TYPE || result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.type == HitTestResult.SRC_ANCHOR_TYPE) {
            show_contextMenu_link(result.extra)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showToolbar() {
        if (!searchOnSite) {
            fabImagebuttonnav.visibility = INVISIBLE
            searchPanel.visibility = GONE
            mainToolbar.visibility = VISIBLE
            omniboxTitle.visibility = VISIBLE
            binding.appBar.visibility = VISIBLE
            hideKeyboard()
            showStatusBar()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun hideToolbar() {
        if (!searchOnSite) {
            fabImagebuttonnav.visibility = VISIBLE
            searchPanel.visibility = GONE
            mainToolbar.visibility = GONE
            omniboxTitle.visibility = GONE
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
        fabImagebuttonnav.visibility = INVISIBLE
        mainToolbar.visibility = GONE
        searchPanel.visibility = VISIBLE
        omniboxTitle.visibility = GONE
        binding.appBar.visibility = VISIBLE
        searchBox.requestFocus()
        showKeyboard()
    }

    private var isNewEpubFile = false
    private fun showSaveEpubDialog() {
        val colors = arrayOf(getString(R.string.existing_epub), getString(R.string.a_new_epub))

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.save_to))
        builder.setItems(colors) { _, index ->
            when(index) {
                0 -> isNewEpubFile = false
                1 -> isNewEpubFile = true
            }
            showEpubFilePicker()
        }
        builder.show()
    }

    private fun showMenuDialog(): Boolean {
        MenuDialog(
                this,
                ninjaWebView,
                { updateAlbum(sp.getString("favoriteURL", "https://github.com/plateaukao/browser")) },
                { removeAlbum(currentAlbumController!!) },
                { saveBookmark() },
                { showSearchPanel() },
                { showSaveEpubDialog() },
                { printPDF() },
        ).show()
        return true
    }

    private fun showBookmarkContextMenu(title: String, url: String,
                                        userName: String, userPW: String, _id: String, pass_creation: String?
    ) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        if (overViewTab == getString(R.string.album_title_history)) {
            dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit).visibility = View.GONE
        } else {
            dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit).visibility = View.VISIBLE
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url, null)
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete).setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val menuView = View.inflate(this, R.layout.dialog_action, null)
            val textView = menuView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_titleConfirm_delete)

            menuView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                bookmarkDB.delete(_id.toInt())
                initBookmarkList()
                hideBottomSheetDialog()
            }
            menuView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(menuView)
            bottomSheetDialog?.show()
        }

        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit).setOnClickListener {
            hideBottomSheetDialog()
                try {
                    bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                    val menuView = View.inflate(this, R.layout.dialog_edit_bookmark, null)
                    val pass_titleET = menuView.findViewById<EditText>(R.id.pass_title)
                    val pass_URLET = menuView.findViewById<EditText>(R.id.pass_url)
                    pass_titleET.setText(title)
                    pass_URLET.setText(url)
                    menuView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                        try {
                            val input_pass_title = pass_titleET.text.toString().trim { it <= ' ' }
                            val input_pass_url = pass_URLET.text.toString().trim { it <= ' ' }
                            bookmarkDB.update(_id.toInt(), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url), "", "", pass_creation)
                            initBookmarkList()
                            hideKeyboard()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            NinjaToast.show(this, R.string.toast_error)
                        }
                        hideBottomSheetDialog()
                    }
                    menuView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                        hideKeyboard()
                        hideBottomSheetDialog()
                    }
                    bottomSheetDialog?.setContentView(menuView)
                    bottomSheetDialog?.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(this, R.string.toast_error)
                }
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
    }

    private fun showHistoryContextMenu(title: String, url: String, recordAdapter: RecordAdapter,
                                       recordList: MutableList<Record>, location: Int
    ) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        if (overViewTab == getString(R.string.album_title_history)) {
            dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit).visibility = View.GONE
        } else {
            dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit).visibility = View.VISIBLE
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url, null)
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete).setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_titleConfirm_delete)

            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                val record = recordList[location]
                val action = RecordAction(this@BrowserActivity)
                action.open(true)
                action.deleteHistoryItem(record)
                action.close()
                recordList.removeAt(location)
                recordAdapter.notifyDataSetChanged()
                updateAutoComplete()
                hideBottomSheetDialog()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
    }

    private fun setCustomFullscreen(fullscreen: Boolean) {
        val decorView = window.decorView
        if (fullscreen) {
            decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun nextAlbumController(next: Boolean): AlbumController? {
        if (BrowserContainer.size() <= 1) {
            return currentAlbumController
        }
        val list = BrowserContainer.list()
        var index = list.indexOf(currentAlbumController)
        if (next) {
            index++
            if (index >= list.size) {
                index = 0
            }
        } else {
            index--
            if (index < 0) {
                index = list.size - 1
            }
        }
        return list[index]
    }

    private var mActionMode: ActionMode? = null
    override fun onActionModeStarted(mode: ActionMode) {
        if (mActionMode == null) {
            var isNaverDictExist = false
            mActionMode = mode
            val menu = mode.menu
            val toBeRemovedList: MutableList<MenuItem> = mutableListOf()
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                if (item.intent?.component?.packageName == "info.plateaukao.naverdict") {
                    isNaverDictExist = true
                    break
                }
                toBeRemovedList.add(item)
            }
            // only works when naver dict app is installed.
            if (isNaverDictExist) {
                for (item in toBeRemovedList) {
                    menu.removeItem(item.itemId)
                }
                for (item in toBeRemovedList) {
                    if (item.title == "Copy") {
                        menu.add(0, item.itemId, Menu.NONE, item.title)
                    }
                }
            }
        }
        super.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        mActionMode = null
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        var view = this.currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val WRITE_EPUB_REQUEST_CODE = 2
        private const val WRITE_PDF_REQUEST_CODE = 3
    }
}