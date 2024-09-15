package info.plateaukao.einkbro.unit

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.CustomFontInfo
import info.plateaukao.einkbro.unit.HelperUnit.needGrantStoragePermission
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.NinjaToast.showShort
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern

object BrowserUnit : KoinComponent {
    const val PROGRESS_MAX = 100
    const val SUFFIX_PNG = ".png"
    private const val SUFFIX_TXT = ".txt"
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_IMAGE = "image/png"
    private const val SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q="
    private const val SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q="
    private const val SEARCH_ENGINE_STARTPAGE = "https://startpage.com/do/search?query="
    private const val SEARCH_ENGINE_BING = "http://www.bing.com/search?q="
    private const val SEARCH_ENGINE_BAIDU = "https://www.baidu.com/s?wd="
    private const val SEARCH_ENGINE_QWANT = "https://www.qwant.com/?q="
    private const val SEARCH_ENGINE_ECOSIA = "https://www.ecosia.org/search?q="
    private const val SEARCH_ENGINE_YANDEX = "https://yandex.com/search/?text="
    private const val SEARCH_ENGINE_STARTPAGE_DE =
        "https://startpage.com/do/search?lui=deu&language=deutsch&query="
    private const val SEARCH_ENGINE_SEARX = "https://searx.me/?q="
    const val URL_ENCODING = "utf-8"
    const val URL_ABOUT_BLANK = "about:blank"
    const val URL_SCHEME_ABOUT = "about:"
    const val URL_SCHEME_MAIL_TO = "mailto:"
    private const val URL_SCHEME_FILE = "file://"
    private const val URL_SCHEME_HTTPS = "https://"
    private const val URL_SCHEME_HTTP = "http://"
    const val URL_SCHEME_INTENT = "intent://"
    private const val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    private const val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    val UA_DESKTOP_PREFIX = "Mozilla/5.0 (X11; Linux " + System.getProperty("os.arch") + ")"
    val UA_MOBILE_PREFIX = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ")"

    private val config: ConfigManager by inject()
    val cookie: Cookie by inject()

    private val neatUrlConfigs: List<String> = parseNeatUrlConfigs()

    private fun dataUrlToMimeType(dataUrl: String): String =
        dataUrl.substring(dataUrl.indexOf("/") + 1, dataUrl.indexOf(";"))

    @RequiresApi(Build.VERSION_CODES.O)
    fun dataUrlToStream(dataUrl: String): InputStream {
        val data = dataUrl.split(",")
        val base64 = data[1]
        val imageBytes = Base64.getDecoder().decode(base64)
        return ByteArrayInputStream(imageBytes)
    }

    fun createDownloadReceiver(activity: Activity): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (activity.isFinishing || downloadFileId == -1L) return

                val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val mostRecentDownload: Uri =
                    downloadManager.getUriForDownloadedFile(downloadFileId) ?: return
                val mimeType: String = downloadManager.getMimeTypeForDownloadedFile(downloadFileId)
                val fileIntent = Intent(ACTION_VIEW).apply {
                    setDataAndType(mostRecentDownload, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                downloadFileId = -1L
                DialogManager(activity).showOkCancelDialog(
                    messageResId = R.string.toast_downloadComplete,
                    okAction = {
                        try {
                            activity.startActivity(fileIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            NinjaToast.show(activity, R.string.toast_error)
                        }
                    }
                )
            }
        }
    }

    fun openDownloadFolder(activity: Activity) {
        val uri = Uri.parse(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString()
        )
        val intent =
            Intent(Intent.ACTION_GET_CONTENT).apply { setDataAndType(uri, "resource/folder") }
        activity.startActivity(
            Intent.createChooser(intent, activity.getString(R.string.dialog_title_download))
        )
    }

