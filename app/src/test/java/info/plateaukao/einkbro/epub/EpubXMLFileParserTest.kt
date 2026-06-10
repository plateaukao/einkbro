package info.plateaukao.einkbro.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpubXMLFileParserTest {

    @Test
    fun `parseAsDocument extracts first heading as title`() {
        val xhtml = "<html><body><h1>The Title</h1><p>Some text.</p></body></html>"
        val parser = EpubXMLFileParser("OEBPS/ch1.xhtml", xhtml.toByteArray(), emptyMap())

        val output = parser.parseAsDocument()

        assertEquals("The Title", output.title)
        // current behavior: body is the raw file content, untouched
        assertEquals(xhtml, output.body)
    }

    @Test
    fun `parseAsDocument picks any heading level as title`() {
        val xhtml = "<html><body><h3>Sub Heading</h3><p>Text.</p></body></html>"
        val parser = EpubXMLFileParser("ch.xhtml", xhtml.toByteArray(), emptyMap())

        assertEquals("Sub Heading", parser.parseAsDocument().title)
    }

    @Test
    fun `parseAsDocument returns null title when no heading exists`() {
        val xhtml = "<html><body><p>Just a paragraph.</p></body></html>"
        val parser = EpubXMLFileParser("ch.xhtml", xhtml.toByteArray(), emptyMap())

        val output = parser.parseAsDocument()

        assertNull(output.title)
        assertEquals(xhtml, output.body)
    }

    @Test
    fun `parseAsImage falls back to default aspect ratio when image is missing`() {
        val parser = EpubXMLFileParser("ch.xhtml", ByteArray(0), emptyMap())

        val result = parser.parseAsImage("images/pic.jpg")

        assertEquals("\n\n<img src=\"images/pic.jpg\" yrel=\"1.45\">\n\n", result)
    }

    @Test
    fun `parseAsImage falls back to default aspect ratio when bitmap cannot be decoded`() {
        // BitmapFactory is an Android framework stub on the JVM; decode fails and
        // the parser must fall back to the default 1.45 aspect ratio.
        val zip = mapOf(
            "images/pic.jpg" to EpubFile("images/pic.jpg", byteArrayOf(1, 2, 3))
        )
        val parser = EpubXMLFileParser("ch.xhtml", ByteArray(0), zip)

        val result = parser.parseAsImage("images/pic.jpg")

        assertEquals("\n\n<img src=\"images/pic.jpg\" yrel=\"1.45\">\n\n", result)
    }
}
