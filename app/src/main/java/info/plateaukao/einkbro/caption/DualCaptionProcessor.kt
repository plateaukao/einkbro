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

    fun processUrl(url: String): String? {
        if (!url.contains(urlWithCaption)) return null

        if (config.dualCaptionLocale.isEmpty()) return runBlocking { String(fetchWithCookies(url)) }

        try {
            val newUrl = "$url&tlang=${config.dualCaptionLocale}"
            val oldCaption = runBlocking { fetchWithCookies(url) }
            val newCaption = runBlocking { fetchWithCookies(newUrl) }
            val oldCaptionJson = json.decodeFromString(serializer, String(oldCaption))
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
            return null
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

    private suspend fun fetchWithCookies(url: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                CookieManager.getInstance().getCookie(url)?.let {
                    connection.addRequestProperty("Cookie", it)
                }
                connection.connectTimeout = 10000
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