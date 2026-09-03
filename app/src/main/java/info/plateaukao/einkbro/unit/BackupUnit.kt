package info.plateaukao.einkbro.unit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.JsonWriter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.room.withTransaction
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Article
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.ChatGptQuery
import info.plateaukao.einkbro.database.ChatSession
import info.plateaukao.einkbro.database.CookieDomain
import info.plateaukao.einkbro.database.DomainConfiguration
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.database.Highlight
import info.plateaukao.einkbro.database.JavascriptDomain
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.database.SavedPage
import info.plateaukao.einkbro.database.VideoTranscript
import info.plateaukao.einkbro.database.WhitelistDomain
import info.plateaukao.einkbro.userscript.UserScriptManager
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
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupCategory(val displayNameResId: Int) {
    ALL_PREFERENCES(R.string.backup_category_all_preferences),
    GPT_SETTINGS(R.string.backup_category_gpt_settings),
    BOOKMARKS(R.string.backup_category_bookmarks),
    HISTORY(R.string.backup_category_history),
    DATABASE_DATA(R.string.backup_category_database_data),
    USERSCRIPTS(R.string.backup_category_userscripts),
    TRANSCRIPTS(R.string.backup_category_transcripts),
    CHAT_SESSIONS(R.string.backup_category_chat_sessions),
}


