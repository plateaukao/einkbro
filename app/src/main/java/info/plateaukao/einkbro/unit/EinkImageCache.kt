package info.plateaukao.einkbro.unit

import android.content.Context
import android.util.LruCache
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Two-tier cache (memory + disk) for e-ink processed images.
 * Key: (url, adjustmentStrength) → processed image bytes.
 */
class EinkImageCache(context: Context) {

    private val memoryCache = LruCache<String, ByteArray>(MAX_MEMORY_BYTES)

    private val diskCacheDir = File(context.cacheDir, "eink_images").apply { mkdirs() }

    fun get(url: String, strength: Int): InputStream? {
        val key = cacheKey(url, strength)

        // 1. Check memory cache
        memoryCache.get(key)?.let {
            return ByteArrayInputStream(it)
        }

        // 2. Check disk cache
        val file = File(diskCacheDir, key)
        if (file.exists()) {
            val bytes = file.readBytes()
            memoryCache.put(key, bytes)
            return ByteArrayInputStream(bytes)
        }

        return null
    }

    fun put(url: String, strength: Int, data: ByteArray) {
        val key = cacheKey(url, strength)
        memoryCache.put(key, data)

        // Write to disk asynchronously
        try {
            val file = File(diskCacheDir, key)
            file.writeBytes(data)
            trimDiskCache()
        } catch (_: Exception) {
            // Disk write failure is non-fatal
        }
    }

    private fun trimDiskCache() {
        val files = diskCacheDir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_DISK_BYTES) return

        // Evict oldest files first
        files.sortBy { it.lastModified() }
        for (file in files) {
            if (totalSize <= MAX_DISK_BYTES) break
            totalSize -= file.length()
            file.delete()
        }
    }

    private fun cacheKey(url: String, strength: Int): String {
        val raw = "$url|$strength"
        val md = MessageDigest.getInstance("MD5")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_MEMORY_BYTES = 16 * 1024 * 1024 // 16 MB
        private const val MAX_DISK_BYTES = 50L * 1024 * 1024  // 50 MB
    }
}
