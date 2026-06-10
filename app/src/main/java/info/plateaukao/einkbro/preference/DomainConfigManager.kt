package info.plateaukao.einkbro.preference

import android.net.Uri
import info.plateaukao.einkbro.database.DomainConfigurationData

class DomainConfigManager(
    private val display: DisplayConfig,
    private val browser: BrowserConfig,
    private val translation: TranslationConfig,
    private val persist: (DomainConfigurationData) -> Unit,
) {
    var domainConfigurationMap = mutableMapOf<String, DomainConfigurationData>()

    fun shouldFixScroll(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldFixScroll } ?: false

    fun toggleFixScroll(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldFixScroll = !config.shouldFixScroll
        persist(config)

        return shouldFixScroll(url)
    }

    fun shouldTranslateSite(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldTranslateSite } ?: false

    fun toggleTranslateSite(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldTranslateSite = !config.shouldTranslateSite
        persist(config)

        return shouldTranslateSite(url)
    }

    fun whiteBackground(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldUseWhiteBackground } ?: false

    fun toggleWhiteBackground(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldUseWhiteBackground = !config.shouldUseWhiteBackground
        persist(config)
        return whiteBackground(url)
    }

    fun hasInvertedColor(url: String): Boolean =
        Uri.parse(url)?.host?.let { domainConfigurationMap[it]?.shouldInvertColor } ?: false

    fun toggleInvertedColor(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return false

        val config = domainConfigurationMap.getOrPut(host) { DomainConfigurationData(host) }
        config.shouldInvertColor = !config.shouldInvertColor
        persist(config)
        return hasInvertedColor(url)
    }

    // Per-site display overrides (null = use global setting)

    fun getFontSize(url: String): Int {
        val host = Uri.parse(url)?.host ?: return display.fontSize
        return domainConfigurationMap[host]?.fontSize ?: display.fontSize
    }

    fun getFontType(url: String): FontType {
        val host = Uri.parse(url)?.host ?: return display.fontType
        return domainConfigurationMap[host]?.fontType ?: display.fontType
    }

    fun getBoldFontStyle(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return display.boldFontStyle
        return domainConfigurationMap[host]?.boldFontStyle ?: display.boldFontStyle
    }

    fun getBlackFontStyle(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return display.blackFontStyle
        return domainConfigurationMap[host]?.blackFontStyle ?: display.blackFontStyle
    }

    fun getFontBoldness(url: String): Int {
        val host = Uri.parse(url)?.host ?: return display.fontBoldness
        return domainConfigurationMap[host]?.fontBoldness ?: display.fontBoldness
    }

    fun getDesktopMode(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return browser.desktop
        return domainConfigurationMap[host]?.desktopMode ?: browser.desktop
    }

    fun getDesktopViewportWidth(url: String): Int? {
        val host = Uri.parse(url)?.host ?: return null
        return domainConfigurationMap[host]?.desktopViewportWidth
    }

    fun getEnableJavascript(url: String): Boolean {
        val host = Uri.parse(url)?.host ?: return browser.enableJavascript
        return domainConfigurationMap[host]?.enableJavascript ?: browser.enableJavascript
    }

    fun getTranslationMode(url: String): TranslationMode {
        val host = Uri.parse(url)?.host ?: return translation.translationMode
        return domainConfigurationMap[host]?.translationMode ?: translation.translationMode
    }

    fun getCustomCss(url: String): String? {
        val host = Uri.parse(url)?.host ?: return null
        return domainConfigurationMap[host]?.customCss?.takeIf { it.isNotBlank() }
    }

    fun getPostLoadJavascript(url: String): String? {
        val host = Uri.parse(url)?.host ?: return null
        return domainConfigurationMap[host]?.postLoadJavascript?.takeIf { it.isNotBlank() }
    }

    fun getDomainConfig(url: String): DomainConfigurationData {
        val host = Uri.parse(url)?.host ?: return DomainConfigurationData("")
        return domainConfigurationMap[host] ?: DomainConfigurationData(host)
    }

    fun updateDomainConfig(config: DomainConfigurationData) {
        if (config.domain.isBlank()) return
        domainConfigurationMap[config.domain] = config
        persist(config)
    }
}
