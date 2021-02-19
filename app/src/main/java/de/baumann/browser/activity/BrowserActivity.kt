package de.baumann.browser.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
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
import android.view.View.OnFocusChangeListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobapphome.mahencryptorlib.MAHEncryptor
import de.baumann.browser.Ninja.R
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
    private lateinit var tab_plus_bottom: ImageButton
    private lateinit var adapter: Adapter_Record

    // Views
    private lateinit var searchUp: ImageButton
    private lateinit var searchDown: ImageButton
    private lateinit var searchCancel: ImageButton
    private lateinit var omniboxRefresh: ImageButton
    private lateinit var omniboxPageBack: ImageButton
    private lateinit var omniboxPageUp: ImageButton
    private lateinit var omniboxPageDown: ImageButton
    private lateinit var omniboxOverflow: ImageButton
    private lateinit var omniboxOverview: ImageButton
    private lateinit var omniboxTabCount: TextView
    private lateinit var open_startPage: ImageButton
    private lateinit var open_bookmark: ImageButton
    private lateinit var open_history: ImageButton
    private lateinit var open_menu: ImageButton
    private lateinit var fab_imageButtonNav: ImageButton
    private lateinit var inputBox: AutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchBox: EditText
    private lateinit var bottomSheetDialog_OverView: AlertDialog
    private lateinit var ninjaWebView: NinjaWebView
    private lateinit var listView: ListView
    private lateinit var omniboxTitle: TextView
    private lateinit var gridView: GridView
    private lateinit var tab_ScrollView: HorizontalScrollView
    private lateinit var overview_top: LinearLayout

    private var bottomSheetDialog: BottomSheetDialog? = null
    private var videoView: VideoView? = null
    private var customView: View? = null

    // Layouts
    private lateinit var appBar: RelativeLayout
    private lateinit var omnibox: RelativeLayout
    private lateinit var searchPanel: RelativeLayout
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
    private lateinit var mahEncryptor: MAHEncryptor
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
    private var dimen156dp = 0f
    private var dimen117dp = 0f
    private var searchOnSite = false
    private var onPause = false
    private var customViewCallback: CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var currentAlbumController: AlbumController? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

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

        WebView.enableSlowWholeDocumentDraw()
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp.edit().putInt("restart_changed", 0).apply()
        sp.edit().putBoolean("pdf_create", false).apply()
        HelperUnit.applyTheme(this)
        setContentView(R.layout.activity_main)
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
        try {
            mahEncryptor = MAHEncryptor.newInstance(Objects.requireNonNull(sp.getString("saved_key", "")))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contentFrame = findViewById(R.id.main_content)
        appBar = findViewById(R.id.appBar)
        dimen156dp = resources.getDimensionPixelSize(R.dimen.layout_width_156dp).toFloat()
        dimen117dp = resources.getDimensionPixelSize(R.dimen.layout_height_117dp).toFloat()
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
                val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
                textView.setText(R.string.toast_downloadComplete)
                val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
                action_ok.setOnClickListener {
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    hideBottomSheetDialog()
                }
                val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
                action_cancel.setOnClickListener { hideBottomSheetDialog() }
                bottomSheetDialog?.setContentView(dialogView)
                bottomSheetDialog?.show()
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
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
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_restart)
            val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
            action_ok.setOnClickListener { onDestroy() }
            val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
            action_cancel.setOnClickListener { dialog.cancel() }
            dialog.setContentView(dialogView)
            dialog.show()
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
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
                val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
                textView.setText(R.string.toast_downloadComplete)
                val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
                action_ok.setOnClickListener {
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    hideBottomSheetDialog()
                }
                val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
                action_cancel.setOnClickListener { hideBottomSheetDialog() }
                bottomSheetDialog?.setContentView(dialogView)
                bottomSheetDialog?.show()
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
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
                hideKeyboard(this)
                hideOverview()
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    return onHideCustomView()
                } else if (omnibox.visibility == View.GONE && sp.getBoolean("sp_toolbarShow", true)) {
                    showOmnibox()
                } else {
                    if (ninjaWebView.canGoBack()) {
                        ninjaWebView.goBack()
                    } else {
                        removeAlbum(currentAlbumController!!)
                    }
                }
                return true
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
        inputBox.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val url = (view.findViewById<View>(R.id.complete_item_url) as TextView).text.toString()
            updateAlbum(url)
            hideKeyboard(this)
        }
    }

    private fun showOverview() {
        showCurrentTabInOverview()
        currentAlbumController?.deactivate()
        currentAlbumController?.activate()
        bottomSheetDialog_OverView.show()
        updateOverViewHeight()

        Handler().postDelayed({
            tab_ScrollView.scrollTo(currentAlbumController?.albumView?.left
                    ?: 0, 0)
        }, 250)
    }

    private fun updateOverViewHeight() {
        val displayRectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(displayRectangle)
        val window = bottomSheetDialog_OverView.window ?: return
        val wlp = window.attributes
        wlp.width = displayRectangle.width()
        //wlp.height = (displayRectangle.height() * 0.5).toInt()
        wlp.height = WindowManager.LayoutParams.WRAP_CONTENT
        wlp.gravity = Gravity.BOTTOM
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = wlp
    }

    override fun hideOverview() {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.dismiss()
        }
    }

    private fun hideBottomSheetDialog() {
        bottomSheetDialog?.cancel()
    }

    override fun onClick(v: View) {
        val action = RecordAction(this)
        ninjaWebView = currentAlbumController as NinjaWebView
        try {
            title = ninjaWebView.title.trim { it <= ' ' }
            url = ninjaWebView.url.trim { it <= ' ' }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (v.id) {
            R.id.tab_plus_bottom -> {
                hideBottomSheetDialog()
                hideOverview()
                addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://www.google.com"), true)
            }
            R.id.button_closeTab -> {
                hideBottomSheetDialog()
                removeAlbum(currentAlbumController!!)
            }
            R.id.button_quit -> {
                hideBottomSheetDialog()
                doubleTapsQuit()
            }
            R.id.menu_shareScreenshot -> if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
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
                hideBottomSheetDialog()
                if (prepareRecord()) {
                    NinjaToast.show(this, getString(R.string.toast_share_failed))
                } else {
                    IntentUnit.share(this, title, url)
                }
            }
            R.id.menu_sharePDF -> {
                hideBottomSheetDialog()
                printPDF(true)
            }
            R.id.menu_openWith -> {
                hideBottomSheetDialog()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                val chooser = Intent.createChooser(intent, getString(R.string.menu_open_with))
                startActivity(chooser)
            }
            R.id.menu_saveScreenshot -> if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
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
                hideBottomSheetDialog()
                try {
                    val mahEncryptor = MAHEncryptor.newInstance(Objects.requireNonNull(sp.getString("saved_key", "")))
                    val encrypted_userName = mahEncryptor.encode("")
                    val encrypted_userPW = mahEncryptor.encode("")
                    val db = BookmarkList(this)
                    db.open()
                    if (db.isExist(url)) {
                        NinjaToast.show(this, R.string.toast_newTitle)
                    } else {
                        db.insert(HelperUnit.secString(ninjaWebView.title), url, encrypted_userName, encrypted_userPW, "01")
                        NinjaToast.show(this, R.string.toast_edit_successful)
                        initBookmarkList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(this, R.string.toast_error)
                }
            }
            R.id.menu_searchSite -> {
                hideBottomSheetDialog()
                hideKeyboard(this)
                showSearchPanel()
            }
            R.id.contextLink_saveAs -> {
                hideBottomSheetDialog()
                printPDF(false)
            }
            R.id.menu_settings -> {
                hideBottomSheetDialog()
                val settings = Intent(this@BrowserActivity, Settings_Activity::class.java)
                startActivity(settings)
            }
            R.id.menu_fileManager -> {
                hideBottomSheetDialog()
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
            R.id.omnibox_page_back -> if (ninjaWebView.canGoBack()) {
                ninjaWebView.goBack()
            } else {
                removeAlbum(currentAlbumController!!)
            }
            R.id.omnibox_page_up -> ninjaWebView.pageUpWithNoAnimation()
            R.id.omnibox_page_down -> ninjaWebView.pageDownWithNoAnimation()
            R.id.omnibox_refresh -> if (url != null && ninjaWebView.isLoadFinish) {
                if (url?.startsWith("https://") != true) {
                    bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                    val dialogView = View.inflate(this, R.layout.dialog_action, null)
                    val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
                    textView.setText(R.string.toast_unsecured)
                    val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
                    action_ok.setOnClickListener {
                        hideBottomSheetDialog()
                        ninjaWebView.loadUrl(url?.replace("http://", "https://") ?: "")
                    }
                    val action_cancel2 = dialogView.findViewById<Button>(R.id.action_cancel)
                    action_cancel2.setOnClickListener {
                        hideBottomSheetDialog()
                        ninjaWebView.reload()
                    }
                    bottomSheetDialog?.setContentView(dialogView)
                    bottomSheetDialog?.show()
                    HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
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

    // Methods
    private fun printPDF(share: Boolean) {
        try {
            if (share) {
                sp.edit().putBoolean("pdf_share", true).apply()
            } else {
                sp.edit().putBoolean("pdf_share", false).apply()
            }
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
            Handler().postDelayed({ open_history.performClick() }, 250)
        } else if ("sc_bookmark" == action) {
            addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            showOverview()
            Handler().postDelayed({ open_bookmark.performClick() }, 250)
        } else if ("sc_startPage" == action) {
            addAlbum(null, sp.getString("favoriteURL", "https://www.google.com"), true)
            showOverview()
            Handler().postDelayed({ open_startPage.performClick() }, 250)
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
        omniboxRefresh = findViewById(R.id.omnibox_refresh)
        omniboxPageBack = findViewById(R.id.omnibox_page_back)
        omniboxPageUp = findViewById(R.id.omnibox_page_up)
        omniboxPageDown = findViewById(R.id.omnibox_page_down)
        omniboxOverview = findViewById(R.id.omnibox_overview)
        omniboxTabCount = findViewById(R.id.omnibox_web_count)
        omniboxOverflow = findViewById(R.id.omnibox_overflow)
        omniboxTitle = findViewById(R.id.omnibox_title)
        progressBar = findViewById(R.id.main_progress_bar)
        val nav_position = Objects.requireNonNull(sp.getString("nav_position", "0"))
        fab_imageButtonNav = when (nav_position) {
            "1" -> findViewById(R.id.fab_imageButtonNav_left)
            "2" -> findViewById(R.id.fab_imageButtonNav_center)
            else -> findViewById(R.id.fab_imageButtonNav_right)
        }
        fab_imageButtonNav.setOnLongClickListener(OnLongClickListener {
            show_dialogFastToggle()
            false
        })
        omniboxOverflow.setOnLongClickListener(OnLongClickListener {
            show_dialogFastToggle()
            false
        })
        fab_imageButtonNav.setOnClickListener(View.OnClickListener { showOmnibox() })
        omniboxOverflow.setOnClickListener(View.OnClickListener { showOverflow() })
        if (sp.getBoolean("sp_gestures_use", true)) {
            fab_imageButtonNav.setOnTouchListener(object : SwipeTouchListener(this) {
                override fun onSwipeTop() {
                    performGesture("setting_gesture_nav_up")
                }

                override fun onSwipeBottom() {
                    performGesture("setting_gesture_nav_down")
                }

                override fun onSwipeRight() {
                    performGesture("setting_gesture_nav_right")
                }

                override fun onSwipeLeft() {
                    performGesture("setting_gesture_nav_left")
                }
            })
            omniboxOverflow.setOnTouchListener(object : SwipeTouchListener(this) {
                override fun onSwipeTop() {
                    performGesture("setting_gesture_nav_up")
                }

                override fun onSwipeBottom() {
                    performGesture("setting_gesture_nav_down")
                }

                override fun onSwipeRight() {
                    performGesture("setting_gesture_nav_right")
                }

                override fun onSwipeLeft() {
                    performGesture("setting_gesture_nav_left")
                }
            })
            inputBox.setOnTouchListener(object : SwipeTouchListener(this) {
                override fun onSwipeTop() {
                    performGesture("setting_gesture_tb_up")
                }

                override fun onSwipeBottom() {
                    performGesture("setting_gesture_tb_down")
                }

                override fun onSwipeRight() {
                    performGesture("setting_gesture_tb_right")
                }

                override fun onSwipeLeft() {
                    performGesture("setting_gesture_tb_left")
                }
            })
        }
        inputBox.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val query = inputBox.getText().toString().trim { it <= ' ' }
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnEditorActionListener true
            }
            updateAlbum(query)
            showOmnibox()
            false
        })
        inputBox.setOnFocusChangeListener(OnFocusChangeListener { v, hasFocus ->
            if (inputBox.hasFocus()) {
                ninjaWebView.stopLoading()
                inputBox.setText(ninjaWebView.url)
                Handler().postDelayed({
                    toggleIconsOnOmnibox(true)
                    inputBox.requestFocus()
                    inputBox.setSelection(0, inputBox.getText().toString().length)
                }, 250)
            } else {
                toggleIconsOnOmnibox(false)
                omniboxTitle.setText(ninjaWebView.title)
                hideKeyboard(this)
            }
        })
        updateAutoComplete()
        omniboxRefresh.setOnClickListener(this)
        omniboxPageBack.setOnClickListener(this)
        omniboxPageUp.setOnClickListener(this)
        omniboxPageDown.setOnClickListener(this)
        omniboxOverview.setOnClickListener(this)

        // scroll to top
        omniboxPageUp.setOnLongClickListener(OnLongClickListener {
            ninjaWebView.jumpToTop()
            true
        })

        // hide bottom bar when refresh button is long pressed.
        omniboxRefresh.setOnLongClickListener(OnLongClickListener {
            hideOmnibox()
            true
        })
    }

    private fun toggleIconsOnOmnibox(shouldHide: Boolean) {
        val visibility = if (shouldHide) View.GONE else View.VISIBLE
        omniboxTitle.visibility = visibility
        omniboxRefresh.visibility = visibility
        omniboxOverview.visibility = visibility
        omniboxTabCount.visibility = visibility
        omniboxPageBack.visibility = visibility
        omniboxPageDown.visibility = visibility
        omniboxPageUp.visibility = visibility
        omniboxOverflow.visibility = visibility
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverview() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = View.inflate(this, R.layout.dialog_overiew, null)
        dialogBuilder.setView(dialogView)
        bottomSheetDialog_OverView = dialogBuilder.create()
        val back = ColorDrawable(Color.TRANSPARENT)
        val inset = InsetDrawable(back, -8, -8, -8, -8)
        bottomSheetDialog_OverView.window?.setBackgroundDrawable(inset)
        bottomSheetDialog_OverView.setCanceledOnTouchOutside(true)
        open_startPage = dialogView.findViewById(R.id.open_newTab_2)
        open_bookmark = dialogView.findViewById(R.id.open_bookmark_2)
        open_history = dialogView.findViewById(R.id.open_history_2)
        open_menu = dialogView.findViewById(R.id.open_menu)
        tab_container = dialogView.findViewById(R.id.tab_container)
        tab_plus_bottom = dialogView.findViewById(R.id.tab_plus_bottom)
        tab_plus_bottom.setOnClickListener(this)
        tab_ScrollView = dialogView.findViewById(R.id.tab_ScrollView)
        overview_top = dialogView.findViewById(R.id.overview_top)
        listView = dialogView.findViewById(R.id.home_list_2)
        open_startPageView = dialogView.findViewById(R.id.open_newTabView)
        open_bookmarkView = dialogView.findViewById(R.id.open_bookmarkView)
        open_historyView = dialogView.findViewById(R.id.open_historyView)
        gridView = dialogView.findViewById(R.id.home_grid_2)

        // allow scrolling in listView without closing the bottomSheetDialog
        listView.setOnTouchListener(OnTouchListener { v, event ->
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) { // Disallow NestedScrollView to intercept touch events.
                if (listView.canScrollVertically(-1)) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event)
            true
        })
        gridView.setOnTouchListener(OnTouchListener { v, event ->
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) { // Disallow NestedScrollView to intercept touch events.
                if (gridView.canScrollVertically(-1)) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event)
            true
        })
        open_menu.setOnClickListener(View.OnClickListener {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_menu_overview, null)
            val bookmark_sort = dialogView.findViewById<LinearLayout>(R.id.bookmark_sort)
            val bookmark_filter = dialogView.findViewById<LinearLayout>(R.id.bookmark_filter)
            if (overViewTab == getString(R.string.album_title_bookmarks)) {
                bookmark_filter.visibility = View.VISIBLE
                bookmark_sort.visibility = View.VISIBLE
            } else if (overViewTab == getString(R.string.album_title_home)) {
                bookmark_filter.visibility = View.GONE
                bookmark_sort.visibility = View.VISIBLE
            } else if (overViewTab == getString(R.string.album_title_history)) {
                bookmark_filter.visibility = View.GONE
                bookmark_sort.visibility = View.GONE
            }
            bookmark_filter.setOnClickListener { show_dialogFilter() }
            bookmark_sort.setOnClickListener {
                hideBottomSheetDialog()
                bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                val dialogView = View.inflate(this, R.layout.dialog_bookmark_sort, null)
                val dialog_sortName = dialogView.findViewById<LinearLayout>(R.id.dialog_sortName)
                val bookmark_sort_tv = dialogView.findViewById<TextView>(R.id.bookmark_sort_tv)
                if (overViewTab == getString(R.string.album_title_bookmarks)) {
                    bookmark_sort_tv.text = resources.getString(R.string.dialog_sortIcon)
                } else if (overViewTab == getString(R.string.album_title_home)) {
                    bookmark_sort_tv.text = resources.getString(R.string.dialog_sortDate)
                }
                dialog_sortName.setOnClickListener {
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
                val dialog_sortIcon = dialogView.findViewById<LinearLayout>(R.id.dialog_sortIcon)
                dialog_sortIcon.setOnClickListener {
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
                bottomSheetDialog?.setContentView(dialogView)
                bottomSheetDialog?.show()
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
            }
            val tv_delete = dialogView.findViewById<LinearLayout>(R.id.tv_delete)
            tv_delete.setOnClickListener {
                hideBottomSheetDialog()
                bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                val dialogView3 = View.inflate(this, R.layout.dialog_action, null)
                val textView = dialogView3.findViewById<TextView>(R.id.dialog_text)
                textView.setText(R.string.hint_database)
                val action_ok = dialogView3.findViewById<Button>(R.id.action_ok)
                action_ok.setOnClickListener {
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
                            BrowserUnit.clearHistory(BrowserActivity@ this)
                            open_history.performClick()
                        }
                    }
                    hideBottomSheetDialog()
                }
                val action_cancel = dialogView3.findViewById<Button>(R.id.action_cancel)
                action_cancel.setOnClickListener { hideBottomSheetDialog() }
                bottomSheetDialog?.setContentView(dialogView3)
                bottomSheetDialog?.show()
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView3, BottomSheetBehavior.STATE_EXPANDED)
            }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        })
        bottomSheetDialog_OverView.setContentView(dialogView)
        open_startPage.setOnClickListener(View.OnClickListener {
            overview_top.visibility = View.VISIBLE
            gridView.visibility = View.VISIBLE
            listView.visibility = View.GONE
            open_startPageView.visibility = View.VISIBLE
            open_bookmarkView.visibility = View.INVISIBLE
            open_historyView.visibility = View.INVISIBLE
            overViewTab = getString(R.string.album_title_home)
            val action = RecordAction(BrowserActivity@ this)
            action.open(false)
            val gridList = action.listGrid(BrowserActivity@ this)
            action.close()
            gridAdapter = GridAdapter(this, gridList)
            gridView.adapter = gridAdapter
            gridAdapter.notifyDataSetChanged()
            gridView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                updateAlbum(gridList[position].url)
                hideOverview()
            }
            gridView.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
                showStartPageContextMenu(gridList[position].title, gridList[position].url, gridList[position])
                true
            }
        })
        open_bookmark.setOnClickListener(View.OnClickListener {
            overview_top.setVisibility(View.GONE)
            gridView.setVisibility(View.GONE)
            listView.setVisibility(View.VISIBLE)
            open_startPageView.setVisibility(View.INVISIBLE)
            open_bookmarkView.setVisibility(View.VISIBLE)
            open_historyView.setVisibility(View.INVISIBLE)
            overViewTab = getString(R.string.album_title_bookmarks)
            sp.edit().putString("filter_bookmarks", "00").apply()
            initBookmarkList()
        })
        open_bookmark.setOnLongClickListener(OnLongClickListener {
            show_dialogFilter()
            false
        })
        open_history.setOnClickListener(View.OnClickListener {
            overview_top.setVisibility(View.GONE)
            gridView.setVisibility(View.GONE)
            listView.setVisibility(View.VISIBLE)
            open_startPageView.setVisibility(View.INVISIBLE)
            open_bookmarkView.setVisibility(View.INVISIBLE)
            open_historyView.setVisibility(View.VISIBLE)
            overViewTab = getString(R.string.album_title_history)
            val action = RecordAction(BrowserActivity@ this)
            action.open(false)
            val list: List<Record>
            list = action.listEntries(this, false)
            action.close()
            adapter = Adapter_Record(this, list)
            listView.adapter = adapter
            adapter.notifyDataSetChanged()
            listView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                updateAlbum(list[position].url)
                hideOverview()
            }
            listView.setOnItemLongClickListener(OnItemLongClickListener { parent, view, position, id ->
                showHistoryContextMenu(list[position].title, list[position].url, adapter, list, position)
                true
            })
        })
        showCurrentTabInOverview()
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
        searchUp = findViewById(R.id.main_search_up)
        searchDown = findViewById(R.id.main_search_down)
        searchCancel = findViewById(R.id.main_search_cancel)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (currentAlbumController != null) {
                    (currentAlbumController as NinjaWebView).findAllAsync(s.toString())
                }
            }
        })
        searchBox.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@OnEditorActionListener false
            }
            if (searchBox.getText().toString().isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnEditorActionListener true
            }
            false
        })
        searchUp.setOnClickListener(View.OnClickListener {
            val query = searchBox.getText().toString()
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnClickListener
            }
            hideKeyboard(this)
            (currentAlbumController as NinjaWebView).findNext(false)
        })
        searchDown.setOnClickListener(View.OnClickListener {
            val query = searchBox.getText().toString()
            if (query.isEmpty()) {
                NinjaToast.show(this, getString(R.string.toast_input_empty))
                return@OnClickListener
            }
            hideKeyboard(this)
            (currentAlbumController as NinjaWebView).findNext(true)
        })
        searchCancel.setOnClickListener(View.OnClickListener { hideSearchPanel() })
    }

    private fun initBookmarkList() {
        val db = BookmarkList(this)
        val row: Cursor
        db.open()
        val layoutStyle = R.layout.list_item_bookmark
        val xml_id = intArrayOf(R.id.record_item_title)
        val column = arrayOf("pass_title")
        val search = sp.getString("filter_bookmarks", "00")
        row = if (Objects.requireNonNull(search) == "00") {
            db.fetchAllData(this)
        } else {
            db.fetchDataByFilter(search, "pass_creation")
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
            toast_login(pass_icon, pass_attachment)
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
        sw_adBlock.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                sp.edit().putBoolean(getString(R.string.sp_ad_block), true).apply()
            } else {
                sp.edit().putBoolean(getString(R.string.sp_ad_block), false).apply()
            }
        }
        sw_cookie.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                sp.edit().putBoolean(getString(R.string.sp_cookies), true).apply()
            } else {
                sp.edit().putBoolean(getString(R.string.sp_cookies), false).apply()
            }
        }
        val toggle_history = dialogView.findViewById<ImageButton>(R.id.toggle_history)
        val toggle_historyView = dialogView.findViewById<View>(R.id.toggle_historyView)
        val toggle_location = dialogView.findViewById<ImageButton>(R.id.toggle_location)
        val toggle_locationView = dialogView.findViewById<View>(R.id.toggle_locationView)
        val toggle_images = dialogView.findViewById<ImageButton>(R.id.toggle_images)
        val toggle_imagesView = dialogView.findViewById<View>(R.id.toggle_imagesView)
        val toggle_remote = dialogView.findViewById<ImageButton>(R.id.toggle_remote)
        val toggle_remoteView = dialogView.findViewById<View>(R.id.toggle_remoteView)
        val toggle_desktop = dialogView.findViewById<ImageButton>(R.id.toggle_desktop)
        val toggle_desktopView = dialogView.findViewById<View>(R.id.toggle_desktopView)
        val toggle_font = dialogView.findViewById<ImageButton>(R.id.toggle_font)
        toggle_font.setOnClickListener {
            bottomSheetDialog?.cancel()
            val intent = Intent(this, Settings_Activity::class.java)
            startActivity(intent)
        }
        if (sp.getBoolean("saveHistory", false)) {
            toggle_historyView.visibility = View.VISIBLE
        } else {
            toggle_historyView.visibility = View.INVISIBLE
        }
        toggle_history.setOnClickListener {
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
        val but_OK = dialogView.findViewById<Button>(R.id.action_ok)
        but_OK.setOnClickListener {
            if (ninjaWebView != null) {
                hideBottomSheetDialog()
                ninjaWebView.initPreferences()
                ninjaWebView.reload()
            }
        }
        val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
        action_cancel.setOnClickListener { hideBottomSheetDialog() }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun toast_login(userName: String, passWord: String) {
        try {
            val decrypted_userName = mahEncryptor.decode(userName)
            val decrypted_userPW = mahEncryptor.decode(passWord)
            val clipboard = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            val unCopy: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val clip = ClipData.newPlainText("text", decrypted_userName)
                    clipboard.setPrimaryClip(clip)
                    NinjaToast.show(this@BrowserActivity, R.string.toast_copy_successful)
                }
            }
            val pwCopy: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val clip = ClipData.newPlainText("text", decrypted_userPW)
                    clipboard.setPrimaryClip(clip)
                    NinjaToast.show(this@BrowserActivity, R.string.toast_copy_successful)
                }
            }
            val intentFilter = IntentFilter("unCopy")
            registerReceiver(unCopy, intentFilter)
            val copy = Intent("unCopy")
            val copyUN = PendingIntent.getBroadcast(this, 0, copy, PendingIntent.FLAG_CANCEL_CURRENT)
            val intentFilter2 = IntentFilter("pwCopy")
            registerReceiver(pwCopy, intentFilter2)
            val copy2 = Intent("pwCopy")
            val copyPW = PendingIntent.getBroadcast(this, 1, copy2, PendingIntent.FLAG_CANCEL_CURRENT)
            val builder: NotificationCompat.Builder
            val mNotificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val CHANNEL_ID = "browser_not" // The id of the channel.
                val name: CharSequence = getString(R.string.app_name) // The user-visible name of the channel.
                val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
                mNotificationManager.createNotificationChannel(mChannel)
                NotificationCompat.Builder(this, CHANNEL_ID)
            } else {
                NotificationCompat.Builder(this)
            }
            val action_UN = NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pasteUN), copyUN).build()
            val action_PW = NotificationCompat.Action.Builder(R.drawable.icon_earth, getString(R.string.toast_titleConfirm_pastePW), copyPW).build()
            val n = builder
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setSmallIcon(R.drawable.ic_notification_ninja)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.toast_titleConfirm_paste))
                    .setColor(resources.getColor(R.color.colorAccent))
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(LongArray(0))
                    .addAction(action_UN)
                    .addAction(action_PW)
                    .build()
            val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            if (decrypted_userName.isNotEmpty() || decrypted_userPW.isNotEmpty()) {
                notificationManager.notify(0, n)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(this, R.string.toast_error)
        }
    }

    @Synchronized
    private fun addAlbum(title: String?, url: String?, foreground: Boolean) {
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
        if (url != null && !url.isEmpty()) {
            ninjaWebView.loadUrl(url)
        }
    }

    private fun updateWebViewCount() {
        omniboxTabCount.text = BrowserContainer.size().toString()
    }

    @Synchronized
    private fun updateAlbum(url: String?) {
        (currentAlbumController as NinjaWebView).loadUrl(url)
        updateOmnibox()
    }

    private fun closeTabConfirmation(okAction: Runnable) {
        if (!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction.run()
        } else {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_close_tab)
            val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
            action_ok.setOnClickListener {
                okAction.run()
                hideBottomSheetDialog()
            }
            val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
            action_cancel.setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    @Synchronized
    override fun removeAlbum(controller: AlbumController) {
        if (BrowserContainer.size() <= 1) {
            if (!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit()
            } else {
                updateAlbum(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"))
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
                if (scrollY > oldScrollY && cutoff >= scrollY) {
                    // Daniel
                    //hideOmnibox();
                } else if (scrollY < oldScrollY) {
                    showOmnibox()
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
            omniboxRefresh.setImageResource(R.drawable.icon_close)
        } else {
            try {
                if (ninjaWebView.url.contains("https://")) {
                    omniboxRefresh.setImageResource(R.drawable.icon_refresh)
                } else {
                    omniboxRefresh.setImageResource(R.drawable.icon_alert)
                }
            } catch (e: Exception) {
                omniboxRefresh.setImageResource(R.drawable.icon_refresh)
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

    private fun show_contextMenu_link(url: String?) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_link, null)
        val contextLink_newTab = dialogView.findViewById<LinearLayout>(R.id.contextLink_newTab)
        contextLink_newTab.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        val contextLink__shareLink = dialogView.findViewById<LinearLayout>(R.id.contextLink__shareLink)
        contextLink__shareLink.setOnClickListener {
            if (prepareRecord()) {
                NinjaToast.show(this, getString(R.string.toast_share_failed))
            } else {
                IntentUnit.share(this, "", url)
            }
            hideBottomSheetDialog()
        }
        val contextLink_openWith = dialogView.findViewById<LinearLayout>(R.id.contextLink_openWith)
        contextLink_openWith.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            val chooser = Intent.createChooser(intent, getString(R.string.menu_open_with))
            startActivity(chooser)
            hideBottomSheetDialog()
        }
        val contextLink_newTabOpen = dialogView.findViewById<LinearLayout>(R.id.contextLink_newTabOpen)
        contextLink_newTabOpen.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
        }
        val contextLink_saveAs = dialogView.findViewById<LinearLayout>(R.id.contextLink_saveAs)
        contextLink_saveAs.setOnClickListener {
            try {
                hideBottomSheetDialog()
                val builder = AlertDialog.Builder(this@BrowserActivity)
                val dialogView = View.inflate(this, R.layout.dialog_edit_extension, null)
                val editTitle = dialogView.findViewById<EditText>(R.id.dialog_edit)
                val editExtension = dialogView.findViewById<EditText>(R.id.dialog_edit_extension)
                val filename = URLUtil.guessFileName(url, null, null)
                editTitle.setHint(R.string.dialog_title_hint)
                editTitle.setText(HelperUnit.fileName(ninjaWebView.url))
                val extension = filename.substring(filename.lastIndexOf("."))
                if (extension.length <= 8) {
                    editExtension.setText(extension)
                }
                builder.setView(dialogView)
                builder.setTitle(R.string.menu_edit)
                builder.setPositiveButton(R.string.app_ok) { dialog, whichButton ->
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
                                hideKeyboard(this)
                            }
                        } else {
                            val source = Uri.parse(url)
                            val request = DownloadManager.Request(source)
                            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            val dm = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
                            dm.enqueue(request)
                            hideKeyboard(this)
                        }
                    }
                }
                builder.setNegativeButton(R.string.app_cancel) { dialog, whichButton ->
                    dialog.cancel()
                    hideKeyboard(this)
                }
                val dialog = builder.create()
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    override fun onLongPress(url: String?) {
        val result = ninjaWebView.hitTestResult
        if (url != null) {
            show_contextMenu_link(url)
        } else if (result.type == HitTestResult.IMAGE_TYPE || result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.type == HitTestResult.SRC_ANCHOR_TYPE) {
            show_contextMenu_link(result.extra)
        }
    }

    private fun doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) {
            finish()
        } else {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_quit)
            val action_ok = dialogView.findViewById<Button>(R.id.action_ok)
            action_ok.setOnClickListener { finish() }
            val action_cancel = dialogView.findViewById<Button>(R.id.action_cancel)
            action_cancel.setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showOmnibox() {
        if (!searchOnSite) {
            fab_imageButtonNav.visibility = View.GONE
            searchPanel.visibility = View.GONE
            omnibox.visibility = View.VISIBLE
            omniboxTitle.visibility = View.VISIBLE
            appBar.visibility = View.VISIBLE
            hideKeyboard(this)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun hideOmnibox() {
        if (!searchOnSite) {
            fab_imageButtonNav.visibility = View.VISIBLE
            searchPanel.visibility = View.GONE
            omnibox.visibility = View.GONE
            omniboxTitle.visibility = View.GONE
            appBar.visibility = View.GONE
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
        fab_imageButtonNav.visibility = View.GONE
        omnibox.visibility = View.GONE
        searchPanel.visibility = View.VISIBLE
        omniboxTitle.visibility = View.GONE
        appBar.visibility = View.VISIBLE
    }

    private fun showOverflow(): Boolean {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu, null)
        dialogView.findViewById<View>(R.id.button_closeTab).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.button_quit).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_shareScreenshot).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_shareLink).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_sharePDF).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_openWith).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_saveScreenshot).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_saveBookmark).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.contextLink_saveAs).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_searchSite).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_settings).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_download).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_fileManager).setOnClickListener(this)
        dialogView.findViewById<View>(R.id.menu_shareClipboard).setOnClickListener {
            hideBottomSheetDialog()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", url)
            Objects.requireNonNull(clipboard).setPrimaryClip(clip)
            NinjaToast.show(this, R.string.toast_copy_successful)
        }
        dialogView.findViewById<View>(R.id.button_openFav).setOnClickListener {
            hideBottomSheetDialog()
            updateAlbum(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"))
        }
        dialogView.findViewById<View>(R.id.menu_sc).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, ninjaWebView.title, ninjaWebView.url)
        }
        dialogView.findViewById<View>(R.id.menu_fav).setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, ninjaWebView.url)
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        return true
    }

    private fun showStartPageContextMenu(title: String, url: String, gridItem: GridItem)
    {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        val db = BookmarkList(this)
        db.open()
        val contextList_edit = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit)
        val contextList_fav = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav)
        val contextList_sc = dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc)
        val contextList_newTab = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab)
        val contextList_newTabOpen = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen)
        val contextList_delete = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete)
        if (overViewTab == getString(R.string.album_title_history)) {
            contextList_edit.visibility = View.GONE
        } else {
            contextList_edit.visibility = View.VISIBLE
        }
        contextList_fav.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        contextList_sc.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url)
        }
        contextList_newTab.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        contextList_newTabOpen.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        contextList_delete.setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_titleConfirm_delete)

            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                val action = RecordAction(this@BrowserActivity)
                action.open(true)
                action.deleteGridItem(gridItem)
                action.close()
                deleteFile(gridItem.filename)
                open_startPage.performClick()
                hideBottomSheetDialog()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }

        contextList_edit.setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_edit_title, null)
            val editText = dialogView.findViewById<EditText>(R.id.dialog_edit)
            editText.setHint(R.string.dialog_title_hint)
            editText.setText(title)
            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                val text = editText.text.toString().trim { it <= ' ' }
                if (text.isEmpty()) {
                    NinjaToast.show(this, getString(R.string.toast_input_empty))
                } else {
                    val action = RecordAction(this@BrowserActivity)
                    action.open(true)
                    gridItem.title = text
                    action.updateGridItem(gridItem)
                    action.close()
                    hideKeyboard(this)
                    open_startPage.performClick()
                }
                hideBottomSheetDialog()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                hideKeyboard(this)
                hideBottomSheetDialog()
            }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun showBookmarkContextMenu(title: String, url: String,
                                        userName: String, userPW: String, _id: String, pass_creation: String?
    ) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        val db = BookmarkList(this)
        db.open()
        val contextList_edit = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit)
        val contextList_fav = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav)
        val contextList_sc = dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc)
        val contextList_newTab = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab)
        val contextList_newTabOpen = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen)
        val contextList_delete = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete)
        if (overViewTab == getString(R.string.album_title_history)) {
            contextList_edit.visibility = View.GONE
        } else {
            contextList_edit.visibility = View.VISIBLE
        }
        contextList_fav.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        contextList_sc.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url)
        }
        contextList_newTab.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        contextList_newTabOpen.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        contextList_delete.setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_titleConfirm_delete)

            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                db.delete(_id.toInt())
                initBookmarkList()
                hideBottomSheetDialog()
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }

        contextList_edit.setOnClickListener {
            hideBottomSheetDialog()
                try {
                    bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                    val dialogView = View.inflate(this, R.layout.dialog_edit_bookmark, null)
                    val pass_titleET = dialogView.findViewById<EditText>(R.id.pass_title)
                    val pass_userNameET = dialogView.findViewById<EditText>(R.id.pass_userName)
                    val pass_userPWET = dialogView.findViewById<EditText>(R.id.pass_userPW)
                    val pass_URLET = dialogView.findViewById<EditText>(R.id.pass_url)
                    val ib_icon = dialogView.findViewById<ImageView>(R.id.ib_icon)
                    val decrypted_userName = mahEncryptor.decode(userName)
                    val decrypted_userPW = mahEncryptor.decode(userPW)
                    pass_titleET.setText(title)
                    pass_userNameET.setText(decrypted_userName)
                    pass_userPWET.setText(decrypted_userPW)
                    pass_URLET.setText(url)
                    dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                        try {
                            val input_pass_title = pass_titleET.text.toString().trim { it <= ' ' }
                            val input_pass_url = pass_URLET.text.toString().trim { it <= ' ' }
                            val encrypted_userName = mahEncryptor.encode(pass_userNameET.text.toString().trim { it <= ' ' })
                            val encrypted_userPW = mahEncryptor.encode(pass_userPWET.text.toString().trim { it <= ' ' })
                            db.update(_id.toInt(), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url), HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), pass_creation)
                            initBookmarkList()
                            hideKeyboard(this)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            NinjaToast.show(this, R.string.toast_error)
                        }
                        hideBottomSheetDialog()
                    }
                    dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                        hideKeyboard(this)
                        hideBottomSheetDialog()
                    }
                    HelperUnit.switchIcon(this, pass_creation, "pass_creation", ib_icon)
                    bottomSheetDialog?.setContentView(dialogView)
                    bottomSheetDialog?.show()
                    HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
                    ib_icon.setOnClickListener {
                        try {
                            val input_pass_title = pass_titleET.text.toString().trim { it <= ' ' }
                            val input_pass_url = pass_URLET.text.toString().trim { it <= ' ' }
                            val encrypted_userName = mahEncryptor.encode(pass_userNameET.text.toString().trim { it <= ' ' })
                            val encrypted_userPW = mahEncryptor.encode(pass_userPWET.text.toString().trim { it <= ' ' })
                            hideBottomSheetDialog()
                            hideKeyboard(this)
                            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                            val dialogView = View.inflate(this, R.layout.dialog_edit_icon, null)
                            val grid = dialogView.findViewById<GridView>(R.id.grid_filter)
                            val itemAlbum_01 = GridItem_filter(sp.getString("icon_01", resources.getString(R.string.color_red)), "icon_01", resources.getDrawable(R.drawable.circle_red_big), "01")
                            val itemAlbum_02 = GridItem_filter(sp.getString("icon_02", resources.getString(R.string.color_pink)), "icon_02", resources.getDrawable(R.drawable.circle_pink_big), "02")
                            val itemAlbum_03 = GridItem_filter(sp.getString("icon_03", resources.getString(R.string.color_purple)), "icon_03", resources.getDrawable(R.drawable.circle_purple_big), "03")
                            val itemAlbum_04 = GridItem_filter(sp.getString("icon_04", resources.getString(R.string.color_blue)), "icon_04", resources.getDrawable(R.drawable.circle_blue_big), "04")
                            val itemAlbum_05 = GridItem_filter(sp.getString("icon_05", resources.getString(R.string.color_teal)), "icon_05", resources.getDrawable(R.drawable.circle_teal_big), "05")
                            val itemAlbum_06 = GridItem_filter(sp.getString("icon_06", resources.getString(R.string.color_green)), "icon_06", resources.getDrawable(R.drawable.circle_green_big), "06")
                            val itemAlbum_07 = GridItem_filter(sp.getString("icon_07", resources.getString(R.string.color_lime)), "icon_07", resources.getDrawable(R.drawable.circle_lime_big), "07")
                            val itemAlbum_08 = GridItem_filter(sp.getString("icon_08", resources.getString(R.string.color_yellow)), "icon_08", resources.getDrawable(R.drawable.circle_yellow_big), "08")
                            val itemAlbum_09 = GridItem_filter(sp.getString("icon_09", resources.getString(R.string.color_orange)), "icon_09", resources.getDrawable(R.drawable.circle_orange_big), "09")
                            val itemAlbum_10 = GridItem_filter(sp.getString("icon_10", resources.getString(R.string.color_brown)), "icon_10", resources.getDrawable(R.drawable.circle_brown_big), "10")
                            val itemAlbum_11 = GridItem_filter(sp.getString("icon_11", resources.getString(R.string.color_grey)), "icon_11", resources.getDrawable(R.drawable.circle_grey_big), "11")
                            val gridList = mutableListOf<GridItem_filter>()
                            if (sp.getBoolean("filter_01", true)) {
                                gridList.add(itemAlbum_01)
                            }
                            if (sp.getBoolean("filter_02", true)) {
                                gridList.add(itemAlbum_02)
                            }
                            if (sp.getBoolean("filter_03", true)) {
                                gridList.add(itemAlbum_03)
                            }
                            if (sp.getBoolean("filter_04", true)) {
                                gridList.add(itemAlbum_04)
                            }
                            if (sp.getBoolean("filter_05", true)) {
                                gridList.add(itemAlbum_05)
                            }
                            if (sp.getBoolean("filter_06", true)) {
                                gridList.add(itemAlbum_06)
                            }
                            if (sp.getBoolean("filter_07", true)) {
                                gridList.add(itemAlbum_07)
                            }
                            if (sp.getBoolean("filter_08", true)) {
                                gridList.add(itemAlbum_08)
                            }
                            if (sp.getBoolean("filter_09", true)) {
                                gridList.add(itemAlbum_09)
                            }
                            if (sp.getBoolean("filter_10", true)) {
                                gridList.add(itemAlbum_10)
                            }
                            if (sp.getBoolean("filter_11", true)) {
                                gridList.add(itemAlbum_11)
                            }
                            val gridAdapter = GridAdapter_filter(this, gridList)
                            grid.adapter = gridAdapter
                            gridAdapter.notifyDataSetChanged()
                            grid.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                                db.update(_id.toInt(), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url), HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), gridList[position].ordinal)
                                initBookmarkList()
                                hideBottomSheetDialog()
                            }
                            bottomSheetDialog?.setContentView(dialogView)
                            bottomSheetDialog?.show()
                            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            hideBottomSheetDialog()
                            NinjaToast.show(this, R.string.toast_error)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(this, R.string.toast_error)
                }
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun showHistoryContextMenu(title: String, url: String, adapterRecord: Adapter_Record,
                                       recordList: MutableList<Record>, location: Int
    ) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        val db = BookmarkList(this)
        db.open()
        val contextList_edit = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit)
        val contextList_fav = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav)
        val contextList_sc = dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc)
        val contextList_newTab = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab)
        val contextList_newTabOpen = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen)
        val contextList_delete = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete)
        if (overViewTab == getString(R.string.album_title_history)) {
            contextList_edit.visibility = View.GONE
        } else {
            contextList_edit.visibility = View.VISIBLE
        }
        contextList_fav.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        contextList_sc.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url)
        }
        contextList_newTab.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        contextList_newTabOpen.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        contextList_delete.setOnClickListener {
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
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }

        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun show_contextMenu_list(title: String, url: String,
                                      adapterRecord: Adapter_Record, recordList: MutableList<Record>, location: Int,
                                      userName: String, userPW: String, _id: String, pass_creation: String?,
                                      gridItem: GridItem) {
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_menu_context_list, null)
        val db = BookmarkList(this)
        db.open()
        val contextList_edit = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_edit)
        val contextList_fav = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_fav)
        val contextList_sc = dialogView.findViewById<LinearLayout>(R.id.menu_contextLink_sc)
        val contextList_newTab = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTab)
        val contextList_newTabOpen = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_newTabOpen)
        val contextList_delete = dialogView.findViewById<LinearLayout>(R.id.menu_contextList_delete)
        if (overViewTab == getString(R.string.album_title_history)) {
            contextList_edit.visibility = View.GONE
        } else {
            contextList_edit.visibility = View.VISIBLE
        }
        contextList_fav.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.setFavorite(this, url)
        }
        contextList_sc.setOnClickListener {
            hideBottomSheetDialog()
            HelperUnit.createShortcut(this, title, url)
        }
        contextList_newTab.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, false)
            NinjaToast.show(this, getString(R.string.toast_new_tab_successful))
            hideBottomSheetDialog()
        }
        contextList_newTabOpen.setOnClickListener {
            addAlbum(getString(R.string.app_name), url, true)
            hideBottomSheetDialog()
            hideOverview()
        }
        contextList_delete.setOnClickListener {
            hideBottomSheetDialog()
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            val dialogView = View.inflate(this, R.layout.dialog_action, null)
            val textView = dialogView.findViewById<TextView>(R.id.dialog_text)
            textView.setText(R.string.toast_titleConfirm_delete)

            dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                when (overViewTab) {
                    getString(R.string.album_title_home) -> {
                        val action = RecordAction(this@BrowserActivity)
                        action.open(true)
                        action.deleteGridItem(gridItem)
                        action.close()
                        deleteFile(gridItem.filename)
                        open_startPage.performClick()
                        hideBottomSheetDialog()
                    }
                    getString(R.string.album_title_bookmarks) -> {
                        db.delete(_id.toInt())
                        initBookmarkList()
                        hideBottomSheetDialog()
                    }
                    getString(R.string.album_title_history) -> {
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
                }
            }
            dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener { hideBottomSheetDialog() }
            bottomSheetDialog?.setContentView(dialogView)
            bottomSheetDialog?.show()
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
        }

        contextList_edit.setOnClickListener {
            hideBottomSheetDialog()
            if (overViewTab == getString(R.string.album_title_home)) {
                bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                val dialogView = View.inflate(this, R.layout.dialog_edit_title, null)
                val editText = dialogView.findViewById<EditText>(R.id.dialog_edit)
                editText.setHint(R.string.dialog_title_hint)
                editText.setText(title)
                dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                    val text = editText.text.toString().trim { it <= ' ' }
                    if (text.isEmpty()) {
                        NinjaToast.show(this, getString(R.string.toast_input_empty))
                    } else {
                        val action = RecordAction(this@BrowserActivity)
                        action.open(true)
                        gridItem.title = text
                        action.updateGridItem(gridItem)
                        action.close()
                        hideKeyboard(this)
                        open_startPage.performClick()
                    }
                    hideBottomSheetDialog()
                }
                dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                    hideKeyboard(this)
                    hideBottomSheetDialog()
                }
                bottomSheetDialog?.setContentView(dialogView)
                bottomSheetDialog?.show()
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
            } else if (overViewTab == getString(R.string.album_title_bookmarks)) {
                try {
                    bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                    val dialogView = View.inflate(this, R.layout.dialog_edit_bookmark, null)
                    val pass_titleET = dialogView.findViewById<EditText>(R.id.pass_title)
                    val pass_userNameET = dialogView.findViewById<EditText>(R.id.pass_userName)
                    val pass_userPWET = dialogView.findViewById<EditText>(R.id.pass_userPW)
                    val pass_URLET = dialogView.findViewById<EditText>(R.id.pass_url)
                    val ib_icon = dialogView.findViewById<ImageView>(R.id.ib_icon)
                    val decrypted_userName = mahEncryptor.decode(userName)
                    val decrypted_userPW = mahEncryptor.decode(userPW)
                    pass_titleET.setText(title)
                    pass_userNameET.setText(decrypted_userName)
                    pass_userPWET.setText(decrypted_userPW)
                    pass_URLET.setText(url)
                    dialogView.findViewById<Button>(R.id.action_ok).setOnClickListener {
                        try {
                            val input_pass_title = pass_titleET.text.toString().trim { it <= ' ' }
                            val input_pass_url = pass_URLET.text.toString().trim { it <= ' ' }
                            val encrypted_userName = mahEncryptor.encode(pass_userNameET.text.toString().trim { it <= ' ' })
                            val encrypted_userPW = mahEncryptor.encode(pass_userPWET.text.toString().trim { it <= ' ' })
                            db.update(_id.toInt(), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url), HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), pass_creation)
                            initBookmarkList()
                            hideKeyboard(this)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            NinjaToast.show(this, R.string.toast_error)
                        }
                        hideBottomSheetDialog()
                    }
                    dialogView.findViewById<Button>(R.id.action_cancel).setOnClickListener {
                        hideKeyboard(this)
                        hideBottomSheetDialog()
                    }
                    HelperUnit.switchIcon(this, pass_creation, "pass_creation", ib_icon)
                    bottomSheetDialog?.setContentView(dialogView)
                    bottomSheetDialog?.show()
                    HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
                    ib_icon.setOnClickListener {
                        try {
                            val input_pass_title = pass_titleET.text.toString().trim { it <= ' ' }
                            val input_pass_url = pass_URLET.text.toString().trim { it <= ' ' }
                            val encrypted_userName = mahEncryptor.encode(pass_userNameET.text.toString().trim { it <= ' ' })
                            val encrypted_userPW = mahEncryptor.encode(pass_userPWET.text.toString().trim { it <= ' ' })
                            hideBottomSheetDialog()
                            hideKeyboard(this)
                            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
                            val dialogView = View.inflate(this, R.layout.dialog_edit_icon, null)
                            val grid = dialogView.findViewById<GridView>(R.id.grid_filter)
                            val itemAlbum_01 = GridItem_filter(sp.getString("icon_01", resources.getString(R.string.color_red)), "icon_01", resources.getDrawable(R.drawable.circle_red_big), "01")
                            val itemAlbum_02 = GridItem_filter(sp.getString("icon_02", resources.getString(R.string.color_pink)), "icon_02", resources.getDrawable(R.drawable.circle_pink_big), "02")
                            val itemAlbum_03 = GridItem_filter(sp.getString("icon_03", resources.getString(R.string.color_purple)), "icon_03", resources.getDrawable(R.drawable.circle_purple_big), "03")
                            val itemAlbum_04 = GridItem_filter(sp.getString("icon_04", resources.getString(R.string.color_blue)), "icon_04", resources.getDrawable(R.drawable.circle_blue_big), "04")
                            val itemAlbum_05 = GridItem_filter(sp.getString("icon_05", resources.getString(R.string.color_teal)), "icon_05", resources.getDrawable(R.drawable.circle_teal_big), "05")
                            val itemAlbum_06 = GridItem_filter(sp.getString("icon_06", resources.getString(R.string.color_green)), "icon_06", resources.getDrawable(R.drawable.circle_green_big), "06")
                            val itemAlbum_07 = GridItem_filter(sp.getString("icon_07", resources.getString(R.string.color_lime)), "icon_07", resources.getDrawable(R.drawable.circle_lime_big), "07")
                            val itemAlbum_08 = GridItem_filter(sp.getString("icon_08", resources.getString(R.string.color_yellow)), "icon_08", resources.getDrawable(R.drawable.circle_yellow_big), "08")
                            val itemAlbum_09 = GridItem_filter(sp.getString("icon_09", resources.getString(R.string.color_orange)), "icon_09", resources.getDrawable(R.drawable.circle_orange_big), "09")
                            val itemAlbum_10 = GridItem_filter(sp.getString("icon_10", resources.getString(R.string.color_brown)), "icon_10", resources.getDrawable(R.drawable.circle_brown_big), "10")
                            val itemAlbum_11 = GridItem_filter(sp.getString("icon_11", resources.getString(R.string.color_grey)), "icon_11", resources.getDrawable(R.drawable.circle_grey_big), "11")
                            val gridList = mutableListOf<GridItem_filter>()
                            if (sp.getBoolean("filter_01", true)) {
                                gridList.add(itemAlbum_01)
                            }
                            if (sp.getBoolean("filter_02", true)) {
                                gridList.add(itemAlbum_02)
                            }
                            if (sp.getBoolean("filter_03", true)) {
                                gridList.add(itemAlbum_03)
                            }
                            if (sp.getBoolean("filter_04", true)) {
                                gridList.add(itemAlbum_04)
                            }
                            if (sp.getBoolean("filter_05", true)) {
                                gridList.add(itemAlbum_05)
                            }
                            if (sp.getBoolean("filter_06", true)) {
                                gridList.add(itemAlbum_06)
                            }
                            if (sp.getBoolean("filter_07", true)) {
                                gridList.add(itemAlbum_07)
                            }
                            if (sp.getBoolean("filter_08", true)) {
                                gridList.add(itemAlbum_08)
                            }
                            if (sp.getBoolean("filter_09", true)) {
                                gridList.add(itemAlbum_09)
                            }
                            if (sp.getBoolean("filter_10", true)) {
                                gridList.add(itemAlbum_10)
                            }
                            if (sp.getBoolean("filter_11", true)) {
                                gridList.add(itemAlbum_11)
                            }
                            val gridAdapter = GridAdapter_filter(this, gridList)
                            grid.adapter = gridAdapter
                            gridAdapter.notifyDataSetChanged()
                            grid.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                                db.update(_id.toInt(), HelperUnit.secString(input_pass_title), HelperUnit.secString(input_pass_url), HelperUnit.secString(encrypted_userName), HelperUnit.secString(encrypted_userPW), gridList[position].ordinal)
                                initBookmarkList()
                                hideBottomSheetDialog()
                            }
                            bottomSheetDialog?.setContentView(dialogView)
                            bottomSheetDialog?.show()
                            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            hideBottomSheetDialog()
                            NinjaToast.show(this, R.string.toast_error)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(this, R.string.toast_error)
                }
            }
        }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun show_dialogFilter() {
        hideBottomSheetDialog()
        open_bookmark.performClick()
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val dialogView = View.inflate(this, R.layout.dialog_edit_icon, null)
        val grid = dialogView.findViewById<GridView>(R.id.grid_filter)
        val itemAlbum_01 = GridItem_filter(sp.getString("icon_01", resources.getString(R.string.color_red)), "icon_01", resources.getDrawable(R.drawable.circle_red_big), "01")
        val itemAlbum_02 = GridItem_filter(sp.getString("icon_02", resources.getString(R.string.color_pink)), "icon_02", resources.getDrawable(R.drawable.circle_pink_big), "02")
        val itemAlbum_03 = GridItem_filter(sp.getString("icon_03", resources.getString(R.string.color_purple)), "icon_03", resources.getDrawable(R.drawable.circle_purple_big), "03")
        val itemAlbum_04 = GridItem_filter(sp.getString("icon_04", resources.getString(R.string.color_blue)), "icon_04", resources.getDrawable(R.drawable.circle_blue_big), "04")
        val itemAlbum_05 = GridItem_filter(sp.getString("icon_05", resources.getString(R.string.color_teal)), "icon_05", resources.getDrawable(R.drawable.circle_teal_big), "05")
        val itemAlbum_06 = GridItem_filter(sp.getString("icon_06", resources.getString(R.string.color_green)), "icon_06", resources.getDrawable(R.drawable.circle_green_big), "06")
        val itemAlbum_07 = GridItem_filter(sp.getString("icon_07", resources.getString(R.string.color_lime)), "icon_07", resources.getDrawable(R.drawable.circle_lime_big), "07")
        val itemAlbum_08 = GridItem_filter(sp.getString("icon_08", resources.getString(R.string.color_yellow)), "icon_08", resources.getDrawable(R.drawable.circle_yellow_big), "08")
        val itemAlbum_09 = GridItem_filter(sp.getString("icon_09", resources.getString(R.string.color_orange)), "icon_09", resources.getDrawable(R.drawable.circle_orange_big), "09")
        val itemAlbum_10 = GridItem_filter(sp.getString("icon_10", resources.getString(R.string.color_brown)), "icon_10", resources.getDrawable(R.drawable.circle_brown_big), "10")
        val itemAlbum_11 = GridItem_filter(sp.getString("icon_11", resources.getString(R.string.color_grey)), "icon_11", resources.getDrawable(R.drawable.circle_grey_big), "11")
        val gridList: MutableList<GridItem_filter> = mutableListOf()
        if (sp.getBoolean("filter_01", true)) {
            gridList.add(itemAlbum_01)
        }
        if (sp.getBoolean("filter_02", true)) {
            gridList.add(itemAlbum_02)
        }
        if (sp.getBoolean("filter_03", true)) {
            gridList.add(itemAlbum_03)
        }
        if (sp.getBoolean("filter_04", true)) {
            gridList.add(itemAlbum_04)
        }
        if (sp.getBoolean("filter_05", true)) {
            gridList.add(itemAlbum_05)
        }
        if (sp.getBoolean("filter_06", true)) {
            gridList.add(itemAlbum_06)
        }
        if (sp.getBoolean("filter_07", true)) {
            gridList.add(itemAlbum_07)
        }
        if (sp.getBoolean("filter_08", true)) {
            gridList.add(itemAlbum_08)
        }
        if (sp.getBoolean("filter_09", true)) {
            gridList.add(itemAlbum_09)
        }
        if (sp.getBoolean("filter_10", true)) {
            gridList.add(itemAlbum_10)
        }
        if (sp.getBoolean("filter_11", true)) {
            gridList.add(itemAlbum_11)
        }
        val gridAdapter = GridAdapter_filter(this, gridList)
        grid.adapter = gridAdapter
        gridAdapter.notifyDataSetChanged()
        grid.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            sp.edit().putString("filter_bookmarks", gridList[position].ordinal).apply()
            initBookmarkList()
            hideBottomSheetDialog()
        }
        bottomSheetDialog?.setContentView(dialogView)
        bottomSheetDialog?.show()
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun setCustomFullscreen(fullscreen: Boolean) {
        val decorView = window.decorView
        if (fullscreen) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
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
            mActionMode = mode
            val menu = mode.menu
            var googleTranslateItem: MenuItem? = null
            val toBeRemovedList: MutableList<MenuItem> = mutableListOf()
            for (index in 1 until menu.size()) {
                val item = menu.getItem(index)
                //if (item.intent?.component?.packageName == "com.google.android.apps.translate") {
                if (item.intent?.component?.packageName == "info.plateaukao.naverdict") {
                    googleTranslateItem = item
                    break
                }
                toBeRemovedList.add(item)
            }
            for (item in toBeRemovedList) {
                menu.removeItem(item.itemId)
            }
        }
        super.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        mActionMode = null
        super.onActionModeFinished(mode)
    }

    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            Objects.requireNonNull(imm).hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}