package info.plateaukao.einkbro.unit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.ChatGptQuery
import info.plateaukao.einkbro.database.CookieDomain
import info.plateaukao.einkbro.database.DomainConfiguration
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.database.JavascriptDomain
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.database.SavedPage
import info.plateaukao.einkbro.database.WhitelistDomain
import info.plateaukao.einkbro.view.EBToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupCategory(val displayNameResId: Int) {
    ALL_PREFERENCES(R.string.backup_category_all_preferences),
    GPT_SETTINGS(R.string.backup_category_gpt_settings),
    BOOKMARKS(R.string.backup_category_bookmarks),
    HISTORY(R.string.backup_category_history),
    DATABASE_DATA(R.string.backup_category_database_data),
}


class BackupUnit(
    private val context: Context,
) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val recordDb: RecordRepository by inject()
    private val sp: SharedPreferences by inject()
    private val coroutineScope: CoroutineScope by inject()

    suspend fun backupData(context: Context, uri: Uri, categories: Set<BackupCategory>): Boolean {
        try {
            val fos = context.contentResolver.openOutputStream(uri) ?: return false
            writeBackupZip(fos, categories)
            EBToast.show(context, R.string.toast_backup_successful)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun backupToTempFile(categories: Set<BackupCategory>): File? {
        return try {
            val tempFile = File(context.cacheDir, "backup_share.zip")
            writeBackupZip(FileOutputStream(tempFile), categories)
            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun writeBackupZip(
        outputStream: java.io.OutputStream,
        categories: Set<BackupCategory>,
    ) {
        val zos = ZipOutputStream(outputStream)

        // Write manifest
        val manifest = JSONObject().apply {
            put("version", 2)
            put("categories", JSONArray(categories.map { it.name }))
        }
        zos.putNextEntry(ZipEntry(MANIFEST_FILE))
        zos.write(manifest.toString().toByteArray())
        zos.closeEntry()

        if (BackupCategory.ALL_PREFERENCES in categories) {
            val sharedPrefsDirectory = File(SHARED_PREFS_PATH)
            val sharedPrefsFiles = sharedPrefsDirectory.listFiles()
            if (sharedPrefsFiles != null) {
                for (sharedPrefsFile in sharedPrefsFiles) {
                    writeFileToZip(zos, sharedPrefsFile, "shared_prefs/${sharedPrefsFile.name}")
                }
            }
        }

        if (BackupCategory.GPT_SETTINGS in categories) {
            val gptJson = exportGptSettings()
            zos.putNextEntry(ZipEntry(GPT_SETTINGS_FILE))
            zos.write(gptJson.toString().toByteArray())
            zos.closeEntry()
        }

        if (BackupCategory.BOOKMARKS in categories) {
            val bookmarks = kotlinx.coroutines.runBlocking {
                bookmarkManager.getAllBookmarks()
            }
            zos.putNextEntry(ZipEntry(BOOKMARKS_FILE))
            zos.write(bookmarks.toJsonString().toByteArray())
            zos.closeEntry()
        }

        if (BackupCategory.HISTORY in categories) {
            val history = recordDb.listAllHistory()
            val jsonArray = JSONArray()
            for (record in history) {
                jsonArray.put(JSONObject().apply {
                    put("title", record.title)
                    put("url", record.url)
                    put("time", record.time)
                })
            }
            zos.putNextEntry(ZipEntry(HISTORY_FILE))
            zos.write(jsonArray.toString().toByteArray())
            zos.closeEntry()
        }

        if (BackupCategory.DATABASE_DATA in categories) {
            val db = bookmarkManager.database
            val json = JSONObject()

            // Favicons
            val favicons = JSONArray()
            for (f in db.faviconDao().getAllFavicons()) {
                favicons.put(JSONObject().apply {
                    put("domain", f.domain)
                    put("icon", f.icon?.let { Base64.encodeToString(it, Base64.NO_WRAP) })
                })
            }
            json.put("favicons", favicons)

            // Articles & Highlights (articles first since highlights reference them)
            val articles = JSONArray()
            for (a in db.articleDao().getAllArticlesAsync()) {
                articles.put(JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("url", a.url)
                    put("date", a.date)
                    put("tags", a.tags)
                })
            }
            json.put("articles", articles)

            val highlights = JSONArray()
            for (h in db.highlightDao().getAllHighlightsAsync()) {
                highlights.put(JSONObject().apply {
                    put("id", h.id)
                    put("articleId", h.articleId)
                    put("content", h.content)
                })
            }
            json.put("highlights", highlights)

            // ChatGptQuery
            val queries = JSONArray()
            for (q in db.chatGptQueryDao().getAllChatGptQueriesAsync()) {
                queries.put(JSONObject().apply {
                    put("id", q.id)
                    put("date", q.date)
                    put("url", q.url)
                    put("model", q.model)
                    put("selectedText", q.selectedText)
                    put("result", q.result)
                })
            }
            json.put("chat_gpt_queries", queries)

            // DomainConfiguration
            val domainConfigs = JSONArray()
            for (dc in db.domainConfigurationDao().getAllDomainConfigurations()) {
                domainConfigs.put(JSONObject().apply {
                    put("domain", dc.domain)
                    put("configuration", dc.configuration)
                })
            }
            json.put("domain_configurations", domainConfigs)

            // SavedPage
            val savedPages = JSONArray()
            for (sp in db.savedPageDao().getAllSavedPagesAsync()) {
                savedPages.put(JSONObject().apply {
                    put("id", sp.id)
                    put("title", sp.title)
                    put("url", sp.url)
                    put("filePath", sp.filePath)
                    put("savedAt", sp.savedAt)
                })
            }
            json.put("saved_pages", savedPages)

            // Domain lists
            val whitelistDomains = JSONArray()
            for (d in db.domainListDao().getAllWhitelistDomains()) {
                whitelistDomains.put(d)
            }
            json.put("whitelist_domains", whitelistDomains)

            val javascriptDomains = JSONArray()
            for (d in db.domainListDao().getAllJavascriptDomains()) {
                javascriptDomains.put(d)
            }
            json.put("javascript_domains", javascriptDomains)

            val cookieDomains = JSONArray()
            for (d in db.domainListDao().getAllCookieDomains()) {
                cookieDomains.put(d)
            }
            json.put("cookie_domains", cookieDomains)

            zos.putNextEntry(ZipEntry(DATABASE_DATA_FILE))
            zos.write(json.toString().toByteArray())
            zos.closeEntry()
        }

        zos.close()
        outputStream.close()
    }

    /**
     * Returns available categories in the zip, or null if it's a legacy backup format.
     */
    fun getAvailableCategories(context: Context, uri: Uri): Set<BackupCategory>? {
        try {
            val fis = context.contentResolver.openInputStream(uri) ?: return null
            val zis = ZipInputStream(fis)
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == MANIFEST_FILE) {
                    val content = zis.readBytes()
                    val manifest = JSONObject(String(content))
                    val categoriesArray = manifest.getJSONArray("categories")
                    val categories = mutableSetOf<BackupCategory>()
                    for (i in 0 until categoriesArray.length()) {
                        try {
                            categories.add(BackupCategory.valueOf(categoriesArray.getString(i)))
                        } catch (_: IllegalArgumentException) { }
                    }
                    zis.close()
                    fis.close()
                    return categories
                }
                zipEntry = zis.nextEntry
            }
            zis.close()
            fis.close()
            return null // legacy format
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun restoreBackupData(
        context: Context,
        uri: Uri,
        categories: Set<BackupCategory>,
    ): Boolean {
        try {
            val fis = context.contentResolver.openInputStream(uri) ?: return false
            val zis = ZipInputStream(fis)

            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                when {
                    zipEntry.name == MANIFEST_FILE -> { /* skip */ }

                    zipEntry.name.startsWith("shared_prefs/")
                            && BackupCategory.ALL_PREFERENCES in categories -> {
                        val fileName = zipEntry.name.removePrefix("shared_prefs/")
                        val file = File("$SHARED_PREFS_PATH$fileName")
                        writeStreamToFile(zis, file)
                    }

                    zipEntry.name == GPT_SETTINGS_FILE
                            && BackupCategory.GPT_SETTINGS in categories -> {
                        val content = zis.readBytes()
                        importGptSettings(JSONObject(String(content)))
                    }

                    zipEntry.name == BOOKMARKS_FILE
                            && BackupCategory.BOOKMARKS in categories -> {
                        val content = zis.readBytes()
                        val bookmarks = JSONArray(String(content))
                            .toJSONObjectList()
                            .map { it.toBookmark() }
                        kotlinx.coroutines.runBlocking {
                            bookmarkManager.overwriteBookmarks(bookmarks)
                        }
                    }

                    zipEntry.name == HISTORY_FILE
                            && BackupCategory.HISTORY in categories -> {
                        val content = zis.readBytes()
                        val jsonArray = JSONArray(String(content))
                        val records = (0 until jsonArray.length()).map { i ->
                            val obj = jsonArray.getJSONObject(i)
                            Record(
                                obj.optString("title"),
                                obj.optString("url"),
                                obj.optLong("time"),
                            )
                        }
                        recordDb.replaceAllHistory(records)
                    }

                    zipEntry.name == DATABASE_DATA_FILE
                            && BackupCategory.DATABASE_DATA in categories -> {
                        val content = zis.readBytes()
                        restoreDatabaseData(JSONObject(String(content)))
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.close()
            fis.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getAvailableCategories(file: File): Set<BackupCategory>? {
        try {
            val zis = ZipInputStream(file.inputStream())
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == MANIFEST_FILE) {
                    val content = zis.readBytes()
                    val manifest = JSONObject(String(content))
                    val categoriesArray = manifest.getJSONArray("categories")
                    val categories = mutableSetOf<BackupCategory>()
                    for (i in 0 until categoriesArray.length()) {
                        try {
                            categories.add(BackupCategory.valueOf(categoriesArray.getString(i)))
                        } catch (_: IllegalArgumentException) { }
                    }
                    zis.close()
                    return categories
                }
                zipEntry = zis.nextEntry
            }
            zis.close()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun restoreBackupData(
        file: File,
        categories: Set<BackupCategory>,
    ): Boolean {
        try {
            val zis = ZipInputStream(file.inputStream())
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                when {
                    zipEntry.name == MANIFEST_FILE -> { /* skip */ }

                    zipEntry.name.startsWith("shared_prefs/")
                            && BackupCategory.ALL_PREFERENCES in categories -> {
                        val fileName = zipEntry.name.removePrefix("shared_prefs/")
                        val target = File("$SHARED_PREFS_PATH$fileName")
                        writeStreamToFile(zis, target)
                    }

                    zipEntry.name == GPT_SETTINGS_FILE
                            && BackupCategory.GPT_SETTINGS in categories -> {
                        val content = zis.readBytes()
                        importGptSettings(JSONObject(String(content)))
                    }

                    zipEntry.name == BOOKMARKS_FILE
                            && BackupCategory.BOOKMARKS in categories -> {
                        val content = zis.readBytes()
                        val bookmarks = JSONArray(String(content))
                            .toJSONObjectList()
                            .map { it.toBookmark() }
                        kotlinx.coroutines.runBlocking {
                            bookmarkManager.overwriteBookmarks(bookmarks)
                        }
                    }

                    zipEntry.name == HISTORY_FILE
                            && BackupCategory.HISTORY in categories -> {
                        val content = zis.readBytes()
                        val jsonArray = JSONArray(String(content))
                        val records = (0 until jsonArray.length()).map { i ->
                            val obj = jsonArray.getJSONObject(i)
                            Record(
                                obj.optString("title"),
                                obj.optString("url"),
                                obj.optLong("time"),
                            )
                        }
                        recordDb.replaceAllHistory(records)
                    }

                    zipEntry.name == DATABASE_DATA_FILE
                            && BackupCategory.DATABASE_DATA in categories -> {
                        val content = zis.readBytes()
                        restoreDatabaseData(JSONObject(String(content)))
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /** Legacy restore: restores everything from old-format zip (no manifest). */
    fun restoreLegacyBackupData(context: Context, uri: Uri): Boolean {
        try {
            bookmarkManager.database.close()

            val fis = context.contentResolver.openInputStream(uri) ?: return false
            val zis = ZipInputStream(fis)

            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val file = File(
                    if (zipEntry.name.endsWith(".db") ||
                        zipEntry.name.contains("einkbro_db")
                    ) "$DATABASE_PATH${zipEntry.name}"
                    else "$SHARED_PREFS_PATH${zipEntry.name}"
                )
                writeStreamToFile(zis, file)
                zipEntry = zis.nextEntry
            }
            zis.close()
            fis.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private suspend fun restoreDatabaseData(json: JSONObject) {
        val db = bookmarkManager.database

        // Favicons
        if (json.has("favicons")) {
            val arr = json.getJSONArray("favicons")
            val favicons = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                FaviconInfo(
                    domain = obj.getString("domain"),
                    icon = if (obj.isNull("icon")) null
                        else Base64.decode(obj.getString("icon"), Base64.NO_WRAP)
                )
            }
            db.faviconDao().deleteAll()
            db.faviconDao().insertAll(favicons)
        }

        // Articles (restore before highlights due to foreign key)
        if (json.has("articles")) {
            val arr = json.getJSONArray("articles")
            val articles = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Article(
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    date = obj.getLong("date"),
                    tags = obj.optString("tags", ""),
                ).apply { id = obj.getInt("id") }
            }
            db.highlightDao().deleteAll()
            db.articleDao().deleteAll()
            db.articleDao().insertAll(articles)
        }

        // Highlights
        if (json.has("highlights")) {
            val arr = json.getJSONArray("highlights")
            val highlights = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Highlight(
                    articleId = obj.getInt("articleId"),
                    content = obj.getString("content"),
                ).apply { id = obj.getInt("id") }
            }
            db.highlightDao().insertAll(highlights)
        }

        // ChatGptQuery
        if (json.has("chat_gpt_queries")) {
            val arr = json.getJSONArray("chat_gpt_queries")
            val queries = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ChatGptQuery(
                    date = obj.getLong("date"),
                    url = obj.getString("url"),
                    model = obj.getString("model"),
                    selectedText = obj.getString("selectedText"),
                    result = obj.getString("result"),
                ).apply { id = obj.getInt("id") }
            }
            db.chatGptQueryDao().deleteAll()
            db.chatGptQueryDao().insertAll(queries)
        }

        // DomainConfiguration
        if (json.has("domain_configurations")) {
            val arr = json.getJSONArray("domain_configurations")
            val configs = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DomainConfiguration(
                    domain = obj.getString("domain"),
                    configuration = obj.getString("configuration"),
                )
            }
            db.domainConfigurationDao().deleteAll()
            db.domainConfigurationDao().insertAll(configs)
        }

        // SavedPage
        if (json.has("saved_pages")) {
            val arr = json.getJSONArray("saved_pages")
            val pages = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SavedPage(
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    filePath = obj.getString("filePath"),
                    savedAt = obj.getLong("savedAt"),
                ).apply { id = obj.getInt("id") }
            }
            db.savedPageDao().deleteAll()
            db.savedPageDao().insertAll(pages)
        }

        // Domain lists
        if (json.has("whitelist_domains")) {
            val arr = json.getJSONArray("whitelist_domains")
            val domains = (0 until arr.length()).map { WhitelistDomain(arr.getString(it)) }
            db.domainListDao().deleteAllWhitelist()
            db.domainListDao().insertAllWhitelist(domains)
        }

        if (json.has("javascript_domains")) {
            val arr = json.getJSONArray("javascript_domains")
            val domains = (0 until arr.length()).map { JavascriptDomain(arr.getString(it)) }
            db.domainListDao().deleteAllJavascript()
            db.domainListDao().insertAllJavascript(domains)
        }

        if (json.has("cookie_domains")) {
            val arr = json.getJSONArray("cookie_domains")
            val domains = (0 until arr.length()).map { CookieDomain(arr.getString(it)) }
            db.domainListDao().deleteAllCookie()
            db.domainListDao().insertAllCookie(domains)
        }
    }

    private fun exportGptSettings(): JSONObject {
        val json = JSONObject()
        val allPrefs = sp.all
        for (key in GPT_PREF_KEYS) {
            val value = allPrefs[key] ?: continue
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value.toDouble())
                is String -> json.put(key, value)
                else -> json.put(key, value.toString())
            }
        }
        return json
    }

    private fun importGptSettings(json: JSONObject) {
        sp.edit {
            for (key in json.keys()) {
                when (val value = json.get(key)) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putFloat(key, value.toFloat())
                    is String -> putString(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
    }

    private fun writeFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val fis = FileInputStream(file)
        zos.putNextEntry(ZipEntry(entryName))
        fis.copyTo(zos)
        zos.closeEntry()
        fis.close()
    }

    private fun writeDirectoryToZip(zos: ZipOutputStream, dir: File, zipPrefix: String) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val entryPath = zipPrefix + file.name
            if (file.isDirectory) {
                zos.putNextEntry(ZipEntry("$entryPath/"))
                zos.closeEntry()
                writeDirectoryToZip(zos, file, "$entryPath/")
            } else {
                writeFileToZip(zos, file, entryPath)
            }
        }
    }

    private fun writeStreamToFile(zis: ZipInputStream, file: File) {
        val fos = FileOutputStream(file)
        zis.copyTo(fos)
        fos.close()
    }

    fun importBookmarks(uri: Uri) {
        coroutineScope.launch {
            try {
                val contentString = getFileContentString(uri)
                // detect if the content is a json array
                val bookmarks = if (contentString.startsWith("[")) {
                    JSONArray(contentString).toJSONObjectList().map { json -> json.toBookmark() }
                } else {
                    //parseHtmlToBookmarkList(contentString)
                    parseChromeBookmarks(contentString)
                }

                if (bookmarks.isNotEmpty()) {
                    bookmarkManager.overwriteBookmarks(bookmarks)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks are imported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks import failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private suspend fun getFileContentString(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri).use {
                it?.bufferedReader()?.readText().orEmpty()
            }
        }
    }

    private var recordId = 0
    private fun parseChromeBookmarks(html: String): List<Bookmark> {
        val doc = Jsoup.parse(html)
        val bookmarks = dlElement(doc.select("DL").first()!!.children(), recordId)
        recordId = 0
        return bookmarks
    }

    private fun dlElement(elements: Elements, parentId: Int): List<Bookmark> {
        val bookmarkList = mutableListOf<Bookmark>()
        for (elem in elements) {
            when (elem.nodeName().uppercase()) {
                "DT" -> bookmarkList.addAll(dtElement(elem.children(), parentId))
                "DL" -> bookmarkList.addAll(dlElement(elem.children(), parentId))
                "P" -> continue
                else -> {}
            }
        }
        return bookmarkList
    }

    private var currentFolderId = 0
    private fun dtElement(elements: Elements, parentId: Int): List<Bookmark> {
        val bookmarkList = mutableListOf<Bookmark>()
        for (elem in elements) {
            when (elem.nodeName().uppercase()) {
                "H3" -> {
                    currentFolderId = ++recordId
                    bookmarkList.add(
                        Bookmark(
                            elem.text(),
                            "",
                            true,
                            parentId,
                        ).apply { id = currentFolderId }
                    )
                }

                "A" -> bookmarkList.add(
                    Bookmark(
                        elem.text(),
                        elem.attr("href"),
                        false,
                        parentId,
                    ).apply { id = ++recordId }
                )

                "DL" -> bookmarkList.addAll(dlElement(elem.children(), currentFolderId))
                "P" -> continue
                else -> {}
            }
        }
        return bookmarkList
    }

    private fun elementToBookmarks(element: Element): List<Bookmark> {
        val bookmarkList = mutableListOf<Bookmark>()
        val bookmarkElements = element.select("a")
        for (bookmarkElement in bookmarkElements) {
            val bookmark = Bookmark(
                bookmarkElement.text(),
                bookmarkElement.attr("href"),
            )
            bookmarkList.add(bookmark)
        }
        return bookmarkList
    }

    private fun JSONArray.toJSONObjectList() =
        (0 until length()).map { get(it) as JSONObject }

    fun exportBookmarks(uri: Uri, showToast: Boolean = true) {
        coroutineScope.launch {
            val bookmarks = bookmarkManager.getAllBookmarks()
            try {
                context.contentResolver.openOutputStream(uri).use {
                    it?.write(bookmarks.toJsonString().toByteArray())
                }
                if (showToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmarks are exported", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmarks export failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    fun preprocessActivityResult(result: ActivityResult): Uri? {
        if (result.resultCode != FragmentActivity.RESULT_OK) return null
        val uri = result.data?.data ?: return null
        context.contentResolver
            .takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        return uri
    }

    fun exportDataToFileUri(uri: Uri, data: String) {
        val fileContent = data.toByteArray()

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(fileContent)
        }
    }

    private fun shareFile(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(intent, "Share via"))
    }


    companion object {
        private const val DATABASE_PATH = "/data/data/info.plateaukao.einkbro/databases/"
        private const val SHARED_PREFS_PATH = "/data/data/info.plateaukao.einkbro/shared_prefs/"
        private const val MANIFEST_FILE = "_manifest.json"
        private const val GPT_SETTINGS_FILE = "gpt_settings.json"
        private const val BOOKMARKS_FILE = "bookmarks.json"
        private const val HISTORY_FILE = "history.json"
        private const val DATABASE_DATA_FILE = "database_data.json"

        private val GPT_PREF_KEYS = listOf(
            "sp_gpt_api_key",
            "sp_gemini_api_key",
            "sp_gpt_system_prompt",
            "sp_gpt_user_prompt",
            "sp_gpt_user_prompt_web_page",
            "sp_gp_model",
            "sp_gpt_voice_model",
            "sp_gpt_voice_prompt",
            "sp_alternative_model",
            "sp_gemini_model",
            "sp_use_openai_tts",
            "sp_external_search_with_gpt",
            "sp_enable_open_ai_stream",
            "sp_gpt_action_items",
            "sp_gpt_action_external",
            "sp_gpt_for_chat_web",
            "sp_gpt_for_summary",
            "sp_gpt_server_url",
            "sp_use_custom_gpt_url",
            "sp_use_gemini_api",
            "K_GPT_VOICE_OPTION",
        )
    }
}

private fun List<Bookmark>.toJsonString(): String {
    val jsonArrays = JSONArray()
    this.map { it.toJsonObject() }.forEach { jsonArrays.put(it) }

    return jsonArrays.toString()
}

private fun Bookmark.toJsonObject(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("title", title)
        put("url", url)
        put("isDirectory", isDirectory)
        put("parent", parent)
        put("order", order)
    }

private fun JSONObject.toBookmark(): Bookmark =
    Bookmark(
        optString("title"),
        optString("url"),
        optBoolean("isDirectory"),
        optInt("parent"),
        optInt("order")
    ).apply { id = optInt("id") }


