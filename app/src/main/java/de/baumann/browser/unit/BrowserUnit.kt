package de.baumann.browser.unit

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.browser.Javascript
import de.baumann.browser.database.RecordDb
import de.baumann.browser.unit.HelperUnit.needGrantStoragePermission
import de.baumann.browser.view.NinjaToast.showShort
import de.baumann.browser.view.dialog.TextInputDialog
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern

object BrowserUnit: KoinComponent {
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
    const val UA_DESKTOP =
        "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/44.0.2403.155 Safari/537.36"

    private val sp: SharedPreferences by inject()
    private val adBlock: AdBlock by inject()
    private val js: Javascript by inject()
    val cookie: Cookie by inject()

    @JvmStatic
    fun isURL(url: String?): Boolean {
        var url = url
        if (url == null) {
            return false
        }
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

        // -- start: remove prefix non-url part
        var foundIndex = query.indexOf(URL_SCHEME_HTTPS)
        if (foundIndex > 0) {
            query = query.substring(foundIndex)
        }
        foundIndex = query.indexOf(URL_SCHEME_HTTP)
        if (foundIndex > 0) {
            query = query.substring(foundIndex)
        }
        // -- end: remove prefix non-url part

        if (isURL(query) && !query.contains(" ")) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query
            }
            if (!query.contains("://")) {
                query = URL_SCHEME_HTTPS + query
            }
            return query
        }

        try {
            query = URLEncoder.encode(query, URL_ENCODING)
        } catch (u: UnsupportedEncodingException) {
            Log.w("browser", "Unsupported Encoding Exception")
        }
        val custom = sp.getString("sp_search_engine_custom", SEARCH_ENGINE_GOOGLE)
        val i = Integer.valueOf(
            Objects.requireNonNull(
                sp.getString(
                    context.getString(R.string.sp_search_engine),
                    "5"
                )
            )
        )
        return when (i) {
            0 -> SEARCH_ENGINE_STARTPAGE + query
            1 -> SEARCH_ENGINE_STARTPAGE_DE + query
            2 -> SEARCH_ENGINE_BAIDU + query
            3 -> SEARCH_ENGINE_BING + query
            6 -> SEARCH_ENGINE_SEARX + query
            7 -> SEARCH_ENGINE_QWANT + query
            8 -> custom + query
            9 -> SEARCH_ENGINE_ECOSIA + query
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

        var filename = guessFilename(url, contentDisposition, mimeType)
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

    private fun internalDownload(activity: Activity, url: String, mimeType: String, filename: String) {
        val cookie = CookieManager.getInstance().getCookie(url)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("Cookie", cookie)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(filename)
            setMimeType(mimeType)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }
        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        showShort(activity, R.string.toast_start_download)
    }

    @JvmStatic
    fun exportWhitelist(context: Context, i: Int): String? {
        val action = RecordDb(context)
        val list: List<String>
        val filename: String
        action.open(false)
        when (i) {
            0 -> {
                list = action.listDomains(RecordUnit.TABLE_WHITELIST)
                filename = context.getString(R.string.export_whitelistAdBlock)
            }
            1 -> {
                list = action.listDomains(RecordUnit.TABLE_JAVASCRIPT)
                filename = context.getString(R.string.export_whitelistJS)
            }
            else -> {
                list = action.listDomains(RecordUnit.TABLE_COOKIE)
                filename = context.getString(R.string.export_whitelistCookie)
            }
        }
        action.close()
        val file =
            File(context.getExternalFilesDir(null), "browser_backup//$filename$SUFFIX_TXT")
        return try {
            val writer = BufferedWriter(FileWriter(file, false))
            for (domain in list) {
                writer.write(domain)
                writer.newLine()
            }
            writer.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun importWhitelist(context: Context, i: Int): Int {
        var count = 0
        try {
            val filename: String
            when (i) {
                0 -> filename = context.getString(R.string.export_whitelistAdBlock)
                1 -> filename = context.getString(R.string.export_whitelistJS)
                else -> filename = context.getString(R.string.export_whitelistAdBlock)
            }
            val file =
                File(context.getExternalFilesDir(null), "browser_backup//$filename$SUFFIX_TXT")
            val action = RecordDb(context)
            action.open(true)
            val reader = BufferedReader(FileReader(file))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                when (i) {
                    0 -> if (!action.checkDomain(line, RecordUnit.TABLE_WHITELIST)) {
                        adBlock.addDomain(line)
                        count++
                    }
                    1 -> if (!action.checkDomain(line, RecordUnit.TABLE_JAVASCRIPT)) {
                        js.addDomain(line)
                        count++
                    }
                    else -> if (!action.checkDomain(line, RecordUnit.COLUMN_DOMAIN)) {
                        cookie.addDomain(line)
                        count++
                    }
                }
            }
            reader.close()
            action.close()
        } catch (e: Exception) {
            Log.w("browser", "Error reading file", e)
        }
        return count
    }

    fun clearHome(context: Context?) {
        val action = RecordDb(context)
        action.open(true)
        action.clearHome()
        action.close()
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
        val action = RecordDb(context)
        action.open(true)
        action.clearHistory()
        action.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
            )
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

    fun deleteDir(dir: File?): Boolean {
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

    fun printTimestamp(function: String) {
        val timestamp = System.currentTimeMillis()
        Log.v("timestamp", "$function timestamp:$timestamp")
    }

    private fun guessFilename(url: String, contentDisposition: String, mimeType: String): String {
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
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }
}