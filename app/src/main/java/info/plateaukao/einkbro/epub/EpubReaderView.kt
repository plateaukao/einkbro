package info.plateaukao.einkbro.epub

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Avinash on 25-05-2017.
 */
class EpubReaderView(
    context: Context,
    browserController: BrowserController?,
) : EBWebView(context, browserController) {
    private lateinit var epub: EpubBook
    private val chapterList: ArrayList<Chapter> = ArrayList()
    var chapterNumber = 0
    var chapterPartPosition = 0
    var progress = 0f
    var pageNumber = 0
    private var resourceLocation = ""

    private var loading = false
    lateinit var listener: EpubReaderListener

    var webTheme: WebThemeType = WebThemeType.LIGHT
        set(value) {
            field = value
            if (webTheme == WebThemeType.LIGHT) {
                processJavascript(
                    """var elements = document.getElementsByTagName('*');
for (var i = 0; i < elements.length; i++) {
 if(elements[i].tagName!="SPAN")
  elements[i].style.backgroundColor='white';
 elements[i].style.color='black';
}"""
                )
            } else {
                processJavascript(
                    """var elements = document.getElementsByTagName('*');
for (var i = 0; i < elements.length; i++) {
 if(elements[i].tagName!="SPAN")
  elements[i].style.backgroundColor='black';
elements[i].style.color='white';
}"""
                )
            }
        }

    fun setEpubReaderListener(listener: EpubReaderListener) {
        this.listener = listener
    }

    inner class Chapter(val name: String, val content: String, val href: String)

    init {
        with(settings) {
            allowContentAccess = true
            allowFileAccess = true
        }
        isEpubReaderMode = true
        isVerticalRead = false
        isReaderModeOn = false
    }

    private fun processJavascript(js: String) {
        evaluateJavascript("(function(){$js})()") { }
    }

    suspend fun openEpubFile(uri: Uri) {
        withContext(IO) {
            val epub = context.contentResolver.openInputStream(uri).use { epubParser(context, inputStream = it!!) }
            this@EpubReaderView.epub = epub
            isVerticalRead = epub.pageProgressDirection == PageProgressDirection.RTL

            val epubTempExtractionLocation = context.cacheDir.toString() + "/tempfiles"
            val dirOEBPS = File(epubTempExtractionLocation + File.separator + "OEBPS")
            val relativeFolder =
                epub.chapters.firstOrNull()?.absPath?.substringBeforeLast("/")?.substringAfter(epub.rootPath) ?: ""
            val dirRelativePath = File(epubTempExtractionLocation + relativeFolder + File.separator)
            resourceLocation = if (dirOEBPS.exists() && dirOEBPS.isDirectory) {
                "file://" + epubTempExtractionLocation + File.separator + "OEBPS" + File.separator
            } else if (dirRelativePath.exists() && dirRelativePath.isDirectory && relativeFolder.isNotEmpty()) {
                "file://" + epubTempExtractionLocation + relativeFolder + File.separator
            } else {
                "file://" + epubTempExtractionLocation + File.separator
            }
        }
    }

    fun gotoLastChapter() {
        if (epub.chapters.isEmpty()) return

        gotoChapter(epub.chapters.last())
        chapterNumber = epub.chapters.size - 1
        chapterPartPosition = 0

        scrollX = 0
        scrollY = 0
    }

    fun gotoFirstChapter() {
        if (epub.chapters.isEmpty()) return
        gotoChapter(epub.chapters.first())
        chapterNumber = 0
        chapterPartPosition = 0
    }

    fun gotoChapter(chapter: EpubBook.Chapter, partNumber: Int = 0) {
        chapterPartPosition = partNumber
        loadDataWithBaseURL(
            resourceLocation,
            chapter.parts[partNumber].body,
            "text/html",
            "utf-8",
            null
        )
        postDelayed({
            if (isVerticalRead) {
                // to right most
                scrollTo(computeHorizontalScrollRange(), 0)
            } else {
                scrollTo(0, 0)
            }
        }, 100)
    }

    fun showTocDialog() {
        try {
            val items = epub.chapters.map { it.title }.toTypedArray()
            AlertDialog.Builder(context, R.style.TouchAreaDialog)
                .setTitle(context.getString(R.string.dialog_toc_title))
                .setItems(items) { _, item ->
                    gotoChapter(epub.chapters.first { it.title == items[item] })
                    listener.onChapterChangeListener(item)
                }.create().apply {
                    with(listView) {
                        divider = ColorDrawable(Color.GRAY)
                        dividerHeight = 1
                        setFooterDividersEnabled(false)
                        overscrollFooter = ColorDrawable(Color.TRANSPARENT)
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("EpubReaderView", e.toString())
        }
    }

    override fun pageDownWithNoAnimation() {
        if (loading) return

        if (isVerticalRead) {
            val pageWidth = shiftOffset()
            val totalWidth = computeHorizontalScrollRange()
            when {
                totalWidth - pageWidth > scrollX + pageWidth -> scrollBy(shiftOffset(), 0)
                else -> gotoPreviousPartOrChapter()
            }
            scrollX = min(computeHorizontalScrollRange() - width, scrollX)
        } else {
            val pageHeight = shiftOffset()
            val totalHeight = getTotalContentHeight()
            when {
                totalHeight - height < 10 -> gotoNextPartOrChapter()
                totalHeight > scrollY + height -> scrollBy(0, pageHeight)
                else -> {
                    gotoNextPartOrChapter()
                }
            }
        }
        loading = false
    }

    private fun gotoNextPartOrChapter() {
        if (epub.chapters[chapterNumber].parts.size > chapterPartPosition + 1) {
            chapterPartPosition++
            gotoChapter(epub.chapters[chapterNumber], chapterPartPosition)
        } else {
            if (chapterNumber + 1 < epub.chapters.size) {
                chapterNumber++
                gotoChapter(epub.chapters[chapterNumber])
            } else {
                listener.onBookEndReached()
            }
        }
    }

    private fun gotoPreviousPartOrChapter() {
        if (chapterPartPosition - 1 >= 0) {
            chapterPartPosition--
            gotoChapter(epub.chapters[chapterNumber], chapterPartPosition)
        } else if (chapterNumber - 1 >= 0) {
            chapterNumber--
            chapterPartPosition = epub.chapters[chapterNumber].parts.size - 1
            gotoChapter(epub.chapters[chapterNumber], chapterPartPosition)
        } else {
            listener.onBookStartReached()
        }
    }

    override fun pageUpWithNoAnimation() {
        if (loading) return

        if (isVerticalRead) {
            val pageWidth = shiftOffset()
            when {
                scrollX == width -> gotoNextPartOrChapter()
                scrollX - pageWidth >= 0 -> scrollBy(-shiftOffset(), 0)
                scrollX > 0 -> scrollX = 0
                else -> gotoNextPartOrChapter()
            }
        } else {
            val pageHeight = shiftOffset()
            when {
                scrollY - pageHeight >= 0 -> scrollBy(0, -shiftOffset())
                scrollY > 0 -> scrollY = 0
                else -> gotoPreviousPartOrChapter()
            }
        }
        loading = false
    }


    private fun getTotalContentHeight(): Int =
        (this.contentHeight * resources.displayMetrics.density).toInt()

    fun getPageHeight(): Int = height - 50

    fun getProgressStart(): Float = progress

    fun getProgressEnd(): Float {
        val value: Float = progress + getPageHeight() / getTotalContentHeight()
        return when {
            getTotalContentHeight() <= 0 -> progress
            value < 1 -> value
            else -> 1.0F
        }
    }

    private fun dpToPixel(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics
    ).roundToInt()

    companion object {
        const val METHOD_HIGHLIGHT = 1
        const val METHOD_UNDERLINE = 2
        const val METHOD_STRIKETHROUGH = 3
    }
}

enum class WebThemeType { LIGHT, DARK }