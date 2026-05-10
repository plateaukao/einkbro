package info.plateaukao.einkbro.unit

import java.net.URI

object GithubUtil {
    /**
     * Convert a github.com /blob/<ref>/<path> page URL to its raw download URL.
     * Returns null when [pageUrl] does not look like a github file view.
     *
     * Uses /raw/<ref>/<path> rather than /raw/refs/heads/<branch>/<path> so the
     * redirect resolves for branches, tags, and commit SHAs alike.
     */
    fun rawUrlForBlobPage(pageUrl: String?): String? {
        pageUrl ?: return null
        val uri = try { URI(pageUrl) } catch (e: Exception) { return null }
        if (uri.host != "github.com") return null
        val path = uri.path ?: return null
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size < 5 || segments[2] != "blob") return null

        val owner = segments[0]
        val repo = segments[1]
        val ref = segments[3]
        val rest = segments.drop(4).joinToString("/")
        if (rest.isBlank()) return null

        val scheme = uri.scheme ?: "https"
        return "$scheme://${uri.host}/$owner/$repo/raw/$ref/$rest"
    }
}
