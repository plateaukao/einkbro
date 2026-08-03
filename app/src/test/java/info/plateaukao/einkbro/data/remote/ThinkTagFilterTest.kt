package info.plateaukao.einkbro.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkTagFilterTest {

    @Test
    fun `plain text passes through untouched`() {
        val filter = ThinkTagFilter()
        assertEquals("Hello ", filter.filter("Hello "))
        assertEquals("world", filter.filter("world"))
        assertFalse(filter.thinking)
    }

    @Test
    fun `think block at stream start is suppressed and flagged`() {
        val filter = ThinkTagFilter()
        assertEquals("", filter.filter("<think>"))
        assertTrue(filter.thinking)
        assertEquals("", filter.filter("pondering the question"))
        assertTrue(filter.thinking)
        assertEquals("The answer is 42.", filter.filter("</think>\n\nThe answer is 42."))
        assertFalse(filter.thinking)
        assertEquals(" More.", filter.filter(" More."))
    }

    @Test
    fun `whole think block in a single chunk`() {
        val filter = ThinkTagFilter()
        assertEquals("Answer.", filter.filter("<think>hmm</think>\nAnswer."))
        assertFalse(filter.thinking)
    }

    @Test
    fun `think tag after answer text is not treated as reasoning`() {
        val filter = ThinkTagFilter()
        assertEquals("Discussing ", filter.filter("Discussing "))
        assertEquals("<think> tags in prose", filter.filter("<think> tags in prose"))
        assertFalse(filter.thinking)
    }

    @Test
    fun `leading whitespace before the think tag is tolerated`() {
        val filter = ThinkTagFilter()
        assertEquals("", filter.filter("\n<think>reason"))
        assertTrue(filter.thinking)
    }

    // ── ChatDelta reasoning channel ──────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `delta parses reasoning_content`() {
        val delta = json.decodeFromString(
            ChatDelta.serializer(),
            """{"content":null,"reasoning_content":"let me think"}"""
        )
        assertEquals("let me think", delta.reasoningContent)
        assertNull(delta.content)
    }

    @Test
    fun `delta parses the alternative reasoning key`() {
        val delta = json.decodeFromString(
            ChatDelta.serializer(),
            """{"reasoning":"hmm"}"""
        )
        assertEquals("hmm", delta.reasoningContent)
    }

    @Test
    fun `plain content delta still parses`() {
        val delta = json.decodeFromString(ChatDelta.serializer(), """{"content":"hi"}""")
        assertEquals("hi", delta.content)
        assertNull(delta.reasoningContent)
    }
}
