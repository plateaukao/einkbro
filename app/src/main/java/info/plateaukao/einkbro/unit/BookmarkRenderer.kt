package info.plateaukao.einkbro.unit

import android.content.Context
import android.util.Log
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object BookmarkRenderer : KoinComponent {

    private const val BG_MAX_DIMENSION = 1600

    private val config: ConfigManager by inject()
    private val bookmarkManager: info.plateaukao.einkbro.database.BookmarkManager by inject()

    fun loadRecentlyUsedBookmarks(webView: EBWebView) {
        val html = getRecentBookmarksContent(webView.context)
        if (html.isNotBlank()) {
            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "utf-8",
                null
            )
            webView.albumTitle = webView.context.getString(R.string.recently_used_bookmarks)
        }
    }

    fun loadStartPage(webView: EBWebView) {
        // local trusted content; the favicon fallback needs js even when the
        // previous page had it blocked per-domain
        webView.settings.javaScriptEnabled = true
        // base/history url is the sentinel so the tab is saved and restored as a
        // start page (about:blank tabs are dropped from the saved album list)
        webView.loadDataWithBaseURL(
            Constants.START_PAGE_URL,
            getStartPageContent(webView.context),
            "text/html",
            "utf-8",
            Constants.START_PAGE_URL
        )
        webView.albumTitle = startPageTitle(webView.context)
    }

    private fun startPageTitle(context: Context): String =
        config.startPageTitle.ifBlank { context.getString(R.string.app_name) }

    private fun getStartPageContent(context: Context): String {
        val content = config.startPageItems.joinToString(separator = "\n") {
            val name = it.title.escapeHtml()
            val initial = it.title.firstOrNull()?.uppercase()?.escapeHtml() ?: "#"
            // prefer the favicon the browser already stored for this domain;
            // fall back to fetching /favicon.ico, then to the initial letter
            val iconSrc = faviconDataUri(it.url) ?: try {
                val uri = java.net.URI(it.url)
                "${uri.scheme}://${uri.host}/favicon.ico"
            } catch (e: Exception) { "" }
            """
            <a href="${it.url}" class="tile">
                <div class="tile-icon">
                    <img src="$iconSrc" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" />
                    <span class="fallback">$initial</span>
                </div>
                <div class="tile-name">$name</div>
            </a>
            """
        }
        val backgroundBytes = startPageBackgroundFile(context)
            .takeIf { it.exists() }?.readBytes()
        val sampledBackground = backgroundBytes?.let { decodeSampled(it) }
        // with a background the image's own brightness picks the theme;
        // without one the page follows the app's dark mode
        val darkTheme =
            if (sampledBackground != null) isDarkBitmap(sampledBackground)
            else isAppDarkMode(context)
        // loadAssetFile keeps newlines (loadAssetFileToString strips them, which
        // would let the inline script's // comments swallow the rest of the page)
        return HelperUnit.loadAssetFile("start_page.html")
            .replace("{{TITLE}}", startPageTitle(context).escapeHtml())
            .replace("{{COLOR_SCHEME}}", if (darkTheme || backgroundBytes != null) "dark" else "light")
            .replace("{{THEME_CLASS}}", if (darkTheme) "dark" else "")
            .replace(
                "{{BG_STYLE}}",
                backgroundBytes?.let { backgroundStyle(it, sampledBackground, darkTheme) } ?: ""
            )
            .replace("{{SEARCH_HINT}}", context.getString(R.string.main_omnibox_input_hint))
            .replace("{{ADD_LABEL}}", context.getString(R.string.whitelist_add))
            .replace("{{CONTENT}}", content)
    }

    private fun isAppDarkMode(context: Context): Boolean =
        when (config.display.darkMode) {
            info.plateaukao.einkbro.preference.DarkMode.DISABLED -> false
            info.plateaukao.einkbro.preference.DarkMode.FORCE_ON -> true
            info.plateaukao.einkbro.preference.DarkMode.SYSTEM ->
                context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

    // no extension: holds JPEG or PNG bytes depending on what was picked
    fun startPageBackgroundFile(context: Context): java.io.File =
        java.io.File(context.filesDir, "start_page_bg")

    /**
     * Copy the picked image into app storage, downscaled and re-encoded so the
     * start page's inline data URI stays small. Returns false when the image
     * cannot be decoded.
     */
    fun saveStartPageBackground(context: Context, uri: android.net.Uri): Boolean = runCatching {
        val resolver = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream always returns null with inJustDecodeBounds; only the
        // stream itself can be null-checked here
        val boundsStream = resolver.openInputStream(uri) ?: return false
        boundsStream.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / BG_MAX_DIMENSION)
        }
        var bitmap = resolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        } ?: return false

        // camera photos carry their rotation only in EXIF
        val rotation = resolver.openInputStream(uri)?.use {
            when (android.media.ExifInterface(it).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
            bitmap = android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        }

        // keep PNG sources as PNG (transparency, crisp flat graphics);
        // everything else re-encodes to JPEG
        val isPng = bounds.outMimeType == "image/png"
        startPageBackgroundFile(context).outputStream().use { stream ->
            if (isPng) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            } else {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
            }
        }
    }.getOrElse { e ->
        Log.w("browser", "Failed saving start page background: $e")
        false
    }

    private fun backgroundStyle(
        bytes: ByteArray,
        sampledBitmap: android.graphics.Bitmap?,
        darkTheme: Boolean,
    ): String {
        val mime = if (bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        ) "image/png" else "image/jpeg"
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        // contain, not cover: show the whole image like its thumbnail instead
        // of a center crop. The letterbox areas above/below continue the
        // image's own edge colors instead of showing bare page color. A halo
        // in the page color (not a solid backing) keeps the text readable over
        // the image without boxing it in.
        val fallbackEdge = if (darkTheme) "#000000" else "#ffffff"
        val topColor = sampledBitmap?.let { averageRowColor(it, 0) } ?: fallbackEdge
        val bottomColor = sampledBitmap?.let { averageRowColor(it, it.height - 1) } ?: fallbackEdge
        val halo = if (darkTheme) "#000" else "#fff"
        return """
        <style>
        html {
            background:
                url('data:$mime;base64,$base64') center center / contain no-repeat fixed,
                linear-gradient(to bottom, $topColor 0%, $topColor 50%, $bottomColor 50%, $bottomColor 100%) fixed;
        }
        body, body.dark { background: transparent; }
        .wordmark, .tile-name {
            text-shadow: 0 0 2px $halo, 0 0 4px $halo, 0 0 6px $halo, 0 0 10px $halo;
        }
        .tile-icon, .tile-icon .fallback { background: #fff; }
        </style>
        """
    }

    // small decode for color/brightness analysis; resolution doesn't matter
    private fun decodeSampled(bytes: ByteArray): android.graphics.Bitmap? = runCatching {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 64)
        }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()

    // average perceived luminance; transparent pixels count as white
    private fun isDarkBitmap(bitmap: android.graphics.Bitmap): Boolean {
        var luma = 0.0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = android.graphics.Color.alpha(pixel) / 255.0
                val r = android.graphics.Color.red(pixel) * alpha + 255 * (1 - alpha)
                val g = android.graphics.Color.green(pixel) * alpha + 255 * (1 - alpha)
                val b = android.graphics.Color.blue(pixel) * alpha + 255 * (1 - alpha)
                luma += 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        return luma / (bitmap.width * bitmap.height) < 128
    }

    private fun averageRowColor(bitmap: android.graphics.Bitmap, y: Int): String {
        var r = 0.0
        var g = 0.0
        var b = 0.0
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel) / 255.0
            r += android.graphics.Color.red(pixel) * alpha + 255 * (1 - alpha)
            g += android.graphics.Color.green(pixel) * alpha + 255 * (1 - alpha)
            b += android.graphics.Color.blue(pixel) * alpha + 255 * (1 - alpha)
        }
        return "#%02x%02x%02x".format(
            (r / bitmap.width).toInt(),
            (g / bitmap.width).toInt(),
            (b / bitmap.width).toInt(),
        )
    }

    private fun faviconDataUri(url: String): String? =
        bookmarkManager.findFaviconBitmapBy(url)?.let { bitmap ->
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            "data:image/png;base64," +
                    android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        }

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun getRecentBookmarksContent(context: Context): String {
        if (config.recentBookmarks.isEmpty()) return ""
        val alignBottom = !config.ui.isToolbarOnTop
        val bookmarks = if (alignBottom) config.recentBookmarks.reversed() else config.recentBookmarks
        val content = bookmarks.joinToString(separator = "\n") {
            val initial = it.name.firstOrNull()?.uppercase() ?: "#"
            val domain = try {
                java.net.URI(it.url).host?.removePrefix("www.") ?: ""
            } catch (e: Exception) { "" }
            val faviconUrl = try {
                val uri = java.net.URI(it.url)
                "${uri.scheme}://${uri.host}/favicon.ico"
            } catch (e: Exception) { "" }
            """
            <a href="${it.url}" class="card">
                <div class="icon">
                    <img src="$faviconUrl" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" />
                    <span class="fallback">$initial</span>
                </div>
                <div class="info">
                    <div class="name">${it.name}</div>
                    <div class="domain">$domain</div>
                </div>
            </a>
            """
        }
        val bodyClass = if (alignBottom) "align-bottom" else ""
        return HelperUnit.loadAssetFileToString(context, "recent_bookmarks.html")
            .replace("{{BODY_CLASS}}", bodyClass)
            .replace("{{CONTENT}}", content)
    }

    suspend fun getResourceAndMimetypeFromUrl(url: String, timeout: Int = 0): Pair<ByteArray, String> {
        var byteArray: ByteArray = "".toByteArray()
        var mimeType = ""
        withContext(Dispatchers.IO) {
            try {
                val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                connection.addRequestProperty("User-Agent", "Mozilla/4.76")
                if (timeout > 0) {
                    connection.connectTimeout = timeout
                    connection.readTimeout = timeout
                }
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    if (isRedirect(connection.responseCode)) {
                        val redirectUrl = connection.getHeaderField("Location")
                        byteArray = getResourceFromUrl(redirectUrl, timeout)
                    }
                } else {
                    mimeType = connection.contentType
                    byteArray = connection.inputStream.readBytes()
                    connection.inputStream.close()
                }
            } catch (e: IOException) {
                Log.w("browser", "Failed getting resource: $e")
                e.printStackTrace()
            }
        }
        return Pair(byteArray, mimeType)
    }

    suspend fun getResourceFromUrl(url: String, timeout: Int = 0): ByteArray {
        return getResourceAndMimetypeFromUrl(url, timeout).first
    }

    private fun isRedirect(responseCode: Int): Boolean = responseCode in 301..399
}
