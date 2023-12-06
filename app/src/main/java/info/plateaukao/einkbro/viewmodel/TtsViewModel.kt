package info.plateaukao.einkbro.viewmodel

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.OpenAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TtsViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()
    private val mediaPlayer by lazy {  MediaPlayer() }
    private var byteArrayChannel: Channel<ByteArray>? = null


    private val openaiRepository: OpenAiRepository
            by lazy { OpenAiRepository(config.gptApiKey) }

    fun hasApiKey(): Boolean = config.gptApiKey.isNotBlank()

    fun readText(context: Context, text: String) {
        byteArrayChannel = Channel(3)
        viewModelScope.launch(Dispatchers.IO) {
            val sentences: List<String> = text.split("(?<=\\.)|(?<=。)".toRegex())

            for (sentence in sentences) {
                val data = openaiRepository.tts(sentence)
                if (data != null && byteArrayChannel != null) {
                    byteArrayChannel?.send(data)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            for (data in byteArrayChannel!!) {
                playAudio(context, data)
                delay(200)
            }
            byteArrayChannel = null
        }
    }

    fun stop() {
        byteArrayChannel?.cancel()
        byteArrayChannel = null
        mediaPlayer.stop()
        mediaPlayer.reset()
    }

    fun isSpeaking() = byteArrayChannel != null

    private suspend fun playAudio(context: Context, data: ByteArray) = suspendCoroutine { cont ->
        val tempFile = generateTempFile(context, data)

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

    private fun generateTempFile(context: Context, data: ByteArray): File {
        val tempFile = File.createTempFile("temp", "aac", context.cacheDir)
        FileOutputStream(tempFile).use { it.write(data) }

        return tempFile
    }
}