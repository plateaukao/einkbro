package info.plateaukao.einkbro.epub

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class EpubCoverParserTest {

    private val coverBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)

    private fun opfXml(coverHref: String, withCoverMeta: Boolean = true): String {
        val coverMeta = if (withCoverMeta) """<meta name="cover" content="cover-image"/>""" else ""
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Cover Test</dc:title>
                $coverMeta
              </metadata>
              <manifest>
                <item id="cover-image" href="$coverHref" media-type="image/jpeg"/>
                <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="ch1"/>
              </spine>
            </package>
        """.trimIndent()
    }

    @Test
    fun `finds cover image referenced by metadata cover meta`() = runTest {
        val epub = zipBytes(
            "META-INF/container.xml" to containerXml("package.opf").toByteArray(),
            "package.opf" to opfXml("images/cover.jpg").toByteArray(),
            "images/cover.jpg" to coverBytes,
        )

        val image = epubCoverParser(ByteArrayInputStream(epub))

        assertNotNull(image)
        assertEquals("images/cover.jpg", image!!.absPath)
        assertEquals("image/jpeg", image.mediaType)
        assertArrayEquals(coverBytes, image.image)
    }

    @Test
    fun `resolves relative href containing dot dot segments`() = runTest {
        // opf at root; href walks into "text" and back out, must collapse to images/cover.jpg
        val epub = zipBytes(
            "META-INF/container.xml" to containerXml("package.opf").toByteArray(),
            "package.opf" to opfXml("text/../images/cover.jpg").toByteArray(),
            "images/cover.jpg" to coverBytes,
        )

        val image = epubCoverParser(ByteArrayInputStream(epub))

        assertNotNull(image)
        assertEquals("images/cover.jpg", image!!.absPath)
        assertArrayEquals(coverBytes, image.image)
    }

    @Test
    fun `resolves dot dot href relative to opf in subdirectory`() = runTest {
        // The parser canonicalizes File(opfDir, href) and converts to invariant
        // ('/') separators. On a desktop JVM relative paths canonicalize against
        // the working directory, so compute the expected zip key the same way.
        val expectedKey = File(File("OEBPS"), "../images/cover.jpg")
            .canonicalFile
            .invariantSeparatorsPath
            .removePrefix("/")
        // Lock in the normalization invariants of the resolved path.
        assertTrue(expectedKey.endsWith("images/cover.jpg"))
        assertFalse(expectedKey.contains(".."))
        assertFalse(expectedKey.contains("\\"))
        assertFalse(expectedKey.startsWith("/"))

        val epub = zipBytes(
            "META-INF/container.xml" to containerXml("OEBPS/package.opf").toByteArray(),
            "OEBPS/package.opf" to opfXml("../images/cover.jpg").toByteArray(),
            expectedKey to coverBytes,
        )

        val image = epubCoverParser(ByteArrayInputStream(epub))

        assertNotNull(image)
        assertArrayEquals(coverBytes, image!!.image)
    }

    @Test
    fun `decodes url encoded href before resolving`() = runTest {
        val epub = zipBytes(
            "META-INF/container.xml" to containerXml("package.opf").toByteArray(),
            "package.opf" to opfXml("images/cover%20art.jpg").toByteArray(),
            "images/cover art.jpg" to coverBytes,
        )

        val image = epubCoverParser(ByteArrayInputStream(epub))

        assertNotNull(image)
        assertEquals("images/cover art.jpg", image!!.absPath)
    }

    @Test
    fun `returns null when metadata has no cover meta`() = runTest {
        val epub = zipBytes(
            "META-INF/container.xml" to containerXml("package.opf").toByteArray(),
            "package.opf" to opfXml("images/cover.jpg", withCoverMeta = false).toByteArray(),
            "images/cover.jpg" to coverBytes,
        )

        assertNull(epubCoverParser(ByteArrayInputStream(epub)))
    }

    @Test
    fun `throws when container xml is missing`() {
        val epub = zipBytes("mimetype" to "application/epub+zip".toByteArray())

        val exception = assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                epubCoverParser(ByteArrayInputStream(epub))
            }
        }
        assertEquals("META-INF/container.xml file missing", exception.message)
    }
}
