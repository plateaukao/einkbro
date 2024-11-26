package info.plateaukao.einkbro.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.processedTextToChunks
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TtsManager(
    private val context: Context,
) : KoinComponent {
    private val config: ConfigManager by inject()

    private var isInitialized: Boolean = false
    private var isPreparing: Boolean = false

    private lateinit var tts: TextToSpeech

    private val utterIdToTextMap = mutableMapOf<Int, String>()


    init {
        GlobalScope.launch {
            delay(1000)
            tts = TextToSpeech(context) {
                if (it == TextToSpeech.SUCCESS) {
                    isInitialized = true
                }
            }
        }
    }

    fun setSpeechRate(rate: Float): Int = tts.setSpeechRate(rate)

    fun getAvailableLanguages(): List<Locale> = tts.availableLanguages?.toList() ?: emptyList()

    private var utterId = 0
    private var isStopped = false
    suspend fun readText(
        text: String,
        onProgress: (Int, Int, String) -> Unit,
    ) = suspendCoroutine { cont ->
        isPreparing = true
        isStopped = false

        tts.language = config.ttsLocale
        tts.setSpeechRate(config.ttsSpeedValue / 100f)

        val chunks = processedTextToChunks(text)

        val currentUtterId = utterId

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                isPreparing = false
                onProgress(
                    (utteranceId.toInt() - currentUtterId) + 1,
                    chunks.size,
                    utterIdToTextMap[utteranceId.toInt()] ?: ""
                )
                Log.d("TtsManager", "Start speaking $utteranceId")
            }

            override fun onDone(utteranceId: String) {
                if (utteranceId.toInt() == utterId - 1) {
                    Log.d("TtsManager", "complete speaking $utteranceId")
                    utterIdToTextMap.remove(utteranceId.toInt())
                    cont.resume(Unit)
                }
                Log.d("TtsManager", "Done speaking $utteranceId")
            }

            override fun onError(utteranceId: String) {
                utterIdToTextMap.remove(utteranceId.toInt())
                Log.e("TtsManager", "Error on utterance $utteranceId")
            }
        })

        for (chunk in chunks) {
            if (isStopped) break

            utterIdToTextMap[utterId] = chunk
            if (tts.isSpeaking) {
                tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, utterId.toString())
            } else {
                tts.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, utterId.toString())
            }
            Log.d("TtsManager", "Add to Speak $chunk")
            utterId++
        }
    }

    fun isSpeaking(): Boolean = isInitialized && (isPreparing || tts.isSpeaking)

    fun stopReading() {
        isStopped = true
        tts.stop()
    }

    fun release() {
        stopReading()
        tts.shutdown()
    }
}
