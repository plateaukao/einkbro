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
    private val findHeadHook: Boolean,
) : WebSocketListener() {
//    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//        super.onClosing(webSocket, code, reason)
//        Log.d("WebSocket", "onClosing: $reason")
//    }
//
//    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//        Log.d("WebSocket", "onFailure: " + t.message)
//    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.contains("Path:turn.end")) {
            webSocket.close(1000, null)
        }
        //Log.d("WebSocket", "onMessage: $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (findHeadHook) {
            findHeadHook(bytes.toByteArray())
        } else {
            fixHeadHook(bytes.toByteArray())
        }
        //Log.d("WebSocket", "onMessage: $bytes")
    }

//    override fun onOpen(webSocket: WebSocket, response: Response) { }

    /**
     * This implementation method is more generic as it searches for the file header marker in the given file header and removes it. However, it may have lower efficiency.
     *
     * @param origin
     */
    private fun findHeadHook(origin: ByteArray) {
        var headIndex = -1
        for (i in 0 until origin.size - head.size) {
            var match = true
            for (j in head.indices) {
                if (origin[i + j] != head[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                headIndex = i
                break
            }
        }
        if (headIndex != -1) {
            val voiceBytesRemoveHead =
                Arrays.copyOfRange(origin, headIndex + head.size, origin.size)

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

    /**
     * This method directly specifies the file header marker, which makes it faster. However, if the format changes, it may become unusable.
     *
     * @param origin
     */
    private fun fixHeadHook(origin: ByteArray) {
        val str = String(origin)
        val skip = if (str.contains("Content-Type")) {
            if (str.contains("audio/mpeg")) {
                130
            } else if (str.contains("codec=opus")) {
                142
            } else {
                0
            }
        } else {
            105
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

    companion object {
        private val head =
            byteArrayOf(0x50, 0x61, 0x74, 0x68, 0x3a, 0x61, 0x75, 0x64, 0x69, 0x6f, 0x0d, 0x0a)
    }
}
