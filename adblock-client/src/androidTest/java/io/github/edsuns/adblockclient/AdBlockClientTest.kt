package io.github.edsuns.adblockclient

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real native library together with the Kotlin-side regex rules:
 * plain rules go through the engine, `/regex/` rules through [RegexFilterSet],
 * and both survive the processed-data round trip.
 */
@RunWith(AndroidJUnit4::class)
class AdBlockClientTest {

    private val list = """
        ! Title: test list
        ||ads.example.net^
        @@||ads.example.net/allowed^
        /^https?:\/\/tracker\.[a-z]+\.test\/t\d+\.js/${'$'}script
        @@/\/t42\.js/
        example.org##.banner
    """.trimIndent()

    private fun freshClient(): AdBlockClient = AdBlockClient("test").apply {
        loadBasicData(list.toByteArray(), preserveRules = true)
    }

    private fun roundTripped(): AdBlockClient {
        val processed = freshClient().getProcessedData()
        return AdBlockClient("test-rt").apply { loadProcessedData(processed) }
    }

    private fun verify(client: AdBlockClient) {
        // native rule
        val native = client.matches("https://ads.example.net/a.js", "https://site.com/", ResourceType.SCRIPT)
        assertTrue(native.shouldBlock)
        assertEquals("||ads.example.net^", native.matchedRule)
        // native exception
        val nativeExc = client.matches("https://ads.example.net/allowed/a.js", "https://site.com/", ResourceType.SCRIPT)
        assertFalse(nativeExc.shouldBlock)
        assertTrue(nativeExc.hasException)
        // regex rule, handled in Kotlin
        val regex = client.matches("https://tracker.foo.test/t7.js", "https://site.com/", ResourceType.SCRIPT)
        assertTrue(regex.shouldBlock)
        assertEquals("/^https?:\\/\\/tracker\\.[a-z]+\\.test\\/t\\d+\\.js/\$script", regex.matchedRule)
        // regex rule respects the resource type option
        assertFalse(client.matches("https://tracker.foo.test/t7.js", "https://site.com/", ResourceType.IMAGE).shouldBlock)
        // regex exception wins over the regex block
        val regexExc = client.matches("https://tracker.foo.test/t42.js", "https://site.com/", ResourceType.SCRIPT)
        assertFalse(regexExc.shouldBlock)
        assertEquals("@@/\\/t42\\.js/", regexExc.matchedExceptionRule)
        // nothing matches an unrelated URL
        val none = client.matches("https://cdn.site.com/app.js", "https://site.com/", ResourceType.SCRIPT)
        assertFalse(none.shouldBlock)
        assertNull(none.matchedExceptionRule)
        // cosmetic filters still come from the engine
        assertEquals(".banner", client.getElementHidingSelectors("https://example.org/"))
    }

    @Test
    fun matchesFromRawList() = verify(freshClient())

    @Test
    fun matchesAfterProcessedDataRoundTrip() = verify(roundTripped())

    @Test
    fun filtersCountIncludesRegexRules() {
        // 4 engine rules (2 block/exception + cosmetic) + 2 regex rules
        val client = freshClient()
        assertEquals(client.getFiltersCount(), roundTripped().getFiltersCount())
        assertTrue(client.getFiltersCount() >= 5)
    }

    @Test
    fun legacyProcessedDataWithoutRegexSectionStillLoads() {
        val plainList = "||ads.example.net^\n"
        val legacy = AdBlockClient("legacy").apply { loadBasicData(plainList.toByteArray(), true) }.getProcessedData()
        val client = AdBlockClient("legacy-rt").apply { loadProcessedData(legacy) }
        assertTrue(client.matches("https://ads.example.net/a.js", "https://site.com/", ResourceType.SCRIPT).shouldBlock)
        assertFalse(client.matches("https://tracker.foo.test/t7.js", "https://site.com/", ResourceType.SCRIPT).shouldBlock)
    }
}
