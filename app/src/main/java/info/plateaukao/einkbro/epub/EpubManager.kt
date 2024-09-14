package info.plateaukao.einkbro.epub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.caption.DualCaptionProcessor
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit.getResourceAndMimetypeFromUrl
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.epub.EpubWriter
import nl.siegmann.epublib.service.MediatypeService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors


class EpubManager(private val context: Context) : KoinComponent {
    private val config: ConfigManager by inject()

    fun saveEpub(
        activity: ComponentActivity,
        fileUri: Uri,
        ninjaWebView: NinjaWebView,
        onProgressChanged: (Int) -> Unit,
        onErrorAction: () -> Unit,
    ) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val isNewFile =
                (DocumentFile.fromSingleUri(activity, fileUri)?.length() ?: 0).toInt() == 0

            val bookName = if (isNewFile) getBookName() else ""
            val chapterName = getChapterName(ninjaWebView.title)

            if (bookName != null && chapterName != null) {
                //val rawHtml = ninjaWebView.getRawReaderHtml()
                val rawHtml = ninjaWebView.dualCaption?.let {
                    DualCaptionProcessor().convertToHtml(it)
                } ?: ninjaWebView.getRawReaderHtml()
                onProgressChanged(5)

                internalSaveEpub(
                    isNewFile,
                    fileUri,
                    rawHtml,
                    bookName,
                    chapterName,
                    ninjaWebView.url.orEmpty(),
                    onProgressChanged,
                    { savedBookName ->
                        HelperUnit.openEpubToLastChapter(activity, fileUri)

                        // save epub file info to preference
                        val bookUri = fileUri.toString()
                        if (config.savedEpubFileInfos.none { it.uri == bookUri }) {
                            config.addSavedEpubFile(EpubFileInfo(savedBookName, bookUri))
                        }
                    },
                    onErrorAction,
                )
            }
        }
    }

    private suspend fun getChapterName(defaultTitle: String?): String? {
        val chapterName = defaultTitle ?: "no title"
        return TextInputDialog(
            context,
            context.getString(R.string.title),
            context.getString(R.string.title_in_toc),
            chapterName
        ).show()
    }

    private suspend fun getBookName(): String? {
        return TextInputDialog(
            context,
            context.getString(R.string.book_name),
            context.getString(R.string.book_name_description),
            "einkbro book"
        ).show()
    }

    fun showWriteEpubFilePicker(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_EPUB
        intent.putExtra(Intent.EXTRA_TITLE, "einkbro.epub")
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activityResultLauncher.launch(intent)
    }

    fun showOpenEpubFilePicker(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Constants.MIME_TYPE_EPUB
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activityResultLauncher.launch(intent)
    }

    private suspend fun internalSaveEpub(
        isNew: Boolean,
        fileUri: Uri,
        html: String,
        bookName: String,
        chapterName: String,
        currentUrl: String,
        onProgressChanged: (Int) -> Unit, // 0..100
        doneAction: (String) -> Unit,
        errorAction: () -> Unit
    ) {
        val webUri = Uri.parse(currentUrl)
        val domain = webUri.host ?: "EinkBro"
        var hasSavedSuccess = false

        withContext(Dispatchers.IO) {
            val book = if (isNew) createBook(domain, bookName) else openBook(fileUri)
            if (book != null) {

                val chapterIndex = book.tableOfContents.allUniqueResources.size + 1
                val chapterFileName = "chapter$chapterIndex.html"

                val (processedHtml, imageMap) = processHtmlString(
                    html,
                    chapterIndex,
                    "${webUri.scheme}://${webUri.host}/"
                )
                Log.i(TAG, "Adding chapter $chapterIndex: $chapterName")
                book.addSection(
                    chapterName,
                    Resource(processedHtml.byteInputStream(), chapterFileName)
                )

                onProgressChanged(10)

                Log.i(TAG, "Downloading " + imageMap.size + " images")
                saveImageResources(
                    book,
                    imageMap
                ) {
                    onProgressChanged(10 + (it * 80).toInt())
                }

                onProgressChanged(90)
                Log.i(TAG, "Saving epub")
                saveBook(book, fileUri)

                doneAction.invoke(book.title)

                hasSavedSuccess = true
                onProgressChanged(100)
            }
        }

        if (!hasSavedSuccess) {
            errorAction()
        }
    }

    fun showEpubReader(uri: Uri) {
        val intent = Intent(context, EpubReaderActivity::class.java).apply {
            data = uri
        }
        context.startActivity(intent)
    }

    private fun createBook(domain: String, bookName: String): Book = Book().apply {
        metadata.addTitle(bookName)
        metadata.addAuthor(Author(domain, "EinkBro App"))
    }

    private fun openBook(uri: Uri): Book? {
        try {
            val takeFlags: Int =
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            val epubInputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return createBook("", "EinkBro")

            return EpubReader().readEpub(epubInputStream)
        } catch (e: IOException) {
            return createBook("", "EinkBro")
        } catch (e: SecurityException) {
            return null
        }
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

    private fun processHtmlString(
        html: String,
        chapterIndex: Int,
        baseUri: String
    ): Pair<String, Map<String, String>> {
        val doc = Jsoup.parse(html, baseUri)
        with(doc.head().allElements) {
            select("link").remove()
            select("meta").remove()
            select("script").forEach { it.remove() }
            select("style").forEach { it.remove() }
        }

        doc.select("html").attr("xmlns", "http://www.w3.org/1999/xhtml")
        // for medium
//        if (baseUri.contains("medium")) {
//            doc.select("source").forEach { it.remove() }
//        }

        val imageKeyUrlMap = mutableMapOf<String, String>()
        doc.select("img").forEachIndexed { index, element ->
            val imgUrl = element.attributes()["src"] ?: element.dataset()["src"].orEmpty()
            val newImageIndex = "img_${chapterIndex}_$index"
            // Sadly, Reader mode does not remove all 1px tracking pixels, do this manually instead.
            if (element.isDummyImage()) {
                Log.w(TAG, "Skipping 1px image $imgUrl")
                element.clearAttributes()
            } else {
                Log.d(TAG, "Mapped $newImageIndex to $imgUrl")
                element.attr("src", newImageIndex)
                imageKeyUrlMap[newImageIndex] = imgUrl
            }
        }

        // for generating html elements with end tag.
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        return Pair(doc.toString(), imageKeyUrlMap)
    }

    private fun Element.isDummyImage(): Boolean =
        attr("height") == "1" && attr("width") == "1"

    private suspend fun saveImageResources(
        book: Book,
        map: Map<String, String>,
        onProgressChanged: (Float) -> Unit,
    ) {
        val mutex = Mutex()
        var processedImageCount = 0

        coroutineScope {
            map.entries.forEach { entry ->
                launch(saveImageDispatcher) {
                    Log.i(TAG, "Loading ${entry.key}: ${entry.value}")
                    val (resource, mimeType) = getResourceAndMimetypeFromUrl(
                        entry.value,
                        timeout = 5_000
                    )
                    val mediaType =
                        MediatypeService.getMediaTypeByName(mimeType) ?: MediatypeService.JPG
                    Log.d(TAG, "Got content type: $mimeType mediaType: $mediaType")
                    mutex.withLock { // Synchronize access to ebook and counter
                        book.addResource(
                            Resource(
                                null,
                                resource,
                                entry.key,
                                mediaType
                            )
                        )
                        processedImageCount++
                        onProgressChanged(processedImageCount.toFloat() / map.size)
                    }
                }
            }
        }
    }

    private val saveImageDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    companion object {
        private const val TAG = "EpubManager"
    }
}
