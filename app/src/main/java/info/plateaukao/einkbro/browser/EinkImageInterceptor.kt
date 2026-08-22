package info.plateaukao.einkbro.browser

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.EinkImageAdjustment
import info.plateaukao.einkbro.preference.EinkImageMode
import info.plateaukao.einkbro.unit.EinkImageCache
import info.plateaukao.einkbro.unit.EinkImageProcessor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Semaphore

class EinkImageInterceptor(
    private val config: ConfigManager,
    private val einkImageCache: EinkImageCache,
) {
    fun processEinkImageRequest(request: WebResourceRequest): WebResourceResponse? {
        val adjustment = config.display.einkImageAdjustment
        if (adjustment == EinkImageAdjustment.OFF) return null
        // FAST mode adjusts images with an injected CSS filter instead
        if (config.display.einkImageMode != EinkImageMode.DEEP) return null

        val url = request.url.toString()
        if (!looksLikeImageUrl(url) && !hasImageAcceptHeader(request)) return null

        // Check cache first
        einkImageCache.get(url, adjustment.strength)?.let { cachedStream ->
            val mimeType = getImageMimeFromUrl(url) ?: "image/jpeg"
            return WebResourceResponse(mimeType, null, cachedStream)
        }
        // Known to exceed the size cap: don't download it again just to reject it.
        if (einkImageCache.isOversized(url)) return null

        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            request.requestHeaders?.forEach { (key, value) ->
                // The WebView advertises gzip/br; a compressed body would defeat
                // both the size cap below and BitmapFactory.
                if (!key.equals("Accept-Encoding", ignoreCase = true)) {
                    connection.setRequestProperty(key, value)
                }
            }
            connection.setRequestProperty("Accept-Encoding", "identity")
            // Add cookies from WebView's CookieManager (CDNs like Instagram require auth cookies)
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrEmpty()) {
                connection.setRequestProperty("Cookie", cookies)
            }
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                connection.disconnect()
                return null
            }

            // Use response Content-Type for actual MIME; fall back to URL extension
            val responseContentType = connection.contentType
            // GIFs may be animated; decoding would freeze them to the first
            // frame. Let the WebView load them natively.
            if (responseContentType?.contains("image/gif", ignoreCase = true) == true) {
                connection.disconnect()
                return null
            }
            val mimeType = getImageMimeFromContentType(responseContentType)
                ?: getImageMimeFromUrl(url)
                ?: run { connection.disconnect(); return null }

            // Never buffer an unbounded body: a huge (or hostile) image would
            // OOM the process. Reject by declared length first, then enforce
            // the same cap while streaming for chunked / unknown lengths. On
            // rejection the WebView fetches and renders the image natively.
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_SOURCE_BYTES) {
                connection.disconnect()
                einkImageCache.markOversized(url)
                return null
            }
            val originalBytes = connection.inputStream.use {
                readAtMost(it, MAX_SOURCE_BYTES, declaredLength)
            } ?: run {
                connection.disconnect()
                einkImageCache.markOversized(url)
                return null
            }

            // Bound concurrent decode/process/encode: several WebView threads
            // intercept at once, and each full-size bitmap is tens of MB.
            processSemaphore.acquire()
            val processedBytes = try {
                EinkImageProcessor.processBytes(originalBytes, mimeType, adjustment.strength)
            } finally {
                processSemaphore.release()
            }

            // null means pass-through (tiny or undecodable): serve the already
            // downloaded original instead of making the WebView re-fetch it.
            val servedBytes = processedBytes ?: originalBytes
            einkImageCache.put(url, adjustment.strength, servedBytes)

            // Forward response headers so CORS / caching / JS fetch still work
            val responseHeaders = mutableMapOf<String, String>()
            var i = 0
            while (true) {
                val key = connection.getHeaderFieldKey(i) ?: if (i == 0) { i++; continue } else break
                val value = connection.getHeaderField(i) ?: break
                val lowerKey = key.lowercase()
                // Skip headers we override or that no longer apply after re-encoding
                if (lowerKey !in setOf("content-length", "content-encoding", "transfer-encoding", "content-type")) {
                    responseHeaders[key] = value
                }
                i++
            }
            WebResourceResponse(
                mimeType, null, statusCode, connection.responseMessage ?: "OK",
                responseHeaders, ByteArrayInputStream(servedBytes)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Read [input] fully, or return null once more than [maxBytes] arrive.
     * [sizeHint] (the Content-Length, or -1) presizes the buffer so a known
     * length costs one allocation instead of repeated doubling.
     */
    private fun readAtMost(input: InputStream, maxBytes: Long, sizeHint: Long): ByteArray? {
        val output = ByteArrayOutputStream(sizeHint.coerceIn(0L, maxBytes).toInt().coerceAtLeast(DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return output.toByteArray()
            total += count
            if (total > maxBytes) return null
            output.write(buffer, 0, count)
        }
    }

    companion object {
        private val processSemaphore = Semaphore(2)

        private const val MB = 1024L * 1024

        // Largest source image worth processing; anything beyond this is
        // decoded by the WebView instead. Decoding downsamples to the screen,
        // so the body itself is the dominant per-request cost: budget 1/16 of
        // the heap, between 10 MB (phones/e-readers) and 25 MB (tablets).
        private val MAX_SOURCE_BYTES: Long =
            (Runtime.getRuntime().maxMemory() / 16).coerceIn(10 * MB, 25 * MB)
    }

    private fun looksLikeImageUrl(url: String): Boolean {
        val lower = url.substringBefore('?').substringBefore('#').lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun hasImageAcceptHeader(request: WebResourceRequest): Boolean {
        val accept = request.requestHeaders?.get("Accept") ?: return false
        return accept.startsWith("image/")
    }

    private fun getImageMimeFromContentType(contentType: String?): String? {
        if (contentType == null) return null
        val lower = contentType.lowercase()
        return when {
            lower.contains("image/jpeg") -> "image/jpeg"
            lower.contains("image/png") -> "image/png"
            lower.contains("image/webp") -> "image/webp"
            else -> null
        }
    }

    private fun getImageMimeFromUrl(url: String): String? {
        val lower = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            else -> null
        }
    }
}
