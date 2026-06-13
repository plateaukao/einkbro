package info.plateaukao.einkbro.userscript

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import android.util.Log
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.UserScript
import info.plateaukao.einkbro.database.UserScriptValue
import info.plateaukao.einkbro.unit.HelperUnit
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/** A userscript paired with its parsed metadata and resolved @require contents. */
data class ParsedUserScript(
    val script: UserScript,
    val metadata: UserScriptMetadata,
    var requiresContent: String = "",
)

/** Outcome of [UserScriptManager.checkAndUpdate]. */
sealed class UpdateResult {
    /** A newer version was downloaded and installed. */
    data class Updated(val from: String, val to: String) : UpdateResult()
    /** The remote version was not newer than the installed one. */
    object UpToDate : UpdateResult()
    /** The script has no `@updateURL`/`@downloadURL` and no install URL to check. */
    object NoSource : UpdateResult()
    /** The check or download failed (network error, empty response, etc.). */
    data class Failed(val message: String) : UpdateResult()
}

class UserScriptManager(private val context: Context) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val coroutineScope: CoroutineScope by inject()
    private val httpClient = OkHttpClient()

    private val userScriptDao get() = bookmarkManager.userScriptDao
    private val valueDao get() = bookmarkManager.userScriptValueDao

    /**
     * Script bodies are stored as files rather than inline in the DB row: a single
     * userscript (e.g. Immersive Translate) can exceed several MB, which overflows
     * Android's 2MB CursorWindow and makes the row unreadable (SQLiteBlobTooBigException).
     * The DB row keeps only metadata; [UserScript.code] is persisted here, keyed by id.
     */
    private val scriptDir: File by lazy { File(context.filesDir, "userscripts").apply { mkdirs() } }
    private fun codeFile(id: Long) = File(scriptDir, "$id.user.js")
    private fun readCodeFile(id: Long): String? = codeFile(id).takeIf { it.exists() }?.readText()
    private fun writeCodeFile(id: Long, code: String) = codeFile(id).writeText(code)
    private fun deleteCodeFile(id: Long) { codeFile(id).delete() }

    @Volatile
    var scripts: List<ParsedUserScript> = emptyList()
        private set

    init {
        coroutineScope.launch(Dispatchers.IO) { reload() }
    }

    suspend fun reload() {
        val rows = try {
            userScriptDao.getAll()
        } catch (e: SQLiteBlobTooBigException) {
            // Legacy rows stored the (multi-MB) script body inline and overflow the 2MB
            // CursorWindow, so they can't be read back. Drop them — bodies now live in files.
            Log.w(TAG, "Dropping oversized inline userscript rows: ${e.message}")
            userScriptDao.deleteAll()
            emptyList()
        }
        val loaded = rows.mapNotNull { row ->
            // Body lives in a file; fall back to row.code for any pre-existing inline (small) rows.
            val code = readCodeFile(row.id) ?: row.code
            try {
                ParsedUserScript(row.copy(code = code), UserScriptMetadata.parse(code))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse userscript ${row.name}: ${e.message}")
                null
            }
        }
        loaded.forEach { resolveRequires(it) }
        scripts = loaded
    }

    private fun resolveRequires(parsed: ParsedUserScript) {
        if (parsed.metadata.requires.isEmpty()) return
        val sb = StringBuilder()
        parsed.metadata.requires.forEach { url ->
            val cached = requireCache[url]
            if (cached != null) {
                sb.append(cached).append('\n')
                return@forEach
            }
            try {
                val resp = httpClient.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string().orEmpty()
                resp.close()
                requireCache[url] = body
                sb.append(body).append('\n')
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch @require $url: ${e.message}")
            }
        }
        parsed.requiresContent = sb.toString()
    }

    fun getMatchingScripts(url: String, runAt: RunAt): List<ParsedUserScript> {
        if (!url.startsWith("http")) return emptyList()
        return scripts.filter { parsed ->
            parsed.script.enabled &&
                parsed.metadata.runAt == runAt &&
                UrlMatcher.matches(url, parsed.metadata.matches, parsed.metadata.includes) &&
                !UrlMatcher.isExcluded(url, parsed.metadata.excludes)
        }
    }

    fun getById(id: Long): ParsedUserScript? = scripts.firstOrNull { it.script.id == id }

    /**
     * Builds the JS to inject for a script: the templated GM shim, then any
     * resolved @require contents, then the script body.
     */
    fun buildInjectionJs(parsed: ParsedUserScript): String {
        val shim = HelperUnit.loadAssetFile("gm_shim.js")
            .replace("__SCRIPT_ID__", parsed.script.id.toString())
            .replace("__GM_INFO__", buildGmInfo(parsed))
        return buildString {
            append(shim).append('\n')
            if (parsed.requiresContent.isNotEmpty()) append(parsed.requiresContent).append('\n')
            append(parsed.script.code)
            // Give the injected script a source URL so Chromium treats its exceptions as
            // same-origin (full message + stack) instead of opaque "Script error.".
            append("\n//# sourceURL=einkbro-userscript-").append(parsed.script.id).append(".user.js\n")
        }
    }

    private fun buildGmInfo(parsed: ParsedUserScript): String {
        val m = parsed.metadata
        val scriptObj = JSONObject().apply {
            put("name", m.name)
            put("namespace", m.namespace)
            put("version", m.version)
            put("description", m.description)
            put("runAt", if (m.runAt == RunAt.DOCUMENT_START) "document-start" else "document-end")
            put("matches", JSONArray(m.matches))
            put("includes", JSONArray(m.includes))
            put("excludes", JSONArray(m.excludes))
            put("grant", JSONArray(m.grants))
            put("connects", JSONArray(m.connects))
        }
        return JSONObject().apply {
            put("script", scriptObj)
            put("scriptHandler", "EinkBro")
            put("version", parsed.script.id.toString())
            put("scriptMetaStr", "")
            put("uuid", parsed.script.id.toString())
        }.toString()
    }

    // region CRUD

    suspend fun add(code: String, sourceUrl: String? = null): Long {
        val metadata = UserScriptMetadata.parse(code)
        // Re-installing a script with the same @name edits the existing one in place
        // rather than creating a duplicate (e.g. fetching a newer version of a script).
        // Skip the "Unnamed script" default so metadata-less scripts don't collapse together.
        val existing = if (metadata.name != UserScriptMetadata.DEFAULT_NAME) {
            scripts.firstOrNull { it.script.name == metadata.name }?.script
        } else null
        if (existing != null) {
            userScriptDao.update(existing.copy(code = "", sourceUrl = sourceUrl ?: existing.sourceUrl))
            writeCodeFile(existing.id, code)
            reload()
            return existing.id
        }
        val maxOrder = scripts.maxOfOrNull { it.script.order } ?: 0
        // Persist only metadata in the DB row; the body goes to a file keyed by the new id.
        val id = userScriptDao.insert(
            UserScript(name = metadata.name, code = "", sourceUrl = sourceUrl, order = maxOrder + 1)
        )
        writeCodeFile(id, code)
        reload()
        return id
    }

    suspend fun update(script: UserScript) {
        // refresh name from metadata in case code changed
        val name = try {
            UserScriptMetadata.parse(script.code).name
        } catch (e: Exception) {
            script.name
        }
        userScriptDao.update(script.copy(name = name, code = ""))
        writeCodeFile(script.id, script.code)
        reload()
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val script = userScriptDao.getById(id) ?: return
        userScriptDao.update(script.copy(enabled = enabled))
        reload()
    }

    /**
     * Checks a script's remote source for a newer version and, if found, replaces it in place
     * (preserving id/enabled/order via [update]). The check URL is `@updateURL`, then
     * `@downloadURL`, then the original install URL; the body is fetched from `@downloadURL`,
     * then the install URL, reusing the already-fetched check response when they coincide.
     * Only updates when the remote `@version` is strictly newer than the installed one.
     */
    suspend fun checkAndUpdate(id: Long): UpdateResult = withContext(Dispatchers.IO) {
        val parsed = getById(id) ?: return@withContext UpdateResult.Failed("script not found")
        val meta = parsed.metadata
        val sourceUrl = parsed.script.sourceUrl.orEmpty()
        val checkUrl = meta.updateUrl.ifEmpty { meta.downloadUrl }.ifEmpty { sourceUrl }
        if (checkUrl.isEmpty()) return@withContext UpdateResult.NoSource

        val checkBody = try {
            httpGet(checkUrl)
        } catch (e: Exception) {
            return@withContext UpdateResult.Failed(e.message ?: "fetch failed")
        } ?: return@withContext UpdateResult.Failed("empty response")

        val remoteVersion = UserScriptMetadata.parse(checkBody).version
        if (!UserScriptMetadata.isNewer(remoteVersion, meta.version)) {
            return@withContext UpdateResult.UpToDate
        }

        val downloadUrl = meta.downloadUrl.ifEmpty { sourceUrl }.ifEmpty { checkUrl }
        val newCode = if (downloadUrl == checkUrl) {
            checkBody
        } else {
            try {
                httpGet(downloadUrl)
            } catch (e: Exception) {
                return@withContext UpdateResult.Failed(e.message ?: "download failed")
            } ?: return@withContext UpdateResult.Failed("empty download")
        }

        update(parsed.script.copy(code = newCode))
        UpdateResult.Updated(from = meta.version, to = remoteVersion)
    }

    private fun httpGet(url: String): String? =
        httpClient.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) { valueDao.deleteAllForScript(id) }
        userScriptDao.deleteById(id)
        deleteCodeFile(id)
        reload()
    }

    // endregion

    // region GM value storage — called from the WebView JS-bridge thread (not main)

    fun gmGetValue(scriptId: Long, key: String): String? = valueDao.getValue(scriptId, key)

    fun gmSetValue(scriptId: Long, key: String, value: String) =
        valueDao.setValue(UserScriptValue(scriptId, key, value))

    fun gmDeleteValue(scriptId: Long, key: String) = valueDao.deleteValue(scriptId, key)

    fun gmListValues(scriptId: Long): List<String> = valueDao.listKeys(scriptId)

    // endregion

    companion object {
        private const val TAG = "UserScriptManager"
        private val requireCache = HashMap<String, String>()
    }
}