    fun getWebViewLinkImageUrl(webView: WebView, message: Message): String {
        val hitTestResult = webView.hitTestResult
        return hitTestResult.extra ?: message.data.getString("src").orEmpty()
    }

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
        GlobalScope.launch(Dispatchers.IO) {
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
    fun isURL(url: String?): Boolean {
        var url = url ?: return false
        url = url.toLowerCase(Locale.getDefault())
        if (url.startsWith(URL_ABOUT_BLANK)
            || url.startsWith(URL_SCHEME_MAIL_TO)
            || url.startsWith(URL_SCHEME_FILE)
        ) {
            return true
        }
        val regex = ("^((ftp|http|https|intent)?://)" // support scheme
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" // ftp的user@
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL -> 199.194.52.184
                + "|" // 允许IP和DOMAIN（域名）
                + "(.)*" // 域名 -> www.
                // + "([0-9a-z_!~*'()-]+\\.)*"                               // 域名 -> www.
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
                + "[a-z]{2,6})" // first level domain -> .com or .museum
                + "(:[0-9]{1,4})?" // 端口 -> :80
                + "((/?)|" // a slash isn't required if there is no file name
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$")
        val pattern = Pattern.compile(regex)
        val isMatch = pattern.matcher(url).matches()
        return if (isMatch) true else try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme
            scheme == "ftp" || scheme == "http" || scheme == "https" || scheme == "intent"
        } catch (exception: Exception) {
            false
        }
    }

    fun queryWrapper(context: Context, query: String): String {
        // Use prefix and suffix to process some special links
        var query = query
        val temp = query.toLowerCase(Locale.getDefault())
        if (temp.contains(URL_PREFIX_GOOGLE_PLAY) && temp.contains(URL_SUFFIX_GOOGLE_PLAY)) {
            val start = temp.indexOf(URL_PREFIX_GOOGLE_PLAY) + URL_PREFIX_GOOGLE_PLAY.length
            val end = temp.indexOf(URL_SUFFIX_GOOGLE_PLAY)
            query = query.substring(start, end)
        }

        // remove prefix non-url part
        if (config.shouldTrimInputUrl) {
            var foundIndex = query.indexOf(URL_SCHEME_HTTPS)
            if (foundIndex > 0) {
                query = query.substring(foundIndex)
            }
            foundIndex = query.indexOf(URL_SCHEME_HTTP)
            if (foundIndex > 0) {
                query = query.substring(foundIndex)
            }
        }

        if (isURL(query)) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query
            }
            if (!query.contains("://")) {
                query = URL_SCHEME_HTTPS + query
            }
            return query.replace(" ", "+")
        }

        try {
            query = URLEncoder.encode(query, URL_ENCODING)
        } catch (u: UnsupportedEncodingException) {
            Log.w("browser", "Unsupported Encoding Exception")
        }
        return when (config.searchEngine.toInt()) {
            0 -> SEARCH_ENGINE_STARTPAGE + query
            1 -> SEARCH_ENGINE_STARTPAGE_DE + query
            2 -> SEARCH_ENGINE_BAIDU + query
            3 -> SEARCH_ENGINE_BING + query
            6 -> SEARCH_ENGINE_SEARX + query
            7 -> SEARCH_ENGINE_QWANT + query
            8 -> SEARCH_ENGINE_ECOSIA + query
            9 -> config.searchEngineUrl + query
            10 -> SEARCH_ENGINE_YANDEX + query
            5 -> SEARCH_ENGINE_GOOGLE + query
            4 -> SEARCH_ENGINE_DUCKDUCKGO + query
            else -> SEARCH_ENGINE_GOOGLE + query
        }
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

    @JvmStatic
    fun download(context: Context, url: String, contentDisposition: String, mimeType: String) {
        val activity = context as Activity
        if (needGrantStoragePermission(activity)) {
            return
        }

        var filename = Uri.decode(guessFilename(url, contentDisposition, mimeType))
        val title = context.getString(R.string.dialog_title_download)

        (activity as LifecycleOwner).lifecycleScope.launch {
            val modifiedFilename = TextInputDialog(
                context,
                "",
                title,
                filename
            ).show()

            modifiedFilename?.let { internalDownload(activity, url, mimeType, it) }
        }
    }

