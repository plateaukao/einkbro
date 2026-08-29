package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.database.SiteRuleKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SiteRuleKeyTest {

    @Test
    fun `host and path are split from a key`() {
        assertEquals("example.com", SiteRuleKey.hostOf("example.com/docs/api"))
        assertEquals("/docs/api", SiteRuleKey.pathOf("example.com/docs/api"))
        assertEquals("example.com", SiteRuleKey.hostOf("example.com"))
        assertEquals("", SiteRuleKey.pathOf("example.com"))
    }

    @Test
    fun `url host is lower-cased and stripped of port and credentials`() {
        assertEquals("example.com", SiteRuleKey.hostOfUrl("https://user:pw@Example.COM:8443/a?b#c"))
        assertEquals("[::1]", SiteRuleKey.hostOfUrl("http://[::1]:8080/x"))
        assertNull(SiteRuleKey.hostOfUrl("about:blank"))
        assertNull(SiteRuleKey.hostOfUrl(""))
    }

    @Test
    fun `url path drops query, fragment and trailing slash`() {
        assertEquals("/docs/api", SiteRuleKey.pathOfUrl("https://example.com/docs/api/?q=1#top"))
        assertEquals("", SiteRuleKey.pathOfUrl("https://example.com/"))
        assertEquals("", SiteRuleKey.pathOfUrl("https://example.com"))
    }

    @Test
    fun `path rules match segment-aligned prefixes only`() {
        assertTrue(SiteRuleKey.matches("example.com/docs", "example.com", "/docs"))
        assertTrue(SiteRuleKey.matches("example.com/docs", "example.com", "/docs/api/v2"))
        assertFalse(SiteRuleKey.matches("example.com/docs", "example.com", "/docs-old"))
        assertFalse(SiteRuleKey.matches("example.com/docs", "example.com", "/"))
        assertTrue(SiteRuleKey.matches("example.com", "example.com", "/anything"))
    }

    @Test
    fun `hosts match exactly - no subdomain wildcard`() {
        assertFalse(SiteRuleKey.matches("example.com", "www.example.com", "/"))
        assertFalse(SiteRuleKey.matches("www.example.com", "example.com", "/"))
        assertTrue(SiteRuleKey.matches("Example.com", "example.com", "/"))
    }

    @Test
    fun `candidate keys walk from host to full path`() {
        assertEquals(
            listOf("example.com", "example.com/a", "example.com/a/b"),
            SiteRuleKey.candidateKeysFor("https://example.com/a/b/?x=1"),
        )
        assertEquals(listOf("example.com"), SiteRuleKey.candidateKeysFor("https://example.com"))
        assertEquals(emptyList<String>(), SiteRuleKey.candidateKeysFor("about:blank"))
    }

    @Test
    fun `specificity counts path segments`() {
        assertEquals(0, SiteRuleKey.specificity("example.com"))
        assertEquals(1, SiteRuleKey.specificity("example.com/a"))
        assertEquals(3, SiteRuleKey.specificity("example.com/a/b/c"))
    }
}

class DomainConfigManagerTest {

    private lateinit var display: DisplayConfig
    private lateinit var browser: BrowserConfig
    private lateinit var translation: TranslationConfig
    private val persisted = mutableListOf<DomainConfigurationData>()
    private val removed = mutableListOf<String>()
    private lateinit var manager: DomainConfigManager

    private val page = "https://example.com/docs/api/index.html"

    @Before
    fun setUp() {
        val sp = FakeSharedPreferences()
        display = DisplayConfig(sp)
        browser = BrowserConfig(sp)
        translation = TranslationConfig(sp)
        persisted.clear()
        removed.clear()
        manager = DomainConfigManager(
            display, browser, translation,
            persist = { persisted += it },
            remove = { removed += it },
        )
    }

    private fun put(rule: DomainConfigurationData) {
        manager.domainConfigurationMap[rule.domain] = rule
    }

