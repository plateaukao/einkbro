package info.plateaukao.einkbro.unit

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.io.File

object PdfMergeUtil {
    /**
     * Writes [sourcePdf] (a WebView-rendered temp PDF) to [destUri], adding a TOC
     * (outline) entry titled [tocTitle] that points at its first page.
     */
    fun savePdfWithToc(
        context: Context,
        sourcePdf: File,
        destUri: Uri,
        tocTitle: String?,
    ): Boolean =
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            // disk-backed scratch: keeps peak RAM low on e-ink devices
            PDDocument.load(sourcePdf, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                if (!tocTitle.isNullOrBlank()) {
                    addTocEntry(doc, tocTitle, 0)
                }
                openTruncatedOutputStream(context, destUri)?.use { out ->
                    doc.save(out)
                } ?: return false
            }
            true
        }.getOrDefault(false)

    /**
     * Appends the pages of [newPagesPdf] (a WebView-rendered temp PDF) to the PDF at
     * [existingUri], adding a TOC (outline) entry titled [tocTitle] that points at the
     * first appended page. The merged document is written to a cache temp file first,
     * then copied over the original, so a merge failure never corrupts the user's file.
     */
    fun appendPdfToExisting(
        context: Context,
        existingUri: Uri,
        newPagesPdf: File,
        tocTitle: String?,
    ): Boolean =
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            val mergedTemp = File.createTempFile("merged", ".pdf", context.cacheDir)
            try {
                val memory = MemoryUsageSetting.setupTempFileOnly()
                val merged = context.contentResolver.openInputStream(existingUri)?.use { input ->
                    PDDocument.load(input, memory).use { dest ->
                        PDDocument.load(newPagesPdf, memory).use { src ->
                            val firstNewPageIndex = dest.numberOfPages
                            PDFMergerUtility().appendDocument(dest, src)
                            if (!tocTitle.isNullOrBlank()) {
                                addTocEntry(dest, tocTitle, firstNewPageIndex)
                            }
                            dest.save(mergedTemp)
                        }
                    }
                    true
                } ?: false
                if (!merged) return false

                openTruncatedOutputStream(context, existingUri)?.use { out ->
                    mergedTemp.inputStream().use { it.copyTo(out) }
                } ?: return false
                true
            } finally {
                mergedTemp.delete()
            }
        }.getOrDefault(false)

    private fun addTocEntry(doc: PDDocument, title: String, pageIndex: Int) {
        val catalog = doc.documentCatalog
        val outline = catalog.documentOutline
            ?: PDDocumentOutline().also { catalog.documentOutline = it }
        outline.addLast(PDOutlineItem().apply {
            this.title = title
            destination = PDPageFitWidthDestination().apply {
                page = doc.getPage(pageIndex)
            }
        })
    }

    // "wt" truncates existing content; some SAF providers only support "w", so fall back.
    private fun openTruncatedOutputStream(context: Context, uri: Uri) =
        runCatching { context.contentResolver.openOutputStream(uri, "wt") }
            .getOrNull() ?: context.contentResolver.openOutputStream(uri)
}
