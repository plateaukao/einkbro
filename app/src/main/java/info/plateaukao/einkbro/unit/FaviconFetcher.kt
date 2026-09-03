package info.plateaukao.einkbro.unit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.FaviconInfo
import info.plateaukao.einkbro.unit.FaviconCandidates.Candidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Fetches a site's favicon ourselves and stores it under the host it was fetched
 * for. WebView's `onReceivedIcon` gives a bitmap but not the page it belongs to:
 * Chromium never cancels an in-flight icon download on navigation, and during a
 * Back/Forward the WebView's `url`/`originalUrl` already name the destination, so
 * icons regularly arrived tagged with the wrong host and were persisted that way.
 * Here the host and the icon candidates come from the same document, so the
 * association can't drift.
 */
class FaviconFetcher(private val bookmarkManager: BookmarkManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Hosts already fetched this process; one icon request per host per session is
    // enough to keep the store fresh and to heal rows poisoned by older versions.
    private val handledHosts: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun isHandled(host: String): Boolean = host in handledHosts

    /** Result of favicon_probe.js: the document's hostname and its icon links. */
    data class Probe(val host: String, val candidates: List<Candidate>)

    /**
     * Stores the best decodable icon among [candidates] (from the document at
     * [pageUrl]) under [host] — and under [aliasHost] too, when a cross-host
     * redirect got here (threads.net -> threads.com), so bookmarks still pointing
     * at the requested host keep their icon. Returns the stored bitmap, or null
     * when every host was already handled this session or nothing decodable was
     * found.
     */
    suspend fun storeForPage(
        host: String,
        pageUrl: String,
        candidates: List<Candidate>,
        aliasHost: String? = null,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val alias = aliasHost?.takeIf { it != host && handledHosts.add(it) }
        if (!handledHosts.add(host) && alias == null) return@withContext null
        // SPA hydration can have replaced the head before the probe ran; the
        // served HTML still declares the icons, so re-read it like refresh() does.
        val effective = candidates.ifEmpty {
            val (finalUrl, html) = fetchHtml(pageUrl) ?: (pageUrl to "")
            FaviconCandidates.parseIconLinks(html, finalUrl)
        }
        val bitmap = download(FaviconCandidates.orderedUrls(effective, pageUrl))
            ?: return@withContext copyStoredToAlias(host, alias)
        store(host, bitmap)
        alias?.let { store(it, bitmap) }
        bitmap
    }

    /**
     * Fetch failed, but the redirect target may have an icon stored from an
     * earlier session — good enough for the alias host, which serves nothing
     * itself (that's why it redirects).
     */
    private suspend fun copyStoredToAlias(host: String, alias: String?): Bitmap? {
        alias ?: return null
        val bitmap = withContext(Dispatchers.Main) {
            bookmarkManager.findFaviconBitmapBy("https://$host/")
        } ?: return null
        store(alias, bitmap)
        return bitmap
    }

    /**
     * Re-fetches the icon for [pageUrl] from scratch — page HTML for the icon links,
     * then the icon — and replaces whatever is stored for its host. Used by the
     * bookmark "refresh icon" action, so it ignores the per-session guard.
     */
    suspend fun refresh(pageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        val host = Uri.parse(pageUrl).host ?: return@withContext null
        val (finalUrl, html) = fetchHtml(pageUrl) ?: (pageUrl to "")
        val candidates = FaviconCandidates.parseIconLinks(html, finalUrl)
        handledHosts.add(host)
        val bitmap = download(FaviconCandidates.orderedUrls(candidates, finalUrl)) ?: return@withContext null
        store(host, bitmap)
        bitmap
    }

    private suspend fun store(host: String, bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        // BookmarkManager's favicon cache is read from the main thread; mutate it there.
        withContext(Dispatchers.Main) {
            bookmarkManager.insertFavicon(FaviconInfo(domain = host, icon = bytes))
        }
        Log.d(TAG, "stored icon for $host (${bitmap.width}x${bitmap.height}, ${bytes.size} bytes)")
    }

    private suspend fun download(urls: List<String>): Bitmap? = withContext(Dispatchers.IO) {
        for (url in urls) {
            val bytes = try {
                if (url.startsWith("data:", ignoreCase = true)) decodeDataUri(url) else fetchBytes(url, "image/*", MAX_ICON_BYTES)
            } catch (e: Exception) {
                Log.d(TAG, "favicon fetch failed: $url: ${e.message}")
                null
            } ?: continue
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
            return@withContext shrink(bitmap)
        }
        Log.d(TAG, "no decodable icon among: $urls")
        null
    }

    private fun fetchHtml(pageUrl: String): Pair<String, String>? = try {
        client.newCall(request(pageUrl, "text/html,application/xhtml+xml")).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) return null
            val type = body.contentType()?.toString().orEmpty()
            if (type.isNotEmpty() && !type.contains("html", ignoreCase = true) && !type.contains("xml", ignoreCase = true)) return null
            val source = body.source()
            source.request(MAX_HTML_BYTES)
            val html = source.buffer.readUtf8(minOf(source.buffer.size, MAX_HTML_BYTES))
            response.request.url.toString() to html
        }
    } catch (e: Exception) {
        Log.d(TAG, "page fetch failed: $pageUrl: ${e.message}")
        null
    }

    private fun fetchBytes(url: String, accept: String, limit: Long): ByteArray? =
        client.newCall(request(url, accept)).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) return null
            if (body.contentLength() > limit) return null
            val source = body.source()
            source.request(limit + 1)
            if (source.buffer.size > limit) return null
            source.buffer.readByteArray()
        }

    private fun request(url: String, accept: String): Request {
        val builder = Request.Builder().url(url)
            .header("Accept", accept)
            .header("User-Agent", USER_AGENT)
        // Icons on private sites (intranets, logged-in dashboards) need the session.
        runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
            ?.takeIf { it.isNotBlank() }?.let { builder.header("Cookie", it) }
        return builder.build()
    }

    private fun decodeDataUri(uri: String): ByteArray? {
        val comma = uri.indexOf(',')
        if (comma < 0) return null
        val header = uri.substring(0, comma)
        val payload = uri.substring(comma + 1)
        return if (header.contains(";base64", ignoreCase = true)) Base64.decode(payload, Base64.DEFAULT)
        else Uri.decode(payload).toByteArray()
    }

    /** Keeps stored icons small; list rows never draw them larger than this. */
    private fun shrink(bitmap: Bitmap): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= MAX_STORED_EDGE) return bitmap
        val scale = MAX_STORED_EDGE.toFloat() / max
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    companion object {
        private const val TAG = "FaviconFetcher"
        private const val MAX_ICON_BYTES = 1024L * 1024
        private const val MAX_HTML_BYTES = 256L * 1024
        private const val MAX_STORED_EDGE = 64
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        /** Parses the evaluateJavascript result of favicon_probe.js (a JSON-encoded string, or "null"). */
        fun parseProbe(result: String?): Probe? {
            if (result.isNullOrEmpty() || result == "null") return null
            return try {
                val json = JSONObject(JSONTokener(result).nextValue() as? String ?: return null)
                val host = json.optString("host").takeIf { it.isNotEmpty() } ?: return null
                val icons = json.optJSONArray("icons")
                val candidates = (0 until (icons?.length() ?: 0)).mapNotNull { i ->
                    val o = icons!!.optJSONObject(i) ?: return@mapNotNull null
                    val href = o.optString("href").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    Candidate(href, o.optString("rel", "icon"), o.optString("sizes"), o.optString("type"))
                }
                Probe(host, candidates)
            } catch (e: Exception) {
                null
            }
        }
    }
}
