package info.plateaukao.einkbro.unit

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private fun spanFor(result: AnnotatedString, text: String): SpanStyle =
        result.spanStyles.first { result.text.substring(it.start, it.end) == text }.item

    // --- edge cases ---

    @Test
    fun `empty input produces empty annotated string`() {
        val result = MarkdownParser.parseMarkdown("")
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `whitespace only input is trimmed to empty`() {
        val result = MarkdownParser.parseMarkdown("   ")
        assertEquals("", result.text)
    }

    @Test
    fun `plain text keeps default font size and normal weight`() {
        val result = MarkdownParser.parseMarkdown("Hello world")
        assertEquals("Hello world", result.text)
        val span = result.spanStyles.single().item
        assertEquals(FontWeight.Normal, span.fontWeight)
        assertEquals(18.sp, span.fontSize)
    }

    // --- headings ---

    @Test
    fun `h1 heading is bold with enlarged font`() {
        val result = MarkdownParser.parseMarkdown("# Heading")
        assertEquals("Heading", result.text)
        val span = result.spanStyles.single().item
        assertEquals(FontWeight.Bold, span.fontWeight)
        assertEquals(22.sp, span.fontSize)
    }

    @Test
    fun `heading levels two to six map to decreasing font sizes`() {
        val cases = mapOf(
            "## " to 21,
            "### " to 20,
            "#### " to 20,
            "##### " to 19,
            "###### " to 19,
        )
        cases.forEach { (prefix, size) ->
            val result = MarkdownParser.parseMarkdown("${prefix}Title")
            assertEquals("level '$prefix'", "Title", result.text)
            val span = result.spanStyles.single().item
            assertEquals("level '$prefix'", FontWeight.Bold, span.fontWeight)
            assertEquals("level '$prefix'", size.sp, span.fontSize)
        }
    }

    @Test
    fun `heading content with inline bold is parsed`() {
        val result = MarkdownParser.parseMarkdown("# Hello **World**")
        assertEquals("Hello World", result.text)
        val headingSpan = spanFor(result, "Hello ")
        assertEquals(FontWeight.Bold, headingSpan.fontWeight)
        assertEquals(22.sp, headingSpan.fontSize)
        assertEquals(FontWeight.Bold, spanFor(result, "World").fontWeight)
    }

    // --- lists ---

    @Test
    fun `unordered list item with asterisk gets bullet prefix`() {
        val result = MarkdownParser.parseMarkdown("* item")
        assertEquals("• item", result.text)
        val bullet = spanFor(result, "• ")
        assertEquals(FontWeight.Bold, bullet.fontWeight)
        assertEquals(18.sp, bullet.fontSize)
        assertEquals(14.sp, spanFor(result, "item").fontSize)
    }

    @Test
    fun `unordered list item with dash gets bullet prefix`() {
        val result = MarkdownParser.parseMarkdown("- item")
        assertEquals("• item", result.text)
    }

    @Test
    fun `ordered list item keeps number with bold style`() {
        val result = MarkdownParser.parseMarkdown("1. First")
        assertEquals("1. First", result.text)
        val number = spanFor(result, "1.")
        assertEquals(FontWeight.Bold, number.fontWeight)
        assertEquals(16.sp, spanFor(result, " First").fontSize)
    }

    // --- inline styles ---

    @Test
    fun `bold text is rendered with bold weight and markers removed`() {
        val result = MarkdownParser.parseMarkdown("before **bold** after")
        assertEquals("before bold after", result.text)
        assertEquals(FontWeight.Bold, spanFor(result, "bold").fontWeight)
        assertEquals(FontWeight.Normal, spanFor(result, "before ").fontWeight)
    }

    @Test
    fun `italic with asterisks is rendered with italic style`() {
        val result = MarkdownParser.parseMarkdown("*slanted*")
        assertEquals("slanted", result.text)
        assertEquals(FontStyle.Italic, spanFor(result, "slanted").fontStyle)
    }

    @Test
    fun `italic with underscores is rendered with italic style`() {
        val result = MarkdownParser.parseMarkdown("_slanted_")
        assertEquals("slanted", result.text)
        assertEquals(FontStyle.Italic, spanFor(result, "slanted").fontStyle)
    }

    @Test
    fun `bold italic combines both styles`() {
        val result = MarkdownParser.parseMarkdown("***both***")
        assertEquals("both", result.text)
        val span = spanFor(result, "both")
        assertEquals(FontWeight.Bold, span.fontWeight)
        assertEquals(FontStyle.Italic, span.fontStyle)
    }

    @Test
    fun `strikethrough is rendered with line through decoration`() {
        val result = MarkdownParser.parseMarkdown("~~gone~~")
        assertEquals("gone", result.text)
        assertEquals(TextDecoration.LineThrough, spanFor(result, "gone").textDecoration)
    }

    @Test
    fun `mixed inline styles in one line`() {
        val result = MarkdownParser.parseMarkdown("plain **bold** and *it*")
        assertEquals("plain bold and it", result.text)
        assertEquals(FontWeight.Bold, spanFor(result, "bold").fontWeight)
        assertEquals(FontStyle.Italic, spanFor(result, "it").fontStyle)
    }

    // --- unsupported syntax passes through ---

    @Test
    fun `links and inline code are passed through as plain text`() {
        val input = "[link](http://example.com) and `code`"
        val result = MarkdownParser.parseMarkdown(input)
        assertEquals(input, result.text)
        val span = result.spanStyles.single().item
        assertEquals(FontWeight.Normal, span.fontWeight)
    }

    // --- multi-line behavior ---

    @Test
    fun `lines are joined with newlines and trailing newline trimmed`() {
        val result = MarkdownParser.parseMarkdown("# Title\nBody text")
        assertEquals("Title\nBody text", result.text)
    }

    @Test
    fun `blank lines between paragraphs are preserved`() {
        val result = MarkdownParser.parseMarkdown("a\n\nb")
        assertEquals("a\n\nb", result.text)
    }
}
