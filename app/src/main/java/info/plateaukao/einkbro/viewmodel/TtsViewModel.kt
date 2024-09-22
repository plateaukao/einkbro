package info.plateaukao.einkbro.viewmodel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.tts.ByteArrayMediaDataSource
import info.plateaukao.einkbro.tts.ETts
import info.plateaukao.einkbro.unit.processedTextToChunks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TtsViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    private val ttsManager: TtsManager by inject()

    private val eTts: ETts = ETts()

    private val mediaPlayer by lazy { MediaPlayer() }
    private var byteArrayChannel: Channel<ByteArray>? = null

    // for showing play controls state
    private val _speakingState = MutableStateFlow(false)
    val speakingState: StateFlow<Boolean> = _speakingState.asStateFlow()

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _readProgress = MutableStateFlow(ReadProgress(0, 0, 0))
    val readProgress: StateFlow<ReadProgress> = _readProgress.asStateFlow()

    private val openaiRepository: OpenAiRepository by lazy { OpenAiRepository() }

    private fun useOpenAiTts(): Boolean = config.useOpenAiTts && config.gptApiKey.isNotBlank()

    private val type: TtsType
        get() = if (useOpenAiTts()) TtsType.GPT else config.ttsType

    private val articlesToBeRead: MutableList<String> = mutableListOf()

    fun readArticle(text: String) {
        _isReading.value = true

        articlesToBeRead.add(text)
        if (isReading()) {
            updateReadProgress()
            return
        }

        viewModelScope.launch {
            while (articlesToBeRead.isNotEmpty()) {
                val article = articlesToBeRead.removeAt(0)

                Log.d("TtsViewModel", "readArticle: $article")
                when (type) {
                    TtsType.ETTS,
                    TtsType.GPT,
                    -> readByEngine(type, article)

                    TtsType.SYSTEM -> {
                        updateReadProgress()
                        readBySystemTts(article)
                    }
                }
            }

            _speakingState.value = false
            _isReading.value = false
            _readProgress.value = ReadProgress(0, 0, 0)
        }

//        if (Build.MODEL.startsWith("Pixel 8")) {
//            IntentUnit.tts(context as Activity, text)
//            return
//        }

    }

    private suspend fun readBySystemTts(text: String) {
        _speakingState.value = true
        ttsManager.readText(
            text,
            onProgress =  { index, total ->
                updateReadProgress(index, total)
            },
        )
    }

    private fun updateReadProgress(
        index: Int = _readProgress.value.index,
        total: Int = _readProgress.value.total,
        articleLeftCount: Int = articlesToBeRead.size) {
        _readProgress.value = ReadProgress(index, total, articleLeftCount)
    }

    private suspend fun readByEngine(ttsType: TtsType, text: String) {
        byteArrayChannel = Channel(1)
        val chunks = processedTextToChunks(text)

        viewModelScope.launch(Dispatchers.IO) {
            _speakingState.value = true

            chunks.forEachIndexed { index, chunk ->
                if (byteArrayChannel == null) return@launch

                fetchSemaphore.withPermit {
                    Log.d("TtsViewModel", "tts sentence fetch: $chunk")
                    val byteArray = if (ttsType == TtsType.ETTS) {
                        eTts.tts(config.ettsVoice, config.ttsSpeedValue, chunk)
                    } else {
                        openaiRepository.tts(chunk)
                    }

                    if (byteArray != null) {
                        Log.d("TtsViewModel", "tts sentence send ($index) : $chunk")
                        byteArrayChannel?.send(byteArray)
                        Log.d("TtsViewModel", "tts sentence sent ($index) : $chunk")
                    }
                }
            }
            byteArrayChannel?.close()
        }

        var index = 0
        for (byteArray in byteArrayChannel!!) {
            Log.d("TtsViewModel", "play audio $index")
            updateReadProgress(index = index + 1, total = chunks.size)

            playAudioByteArray(byteArray)
            index++
            if (byteArrayChannel?.isClosedForSend == true &&
                byteArrayChannel?.isEmpty == true) break
        }

        byteArrayChannel = null
    }

    private val fetchSemaphore = Semaphore(3)

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)

    private var isInPause = false
    fun pauseOrResume() {
        if (type == TtsType.SYSTEM) {
            // TODO
            return
        } else {
            mediaPlayer.let {
                if (it.isPlaying) {
                    it.pause()
                    isInPause = true
                } else {
                    it.start()
                    isInPause = false
                }
            }
        }
    }

    fun stop() {
        ttsManager.stopReading()

        byteArrayChannel?.cancel()
        byteArrayChannel?.close()
        byteArrayChannel = null
        mediaPlayer.stop()
        mediaPlayer.reset()
        isInPause = false

        articlesToBeRead.clear()

        _speakingState.value = false
        _isReading.value = false
    }

    fun isReading(): Boolean {
        Log.d("TtsViewModel", "isSpeaking: ${ttsManager.isSpeaking()} ${byteArrayChannel != null}")
        return ttsManager.isSpeaking() || byteArrayChannel != null
    }

    fun isVoicePlaying(): Boolean = !isInPause

    fun getAvailableLanguages(): List<Locale> = ttsManager.getAvailableLanguages()

    private suspend fun playAudioByteArray(byteArray: ByteArray) = suspendCoroutine { cont ->
        try {
            mediaPlayer.setDataSource(ByteArrayMediaDataSource(byteArray))
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
            }
            mediaPlayer.prepare()

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.reset()
                cont.resume(0)
            }
        } catch (e: Exception) {
            Log.e("TtsViewModel", "playAudioArray: ${e.message}")
            mediaPlayer.reset()
            cont.resume(0)
        }
    }
}

enum class TtsType {
    SYSTEM, GPT, ETTS
}

fun TtsType.toStringResId(): Int {
    return when (this) {
        TtsType.GPT -> R.string.tts_type_gpt
        TtsType.ETTS -> R.string.tts_type_etts
        TtsType.SYSTEM -> R.string.tts_type_system
    }
}

data class ReadProgress(val index: Int, val total: Int, val articleLeftCount: Int) {
    override fun toString(): String {
        if (total == 0) return "($articleLeftCount)"

        return "$index/$total " +
                if (articleLeftCount > 0) {
                    "($articleLeftCount)"
                } else {
                    ""
                }
    }
}