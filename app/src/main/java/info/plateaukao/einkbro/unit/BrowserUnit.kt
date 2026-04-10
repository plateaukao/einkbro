package info.plateaukao.einkbro.unit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebView
import android.webkit.WebView.HitTestResult.ANCHOR_TYPE
import android.webkit.WebView.HitTestResult.IMAGE_ANCHOR_TYPE
import android.webkit.WebView.HitTestResult.IMAGE_TYPE
import android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE
import android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.CustomFontInfo
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.ShortcutEditDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Objects
import kotlin.system.exitProcess

object BrowserUnit : KoinComponent {
    const val PROGRESS_MAX = 100
    const val SUFFIX_PNG = ".png"
    private const val SUFFIX_TXT = ".txt"
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_IMAGE = "image/png"
    const val URL_ENCODING = "utf-8"
    const val URL_ABOUT_BLANK = "about:blank"
    const val URL_SCHEME_ABOUT = "about:"
    const val URL_SCHEME_MAIL_TO = "mailto:"
    const val URL_SCHEME_INTENT = "intent://"
    val UA_DESKTOP_PREFIX = "Mozilla/5.0 (X11; Linux " + System.getProperty("os.arch") + ")"
    val UA_MOBILE_PREFIX = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ")"

    private val config: ConfigManager by inject()
    val cookie: Cookie by inject()
    private val coroutineScope: CoroutineScope by inject()

    // --- Forwarding stubs for DownloadHelper (preserves existing call sites) ---

    @JvmStatic
    fun download(context: Context, url: String, contentDisposition: String, mimeType: String) =
        DownloadHelper.download(context, url, contentDisposition, mimeType)

    var downloadFileId: Long
        get() = DownloadHelper.downloadFileId
        set(value) { DownloadHelper.downloadFileId = value }

    fun createDownloadReceiver(activity: Activity): BroadcastReceiver =
        DownloadHelper.createDownloadReceiver(activity)

    fun openDownloadFolder(activity: Activity) =
        DownloadHelper.openDownloadFolder(activity)

    // --- Forwarding stubs for UrlHelper (preserves existing call sites) ---

    @JvmStatic
    fun isURL(url: String?): Boolean = UrlHelper.isURL(url)

    fun queryWrapper(context: Context, query: String): String =
        UrlHelper.queryWrapper(context, query)

    fun stripUrlQuery(url: String): String = UrlHelper.stripUrlQuery(url)

    @RequiresApi(Build.VERSION_CODES.O)
    fun dataUrlToStream(dataUrl: String): InputStream = UrlHelper.dataUrlToStream(dataUrl)

    // --- Forwarding stubs for BookmarkRenderer (preserves existing call sites) ---

    fun loadRecentlyUsedBookmarks(webView: EBWebView) =
        BookmarkRenderer.loadRecentlyUsedBookmarks(webView)

    fun getRecentBookmarksContent(context: Context): String =
        BookmarkRenderer.getRecentBookmarksContent(context)

    suspend fun getResourceAndMimetypeFromUrl(url: String, timeout: Int = 0): Pair<ByteArray, String> =
        BookmarkRenderer.getResourceAndMimetypeFromUrl(url, timeout)

    suspend fun getResourceFromUrl(url: String, timeout: Int = 0): ByteArray =
        BookmarkRenderer.getResourceFromUrl(url, timeout)

    // --- Remaining functions ---

    fun getWebViewLinkImageUrl(webView: WebView, message: Message): String {
        val hitTestResult = webView.hitTestResult
        return hitTestResult.extra ?: message.data.getString("src").orEmpty()
    }

    @Suppress("DEPRECATION")
    fun getWebViewLinkUrl(webView: WebView, message: Message): String {
        val hitTestResult = webView.hitTestResult

        if (!listOf(
                IMAGE_TYPE,
                IMAGE_ANCHOR_TYPE,
                SRC_ANCHOR_TYPE,
                SRC_IMAGE_ANCHOR_TYPE,
                ANCHOR_TYPE
            )
                .contains(hitTestResult.type)
        ) return ""

        val linkUrl = message.data.getString("url")
        val imgUrl = message.data.getString("src")
        return linkUrl ?: imgUrl ?: return ""
    }

