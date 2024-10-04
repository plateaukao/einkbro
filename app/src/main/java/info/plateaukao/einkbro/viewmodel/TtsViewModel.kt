package info.plateaukao.einkbro.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TranslateRepository
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.tts.ByteArrayMediaDataSource
import info.plateaukao.einkbro.tts.CustomMediaPlayer
import info.plateaukao.einkbro.tts.ETts
import info.plateaukao.einkbro.unit.processedTextToChunks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val mediaPlayer by lazy { CustomMediaPlayer() }
    private var byteArrayChannel: Channel<ChannelData>? = null

    private val _readProgress = MutableStateFlow(ReadProgress(0, 0, 0))
    val readProgress: StateFlow<ReadProgress> get() = _readProgress

    private val _readingState = MutableStateFlow(TtsReadingState.IDLE)
    val readingState: StateFlow<TtsReadingState> get() = _readingState

    private val _showCurrentText = MutableStateFlow(config.ttsShowCurrentText)
    val showCurrentText: StateFlow<Boolean> get() = _showCurrentText

    private val openaiRepository: OpenAiRepository by lazy { OpenAiRepository() }

    private val translateRepository: TranslateRepository by lazy { TranslateRepository() }

    private fun useOpenAiTts(): Boolean = config.useOpenAiTts && config.gptApiKey.isNotBlank()

    private val type: TtsType
        get() = if (useOpenAiTts()) TtsType.GPT else config.ttsType

    private val articlesToBeRead: MutableList<String> = mutableListOf()

    private val _currentReadingContent = MutableStateFlow("")
    val currentReadingContent: StateFlow<String> get() = _currentReadingContent

    fun readArticle(text: String) {

        articlesToBeRead.add(text)
        if (isReading()) {
            updateReadProgress()
            return
        } else {
            _readingState.value = TtsReadingState.PREPARING
        }

        viewModelScope.launch {
            while (articlesToBeRead.isNotEmpty()) {
                val article = articlesToBeRead.removeAt(0)
                if (article.isEmpty()) continue

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

            _currentReadingContent.value = ""
            _readProgress.value = ReadProgress(0, 0, 0)
            _readingState.value = TtsReadingState.IDLE
        }

//        if (Build.MODEL.startsWith("Pixel 8")) {
//            IntentUnit.tts(context as Activity, text)
//            return
//        }

    }

    private suspend fun readBySystemTts(text: String) {
        _readingState.value = TtsReadingState.PLAYING
        ttsManager.readText(
            text,
            onProgress = { index, total, currentContent ->
                updateReadProgress(index, total, currentContent)
            },
        )
    }

    private fun updateReadProgress(
        index: Int = _readProgress.value.index,
        total: Int = _readProgress.value.total,
        text: String? = null,
        articleLeftCount: Int = articlesToBeRead.size,
    ) {
        _readProgress.value = ReadProgress(index, total, articleLeftCount)
        text?.let {
            //_currentReadingContent.value = it
            maybeInsertTranslationText(text)
        }
    }

    private val translationSeparator = "\n---\n"
    private fun maybeInsertTranslationText(text: String) {
        if (config.ttsShowTextTranslation) {
            viewModelScope.launch {
                val translatedText = translateRepository.gTranslateWithApi(text, config.translationLanguage.value)
                _currentReadingContent.value = "$text$translationSeparator$translatedText"
            }
        } else {
            if (_currentReadingContent.value.contains(translationSeparator)) {
                _currentReadingContent.value = _currentReadingContent.value.substringBefore(translationSeparator)
            } else {
                _currentReadingContent.value = text
            }
        }
    }

    private suspend fun readByEngine(ttsType: TtsType, text: String) {
        byteArrayChannel?.cancel()
        byteArrayChannel = Channel(1)
        val chunks = processedTextToChunks(text)

        val articleTtsFetchJob = viewModelScope.launch(Dispatchers.IO) {

            chunks.forEachIndexed { index, chunk ->
                if (byteArrayChannel == null) return@launch

                fetchSemaphore.withPermit {
                    Log.d("TtsViewModel", "tts sentence fetch: $chunk")
                    val byteArray = if (ttsType == TtsType.ETTS) {
                        eTts.tts(config.ettsVoice, config.ttsSpeedValue, chunk)
                    } else {
                        openaiRepository.tts(chunk)
                    }

                    Log.d("TtsViewModel", "tts sentence send ($index) : $chunk")
                    if (byteArrayChannel == null) return@launch
                    byteArrayChannel?.send(ChannelData(byteArray, chunk, index))
                    Log.d("TtsViewModel", "tts sentence sent ($index) : $chunk")
                }
            }
        }

        var index = 0
        while (byteArrayChannel != null) {
            val channelData = byteArrayChannel!!.receive()
            Log.d("TtsViewModel", "play audio $index")

            val byteArray = channelData.byteArray
            val text = channelData.text
            val chunkIndex = channelData.chunkIndex
            updateReadProgress(index = index + 1, total = chunks.size, text)

            if (byteArray != null) {
                playAudioByteArray(byteArray)
            } else {
                Log.e("TtsViewModel", "byteArray is null for $text")
            }
            if (chunkIndex == chunks.size - 1) break

            index++
        }

        articleTtsFetchJob.cancel()
        byteArrayChannel?.cancel()
        byteArrayChannel = null
    }

    private val fetchSemaphore = Semaphore(3)

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)

    fun pauseOrResume() {
        if (type == TtsType.SYSTEM) {
            // TODO
            return
        }

        try {
            mediaPlayer.let {
                if (it.isPlaying) {
                    _readingState.value = TtsReadingState.PAUSED
                    it.pause()
                } else {
                    _readingState.value = TtsReadingState.PLAYING
                    it.start()
                }
            }
        } catch (e: Exception) {
            Log.e("TtsViewModel", "pauseOrResume: ${e.message}")
            mediaPlayer.reset()
        }
    }

    fun hasNextArticle(): Boolean = articlesToBeRead.isNotEmpty()

    fun nextArticle() {
        stop()
    }

    fun reset() {
        articlesToBeRead.clear()
        stop()
        _currentReadingContent.value = ""
        _readingState.value = TtsReadingState.IDLE
    }

    fun stop() {
        if (type == TtsType.SYSTEM) {
            ttsManager.stopReading()
        } else {
            byteArrayChannel?.cancel()
            byteArrayChannel?.close()
            byteArrayChannel = null
            mediaPlayer.reset()
        }
    }

    fun isReading(): Boolean = _readingState.value != TtsReadingState.IDLE

    fun toggleShowCurrentText() {
        config::ttsShowCurrentText.toggle()
        _showCurrentText.value = config.ttsShowCurrentText
    }

    fun toggleShowTranslation() {
        config::ttsShowTextTranslation.toggle()

        maybeInsertTranslationText(_currentReadingContent.value)
    }

    fun getAvailableLanguages(): List<Locale> = ttsManager.getAvailableLanguages()

    private suspend fun playAudioByteArray(byteArray: ByteArray) = suspendCoroutine { cont ->
        try {
            mediaPlayer.setOnResetListener {
                mediaPlayer.setOnResetListener { }
                cont.resume(0)
            }

            mediaPlayer.setDataSource(ByteArrayMediaDataSource(byteArray))

            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
            }
            mediaPlayer.prepare()

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.reset()
            }
            mediaPlayer.setOnErrorListener { value1, value2, value3 ->
                Log.e("TtsViewModel", "playAudioArray: error $value1 $value2 $value3")
                mediaPlayer.reset()
                true
            }

        } catch (e: Exception) {
            //mediaPlayer.reset()
            Log.e("TtsViewModel", "playAudioArray exception: ${e.message}")
            cont.resume(0)
        }
    }
}

enum class TtsType {
    SYSTEM, GPT, ETTS
}

enum class TtsReadingState {
    PREPARING, PLAYING, PAUSED, IDLE
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

class ChannelData(val byteArray: ByteArray?, val text: String, val chunkIndex: Int)