    @Test
    fun `no rules resolves to global settings`() {
        display.fontSize = 120
        assertEquals(120, manager.getFontSize(page))
        assertEquals(browser.desktop, manager.getDesktopMode(page))
        assertFalse(manager.whiteBackground(page))
        assertTrue(manager.matchingRules(page).isEmpty())
    }

    @Test
    fun `host rule applies to every path`() {
        put(DomainConfigurationData("example.com", fontSize = 150))
        assertEquals(150, manager.getFontSize(page))
        assertEquals(150, manager.getFontSize("https://example.com/"))
        assertEquals(display.fontSize, manager.getFontSize("https://www.example.com/docs"))
    }

    @Test
    fun `most specific matching rule wins`() {
        put(DomainConfigurationData("example.com", fontSize = 150))
        put(DomainConfigurationData("example.com/docs", fontSize = 170))
        put(DomainConfigurationData("example.com/docs/api", fontSize = 190))
        put(DomainConfigurationData("example.com/blog", fontSize = 80))

        assertEquals(190, manager.getFontSize(page))
        assertEquals(170, manager.getFontSize("https://example.com/docs/guide"))
        assertEquals(150, manager.getFontSize("https://example.com/"))
        assertEquals(80, manager.getFontSize("https://example.com/blog/post"))
        assertEquals(
            listOf("example.com/docs/api", "example.com/docs", "example.com"),
            manager.matchingKeys(page),
        )
    }

    @Test
    fun `fields cascade independently through the chain`() {
        put(DomainConfigurationData("example.com", fontSize = 150, customCss = "body{}", desktopMode = true))
        put(DomainConfigurationData("example.com/docs", fontSize = 170))

        assertEquals(170, manager.getFontSize(page))
        assertEquals("body{}", manager.getCustomCss(page))
        assertTrue(manager.getDesktopMode(page))

        val effective = manager.getEffectiveConfig(page)
        assertEquals("example.com/docs", effective.domain)
        assertEquals(170, effective.fontSize)
        assertEquals("body{}", effective.customCss)
        assertEquals(true, effective.desktopMode)
    }

    @Test
    fun `explicit false on a path rule overrides true on the host rule`() {
        put(DomainConfigurationData("example.com", shouldUseWhiteBackground = true, desktopMode = true))
        put(DomainConfigurationData("example.com/docs", shouldUseWhiteBackground = false, desktopMode = false))

        assertTrue(manager.whiteBackground("https://example.com/"))
        assertFalse(manager.whiteBackground(page))
        assertTrue(manager.getDesktopMode("https://example.com/"))
        assertFalse(manager.getDesktopMode(page))
    }

    @Test
    fun `inherited config excludes the rule being edited and deeper rules`() {
        put(DomainConfigurationData("example.com", fontSize = 150))
        put(DomainConfigurationData("example.com/docs", fontSize = 170))
        put(DomainConfigurationData("example.com/docs/api", fontSize = 190))

        val inherited = manager.getInheritedConfig("https://example.com/docs", "example.com/docs")
        assertEquals(150, inherited.fontSize)
        assertEquals("example.com", inherited.domain)
    }

    @Test
    fun `quick toggle writes to the host rule when no path rule sets the field`() {
        put(DomainConfigurationData("example.com/docs", fontSize = 170))

        assertTrue(manager.toggleWhiteBackground(page))

        assertEquals(true, manager.getRule("example.com")?.shouldUseWhiteBackground)
        assertNull(manager.getRule("example.com/docs")?.shouldUseWhiteBackground)
        assertEquals("example.com", persisted.single().domain)
    }

    @Test
    fun `quick toggle writes to the path rule that already sets the field`() {
        put(DomainConfigurationData("example.com", shouldUseWhiteBackground = true))
        put(DomainConfigurationData("example.com/docs", shouldUseWhiteBackground = false))

        assertFalse(manager.whiteBackground(page))
        assertTrue(manager.toggleWhiteBackground(page))

        assertEquals(true, manager.getRule("example.com/docs")?.shouldUseWhiteBackground)
        assertEquals(true, manager.getRule("example.com")?.shouldUseWhiteBackground)
        assertEquals("example.com/docs", persisted.single().domain)
    }

