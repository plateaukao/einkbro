package info.plateaukao.einkbro.viewmodel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.tts.ETts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

    private val eTts: ETts = ETts()

    private val mediaPlayer by lazy { MediaPlayer() }
    private var audioFileChannel: Channel<File>? = null

    private val _speakingState = MutableStateFlow(false)
    val speakingState: StateFlow<Boolean> = _speakingState.asStateFlow()

    private val openaiRepository: OpenAiRepository by lazy { OpenAiRepository() }

    private fun useOpenAiTts(): Boolean = config.useOpenAiTts && config.gptApiKey.isNotBlank()

    fun readText(text: String) {
        if (isSpeaking()) {
            stop()
            return
        }

        val type = if (useOpenAiTts()) TtsType.GPT else config.ttsType

        when (type) {
            TtsType.ETTS,
            TtsType.GPT,
            -> readByEngine(type, text)

            TtsType.SYSTEM -> readBySystemTts(text)
        }

//        if (Build.MODEL.startsWith("Pixel 8")) {
//            IntentUnit.tts(context as Activity, text)
//            return
//        }
    }

    private fun readBySystemTts(text: String) {
        _speakingState.value = true
        ttsManager.readText(text)
        viewModelScope.launch {
            while (ttsManager.isSpeaking()) {
                delay(2000)
            }
            _speakingState.value = false
        }
    }

    private fun readByEngine(ttsType: TtsType, text: String) {
        _speakingState.value = true
        audioFileChannel = Channel(1)
        val processedText = text.replace("\\n", " ").replace("\\\"", "").replace("\\t", "")
        viewModelScope.launch(Dispatchers.IO) {
            val sentences: List<String> =
                processedText.split("(?<=\\.)|(?<=。)|(?<=？)|(?<=\\?)".toRegex())


            _speakingState.value = true
            for (sentence in sentences) {
                if (audioFileChannel == null) break
                fetchSemaphore.withPermit {
                    Log.d("TtsViewModel", "tts sentence fetch: $sentence")
                    val file = if (ttsType == TtsType.ETTS) {
                        eTts.tts(config.ettsVoice, config.ttsSpeedValue, sentence)
                    } else {
                        openaiRepository.tts(sentence)?.let { data ->
                            generateTempFile(data)
                        }
                    }

                    if (file != null) {
                        Log.d("TtsViewModel", "tts sentence send: $sentence")
                        audioFileChannel?.send(file)
                        Log.d("TtsViewModel", "tts sentence sent: $sentence")
                    }
                }
            }
            audioFileChannel?.close()
        }

        viewModelScope.launch(Dispatchers.IO) {
            var index = 0
            for (file in audioFileChannel!!) {
                Log.d("TtsViewModel", "play audio $index")
                playAudioFile(file)
                delay(100)
                index++
                if (audioFileChannel?.isClosedForSend == true &&
                    audioFileChannel?.isEmpty == true
                ) break
            }
            _speakingState.value = false
            audioFileChannel = null
        }
    }

    private val fetchSemaphore = Semaphore(3)

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)

    fun stop() {
        ttsManager.stopReading()

        audioFileChannel?.cancel()
        audioFileChannel?.close()
        audioFileChannel = null
        mediaPlayer.stop()
        mediaPlayer.reset()

        _speakingState.value = false
    }

    fun isSpeaking(): Boolean {
        Log.d("TtsViewModel", "isSpeaking: ${ttsManager.isSpeaking()} ${audioFileChannel != null}")
        return ttsManager.isSpeaking() || audioFileChannel != null
    }

    fun getAvailableLanguages(): List<Locale> = ttsManager.getAvailableLanguages()

    private suspend fun playAudioFile(file: File) = suspendCoroutine { cont ->
        try {
            FileInputStream(file).use { fis ->
                mediaPlayer.setDataSource(fis.fd)
                mediaPlayer.prepare()
                mediaPlayer.start()

                mediaPlayer.setOnCompletionListener {
                    file.delete()
                    mediaPlayer.reset()
                    cont.resume(0)
                }
            }
        } catch (e: Exception) {
            Log.e("TtsViewModel", "playAudioFile: ${e.message}")
            file.delete()
            mediaPlayer.reset()
            cont.resume(0)
        }
    }

    private fun generateTempFile(data: ByteArray): File {
        val tempFile = File.createTempFile("temp", "aac", EinkBroApplication.instance.cacheDir)
        FileOutputStream(tempFile).use { it.write(data) }

        return tempFile
    }
}

enum class TtsType {
    SYSTEM, GPT, ETTS
}