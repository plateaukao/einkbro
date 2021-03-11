package de.baumann.browser.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.SearchManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
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
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView.HitTestResult
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.ActivityMainBinding
import de.baumann.browser.Ninja.databinding.DialogMenuBinding
import de.baumann.browser.browser.*
import de.baumann.browser.database.BookmarkList
import de.baumann.browser.database.Record
import de.baumann.browser.database.RecordAction
import de.baumann.browser.service.ClearService
import de.baumann.browser.task.ScreenshotTask
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.*
import java.io.File
import java.util.*

class BrowserActivity : AppCompatActivity(), BrowserController, View.OnClickListener {
    private lateinit var adapter: Adapter_Record

    private lateinit var open_startPage: ImageButton
    private lateinit var open_bookmark: ImageButton
    private lateinit var open_history: ImageButton
    private lateinit var open_menu: ImageButton
    private lateinit var fab_imageButtonNav: ImageButton
    private lateinit var inputBox: AutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchBox: EditText
    private lateinit var overviewView: ViewGroup
    private lateinit var ninjaWebView: NinjaWebView
    private lateinit var listView: ListView
    private lateinit var omniboxTitle: TextView
    private lateinit var tab_ScrollView: HorizontalScrollView
    private lateinit var overview_top: LinearLayout

    private var bottomSheetDialog: BottomSheetDialog? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Layouts
    private lateinit var omnibox: RelativeLayout
    private lateinit var searchPanel: ViewGroup
    private lateinit var contentFrame: FrameLayout
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
    private lateinit var sp: SharedPreferences
    private var javaHosts: Javascript? = null
    private var cookieHosts: Cookie? = null
    private var adBlock: AdBlock? = null
    private lateinit var gridAdapter: GridAdapter
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
    private var onPause = false
    private var customViewCallback: CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var currentAlbumController: AlbumController? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var binding: ActivityMainBinding