    @Test
    fun `quick toggle flips the effective value when host sets it and path inherits`() {
        put(DomainConfigurationData("example.com", shouldInvertColor = true))

        assertTrue(manager.hasInvertedColor(page))
        assertFalse(manager.toggleInvertedColor(page))
        assertEquals(false, manager.getRule("example.com")?.shouldInvertColor)
        assertFalse(manager.hasInvertedColor("https://example.com/other"))
    }

    @Test
    fun `translation mode setter follows the same write target rule`() {
        put(DomainConfigurationData("example.com/docs", translationMode = TranslationMode.GOOGLE_URL))

        manager.setTranslationMode(page, TranslationMode.TRANSLATE_BY_PARAGRAPH)
        assertEquals(
            TranslationMode.TRANSLATE_BY_PARAGRAPH,
            manager.getRule("example.com/docs")?.translationMode,
        )
        assertNull(manager.getRule("example.com"))

        manager.setTranslationMode("https://example.com/blog", TranslationMode.GOOGLE_IN_PLACE)
        assertEquals(TranslationMode.GOOGLE_IN_PLACE, manager.getRule("example.com")?.translationMode)
    }

    @Test
    fun `delete removes the rule and notifies persistence`() {
        put(DomainConfigurationData("example.com/docs", fontSize = 170))
        manager.deleteRule("example.com/docs")
        assertNull(manager.getRule("example.com/docs"))
        assertEquals(listOf("example.com/docs"), removed)

        manager.deleteRule("example.com/missing")
        assertEquals(1, removed.size)
    }

    @Test
    fun `saving a rule with nothing set removes it`() {
        put(DomainConfigurationData("example.com", fontSize = 150))
        manager.updateDomainConfig(DomainConfigurationData("example.com", shouldInvertColor = false))
        assertNull(manager.getRule("example.com"))
        assertEquals(listOf("example.com"), removed)
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun `allRules skips empty leftovers and groups by host`() {
        put(DomainConfigurationData("b.com/docs", fontSize = 1))
        put(DomainConfigurationData("b.com", shouldUseWhiteBackground = false))
        put(DomainConfigurationData("a.com", fontSize = 2))
        put(DomainConfigurationData("c.com"))
        assertEquals(listOf("a.com", "b.com/docs"), manager.allRules().map { it.domain })
    }

    @Test
    fun `rulesForHost lists host rule first then paths`() {
        put(DomainConfigurationData("example.com/docs/api"))
        put(DomainConfigurationData("example.com"))
        put(DomainConfigurationData("example.com/blog"))
        put(DomainConfigurationData("other.com"))

        assertEquals(
            listOf("example.com", "example.com/blog", "example.com/docs/api"),
            manager.rulesForHost("example.com").map { it.domain },
        )
    }

    @Test
    fun `blank css and js are treated as unset`() {
        put(DomainConfigurationData("example.com", customCss = "a{}", postLoadJavascript = "x()"))
        put(DomainConfigurationData("example.com/docs", customCss = "  ", postLoadJavascript = ""))

        assertEquals("a{}", manager.getCustomCss(page))
        assertEquals("x()", manager.getPostLoadJavascript(page))
    }

    @Test
    fun `switched-off css and js are skipped and fall through to the parent rule`() {
        put(DomainConfigurationData("example.com", customCss = "a{}", postLoadJavascript = "x()"))
        put(
            DomainConfigurationData(
                "example.com/docs", customCss = "b{}", postLoadJavascript = "y()",
                customCssEnabled = false, postLoadJavascriptEnabled = false,
            )
        )

        // the path rule keeps its code but the host rule's scripts apply
        assertEquals("a{}", manager.getCustomCss(page))
        assertEquals("x()", manager.getPostLoadJavascript(page))
        assertEquals("b{}", manager.getRule("example.com/docs")!!.customCss)

        // switching the host rule off too leaves nothing to inject
        manager.getRule("example.com")!!.customCssEnabled = false
        manager.getRule("example.com")!!.postLoadJavascriptEnabled = false
        assertNull(manager.getCustomCss(page))
        assertNull(manager.getPostLoadJavascript(page))
        assertNull(manager.getEffectiveConfig(page).customCss)
    }

    @Test
    fun `saving new code switches a disabled script back on`() {
        put(DomainConfigurationData("example.com", customCss = "a{}", customCssEnabled = false))
        put(DomainConfigurationData("example.com", postLoadJavascript = "x()", postLoadJavascriptEnabled = false)
            .let { it.copy(customCss = "a{}", customCssEnabled = false) })

        manager.setCustomCss(page, "b{}")
        manager.setPostLoadJavascript(page, "y()")

        val rule = manager.getRule("example.com")!!
        assertTrue(rule.customCssEnabled)
        assertTrue(rule.postLoadJavascriptEnabled)
        assertEquals("b{}", manager.getCustomCss(page))
        assertEquals("y()", manager.getPostLoadJavascript(page))
    }

    @Test
    fun `rows written before the switches existed decode as enabled`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val old = json.decodeFromString(
            DomainConfigurationData.serializer(),
            """{"domain":"example.com","customCss":"a{}","postLoadJavascript":"x()"}""",
        )
        assertTrue(old.customCssEnabled)
        assertTrue(old.postLoadJavascriptEnabled)
        assertEquals("a{}", old.activeCustomCss)
        assertNull(old.copy(customCssEnabled = false).activeCustomCss)
    }

