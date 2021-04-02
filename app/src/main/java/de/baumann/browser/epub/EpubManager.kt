package de.baumann.browser.epub

import android.content.Context
import android.net.Uri
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.epub.EpubWriter
import java.io.IOException
import java.io.InputStream


class EpubManager(private val context: Context) {

    fun createBook(domain: String, bookName: String): Book = Book().apply {
        metadata.addTitle(bookName)
        metadata.addAuthor(Author(domain, "EinkBro App"))
    }

    fun openBook(uri: Uri): Book {
        try {
            val epubInputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: return createBook("", "EinkBro")

            return EpubReader().readEpub(epubInputStream)
        } catch(e: IOException) {
            return createBook("", "EinkBro")
        }
    }

    fun saveBook(book: Book, uri: Uri) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            EpubWriter().write(book, outputStream)
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}