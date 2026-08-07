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
        webView.albumTitle = webView.context.getString(R.string.app_name)
    }

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
        // loadAssetFile keeps newlines (loadAssetFileToString strips them, which
        // would let the inline script's // comments swallow the rest of the page)
        return HelperUnit.loadAssetFile("start_page.html")
            .replace("{{SEARCH_HINT}}", context.getString(R.string.main_omnibox_input_hint))
            .replace("{{ADD_LABEL}}", context.getString(R.string.whitelist_add))
            .replace("{{CONTENT}}", content)
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
