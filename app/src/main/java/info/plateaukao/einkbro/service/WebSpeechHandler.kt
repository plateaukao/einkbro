package info.plateaukao.einkbro.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.tts.ByteArrayMediaDataSource
import info.plateaukao.einkbro.tts.CustomMediaPlayer
import info.plateaukao.einkbro.tts.ETts
import info.plateaukao.einkbro.tts.entity.VoiceItem
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TtsType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Native backend for the Web Speech API polyfill
 * (assets/speech_synthesis_polyfill.js). Speaks single utterances requested by
 * page JavaScript with the engine chosen in the app's TTS settings: system
 * TextToSpeech, Edge-TTS, or OpenAI TTS.
 *
 * Unlike TtsViewModel's article reader this handles one short utterance at a
 * time (queueing lives on the JS side) and honors the page-requested language,
 * rate, and voice instead of the reader's locale settings. It owns a dedicated
 * TextToSpeech instance so switching to a page-requested language can't disturb
 * an ongoing article read in TtsManager.
 */
class WebSpeechHandler(private val context: Context) : KoinComponent {
    private val config: ConfigManager by inject()
    private val coroutineScope: CoroutineScope by inject()

    private val eTts: ETts by lazy { ETts() }
    private val openAiRepository: OpenAiRepository by lazy { OpenAiRepository() }
    private val mainHandler = Handler(Looper.getMainLooper())

    // All MediaPlayer calls must stay on the main thread: playAudio drives it
    // there, and MediaPlayer is not safe against concurrent cross-thread calls.
    private var mediaPlayer: CustomMediaPlayer? = null

    private var systemTts: TextToSpeech? = null
    private var systemTtsInit: CompletableDeferred<Boolean>? = null

    private var speakJob: Job? = null
    private var webViewRef: WeakReference<EBWebView>? = null

    private val edgeVoices: List<VoiceItem> by lazy {
        try {
            Json.decodeFromString(HelperUnit.getStringFromAsset("eVoiceList.json"))
        } catch (e: Exception) {
            Log.e(TAG, "failed to load eVoiceList.json", e)
            emptyList()
        }
    }

    private fun resolveType(): TtsType =
        if (config.tts.useOpenAiTts && config.ai.gptApiKey.isNotBlank()) TtsType.GPT
        else config.tts.ttsType

    /** Called on the WebView JS bridge thread; must return quickly. */
    fun getVoicesJson(): String = when (resolveType()) {
        TtsType.SYSTEM -> systemVoicesJson()
        TtsType.ETTS -> edgeVoicesJson()
        // GPT voices are fixed multilingual personas with no language list;
        // speak() still works, pages just can't pick a per-language voice.
        TtsType.GPT -> "[]"
    }

    @Synchronized
    fun speak(
        webView: EBWebView,
        text: String,
        lang: String,
        rate: Float,
        pitch: Float,
        voiceName: String,
        utteranceId: String,
    ) {
        cancel()
        webViewRef = WeakReference(webView)
        val type = resolveType()
        // NaN passes through coerceIn unchanged (NaN comparisons are false), so
        // guard against a page calling the bridge directly with bad numbers.
        val safeRate = if (rate.isFinite()) rate else 1f
        val safePitch = if (pitch.isFinite()) pitch else 1f
        speakJob = coroutineScope.launch {
            when (type) {
                TtsType.SYSTEM -> speakBySystemTts(text, lang, safeRate, safePitch, utteranceId)
                else -> speakByByteEngine(type, text, lang, safeRate, utteranceId, voiceName)
            }
        }
    }

    @Synchronized
    fun cancel() {
        speakJob?.cancel()
        speakJob = null
        runCatching { systemTts?.stop() }
        // Resetting also unblocks a coroutine suspended in playAudio via the
        // reset listener. The pending speakJob's playback can't start before
        // this posted reset runs: its audio fetch alone takes longer.
        mainHandler.post { runCatching { mediaPlayer?.reset() } }
    }

    /** Pauses byte-engine playback; system TTS has no pause and keeps speaking. */
    fun pause() {
        mainHandler.post { runCatching { mediaPlayer?.takeIf { it.isPlaying }?.pause() } }
    }

    fun resume() {
        mainHandler.post { runCatching { mediaPlayer?.takeIf { !it.isPlaying }?.start() } }
    }

