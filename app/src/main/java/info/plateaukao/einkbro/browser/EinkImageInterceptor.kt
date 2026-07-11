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

        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            request.requestHeaders?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
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

            val originalBytes = connection.inputStream.use { it.readBytes() }

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

    companion object {
        private val processSemaphore = Semaphore(2)
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
