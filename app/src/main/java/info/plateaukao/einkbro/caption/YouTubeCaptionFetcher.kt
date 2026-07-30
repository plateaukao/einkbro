package info.plateaukao.einkbro.caption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

@Serializable
data class YouTubeCaptionTrack(
    val baseUrl: String = "",
    val languageCode: String = "",
    val kind: String = "",
)

@Serializable
private data class PlayerResponse(val captions: PlayerCaptions = PlayerCaptions())

@Serializable
private data class PlayerCaptions(
    @SerialName("playerCaptionsTracklistRenderer")
    val tracklist: CaptionTracklist = CaptionTracklist(),
)

@Serializable
private data class CaptionTracklist(val captionTracks: List<YouTubeCaptionTrack> = emptyList())

/**
 * Actively fetches the caption transcript of a YouTube video so AI features can work
 * on it instead of the noisy watch-page DOM. Caption tracks are looked up through the
 * InnerTube player API with the ANDROID client: the web player's timedtext URLs (in
 * ytInitialPlayerResponse or the watch page HTML) are gated by a proof-of-origin
 * token and return an empty body when fetched outside the player, while the ANDROID
 * client's URLs are served without it.
 */
class YouTubeCaptionFetcher {
    private val dualCaptionProcessor = DualCaptionProcessor()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCaptionJson(pageUrl: String): String? {
        val videoId = extractVideoId(pageUrl) ?: return null
        val tracks = fetchCaptionTracks(videoId) ?: return null
        val track = pickTrack(tracks, Locale.getDefault().language) ?: return null
        val captionJson = withContext(Dispatchers.IO) {
            dualCaptionProcessor.processUrl(captionUrl(track.baseUrl))
        } ?: return null
        return captionJson.takeIf(::isTimedTextWithContent)
    }

    private suspend fun fetchCaptionTracks(videoId: String): List<YouTubeCaptionTrack>? =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(PLAYER_API_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", ANDROID_CLIENT_USER_AGENT)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.outputStream.use { it.write(playerRequestBody(videoId).toByteArray()) }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.use { String(it.readBytes()) }
                json.decodeFromString(PlayerResponse.serializer(), body)
                    .captions.tracklist.captionTracks
                    .filter { it.baseUrl.isNotEmpty() }
                    .ifEmpty { null }
            } catch (e: Exception) {
                null
            }
        }

    private fun isTimedTextWithContent(jsonString: String): Boolean = try {
        json.decodeFromString(TimedText.serializer(), jsonString)
            .events.any { event -> event.segs?.any { it.utf8.isNotBlank() } == true }
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val PLAYER_API_URL = "https://www.youtube.com/youtubei/v1/player"
        private const val ANDROID_CLIENT_USER_AGENT =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"
        private val videoIdPattern = Regex("^[A-Za-z0-9_-]{6,20}$")

        private fun playerRequestBody(videoId: String): String =
            """{"context":{"client":{"clientName":"ANDROID","clientVersion":"20.10.38","androidSdkVersion":30,"hl":"en"}},"videoId":"$videoId"}"""

        fun isVideoUrl(url: String?): Boolean = url != null && extractVideoId(url) != null

        fun extractVideoId(url: String): String? = try {
            val uri = URI(url)
            val host = uri.host.orEmpty().removePrefix("www.").removePrefix("m.")
            val path = uri.path.orEmpty()
            when {
                host == "youtu.be" -> path.trim('/').substringBefore('/')

                host.endsWith("youtube.com") && path == "/watch" ->
                    uri.rawQuery.orEmpty().split('&')
                        .firstOrNull { it.startsWith("v=") }
                        ?.substringAfter('=')

                host.endsWith("youtube.com") &&
                        (path.startsWith("/shorts/") || path.startsWith("/live/")) ->
                    path.split('/').getOrNull(2)

                else -> null
            }?.takeIf { videoIdPattern.matches(it) }
        } catch (e: Exception) {
            null
        }

        fun pickTrack(
            tracks: List<YouTubeCaptionTrack>,
            preferredLanguage: String,
        ): YouTubeCaptionTrack? {
            val matchesLanguage = { track: YouTubeCaptionTrack ->
                preferredLanguage.isNotBlank() && track.languageCode.startsWith(preferredLanguage)
            }
            return tracks.firstOrNull { it.kind != "asr" && matchesLanguage(it) }
                ?: tracks.firstOrNull { it.kind != "asr" }
                ?: tracks.firstOrNull(matchesLanguage)
                ?: tracks.firstOrNull()
        }

        // The ANDROID client hands out srv3 (XML) URLs; TimedText needs json3.
        fun captionUrl(baseUrl: String): String {
            val absolute =
                if (baseUrl.startsWith("/")) "https://www.youtube.com$baseUrl" else baseUrl
            return if (absolute.contains("fmt=")) {
                absolute.replace(Regex("fmt=[^&]*"), "fmt=json3")
            } else {
                "$absolute&fmt=json3"
            }
        }
    }
}
