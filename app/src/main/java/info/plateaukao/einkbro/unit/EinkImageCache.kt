package info.plateaukao.einkbro.unit

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.LruCache
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import timber.log.Timber

/**
 * Two-tier cache (memory + disk) for e-ink processed images.
 * Key: (url, adjustmentStrength) → processed image bytes.
 *
 * One instance is shared application-wide (Koin single): every tab's
 * WebViewClient hitting the same disk directory through its own memory tier
 * would multiply RAM use by the tab count and duplicate the disk trimming.
 */
class EinkImageCache(context: Context) {

    // LruCache's default sizeOf() is 1 per entry, so a plain
    // LruCache<String, ByteArray>(16 MB) would have capped the *entry count*
    // at 16 million, not the bytes. Account in bytes.
    private val memoryCache = object : LruCache<String, ByteArray>(MAX_MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    private val diskCacheDir = File(context.cacheDir, "eink_images").apply { mkdirs() }

    // Negative cache: URLs whose bodies exceeded the interceptor's size cap.
    // Without it, every page load re-downloads a chunked (unknown-length)
    // oversized image up to the cap before giving up. Count-based LRU is the
    // right unit here (default sizeOf); entries are just short strings.
    private val oversizedUrls = LruCache<String, Boolean>(MAX_OVERSIZED_URLS)

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
            putInMemory(key, bytes)
            return ByteArrayInputStream(bytes)
        }

        return null
    }

    fun put(url: String, strength: Int, data: ByteArray) {
        val key = cacheKey(url, strength)
        putInMemory(key, data)

        // Write to disk synchronously; callers already run off the UI thread.
        try {
            val file = File(diskCacheDir, key)
            file.writeBytes(data)
            // Listing and stat-ing the whole cache directory per store is a
            // full directory walk per image; keep a running byte count and
            // only walk when the budget is actually exceeded. Overwrites make
            // the count drift high, which just triggers an early trim that
            // resets it to the real total.
            ensureDiskUsageCounted()
            if (approxDiskBytes.addAndGet(data.size.toLong()) > MAX_DISK_BYTES) {
                trimDiskCache()
            }
        } catch (_: Exception) {
            // Disk write failure is non-fatal
        }
    }

    // -1 = not yet counted; first put lists the directory once.
    private val approxDiskBytes = java.util.concurrent.atomic.AtomicLong(-1)

    private fun ensureDiskUsageCounted() {
        if (approxDiskBytes.get() < 0) {
            val total = diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
            approxDiskBytes.compareAndSet(-1, total)
        }
    }

    /**
     * Release memory in response to [ComponentCallbacks2.onTrimMemory] levels.
     * The disk tier keeps everything, so evicting here only costs a re-read.
     */
    fun trimMemory(level: Int) {
        Timber.d("trimMemory level=%d, cached=%d bytes", level, memoryCache.size())
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
                level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> memoryCache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ->
                memoryCache.trimToSize(MAX_MEMORY_BYTES / 4)
        }
    }

    fun clearMemory() = memoryCache.evictAll()

    fun markOversized(url: String) {
        oversizedUrls.put(url, true)
    }

    fun isOversized(url: String): Boolean = oversizedUrls.get(url) != null

    // A single entry larger than a quarter of the budget would evict most of
    // the working set for one image; leave such entries on disk only.
    private fun putInMemory(key: String, data: ByteArray) {
        if (data.size > MAX_MEMORY_ENTRY_BYTES) return
        memoryCache.put(key, data)
    }

    private fun trimDiskCache() {
        val files = diskCacheDir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize > MAX_DISK_BYTES) {
            // Evict oldest files first
            files.sortBy { it.lastModified() }
            for (file in files) {
                if (totalSize <= MAX_DISK_BYTES) break
                totalSize -= file.length()
                file.delete()
            }
        }
        approxDiskBytes.set(totalSize)
    }

    private fun cacheKey(url: String, strength: Int): String {
        val raw = "$url|$strength"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        // manual hex: 32 String.format calls per lookup showed up on the
        // request path (get computes the key too, not just put)
        val out = CharArray(digest.size * 2)
        for (i in digest.indices) {
            val b = digest[i].toInt() and 0xff
            out[i * 2] = HEX_DIGITS[b ushr 4]
            out[i * 2 + 1] = HEX_DIGITS[b and 0xf]
        }
        return String(out)
    }

    companion object {
        private val HEX_DIGITS = "0123456789abcdef".toCharArray()

        private const val MAX_MEMORY_BYTES = 16 * 1024 * 1024 // 16 MB
        private const val MAX_MEMORY_ENTRY_BYTES = MAX_MEMORY_BYTES / 4
        private const val MAX_OVERSIZED_URLS = 512
        private const val MAX_DISK_BYTES = 50L * 1024 * 1024  // 50 MB
    }
}
