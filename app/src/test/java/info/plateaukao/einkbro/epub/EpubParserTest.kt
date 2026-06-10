package info.plateaukao.einkbro.epub

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class EpubParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk()

    private val coverBytes = byteArrayOf(0x11, 0x22, 0x33)
    private val chapter1Xhtml =
        "<html><body><h1>Chapter 1</h1><p>First chapter text.</p></body></html>"
    private val chapter2Xhtml =
        "<html><body><h1>Chapter 2</h1><p>Second chapter text.</p></body></html>"
    private val chapter2bXhtml =
        "<html><body><p>Continuation of chapter two.</p></body></html>"

    @Before
    fun setUp() {
        every { context.cacheDir } returns tempFolder.root
    }

    private fun opfXml(
        metadataExtra: String = "",
        spineAttrs: String = "",
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>My Test Book</dc:title>
            <dc:creator>Test Author</dc:creator>
            <dc:description>A test description.</dc:description>
            <meta name="cover" content="cover-image"/>
            $metadataExtra
          </metadata>
          <manifest>
            <item id="cover-image" href="text/../images/cover.jpg" media-type="image/jpeg"/>
            <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
            <item id="ch1" href="text/chapter1.xhtml" media-type="application/xhtml+xml"/>
            <item id="ch2" href="text/chapter2.xhtml" media-type="application/xhtml+xml"/>
            <item id="ch2b" href="text/chapter2b.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine $spineAttrs>
            <itemref idref="ch1"/>
            <itemref idref="ch2"/>
            <itemref idref="ch2b"/>
          </spine>
        </package>
    """.trimIndent()

    private val ncxXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
          <navMap>
            <navPoint id="n1"><navLabel><text>Chapter 1</text></navLabel><content src="text/chapter1.xhtml"/></navPoint>
            <navPoint id="n2"><navLabel><text>Chapter 2</text></navLabel><content src="text/chapter2.xhtml"/></navPoint>
          </navMap>
        </ncx>
    """.trimIndent()

    private fun defaultEpub(
        metadataExtra: String = "",
        spineAttrs: String = "",
        extraEntries: List<Pair<String, ByteArray>> = emptyList(),
    ): ByteArray = zipBytes(
        "META-INF/container.xml" to containerXml("package.opf").toByteArray(),
        "package.opf" to opfXml(metadataExtra, spineAttrs).toByteArray(),
        "toc.ncx" to ncxXml.toByteArray(),
        "text/chapter1.xhtml" to chapter1Xhtml.toByteArray(),
        "text/chapter2.xhtml" to chapter2Xhtml.toByteArray(),
        "text/chapter2b.xhtml" to chapter2bXhtml.toByteArray(),
        "images/cover.jpg" to coverBytes,
        *extraEntries.toTypedArray(),
    )

    @Test
    fun `parses metadata title author and description`() = runTest {
        val book = epubParser(context, ByteArrayInputStream(defaultEpub()))

        assertEquals("My Test Book", book.title)
        assertEquals("My Test Book", book.fileName)
        assertEquals("Test Author", book.author)
        assertEquals("A test description.", book.description)
        assertFalse(book.isCreatedByEinkBro)
    }

    @Test
    fun `builds chapters from spine grouped by ncx toc entries`() = runTest {
        val book = epubParser(context, ByteArrayInputStream(defaultEpub()))

        assertEquals(2, book.chapters.size)

        val first = book.chapters[0]
        assertEquals("Chapter 1", first.title)
        assertEquals("text/chapter1.xhtml", first.absPath)
        assertEquals(1, first.parts.size)
        assertEquals("text/chapter1.xhtml", first.parts[0].absPath)
        assertEquals(chapter1Xhtml, first.parts[0].body)

        // chapter2b has no toc entry, so it is appended to chapter 2 as a part
        val second = book.chapters[1]
        assertEquals("Chapter 2", second.title)
        assertEquals(2, second.parts.size)
        assertEquals(chapter2Xhtml, second.parts[0].body)
        assertEquals(chapter2bXhtml, second.parts[1].body)
    }

    @Test
    fun `resolves cover image manifest href with dot dot segments`() = runTest {
        // href is "text/../images/cover.jpg"; it must collapse to images/cover.jpg
        val book = epubParser(context, ByteArrayInputStream(defaultEpub()))

        assertNotNull(book.coverImage)
        assertEquals("images/cover.jpg", book.coverImage!!.absPath)
        assertArrayEquals(coverBytes, book.coverImage!!.image)
    }

    @Test
    fun `collects manifest and unlisted images`() = runTest {
        val photoBytes = byteArrayOf(0x42)
        val epub = defaultEpub(extraEntries = listOf("extra/photo.png" to photoBytes))

        val book = epubParser(context, ByteArrayInputStream(epub))

        assertEquals(2, book.images.size)
        val cover = book.images.first { it.absPath == "images/cover.jpg" }
        assertEquals("image/jpeg", cover.mediaType)
        val photo = book.images.first { it.absPath == "extra/photo.png" }
        assertEquals("image/png", photo.mediaType)
        assertArrayEquals(photoBytes, photo.image)
    }

    @Test
    fun `parses rtl page progression direction`() = runTest {
        val epub = defaultEpub(spineAttrs = """page-progression-direction="rtl"""")

        val book = epubParser(context, ByteArrayInputStream(epub))

        assertEquals(PageProgressDirection.RTL, book.pageProgressDirection)
    }

    @Test
    fun `defaults to ltr page progression direction`() = runTest {
        val book = epubParser(context, ByteArrayInputStream(defaultEpub()))

        assertEquals(PageProgressDirection.LTR, book.pageProgressDirection)
    }

    @Test
    fun `detects einkbro identifier in metadata`() = runTest {
        val epub = defaultEpub(
            metadataExtra = "<dc:identifier>EinkBro</dc:identifier>"
        )

        val book = epubParser(context, ByteArrayInputStream(epub))

        assertTrue(book.isCreatedByEinkBro)
    }

    @Test
    fun `throws when container xml is missing`() {
        val epub = zipBytes("mimetype" to "application/epub+zip".toByteArray())

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                epubParser(context, ByteArrayInputStream(epub))
            }
        }
        assertEquals("META-INF/container.xml file missing", exception.message)
    }
}