    var downloadFileId = -1L
    private fun internalDownload(
        activity: Activity,
        url: String,
        mimeType: String,
        filename: String
    ) {
        val cookie = CookieManager.getInstance().getCookie(url)
        if (Uri.parse(url).host == null) {
            showShort(activity, R.string.error_download_link_invalid)
            return
        }
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            if (cookie != null) addRequestHeader("Cookie", cookie)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(filename)
            setMimeType(mimeType)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }
        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadFileId = manager.enqueue(request)
        showShort(activity, R.string.toast_start_download)
    }

    @JvmStatic
    fun clearCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (exception: Exception) {
            Log.w("browser", "Error clearing cache")
        }
    }

    @JvmStatic
    fun clearCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        cookieManager.removeAllCookies { }
    }

    @JvmStatic
    fun clearHistory(context: Context) {
        with(RecordDb(context)) {
            clearHistory()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                Objects.requireNonNull(shortcutManager).removeAllDynamicShortcuts()
            }
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

    fun guessFilename(url: String, contentDisposition: String, mimeType: String): String {
        val prefix = "filename*=utf-8''"
        val decodedContentDescription = URLDecoder.decode(contentDisposition)
        if (decodedContentDescription.toLowerCase().contains(prefix)) {
            val index = decodedContentDescription.toLowerCase().indexOf(prefix)
            return decodedContentDescription.substring(index + prefix.length)
        }
        val anotherPrefix = "filename=\""
        if (contentDisposition.contains(anotherPrefix)) {
            val index = contentDisposition.indexOf(anotherPrefix)
            return contentDisposition.substring(
                index + anotherPrefix.length,
                contentDisposition.length - 1
            )
        }
        val anotherPrefix2 = "filename="
        if (contentDisposition.contains(anotherPrefix2)) {
            val index = contentDisposition.indexOf(anotherPrefix2)
            return contentDisposition.substring(
                index + anotherPrefix2.length,
                contentDisposition.length
            )
        }

        var filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        if (filename.endsWith(".bin")) {
            var decodedUrl: String? = Uri.decode(url)
            if (decodedUrl != null) {
                val queryIndex = decodedUrl.indexOf('?')
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex)
                }
                if (!decodedUrl.endsWith("/")) {
                    val index = decodedUrl.lastIndexOf('/') + 1
                    if (index > 0) {
                        filename = decodedUrl.substring(index)
                        if (filename.indexOf('.') < 0) {
                            filename += "bin"
                        }
                    }
                }
            }
        }
        return filename
    }


    fun openFontFilePicker(resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        resultLauncher.launch(intent)
    }

    fun openBookmarkFilePicker(resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        resultLauncher.launch(intent)
    }

    fun createBookmarkFilePicker(resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.putExtra(Intent.EXTRA_TITLE, "einkbro_bookmarks.json")
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

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
        val fileFormat = dataUrlToMimeType(url)
        tempImageInputStream = dataUrlToStream(url)
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

        val file = File(uri.path)
        if (isReaderMode) {
            config.readerCustomFontInfo = CustomFontInfo(file.name, uri.toString())
        } else {
            config.customFontInfo = CustomFontInfo(file.name, uri.toString())
        }
    }

    fun stripUrlQuery(url: String): String {
        if (!config.shouldPruneQueryParameters) return url

        try {
            var strippedCount = 0
            val uri = Uri.parse(url)
            if (uri.authority == null) return url

            val params = uri.queryParameterNames
            if (params.isEmpty()) return url

            val uriBuilder = uri.buildUpon().clearQuery()
            for (param in params) {
                if (!matchNeatUrlConfig(uri.host.orEmpty(), param)) {
                    uriBuilder.appendQueryParameter(param, uri.getQueryParameter(param))
                } else {
                    strippedCount++
                }
            }

            // only return the stripped url if we stripped something
            // to fix the only key but no value scenario
            return if (strippedCount > 0) {
                Log.d("strippedCount", "$strippedCount")
                uriBuilder.build().toString()
            } else {
                url
            }
        } catch (e: Exception) {
            return url
        }
    }

    private fun matchNeatUrlConfig(host: String, param: String): Boolean {
        neatUrlConfigs.forEach { paramConfig ->
            // handle host part
            if (paramConfig.contains("@")) {
                val paramConfigs = paramConfig.split("@")
                if (paramConfigs.size == 2) {
                    val paramValue = paramConfigs[0]
                    val hostValue = paramConfigs[1]
                    val modifiedHost = host.replace("www.", "")
                    if (matchStarString(hostValue, modifiedHost) &&
                        matchStarString(paramValue, param)
                    ) {
                        return true
                    }
                }
            }

            // handle normal param part
            if (matchStarString(paramConfig, param)) return true
        }
        return false
    }
    
    fun loadRecentlyUsedBookmarks(webView: NinjaWebView) {
        val html = getRecentBookmarksContent()
        if (html.isNotBlank()) {
            webView.loadDataWithBaseURL(
                null,
                getRecentBookmarksContent(),
                "text/html",
                "utf-8",
                null
            )
            webView.albumTitle = webView.context.getString(R.string.recently_used_bookmarks)
        }
    }

    private fun matchStarString(config: String, param: String): Boolean {
        if (config.endsWith("*")) {
            if (param.startsWith(config.substring(0, config.length - 1))) return true
        } else if (config.startsWith("*")) {
            if (param.endsWith(config.substring(1, config.length))) return true
        } else if (config == param) {
            return true
        }
        return false
    }

    internal data class NeatUrlConfig(val name: String, val params: List<String>)

    @Suppress("UNCHECKED_CAST")
    private fun parseNeatUrlConfigs(): List<String> {
        val configArray = JSONObject(Constants.NEAT_URL_DATA)
            .getJSONArray("categories")

        return (0 until configArray.length()).map { index ->
            val config = configArray.getJSONObject(index)
            val paramsArray = config.getJSONArray("params")
            (0 until paramsArray.length()).map { s -> paramsArray.getString(s) }
        }.flatten()
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

    fun getRecentBookmarksContent(): String {
        if (config.recentBookmarks.isEmpty()) return ""
        val content = config.recentBookmarks.joinToString(separator = " ") {
            """<button><a href="${it.url}">${it.name}</a></button> """
        }
        return """
            <html>
            <head>
                <style>
                body {
                flex-wrap: wrap;
                }
                a{
                  text-decoration:none;  
                }
                button {
                  border: 2px solid black;
                  background-color: white;
                  color: black;
                  padding: 14px 28px;
                  font-size: 16px;
                  cursor: pointer;
                  border-color: #2196F3;
                  color: dodgerblue;
                  border-radius: 12px;
                }
                button:hover {
                  background: #2196F3;
                  color: white;
                }
                </style>
            </head>
            <body>
                <center> $content </center>
            </body>
            </html>
        """.trimIndent()
    }

    suspend fun getResourceAndMimetypeFromUrl(url: String, timeout: Int = 0): Pair<ByteArray, String> {
        var byteArray: ByteArray = "".toByteArray()
        var mimeType = ""
        withContext(Dispatchers.IO) {
            try {
                val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                connection.addRequestProperty("User-Agent", "Mozilla/4.76")
                if (timeout > 0) connection.connectTimeout = timeout
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    if (isRedirect(connection.responseCode)) {
                        val redirectUrl = connection.getHeaderField("Location")
                        byteArray = getResourceFromUrl(redirectUrl, timeout)
                    }
                } else {
                    mimeType = connection.contentType
                    byteArray = connection.inputStream.readBytes()
                    connection.inputStream.close()
                }
            } catch (e: IOException) {
                Log.w("browser", "Failed getting resource: $e")
                e.printStackTrace()
            }
        }
        return Pair(byteArray, mimeType)
    }

    suspend fun getResourceFromUrl(url: String, timeout: Int = 0): ByteArray {
        return getResourceAndMimetypeFromUrl(url, timeout).first
    }

    private fun isRedirect(responseCode: Int): Boolean = responseCode in 301..399
}