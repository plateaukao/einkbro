package info.plateaukao.einkbro.tts

import android.media.MediaDataSource
import java.io.IOException

class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {

    override fun close() {
        // Nothing to close
    }

    @Throws(IOException::class)
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        val length = data.size
        if (position >= length) {
            return -1 // Indicates end of data
        }
        val remaining = length - position.toInt()
        val count = if (size > remaining) remaining else size
        System.arraycopy(data, position.toInt(), buffer, offset, count)
        return count
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }
}