    private suspend fun speakBySystemTts(
        text: String,
        lang: String,
        rate: Float,
        pitch: Float,
        utteranceId: String,
    ) {
        val ready = withTimeoutOrNull(SYSTEM_TTS_INIT_TIMEOUT_MS) { ensureSystemTtsInit().await() } ?: false
        val tts = systemTts
        if (!ready || tts == null) {
            dispatchEvent(utteranceId, "error")
            return
        }

        val locale = if (lang.isNotBlank()) Locale.forLanguageTag(lang.replace('_', '-')) else config.tts.ttsLocale
        val result = runCatching { tts.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "no system voice for $locale")
            dispatchEvent(utteranceId, "error")
            return
        }
        tts.setSpeechRate(rate.coerceIn(0.1f, 4f))
        tts.setPitch(pitch.coerceIn(0.5f, 2f))
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) = dispatchEvent(id, "start")
            override fun onDone(id: String) = dispatchEvent(id, "end")
            @Deprecated("Deprecated in Java")
            override fun onError(id: String) = dispatchEvent(id, "error")
        })
        // Don't speak an utterance that was cancelled while awaiting engine init.
        coroutineContext.ensureActive()
        // A synchronous failure never reaches the progress listener; without the
        // error event the polyfill's queue would stall forever.
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS) {
            dispatchEvent(utteranceId, "error")
        }
    }

    private suspend fun speakByByteEngine(
        type: TtsType,
        text: String,
        lang: String,
        rate: Float,
        utteranceId: String,
        voiceName: String,
    ) {
        val byteArray = withContext(Dispatchers.IO) {
            if (type == TtsType.ETTS) {
                // ETts speed is percent, 100 = normal — same scale as utterance.rate * 100.
                val speed = (rate * 100).toInt().coerceIn(20, 300)
                eTts.tts(resolveEdgeVoice(lang, voiceName), speed, text)
            } else {
                openAiRepository.tts(text)
            }
        }
        if (byteArray == null || byteArray.isEmpty()) {
            Log.e(TAG, "no audio returned for utterance $utteranceId")
            dispatchEvent(utteranceId, "error")
            return
        }
        dispatchEvent(utteranceId, "start")
        withContext(Dispatchers.Main) { playAudio(byteArray) }
        dispatchEvent(utteranceId, "end")
    }

    /**
     * Voice priority: the page's explicitly chosen voice, then the user's
     * configured Edge voice when it matches the requested language (so a
     * hand-picked ar-EG voice serves an ar-SA page), then any voice for the
     * exact locale, then any for the language, then the configured voice
     * (the multilingual default can speak most languages).
     */
    private fun resolveEdgeVoice(lang: String, voiceName: String): VoiceItem {
        val configured = config.tts.ettsVoice
        if (voiceName.isNotBlank()) {
            edgeVoices.firstOrNull { it.shortName.equals(voiceName, ignoreCase = true) }?.let { return it }
        }
        if (lang.isBlank()) return configured
        val locale = lang.replace('_', '-')
        val language = locale.substringBefore('-')
        if (configured.getLanguageCode().equals(language, ignoreCase = true)) return configured
        return edgeVoices.firstOrNull { it.locale.equals(locale, ignoreCase = true) }
            ?: edgeVoices.firstOrNull { it.getLanguageCode().equals(language, ignoreCase = true) }
            ?: configured
    }

    private suspend fun playAudio(byteArray: ByteArray) = suspendCoroutine { cont ->
        val player = mediaPlayer ?: CustomMediaPlayer().also { mediaPlayer = it }
        try {
            player.setOnResetListener {
                player.setOnResetListener { }
                cont.resume(Unit)
            }
            player.setDataSource(ByteArrayMediaDataSource(byteArray))
            player.setOnPreparedListener { player.start() }
            player.prepare()
            player.setOnCompletionListener { player.reset() }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "playAudio error: $what, $extra")
                player.reset()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "playAudio exception: ${e.message}")
            player.setOnResetListener { }
            runCatching { player.reset() }
            cont.resume(Unit)
        }
    }

    @Synchronized
    private fun ensureSystemTtsInit(): CompletableDeferred<Boolean> {
        systemTtsInit?.let { return it }
        val deferred = CompletableDeferred<Boolean>()
        systemTtsInit = deferred
        coroutineScope.launch(Dispatchers.Main) {
            systemTts = TextToSpeech(context) { status ->
                deferred.complete(status == TextToSpeech.SUCCESS)
            }
        }
        return deferred
    }

    private fun systemVoicesJson(): String {
        val initDeferred = ensureSystemTtsInit()
        // Engine still initializing: report nothing yet; the polyfill retries
        // and fires voiceschanged once the list is available.
        if (!initDeferred.isCompleted) return "[]"
        val tts = systemTts ?: return "[]"
        val locales = runCatching { tts.availableLanguages?.toList() }.getOrNull() ?: emptyList()
        val array = JSONArray()
        locales.forEach { locale ->
            array.put(JSONObject().apply {
                put("name", locale.displayName)
                put("lang", locale.toLanguageTag())
                put("localService", true)
                put("default", locale.language == config.tts.ttsLocale.language)
            })
        }
        return array.toString()
    }

    private fun edgeVoicesJson(): String {
        val configuredShortName = config.tts.ettsVoice.shortName
        val array = JSONArray()
        edgeVoices.forEach { voice ->
            array.put(JSONObject().apply {
                put("name", voice.shortName)
                put("lang", voice.locale)
                put("localService", false)
                put("default", voice.shortName == configuredShortName)
            })
        }
        return array.toString()
    }

    // evaluateJavascript reaches the main frame only: an utterance spoken from
    // an iframe plays but its start/end events are dropped (the main frame's
    // polyfill has no matching utterance and ignores them).
    private fun dispatchEvent(utteranceId: String, event: String) {
        val webView = webViewRef?.get() ?: return
        // Ids are polyfill-generated counters; anything else never gets events.
        val id = utteranceId.takeIf { it.matches(UTTERANCE_ID_REGEX) } ?: return
        webView.post {
            if (webView.isAttachedToWindow) {
                webView.evaluateJavascript(
                    "window.__ebTts && window.__ebTts.dispatch('$id', '$event')",
                    null
                )
            }
        }
    }

    companion object {
        private const val TAG = "WebSpeechHandler"
        private const val SYSTEM_TTS_INIT_TIMEOUT_MS = 5000L
        val UTTERANCE_ID_REGEX = Regex("^[0-9]{1,12}$")
    }
}
