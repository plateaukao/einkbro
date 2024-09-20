package info.plateaukao.einkbro.tts

import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays

open class TTSWebSocketListener(
    private val storage: String,
    private val fileName: String,
) : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.contains("Path:turn.end")) {
            webSocket.close(1000, null)
        }
        //Log.d("WebSocket", "onMessage: $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        fixHeadHook(bytes.toByteArray())
        //Log.d("WebSocket", "onMessage: $bytes")
    }

    private fun fixHeadHook(origin: ByteArray) {
        val str = String(origin)
        val skip = when {
            str.contains("Content-Type") -> when {
                str.contains("audio/mpeg") -> 130
                str.contains("codec=opus") -> 142
                else -> 0
            }
            else -> 105
        }

        val voiceBytesRemoveHead = Arrays.copyOfRange(origin, skip, origin.size)
        try {
            FileOutputStream(storage + File.separator + fileName, true).use { fos ->
                fos.write(voiceBytesRemoveHead)
                fos.flush()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
