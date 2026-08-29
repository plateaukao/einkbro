package info.plateaukao.einkbro.unit

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.math.abs

/**
 * Pure (Android-free) half of favicon fetching: which icon URLs a page offers and
 * in which order to try them. See [FaviconFetcher] for the network/bitmap half.
 */
object FaviconCandidates {

    data class Candidate(
        val href: String,
        val rel: String = "icon",
        val sizes: String = "",
        val type: String = "",
    )

    /** Icons are stored at most this large; the sort prefers sources close to it. */
    const val PREFERRED_SIZE = 48

    private val LINK_TAG = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))")

    /** Extracts icon `<link>` tags from [html]; hrefs are resolved against [baseUrl]. */
    fun parseIconLinks(html: String, baseUrl: String): List<Candidate> {
        val base = baseUrl.toHttpUrlOrNull() ?: return emptyList()
        return LINK_TAG.findAll(html).mapNotNull { match ->
            val attrs = ATTRIBUTE.findAll(match.value).associate { m ->
                m.groupValues[1].lowercase() to (m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } })
            }
            val rel = attrs["rel"]?.lowercase() ?: return@mapNotNull null
            if (!isIconRel(rel)) return@mapNotNull null
            val href = attrs["href"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val resolved = if (href.startsWith("data:", ignoreCase = true)) href
            else base.resolve(unescapeHtml(href))?.toString() ?: return@mapNotNull null
            Candidate(resolved, rel, attrs["sizes"].orEmpty(), attrs["type"].orEmpty())
        }.toList()
    }

    /**
     * Absolute icon URLs in the order worth trying: declared favicons nearest
     * [PREFERRED_SIZE] first, then touch icons, then `/favicon.ico` as the fallback
     * every browser tries. SVG sources are skipped (BitmapFactory can't decode them).
     */
    fun orderedUrls(candidates: List<Candidate>, pageUrl: String): List<String> {
        val decodable = candidates.filter { !isSvg(it) && isSupportedScheme(it.href) }
        val (touch, plain) = decodable.partition { isTouchIcon(it.rel) }
        val ordered = plain.withIndex()
            .sortedWith(compareBy({ abs(sizeOf(it.value) - PREFERRED_SIZE) }, { it.index }))
            .map { it.value.href } + touch.map { it.href }
        val fallback = pageUrl.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}${portSuffix(it)}/favicon.ico" }
        return (ordered + listOfNotNull(fallback)).distinct()
    }

    private fun isIconRel(rel: String): Boolean =
        rel.split(' ', '\t', '\n').any { it == "icon" || it.startsWith("apple-touch-icon") }

    private fun isTouchIcon(rel: String): Boolean = rel.lowercase().contains("apple-touch-icon")

    private fun isSvg(c: Candidate): Boolean =
        c.type.contains("svg", ignoreCase = true) ||
            c.href.substringBefore('?').substringBefore('#').endsWith(".svg", ignoreCase = true) ||
            c.href.startsWith("data:image/svg", ignoreCase = true)

    private fun isSupportedScheme(href: String): Boolean =
        href.startsWith("http://", true) || href.startsWith("https://", true) || href.startsWith("data:", true)

    /** Largest declared edge, or a typical favicon size when the tag says nothing. */
    private fun sizeOf(c: Candidate): Int {
        val sizes = c.sizes.trim().lowercase()
        if (sizes.isEmpty() || sizes == "any") return 32
        return sizes.split(' ').mapNotNull { token ->
            token.substringBefore('x').toIntOrNull()
        }.maxOrNull() ?: 32
    }

    private fun portSuffix(url: HttpUrl): String =
        if (url.port == HttpUrl.defaultPort(url.scheme)) "" else ":${url.port}"

    private fun unescapeHtml(s: String): String =
        s.replace("&amp;", "&").replace("&#38;", "&").replace("&quot;", "\"").replace("&#39;", "'")
}
