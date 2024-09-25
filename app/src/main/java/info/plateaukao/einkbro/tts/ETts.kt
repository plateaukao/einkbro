package info.plateaukao.einkbro.tts

import android.util.Log
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.tts.entity.VoiceItem
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// ported from https://github.com/9ikj/Edge-TTS-Lib/
class ETts {
    private var headers: HashMap<String, String> = HashMap<String, String>().apply {
        put("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
        put("Pragma", "no-cache")
        put("Cache-Control", "no-cache")
        put(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Safari/537.36 Edg/99.0.1150.55"
        )
    }
    private val storage = EinkBroApplication.instance.cacheDir.absolutePath

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun tts(voice: VoiceItem, speed: Int, content: String): ByteArray? =
        suspendCoroutine { continuation ->
            val processedContent = removeIncompatibleCharacters(content)
            if (processedContent.isNullOrBlank()) {
                continuation.resume(null)
            }
            val storageFolder = File(storage)
            if (!storageFolder.exists()) {
                storageFolder.mkdirs()
            }

            val dateStr = dateToString(Date())
            val reqId = uuid()
            val audioFormat = mkAudioFormat(dateStr, FORMAT)
            val ssml = mkssml(
                voice.locale,
                voice.name,
                processedContent,
                "+0Hz",
                "+${speed - 100}%",
                "+0%"
            )
            val ssmlHeadersPlusData = ssmlHeadersPlusData(reqId, dateStr, ssml)

            val storageFile = File(storage)
            if (!storageFile.exists()) {
                storageFile.mkdirs()
            }

            val request = Request.Builder()
                .url("wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4")
                .headers(headers.toHeaders())
                .build()

            try {
                val client = okHttpClient.newWebSocket(
                    request,
                    object : TTSWebSocketListener() {
                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            continuation.resume(byteArray)
                        }
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            super.onFailure(webSocket, t, response)
                            response?.close()
                            Log.d("TTSWebSocketListener", "onFailure: ${t.message}")
                            continuation.resume(null)
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
        val sdf = SimpleDateFormat(
            "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.getDefault()
        )
        return sdf.format(date)
    }

    private fun uuid(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    private fun removeIncompatibleCharacters(input: String): String {
        if (input.isBlank()) {
            return ""
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

    private fun mkAudioFormat(dateStr: String, format: String): String =
        "X-Timestamp:" + dateStr + "\r\n" +
                "Content-Type:application/json; charset=utf-8\r\n" +
                "Path:speech.config\r\n\r\n" +
                "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"true\"},\"outputFormat\":\"" + format + "\"}}}}\n"


    private fun mkssml(
        locate: String,
        voiceName: String,
        content: String,
        voicePitch: String,
        voiceRate: String,
        voiceVolume: String,
    ): String =
        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='" +
                locate + "'>" +
                "<voice name='" + voiceName + "'><prosody pitch='" + voicePitch +
                "' rate='" + voiceRate + "' volume='" + voiceVolume + "'>" +
                content + "</prosody></voice></speak>"


    private fun ssmlHeadersPlusData(requestId: String, timestamp: String, ssml: String): String =
        "X-RequestId:" + requestId + "\r\n" +
                "Content-Type:application/ssml+xml\r\n" +
                "X-Timestamp:" + timestamp + "Z\r\n" +
                "Path:ssml\r\n\r\n" + ssml

    companion object {
        private const val FORMAT = "audio-24khz-48kbitrate-mono-mp3"
        private const val VOICES_LIST_URL =
            "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    }
}

private open class TTSWebSocketListener : WebSocketListener() {
    var byteArray: ByteArray = ByteArray(0)

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        webSocket.close(1000, null)
    }
    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.contains("Path:turn.end")) {
            webSocket.close(1000, null)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        response?.close()
        Log.d("TTSWebSocketListener", "onFailure: ${t.message}")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
        fixHeadHook(bytes.toByteArray())

    private fun fixHeadHook(origin: ByteArray) {
        val str = String(origin)
        val skip = when {
            str.contains("Content-Type") -> when {
                str.contains("audio/mpeg") -> 130
                str.contains("codec=opus") -> 142
                else -> 0
            }

            else -> 105
        }

        byteArray += Arrays.copyOfRange(origin, skip, origin.size)
    }
}
