package info.plateaukao.einkbro.service

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class TtsManager(
    private val context: Context
) {
    val tts: TextToSpeech by lazy {
        TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }
    }

    fun readText(text: String) {
        if (tts.isSpeaking) {
            tts.stop()
        }

        text.chunked(TextToSpeech.getMaxSpeechInputLength())
            .forEach { chunk ->
                if (tts.isSpeaking)
                    tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, null)
                else
                    tts.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, null)
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