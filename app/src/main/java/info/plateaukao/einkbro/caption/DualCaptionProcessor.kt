package info.plateaukao.einkbro.caption

import android.webkit.CookieManager
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.HttpURLConnection
import java.net.URL

class DualCaptionProcessor : KoinComponent {
    private val config: ConfigManager by inject()
    private val serializer = TimedText.serializer()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun processUrl(url: String, requestHeaders: Map<String, String>? = null): String? {
        if (!url.contains(urlWithCaption)) return null

        val rawCaption: ByteArray = runBlocking { fetchWithCookies(url, requestHeaders) }
        if (rawCaption.isEmpty()) return null

        if (config.tts.dualCaptionLocale.isEmpty()) return String(rawCaption)

        try {
            val newUrl = "$url&tlang=${config.tts.dualCaptionLocale}"
            val newCaption = runBlocking { fetchWithCookies(newUrl, requestHeaders) }
            val oldCaptionJson = json.decodeFromString(serializer, String(rawCaption))
            val newCaptionJson = json.decodeFromString(serializer, String(newCaption))

            oldCaptionJson.wsWinStyles.forEach {
                if (it.mhModeHint != null) {
                    it.mhModeHint = 0
                }
                if (it.sdScrollDir != null) {
                    it.sdScrollDir = 0
                }
            }

            oldCaptionJson.events.forEach { event ->
                if (event.segs != null && event.segs.isNotEmpty()) {
                    val first = event.segs.first()
                    first.utf8 = event.segs.map { it.utf8 }.reduce { acc, s -> acc + s }

                    val newCaptionSeg = newCaptionJson.events.firstOrNull { it.tStartMs == event.tStartMs }?.segs
                    if (!newCaptionSeg.isNullOrEmpty()) {
                        first.utf8 += ("\n" +
                                newCaptionSeg.map { it.utf8 }.reduce { acc, str -> acc + str })
                    }
                    event.segs.clear()
                    event.segs.add(first)
                }
            }

            return json.encodeToString(serializer, oldCaptionJson)
        } catch (exception: Exception) {
            // Dual-language merge failed (timeout, parse error, etc.). Fall back to
            // the single-language raw caption so capture still works and the player
            // still gets a valid response.
            return String(rawCaption)
        }
    }

    fun convertToHtml(jsonString: String): String {
        val timedText = json.decodeFromString(serializer, jsonString)
        val sb = StringBuilder()
        sb.append("<html><head><style>body{font-size: 1.5em;}</style></head><body>")
        timedText.events.forEach { event ->
            event.segs?.forEach { segment ->
                sb.append("${segment.utf8.replace("\n\n", "<br>").replace("\n", "<br>")}<br><br>")
            }
        }
        sb.append("</body></html>")
        return sb.toString().replace("<br><br><br>", "<br><br>").replace("<br><br><br>", "<br><br>")
    }

    private suspend fun fetchWithCookies(
        url: String,
        requestHeaders: Map<String, String>? = null,
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                if (requestHeaders.isNullOrEmpty()) {
                    connection.addRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                } else {
                    // Mirror the player's original request headers (User-Agent,
                    // Accept-Language, etc.) so the YouTube CDN sees the same
                    // signature it issued the timedtext URL for.
                    requestHeaders.forEach { (name, value) ->
                        when (name.lowercase()) {
                            // Hop-by-hop / forbidden headers HttpURLConnection sets
                            // itself or rejects.
                            "host", "content-length", "connection", "transfer-encoding",
                            "expect", "upgrade", "via",
                            // Cookie is attached below from CookieManager so we get
                            // the latest jar, not the snapshot in the request.
                            "cookie",
                            // Forwarding Accept-Encoding disables HttpURLConnection's
                            // transparent gzip decoding — we'd hand back compressed
                            // bytes the player can't parse.
                            "accept-encoding",
                            // Don't forward conditional-fetch headers; we always
                            // need the full body, never a 304.
                            "if-none-match", "if-modified-since",
                            "if-match", "if-unmodified-since" -> Unit
                            else -> connection.addRequestProperty(name, value)
                        }
                    }
                }
                CookieManager.getInstance().getCookie(url)?.let {
                    connection.addRequestProperty("Cookie", it)
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { it.readBytes() }
                } else ByteArray(0)
            } catch (e: Exception) {
                ByteArray(0)
            }
        }
    }

    companion object {
        const val urlWithCaption = "timedtext"
    }
}