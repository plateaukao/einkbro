package info.plateaukao.einkbro.caption

import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.VideoTranscript
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
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
private data class PlayerResponse(
    val captions: PlayerCaptions = PlayerCaptions(),
    val playabilityStatus: PlayabilityStatus = PlayabilityStatus(),
)

@Serializable
private data class PlayabilityStatus(
    val status: String = "",
    val reason: String = "",
)

@Serializable
private data class PlayerCaptions(
    @SerialName("playerCaptionsTracklistRenderer")
    val tracklist: CaptionTracklist = CaptionTracklist(),
)

@Serializable
private data class CaptionTracklist(val captionTracks: List<YouTubeCaptionTrack> = emptyList())

@Serializable
private data class GeminiFileData(@SerialName("file_uri") val fileUri: String)

@Serializable
private data class GeminiPart(
    val text: String? = null,
    @SerialName("file_data") val fileData: GeminiFileData? = null,
)

@Serializable
private data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
private data class GeminiSafetySetting(val category: String, val threshold: String)

@Serializable
private data class GeminiThinkingConfig(val thinkingBudget: Int)

@Serializable
private data class GeminiGenerationConfig(
    val thinkingConfig: GeminiThinkingConfig,
    val mediaResolution: String,
)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val safety_settings: List<GeminiSafetySetting>,
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
private data class GeminiCandidate(val content: GeminiResponseContent = GeminiResponseContent())

@Serializable
private data class GeminiResponseContent(val parts: List<GeminiResponsePart> = emptyList())

@Serializable
private data class GeminiResponsePart(val text: String = "")

@Serializable
private data class GeminiErrorResponse(val error: GeminiErrorBody = GeminiErrorBody())

@Serializable
private data class GeminiErrorBody(val message: String = "", val status: String = "")

/** Outcome of a caption/transcript lookup for a video page. */
sealed interface CaptionFetchResult {
    /** Timedtext JSON in the shape EBWebView.dualCaption expects. */
    data class Captions(val timedTextJson: String) : CaptionFetchResult

    /** No captions and nothing to transcribe with — fall back to page text quietly. */
    data object None : CaptionFetchResult

    /**
     * Captions/transcription were expected but could not be obtained; [message] is
     * user-showable. [transient] failures (rate limit, network) may succeed on a
     * later attempt and shouldn't be remembered against the video.
     */
    data class Failed(val message: String, val transient: Boolean) : CaptionFetchResult
}

/**
 * Actively fetches the caption transcript of a YouTube video so AI features can work
 * on it instead of the noisy watch-page DOM. Caption tracks are looked up through the
 * InnerTube player API with the ANDROID client: the web player's timedtext URLs (in
 * ytInitialPlayerResponse or the watch page HTML) are gated by a proof-of-origin
 * token and return an empty body when fetched outside the player, while the ANDROID
 * client's URLs are served without it.
 *
 * For videos with no caption track at all, falls back to asking Gemini to watch the
 * video (the API accepts public YouTube URLs directly) when a Gemini key is
 * configured. Generated transcripts are cached permanently in Room so a video is
 * only ever transcribed once.
 */
class YouTubeCaptionFetcher : KoinComponent {
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val dualCaptionProcessor = DualCaptionProcessor()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns [CaptionFetchResult.Captions] with timedtext JSON (the [TimedText]
     * shape stored in EBWebView.dualCaption) when a caption track or transcript is
     * available. [onGeminiTranscribe] fires on the main thread right before a (slow)
     * Gemini transcription request starts — not on cache hits — so callers can tell
     * the user why the AI action is taking minutes instead of seconds.
     */
    suspend fun fetchCaption(
        pageUrl: String,
        onGeminiTranscribe: (() -> Unit)? = null,
    ): CaptionFetchResult {
        val videoId = extractVideoId(pageUrl) ?: return CaptionFetchResult.None
        val player = fetchPlayerResponse(videoId)
        val tracks = player?.captions?.tracklist?.captionTracks
            ?.filter { it.baseUrl.isNotEmpty() }
            .orEmpty()
        fetchTimedTextFromTracks(tracks)?.let { return CaptionFetchResult.Captions(it) }

        // Gemini can only watch videos the anonymous YouTube API can play. For
        // members-only / private / age-gated videos it would return a bare 403, so
        // report YouTube's own reason instead of attempting a doomed transcription.
        val playability = player?.playabilityStatus
        if (playability != null && playability.status.isNotEmpty() && playability.status != "OK") {
            return CaptionFetchResult.Failed(
                playability.reason.ifBlank { playability.status },
                transient = false,
            )
        }
        return fetchTranscriptWithGemini(videoId, onGeminiTranscribe)
    }

    private suspend fun fetchTimedTextFromTracks(tracks: List<YouTubeCaptionTrack>): String? {
        val track = pickTrack(tracks, Locale.getDefault().language) ?: return null
        val captionJson = withContext(Dispatchers.IO) {
            dualCaptionProcessor.processUrl(captionUrl(track.baseUrl))
        } ?: return null
        return captionJson.takeIf(::isTimedTextWithContent)
    }

