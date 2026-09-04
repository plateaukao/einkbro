package io.github.edsuns.adfilter.impl

import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 *
 * Files are framed with a small header (magic + payload length + CRC32) and
 * written via a temp file + atomic rename. An interrupted write used to leave
 * a truncated blob in place, which the native deserializer then walked off
 * the end of on every launch — a permanent startup crash loop. With the
 * header, any torn or bit-rotted file is detected and discarded here, before
 * native code ever sees it.
 */
internal class BinaryDataStore(private val dir: File) {

    init {
        if (!dir.exists() && !dir.mkdirs()) {
            Timber.v("BinaryDataStore: failed to create store dirs")
        }
    }

    fun hasData(name: String): Boolean = File(dir, name).exists()

    fun loadData(name: String): ByteArray {
        val bytes = File(dir, name).readBytes()
        if (bytes.size < HEADER_SIZE ||
            !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
        ) {
            // Legacy file from before the header existed: pass it through and
            // rely on the native deserializer's own validation.
            return bytes
        }
        val header = ByteBuffer.wrap(bytes, MAGIC.size, Int.SIZE_BYTES + Long.SIZE_BYTES)
        val length = header.int
        val crc = header.long
        if (length != bytes.size - HEADER_SIZE || crc != crc32(bytes, HEADER_SIZE)) {
            Timber.w("BinaryDataStore: discarding corrupt data: $name")
            clearData(name)
            return ByteArray(0)
        }
        return bytes.copyOfRange(HEADER_SIZE, bytes.size)
    }

    fun saveData(name: String, byteArray: ByteArray) {
        val header = ByteBuffer.allocate(HEADER_SIZE)
            .put(MAGIC)
            .putInt(byteArray.size)
            .putLong(CRC32().apply { update(byteArray) }.value)
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(header.array() + byteArray)
        if (!tmp.renameTo(File(dir, name))) {
            // Same-directory rename should be atomic; fall back to a direct
            // write rather than losing the data entirely.
            Timber.w("BinaryDataStore: atomic rename failed for $name")
            File(dir, name).writeBytes(header.array() + byteArray)
            tmp.delete()
        }
    }

    fun clearData(name: String) {
        File(dir, name).delete()
    }

    private fun crc32(bytes: ByteArray, offset: Int): Long =
        CRC32().apply { update(bytes, offset, bytes.size - offset) }.value

    companion object {
        private val MAGIC = byteArrayOf(0x45, 0x42, 0x44, 0x53, 0x31) // "EBDS1"
        private val HEADER_SIZE = MAGIC.size + Int.SIZE_BYTES + Long.SIZE_BYTES
    }
}