    fun getWebViewLinkTitle(webView: WebView, action: (String) -> Unit) {
        val newMessage = Message().apply {
            target = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    val titleText = msg.data.getString("title")?.replace("\n", "")?.trim().orEmpty()
                    action(titleText)
                }
            }
        }
        webView.requestFocusNodeHref(newMessage)
    }

    private fun saveImage(
        context: Context,
        inputStream: InputStream,
        uri: Uri,
        postAction: (Uri) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)
                    .use { it?.write(inputStream.readBytes()) }
                withContext(Dispatchers.Main) { postAction(uri) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createNaverDictWebView(context: Context): WebView {
        return WebView(ContextThemeWrapper(context, R.style.AppTheme)).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.let {
                        view.evaluateJavascript(
                            """
                            document.getElementById("bookmark").remove();
                            document.getElementsByClassName("gnb_wrap")[0].remove();
                            document.getElementsByClassName("search_wrap")[0].remove();
                            document.getElementsByClassName("search_area")[0].remove();
                        """.trimIndent(), null
                        )
                    }
                    view?.postDelayed(
                        {
                            view.evaluateJavascript(
                                """
                            document.getElementById("_id_mobile_ad").remove();
                        """.trimIndent(), null
                            )

                        },
                        1000
                    )
                }
            }
        }
    }

    @JvmStatic
    fun clearCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (exception: Exception) {
            android.util.Log.w("browser", "Error clearing cache")
        }
    }

    @JvmStatic
    fun clearCookie() {
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.flush()
        cookieManager.removeAllCookies { }
    }

    @JvmStatic
    fun clearHistory(context: Context) {
        val recordRepository: RecordRepository by inject()
        recordRepository.clearHistory()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            Objects.requireNonNull(shortcutManager).removeAllDynamicShortcuts()
        }
    }

    @JvmStatic
    fun clearIndexedDB(context: Context) {
        val data = Environment.getDataDirectory()
        val indexedDB = "//data//" + context.packageName + "//app_webview//" + "//IndexedDB"
        val localStorage = "//data//" + context.packageName + "//app_webview//" + "//Local Storage"
        val indexedDB_dir = File(data, indexedDB)
        val localStorage_dir = File(data, localStorage)
        deleteDir(indexedDB_dir)
        deleteDir(localStorage_dir)
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (aChildren in Objects.requireNonNull(children)) {
                val success = deleteDir(File(dir, aChildren))
                if (!success) {
                    return false
                }
            }
        }
        return dir != null && dir.delete()
    }

    fun bitmap2File(context: Context, bitmap: Bitmap, filename: String?): Boolean {
        try {
            val fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun file2Bitmap(context: Context, filename: String?): Bitmap? {
        return try {
            val fileInputStream = context.openFileInput(filename)
            BitmapFactory.decodeStream(fileInputStream)
        } catch (e: Exception) {
            null
        }
    }

    fun openFontFilePicker(resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        resultLauncher.launch(intent)
    }


    fun createFilePicker(resultLauncher: ActivityResultLauncher<Intent>, title: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.putExtra(Intent.EXTRA_TITLE, title)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        resultLauncher.launch(intent)
    }

    private var tempImageInputStream: InputStream? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveImageFromUrl(url: String, resultLauncher: ActivityResultLauncher<Intent>) {
        val fileFormat = UrlHelper.dataUrlToMimeType(url)
        tempImageInputStream = UrlHelper.dataUrlToStream(url)
        val mimeType = when (fileFormat.lowercase()) {
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, "download.$fileFormat")
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        resultLauncher.launch(intent)
    }


    fun handleFontSelectionResult(
        context: Context,
        activityResult: ActivityResult,
        isReaderMode: Boolean = false
    ) {
        if (activityResult.data == null || activityResult.resultCode != Activity.RESULT_OK) return
        val uri = activityResult.data?.data ?: return

        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver?.takePersistableUriPermission(uri, takeFlags)

        val file = File(uri.path ?: return)
        if (isReaderMode) {
            config.readerCustomFontInfo = CustomFontInfo(file.name, uri.toString())
        } else {
            config.customFontInfo = CustomFontInfo(file.name, uri.toString())
        }
    }

    fun handleSaveImageFilePickerResult(
        activity: ComponentActivity,
        activityResult: ActivityResult,
        postAction: (Uri) -> Unit
    ) {
        if (activityResult.data == null || activityResult.resultCode != Activity.RESULT_OK) return
        val uri = activityResult.data?.data ?: return
        // SAVE IMAGE
        tempImageInputStream?.let { saveImage(activity, it, uri, postAction) }
        tempImageInputStream?.close()
        tempImageInputStream = null
    }

    fun createShortcut(activity: FragmentActivity, ebWebView: EBWebView) {
        val currentUrl = ebWebView.url ?: return
        ShortcutEditDialog(
            activity,
            HelperUnit.secString(ebWebView.title),
            currentUrl,
            ebWebView.favicon,
            {
                ViewUnit.hideKeyboard(activity)
                EBToast.show(activity, R.string.toast_edit_successful)
            },
            { ViewUnit.hideKeyboard(activity) }
        ).show()
    }

    fun restartApp(activity: Activity) {
        finishAffinity(activity) // Finishes all activities.
        activity.startActivity(activity.packageManager.getLaunchIntentForPackage(activity.packageName))    // Start the launch activity
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
        exitProcess(0)
    }
}
