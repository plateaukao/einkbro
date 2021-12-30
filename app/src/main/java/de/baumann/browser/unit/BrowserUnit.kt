package de.baumann.browser.unit

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
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
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.browser.Javascript
import de.baumann.browser.database.RecordDb
import de.baumann.browser.unit.HelperUnit.needGrantStoragePermission
import de.baumann.browser.view.NinjaToast.showShort
import de.baumann.browser.view.dialog.TextInputDialog
import kotlinx.coroutines.launch
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern

object BrowserUnit {
    const val PROGRESS_MAX = 100
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
    const val URL_SCHEME_ABOUT = "about:"
    const val URL_SCHEME_MAIL_TO = "mailto:"
    private const val URL_SCHEME_HTTPS = "https://"
    const val URL_SCHEME_INTENT = "intent://"
    const val UA_DESKTOP =
        "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/44.0.2403.155 Safari/537.36"

    fun queryWrapper(context: Context, query: String): String {
        if (URLUtil.isValidUrl(query)) {
            return query
        }

        if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
            return query
        }

        val queryWithHttpsPrefix = URL_SCHEME_HTTPS + query
        if (URLUtil.isValidUrl(queryWithHttpsPrefix) && !query.contains(" ")) {
            return queryWithHttpsPrefix
        }

        // Use prefix and suffix to process some special links
        var query = query
        try {
            query = URLEncoder.encode(query, URL_ENCODING)
        } catch (u: UnsupportedEncodingException) {
            Log.w("browser", "Unsupported Encoding Exception")
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
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
            var adBlock: AdBlock? = null
            var js: Javascript? = null
            var cookie: Cookie? = null
            when (i) {
                0 -> {
                    adBlock = AdBlock(context)
                    filename = context.getString(R.string.export_whitelistAdBlock)
                }
                1 -> {
                    js = Javascript(context)
                    filename = context.getString(R.string.export_whitelistJS)
                }
                else -> {
                    cookie = Cookie(context)
                    filename = context.getString(R.string.export_whitelistAdBlock)
                }
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
                        adBlock!!.addDomain(line)
                        count++
                    }
                    1 -> if (!action.checkDomain(line, RecordUnit.TABLE_JAVASCRIPT)) {
                        js!!.addDomain(line)
                        count++
                    }
                    else -> if (!action.checkDomain(line, RecordUnit.COLUMN_DOMAIN)) {
                        cookie!!.addDomain(line)
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