    private lateinit var bookmarkDB: BookmarkList
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
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp.edit().putInt("restart_changed", 0).apply()
        sp.edit().putBoolean("pdf_create", false).apply()
        HelperUnit.applyTheme(this)
        setContentView(binding.root)
        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")) == "no") {
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
        contentFrame = findViewById(R.id.main_content)
        initOmnibox()
        initSearchPanel()
        initOverview()
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
        if (sp.getBoolean("start_tabStart", false)) {
            showOverview()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
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
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onPause() {
        onPause = true
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply()
            val dialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            dialogView.findViewById<TextView>(R.id.dialog_text).setText(R.string.toast_restart)
            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                dialog.dismiss()
                recreate()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { dialog.cancel() }
            dialog.setContentView(dialogView)
            dialog.show()
        }
        dispatchIntent(intent)
        updateOmnibox()
        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).apply()
            if (sp.getBoolean("pdf_share", false)) {
                sp.edit().putBoolean("pdf_share", false).apply()
                startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            } else {
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
        }
    }

    public override fun onDestroy() {
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            val toClearService = Intent(this, ClearService::class.java)
            startService(toClearService)
        }
        BrowserContainer.clear()
        IntentUnit.setContext(null)
        unregisterReceiver(downloadReceiver)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                ninjaWebView.pageDownWithNoAnimation()
                hideOmnibox()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                ninjaWebView.pageUpWithNoAnimation()
                hideOmnibox()
                return true
            }
            KeyEvent.KEYCODE_MENU -> return showOverflow()
            KeyEvent.KEYCODE_BACK -> {
                hideKeyboard()
                if (overviewView.visibility == VISIBLE) {
                    hideOverview()
                    return true
                }
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    return onHideCustomView()
                } else if (omnibox.visibility == GONE && sp.getBoolean("sp_toolbarShow", true)) {
                    showOmnibox()
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
            contentFrame.removeAllViews()
            contentFrame.addView(av)
        } else {
            contentFrame.removeAllViews()
            contentFrame.addView(controller as View)
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
        currentAlbumController?.deactivate()
        currentAlbumController?.activate()
        overviewView.visibility = VISIBLE

        ninjaWebView.postDelayed({
            tab_ScrollView.scrollTo(currentAlbumController?.albumView?.left ?: 0, 0)
        }, 250)
    }

    override fun hideOverview() {
        overviewView.visibility = INVISIBLE
    }

    private fun hideBottomSheetDialog() {
        bottomSheetDialog?.cancel()
    }

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
            R.id.button_closeTab -> removeAlbum(currentAlbumController!!)
            R.id.button_quit -> finish()
            R.id.menu_shareScreenshot -> if (Build.VERSION.SDK_INT in 23..28) {
                val hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    HelperUnit.grantPermissionsStorage(this)
                } else {
                    hideBottomSheetDialog()
                    sp.edit().putInt("screenshot", 1).apply()
                    ScreenshotTask(this, ninjaWebView).execute()
                }
            } else {
                hideBottomSheetDialog()
                sp.edit().putInt("screenshot", 1).apply()
                ScreenshotTask(this, ninjaWebView).execute()
            }
            R.id.menu_shareLink -> {
                if (prepareRecord()) {
                    NinjaToast.show(this, getString(R.string.toast_share_failed))
                } else {
                    IntentUnit.share(this, title, url)
                }
            }
            R.id.menu_sharePDF -> printPDF(true)
            R.id.menu_openWith -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                val chooser = Intent.createChooser(intent, getString(R.string.menu_open_with))
                startActivity(chooser)
            }
            R.id.menu_saveScreenshot -> if (Build.VERSION.SDK_INT in 23..28) {
                val hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    HelperUnit.grantPermissionsStorage(this)
                } else {
                    hideBottomSheetDialog()
                    sp.edit().putInt("screenshot", 0).apply()
                    ScreenshotTask(this, ninjaWebView).execute()
                }
            } else {
                hideBottomSheetDialog()
                sp.edit().putInt("screenshot", 0).apply()
                ScreenshotTask(this, ninjaWebView).execute()
            }
            R.id.menu_saveBookmark -> {
                try {
                    if (bookmarkDB.isExist(url)) {
                        NinjaToast.show(this, R.string.toast_newTitle)
                    } else {
                        bookmarkDB.insert(HelperUnit.secString(ninjaWebView.title), url, "", "", "01")
                        NinjaToast.show(this, R.string.toast_edit_successful)
                        initBookmarkList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(this, R.string.toast_error)
                }
            }
            R.id.menu_searchSite -> {
                hideKeyboard()
                showSearchPanel()
            }
            R.id.contextLink_saveAs -> printPDF(false)
            R.id.menu_settings -> {
                val settings = Intent(this@BrowserActivity, Settings_Activity::class.java)
                startActivity(settings)
            }
            R.id.menu_fileManager -> {
                val intent2 = Intent(Intent.ACTION_VIEW)
                intent2.type = "*/*"
                intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent2, null)
            }
            R.id.menu_download -> {
                hideBottomSheetDialog()
                startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            }
            R.id.omnibox_overview -> showOverview()
            R.id.omnibox_touch -> toggleTouchFeature()
            R.id.touch_left -> ninjaWebView.pageUpWithNoAnimation()
            R.id.touch_right -> ninjaWebView.pageDownWithNoAnimation()
            /* Daniel
            R.id.omnibox_page_back -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                removeAlbum(currentAlbumController!!)
            }
            */
            R.id.omnibox_page_up -> ninjaWebView.pageUpWithNoAnimation()
            R.id.omnibox_page_down -> ninjaWebView.pageDownWithNoAnimation()
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
            else -> {
            }
        }
    }

    private fun  toggleTouchFeature() {
        if (binding.omniboxTouch.alpha != 1.0F) {
            binding.omniboxTouch.alpha = 1.0F
            findViewById<View>(R.id.touch_left).visibility = VISIBLE
            findViewById<View>(R.id.touch_right).visibility = VISIBLE
        } else {
            binding.omniboxTouch.alpha = 0.3F
            findViewById<View>(R.id.touch_left).visibility = INVISIBLE
            findViewById<View>(R.id.touch_right).visibility = INVISIBLE
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
        AlertDialog.Builder(this).apply{
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

    private fun printPDF(share: Boolean) {
        try {
            sp.edit().putBoolean("pdf_share", share).apply()
            val title = HelperUnit.fileName(ninjaWebView.url)
            val printManager = getSystemService(PRINT_SERVICE) as PrintManager
            val printAdapter = ninjaWebView.createPrintDocumentAdapter(title)
            Objects.requireNonNull(printManager).print(title, printAdapter, PrintAttributes.Builder().build())
            sp.edit().putBoolean("pdf_create", true).apply()
        } catch (e: Exception) {
            NinjaToast.show(this, R.string.toast_error)
            sp.edit().putBoolean("pdf_create", false).apply()
            e.printStackTrace()
        }
    }

    private fun dispatchIntent(intent: Intent) {
        val action = intent.action
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        if ("" == action) {
            Log.i(ContentValues.TAG, "resumed FOSS browser")
        } else if (intent.action != null && intent.action == Intent.ACTION_WEB_SEARCH) {
            addAlbum(null, intent.getStringExtra(SearchManager.QUERY), true)
        } else if (filePathCallback != null) {
            filePathCallback = null
        } else if ("sc_history" == action) {
            addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            showOverview()
            ninjaWebView.postDelayed({ open_history.performClick() }, 250)
        } else if ("sc_bookmark" == action) {
            addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            showOverview()
            ninjaWebView.postDelayed({ open_bookmark.performClick() }, 250)
        } else if ("sc_startPage" == action) {
            addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            showOverview()
            ninjaWebView.postDelayed({ open_startPage.performClick() }, 250)
        } else if (Intent.ACTION_SEND == action) {
            addAlbum(null, url, true)
        } else {
            if (!onPause) {
                addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            }
        }
        getIntent().action = ""
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOmnibox() {
        omnibox = findViewById(R.id.main_omnibox)
        inputBox = findViewById(R.id.main_omnibox_input)
        omniboxTitle = findViewById(R.id.omnibox_title)
        progressBar = findViewById(R.id.main_progress_bar)
        val nav_position = Objects.requireNonNull(sp.getString("nav_position", "0"))
        fab_imageButtonNav = when (nav_position) {
            "1" -> findViewById(R.id.fab_imageButtonNav_left)
            "2" -> findViewById(R.id.fab_imageButtonNav_center)
            else -> findViewById(R.id.fab_imageButtonNav_right)
        }
        expandViewTouchArea(fab_imageButtonNav, ViewUnit.dpToPixel(this, 20).toInt())
        fab_imageButtonNav.setOnClickListener { showOmnibox() }
        fab_imageButtonNav.setOnLongClickListener {
            show_dialogFastToggle()
            false
        }
        binding.omniboxOverflow.setOnLongClickListener {
            show_dialogFastToggle()
            false
        }
        binding.omniboxOverflow.setOnClickListener { showOverflow() }
        if (sp.getBoolean("sp_gestures_use", true)) {
            val onTouchListener = object : SwipeTouchListener(this) {
                override fun onSwipeTop() = performGesture("setting_gesture_nav_up")
                override fun onSwipeBottom() = performGesture("setting_gesture_nav_down")
                override fun onSwipeRight() = performGesture("setting_gesture_nav_right")
                override fun onSwipeLeft() = performGesture("setting_gesture_nav_left")
            }
            fab_imageButtonNav.setOnTouchListener(onTouchListener)
            binding.omniboxOverflow.setOnTouchListener(onTouchListener)
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
            showOmnibox()
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
        binding.omniboxOverview.setOnLongClickListener {
            showOverview()
            open_bookmark.performClick()
            true
        }

        // scroll to top
        binding.omniboxPageUp.setOnLongClickListener {
            ninjaWebView.jumpToTop()
            true
        }

        // hide bottom bar when refresh button is long pressed.
        binding.omniboxRefresh.setOnLongClickListener {
            hideOmnibox()
            true
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
            "09" -> addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://www.google.com"), true)
            "10" -> removeAlbum(currentAlbumController!!)
            // page up
            "11" -> ninjaWebView.pageUpWithNoAnimation()
            // page down
            "12" -> ninjaWebView.pageDownWithNoAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverview() {
        overviewView = findViewById(R.id.layout_overview)
        open_startPage = findViewById(R.id.open_newTab_2)
        open_bookmark = findViewById(R.id.open_bookmark_2)
        open_history = findViewById(R.id.open_history_2)
        open_menu = findViewById(R.id.open_menu)
        tab_container = findViewById(R.id.tab_container)
        tab_ScrollView = findViewById(R.id.tab_ScrollView)
        overview_top = findViewById(R.id.overview_top)
        listView = findViewById(R.id.home_list_2)
        open_startPageView = findViewById(R.id.open_newTabView)
        open_bookmarkView = findViewById(R.id.open_bookmarkView)
        open_historyView = findViewById(R.id.open_historyView)

        overviewView.setOnTouchListener { _, _ ->
            hideOverview()
            true
        }

        // allow scrolling in listView without closing the bottomSheetDialog
        listView.setOnTouchListener { v, event ->
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) { // Disallow NestedScrollView to intercept touch events.
                if (listView.canScrollVertically(-1)) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event)
            true
        }
        open_menu.setOnClickListener {
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
                        open_startPage.performClick()
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
                        open_startPage.performClick()
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
                            open_startPage.performClick()
                        }
                        getString(R.string.album_title_bookmarks) -> {
                            val data = Environment.getDataDirectory()
                            val bookmarksPath_app = "//data//$packageName//databases//pass_DB_v01.db"
                            val bookmarkFile_app = File(data, bookmarksPath_app)
                            BrowserUnit.deleteDir(bookmarkFile_app)
                            open_bookmark.performClick()
                        }
                        getString(R.string.album_title_history) -> {
                            BrowserUnit.clearHistory(this)
                            open_history.performClick()
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
        open_startPage.setOnClickListener {
            overview_top.visibility = VISIBLE
            listView.visibility = GONE
            toggleOverviewFocus(open_startPageView)
            overViewTab = getString(R.string.album_title_home)
        }
        open_bookmark.setOnClickListener {
            overview_top.visibility = INVISIBLE
            listView.visibility = VISIBLE
            toggleOverviewFocus(open_bookmarkView)
            overViewTab = getString(R.string.album_title_bookmarks)
            initBookmarkList()
        }
        open_history.setOnClickListener {
            overview_top.visibility = INVISIBLE
            listView.visibility = VISIBLE
            toggleOverviewFocus(open_historyView)
            overViewTab = getString(R.string.album_title_history)
            val action = RecordAction(this)
            action.open(false)
            val list: List<Record>
            list = action.listEntries(this, false)
            action.close()
            adapter = Adapter_Record(this, list)
            listView.adapter = adapter
            adapter.notifyDataSetChanged()
            listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
                updateAlbum(list[position].url)
                hideOverview()
            }
            listView.onItemLongClickListener = OnItemLongClickListener { _, _, position, _ ->
                showHistoryContextMenu(list[position].title, list[position].url, adapter, list, position)
                true
            }
        }
        showCurrentTabInOverview()
    }

    private fun toggleOverviewFocus(view: View) {
        open_startPageView.visibility = if (open_startPageView == view) VISIBLE else INVISIBLE
        open_bookmarkView.visibility = if (open_bookmarkView== view) VISIBLE else INVISIBLE
        open_historyView.visibility = if (open_historyView== view) VISIBLE else INVISIBLE
    }

    private fun showCurrentTabInOverview() {
        when (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            "3" -> open_bookmark.performClick()
            "4" -> open_history.performClick()
            else -> open_startPage.performClick()
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
        searchBox.setOnEditorActionListener(OnEditorActionListener { _, actionId, _->
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
        val row: Cursor
        val layoutStyle = R.layout.list_item_bookmark
        val xml_id = intArrayOf(R.id.record_item_title)
        val column = arrayOf("pass_title")
        val search = sp.getString("filter_bookmarks", "00")
        row = if (Objects.requireNonNull(search) == "00") {
            bookmarkDB.fetchAllData(this)
        } else {
            bookmarkDB.fetchDataByFilter(search, "pass_creation")
        }
        val adapter: SimpleCursorAdapter = object : SimpleCursorAdapter(this, layoutStyle, row, column, xml_id, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val row = listView.getItemAtPosition(position) as Cursor
                val bookmarks_icon = row.getString(row.getColumnIndexOrThrow("pass_creation"))
                val v = super.getView(position, convertView, parent)
                val iv_icon = v.findViewById<ImageView>(R.id.ib_icon)
                HelperUnit.switchIcon(this@BrowserActivity, bookmarks_icon, "pass_creation", iv_icon)
                return v
            }
        }

        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { adapterView, view, position, id ->
            val pass_content = row.getString(row.getColumnIndexOrThrow("pass_content"))
            val pass_icon = row.getString(row.getColumnIndexOrThrow("pass_icon"))
            val pass_attachment = row.getString(row.getColumnIndexOrThrow("pass_attachment"))
            updateAlbum(pass_content)
            hideOverview()
        }
        listView.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val row = listView.getItemAtPosition(position) as Cursor
            val _id = row.getString(row.getColumnIndexOrThrow("_id"))
            val pass_title = row.getString(row.getColumnIndexOrThrow("pass_title"))
            val pass_content = row.getString(row.getColumnIndexOrThrow("pass_content"))
            val pass_icon = row.getString(row.getColumnIndexOrThrow("pass_icon"))
            val pass_attachment = row.getString(row.getColumnIndexOrThrow("pass_attachment"))
            val pass_creation = row.getString(row.getColumnIndexOrThrow("pass_creation"))
            showBookmarkContextMenu(pass_title, pass_content, pass_icon, pass_attachment, _id, pass_creation)
            true
        }
    }

    private fun show_dialogFastToggle() {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_toggle, null)
        val sw_java = dialogView.findViewById<CheckBox>(R.id.switch_js)
        val whiteList_js = dialogView.findViewById<ImageButton>(R.id.imageButton_js)
        val sw_adBlock = dialogView.findViewById<CheckBox>(R.id.switch_adBlock)
        val whiteList_ab = dialogView.findViewById<ImageButton>(R.id.imageButton_ab)
        val sw_cookie = dialogView.findViewById<CheckBox>(R.id.switch_cookie)
        val whitelist_cookie = dialogView.findViewById<ImageButton>(R.id.imageButton_cookie)
        val dialog_title = dialogView.findViewById<TextView>(R.id.dialog_title)
        dialog_title.text = HelperUnit.domain(ninjaWebView.url)
        javaHosts = Javascript(this)
        cookieHosts = Cookie(this)
        adBlock = AdBlock(this)
        ninjaWebView = currentAlbumController as NinjaWebView
        val url = ninjaWebView.url
        sw_java.isChecked = sp.getBoolean(getString(R.string.sp_javascript), true)
        sw_adBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
        sw_cookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)
        if (javaHosts!!.isWhite(url)) {
            whiteList_js.setImageResource(R.drawable.check_green)
        } else {
            whiteList_js.setImageResource(R.drawable.ic_action_close_red)
        }
        if (cookieHosts!!.isWhite(url)) {
            whitelist_cookie.setImageResource(R.drawable.check_green)
        } else {
            whitelist_cookie.setImageResource(R.drawable.ic_action_close_red)
        }
        if (adBlock!!.isWhite(url)) {
            whiteList_ab.setImageResource(R.drawable.check_green)
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red)
        }
        whiteList_js.setOnClickListener {
            if (javaHosts!!.isWhite(ninjaWebView.url)) {
                whiteList_js.setImageResource(R.drawable.ic_action_close_red)
                javaHosts!!.removeDomain(HelperUnit.domain(url))
            } else {
                whiteList_js.setImageResource(R.drawable.check_green)
                javaHosts!!.addDomain(HelperUnit.domain(url))
            }
        }
        whitelist_cookie.setOnClickListener {
            if (cookieHosts!!.isWhite(ninjaWebView.url)) {
                whitelist_cookie.setImageResource(R.drawable.ic_action_close_red)
                cookieHosts!!.removeDomain(HelperUnit.domain(url))
            } else {
                whitelist_cookie.setImageResource(R.drawable.check_green)
                cookieHosts!!.addDomain(HelperUnit.domain(url))
            }
        }
        whiteList_ab.setOnClickListener {
            if (adBlock!!.isWhite(ninjaWebView.url)) {
                whiteList_ab.setImageResource(R.drawable.ic_action_close_red)
                adBlock!!.removeDomain(HelperUnit.domain(url))
            } else {
                whiteList_ab.setImageResource(R.drawable.check_green)
                adBlock!!.addDomain(HelperUnit.domain(url))
            }
        }
        sw_java.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                sp.edit().putBoolean(getString(R.string.sp_javascript), true).apply()
            } else {
                sp.edit().putBoolean(getString(R.string.sp_javascript), false).apply()
            }
        }
        sw_adBlock.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_ad_block), isChecked).apply() }
        sw_cookie.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_cookies), isChecked).apply() }
        val toggle_historyView = dialogView.findViewById<View>(R.id.toggle_historyView)
        val toggle_location = dialogView.findViewById<ImageButton>(R.id.toggle_location)
        val toggle_locationView = dialogView.findViewById<View>(R.id.toggle_locationView)
        val toggle_images = dialogView.findViewById<ImageButton>(R.id.toggle_images)
        val toggle_imagesView = dialogView.findViewById<View>(R.id.toggle_imagesView)
        val toggle_remote = dialogView.findViewById<ImageButton>(R.id.toggle_remote)
        val toggle_remoteView = dialogView.findViewById<View>(R.id.toggle_remoteView)
        val toggle_desktop = dialogView.findViewById<ImageButton>(R.id.toggle_desktop)
        val toggle_desktopView = dialogView.findViewById<View>(R.id.toggle_desktopView)
        dialogView.findViewById<ImageButton>(R.id.toggle_font).setOnClickListener {
            bottomSheetDialog?.cancel()
            val intent = Intent(this, Settings_Activity::class.java)
            startActivity(intent)
        }
        if (sp.getBoolean("saveHistory", false)) {
            toggle_historyView.visibility = View.VISIBLE
        } else {
            toggle_historyView.visibility = View.INVISIBLE
        }
        dialogView.findViewById<ImageButton>(R.id.toggle_history).setOnClickListener {
            if (sp.getBoolean("saveHistory", false)) {
                toggle_historyView.visibility = View.INVISIBLE
                sp.edit().putBoolean("saveHistory", false).apply()
            } else {
                toggle_historyView.visibility = View.VISIBLE
                sp.edit().putBoolean("saveHistory", true).apply()
            }
        }
        if (sp.getBoolean(getString(R.string.sp_location), false)) {
            toggle_locationView.visibility = View.VISIBLE
        } else {
            toggle_locationView.visibility = View.INVISIBLE
        }
        toggle_location.setOnClickListener {
            if (sp.getBoolean(getString(R.string.sp_location), false)) {
                toggle_locationView.visibility = View.INVISIBLE
                sp.edit().putBoolean(getString(R.string.sp_location), false).apply()
            } else {
                toggle_locationView.visibility = View.VISIBLE
                sp.edit().putBoolean(getString(R.string.sp_location), true).apply()
            }
        }
        if (sp.getBoolean(getString(R.string.sp_images), true)) {
            toggle_imagesView.visibility = View.VISIBLE
        } else {
            toggle_imagesView.visibility = View.INVISIBLE
        }
        toggle_images.setOnClickListener {
            if (sp.getBoolean(getString(R.string.sp_images), true)) {
                toggle_imagesView.visibility = View.INVISIBLE
                sp.edit().putBoolean(getString(R.string.sp_images), false).apply()
            } else {
                toggle_imagesView.visibility = View.VISIBLE
                sp.edit().putBoolean(getString(R.string.sp_images), true).apply()
            }
        }
        if (sp.getBoolean("sp_remote", true)) {
            toggle_remoteView.visibility = View.VISIBLE
        } else {
            toggle_remoteView.visibility = View.INVISIBLE
        }
        toggle_remote.setOnClickListener {
            if (sp.getBoolean("sp_remote", true)) {
                toggle_remoteView.visibility = View.INVISIBLE
                sp.edit().putBoolean("sp_remote", false).apply()
            } else {
                toggle_remoteView.visibility = View.VISIBLE
                sp.edit().putBoolean("sp_remote", true).apply()
            }
        }
        if (sp.getBoolean("sp_desktop", false)) {
            toggle_desktopView.visibility = View.VISIBLE
        } else {
            toggle_desktopView.visibility = View.INVISIBLE
        }
        toggle_desktop.setOnClickListener {
            if (sp.getBoolean("sp_desktop", false)) {
                toggle_desktopView.visibility = View.INVISIBLE
                sp.edit().putBoolean("sp_desktop", false).apply()
            } else {
                toggle_desktopView.visibility = View.VISIBLE
                sp.edit().putBoolean("sp_desktop", true).apply()
            }
        }
        dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
            if (ninjaWebView != null) {
                hideBottomSheetDialog()
                ninjaWebView.initPreferences()
                ninjaWebView.reload()
            }
        }
        dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
    }


    @Synchronized
    private fun addAlbum(title: String?, url: String?, foreground: Boolean) {
        if (url == null) return

        ninjaWebView = NinjaWebView(this)
        ninjaWebView.browserController = this
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
            showOmnibox()
            showAlbum(ninjaWebView)
        }
        if (url.isNotEmpty()) {
            ninjaWebView.loadUrl(url)
        }
    }

    private fun updateWebViewCount() {
        binding.omniboxOverview.text = BrowserContainer.size().toString()
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

    private fun scrollChange() {
        if (Objects.requireNonNull(sp.getBoolean("hideToolbar", true))) {
            ninjaWebView.setOnScrollChangeListener { scrollY, oldScrollY ->
                val height = Math.floor(ninjaWebView.contentHeight * ninjaWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ninjaWebView.height
                val cutoff = height - webViewHeight - 112 * Math.round(resources.displayMetrics.density)
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    // Daniel
                    //hideOmnibox();
                } else if (scrollY < oldScrollY) {
                    //showOmnibox()
                }
            }
        }
    }

    @Synchronized
    override fun updateProgress(progress: Int) {
        progressBar.progress = progress
        updateOmnibox()
        updateAutoComplete()
        scrollChange()
        HelperUnit.initRendering(contentFrame)
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
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = filePathCallback
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
                KeyEvent.KEYCODE_K ->  {
                    val controller = nextAlbumController(false) ?: return true
                    showAlbum(controller)
                }
                KeyEvent.KEYCODE_G -> ninjaWebView.jumpToBottom()
                else -> return false
            }
        } else { // non-capital
            when (event.keyCode) {
                // vim bindings
                KeyEvent.KEYCODE_B -> {
                    showOverview()
                    open_bookmark.performClick()
                }
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
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            val chooser = Intent.createChooser(intent, getString(R.string.menu_open_with))
            startActivity(chooser)
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.contextLink_newTabOpen).setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
        }
        dialogView.findViewById<LinearLayout>(R.id.contextLink_saveAs).setOnClickListener {
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
    private fun showOmnibox() {
        if (!searchOnSite) {
            fab_imageButtonNav.visibility = INVISIBLE
            searchPanel.visibility = GONE
            omnibox.visibility = VISIBLE
            omniboxTitle.visibility = VISIBLE
            binding.appBar.visibility = VISIBLE
            hideKeyboard()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun hideOmnibox() {
        if (!searchOnSite) {
            fab_imageButtonNav.visibility = VISIBLE
            searchPanel.visibility = GONE
            omnibox.visibility = GONE
            omniboxTitle.visibility = GONE
            binding.appBar.visibility = GONE
        }
    }

    private fun hideSearchPanel() {
        searchOnSite = false
        searchBox.setText("")
        showOmnibox()
    }

    @SuppressLint("RestrictedApi")
    private fun showSearchPanel() {
        searchOnSite = true
        fab_imageButtonNav.visibility = INVISIBLE
        omnibox.visibility = GONE
        searchPanel.visibility = VISIBLE
        omniboxTitle.visibility = GONE
        binding.appBar.visibility = VISIBLE
    }

    private fun showOverflow(): Boolean {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val binding = DialogMenuBinding.inflate(this.layoutInflater)
        binding.menuShareClipboard.setOnClickListener {
            hideBottomSheetDialog()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", url)
            Objects.requireNonNull(clipboard).setPrimaryClip(clip)
            NinjaToast.show(this, R.string.toast_copy_successful)
        }
        binding.buttonOpenFav.setOnClickListener {
            hideBottomSheetDialog()
            updateAlbum(sp.getString("favoriteURL", "https://github.com/plateaukao/browser"))
        }
        binding.menuSc.setOnClickListener {
            hideBottomSheetDialog()
            ninjaWebView.favicon
            HelperUnit.createShortcut(this, ninjaWebView.title, ninjaWebView.url, ninjaWebView.favicon)
        }
        binding.menuFav.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, ninjaWebView.url)
        }

        bottomSheetDialog?.setContentView(binding.root)
        bottomSheetDialog?.show()
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

    private fun showHistoryContextMenu(title: String, url: String, adapterRecord: Adapter_Record,
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
                adapterRecord.notifyDataSetChanged()
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
    }
}