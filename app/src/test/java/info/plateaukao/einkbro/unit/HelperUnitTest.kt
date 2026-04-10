package info.plateaukao.einkbro.unit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelperUnitTest {

    @Test
    fun `pruneWebTitle removes text after pipe`() {
        assertEquals("Google", "Google | Search".pruneWebTitle())
    }

    @Test
    fun `pruneWebTitle removes text after dash`() {
        assertEquals("Article Title", "Article Title - News Site".pruneWebTitle())
    }

    @Test
    fun `pruneWebTitle keeps title without separator`() {
        assertEquals("Simple Title", "Simple Title".pruneWebTitle())
    }

    @Test
    fun `pruneWebTitle prefers pipe over dash`() {
        assertEquals("First Part", "First Part | Second - Third".pruneWebTitle())
    }

    @Test
    fun `getWordCount returns 0 for empty string`() {
        assertEquals(0, "".getWordCount())
    }

    @Test
    fun `getWordCount counts English words`() {
        assertEquals(4, "This is a test".getWordCount())
    }

    @Test
    fun `getWordCount counts CJK characters`() {
        val text = "これはテストです。"
        assertTrue(text.getWordCount() > 0)
    }

    @Test
    fun `getWordCount counts Korean syllables`() {
        val text = "테스트 문장입니다"
        assertTrue(text.getWordCount() > 0)
    }

    @Test
    fun `processedTextToChunks splits at sentence boundaries`() {
        val text = "First sentence. Second sentence. Third sentence."
        val chunks = processedTextToChunks(text)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.size <= 3)
    }

    @Test
    fun `processedTextToChunks handles empty text`() {
        val chunks = processedTextToChunks("")
        assertTrue(chunks.size <= 1)
    }

    @Test
    fun `processedTextToChunks merges short sentences`() {
        val text = "Hi. Hey. Ok."
        val chunks = processedTextToChunks(text)
        // Short sentences should be merged into one chunk
        assertEquals(1, chunks.size)
    }

    @Test
    fun `processedTextToChunks removes escaped characters`() {
        val text = "Line one\\nLine two\\tTabbed\\\"Quoted\\\\"
        val chunks = processedTextToChunks(text)
        assertTrue(chunks.isNotEmpty())
        for (chunk in chunks) {
            assertTrue(!chunk.contains("\\n"))
            assertTrue(!chunk.contains("\\t"))
        }
    }

    @Test
    fun `processedTextToChunks handles CJK sentence boundaries`() {
        val text = "第一句話。第二句話。第三句話。"
        val chunks = processedTextToChunks(text)
        assertTrue(chunks.isNotEmpty())
    }
}
