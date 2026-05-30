package info.plateaukao.einkbro.userscript

/**
 * Converts Tampermonkey `@match` / `@include` patterns to regexes and tests URLs.
 *
 * - `@match` follows the Chrome match-pattern grammar (scheme://host/path with `*`).
 * - `@include` is looser: a glob where `*` matches any run of chars; a value that
 *   looks like `/regex/` is treated as a regex.
 */
object UrlMatcher {

    fun matches(url: String, matchPatterns: List<String>, includePatterns: List<String>): Boolean {
        if (matchPatterns.isEmpty() && includePatterns.isEmpty()) return false
        return matchPatterns.any { matchPattern(url, it) } ||
            includePatterns.any { includePattern(url, it) }
    }

    fun isExcluded(url: String, excludePatterns: List<String>): Boolean =
        excludePatterns.any { includePattern(url, it) || matchPattern(url, it) }

    private val matchCache = HashMap<String, Regex?>()
    private val includeCache = HashMap<String, Regex>()

    private fun matchPattern(url: String, pattern: String): Boolean {
        if (pattern == "*" || pattern == "<all_urls>") return url.startsWith("http")
        val regex = matchCache.getOrPut(pattern) { compileMatch(pattern) } ?: return false
        return regex.matches(url)
    }

    private fun includePattern(url: String, pattern: String): Boolean {
        // /regex/ form
        if (pattern.length > 2 && pattern.startsWith("/") && pattern.endsWith("/")) {
            return try {
                Regex(pattern.substring(1, pattern.length - 1)).containsMatchIn(url)
            } catch (e: Exception) {
                false
            }
        }
        val regex = includeCache.getOrPut(pattern) { globToRegex(pattern) }
        return regex.matches(url)
    }

    /** Chrome match pattern: <scheme>://<host><path> */
    private fun compileMatch(pattern: String): Regex? {
        val schemeSep = pattern.indexOf("://")
        if (schemeSep < 0) return null
        val scheme = pattern.substring(0, schemeSep)
        val rest = pattern.substring(schemeSep + 3)
        val slash = rest.indexOf('/')
        if (slash < 0) return null
        val host = rest.substring(0, slash)
        val path = rest.substring(slash)

        val schemeRegex = when (scheme) {
            "*" -> "https?"
            else -> Regex.escape(scheme)
        }
        val hostRegex = when {
            host == "*" -> "[^/]+"
            host.startsWith("*.") -> "(?:[^/]+\\.)?" + Regex.escape(host.substring(2))
            else -> Regex.escape(host)
        }
        val pathRegex = globBody(path)
        // Chrome match patterns match host only; tolerate an explicit :port in the URL.
        return try {
            Regex("^$schemeRegex://$hostRegex(?::\\d+)?$pathRegex$")
        } catch (e: Exception) {
            null
        }
    }

    private fun globToRegex(pattern: String): Regex {
        // Anchor loosely: include patterns without scheme should still match.
        val body = globBody(pattern)
        return try {
            Regex("^$body$")
        } catch (e: Exception) {
            Regex(Regex.escape(pattern))
        }
    }

    /** Escape everything except `*` (any chars). */
    private fun globBody(glob: String): String =
        glob.split("*").joinToString(".*") { Regex.escape(it) }
}
