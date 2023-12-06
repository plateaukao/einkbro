package info.plateaukao.einkbro.viewmodel

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.OpenAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TtsViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()
    private val mediaPlayer by lazy {  MediaPlayer() }

    private val openaiRepository: OpenAiRepository
            by lazy { OpenAiRepository(config.gptApiKey) }

    fun hasApiKey(): Boolean = config.gptApiKey.isNotBlank()

    fun readText(context: Context, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sentences: List<String> = text.split("(?<=\\.)|(?<=。)".toRegex())

            for (sentence in sentences) {
                val data = openaiRepository.tts(sentence)
                if (data != null) {
                    playAudio(context, data)
                }
            }
        }
    }

    private suspend fun playAudio(context: Context, data: ByteArray) = suspendCoroutine { cont ->
        // Creating a temporary file
        val tempFile = File.createTempFile("temp", "aac", context.cacheDir)
        tempFile.deleteOnExit()
        val fos = FileOutputStream(tempFile)
        fos.write(data)
        fos.close()

        java.io.FileInputStream(tempFile).use { fis ->
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
}