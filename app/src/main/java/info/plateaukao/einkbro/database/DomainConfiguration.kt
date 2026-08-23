package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import kotlinx.serialization.Serializable

@Entity(tableName = "domain_configuration")
data class DomainConfiguration(
    @PrimaryKey
    var domain: String,
    var configuration: String,
)

/**
 * One site rule. [domain] is the rule key: either a bare host
 * (`example.com`) or a host plus a segment-aligned path prefix
 * (`example.com/docs/api`), see [SiteRuleKey]. Every field is nullable —
 * null means "not set here", so a path rule inherits from the host rule and
 * the host rule from the global setting.
 */
@Serializable
data class DomainConfigurationData(
    val domain: String,
    var shouldFixScroll: Boolean? = null,
    var shouldTranslateSite: Boolean? = null,
    var shouldUseWhiteBackground: Boolean? = null,
    var shouldInvertColor: Boolean? = null,
    // Per-site display overrides (null = use global setting)
    var fontSize: Int? = null,
    var fontType: FontType? = null,
    var boldFontStyle: Boolean? = null,
    var blackFontStyle: Boolean? = null,
    var fontBoldness: Int? = null,
    var desktopMode: Boolean? = null,
    var desktopViewportWidth: Int? = null,
    var enableJavascript: Boolean? = null,
    var enableAdBlock: Boolean? = null,
    var enableCookies: Boolean? = null,
    var translationMode: TranslationMode? = null,
    var customCss: String? = null,
    var postLoadJavascript: String? = null,
) {
    val host: String get() = SiteRuleKey.hostOf(domain)
    val path: String get() = SiteRuleKey.pathOf(domain)
    val isHostRule: Boolean get() = path.isEmpty()

    /** Number of fields this rule sets explicitly. */
    val overrideCount: Int
        get() = listOf(
            shouldFixScroll, shouldTranslateSite, shouldUseWhiteBackground, shouldInvertColor,
            fontSize, fontType, boldFontStyle, blackFontStyle, fontBoldness,
            desktopMode, desktopViewportWidth, enableJavascript, enableAdBlock, enableCookies,
            translationMode, customCss?.takeIf { it.isNotBlank() },
            postLoadJavascript?.takeIf { it.isNotBlank() },
        ).count { it != null }

    val isEmpty: Boolean get() = overrideCount == 0

    /**
     * Rows written before path rules existed stored the four legacy flags as
     * plain `false`. On a host rule `false` and `null` resolve identically (the
     * host rule is the end of the chain), so normalise to null; otherwise old
     * rows would show every flag as an explicit override in the editor.
     */
    fun normalizedLegacyFlags(): DomainConfigurationData {
        if (!isHostRule) return this
        return copy(
            shouldFixScroll = shouldFixScroll?.takeIf { it },
            shouldTranslateSite = shouldTranslateSite?.takeIf { it },
            shouldUseWhiteBackground = shouldUseWhiteBackground?.takeIf { it },
            shouldInvertColor = shouldInvertColor?.takeIf { it },
        )
    }
}

/**
 * Rule-key helpers. A key is `host` or `host/path/prefix` — lower-case host,
 * no scheme, no query, no trailing slash.
 */
object SiteRuleKey {
    fun hostOf(key: String): String = key.substringBefore('/')

    /** `""` for a host rule, otherwise `/a/b` (leading slash, no trailing slash). */
    fun pathOf(key: String): String {
        val p = key.substringAfter('/', "")
        return if (p.isEmpty()) "" else "/$p"
    }

    fun of(host: String, path: String): String {
        val normalized = normalizePath(path)
        return if (normalized.isEmpty()) host else host + normalized
    }

    /** Strips trailing slashes and collapses "/" to "" (root == host rule). */
    fun normalizePath(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        val trimmed = path.trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }

    // Hand-rolled instead of android.net.Uri so the resolver is plain-JVM
    // testable, and so hosts compare case-insensitively (Uri keeps case).
    fun hostOfUrl(url: String): String? {
        val authority = authorityOf(url) ?: return null
        var host = authority.substringAfterLast('@')
        host = if (host.startsWith("[")) {
            host.substringBefore(']') + "]"
        } else {
            host.substringBefore(':')
        }
        return host.lowercase().takeIf { it.isNotBlank() }
    }

    fun pathOfUrl(url: String): String {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return ""
        val afterAuthority = url.indexOf('/', schemeEnd + 3)
        if (afterAuthority < 0) return ""
        val end = url.indexOfAny(charArrayOf('?', '#'), afterAuthority)
            .let { if (it < 0) url.length else it }
        return normalizePath(url.substring(afterAuthority, end))
    }

    private fun authorityOf(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return null
        val start = schemeEnd + 3
        val end = url.indexOfAny(charArrayOf('/', '?', '#'), start)
            .let { if (it < 0) url.length else it }
        return url.substring(start, end).takeIf { it.isNotEmpty() }
    }

    /** True when a rule with [key] applies to a page at [host] + [path]. */
    fun matches(key: String, host: String, path: String): Boolean {
        if (!hostOf(key).equals(host, ignoreCase = true)) return false
        val rulePath = pathOf(key)
        if (rulePath.isEmpty()) return true
        return path == rulePath || path.startsWith("$rulePath/")
    }

    /** Number of path segments; host rules are 0. Higher = more specific. */
    fun specificity(key: String): Int {
        val p = pathOf(key)
        return if (p.isEmpty()) 0 else p.count { it == '/' }
    }

    /**
     * Every key that could scope a rule for [url], from the host down to the
     * full path: `["example.com", "example.com/a", "example.com/a/b"]`.
     */
    fun candidateKeysFor(url: String): List<String> {
        val host = hostOfUrl(url) ?: return emptyList()
        val segments = pathOfUrl(url).split('/').filter { it.isNotEmpty() }
        val keys = ArrayList<String>(segments.size + 1)
        keys += host
        val sb = StringBuilder(host)
        for (seg in segments) {
            sb.append('/').append(seg)
            keys += sb.toString()
        }
        return keys
    }
}