class BackupUnit(
    private val context: Context,
) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val userScriptManager: UserScriptManager by inject()
    private val recordDb: RecordRepository by inject()
    private val sp: SharedPreferences by inject()
    private val coroutineScope: CoroutineScope by inject()

    // Per-app data directories derived from the running app's own context, so backup
    // and restore touch *this* app's sandbox rather than a hardcoded package path.
    // Required for non-default applicationIds (e.g. the `.a` side-by-side build),
    // whose data lives under /data/data/info.plateaukao.einkbro.a/.
    private val sharedPrefsDir: File get() = File(context.dataDir, "shared_prefs")
    private val databasesDir: File get() = File(context.dataDir, "databases")

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

    suspend fun backupToTempFile(
        categories: Set<BackupCategory>,
        fileName: String = "backup_share.zip",
    ): File? {
        return try {
            val tempFile = File(context.cacheDir, fileName)
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
            val sharedPrefsDirectory = sharedPrefsDir
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
            zos.writeJsonEntry(BOOKMARKS_FILE) { w ->
                w.beginArray()
                for (b in bookmarkManager.getAllBookmarks()) {
                    w.beginObject()
                    w.name("id").value(b.id)
                    w.name("title").value(b.title)
                    w.name("url").value(b.url)
                    w.name("isDirectory").value(b.isDirectory)
                    w.name("parent").value(b.parent)
                    w.name("order").value(b.order)
                    w.endObject()
                }
                w.endArray()
            }
        }

        if (BackupCategory.HISTORY in categories) {
            zos.writeJsonEntry(HISTORY_FILE) { w ->
                w.beginArray()
                for (record in recordDb.listAllHistory()) {
                    w.beginObject()
                    w.name("title").value(record.title)
                    w.name("url").value(record.url)
                    w.name("time").value(record.time)
                    w.endObject()
                }
                w.endArray()
            }
        }

        if (BackupCategory.DATABASE_DATA in categories) {
            val db = bookmarkManager.database
            // Favicon blobs are deliberately not exported: they are re-fetchable
            // from the sites themselves and used to dominate the backup size.
            // Restore still accepts a "favicons" array from older backups.
            zos.writeJsonEntry(DATABASE_DATA_FILE) { w ->
                w.beginObject()

                // Articles & Highlights (articles first since highlights reference them)
                w.name("articles").beginArray()
                for (a in db.articleDao().getAllArticlesAsync()) {
                    w.beginObject()
                    w.name("id").value(a.id)
                    w.name("title").value(a.title)
                    w.name("url").value(a.url)
                    w.name("date").value(a.date)
                    w.name("tags").value(a.tags)
                    w.endObject()
                }
                w.endArray()

                w.name("highlights").beginArray()
                for (h in db.highlightDao().getAllHighlightsAsync()) {
                    w.beginObject()
                    w.name("id").value(h.id)
                    w.name("articleId").value(h.articleId)
                    w.name("content").value(h.content)
                    w.endObject()
                }
                w.endArray()

                w.name("chat_gpt_queries").beginArray()
                for (q in db.chatGptQueryDao().getAllChatGptQueriesAsync()) {
                    w.beginObject()
                    w.name("id").value(q.id)
                    w.name("date").value(q.date)
                    w.name("url").value(q.url)
                    w.name("model").value(q.model)
                    w.name("selectedText").value(q.selectedText)
                    w.name("result").value(q.result)
                    w.endObject()
                }
                w.endArray()

                w.name("domain_configurations").beginArray()
                for (dc in db.domainConfigurationDao().getAllDomainConfigurations()) {
                    w.beginObject()
                    w.name("domain").value(dc.domain)
                    w.name("configuration").value(dc.configuration)
                    w.endObject()
                }
                w.endArray()

                w.name("saved_pages").beginArray()
                for (sp in db.savedPageDao().getAllSavedPagesAsync()) {
                    w.beginObject()
                    w.name("id").value(sp.id)
                    w.name("title").value(sp.title)
                    w.name("url").value(sp.url)
                    w.name("filePath").value(sp.filePath)
                    w.name("savedAt").value(sp.savedAt)
                    w.endObject()
                }
                w.endArray()

                w.name("whitelist_domains").beginArray()
                for (d in db.domainListDao().getAllWhitelistDomains()) w.value(d)
                w.endArray()

                w.name("javascript_domains").beginArray()
                for (d in db.domainListDao().getAllJavascriptDomains()) w.value(d)
                w.endArray()

                w.name("cookie_domains").beginArray()
                for (d in db.domainListDao().getAllCookieDomains()) w.value(d)
                w.endArray()

                w.endObject()
            }
        }

        if (BackupCategory.USERSCRIPTS in categories) {
            writeUserscriptsToZip(zos, USERSCRIPTS_DIR)
        }

        if (BackupCategory.TRANSCRIPTS in categories) {
            zos.writeJsonEntry(TRANSCRIPTS_FILE) { w ->
                w.beginArray()
                for (t in bookmarkManager.database.videoTranscriptDao().getAllTranscripts()) {
                    w.beginObject()
                    w.name("videoId").value(t.videoId)
                    w.name("transcript").value(t.transcript)
                    w.name("timestamp").value(t.timestamp)
                    w.endObject()
                }
                w.endArray()
            }
        }

        if (BackupCategory.CHAT_SESSIONS in categories) {
            zos.writeJsonEntry(CHAT_SESSIONS_FILE) { w ->
                w.beginArray()
                for (s in bookmarkManager.database.chatSessionDao().getAllSessions()) {
                    w.beginObject()
                    w.name("id").value(s.id)
                    w.name("title").value(s.title)
                    w.name("created").value(s.created)
                    w.name("lastUpdated").value(s.lastUpdated)
                    w.name("webTitle").value(s.webTitle)
                    w.name("webUrl").value(s.webUrl)
                    w.name("messages").value(s.messages)
                    w.endObject()
                }
                w.endArray()
            }
        }

        zos.close()
        outputStream.close()
    }

    /**
     * Streams one JSON zip entry through [JsonWriter] so a large table never
     * exists as an in-memory JSON tree plus its String plus its byte array.
     * The writer is flushed, not closed — closing it would close the zip.
     */
    private inline fun ZipOutputStream.writeJsonEntry(name: String, block: (JsonWriter) -> Unit) {
        putNextEntry(ZipEntry(name))
        val writer = JsonWriter(OutputStreamWriter(this, Charsets.UTF_8))
        block(writer)
        writer.flush()
        closeEntry()
    }

    /**
     * Writes each script body as its own `<name>.user.js` entry — so the zip is also
     * usable by other userscript managers — plus a [USERSCRIPTS_META_FILE] entry
     * carrying the data the script header can't (enabled state, source URL, GM values),
     * all under [prefix].
     */
    private suspend fun writeUserscriptsToZip(zos: ZipOutputStream, prefix: String = "") {
        val manifest = JSONArray()
        val usedNames = mutableSetOf<String>()
        for ((script, values) in userScriptManager.getAllForBackup()) {
            val base = script.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                .ifEmpty { "script" }
            var fileName = "$base.user.js"
            var suffix = 0
            while (!usedNames.add(fileName)) fileName = "$base-${++suffix}.user.js"
            zos.putNextEntry(ZipEntry(prefix + fileName))
            zos.write(script.code.toByteArray())
            zos.closeEntry()
            manifest.put(JSONObject().apply {
                put("file", fileName)
                put("name", script.name)
                put("enabled", script.enabled)
                script.sourceUrl?.let { put("sourceUrl", it) }
                if (values.isNotEmpty()) {
                    put("values", JSONObject().apply { values.forEach { put(it.key, it.value) } })
                }
            })
        }
        zos.putNextEntry(ZipEntry(prefix + USERSCRIPTS_META_FILE))
        zos.write(JSONObject().put("scripts", manifest).toString().toByteArray())
        zos.closeEntry()
    }

    /**
     * Installs scripts collected from a zip. [meta] (the [USERSCRIPTS_META_FILE] entry)
     * supplies enabled state, source URL, and GM values per file when present. Scripts
     * merge by @name — a same-named script updates in place rather than duplicating,
     * while scripts only installed locally are left untouched — so neither a backup
     * restore nor a zip from another device or script manager can wipe local scripts.
     * Returns the imported count.
     */
    private suspend fun restoreUserscripts(
        bodies: Map<String, String>,
        meta: JSONObject?,
    ): Int {
        val metaByFile = LinkedHashMap<String, JSONObject>()
        meta?.optJSONArray("scripts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                obj.optString("file").takeIf { it.isNotEmpty() }?.let { metaByFile[it] = obj }
            }
        }
        // Manifest order first (preserves the backed-up ordering), then any .user.js
        // entries the manifest doesn't know about (e.g. zips from other managers).
        val orderedFiles = metaByFile.keys.filter { it in bodies } +
            bodies.keys.filter { it !in metaByFile }
        var imported = 0
        for (file in orderedFiles) {
            // Per-file so one malformed manifest entry can't abort the rest of the batch.
            try {
                val scriptMeta = metaByFile[file]
                val values = mutableMapOf<String, String>()
                scriptMeta?.optJSONObject("values")?.let { v ->
                    v.keys().forEach { key -> values[key] = v.optString(key) }
                }
                val id = userScriptManager.importScript(
                    code = bodies.getValue(file),
                    enabled = scriptMeta?.optBoolean("enabled", true) ?: true,
                    sourceUrl = scriptMeta?.optString("sourceUrl")?.takeIf { it.isNotEmpty() },
                    values = values,
                )
                if (id != null) imported++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        userScriptManager.reload()
        return imported
    }

    /**
     * Categories available in the zip, each with the uncompressed byte size of its
     * entries (mirroring the backup dialog's estimates), or null for a legacy backup
     * format (no manifest). Reads the whole stream once — call on an IO dispatcher.
     */
    fun getAvailableCategoryOptions(
        context: Context,
        uri: Uri,
    ): List<Pair<BackupCategory, Long>>? = try {
        context.contentResolver.openInputStream(uri)?.use { fis ->
            scanZipCategories(ZipInputStream(fis))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun getAvailableCategoryOptions(file: File): List<Pair<BackupCategory, Long>>? = try {
        file.inputStream().use { fis -> scanZipCategories(ZipInputStream(fis)) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun scanZipCategories(zis: ZipInputStream): List<Pair<BackupCategory, Long>>? {
        var manifestCategories: Set<BackupCategory>? = null
        val sizes = mutableMapOf<BackupCategory, Long>()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name == MANIFEST_FILE) {
                val manifest = JSONObject(String(zis.readBytes()))
                val categoriesArray = manifest.getJSONArray("categories")
                manifestCategories = buildSet {
                    for (i in 0 until categoriesArray.length()) {
                        try {
                            add(BackupCategory.valueOf(categoriesArray.getString(i)))
                        } catch (_: IllegalArgumentException) { }
                    }
                }
            } else {
                categoryForEntry(zipEntry.name)?.let { category ->
                    // ZipEntry.size is unreliable on a stream, so count the bytes.
                    var entrySize = 0L
                    while (true) {
                        val n = zis.read(buffer)
                        if (n < 0) break
                        entrySize += n
                    }
                    sizes.merge(category, entrySize, Long::plus)
                }
            }
            zipEntry = zis.nextEntry
        }
        val available = manifestCategories ?: return null // legacy format
        return BackupCategory.entries.filter { it in available }.map { it to (sizes[it] ?: 0L) }
    }

    private fun categoryForEntry(name: String): BackupCategory? = when {
        name.startsWith("shared_prefs/") -> BackupCategory.ALL_PREFERENCES
        name == GPT_SETTINGS_FILE -> BackupCategory.GPT_SETTINGS
        name == BOOKMARKS_FILE -> BackupCategory.BOOKMARKS
        name == HISTORY_FILE -> BackupCategory.HISTORY
        name == DATABASE_DATA_FILE -> BackupCategory.DATABASE_DATA
        name.startsWith(USERSCRIPTS_DIR) -> BackupCategory.USERSCRIPTS
        name == TRANSCRIPTS_FILE -> BackupCategory.TRANSCRIPTS
        name == CHAT_SESSIONS_FILE -> BackupCategory.CHAT_SESSIONS
        else -> null
    }

    suspend fun restoreBackupData(
        context: Context,
        uri: Uri,
        categories: Set<BackupCategory>,
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val fis = context.contentResolver.openInputStream(uri)
                    ?: return@withContext false
                val zis = ZipInputStream(fis)
                restoreZipEntries(zis, categories)
                zis.close()
                fis.close()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun restoreZipEntries(
        zis: ZipInputStream,
        categories: Set<BackupCategory>,
    ) {
        // Userscript body entries and their metadata entry can appear in any order,
        // so collect them during the single pass and restore after the loop.
        val userscriptBodies = LinkedHashMap<String, String>()
        var userscriptsMeta: JSONObject? = null

        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            when {
                zipEntry.name == MANIFEST_FILE -> { /* skip */ }

                zipEntry.name.startsWith("shared_prefs/")
                        && BackupCategory.ALL_PREFERENCES in categories -> {
                    val fileName = zipEntry.name.removePrefix("shared_prefs/")
                    val file = File(sharedPrefsDir, remapPrefsFileName(fileName))
                    writeStreamToFile(zis, file)
                }

                zipEntry.name == GPT_SETTINGS_FILE
                        && BackupCategory.GPT_SETTINGS in categories
                        && BackupCategory.ALL_PREFERENCES !in categories -> {
                    val content = zis.readBytes()
                    importGptSettings(JSONObject(String(content)))
                }

                zipEntry.name == BOOKMARKS_FILE
                        && BackupCategory.BOOKMARKS in categories -> {
                    val content = zis.readBytes()
                    val bookmarks = JSONArray(String(content))
                        .toJSONObjectList()
                        .map { it.toBookmark() }
                    mergeBookmarks(bookmarks)
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

                zipEntry.name == TRANSCRIPTS_FILE
                        && BackupCategory.TRANSCRIPTS in categories -> {
                    restoreTranscripts(JSONArray(String(zis.readBytes())))
                }

                zipEntry.name == CHAT_SESSIONS_FILE
                        && BackupCategory.CHAT_SESSIONS in categories -> {
                    restoreChatSessions(JSONArray(String(zis.readBytes())))
                }

                zipEntry.name.startsWith(USERSCRIPTS_DIR)
                        && BackupCategory.USERSCRIPTS in categories -> {
                    val fileName = zipEntry.name.removePrefix(USERSCRIPTS_DIR)
                    when {
                        fileName == USERSCRIPTS_META_FILE ->
                            userscriptsMeta = JSONObject(String(zis.readBytes()))
                        fileName.endsWith(".user.js") ->
                            userscriptBodies[fileName] = String(zis.readBytes())
                    }
                }
            }
            zipEntry = zis.nextEntry
        }

        if (BackupCategory.USERSCRIPTS in categories && userscriptBodies.isNotEmpty()) {
            restoreUserscripts(userscriptBodies, userscriptsMeta)
        }
    }

    suspend fun restoreBackupData(
        file: File,
        categories: Set<BackupCategory>,
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val zis = ZipInputStream(file.inputStream())
                restoreZipEntries(zis, categories)
                zis.close()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
                val file = if (zipEntry.name.endsWith(".db") ||
                    zipEntry.name.contains("einkbro_db")
                ) File(databasesDir, zipEntry.name)
                else File(sharedPrefsDir, remapPrefsFileName(zipEntry.name))
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

    /**
     * Merges a backed-up bookmark tree into the local one instead of replacing it.
     * Imported ids are per-device autoincrements and mean nothing here, so folders
     * match by title within the same (already mapped) parent — created with fresh
     * ids when missing — and bookmarks match by url within their mapped folder, so
     * nothing local is deleted and re-restoring the same backup adds nothing.
     */
    private suspend fun mergeBookmarks(imported: List<Bookmark>) = bookmarkManager.database.withTransaction {
        val dao = bookmarkManager.database.bookmarkDao()
        val local = dao.getAllBookmarks()
        val localFolders = HashMap<Pair<Int, String>, Int>()
        local.filter { it.isDirectory }.forEach { localFolders[it.parent to it.title] = it.id }
        val localUrls = local.filterNot { it.isDirectory }
            .map { it.parent to it.url }
            .toHashSet()

        // Map imported folder ids to local ones, walking parents before children.
        // Real exports only have positive ids; a non-positive one would collide with
        // the id 0 root sentinel, so such corrupted entries aren't created at all. A
        // parent id that isn't an imported folder falls back to the root folder
        // rather than dropping the subtree, and on a parent cycle (equally only
        // possible in a hand-corrupted zip) the first-processed member lands at the
        // root with the rest of the cycle chained under it.
        val folders = imported.filter { it.isDirectory && it.id > 0 }
        val importedFolderIds = folders.map { it.id }.toHashSet()
        val idMap = HashMap<Int, Int>().apply { put(0, 0) }
        val pending = folders.toMutableList()
        while (pending.isNotEmpty()) {
            val ready = pending
                .filter { it.parent in idMap || it.parent !in importedFolderIds }
                .ifEmpty { pending.toList() }
            for (folder in ready) {
                val localParent = idMap[folder.parent] ?: 0
                val key = localParent to folder.title
                idMap[folder.id] = localFolders[key] ?: dao.insert(
                    Bookmark(folder.title, folder.url, true, localParent, folder.order)
                ).toInt().also { localFolders[key] = it }
            }
            pending.removeAll(ready)
        }

        for (bookmark in imported.filterNot { it.isDirectory }) {
            val localParent = idMap[bookmark.parent] ?: 0
            if (localUrls.add(localParent to bookmark.url)) {
                dao.insert(Bookmark(bookmark.title, bookmark.url, false, localParent, bookmark.order))
            }
        }
    }

    private suspend fun restoreDatabaseData(json: JSONObject) {
        val db = bookmarkManager.database

        // Favicons — only present in backups from older versions (new backups no
        // longer export the blobs). Merge: keep local icons and add only domains
        // without one.
        if (json.has("favicons")) {
            val dao = db.faviconDao()
            val existingDomains = dao.getAllDomains().toHashSet()
            val arr = json.getJSONArray("favicons")
            val favicons = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                FaviconInfo(
                    domain = obj.getString("domain"),
                    icon = if (obj.isNull("icon")) null
                        else Base64.decode(obj.getString("icon"), Base64.NO_WRAP)
                ).takeIf { existingDomains.add(it.domain) }
            }
            dao.insertAll(favicons)
            bookmarkManager.noteFaviconDomains(favicons.map { it.domain })
        }

        // Articles + Highlights — merge: an article matches a local one on
        // url + date (ids are per-device autoincrements); unmatched articles are
        // inserted with fresh ids. Backup article ids are remapped through
        // [articleIdMap] so highlights land on the right local article; highlights
        // dedupe on (article, content), and one without a mapped article is skipped
        // rather than tripping the foreign key.
        val articleIdMap = HashMap<Int, Int>()
        if (json.has("articles")) {
            val dao = db.articleDao()
            val localByKey = HashMap<Pair<String, Long>, Int>()
            dao.getAllArticlesAsync().forEach { localByKey[it.url to it.date] = it.id }
            val arr = json.getJSONArray("articles")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val article = Article(
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    date = obj.getLong("date"),
                    tags = obj.optString("tags", ""),
                )
                val key = article.url to article.date
                articleIdMap[obj.getInt("id")] = localByKey[key]
                    ?: dao.insert(article).toInt().also { localByKey[key] = it }
            }
        }

        if (json.has("highlights")) {
            val dao = db.highlightDao()
            val seen = dao.getAllHighlightsAsync().map { it.articleId to it.content }.toHashSet()
            val arr = json.getJSONArray("highlights")
            val highlights = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val articleId = articleIdMap[obj.getInt("articleId")] ?: return@mapNotNull null
                Highlight(
                    articleId = articleId,
                    content = obj.getString("content"),
                ).takeIf { seen.add(articleId to it.content) }
            }
            dao.insertAll(highlights)
        }

        // ChatGptQuery — merge: keep local rows and append only queries not already
        // present. Ids are per-device autoincrements, so matching is on content
        // (date + url + model + selectedText) and inserted rows get fresh ids.
        if (json.has("chat_gpt_queries")) {
            val dao = db.chatGptQueryDao()
            val seenKeys = dao.getAllChatGptQueriesAsync().map { it.mergeKey() }.toHashSet()
            val arr = json.getJSONArray("chat_gpt_queries")
            val queries = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                ChatGptQuery(
                    date = obj.getLong("date"),
                    url = obj.getString("url"),
                    model = obj.getString("model"),
                    selectedText = obj.getString("selectedText"),
                    result = obj.getString("result"),
                ).takeIf { seenKeys.add(it.mergeKey()) }
            }
            dao.insertAll(queries)
        }

        // DomainConfiguration — merge field by field: a setting tweaked on this
        // device is kept, the backup fills in whatever the local rule leaves
        // unset (or the whole rule when there is none). Decoding both sides
        // normalises legacy rows, so an empty leftover rule never blocks a
        // restore. The in-memory map is refreshed right away; it is otherwise
        // only loaded at startup and the restart prompt may be declined.
        if (json.has("domain_configurations")) {
            val dao = db.domainConfigurationDao()
            val local = dao.getAllDomainConfigurations()
                .associate { it.domain to bookmarkManager.decodeDomainConfiguration(it) }
            val arr = json.getJSONArray("domain_configurations")
            val merged = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val row = DomainConfiguration(
                    domain = obj.getString("domain"),
                    configuration = obj.getString("configuration"),
                )
                val imported = runCatching { bookmarkManager.decodeDomainConfiguration(row) }
                    .getOrNull() ?: return@mapNotNull null
                val result = local[row.domain]?.mergedWith(imported) ?: imported
                result.takeIf { !it.isEmpty }?.let { bookmarkManager.encodeDomainConfiguration(it) }
            }
            dao.insertAll(merged)
            bookmarkManager.reloadDomainConfigurations()
        }

        // SavedPage — merge: append entries whose file isn't tracked locally yet
        // (filePath identifies the saved file); new rows get fresh ids.
        if (json.has("saved_pages")) {
            val dao = db.savedPageDao()
            val seenPaths = dao.getAllSavedPagesAsync().map { it.filePath }.toHashSet()
            val arr = json.getJSONArray("saved_pages")
            val pages = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                SavedPage(
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    filePath = obj.getString("filePath"),
                    savedAt = obj.getLong("savedAt"),
                ).takeIf { seenPaths.add(it.filePath) }
            }
            dao.insertAll(pages)
        }

        // Domain lists — merge: rows are just the domain (its primary key), so the
        // REPLACE inserts dedupe against local entries and nothing is deleted.
        if (json.has("whitelist_domains")) {
            val arr = json.getJSONArray("whitelist_domains")
            val domains = (0 until arr.length()).map { WhitelistDomain(arr.getString(it)) }
            db.domainListDao().insertAllWhitelist(domains)
        }

        if (json.has("javascript_domains")) {
            val arr = json.getJSONArray("javascript_domains")
            val domains = (0 until arr.length()).map { JavascriptDomain(arr.getString(it)) }
            db.domainListDao().insertAllJavascript(domains)
        }

        if (json.has("cookie_domains")) {
            val arr = json.getJSONArray("cookie_domains")
            val domains = (0 until arr.length()).map { CookieDomain(arr.getString(it)) }
            db.domainListDao().insertAllCookie(domains)
        }
    }

    // Merge: local transcripts are kept (each one cost a Gemini transcription) and
    // only videos not transcribed on this device are appended.
    private suspend fun restoreTranscripts(arr: JSONArray) {
        val dao = bookmarkManager.database.videoTranscriptDao()
        val existingIds = dao.getAllTranscripts().map { it.videoId }.toHashSet()
        val transcripts = (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            VideoTranscript(
                videoId = obj.getString("videoId"),
                transcript = obj.getString("transcript"),
                timestamp = obj.optLong("timestamp"),
            ).takeIf { it.videoId !in existingIds }
        }
        dao.insertAll(transcripts)
    }

    // Merge: sessions this device doesn't have are appended; when both sides hold
    // the same session (same UUID), the one updated last wins. The backup doesn't
    // carry webContent, so a backup-wins update keeps the local page text.
    private suspend fun restoreChatSessions(arr: JSONArray) {
        val dao = bookmarkManager.database.chatSessionDao()
        val existingById = dao.getAllSessions().associateBy { it.id }
        val sessions = (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val local = existingById[obj.getString("id")]
            if (local != null && local.lastUpdated >= obj.optLong("lastUpdated")) {
                return@mapNotNull null
            }
            ChatSession(
                id = obj.getString("id"),
                title = obj.optString("title"),
                created = obj.optLong("created"),
                lastUpdated = obj.optLong("lastUpdated"),
                webTitle = obj.optString("webTitle"),
                webUrl = obj.optString("webUrl"),
                messages = obj.optString("messages", "[]"),
                webContent = local?.webContent ?: "",
            )
        }
        dao.insertAll(sessions)
    }

    /**
     * Backup categories to offer in the backup dialog, each with a pre-compression
     * estimate of its exported size in bytes. Categories that exist purely for
     * optional bulky data (transcripts, chat sessions) are omitted while empty.
     */
    suspend fun getBackupCategoryOptions(): List<Pair<BackupCategory, Long>> =
        withContext(Dispatchers.IO) {
            BackupCategory.entries.mapNotNull { category ->
                val size = estimateCategorySize(category)
                if (size == 0L && category in OPTIONAL_WHEN_EMPTY) null
                else category to size
            }
        }

    /**
     * Estimated size of one category's exported JSON. DB-backed categories are
     * summed with SQL LENGTH() plus a rough per-row overhead for JSON keys and
     * punctuation, so nothing is serialized twice just to be measured.
     */
    private fun estimateCategorySize(category: BackupCategory): Long = when (category) {
        BackupCategory.ALL_PREFERENCES ->
            sharedPrefsDir.listFiles()?.sumOf { it.length() } ?: 0L

        BackupCategory.GPT_SETTINGS ->
            exportGptSettings().toString().length.toLong()

        BackupCategory.BOOKMARKS ->
            rawSum("SELECT IFNULL(SUM(LENGTH(title) + LENGTH(url) + 80), 0) FROM bookmarks")

        BackupCategory.HISTORY ->
            rawSum("SELECT IFNULL(SUM(LENGTH(TITLE) + LENGTH(URL) + 50), 0) FROM HISTORY")

        BackupCategory.DATABASE_DATA ->
            rawSum("SELECT IFNULL(SUM(LENGTH(title) + LENGTH(url) + LENGTH(tags) + 70), 0) FROM articles") +
                rawSum("SELECT IFNULL(SUM(LENGTH(content) + 50), 0) FROM highlights") +
                rawSum("SELECT IFNULL(SUM(LENGTH(selectedText) + LENGTH(result) + LENGTH(url) + LENGTH(model) + 90), 0) FROM chat_gpt_query") +
                rawSum("SELECT IFNULL(SUM(LENGTH(domain) + LENGTH(configuration) + 40), 0) FROM domain_configuration") +
                rawSum("SELECT IFNULL(SUM(LENGTH(title) + LENGTH(url) + LENGTH(filePath) + 70), 0) FROM saved_pages") +
                rawSum("SELECT IFNULL(SUM(LENGTH(DOMAIN) + 4), 0) FROM WHITELIST") +
                rawSum("SELECT IFNULL(SUM(LENGTH(DOMAIN) + 4), 0) FROM JAVASCRIPT") +
                rawSum("SELECT IFNULL(SUM(LENGTH(DOMAIN) + 4), 0) FROM COOKIE")

        BackupCategory.USERSCRIPTS ->
            rawSum("SELECT IFNULL(SUM(LENGTH(code) + LENGTH(name) + 80), 0) FROM user_scripts") +
                rawSum("SELECT IFNULL(SUM(LENGTH(`key`) + LENGTH(`value`) + 10), 0) FROM user_script_values")

        BackupCategory.TRANSCRIPTS ->
            rawSum("SELECT IFNULL(SUM(LENGTH(transcript) + LENGTH(videoId) + 60), 0) FROM video_transcripts")

        BackupCategory.CHAT_SESSIONS ->
            rawSum("SELECT IFNULL(SUM(LENGTH(messages) + LENGTH(title) + LENGTH(webTitle) + LENGTH(webUrl) + 120), 0) FROM chat_sessions")
    }

    private fun rawSum(sql: String): Long =
        bookmarkManager.database.openHelper.readableDatabase.query(sql).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
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

    /**
     * The default SharedPreferences file is named "<packageName>_preferences.xml".
     * When restoring a backup made under a different applicationId (the real app into
     * the `.a` build, or vice versa), rewrite that name to the current package so this
     * app actually reads the restored values. Other prefs files keep their fixed names
     * (ad-filter's "io.github.edsuns.filter", WebView's), so they restore as-is.
     * No-op when source and target package already match.
     */
    private fun remapPrefsFileName(name: String): String =
        if (name.endsWith("_preferences.xml")) "${context.packageName}_preferences.xml" else name

    private fun writeStreamToFile(zis: ZipInputStream, file: File) {
        file.parentFile?.mkdirs()
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

    fun exportUserscripts(uri: Uri) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        val zos = ZipOutputStream(os)
                        writeUserscriptsToZip(zos)
                        zos.close()
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Userscripts are exported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Userscripts export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun importUserscripts(uri: Uri) {
        coroutineScope.launch {
            try {
                val bodies = LinkedHashMap<String, String>()
                var meta: JSONObject? = null
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { fis ->
                        val zis = ZipInputStream(fis)
                        var entry = zis.nextEntry
                        while (entry != null) {
                            // Match on the base name so both flat userscripts.zip files and
                            // full app-data backups (entries under "userscripts/") import.
                            if (!entry.isDirectory) {
                                val baseName = entry.name.substringAfterLast('/')
                                when {
                                    baseName == USERSCRIPTS_META_FILE ->
                                        meta = JSONObject(String(zis.readBytes()))
                                    baseName.endsWith(".user.js") -> {
                                        // Same base name at different depths: suffix instead
                                        // of overwriting, so no script body is dropped.
                                        var key = baseName
                                        var n = 0
                                        while (key in bodies) key = "${++n}-$baseName"
                                        bodies[key] = String(zis.readBytes())
                                    }
                                }
                            }
                            entry = zis.nextEntry
                        }
                        zis.close()
                    }
                }
                val count = restoreUserscripts(bodies, meta)
                withContext(Dispatchers.Main) {
                    val message = if (count > 0) "$count userscript(s) imported"
                    else "No userscripts found in file"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Userscripts import failed", Toast.LENGTH_SHORT).show()
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
        private const val MANIFEST_FILE = "_manifest.json"
        private const val GPT_SETTINGS_FILE = "gpt_settings.json"
        private const val BOOKMARKS_FILE = "bookmarks.json"
        private const val HISTORY_FILE = "history.json"
        private const val DATABASE_DATA_FILE = "database_data.json"
        private const val USERSCRIPTS_DIR = "userscripts/"
        private const val USERSCRIPTS_META_FILE = "userscripts.json"
        private const val TRANSCRIPTS_FILE = "transcripts.json"
        private const val CHAT_SESSIONS_FILE = "chat_sessions.json"

        private val OPTIONAL_WHEN_EMPTY =
            setOf(BackupCategory.TRANSCRIPTS, BackupCategory.CHAT_SESSIONS)

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

// List equality makes this collision-proof (a "|"-joined string key would be
// ambiguous when the url or selected text itself contains the delimiter).
private fun ChatGptQuery.mergeKey(): List<Any> = listOf(date, url, model, selectedText)

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


