package info.plateaukao.einkbro.unit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBlocksTest {

    @Test
    fun `plain markdown is a single text block`() {
        val md = "# Title\n\nSome **bold** text."
        assertEquals(listOf(MarkdownBlock.Text(md)), MarkdownBlocks.split(md))
        assertFalse(MarkdownBlocks.hasRichBlocks(md))
    }

    @Test
    fun `image becomes an image block, with optional title`() {
        val md = "Look:\n\n![A chart](https://example.com/a.png \"chart\")\n\nand ![](https://example.com/b.jpg) inline."
        assertTrue(MarkdownBlocks.hasRichBlocks(md))
        assertEquals(
            listOf(
                MarkdownBlock.Text("Look:"),
                MarkdownBlock.Image("https://example.com/a.png", "A chart"),
                MarkdownBlock.Text("and"),
                MarkdownBlock.Image("https://example.com/b.jpg", ""),
                MarkdownBlock.Text("inline."),
            ),
            MarkdownBlocks.split(md),
        )
    }

    @Test
    fun `plain links are not images`() {
        val md = "See [the docs](https://example.com/docs)."
        assertEquals(listOf(MarkdownBlock.Text(md)), MarkdownBlocks.split(md))
        assertFalse(MarkdownBlocks.hasRichBlocks(md))
    }

    @Test
    fun `unfinished image mid-stream stays text`() {
        val md = "Here ![partial](https://example.com/a.pn"
        assertEquals(listOf(MarkdownBlock.Text(md)), MarkdownBlocks.split(md))
    }

    @Test
    fun `code fences are left to the text renderer`() {
        val md = "```mermaid\nflowchart LR\n    A --> B\n```"
        assertEquals(listOf(MarkdownBlock.Text(md)), MarkdownBlocks.split(md))
    }
}
