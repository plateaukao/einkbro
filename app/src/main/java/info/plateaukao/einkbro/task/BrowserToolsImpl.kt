package info.plateaukao.einkbro.task

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.core.content.ContextCompat
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.WebViewCallback
import info.plateaukao.einkbro.data.remote.ChatMessage
import info.plateaukao.einkbro.data.remote.ChatRole
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.epub.EpubChapterContent
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.preference.SavedFileInfo
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Default [BrowserTools] implementation backed by an off-screen [EBWebView] and the
 * shared [OpenAiRepository]. One instance is created per [TaskRunner] invocation and
 * [dispose]d when the task completes or is cancelled.
 */
class BrowserToolsImpl(
    private val context: Context,
    private val webViewCallback: WebViewCallback,
    private val browserState: BrowserState,
    private val config: ConfigManager,
    private val openAiRepository: OpenAiRepository,
    private val ttsViewModel: TtsViewModel,
    private val progressSink: (TaskProgress.StepLine) -> Unit,
    private val finishSink: (String) -> Unit,
    private val initialSnapshot: InitialPageSnapshot? = null,
) : BrowserTools {

    private var bgWebView: EBWebView? = null
    private var currentLoadDeferred: CompletableDeferred<Boolean>? = null

    private val json = Json { ignoreUnknownKeys = true }

    // ── JavaScript evaluation ───────────────────────────────────────────

    override suspend fun runJavascriptInBg(code: String): String? {
        val wv = bgWebView ?: return null
        return evaluateJs(wv, code)
    }

    override suspend fun runJavascriptInInitialTab(code: String): String? {
        val wv = initialSnapshot?.originWebView?.get() ?: return null
        return evaluateJs(wv, code)
    }

    /** Null when [webView] has already been destroyed (e.g. the user closed that tab). */
    private suspend fun evaluateJs(webView: EBWebView, code: String): String? =
        withContext(Dispatchers.Main) {
            if (webView.isWebViewDestroyed) return@withContext null
            withTimeoutOrNull(JS_EVAL_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    try {
                        webView.evaluateJavascript(code) { result ->
                            if (cont.isActive) cont.resume(result ?: "null")
                        }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume("error: ${e.message}")
                    }
                }
            } ?: "error: javascript evaluation timed out"
        }

    // ── Navigation / content ────────────────────────────────────────────

    override suspend fun openUrlInBg(url: String): Boolean {
        val webView = ensureBgWebView()
        val deferred = CompletableDeferred<Boolean>()
        currentLoadDeferred = deferred
        withContext(Dispatchers.Main) { webView.loadUrl(url) }
        val ok = withTimeoutOrNull(PAGE_LOAD_TIMEOUT_MS) { deferred.await() } ?: false
        currentLoadDeferred = null
        return ok
    }

    override suspend fun searchInBg(query: String): Boolean {
        val searchUrl = BrowserUnit.queryWrapper(context, query)
        return openUrlInBg(searchUrl)
    }

    override suspend fun currentBgPageText(): String {
        val wv = bgWebView ?: return ""
        return withContext(Dispatchers.Main) {
            withTimeoutOrNull(READER_EXTRACT_TIMEOUT_MS) { wv.getRawText() }
        } ?: ""
    }

    override suspend fun currentBgPageLinks(): List<BrowserTools.Link> {
        val wv = bgWebView ?: return emptyList()
        val raw = withContext(Dispatchers.Main) { wv.jsBridge.getPageLinks() }
        return parseLinks(raw)
    }

    override suspend fun activeTabText(): String {
        val wv = browserState.ebWebView
        return withContext(Dispatchers.Main) { wv.getRawText() }
    }

    override suspend fun activeTabLinks(): List<BrowserTools.Link> {
        val wv = browserState.ebWebView
        val raw = withContext(Dispatchers.Main) { wv.jsBridge.getPageLinks() }
        return parseLinks(raw)
    }

    override fun activeTabUrl(): String = browserState.ebWebView.url.orEmpty()

    override fun activeTabTitle(): String = browserState.ebWebView.title.orEmpty()

    // ── Originating page snapshot ──────────────────────────────────────

    override fun initialPageUrl(): String = initialSnapshot?.url.orEmpty()
    override fun initialPageTitle(): String = initialSnapshot?.title.orEmpty()
    override fun initialPageText(): String = initialSnapshot?.text.orEmpty()
    override fun initialPageLinks(): List<BrowserTools.Link> = initialSnapshot?.links.orEmpty()
    override fun initialPageRawHtml(): String = initialSnapshot?.rawHtml.orEmpty()

    // ── Page source (network-level) ─────────────────────────────────────

    private val sourceClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Last fetched (url, html) pair so paged tool reads don't re-download. */
    private var pageSourceCache: Pair<String, String>? = null

    override suspend fun fetchPageSource(url: String): String? {
        pageSourceCache?.let { if (it.first == url) return it.second }
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val request = try {
            Request.Builder()
                .url(url)
                .header("User-Agent", browserUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .apply {
                    CookieManager.getInstance().getCookie(url)?.let { header("Cookie", it) }
                }
                .build()
        } catch (e: Exception) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                sourceClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val contentType = resp.header("Content-Type").orEmpty().lowercase()
                    if (BINARY_TYPE_PREFIXES.any { contentType.startsWith(it) }) return@use null
                    val text = resp.body?.string() ?: return@use null
                    val capped = if (text.length > MAX_SOURCE_CHARS) text.take(MAX_SOURCE_CHARS) else text
                    pageSourceCache = url to capped
                    capped
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun browserUserAgent(): String =
        withContext(Dispatchers.Main) {
            try {
                browserState.ebWebView.settings.userAgentString
            } catch (e: Exception) {
                null
            }
        } ?: WebSettings.getDefaultUserAgent(context)

    // ── Domain config ───────────────────────────────────────────────────

    private fun initialHost(): String? {
        val url = initialSnapshot?.url ?: return null
        return android.net.Uri.parse(url)?.host?.takeIf { it.isNotBlank() }
    }

    override fun getInitialDomainJavascript(): String {
        val host = initialHost() ?: return ""
        return config.domainConfigurationMap[host]?.postLoadJavascript.orEmpty()
    }

    override fun getInitialDomainCss(): String {
        val host = initialHost() ?: return ""
        return config.domainConfigurationMap[host]?.customCss.orEmpty()
    }

    override fun setInitialDomainJavascript(code: String) {
        val host = initialHost() ?: return
        val current = config.domainConfigurationMap[host] ?: DomainConfigurationData(host)
        config.updateDomainConfig(current.copy(postLoadJavascript = code.ifBlank { null }))
    }

    override fun setInitialDomainCss(code: String) {
        val host = initialHost() ?: return
        val current = config.domainConfigurationMap[host] ?: DomainConfigurationData(host)
        config.updateDomainConfig(current.copy(customCss = code.ifBlank { null }))
    }

    private fun parseLinks(raw: String): List<BrowserTools.Link> {
        if (raw.isBlank() || raw == "null") return emptyList()
        return try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RawLink.serializer()), raw)
                .map { BrowserTools.Link(it.text, it.href) }
        } catch (e: Exception) {
            android.util.Log.e("BrowserToolsImpl", "Failed to parse links JSON: $raw", e)
            emptyList()
        }
    }

    @Serializable
    private data class RawLink(val text: String, val href: String)

    // ── LLM ─────────────────────────────────────────────────────────────

    override suspend fun askLlm(system: String, user: String): String? {
        val action = summarizeActionInfo()
        val messages = mutableListOf<ChatMessage>()
        if (system.isNotBlank()) messages += ChatMessage(content = system, role = ChatRole.System)
        messages += ChatMessage(content = user, role = ChatRole.User)

        return withContext(Dispatchers.IO) {
            if (action.actionType == GptActionType.Gemini) {
                openAiRepository.queryGemini(messages, action).valueOrNull()?.takeIf { it.isNotBlank() }
            } else {
                val completion = openAiRepository.chatCompletion(messages, action)
                completion?.choices
                    ?.firstOrNull { it.message.role == ChatRole.Assistant }
                    ?.message?.content
            }
        }
    }

    override fun summarizeActionInfo(): ChatGPTActionInfo = ChatGPTActionInfo(
        name = "task",
        systemMessage = "You are a helpful assistant that follows instructions precisely.",
        userMessage = "",
        actionType = config.ai.getDefaultActionType(),
        model = config.ai.getDefaultActionModel(),
    )

    override fun defaultLanguageName(): String {
        val tag = config.uiLocaleLanguage.ifBlank { null }
        val locale = if (tag != null) java.util.Locale.forLanguageTag(tag)
        else java.util.Locale.getDefault()
        val name = locale.getDisplayName(java.util.Locale.ENGLISH)
        return if (name.isBlank()) "the user's language" else name
    }

    override fun speak(text: String, title: String) {
        if (text.isBlank()) return
        ttsViewModel.readArticle(text, title)
    }

    // ── EPUB export ─────────────────────────────────────────────────────

    private val epubManager: EpubManager by lazy { EpubManager(context) }

    override suspend fun saveEpub(
        bookName: String,
        chapters: List<BrowserTools.EpubChapterSpec>,
        onChapterLoaded: (Int, Int, String) -> Unit,
    ): String? {
        if (chapters.isEmpty()) return null

        val contents = mutableListOf<EpubChapterContent>()
        chapters.forEachIndexed { index, spec ->
            if (!openUrlInBg(spec.url)) return@forEachIndexed
            val wv = bgWebView ?: return@forEachIndexed
            val loaded = withContext(Dispatchers.Main) {
                withTimeoutOrNull(READER_EXTRACT_TIMEOUT_MS) { wv.getRawReaderHtml() }
                    ?.let { it to wv.title.orEmpty() }
            } ?: return@forEachIndexed
            val (html, pageTitle) = loaded
            if (html.isBlank()) return@forEachIndexed
            val title = spec.title.ifBlank { pageTitle.ifBlank { spec.url } }
            contents += EpubChapterContent(title, html, spec.url)
            onChapterLoaded(index + 1, chapters.size, title)
        }
        if (contents.isEmpty()) return null

        val safeName = bookName.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60)
        val (fileUri, savedLocation) = createEpubDownloadUri(safeName) ?: return null
        if (!epubManager.saveEpubDirectly(fileUri, bookName, contents)) {
            // Don't leave a 0-byte MediaStore entry in Downloads on failure.
            try {
                context.contentResolver.delete(fileUri, null, null)
            } catch (_: Exception) {
            }
            return null
        }

        val bookUri = fileUri.toString()
        if (config.savedEpubFileInfos.none { it.uri == bookUri }) {
            config.addSavedEpubFile(SavedFileInfo(bookName, bookUri))
        }
        return savedLocation
    }

    /** Creates a writable destination for a new EPUB plus its human-readable location.
     *  MediaStore Downloads on Q+; on older devices the public Downloads folder when
     *  the legacy storage permission is granted, else app-private storage (the book is
     *  still registered in EinkBro's saved-EPUB list, so the user can open it there). */
    private fun createEpubDownloadUri(fileName: String): Pair<Uri, String>? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.epub")
                put(MediaStore.MediaColumns.MIME_TYPE, Constants.MIME_TYPE_EPUB)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?.let { it to "${Environment.DIRECTORY_DOWNLOADS}/$fileName.epub" }
        } else {
            val canWritePublic = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val dir = if (canWritePublic) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }
            if (!dir.exists()) dir.mkdirs()
            var file = File(dir, "$fileName.epub")
            var suffix = 1
            while (file.exists()) {
                file = File(dir, "$fileName (${suffix++}).epub")
            }
            val location = if (canWritePublic) "${Environment.DIRECTORY_DOWNLOADS}/${file.name}"
            else "EinkBro's saved EPUB list (${file.name})"
            Uri.fromFile(file) to location
        }
    } catch (e: Exception) {
        android.util.Log.e("BrowserToolsImpl", "createEpubDownloadUri failed", e)
        null
    }

    // ── Progress reporting ──────────────────────────────────────────────

    override fun info(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Info, text))

    override fun tool(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Tool, text))

    override fun error(text: String) =
        progressSink(TaskProgress.StepLine(TaskProgress.StepLine.Kind.Error, text))

    override fun finish(markdown: String) = finishSink(markdown)

    // ── Lifecycle ───────────────────────────────────────────────────────

    private fun ensureBgWebView(): EBWebView {
        bgWebView?.let { return it }
        val wv = EBWebView(context, webViewCallback).apply {
            setOnPageFinishedAction {
                currentLoadDeferred?.complete(true)
            }
        }
        bgWebView = wv
        return wv
    }

    override fun dispose() {
        val wv = bgWebView ?: return
        try {
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (_: Exception) {
        }
        bgWebView = null
        currentLoadDeferred?.complete(false)
        currentLoadDeferred = null
    }

    companion object {
        private const val PAGE_LOAD_TIMEOUT_MS = 30_000L
        private const val JS_EVAL_TIMEOUT_MS = 10_000L

        // Reader-mode extraction goes through the JS bridge, whose callback is not
        // guaranteed to fire on every page — never wait on it unbounded.
        private const val READER_EXTRACT_TIMEOUT_MS = 15_000L

        // Memory guard for fetched page sources; windowed tool reads never need more.
        private const val MAX_SOURCE_CHARS = 2_000_000

        // Content types that would decode to garbage if treated as page source.
        private val BINARY_TYPE_PREFIXES = listOf(
            "image/", "video/", "audio/", "font/",
            "application/octet-stream", "application/pdf", "application/zip",
        )
    }
}
