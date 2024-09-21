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

    private val _readProgress = MutableStateFlow("")
    val readProgress: StateFlow<String> = _readProgress.asStateFlow()

    private val openaiRepository: OpenAiRepository by lazy { OpenAiRepository() }

    private fun useOpenAiTts(): Boolean = config.useOpenAiTts && config.gptApiKey.isNotBlank()

    private val type: TtsType
        get() = if (useOpenAiTts()) TtsType.GPT else config.ttsType

    private val articlesToBeRead: MutableList<String> = mutableListOf()

    fun readArticle(text: String) {
        articlesToBeRead.add(text)
        if (isReading()) {
            return
        }

        viewModelScope.launch {
            while (articlesToBeRead.isNotEmpty()) {
                val article = articlesToBeRead.removeAt(0)

                when (type) {
                    TtsType.ETTS,
                    TtsType.GPT,
                    -> readByEngine(type, article)

                    TtsType.SYSTEM -> {
                        _readProgress.value = if (articlesToBeRead.isNotEmpty()) {
                            "(${articlesToBeRead.size})"
                        } else {
                            ""
                        }

                        readBySystemTts(article)
                    }
                }
            }
        }

//        if (Build.MODEL.startsWith("Pixel 8")) {
//            IntentUnit.tts(context as Activity, text)
//            return
//        }
    }

    private suspend fun readBySystemTts(text: String) {
        _speakingState.value = true
        ttsManager.readText(text)

        while (ttsManager.isSpeaking()) {
            delay(2000)
        }
        _speakingState.value = false
    }

    private suspend fun readByEngine(ttsType: TtsType, text: String) {
        _speakingState.value = true
        audioFileChannel = Channel(1)

        viewModelScope.launch(Dispatchers.IO) {
            _speakingState.value = true

            val chunks = processedTextToChunks(text)
            chunks.forEachIndexed { index, chunk ->
                if (audioFileChannel == null) return@launch

                _readProgress.value =
                    "${index + 1}/${chunks.size} " + if (articlesToBeRead.isNotEmpty()) {
                        "(${articlesToBeRead.size})"
                    } else {
                        ""
                    }

                fetchSemaphore.withPermit {
                    Log.d("TtsViewModel", "tts sentence fetch: $chunk")
                    val file = if (ttsType == TtsType.ETTS) {
                        eTts.tts(config.ettsVoice, config.ttsSpeedValue, chunk)
                    } else {
                        openaiRepository.tts(chunk)?.let { generateTempFile(it) }
                    }

                    if (file != null) {
                        Log.d("TtsViewModel", "tts sentence send: $chunk")
                        audioFileChannel?.send(file)
                        Log.d("TtsViewModel", "tts sentence sent: $chunk")
                    }
                }
            }
            audioFileChannel?.close()
        }

        var index = 0
        for (file in audioFileChannel!!) {
            Log.d("TtsViewModel", "play audio $index")
            playAudioFile(file)
            //delay(100)
            index++
            if (audioFileChannel?.isClosedForSend == true && audioFileChannel?.isEmpty == true
            ) break
        }

        _speakingState.value = false
        audioFileChannel = null
    }

    private fun processedTextToChunks(text: String): MutableList<String> {
        val processedText = text.replace("\\n", " ").replace("\\\"", "").replace("\\t", "")
        val sentences = processedText.split("(?<=\\.)(?!\\d)|(?<=。)|(?<=？)|(?<=\\?)".toRegex())
        val chunks = sentences.fold(mutableListOf<String>()) { acc, sentence ->
            if (acc.isEmpty() || acc.last().length + sentence.length > 100) {
                acc.add(sentence)
            } else {
                val last = acc.last()
                acc[acc.size - 1] = "$last$sentence"
            }
            acc
        }
        return chunks
    }

    private val fetchSemaphore = Semaphore(3)

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)

    fun pauseOrResume() {
        if (type == TtsType.SYSTEM) {
            // TODO
            return
        } else {
            mediaPlayer.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.start()
                }
            }
        }
    }

    fun stop() {
        ttsManager.stopReading()

        audioFileChannel?.cancel()
        audioFileChannel?.close()
        audioFileChannel = null
        mediaPlayer.stop()
        mediaPlayer.reset()

        articlesToBeRead.clear()

        _speakingState.value = false
    }

    fun isReading(): Boolean {
        Log.d("TtsViewModel", "isSpeaking: ${ttsManager.isSpeaking()} ${audioFileChannel != null}")
        return ttsManager.isSpeaking() || audioFileChannel != null
    }

    fun isVoicePlaying(): Boolean {
        return mediaPlayer.isPlaying
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