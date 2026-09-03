package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.database.SiteRuleKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-site settings, resolved per URL through a chain of rules.
 *
 * Rules are keyed by [SiteRuleKey]: a bare host, or a host plus a path
 * prefix. For a URL the matching rules are ordered most specific first
 * (longest path prefix, host rule last), and each field takes the first
 * non-null value along that chain, falling back to the global setting.
 * Hosts match exactly — no subdomain wildcards.
 */
class DomainConfigManager(
    private val display: DisplayConfig,
    private val browser: BrowserConfig,
    private val translation: TranslationConfig,
    private val persist: (DomainConfigurationData) -> Unit,
    private val remove: (String) -> Unit = {},
) {
    // ConcurrentHashMap: read from WebView worker threads (shouldInterceptRequest),
    // written on the main thread when a site config is saved.
    var domainConfigurationMap: MutableMap<String, DomainConfigurationData> = ConcurrentHashMap()

    // Merged per-URL views, keyed by host+path. getEffectiveConfig runs twice
    // per subresource on WebView worker threads, and scanning + sorting every
    // rule plus a 20-field merge per request is measurable; any rule mutation
    // clears the cache. Cached instances are shared — the getEffectiveConfig
    // contract (read-only result) is what keeps that safe.
    private val effectiveConfigCache = ConcurrentHashMap<String, DomainConfigurationData>()

    private fun invalidateResolutionCache() = effectiveConfigCache.clear()

    private fun save(rule: DomainConfigurationData) {
        persist(rule)
        invalidateResolutionCache()
    }

    /** Bulk-loads rules read from the database, replacing resolution state. */
    fun putAllRules(rules: Map<String, DomainConfigurationData>) {
        domainConfigurationMap.putAll(rules)
        invalidateResolutionCache()
    }

    // region resolution

    /** Rules that apply to [url], most specific first. Empty for non-http(s)-style URLs. */
    fun matchingRules(url: String): List<DomainConfigurationData> {
        val host = SiteRuleKey.hostOfUrl(url) ?: return emptyList()
        val path = SiteRuleKey.pathOfUrl(url)
        return domainConfigurationMap.values
            .filter { SiteRuleKey.matches(it.domain, host, path) }
            .sortedByDescending { SiteRuleKey.specificity(it.domain) }
    }

    /** Keys of [matchingRules]; cheap identity for "did the rule chain change". */
    fun matchingKeys(url: String): List<String> = matchingRules(url).map { it.domain }

    private inline fun <T> resolve(url: String, field: (DomainConfigurationData) -> T?): T? {
        for (rule in matchingRules(url)) {
            field(rule)?.let { return it }
        }
        return null
    }

    /**
     * The merged view for [url]: every field is the first value set along the
     * rule chain. Read-only — do not save it back (that would freeze inherited
     * values into whichever rule [DomainConfigurationData.domain] names).
     */
    fun getEffectiveConfig(url: String): DomainConfigurationData {
        val host = SiteRuleKey.hostOfUrl(url) ?: return DomainConfigurationData("")
        val key = host + SiteRuleKey.pathOfUrl(url)
        effectiveConfigCache[key]?.let { return it }
        val merged = merge(url, matchingRules(url))
        if (effectiveConfigCache.size >= MAX_CACHED_RESOLUTIONS) invalidateResolutionCache()
        effectiveConfigCache[key] = merged
        return merged
    }

    /**
     * What [url] would resolve to if the rule [excludingKey] did not exist —
     * the "inherited" value the editor shows as the fallback for that rule.
     */
    fun getInheritedConfig(url: String, excludingKey: String): DomainConfigurationData =
        merge(url, matchingRules(url).filter { it.domain != excludingKey })

    private fun merge(url: String, chain: List<DomainConfigurationData>): DomainConfigurationData {
        val host = SiteRuleKey.hostOfUrl(url) ?: return DomainConfigurationData("")
        if (chain.isEmpty()) return DomainConfigurationData(host)
        return DomainConfigurationData(
            domain = chain.first().domain,
            shouldFixScroll = chain.firstNotNullOfOrNull { it.shouldFixScroll },
            shouldTranslateSite = chain.firstNotNullOfOrNull { it.shouldTranslateSite },
            shouldUseWhiteBackground = chain.firstNotNullOfOrNull { it.shouldUseWhiteBackground },
            shouldInvertColor = chain.firstNotNullOfOrNull { it.shouldInvertColor },
            fontSize = chain.firstNotNullOfOrNull { it.fontSize },
            fontType = chain.firstNotNullOfOrNull { it.fontType },
            boldFontStyle = chain.firstNotNullOfOrNull { it.boldFontStyle },
            blackFontStyle = chain.firstNotNullOfOrNull { it.blackFontStyle },
            fontBoldness = chain.firstNotNullOfOrNull { it.fontBoldness },
            desktopMode = chain.firstNotNullOfOrNull { it.desktopMode },
            desktopViewportWidth = chain.firstNotNullOfOrNull { it.desktopViewportWidth },
            enableJavascript = chain.firstNotNullOfOrNull { it.enableJavascript },
            enableAdBlock = chain.firstNotNullOfOrNull { it.enableAdBlock },
            enableCookies = chain.firstNotNullOfOrNull { it.enableCookies },
            enableImages = chain.firstNotNullOfOrNull { it.enableImages },
            translationMode = chain.firstNotNullOfOrNull { it.translationMode },
            customCss = chain.firstNotNullOfOrNull { it.activeCustomCss },
            postLoadJavascript = chain.firstNotNullOfOrNull { it.activePostLoadJavascript },
        )
    }

    // endregion

    // region rule access (for the editor)

    fun getRule(key: String): DomainConfigurationData? = domainConfigurationMap[key]

    fun getRuleOrNew(key: String): DomainConfigurationData =
        domainConfigurationMap[key] ?: DomainConfigurationData(key)

    /** All rules whose host is [host] (the host rule and its path rules), host first. */
    fun rulesForHost(host: String): List<DomainConfigurationData> =
        domainConfigurationMap.values
            .filter { it.host.equals(host, ignoreCase = true) }
            .sortedWith(compareBy({ SiteRuleKey.specificity(it.domain) }, { it.domain }))

    /**
     * Every rule that sets something, grouped by host (host rule first, then
     * its paths). Rows left behind by a quick toggle that was switched back
     * off carry no overrides and are skipped.
     */
    fun allRules(): List<DomainConfigurationData> =
        domainConfigurationMap.values
            .filter { !it.normalizedLegacyFlags().isEmpty }
            .sortedWith(compareBy({ it.host }, { SiteRuleKey.specificity(it.domain) }, { it.domain }))

    fun updateDomainConfig(config: DomainConfigurationData) {
        if (config.domain.isBlank()) return
        if (config.normalizedLegacyFlags().isEmpty) {
            // nothing set any more: drop the row instead of storing an empty rule
            deleteRule(config.domain)
            return
        }
        domainConfigurationMap[config.domain] = config
        save(config)
    }

    fun deleteRule(key: String) {
        if (domainConfigurationMap.remove(key) != null) {
            remove(key)
            invalidateResolutionCache()
        }
    }

    /**
     * Rule a quick toggle writes into: the most specific matching rule that
     * already sets [field], else the host rule (created on demand). Path rules
     * only take part once the user set that field up in site settings.
     */
    private fun writeTargetFor(
        url: String,
        field: (DomainConfigurationData) -> Any?,
    ): DomainConfigurationData? {
        val host = SiteRuleKey.hostOfUrl(url) ?: return null
        matchingRules(url).firstOrNull { field(it) != null }?.let { return it }
        return domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
    }

    // endregion

    // region legacy boolean flags + quick toggles

    fun shouldFixScroll(url: String): Boolean = resolve(url) { it.shouldFixScroll } ?: false

    fun toggleFixScroll(url: String): Boolean {
        val target = writeTargetFor(url) { it.shouldFixScroll } ?: return false
        target.shouldFixScroll = !shouldFixScroll(url)
        save(target)
        return shouldFixScroll(url)
    }

    fun shouldTranslateSite(url: String): Boolean = resolve(url) { it.shouldTranslateSite } ?: false

    fun toggleTranslateSite(url: String): Boolean {
        val target = writeTargetFor(url) { it.shouldTranslateSite } ?: return false
        target.shouldTranslateSite = !shouldTranslateSite(url)
        save(target)
        return shouldTranslateSite(url)
    }

    fun whiteBackground(url: String): Boolean = resolve(url) { it.shouldUseWhiteBackground } ?: false

    fun toggleWhiteBackground(url: String): Boolean {
        val target = writeTargetFor(url) { it.shouldUseWhiteBackground } ?: return false
        target.shouldUseWhiteBackground = !whiteBackground(url)
        save(target)
        return whiteBackground(url)
    }

    fun hasInvertedColor(url: String): Boolean = resolve(url) { it.shouldInvertColor } ?: false

    fun toggleInvertedColor(url: String): Boolean {
        val target = writeTargetFor(url) { it.shouldInvertColor } ?: return false
        target.shouldInvertColor = !hasInvertedColor(url)
        save(target)
        return hasInvertedColor(url)
    }

    fun setTranslationMode(url: String, mode: TranslationMode) {
        val target = writeTargetFor(url) { it.translationMode } ?: return
        target.translationMode = mode
        save(target)
    }

    /** Writing new code switches the script back on: a fresh save is meant to take effect. */
    fun setPostLoadJavascript(url: String, code: String?) {
        val target = writeTargetFor(url) { it.postLoadJavascript } ?: return
        target.postLoadJavascript = code?.ifBlank { null }
        target.postLoadJavascriptEnabled = true
        save(target)
    }

    fun setCustomCss(url: String, code: String?) {
        val target = writeTargetFor(url) { it.customCss } ?: return
        target.customCss = code?.ifBlank { null }
        target.customCssEnabled = true
        save(target)
    }

    // endregion

    // region per-site display overrides (null = use global setting)

    fun getFontSize(url: String): Int = resolve(url) { it.fontSize } ?: display.fontSize

    fun getFontType(url: String): FontType = resolve(url) { it.fontType } ?: display.fontType

    fun getBoldFontStyle(url: String): Boolean =
        resolve(url) { it.boldFontStyle } ?: display.boldFontStyle

    fun getBlackFontStyle(url: String): Boolean =
        resolve(url) { it.blackFontStyle } ?: display.blackFontStyle

    fun getFontBoldness(url: String): Int = resolve(url) { it.fontBoldness } ?: display.fontBoldness

    fun getDesktopMode(url: String): Boolean = resolve(url) { it.desktopMode } ?: browser.desktop

    fun getDesktopViewportWidth(url: String): Int? = resolve(url) { it.desktopViewportWidth }

    fun getEnableJavascript(url: String): Boolean =
        resolve(url) { it.enableJavascript } ?: browser.enableJavascript

    fun getEnableImages(url: String): Boolean =
        resolve(url) { it.enableImages } ?: browser.enableImages

    fun getTranslationMode(url: String): TranslationMode =
        resolve(url) { it.translationMode } ?: translation.translationMode

    /** The CSS to inject for [url]: first rule in the chain whose CSS is set and switched on. */
    fun getCustomCss(url: String): String? = resolve(url) { it.activeCustomCss }

    /** The JS to run after load for [url]: first rule in the chain whose JS is set and switched on. */
    fun getPostLoadJavascript(url: String): String? = resolve(url) { it.activePostLoadJavascript }

    // endregion

    companion object {
        private const val MAX_CACHED_RESOLUTIONS = 512
    }
}
