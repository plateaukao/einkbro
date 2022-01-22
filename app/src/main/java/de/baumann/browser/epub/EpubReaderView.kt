package de.baumann.browser.epub

import android.os.Build
import android.view.View.OnTouchListener
import android.webkit.ValueCallback
import org.json.JSONObject
import android.webkit.JavascriptInterface
import nl.siegmann.epublib.service.MediatypeService
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.domain.Spine
import android.webkit.WebViewClient
import android.webkit.WebView
import android.annotation.TargetApi
import android.webkit.WebResourceRequest
import de.baumann.browser.Ninja.R
import android.animation.ObjectAnimator
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import androidx.appcompat.app.AlertDialog
import de.baumann.browser.browser.BrowserController
import de.baumann.browser.view.Album
import de.baumann.browser.view.NinjaWebView
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList

/**
 * Created by Avinash on 25-05-2017.
 */
class EpubReaderView(
    context: Context,
    browserController: BrowserController?
) : NinjaWebView(context, browserController) {
    private lateinit var book: Book
    var ChapterList: ArrayList<Chapter?> = ArrayList()
    private var ChapterNumber = 0
    private var Progress = 0f
    private var PageNumber = 0
    private var touchX = 0f
    private var touchY = 0f
    private var touchTime: Long = 0
    private var ResourceLocation = ""
    private var mActionMode: ActionMode? = null
    private var actionModeCallback: SelectActionModeCallback? = null
    var selectedText = ""
        private set
    private var loading = false
    private var listener: EpubReaderListener? = null
    @JvmField
    var THEME_LIGHT = 1
    @JvmField
    var THEME_DARK = 2
    @JvmField
    var METHOD_HIGHLIGHT = 1
    @JvmField
    var METHOD_UNDERLINE = 2
    @JvmField
    var METHOD_STRIKETHROUGH = 3
    private var current_theme = 1 //Light
    private var textSelectionMode = false

    interface EpubReaderListener {
        fun OnPageChangeListener(ChapterNumber: Int, PageNumber: Int, ProgressStart: Float, ProgressEnd: Float)
        fun OnChapterChangeListener(ChapterNumber: Int)
        fun OnTextSelectionModeChangeListner(mode: Boolean?)
        fun OnLinkClicked(url: String?)
        fun OnBookStartReached()
        fun OnBookEndReached()
        fun OnSingleTap()
    }

    fun setEpubReaderListener(listener: EpubReaderListener?) {
        this.listener = listener
    }

    inner class Chapter(var name: String, var content: String, var href: String)
    inner class SelectActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.d("onCreateActionMode", "triggered")
            mActionMode = mode
            textSelectionMode = true
            listener!!.OnTextSelectionModeChangeListner(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Log.d("onPrepareActionMode", "triggered")
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Log.d("onActionItemClicked", "triggered")
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            Log.d("onDestroyActionMode", "triggered")
            listener!!.OnTextSelectionModeChangeListner(false)
            textSelectionMode = false
        }
    }

    //For >5.0 Android Version
    override fun startActionMode(callback: ActionMode.Callback, ModeType: Int): ActionMode? {
        Log.d("startActionMode", "triggered")
        val parent = parent ?: return null
        actionModeCallback = SelectActionModeCallback()
        return parent.startActionModeForChild(this, actionModeCallback)
    }

    //For <=5.0 Android Version
    override fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        Log.d("startActionMode", "triggered")
        val parent = parent ?: return null
        actionModeCallback = SelectActionModeCallback()
        return parent.startActionModeForChild(this, actionModeCallback)
    }

    private fun init(context: Context) {
        val settings = this.settings
        settings.javaScriptEnabled = true
        settings.defaultTextEncodingName = "UTF-8"
        if (Build.VERSION.SDK_INT <= 19) addJavascriptInterface(JavaScriptInterface(), "js")
        setOnTouchListener(OnTouchListener { v, event ->
            val action = event.action
            when (action) {
                MotionEvent.ACTION_MOVE -> return@OnTouchListener if (Build.VERSION.SDK_INT <= 19 && textSelectionMode) false else true
                MotionEvent.ACTION_DOWN -> {
                    touchX = event.rawX
                    touchY = event.rawY
                    touchTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    val x = event.rawX
                    val y = event.rawY
                    if (touchX - x > ConvertIntoPixel(100) && System.currentTimeMillis() - touchTime < 500) {
                        NextPage()
                    } else if (x - touchX > ConvertIntoPixel(100) && System.currentTimeMillis() - touchTime < 500) {
                        PreviousPage()
                    } else if (touchY - y > ConvertIntoPixel(100) && System.currentTimeMillis() - touchTime < 500) {
                        NextPage()
                    } else if (y - touchY > ConvertIntoPixel(100) && System.currentTimeMillis() - touchTime < 500) {
                        PreviousPage()
                    } else if (Math.abs(y - touchY) < ConvertIntoPixel(10) && Math.abs(touchX - x) < ConvertIntoPixel(10) && System.currentTimeMillis() - touchTime < 250) {
                        Log.d("Tap Details", Math.abs(y - touchY).toString() + " " + Math.abs(touchX - x) + " " + (System.currentTimeMillis() - touchTime))
                        listener!!.OnSingleTap()
                    }
                }
            }
            false
        })
    }

    fun GetTheme(): Int {
        return current_theme
    }

    private fun ProcessJavascript(js: String, callbackFunction: String) {
        //Log.d("EpubReader",callbackFunction+" Called");
        if (Build.VERSION.SDK_INT > 19) {
            evaluateJavascript("(function(){$js})()") { }
        } else {
            this.loadUrl("javascript:js.$callbackFunction((function(){$js})())")
        }
    }

    fun SetTheme(theme: Int) {
        if (theme == THEME_LIGHT) {
            current_theme = THEME_LIGHT
            ProcessJavascript("""var elements = document.getElementsByTagName('*');
for (var i = 0; i < elements.length; i++) {
 if(elements[i].tagName!="SPAN")
  elements[i].style.backgroundColor='white';
 elements[i].style.color='black';
}""", "changeTheme")
        } else {
            current_theme = THEME_DARK
            ProcessJavascript("""var elements = document.getElementsByTagName('*');
for (var i = 0; i < elements.length; i++) {
 if(elements[i].tagName!="SPAN")
  elements[i].style.backgroundColor='black';
elements[i].style.color='white';
}""", "changeTheme")
        }
    }

    fun Annotate(jsonData: String, selectionMethod: Int, hashcolor: String) {
        var js = ""
        js = if (Build.VERSION.SDK_INT <= 19) """	var data = JSON.parse('${jsonData.replace("'", "\\'").replace("\"", "\\\"")}');
""" else "\tvar data = JSON.parse($jsonData);\n"
        js = """$js	var selectedText = data['selectedText'];
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
        if (selectionMethod == METHOD_HIGHLIGHT) js = "$js\tdocument.execCommand(\"HiliteColor\", false, \"$hashcolor\");\n"
        if (selectionMethod == METHOD_UNDERLINE) js = "$js\tdocument.execCommand(\"underline\");\n"
        if (selectionMethod == METHOD_STRIKETHROUGH) js = "$js\tdocument.execCommand(\"strikeThrough\");\n"
        js = """$js	sel.removeAllRanges();
	document.designMode = "off";
	return "{\"status\":1}";
"""
        ProcessJavascript(js, "annotate")
    }

    fun ExitSelectionMode() {
        mActionMode!!.finish()
        val js = "window.getSelection().removeAllRanges();"
        ProcessJavascript(js, "deselect")
    }

    fun ProcessTextSelection() {
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
        if (Build.VERSION.SDK_INT > 19) {
            evaluateJavascript("(function(){$js})()",
                    object : ValueCallback<String> {
                        override fun onReceiveValue(value: String) {
                            //Log.v("EpubReader", "SELECTION>19:" + value);
                            //Log.v("EpubReader", "SELECTION_P>19:" +  value.substring(1,value.length()-1).replaceAll("\\\\\"","\""));
                            //Log.v("EpubReader", "SELECTION_P>19:" +  value.substring(1,value.length()-1).replaceAll("\\\\\"","\"").replaceAll("\\\\\\\\\"","\\\\\"").replaceAll("\\\\\\\"","\\\\\"").replaceAll("\\\\\\\\\\\"","\\\\\""));
                            var text = ""
                            try {
                                val parse_json = value.substring(1, value.length - 1).replace("\\\\\"".toRegex(), "\"").replace("\\\\\\\\\"".toRegex(), "\\\\\"").replace("\\\\\\\"".toRegex(), "\\\\\"").replace("\\\\\\\\\\\"".toRegex(), "\\\\\"")
                                val `object` = JSONObject(parse_json)
                                text = `object`.getString("selectedText")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val selectedTextJson = JSONObject()
                            try {
                                selectedTextJson.put("DataString", value)
                                selectedTextJson.put("ChapterNumber", ChapterNumber)
                                selectedTextJson.put("SelectedText", text)
                            } catch (e: Exception) {
                                selectedText = ""
                            }
                            selectedText = selectedTextJson.toString()
                        }
                    })
        } else {
            this.loadUrl("javascript:js.selection((function(){$js})())")
            //this.loadUrl("javascript:js.selection2((function(){window.getSelection().toString()})())");
            //this.loadUrl("javascript:js.selection2((function(){document.getSelection().toString()})())");
            //this.loadUrl("javascript:js.selection2((function(){document.selection.createRange().text})())");
        }
    }

    inner class JavaScriptInterface {
        @JavascriptInterface
        fun selection(value: String) {
            //Log.v("EpubReader", "SELECTION<=19:" + value);
            var text = ""
            try {
                val `object` = JSONObject(value //.substring(1,value.length()-1).replaceAll("\\\\\\\"","\\\"").replaceAll("\\\"","\"");
                )
                if (`object`.has("selectedText")) text = `object`.getString("selectedText")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (text != "") {
                val selectedTextJson = JSONObject()
                try {
                    selectedTextJson.put("DataString", value)
                    selectedTextJson.put("ChapterNumber", ChapterNumber)
                    selectedTextJson.put("SelectedText", text)
                } catch (e: Exception) {
                    selectedText = ""
                }
                selectedText = selectedTextJson.toString()
            }
        }

        fun selection2(value: String?) {
            //Log.v("EpubReader", "SELECTION2<=19:" + value);
        }

        @JavascriptInterface
        fun annotate(response: String?) {
            //Log.v("EpubReader","annotate<=19 "+response);
        }

        @JavascriptInterface
        fun deselect(response: String?) {
            //Log.v("EpubReader","Deselect<=19 "+response);
        }
    }

    private fun DownloadResource(directory: String) {
        //Log.d("epubResourcePath", directory);
        try {
            val rst = book.resources
            val clrst = rst.all
            val itr: Iterator<Resource> = clrst.iterator()
            while (itr.hasNext()) {
                val rs = itr.next()
                if (rs.mediaType === MediatypeService.JPG || rs.mediaType === MediatypeService.PNG || rs.mediaType === MediatypeService.GIF || rs.mediaType === MediatypeService.CSS) {
                    val oppath1 = File(directory + File.separator + rs.href)
                    val dir = File(oppath1.absolutePath.substring(0, oppath1.absolutePath.lastIndexOf("/")))
                    if (!dir.exists()) dir.mkdirs()
                    oppath1.createNewFile()
                    val fos1 = FileOutputStream(oppath1)
                    fos1.write(rs.data)
                    fos1.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("error", e.message!!)
        }
    }

    fun OpenEpubFile(epub_location: String?) {
        var epubInputStream: InputStream? = null
        try {
            epubInputStream = context!!.contentResolver.openInputStream(Uri.parse(epub_location))
            //epubInputStream = new BufferedInputStream(new FileInputStream(epub_location));
            this.book = EpubReader().readEpub(epubInputStream)
            val book = this.book
            val epub_temp_extraction_location = context!!.cacheDir.toString() + "/tempfiles"
            deleteFiles(File(epub_temp_extraction_location))
            if (!File(epub_temp_extraction_location).exists()) File(epub_temp_extraction_location).mkdirs()
            try {
                DownloadResource(epub_temp_extraction_location)
            } catch (e: Exception) {
                Log.e("Exception", e.message!!)
            }
            val dir1 = File(epub_temp_extraction_location + File.separator + "OEBPS")
            val resource_folder = book.getOpfResource().href.replace("content.opf", "").replace("/", "")
            val dir2 = File(epub_temp_extraction_location + File.separator + resource_folder)
            ResourceLocation = if (dir1.exists() && dir1.isDirectory) {
                "file://" + epub_temp_extraction_location + File.separator + "OEBPS" + File.separator
            } else if (dir2.exists() && dir2.isDirectory && resource_folder != "") {
                "file://" + epub_temp_extraction_location + File.separator + resource_folder + File.separator
            } else {
                "file://" + epub_temp_extraction_location + File.separator
            }
            ChapterList.clear()
            if (ResourceLocation.contains("OEPBS") && book.getTableOfContents().tocReferences.size > 1) ProcessChaptersByTOC(book.getTableOfContents().tocReferences) else if (book.getTableOfContents().tocReferences.size > 1) {
                ProcessChaptersByTOC(book.getTableOfContents().tocReferences)
            } else ProcessChaptersBySpline(book.getSpine())
        } catch (e: Exception) {
        } finally {
            if (epubInputStream != null) {
                try {
                    epubInputStream.close()
                } catch (e: IOException) {
                    // ignore
                }
            }
        }
    }

    private fun ProcessChaptersByTOC(tocReferences: List<TOCReference>) {
        if (tocReferences.size > 0) {
            for (TOC in tocReferences) {
                val builder = StringBuilder()
                try {
                    val r = BufferedReader(InputStreamReader(TOC.resource.inputStream))
                    var aux: String? = ""
                    while (r.readLine().also { aux = it } != null) {
                        builder.append(aux)
                    }
                } catch (e: Exception) {
                }
                ChapterList.add(Chapter(TOC.title, builder.toString(), TOC.completeHref))
                if (TOC.children.size > 0) {
                    ProcessChaptersByTOC(TOC.children)
                }
            }
        }
    }

    private fun ProcessChaptersBySpline(spine: Spine?) {
        var ChapterNumber = 1
        if (spine != null) {
            for (i in 0 until spine.size()) {
                val builder = StringBuilder()
                try {
                    val r = BufferedReader(InputStreamReader(spine.getResource(i).inputStream))
                    var aux: String? = ""
                    while (r.readLine().also { aux = it } != null) {
                        builder.append(aux)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                ChapterList.add(Chapter(if (spine.getResource(i).title != null) spine.getResource(i).title else ChapterNumber.toString() + "", builder.toString(), spine.getResource(i).href))
                ChapterNumber++
            }
        } else {
            Log.d("EpubReader", "spline is null")
        }
    }

    fun GotoPosition(ChapterNumber: Int, Progress: Float) {
        if (ChapterNumber < 0) {
            this.ChapterNumber = 0
            this.Progress = 0f
        } else if (ChapterNumber >= ChapterList.size) {
            this.ChapterNumber = ChapterList.size - 1
            this.Progress = 1f
        } else {
            this.ChapterNumber = ChapterNumber
            this.Progress = Progress
        }
        loadDataWithBaseURL(ResourceLocation, ChapterList[this.ChapterNumber]!!.content.replace("href=\"http".toRegex(), "hreflink=\"http").replace("<a href=\"[^\"]*".toRegex(), "<a ").replace("hreflink=\"http".toRegex(), "href=\"http"), "text/html", "utf-8", null)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                SetTheme(current_theme)
                val handler = Handler()
                handler.postDelayed({
                    val TotalHeight = GetTotalContentHeight()
                    scrollTo(0, (TotalHeight * Progress).toInt())
                }, 500)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                listener!!.OnLinkClicked(url)
                return true
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                listener!!.OnLinkClicked(request.url.toString())
                return true
            }
        }
    }

    fun ListChaptersDialog(theme: Int) {
        try {
            val ChapterListString = ArrayList<String>()
            for (i in ChapterList.indices) {
                ChapterListString.add(ChapterList[i]!!.name)
            }
            val items = ChapterListString.toTypedArray()
            val alertbuilder: AlertDialog.Builder
            alertbuilder = if (theme == THEME_DARK) AlertDialog.Builder(context!!, R.style.TouchAreaDialog) else AlertDialog.Builder(context!!, R.style.TouchAreaDialog)
            alertbuilder.setTitle("Select the Chapter")
            alertbuilder.setItems(items) { dialog, item ->
                GotoPosition(item, 0f)
                listener!!.OnChapterChangeListener(item)
            }
            val alert = alertbuilder.create()
            alert.show()
        } catch (e: Exception) {
        }
    }

    fun NextPage() {
        if (!loading) {
            val pageHeight = this.height - 50
            val TotalHeight = GetTotalContentHeight()
            if (TotalHeight > this.scrollY + this.height) {
                loading = true
                Progress = (this.scrollY + pageHeight).toFloat() / TotalHeight
                PageNumber = ((this.scrollY + pageHeight) / pageHeight)
                val anim = ObjectAnimator.ofInt(this, "scrollY",
                        (PageNumber - 1) * pageHeight, PageNumber * pageHeight)
                anim.duration = 400
                anim.start()
                listener!!.OnPageChangeListener(GetChapterNumber(), GetPageNumber(), GetProgressStart(), GetProgressEnd())
                Log.d("EpubReaderProgress", Progress.toString() + " " + pageHeight + " " + this.scrollY + " " + TotalHeight)
                loading = false
            } else {
                NextChapter()
            }
        }
    }

    fun PreviousPage() {
        if (!loading) {
            val pageHeight = this.height - 50
            val TotalHeight = GetTotalContentHeight()
            if (this.scrollY - pageHeight >= 0) {
                loading = true
                Progress = (this.scrollY - pageHeight).toFloat() / TotalHeight
                PageNumber = ((this.scrollY - pageHeight) / pageHeight)
                val anim = ObjectAnimator.ofInt(this, "scrollY",
                        ((PageNumber + 1) * pageHeight), (PageNumber * pageHeight))
                anim.duration = 400
                anim.start()
                listener!!.OnPageChangeListener(GetChapterNumber(), GetPageNumber(), GetProgressStart(), GetProgressEnd())
                loading = false
            } else if (this.scrollY > 0) {
                loading = true
                Progress = 0f
                PageNumber = 0
                val anim = ObjectAnimator.ofInt(this, "scrollY",
                        ((PageNumber + 1) * pageHeight), (PageNumber * pageHeight))
                anim.duration = 400
                anim.start()
                listener!!.OnPageChangeListener(GetChapterNumber(), GetPageNumber(), GetProgressStart(), GetProgressEnd())
                loading = false
            } else {
                PreviousChapter()
            }
        }
    }

    fun NextChapter() {
        if (ChapterList.size > ChapterNumber + 1 && !loading) {
            loading = true
            GotoPosition(ChapterNumber + 1, 0f)
            listener!!.OnChapterChangeListener(ChapterNumber)
            listener!!.OnPageChangeListener(GetChapterNumber(), GetPageNumber(), GetProgressStart(), GetProgressEnd())
            loading = false
        } else if (ChapterList.size <= ChapterNumber + 1) {
            listener!!.OnBookEndReached()
        }
    }

    fun PreviousChapter() {
        if (ChapterNumber - 1 >= 0 && !loading) {
            loading = true
            GotoPosition(ChapterNumber - 1, 1f)
            listener!!.OnChapterChangeListener(ChapterNumber)
            listener!!.OnPageChangeListener(GetChapterNumber(), GetPageNumber(), GetProgressStart(), GetProgressEnd())
            loading = false
        } else if (ChapterNumber - 1 < 0) {
            listener!!.OnBookStartReached()
        }
    }

    fun GetChapterContent(): String {
        return ChapterList[ChapterNumber]!!.content
    }

    private fun GetTotalContentHeight(): Int {
        return (this.contentHeight * resources.displayMetrics.density).toInt()
    }

    fun GetPageHeight(): Int {
        return this.height - 50
    }

    fun GetProgressStart(): Float {
        return Progress
    }

    fun GetProgressEnd(): Float {
        val value: Float = Progress + GetPageHeight() / GetTotalContentHeight()
        return when {
            GetTotalContentHeight() <= 0 -> Progress
            value < 1 -> value
            else -> 1.0F
        }
    }

    fun GetChapterNumber(): Int {
        return ChapterNumber
    }

    fun GetPageNumber(): Int {
        return PageNumber
    }

    private fun ConvertIntoPixel(dp: Int): Int {
        val r = context!!.resources
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics))
    }

    companion object {
        private fun deleteFiles(file: File) {
            if (file.isDirectory) {
                val files = file.listFiles() //All files and sub folders
                var x = 0
                while (files != null && x < files.size) {
                    deleteFiles(files[x])
                    x++
                }
                file.delete()
            } else file.delete()
        }
    }
}