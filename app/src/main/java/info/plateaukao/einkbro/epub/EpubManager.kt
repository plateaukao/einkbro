package info.plateaukao.einkbro.epub

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit.getResourceFromUrl
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.epub.EpubWriter
import nl.siegmann.epublib.service.MediatypeService
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.io.InputStream


class EpubManager(private val context: Context) : KoinComponent {
    private val dialogManager: DialogManager by lazy { DialogManager(context as Activity) }
    private val config: ConfigManager by inject()

    fun saveEpub(
        activity: ComponentActivity,
        fileUri: Uri,
        ninjaWebView: NinjaWebView,
    ) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val isNewFile =
                (DocumentFile.fromSingleUri(activity, fileUri)?.length() ?: 0).toInt() == 0

            val bookName = if (isNewFile) getBookName() else ""
            val chapterName = getChapterName(ninjaWebView.title)

            if (bookName != null && chapterName != null) {
                val progressDialog = ProgressDialog(activity, R.style.TouchAreaDialog).apply {
                    setTitle(R.string.saving_epub)
                    show()
                }

                val rawHtml = ninjaWebView.getRawHtml()
                internalSaveEpub(
                    isNewFile,
                    fileUri,
                    rawHtml,
                    bookName,
                    chapterName,
                    ninjaWebView.url ?: "",
                    { savedBookName ->
                        progressDialog.dismiss()
                        HelperUnit.openEpubToLastChapter(activity, fileUri)

                        // save epub file info to preference
                        val bookUri = fileUri.toString()
                        if (config.savedEpubFileInfos.none { it.uri == bookUri }) {
                            config.addSavedEpubFile(EpubFileInfo(savedBookName, bookUri))
                        }
                    },
                    { progressDialog.dismiss() }
                )
            }
        }
    }

    suspend fun getChapterName(defaultTitle: String?): String? {
        var chapterName = defaultTitle ?: "no title"
        return TextInputDialog(
            context,
            context.getString(R.string.title),
            context.getString(R.string.title_in_toc),
            chapterName
        ).show()
    }

    suspend fun getBookName(): String? {
        return TextInputDialog(
            context,
            context.getString(R.string.book_name),
            context.getString(R.string.book_name_description),
            "einkbro book"
        ).show()
    }

    fun showEpubFilePicker(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_EPUB
        intent.putExtra(Intent.EXTRA_TITLE, "einkbro.epub")
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activityResultLauncher.launch(intent)
    }

    private suspend fun internalSaveEpub(
        isNew: Boolean,
        fileUri: Uri,
        html: String,
        bookName: String,
        chapterName: String,
        currentUrl: String,
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

                book.addSection(
                    chapterName,
                    Resource(processedHtml.byteInputStream(), chapterFileName)
                )

                saveImageResources(book, imageMap)

                saveBook(book, fileUri)

                doneAction.invoke(book.title)

                hasSavedSuccess = true
            }
        }

        if (!hasSavedSuccess) {
            errorAction()
            dialogManager.showOkCancelDialog(
                messageResId = R.string.cannot_open_file,
                okAction = {},
                showInCenter = true,
                showNegativeButton = false,
            )
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
        doc.head().allElements.select("link").remove()
        doc.head().allElements.select("meta").remove()
        val scripts = doc.head().allElements.select("script")
        scripts.forEach {
            if (it.attr("type").equals("text/javascript")) {
                it.remove()
            }
        }

        val imageKeyUrlMap = mutableMapOf<String, String>()
        doc.select("img").forEachIndexed { index, element ->
            val imgUrl = element.attributes()["src"] ?: element.dataset()["src"] ?: ""
            val newImageIndex = "img_${chapterIndex}_$index"
            element.attr("src", newImageIndex)
            imageKeyUrlMap[newImageIndex] = imgUrl
        }

        return Pair(doc.toString(), imageKeyUrlMap)
    }

    private suspend fun saveImageResources(book: Book, map: Map<String, String>) {
        map.entries.forEach { entry ->
            book.addResource(
                Resource(
                    null,
                    getResourceFromUrl(entry.value),
                    entry.key,
                    MediatypeService.JPG
                )
            )
        }
    }
}