    private suspend fun fetchPlayerResponse(videoId: String): PlayerResponse? =
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
            } catch (e: Exception) {
                Timber.w(e, "YouTube player API request failed")
                null
            }
        }

    private suspend fun fetchTranscriptWithGemini(
        videoId: String,
        onTranscribeStart: (() -> Unit)?,
    ): CaptionFetchResult {
        if (config.ai.geminiApiKey.isBlank()) return CaptionFetchResult.None

        bookmarkManager.getVideoTranscript(videoId)?.let {
            return CaptionFetchResult.Captions(transcriptToTimedTextJson(it.transcript))
        }

        onTranscribeStart?.let { withContext(Dispatchers.Main) { it() } }
        return when (val outcome = requestGeminiTranscript(videoId)) {
            is GeminiOutcome.Transcript -> {
                bookmarkManager.insertVideoTranscript(
                    VideoTranscript(videoId, outcome.text, System.currentTimeMillis())
                )
                CaptionFetchResult.Captions(transcriptToTimedTextJson(outcome.text))
            }
            // The transcription prompt asks for empty output when there's no speech;
            // fall back to page text without complaining.
            GeminiOutcome.NoSpeech -> CaptionFetchResult.None
            is GeminiOutcome.Error ->
                CaptionFetchResult.Failed(outcome.message, outcome.transient)
        }
    }

    private sealed interface GeminiOutcome {
        data class Transcript(val text: String) : GeminiOutcome
        data object NoSpeech : GeminiOutcome
        data class Error(val message: String, val transient: Boolean) : GeminiOutcome
    }

    private suspend fun requestGeminiTranscript(videoId: String): GeminiOutcome =
        withContext(Dispatchers.IO) {
            try {
                val model = config.ai.geminiModel.ifBlank { "gemini-3.5-flash-lite" }
                val connection = URL("$GEMINI_API_PREFIX$model:generateContent")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("x-goog-api-key", config.ai.geminiApiKey)
                connection.connectTimeout = 15000
                // Gemini has to ingest the whole video before answering.
                connection.readTimeout = 300000
                connection.outputStream.use {
                    it.write(geminiRequestBody(videoId).toByteArray())
                }
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream
                        ?.use { String(it.readBytes()) }
                        .orEmpty()
                    Timber.e("Gemini transcription failed ($code): $errorBody")
                    return@withContext GeminiOutcome.Error(
                        "Gemini ($code): ${parseGeminiError(errorBody)}",
                        transient = code == 429 || code >= 500,
                    )
                }
                val body = connection.inputStream.use { String(it.readBytes()) }
                val text = json.decodeFromString(GeminiResponse.serializer(), body)
                    .candidates.firstOrNull()
                    ?.content?.parts
                    ?.joinToString("") { it.text }
                    ?.trim()
                if (text.isNullOrBlank()) GeminiOutcome.NoSpeech
                else GeminiOutcome.Transcript(text)
            } catch (e: Exception) {
                Timber.e(e, "Gemini transcription request failed")
                GeminiOutcome.Error(e.message ?: "network error", transient = true)
            }
        }

    private fun parseGeminiError(errorBody: String): String = try {
        val error = json.decodeFromString(GeminiErrorResponse.serializer(), errorBody).error
        error.message.ifBlank { error.status.ifBlank { "unknown error" } }
    } catch (e: Exception) {
        "unknown error"
    }

    private fun geminiRequestBody(videoId: String): String {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(fileData = GeminiFileData(fileUri = "https://www.youtube.com/watch?v=$videoId")),
                        GeminiPart(text = TRANSCRIBE_PROMPT),
                    )
                )
            ),
            safety_settings = listOf(
                GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
                GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE"),
            ),
            generationConfig = GeminiGenerationConfig(
                thinkingConfig = GeminiThinkingConfig(thinkingBudget = 0),
                // Transcription only needs the audio track; low resolution cuts the
                // video-frame token cost roughly 4x, which matters on the free tier.
                mediaResolution = "MEDIA_RESOLUTION_LOW",
            ),
        )
        return json.encodeToString(GeminiRequest.serializer(), request)
    }

    private fun isTimedTextWithContent(jsonString: String): Boolean = try {
        json.decodeFromString(TimedText.serializer(), jsonString)
            .events.any { event -> event.segs?.any { it.utf8.isNotBlank() } == true }
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val PLAYER_API_URL = "https://www.youtube.com/youtubei/v1/player"
        private const val GEMINI_API_PREFIX =
            "https://generativelanguage.googleapis.com/v1beta/models/"
        private const val ANDROID_CLIENT_USER_AGENT =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"
        private const val TRANSCRIBE_PROMPT =
            "Transcribe all spoken content of this video in its original spoken language. " +
                    "Output only the transcript as plain text paragraphs. Do not include " +
                    "timestamps, speaker labels, or any commentary. If the video has no " +
                    "speech, output nothing."
        private val videoIdPattern = Regex("^[A-Za-z0-9_-]{6,20}$")
        private val timedTextJson = Json { ignoreUnknownKeys = true }

        // hl localizes playabilityStatus.reason, which is shown to the user on failure.
        private fun playerRequestBody(videoId: String): String {
            val hl = Locale.getDefault().language.ifBlank { "en" }
            return """{"context":{"client":{"clientName":"ANDROID","clientVersion":"20.10.38","androidSdkVersion":30,"hl":"$hl"}},"videoId":"$videoId"}"""
        }

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

        // Wraps a plain-text transcript in the TimedText shape so it flows through the
        // same dualCaption pipeline (getRawText, TTS, EPUB export) as real captions.
        fun transcriptToTimedTextJson(transcript: String): String {
            val events = transcript.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line -> Event(segs = mutableListOf(Segment(utf8 = line))) }
            return timedTextJson.encodeToString(
                TimedText.serializer(),
                TimedText(events = events.toMutableList())
            )
        }
    }
}
