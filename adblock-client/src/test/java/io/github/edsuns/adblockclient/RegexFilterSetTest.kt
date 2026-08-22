package io.github.edsuns.adblockclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexFilterSetTest {

    private val noMatch = MatchResult(false, null, null)

    private fun set(vararg lines: String) = RegexFilterSet.parse(lines.joinToString("\n"))

    private fun RegexFilterSet.check(
        url: String,
        documentDomain: String = "example.com",
        type: ResourceType = ResourceType.SCRIPT,
        native: MatchResult = noMatch,
    ) = combine(native, url, documentDomain, type)

    @Test
    fun plainRegexBlocks() {
        val s = set("/^https?:\\/\\/ads\\.example\\.net\\/[a-z]{4}\\.js/")
        assertTrue(s.check("https://ads.example.net/abcd.js").shouldBlock)
        assertFalse(s.check("https://ads.example.net/abc.js").shouldBlock)
        assertEquals("/^https?:\\/\\/ads\\.example\\.net\\/[a-z]{4}\\.js/", s.check("https://ads.example.net/abcd.js").matchedRule)
    }

    @Test
    fun caseInsensitiveUnlessMatchCase() {
        assertTrue(set("/TRACKER\\.js/").check("https://cdn.example.com/tracker.js").shouldBlock)
        assertFalse(set("/TRACKER\\.js/\$match-case").check("https://cdn.example.com/tracker.js").shouldBlock)
        assertTrue(set("/TRACKER\\.js/\$match-case").check("https://cdn.example.com/TRACKER.js").shouldBlock)
    }

    @Test
    fun resourceTypeOptions() {
        val s = set("/\\/ad\\d+\\.js/\$script")
        assertTrue(s.check("https://x.com/ad12.js", type = ResourceType.SCRIPT).shouldBlock)
        assertFalse(s.check("https://x.com/ad12.js", type = ResourceType.IMAGE).shouldBlock)
        // unknown resource type never matches a typed rule
        assertFalse(s.check("https://x.com/ad12.js", type = ResourceType.UNKNOWN).shouldBlock)
        // but does match an untyped one
        assertTrue(set("/\\/ad\\d+\\.js/").check("https://x.com/ad12.js", type = ResourceType.UNKNOWN).shouldBlock)
        // ~type excludes
        assertFalse(set("/\\/ad\\d+\\.js/\$~script").check("https://x.com/ad12.js", type = ResourceType.SCRIPT).shouldBlock)
        assertTrue(set("/\\/ad\\d+\\.js/\$~image").check("https://x.com/ad12.js", type = ResourceType.SCRIPT).shouldBlock)
    }

    @Test
    fun thirdPartyOptions() {
        val s = set("/\\/pixel\\.gif/\$third-party")
        assertTrue(s.check("https://tracker.net/pixel.gif", documentDomain = "example.com", type = ResourceType.IMAGE).shouldBlock)
        assertFalse(s.check("https://static.example.com/pixel.gif", documentDomain = "example.com", type = ResourceType.IMAGE).shouldBlock)
        val first = set("/\\/pixel\\.gif/\$~third-party")
        assertFalse(first.check("https://tracker.net/pixel.gif", documentDomain = "example.com", type = ResourceType.IMAGE).shouldBlock)
        assertTrue(first.check("https://example.com/pixel.gif", documentDomain = "example.com", type = ResourceType.IMAGE).shouldBlock)
        // "notexample.com" is not a subdomain of "example.com"
        assertTrue(s.check("https://notexample.com/pixel.gif", documentDomain = "example.com", type = ResourceType.IMAGE).shouldBlock)
    }

    @Test
    fun domainOptions() {
        val s = set("/\\/banner\\d+/\$image,domain=site.com|~sub.site.com|other.*")
        assertTrue(s.check("https://cdn.net/banner1", documentDomain = "site.com", type = ResourceType.IMAGE).shouldBlock)
        assertTrue(s.check("https://cdn.net/banner1", documentDomain = "m.site.com", type = ResourceType.IMAGE).shouldBlock)
        assertFalse(s.check("https://cdn.net/banner1", documentDomain = "sub.site.com", type = ResourceType.IMAGE).shouldBlock)
        assertTrue(s.check("https://cdn.net/banner1", documentDomain = "other.co.uk", type = ResourceType.IMAGE).shouldBlock)
        assertFalse(s.check("https://cdn.net/banner1", documentDomain = "unrelated.org", type = ResourceType.IMAGE).shouldBlock)
        // anti-domain only: applies everywhere except there
        val anti = set("/\\/banner\\d+/\$domain=~site.com")
        assertTrue(anti.check("https://cdn.net/banner1", documentDomain = "unrelated.org").shouldBlock)
        assertFalse(anti.check("https://cdn.net/banner1", documentDomain = "site.com").shouldBlock)
    }

    @Test
    fun exceptionsWinAndSurviveNativeBlock() {
        val s = set("/\\/ads\\//", "@@/\\/ads\\/allowed/")
        assertTrue(s.check("https://x.com/ads/a.js").shouldBlock)
        val r = s.check("https://x.com/ads/allowed/a.js")
        assertFalse(r.shouldBlock)
        assertTrue(r.hasException)
        // regex exception overrides a native block, keeping the native rule text
        val nativeBlock = MatchResult(true, "||x.com^", null)
        val combined = s.check("https://x.com/ads/allowed/a.js", native = nativeBlock)
        assertFalse(combined.shouldBlock)
        assertEquals("||x.com^", combined.matchedRule)
        assertEquals("@@/\\/ads\\/allowed/", combined.matchedExceptionRule)
        // a native exception is returned untouched
        val nativeException = MatchResult(false, "||x.com^", "@@||x.com/ads")
        assertEquals(nativeException, s.check("https://x.com/ads/a.js", native = nativeException))
    }

    @Test
    fun unsupportedOptionsDisableRule() {
        assertTrue(set("/\\/ads\\//\$popup").isEmpty())
        assertTrue(set("/\\/ads\\//\$script,header=set-cookie:/^GL_UI4=/").isEmpty())
        assertTrue(set("/\\/ads\\//\$badfilter").isEmpty())
        // a pattern whose own text ends in "/$x/" is still a plain regex rule
        assertEquals(1, set("/foo\\/\$bar\\//").size)
        assertTrue(set("/\\/ads\\//\$csp=script-src 'self'").isEmpty())
        // behavioural-only options keep the rule
        assertEquals(1, set("/\\/ads\\//\$script,redirect=noopjs").size)
        assertEquals(1, set("/\\/ads\\//\$important").size)
    }

    @Test
    fun nonRegexLinesIgnored() {
        assertTrue(set("! comment", "||ads.example.com^", "example.com##.ad", "/not-a-regex", "").isEmpty())
        assertTrue(set("/x/").isEmpty().not())
        // An invalid pattern parses (so it is counted) but never matches
        assertFalse(set("/[unclosed/").check("https://x.com/[unclosed").shouldBlock)
    }

    @Test
    fun onlyBlockableProtocols() {
        val s = set("/\\/ads\\//")
        assertTrue(s.check("https://x.com/ads/a.js").shouldBlock)
        assertTrue(s.check("wss://x.com/ads/a").shouldBlock)
        assertTrue(s.check("blob:https://x.com/ads/a").shouldBlock)
        assertFalse(s.check("file:///ads/a.js").shouldBlock)
        assertFalse(s.check("data:text/plain,/ads/").shouldBlock)
    }

    @Test
    fun requiredLiteralExtraction() {
        assertEquals("139.45.197.2", RegexRule.requiredLiteral("^139\\.45\\.197\\.2(4[0-9]|5[0-4])"))
        assertEquals("51.195.31.", RegexRule.requiredLiteral("(https?:\\/\\/)51\\.195\\.31\\..{100,}"))
        assertEquals("doseofporn", RegexRule.requiredLiteral("doseofporn.com\\/[a-z]{0,5}[1-9]{0,5}"))
        assertEquals("https://rayinfosports.com/wp-content/uploads/", RegexRule.requiredLiteral("^https:\\/\\/rayinfosports\\.com\\/wp-content\\/uploads\\/[a-z]{8}\\.js\\?ver="))
        // optional last char is dropped
        assertEquals("abc", RegexRule.requiredLiteral("abcd?e"))
        assertEquals("abc", RegexRule.requiredLiteral("abcd*"))
        assertEquals("abc", RegexRule.requiredLiteral("abcd{0,3}"))
        // '+' keeps the char but breaks the run
        assertEquals("abcd", RegexRule.requiredLiteral("abcd+xyz"))
        // top-level alternation: nothing is required
        assertNull(RegexRule.requiredLiteral("abcdef|ghijkl"))
        // escaped class sequences break the run
        assertEquals("bar", RegexRule.requiredLiteral("fo\\dbar"))
        assertNull(RegexRule.requiredLiteral("\\d+"))
        // every top-level run is required, longest first
        assertEquals(listOf("https://", "/z-"), RegexRule.requiredLiterals("^https:\\/\\/[0-9a-z]{5,}\\.[a-z]{2,3}\\/z-[5-9]\\d{6}$"))
    }

    @Test
    fun anchoredPatternsStillMatchOnlyAtStart() {
        val s = set("/^https:\\/\\/x\\.com\\/ad\\.js/")
        assertTrue(s.check("https://x.com/ad.js").shouldBlock)
        assertFalse(s.check("https://y.com/?u=https://x.com/ad.js").shouldBlock)
        // unanchored pattern matches anywhere
        assertTrue(set("/x\\.com\\/ad\\.js/").check("https://y.com/?u=https://x.com/ad.js").shouldBlock)
    }

    @Test
    fun literalPrefilterDoesNotChangeResults() {
        // The literal for this rule is "ads.example.net/" (case-folded); mixed-case URLs still match.
        val s = set("/ads\\.example\\.net\\/\\d+/")
        assertTrue(s.check("https://ADS.Example.NET/123").shouldBlock)
        assertFalse(s.check("https://ads.example.org/123").shouldBlock)
    }

    @Test
    fun packRoundTripAndLegacyBlob() {
        val native = ByteArray(1000) { (it * 7).toByte() }
        val s = set("/\\/ads\\//\$script", "@@/\\/ads\\/ok/", "! skipped")
        val packed = RegexFilterSet.pack(native, s)
        val unpacked = RegexFilterSet.unpack(packed)
        assertEquals(native.size, unpacked.nativeLength)
        assertTrue(native.contentEquals(packed.copyOf(unpacked.nativeLength)))
        assertEquals(2, unpacked.regexSet.size)
        assertTrue(unpacked.regexSet.check("https://x.com/ads/a.js").shouldBlock)
        assertTrue(unpacked.regexSet.check("https://x.com/ads/ok/a.js").hasException)

        // an empty set adds nothing
        assertTrue(RegexFilterSet.pack(native, RegexFilterSet.EMPTY).contentEquals(native))
        // a blob from before the regex section existed loads as-is
        val legacy = RegexFilterSet.unpack(native)
        assertEquals(native.size, legacy.nativeLength)
        assertTrue(legacy.regexSet.isEmpty())
        // a tiny/odd blob is also left alone
        assertEquals(3, RegexFilterSet.unpack(byteArrayOf(1, 2, 3)).nativeLength)
        assertEquals(8, RegexFilterSet.unpack("EBRXEBRX".toByteArray()).nativeLength)
    }

    @Test
    fun hostHelpers() {
        assertEquals("ads.example.net", MatchContext.urlHost("https://ads.example.net/x?y=1"))
        assertEquals("ads.example.net", MatchContext.urlHost("https://ads.example.net"))
        assertEquals("ads.example.net", MatchContext.urlHost("http://ads.example.net:8080/"))
        assertFalse(MatchContext.isThirdPartyHost("example.com", "example.com"))
        assertFalse(MatchContext.isThirdPartyHost("example.com", "www.example.com"))
        assertTrue(MatchContext.isThirdPartyHost("example.com", "notexample.com"))
        assertTrue(MatchContext.isThirdPartyHost("example.com", "other.org"))
    }
}
