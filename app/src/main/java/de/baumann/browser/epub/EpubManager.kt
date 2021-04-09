package de.baumann.browser.epub

import android.content.Context
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.epub.EpubWriter
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class EpubManager(private val context: Context) {

    private fun createBook(domain: String, bookName: String): Book = Book().apply {
        metadata.addTitle(bookName)
        metadata.addAuthor(Author(domain, "EinkBro App"))
    }

    private fun openBook(uri: Uri): Book {
        try {
            val epubInputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: return createBook("", "EinkBro")

            return EpubReader().readEpub(epubInputStream)
        } catch(e: IOException) {
            return createBook("", "EinkBro")
        }
    }

    suspend fun saveEpub(
            isNew: Boolean,
            fileUri: Uri,
            html: String,
            bookName: String,
            chapterName: String,
            currentUrl: String,
            doneAction: () -> Unit
    ) {
        val webUri = Uri.parse(currentUrl)
        val domain = webUri.host ?: "EinkBro"

        val book = if (isNew) createBook(domain, bookName) else openBook(fileUri)

        val chapterIndex = book.tableOfContents.allUniqueResources.size + 1
        val chapterFileName = "chapter$chapterIndex.html"

        val (processedHtml, imageMap) = processHtmlString(html, chapterIndex, "${webUri.scheme}://${webUri.host}/")

        book.addSection(chapterName, Resource(processedHtml.byteInputStream(), chapterFileName))

        //if(isNew) {
            //saveImageResources(book, imageMap)
        //}

        saveBook(book, fileUri)
        doneAction.invoke()
    }

    private fun saveBook(book: Book, uri: Uri) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            EpubWriter().write(book, outputStream)
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun processHtmlString(html: String, chapterIndex: Int, baseUri: String): Pair<String, Map<String, String>> {
        val doc = Jsoup.parse(html, baseUri)
        doc.head().allElements.select("link").remove()
        doc.head().allElements.select("meta").remove()
        doc.head().allElements.select("script").remove()

        val imageKeyUrlMap = mutableMapOf<String, String>()
        doc.select("img").forEachIndexed { index, element ->
            val imgUrl = element.dataset()["src"] as String? ?: ""
            val newImageIndex = "img_${chapterIndex}_$index"
            element.attr("src", newImageIndex)
            imageKeyUrlMap[newImageIndex] = imgUrl
        }

        return Pair(doc.toString(), imageKeyUrlMap)
    }

    private suspend fun saveImageResources(book: Book, map: Map<String, String>) {
        map.entries.forEach { entry ->
            book.addResource(Resource(getResourceFromUrl(entry.value), entry.key))
        }
    }

    private suspend fun getResourceFromUrl(url: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val bytes = connection.inputStream.readBytes()
                connection.inputStream.close()
                bytes
            } catch (e: IOException) {
                e.printStackTrace()
                "".toByteArray()
            }
        }
    }

}