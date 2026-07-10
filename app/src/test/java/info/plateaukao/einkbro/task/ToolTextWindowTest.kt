package info.plateaukao.einkbro.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolTextWindowTest {

    @Test
    fun `short document is returned whole with end-of-document header`() {
        val result = ToolTextWindow.window("hello world", maxChars = 100)
        assertEquals("[chars 0..11 of 11; end of document]\nhello world", result)
    }

    @Test
    fun `long document is windowed and header tells the next offset`() {
        val content = "a".repeat(50)
        val result = ToolTextWindow.window(content, maxChars = 20)
        assertEquals("[chars 0..20 of 50; continue with offset=20]\n" + "a".repeat(20), result)
    }

    @Test
    fun `offset pages through the document`() {
        val content = "0123456789".repeat(5) // 50 chars
        val result = ToolTextWindow.window(content, maxChars = 20, offset = 20)
        assertEquals("[chars 20..40 of 50; continue with offset=40]\n" + content.substring(20, 40), result)
    }

    @Test
    fun `offset past end returns empty window instead of crashing`() {
        val result = ToolTextWindow.window("short", maxChars = 10, offset = 999)
        assertEquals("[chars 5..5 of 5; end of document]\n", result)
    }

    @Test
    fun `negative offset is clamped to zero`() {
        val result = ToolTextWindow.window("hello", maxChars = 10, offset = -5)
        assertTrue(result.startsWith("[chars 0..5 of 5"))
    }

    @Test
    fun `search jumps to match with pre-context`() {
        val content = "x".repeat(1000) + "<div class=\"ad-banner\">buy now</div>" + "y".repeat(1000)
        val result = ToolTextWindow.window(content, maxChars = 100, search = "ad-banner")
        // match at 1012; pre-context capped to maxChars/2 = 50, so window starts at 962
        assertTrue(result.startsWith("[chars 962.."))
        assertTrue(result.contains("ad-banner"))
    }

    @Test
    fun `search is case-insensitive`() {
        val content = "x".repeat(300) + "COOKIE-CONSENT" + "y".repeat(300)
        val result = ToolTextWindow.window(content, maxChars = 100, search = "cookie-consent")
        assertTrue(result.contains("COOKIE-CONSENT"))
    }

    @Test
    fun `search respects starting offset to find later occurrences`() {
        val content = "match" + "x".repeat(500) + "match" + "y".repeat(100)
        val result = ToolTextWindow.window(content, maxChars = 50, offset = 100, search = "match")
        // second occurrence is at 505; pre-context capped to 25, so window starts at 480
        assertTrue(result.startsWith("[chars 480.."))
        assertTrue(result.contains("match"))
    }

    @Test
    fun `search miss reports not found with document size`() {
        val result = ToolTextWindow.window("hello world", maxChars = 100, search = "absent")
        assertEquals("not found: \"absent\" (searched from offset 0; document is 11 chars)", result)
    }

    @Test
    fun `search near document start clamps pre-context to zero`() {
        val content = "needle" + "x".repeat(500)
        val result = ToolTextWindow.window(content, maxChars = 50, search = "needle")
        assertTrue(result.startsWith("[chars 0.."))
        assertTrue(result.contains("needle"))
    }
}
