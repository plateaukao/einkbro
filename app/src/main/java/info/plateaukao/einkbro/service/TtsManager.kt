package info.plateaukao.einkbro.service

import android.content.Context
import android.speech.tts.TextToSpeech
import info.plateaukao.einkbro.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class TtsManager(
    private val context: Context
): KoinComponent {
    private val config: ConfigManager by inject()

    val tts: TextToSpeech by lazy {
        TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setSpeechRate(config.ttsSpeedValue/100f)
            }
        }
    }

    fun setSpeechRate(rate: Float): Int = tts.setSpeechRate(rate)

    fun readText(locale: Locale, text: String) {
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.language = locale

        text.replace("\\n", "").replace("\\\"", "")
            .chunked(TextToSpeech.getMaxSpeechInputLength())
            .forEach { chunk ->
                if (tts.isSpeaking)
                    tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, null)
                else {
                    tts.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
    }

    fun isSpeaking() = tts.isSpeaking

    fun stopReading() {
        tts.stop()
    }

    fun release() {
        stopReading()
        tts.shutdown()
    }
}
