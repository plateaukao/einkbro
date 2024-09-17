package info.plateaukao.einkbro.tts

import android.media.MediaPlayer
import icu.xmc.edgettslib.entity.VoiceItem
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.unit.HelperUnit
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// ported from https://github.com/9ikj/Edge-TTS-Lib/
class ETts private constructor() {

    companion object {
        private var sInstance: ETts? = null
            get() {
                if (field == null) {
                    field = ETts()
                }
                return field
            }

        fun getInstance(): ETts {
            return sInstance!!
        }

    }

    private val json = Json { ignoreUnknownKeys = true }
    private val voiceList: List<VoiceItem> = json.decodeFromString(
        HelperUnit.getStringFromAsset("eVoiceList.json")
    )

    private var headers: HashMap<String, String> = HashMap<String, String>().apply {
        put("Origin", UrlConstant.EDGE_ORIGIN)
        put("Pragma", "no-cache")
        put("Cache-Control", "no-cache")
        put("User-Agent", UrlConstant.EDGE_UA)
    }
    private lateinit var voice: VoiceItem
    private var format = "audio-24khz-48kbitrate-mono-mp3"
    private var findHeadHook = false
    private var voicePitch = "+0Hz"
    private var voiceRate = "+0%"
    private var voiceVolume = "+0%"
    private var storage = ""
    private var mediaPlayer: MediaPlayer? = null
    private var lastPlayMp3: File? = null

    private var request: Request? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun initialize(voice: VoiceItem? = null) {
        this.voice = voice ?: voiceList.first { it.ShortName == "en-US-RogerNeural" }
        storage = EinkBroApplication.instance.cacheDir.absolutePath
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnCompletionListener {
            lastPlayMp3?.delete()
        }
    }

    fun setVoicePitch(voicePitch: Int): ETts {
        this.voicePitch = "+${voicePitch}Hz"
        return this
    }

    fun setVoiceRate(voiceRate: Int): ETts {
        this.voiceRate = "+${voiceRate}%"
        return this
    }

    fun setVoiceVolume(voiceVolume: Int): ETts {
        this.voiceVolume = "+${voiceVolume}%"
        return this
    }

    fun formatMp3(): ETts {
        format = "audio-24khz-48kbitrate-mono-mp3"
        return this
    }

//    fun formatOpus(): TTS {
//        format = "webm-24khz-16bit-mono-opus"
//        return this
//    }

    fun findHeadHook(): ETts {
        findHeadHook = true
        return this
    }

    fun fixHeadHook(): ETts {
        findHeadHook = false
        return this
    }

    fun storage(storage: String): ETts {
        this.storage = storage
        return this
    }

    fun headers(headers: HashMap<String, String>): ETts {
        this.headers = headers
        return this
    }

    suspend fun tts(content: String): File? = suspendCoroutine { continuation ->
        val str = removeIncompatibleCharacters(content)
        if (str.isNullOrBlank()) {
            throw RuntimeException("invalid content")
        }
        val storageFolder = File(storage)
        if (!storageFolder.exists()) {
            storageFolder.mkdirs()
        }
        val dateStr = dateToString(Date())
        val reqId = uuid()
        val audioFormat = TtsUtil.mkAudioFormat(dateStr, format)
        val ssml = TtsUtil.mkssml(
            voice.Locale,
            voice.Name,
            content,
            voicePitch,
            voiceRate,
            voiceVolume
        )
        val ssmlHeadersPlusData = TtsUtil.ssmlHeadersPlusData(reqId, dateStr, ssml)
        var fileName = reqId
        if (format == "audio-24khz-48kbitrate-mono-mp3") {
            fileName += ".mp3"
        } else if (format == "webm-24khz-16bit-mono-opus") {
            fileName += ".opus"
        }
        val storageFile = File(storage)
        if (!storageFile.exists()) {
            storageFile.mkdirs()
        }
        if (request == null) {
            request = Request.Builder()
                .url(UrlConstant.EDGE_URL)
                .headers(headers.toHeaders())
                .build()
        }

        try {
            val client =
                okHttpClient.newWebSocket(
                    request!!,
                    object : TTSWebSocketListener(storage, fileName, findHeadHook) {
                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            val file = File(storage, fileName)
                            if (file.exists()) {
                                continuation.resume(file)
                            } else {
                                continuation.resume(null)
                            }
//                                mediaPlayer?.reset()
//                                mediaPlayer?.setDataSource(file.absolutePath)
//                                mediaPlayer?.prepare()
//                                mediaPlayer?.start()

                        }
                    })
            client.send(audioFormat)
            client.send(ssmlHeadersPlusData)
        } catch (e: Throwable) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

    private fun dateToString(date: Date): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.getDefault())
        return sdf.format(date)
    }

    private fun uuid(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    private fun removeIncompatibleCharacters(input: String): String? {
        if (input.isBlank()) {
            return null
        }
        val output = StringBuilder()
        for (element in input) {
            val code = element.code
            if (code in 0..8 || code in 11..12 || code in 14..31) {
                output.append(" ")
            } else {
                output.append(element)
            }
        }
        return output.toString()
    }
}