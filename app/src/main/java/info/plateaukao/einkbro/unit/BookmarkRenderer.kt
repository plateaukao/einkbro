package info.plateaukao.einkbro.unit

import android.content.Context
import android.util.Log
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
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

    fun getRecentBookmarksContent(context: Context): String {
        if (config.recentBookmarks.isEmpty()) return ""
        val alignBottom = !config.isToolbarOnTop
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
                if (timeout > 0) connection.connectTimeout = timeout
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
