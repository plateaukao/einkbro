package info.plateaukao.einkbro.unit

import android.content.Context
import android.net.Uri
import info.plateaukao.einkbro.unit.pdf.PdfTocEditor
import java.io.File

/**
 * Save-as-PDF post-processing (TOC entries, page appending) on top of the
 * in-repo COS-level engine (unit/pdf/), which writes incremental updates —
 * everything it doesn't understand in an existing PDF is preserved verbatim.
 * Replaced pdfbox-android, which cost about 370 KB of dex for these two
 * operations. Encrypted PDFs fail gracefully (return false), as before.
 */
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
            openTruncatedOutputStream(context, destUri)?.use { out ->
                PdfTocEditor.writeWithToc(sourcePdf, out, tocTitle)
            } ?: false
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
            // the parser needs a seekable file, so snapshot the SAF content first
            val existingTemp = File.createTempFile("existing", ".pdf", context.cacheDir)
            val mergedTemp = File.createTempFile("merged", ".pdf", context.cacheDir)
            try {
                context.contentResolver.openInputStream(existingUri)?.use { input ->
                    existingTemp.outputStream().use { input.copyTo(it) }
                } ?: return false

                val merged = mergedTemp.outputStream().use { out ->
                    PdfTocEditor.appendWithToc(existingTemp, newPagesPdf, out, tocTitle)
                }
                if (!merged) return false

                openTruncatedOutputStream(context, existingUri)?.use { out ->
                    mergedTemp.inputStream().use { it.copyTo(out) }
                } ?: return false
                true
            } finally {
                existingTemp.delete()
                mergedTemp.delete()
            }
        }.getOrDefault(false)

    // "wt" truncates existing content; some SAF providers only support "w", so fall back.
    private fun openTruncatedOutputStream(context: Context, uri: Uri) =
        runCatching { context.contentResolver.openOutputStream(uri, "wt") }
            .getOrNull() ?: context.contentResolver.openOutputStream(uri)
}
