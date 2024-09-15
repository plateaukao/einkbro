package info.plateaukao.einkbro.viewmodel

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TtsViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    private val ttsManager: TtsManager by inject()

    private val mediaPlayer by lazy { MediaPlayer() }
    private var byteArrayChannel: Channel<ByteArray>? = null

    private val _speakingState = MutableStateFlow(false)
    val speakingState: StateFlow<Boolean> = _speakingState.asStateFlow()

    private val openaiRepository: OpenAiRepository by lazy { OpenAiRepository() }

    private fun useOpenAiTts(): Boolean = config.useOpenAiTts && config.gptApiKey.isNotBlank()

    fun readText(text: String) {
        if (isSpeaking()) {
            stop()
            return
        }

        if (useOpenAiTts()) {
            viewModelScope.launch {
                readTextByGpt(text);
            }
            return
        }

//        if (Build.MODEL.startsWith("Pixel 8")) {
//            IntentUnit.tts(context as Activity, text)
//            return
//        }

        _speakingState.value = true
        ttsManager.readText(text)
        viewModelScope.launch {
            while (ttsManager.isSpeaking()) {
                delay(2000)
            }
            _speakingState.value = false
        }
    }

    private fun readTextByGpt(text: String) {
        byteArrayChannel = Channel(3)
        viewModelScope.launch(Dispatchers.IO) {
            val sentences: List<String> = text.split("(?<=\\.)|(?<=。)".toRegex())

            _speakingState.value = true
            for (sentence in sentences) {
                val data = openaiRepository.tts(sentence)
                if (data != null && byteArrayChannel != null) {
                    byteArrayChannel?.send(data)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            for (data in byteArrayChannel!!) {
                playAudio(data)
                delay(200)
            }
            delay(2000)
            _speakingState.value = false
            byteArrayChannel = null
        }
    }

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)

    fun stop() {
        ttsManager.stopReading()

        byteArrayChannel?.cancel()
        byteArrayChannel = null
        mediaPlayer.stop()
        mediaPlayer.reset()

        _speakingState.value = false
    }

    fun isSpeaking(): Boolean {
        return ttsManager.isSpeaking() || byteArrayChannel != null
    }

    fun getAvailableLanguages(): List<Locale> = ttsManager.getAvailableLanguages()

    private suspend fun playAudio(data: ByteArray) = suspendCoroutine { cont ->
        val tempFile = generateTempFile(data)

        FileInputStream(tempFile).use { fis ->
            mediaPlayer.setDataSource(fis.fd)
            mediaPlayer.prepare()
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                tempFile.delete()
                mediaPlayer.reset()
                cont.resume(0)
            }
        }
    }

    private fun generateTempFile(data: ByteArray): File {
        val tempFile = File.createTempFile("temp", "aac", EinkBroApplication.instance.cacheDir)
        FileOutputStream(tempFile).use { it.write(data) }

        return tempFile
    }
}