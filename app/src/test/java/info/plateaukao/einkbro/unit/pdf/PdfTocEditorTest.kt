package info.plateaukao.einkbro.unit.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Validates the in-repo COS-level PDF writer against desktop Apache pdfbox:
 * whatever this engine writes must load cleanly in an independent parser with
 * the expected pages, outline entries, and text.
 */
class PdfTocEditorTest {

    private fun makePdf(pageTexts: List<String>): File {
        val file = File.createTempFile("src", ".pdf").apply { deleteOnExit() }
        PDDocument().use { doc ->
            for (text in pageTexts) {
                val page = PDPage()
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA, 12f)
                    cs.newLineAtOffset(72f, 700f)
                    cs.showText(text)
                    cs.endText()
                }
            }
            doc.save(file)
        }
        return file
    }

    private fun textOfPage(doc: PDDocument, pageIndex: Int): String =
        PDFTextStripper().apply {
            startPage = pageIndex + 1
            endPage = pageIndex + 1
        }.getText(doc).trim()

    private fun outlineTitles(doc: PDDocument): List<String> {
        val outline = doc.documentCatalog.documentOutline ?: return emptyList()
        return outline.children().map { it.title }
    }

    @Test
    fun writeWithToc_addsOutlineEntryForFirstPage() {
        val source = makePdf(listOf("page one", "page two"))
        val out = File.createTempFile("out", ".pdf").apply { deleteOnExit() }
        val ok = out.outputStream().use { PdfTocEditor.writeWithToc(source, it, "My Article") }
        assertTrue(ok)

        PDDocument.load(out).use { doc ->
            assertEquals(2, doc.numberOfPages)
            assertEquals("page one", textOfPage(doc, 0))
            assertEquals(listOf("My Article"), outlineTitles(doc))
            val item = doc.documentCatalog.documentOutline!!.children().first()
            val dest = item.destination as PDPageDestination
            assertEquals(0, doc.pages.indexOf(dest.page))
        }
    }

    @Test
    fun writeWithToc_blankTitle_isPlainCopy() {
        val source = makePdf(listOf("only page"))
        val out = File.createTempFile("out", ".pdf").apply { deleteOnExit() }
        val ok = out.outputStream().use { PdfTocEditor.writeWithToc(source, it, null) }
        assertTrue(ok)
        assertEquals(source.length(), out.length())
        PDDocument.load(out).use { doc -> assertEquals(1, doc.numberOfPages) }
    }

    @Test
    fun append_addsPagesAndOutlineEntry() {
        val existing = makePdf(listOf("first doc"))
        val extra = makePdf(listOf("second doc page A", "second doc page B"))
        val out = File.createTempFile("out", ".pdf").apply { deleteOnExit() }
        val ok = out.outputStream().use {
            PdfTocEditor.appendWithToc(existing, extra, it, "Second Article")
        }
        assertTrue(ok)

        PDDocument.load(out).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertEquals("first doc", textOfPage(doc, 0))
            assertEquals("second doc page A", textOfPage(doc, 1))
            assertEquals("second doc page B", textOfPage(doc, 2))
            assertEquals(listOf("Second Article"), outlineTitles(doc))
            val item = doc.documentCatalog.documentOutline!!.children().first()
            val dest = item.destination as PDPageDestination
            assertEquals(1, doc.pages.indexOf(dest.page))
        }
    }

    @Test
    fun append_twice_parsesOwnUpdateAndGrowsOutline() {
        val base = makePdf(listOf("base"))
        val withToc = File.createTempFile("toc", ".pdf").apply { deleteOnExit() }
        withToc.outputStream().use { PdfTocEditor.writeWithToc(base, it, "Chapter 1") }

        val extra1 = makePdf(listOf("chapter two"))
        val merged1 = File.createTempFile("merged1", ".pdf").apply { deleteOnExit() }
        assertTrue(merged1.outputStream().use {
            PdfTocEditor.appendWithToc(withToc, extra1, it, "Chapter 2")
        })

        // second round parses the first round's incremental update
        val extra2 = makePdf(listOf("chapter three"))
        val merged2 = File.createTempFile("merged2", ".pdf").apply { deleteOnExit() }
        assertTrue(merged2.outputStream().use {
            PdfTocEditor.appendWithToc(merged1, extra2, it, "Chapter 3")
        })

        PDDocument.load(merged2).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertEquals(listOf("Chapter 1", "Chapter 2", "Chapter 3"), outlineTitles(doc))
            assertEquals("base", textOfPage(doc, 0))
            assertEquals("chapter two", textOfPage(doc, 1))
            assertEquals("chapter three", textOfPage(doc, 2))
            val items = doc.documentCatalog.documentOutline!!.children().toList()
            assertEquals(0, doc.pages.indexOf((items[0].destination as PDPageDestination).page))
            assertEquals(1, doc.pages.indexOf((items[1].destination as PDPageDestination).page))
            assertEquals(2, doc.pages.indexOf((items[2].destination as PDPageDestination).page))
        }
    }

    @Test
    fun unicodeTitle_roundTrips() {
        val source = makePdf(listOf("cjk title test"))
        val out = File.createTempFile("out", ".pdf").apply { deleteOnExit() }
        assertTrue(out.outputStream().use { PdfTocEditor.writeWithToc(source, it, "中文標題") })
        PDDocument.load(out).use { doc ->
            assertEquals(listOf("中文標題"), outlineTitles(doc))
        }
    }

    @Test
    fun append_ontoXrefStreamDocumentWithObjectStream() {
        val existing = buildXrefStreamPdf()
        // sanity: pdfbox can read the hand-built fixture
        PDDocument.load(existing).use { doc ->
            assertEquals(1, doc.numberOfPages)
            assertEquals("XrefStreamPage", textOfPage(doc, 0))
        }

        val extra = makePdf(listOf("appended page"))
        val out = File.createTempFile("out", ".pdf").apply { deleteOnExit() }
        assertTrue(out.outputStream().use {
            PdfTocEditor.appendWithToc(existing, extra, it, "Appended")
        })

        PDDocument.load(out).use { doc ->
            assertEquals(2, doc.numberOfPages)
            assertEquals("XrefStreamPage", textOfPage(doc, 0))
            assertEquals("appended page", textOfPage(doc, 1))
            assertEquals(listOf("Appended"), outlineTitles(doc))
            assertNotNull(doc.documentCatalog.documentOutline)
        }
    }

    /**
     * Minimal PDF 1.5 file using an xref *stream* whose catalog and page-tree
     * root live inside an object *stream* — the constructs a modern non-Skia
     * producer would use, which the parser must handle for arbitrary existing
     * documents picked by the user.
     */
    private fun buildXrefStreamPdf(): File {
        val out = ByteArrayOutputStream()
        fun pos() = out.size().toLong()
        fun ascii(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))

        ascii("%PDF-1.5\n")
        val off3 = pos()
        val content = "BT /F1 12 Tf 72 700 Td (XrefStreamPage) Tj ET"
        ascii(
            "3 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]" +
                "/Resources<</Font<</F1<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>>>>>" +
                "/Contents 4 0 R>>\nendobj\n"
        )
        val off4 = pos()
        ascii("4 0 obj\n<</Length ${content.length}>>\nstream\n$content\nendstream\nendobj\n")

        val body1 = "<</Type/Catalog/Pages 2 0 R>>"
        val body2 = "<</Type/Pages/Kids[3 0 R]/Count 1>>"
        val header = "1 0 2 ${body1.length + 1}\n"
        val stmData = header + body1 + " " + body2
        val off5 = pos()
        ascii("5 0 obj\n<</Type/ObjStm/N 2/First ${header.length}/Length ${stmData.length}>>\nstream\n$stmData\nendstream\nendobj\n")

        val off6 = pos()
        val entries = ByteArrayOutputStream()
        fun entry(type: Int, f2: Long, f3: Int) {
            entries.write(type)
            entries.write(((f2 shr 24) and 0xff).toInt())
            entries.write(((f2 shr 16) and 0xff).toInt())
            entries.write(((f2 shr 8) and 0xff).toInt())
            entries.write((f2 and 0xff).toInt())
            entries.write((f3 shr 8) and 0xff)
            entries.write(f3 and 0xff)
        }
        entry(0, 0, 0xffff) // 0: free list head
        entry(2, 5, 0) // 1: catalog, in object stream 5 at index 0
        entry(2, 5, 1) // 2: pages root, in object stream 5 at index 1
        entry(1, off3, 0)
        entry(1, off4, 0)
        entry(1, off5, 0)
        entry(1, off6, 0) // 6: the xref stream itself
        val entryBytes = entries.toByteArray()
        ascii("6 0 obj\n<</Type/XRef/Size 7/W[1 4 2]/Root 1 0 R/Length ${entryBytes.size}>>\nstream\n")
        out.write(entryBytes)
        ascii("\nendstream\nendobj\n")
        ascii("startxref\n$off6\n%%EOF\n")

        val file = File.createTempFile("xrefstream", ".pdf").apply { deleteOnExit() }
        file.writeBytes(out.toByteArray())
        return file
    }
}