    @Test
    fun `mergedWith carries the switch with whichever side supplied the script`() {
        val local = DomainConfigurationData("example.com", customCss = "a{}", customCssEnabled = false)
        val backup = DomainConfigurationData(
            "example.com", customCss = "b{}", postLoadJavascript = "x()", postLoadJavascriptEnabled = false,
        )
        val merged = local.mergedWith(backup)
        assertEquals("a{}", merged.customCss)
        assertFalse(merged.customCssEnabled)
        assertEquals("x()", merged.postLoadJavascript)
        assertFalse(merged.postLoadJavascriptEnabled)
    }

    @Test
    fun `mergedWith keeps local values and fills gaps from the backup`() {
        val local = DomainConfigurationData("example.com", fontSize = 150, customCss = " ")
        val backup = DomainConfigurationData(
            "example.com", fontSize = 170, desktopMode = true, customCss = "a{}",
            shouldUseWhiteBackground = true,
        )
        val merged = local.mergedWith(backup)
        assertEquals(150, merged.fontSize)
        assertEquals(true, merged.desktopMode)
        assertEquals("a{}", merged.customCss)
        assertEquals(true, merged.shouldUseWhiteBackground)

        // an empty leftover row takes everything from the backup
        val leftover = DomainConfigurationData("example.com", shouldInvertColor = false)
            .normalizedLegacyFlags()
        assertEquals(backup.copy(), leftover.mergedWith(backup))
    }

    @Test
    fun `legacy host rows normalise false flags to unset`() {
        val legacy = DomainConfigurationData(
            "example.com",
            shouldFixScroll = false, shouldTranslateSite = true,
            shouldUseWhiteBackground = false, shouldInvertColor = false,
            fontSize = 120,
        ).normalizedLegacyFlags()
        assertNull(legacy.shouldFixScroll)
        assertEquals(true, legacy.shouldTranslateSite)
        assertNull(legacy.shouldUseWhiteBackground)
        assertEquals(2, legacy.overrideCount)

        val pathRule = DomainConfigurationData("example.com/docs", shouldInvertColor = false)
            .normalizedLegacyFlags()
        assertEquals(false, pathRule.shouldInvertColor)
    }
}
