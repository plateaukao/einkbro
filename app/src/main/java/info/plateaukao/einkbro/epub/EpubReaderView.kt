package info.plateaukao.einkbro.epub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Avinash on 25-05-2017.
 */
class EpubReaderView(
    context: Context,
    browserController: BrowserController?
) : EBWebView(context, browserController) {
    private lateinit var book: Book
    private lateinit var epub: EpubBook
    private val chapterList: ArrayList<Chapter> = ArrayList()
    var chapterNumber = 0
    var progress = 0f
    var pageNumber = 0
    private var touchX = 0f
    private var touchY = 0f
    private var touchTime: Long = 0
    private var resourceLocation = ""
    private var actionMode: ActionMode? = null
    private var actionModeCallback: SelectActionModeCallback? = null
    var selectedText = ""
        private set
    var selectedTextInfo: SelectedTextInfo? = null

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

    private var textSelectionMode = false

    fun setEpubReaderListener(listener: EpubReaderListener) {
        this.listener = listener
    }

    inner class Chapter(val name: String, val content: String, val href: String)
    inner class SelectActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            mode.menuInflater.inflate(R.menu.menu_ereader, menu);
            textSelectionMode = true
            listener.onTextSelectionModeChangeListener(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_copy -> copySelection()
                R.id.menu_highlight -> annotateSelection(AnnotateType.HIGHLIGHT)
                R.id.menu_underline -> annotateSelection(AnnotateType.UNDERLINE)
                R.id.menu_strike -> annotateSelection(AnnotateType.STRIKETHROUGH)
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            listener.onTextSelectionModeChangeListener(false)
            textSelectionMode = false
        }
    }

    private fun copySelection() {
        processTextSelection {
            val text = selectedTextInfo?.text ?: return@processTextSelection
            val clip = ClipData.newPlainText("Copied Text", text)
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                clip
            )

            EBToast.showShort(context, "text s copied")
        }
    }

    private fun annotateSelection(annotateType: AnnotateType) {
        processTextSelection {
            val info = selectedTextInfo ?: return@processTextSelection
            if (info.chapterNumber >= 0 && info.chapterNumber == this.chapterNumber && info.dataString.isNotBlank()) {
                // TODO: Save ChapterNumber,DataString,Color,AnnotateMethod,BookLocation etc in database/Server to recreate highlight
                annotate(info.dataString, annotateType, "#ef9a9a")
            }
        }
    }

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

    fun annotate(jsonData: String, annotateType: AnnotateType, hashcolor: String) {
        var js = """
    var data = JSON.parse($jsonData);
    var selectedText = data['selectedText'];
	var startOffset = data['startOffset'];
	var endOffset = data['endOffset'];
	var startNodeData = data['startNodeData'];
	var startNodeHTML = data['startNodeHTML'];
	var startNodeTagName = data['startNodeTagName'];
	var endNodeData = data['endNodeData'];
	var endNodeHTML = data['endNodeHTML'];
	var endNodeTagName = data['endNodeTagName'];
    var tagList = document.getElementsByTagName(startNodeTagName);
    for (var i = 0; i < tagList.length; i++) {
        if (tagList[i].innerHTML == startNodeHTML) {
            var startFoundEle = tagList[i];
        }
    }
	var nodeList = startFoundEle.childNodes;
    for (var i = 0; i < nodeList.length; i++) {
        if (nodeList[i].data == startNodeData) {
            var startNode = nodeList[i];
        }
    }
	var tagList = document.getElementsByTagName(endNodeTagName);
    for (var i = 0; i < tagList.length; i++) {
        if (tagList[i].innerHTML == endNodeHTML) {
            var endFoundEle = tagList[i];
        }
    }
    var nodeList = endFoundEle.childNodes;
    for (var i = 0; i < nodeList.length; i++) {
        if (nodeList[i].data == endNodeData) {
            var endNode = nodeList[i];
        }
    }
    var range = document.createRange();
	range.setStart(startNode, startOffset);
    range.setEnd(endNode, endOffset);
    var sel = window.getSelection();
	sel.removeAllRanges();
	document.designMode = "on";
	sel.addRange(range);
"""
        js += when (annotateType) {
            AnnotateType.HIGHLIGHT -> "\tdocument.execCommand(\"HiliteColor\", false, \"$hashcolor\");\n"
            AnnotateType.UNDERLINE -> "\tdocument.execCommand(\"underline\");\n"
            AnnotateType.STRIKETHROUGH -> "\tdocument.execCommand(\"strikeThrough\");\n"
        }
        js += """ sel.removeAllRanges();
	document.designMode = "off";
	return "{\"status\":1}";
"""
        processJavascript(js)
    }

    fun exitSelectionMode() {
        actionMode!!.finish()
        val js = "window.getSelection().removeAllRanges();"
        processJavascript(js)
    }

    fun processTextSelection(postAction: (() -> Unit)?) {
        val js = """	var sel = window.getSelection();
	var jsonData ={};
	if(!sel.isCollapsed) {
		var range = sel.getRangeAt(0);
		startNode = range.startContainer;
		endNode = range.endContainer;
		jsonData['selectedText'] = range.toString();
		jsonData['startOffset'] = range.startOffset;  // where the range starts
		jsonData['endOffset'] = range.endOffset;      // where the range ends
		jsonData['startNodeData'] = startNode.data;                       // the actual selected text
		jsonData['startNodeHTML'] = startNode.parentElement.innerHTML;    // parent element innerHTML
		jsonData['startNodeTagName'] = startNode.parentElement.tagName;   // parent element tag name
		jsonData['endNodeData'] = endNode.data;                       // the actual selected text
		jsonData['endNodeHTML'] = endNode.parentElement.innerHTML;    // parent element innerHTML
		jsonData['endNodeTagName'] = endNode.parentElement.tagName;   // parent element tag name
		jsonData['status'] = 1;
	}else{
		jsonData['status'] = 0;
	}
	return (JSON.stringify(jsonData));"""
        evaluateJavascript("(function(){$js})()") { value ->
            val parseJson = value.substring(1, value.length - 1)
                .replace("\\\\\"".toRegex(), "\"")
                .replace("\\\\\\\\\"".toRegex(), "\\\\\"")
                .replace("\\\\\\\"".toRegex(), "\\\\\"")
                .replace("\\\\\\\\\\\"".toRegex(), "\\\\\"")
            selectedTextInfo =
                SelectedTextInfo.from(parseJson, chapterNumber, value)
                    ?: return@evaluateJavascript // TODO: show error
            postAction?.invoke()
            exitSelectionMode()
        }
    }

    suspend fun openEpubFile(uri: Uri) {
        withContext(IO) {
            val epub = context.contentResolver.openInputStream(uri).use { epubParser(context, inputStream = it!!) }
            this@EpubReaderView.epub = epub

            val epubTempExtractionLocation = context.cacheDir.toString() + "/tempfiles"
            val dirOEBPS = File(epubTempExtractionLocation + File.separator + "OEBPS")
            val relativeFolder = epub.chapters.firstOrNull()?.absPath?.substringBeforeLast("/")?.substringAfter(epub.rootPath) ?: ""
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

        scrollX = 0
        scrollY = 0
    }

    fun gotoFirstChapter() {
        if (epub.chapters.isEmpty()) return
        gotoChapter(epub.chapters.first())
        chapterNumber = 0
    }

    fun gotoChapter(chapter: EpubBook.Chapter) {
        loadDataWithBaseURL(
            resourceLocation,
            chapter.body,
            "text/html",
            "utf-8",
            null
        )
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
                totalWidth > scrollX + pageWidth -> scrollBy(shiftOffset(), 0)
                else -> previousChapter()
            }
            scrollX = min(computeHorizontalScrollRange() - width, scrollX)
        } else {
            val pageHeight = shiftOffset()
            val totalHeight = getTotalContentHeight()
            when {
                totalHeight > scrollY + height -> scrollBy(0, pageHeight)
                else -> nextChapter()
            }
        }
        loading = false
    }

    override fun pageUpWithNoAnimation() {
        if (loading) return

        if (isVerticalRead) {
            val pageWidth = shiftOffset()
            when {
                scrollX - pageWidth >= 0 -> scrollBy(-shiftOffset(), 0)
                scrollX > 0 -> scrollX = 0
                else -> nextChapter()
            }
            scrollBy(-shiftOffset(), 0)
            scrollX = max(0, scrollX)
        } else {
            val pageHeight = shiftOffset()
            when {
                scrollY - pageHeight >= 0 -> scrollBy(0, -shiftOffset())
                scrollY > 0 -> scrollY = 0
                else -> previousChapter()
            }
        }
        loading = false
    }

    fun nextChapter() {
        if (epub.chapters.size > chapterNumber + 1 && !loading) {
            chapterNumber++
            loading = true
            gotoChapter(epub.chapters[chapterNumber])
            listener.onChapterChangeListener(chapterNumber)
            listener.onPageChangeListener(
                this.chapterNumber,
                this.pageNumber,
                getProgressStart(),
                getProgressEnd()
            )
            loading = false
            scrollX = 0
            scrollY = 0
        } else if (chapterList.size <= chapterNumber + 1) {
            listener.onBookEndReached()
        }
    }

    fun previousChapter() {
        if (chapterNumber - 1 >= 0 && !loading) {
            loading = true
            chapterNumber--
            gotoChapter(epub.chapters[chapterNumber])
            listener.onChapterChangeListener(chapterNumber)
            listener.onPageChangeListener(
                chapterNumber,
                pageNumber,
                getProgressStart(),
                getProgressEnd()
            )
            loading = false
            postDelayed({
                val totalHeight = getTotalContentHeight()
                scrollY = totalHeight
            }, 200)
        } else if (chapterNumber - 1 < 0) {
            listener.onBookStartReached()
        }
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

enum class AnnotateType { HIGHLIGHT, UNDERLINE, STRIKETHROUGH }