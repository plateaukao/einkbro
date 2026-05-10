package info.plateaukao.einkbro.activity.delegates

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintCallbacks
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.provider.DocumentsContract
import android.webkit.ValueCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.activity.SavedPagesActivity
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.SavedPage
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.SupernoteStorage
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class FileHandlingDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val bookmarkManager: BookmarkManager,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val backupUnit: BackupUnit by lazy { BackupUnit(activity) }
    private val epubManager: EpubManager by lazy { EpubManager(activity) }
    private val dialogManager: DialogManager by lazy { DialogManager(activity) }

    lateinit var saveImageFilePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var createWebArchivePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var savePdfFilePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var writeEpubFilePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    lateinit var openEpubFilePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var supernoteFolderPickerLauncher: ActivityResultLauncher<Uri?>

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun initLaunchers() {
        saveImageFilePickerLauncher = IntentUnit.createSaveImageFilePickerLauncher(activity)
        createWebArchivePickerLauncher =
            IntentUnit.createResultLauncher(activity) { saveWebArchive(it) }
        savePdfFilePickerLauncher =
            IntentUnit.createResultLauncher(activity) { savePdfFromPickerResult(it) }
        writeEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(activity) {
                val uri = backupUnit.preprocessActivityResult(it) ?: return@createResultLauncher
                saveEpub(uri)
            }
        fileChooserLauncher =
            IntentUnit.createResultLauncher(activity) { handleWebViewFileChooser(it) }
        openEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(activity) { handleEpubUri(it) }
        supernoteFolderPickerLauncher =
            activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                SupernoteStorage.onPickerResult(activity, uri)
            }
        SupernoteStorage.registerPicker(supernoteFolderPickerLauncher)
    }

    private fun handleEpubUri(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val uri = result.data?.data ?: return
        HelperUnit.openFile(activity, uri)
    }

    private fun handleWebViewFileChooser(result: ActivityResult) {
        if (result.resultCode != RESULT_OK || filePathCallback == null) {
            filePathCallback = null
            return
        }
        val data = result.data
        if (data != null) {
            val dataString = data.dataString
            val results = arrayOf(Uri.parse(dataString))
            filePathCallback?.onReceiveValue(results)
        }
        filePathCallback = null
    }

    @Suppress("DEPRECATION")
    fun saveEpub(uri: Uri) {
        val progressDialog =
            dialogManager.createProgressDialog(R.string.saving_epub).apply { show() }
        epubManager.saveEpub(
            activity,
            uri,
            state.ebWebView,
            {
                progressDialog.progress = it
                if (it == 100) {
                    progressDialog.dismiss()
                }
            },
            {
                progressDialog.dismiss()
                dialogManager.showOkCancelDialog(
                    messageResId = R.string.cannot_save_epub,
                    okAction = {},
                    showInCenter = true,
                    showNegativeButton = false,
                )
            }
        )
    }

    private fun saveWebArchive(result: ActivityResult) {
        val uri = backupUnit.preprocessActivityResult(result) ?: return
        saveWebArchiveToUri(uri)
    }

    private fun saveWebArchiveToUri(uri: Uri) {
        val filePath = File(activity.filesDir.absolutePath + "/temp.mht").absolutePath
        state.ebWebView.saveWebArchive(filePath, false) {
            val tempFile = File(filePath)
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        fileChooserLauncher.launch(chooserIntent)
    }

    fun showEpubDialog() = dialogManager.showEpubDialog(
        onSaveEpub = { uri ->
            if (uri == null) {
                epubManager.showWriteEpubFilePicker(
                    writeEpubFilePickerLauncher,
                    state.ebWebView.title ?: "einkbro"
                )
            } else {
                saveEpub(uri)
            }
        },
        onOpenEpub = { uri ->
            if (uri != null) {
                HelperUnit.openFile(activity, uri)
            } else {
                epubManager.showOpenEpubFilePicker(openEpubFilePickerLauncher)
            }
        },
    )

    fun showWebArchiveFilePicker() {
        val fileName = "${state.ebWebView.title}.mht"
        BrowserUnit.createFilePicker(createWebArchivePickerLauncher, fileName)
    }

    fun showPdfFilePicker() {
        val fileName = "${HelperUnit.fileName(state.ebWebView.url)}.pdf"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (HelperUnit.isSupernoteDocumentInstalled(activity)) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, HelperUnit.supernoteDocumentInitialUri())
            }
        }
        savePdfFilePickerLauncher.launch(intent)
    }

    private fun savePdfFromPickerResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val uri = result.data?.data ?: return
        savePdfToUri(uri)
    }

    private fun savePdfToUri(uri: Uri) {
        val pfd = runCatching { activity.contentResolver.openFileDescriptor(uri, "rw") }
            .getOrNull()
        if (pfd == null) {
            EBToast.show(activity, R.string.toast_error)
            return
        }

        val ebWebView = state.ebWebView
        val mediaSize = config.display.pdfPaperSize.mediaSize
        // WebView's print pipeline lays out the page at mediaWidthInches * 96 CSS px,
        // regardless of Resolution dpi. Match that against the live page's layout width
        // so the PDF preserves the proportions the user already sees on screen.
        val printViewportCssPx = mediaSize.widthMils / 1000.0 * 96.0

        ebWebView.evaluateJavascript(HelperUnit.loadAssetFile("pdf_measure_layout.js")) { result ->
            val pageWidth = result?.toDoubleOrNull()?.takeIf { it > 0 } ?: printViewportCssPx
            val zoom = (printViewportCssPx / pageWidth).coerceIn(0.4, 1.5)
            val styleJs = HelperUnit.loadAssetFile("pdf_print_style.js")
                .replace("__ZOOM__", "%.3f".format(java.util.Locale.US, zoom))
            ebWebView.evaluateJavascript(styleJs) {
                startPdfRender(ebWebView, uri, pfd)
            }
        }
    }

    private fun startPdfRender(
        ebWebView: EBWebView,
        uri: Uri,
        pfd: android.os.ParcelFileDescriptor,
    ) {
        val title = ebWebView.title.orEmpty().ifBlank { "page" }
        val adapter = ebWebView.createPrintDocumentAdapter(title) { /* unused */ }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(config.display.pdfPaperSize.mediaSize)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        adapter.onStart()
        adapter.onLayout(null, attrs, CancellationSignal(),
            object : PrintCallbacks.LayoutCallback() {
                override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                    adapter.onWrite(
                        arrayOf(PageRange.ALL_PAGES),
                        pfd,
                        CancellationSignal(),
                        object : PrintCallbacks.WriteCallback() {
                            override fun onWriteFinished(pages: Array<out PageRange>?) {
                                finalizePdf(adapter, pfd, uri, success = true)
                            }
                            override fun onWriteFailed(error: CharSequence?) {
                                finalizePdf(adapter, pfd, uri, success = false)
                            }
                            override fun onWriteCancelled() {
                                finalizePdf(adapter, pfd, uri, success = false)
                            }
                        }
                    )
                }
                override fun onLayoutFailed(error: CharSequence?) {
                    finalizePdf(adapter, pfd, uri, success = false)
                }
                override fun onLayoutCancelled() {
                    finalizePdf(adapter, pfd, uri, success = false)
                }
            },
            null
        )
    }

    private fun finalizePdf(
        adapter: PrintDocumentAdapter,
        pfd: android.os.ParcelFileDescriptor,
        uri: Uri,
        success: Boolean,
    ) {
        runCatching { pfd.close() }
        runCatching { adapter.onFinish() }
        activity.runOnUiThread {
            state.ebWebView.evaluateJavascript(
                HelperUnit.loadAssetFile("pdf_print_cleanup.js"),
                null,
            )
            if (success) {
                dialogManager.showOkCancelDialog(
                    messageResId = R.string.toast_downloadComplete,
                    okAction = { HelperUnit.openFile(activity, uri) },
                )
            } else {
                runCatching { activity.contentResolver.delete(uri, null, null) }
                EBToast.show(activity, R.string.toast_error)
            }
        }
    }

    fun savePageForLater() {
        val ebWebView = state.ebWebView
        val title = ebWebView.title.orEmpty()
        val url = ebWebView.url.orEmpty()
        if (url.isBlank()) return

        val savedPagesDir = File(activity.filesDir, "saved_pages")
        if (!savedPagesDir.exists()) savedPagesDir.mkdirs()
        val fileName = "${System.currentTimeMillis()}.mht"
        val filePath = File(savedPagesDir, fileName).absolutePath

        ebWebView.saveWebArchive(filePath, false) { savedPath ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                if (savedPath != null) {
                    bookmarkManager.insertSavedPage(
                        SavedPage(
                            title = title,
                            url = url,
                            filePath = savedPath,
                            savedAt = System.currentTimeMillis(),
                        )
                    )
                    withContext(Dispatchers.Main) {
                        EBToast.show(activity, R.string.toast_saved_page)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        EBToast.show(activity, R.string.toast_save_page_failed)
                    }
                }
            }
        }
    }

    fun showSavedPages() {
        activity.startActivity(SavedPagesActivity.createIntent(activity))
    }

    fun showOpenEpubFilePicker() =
        epubManager.showOpenEpubFilePicker(openEpubFilePickerLauncher)
}
