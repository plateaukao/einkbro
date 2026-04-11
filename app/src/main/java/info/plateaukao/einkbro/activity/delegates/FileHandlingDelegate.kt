package info.plateaukao.einkbro.activity.delegates

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.activity.SavedPagesActivity
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.SavedPage
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileHandlingDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val bookmarkManager: BookmarkManager,
) {
    private val backupUnit: BackupUnit by lazy { BackupUnit(activity) }
    private val epubManager: EpubManager by lazy { EpubManager(activity) }
    private val dialogManager: DialogManager by lazy { DialogManager(activity) }

    lateinit var saveImageFilePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var createWebArchivePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var writeEpubFilePickerLauncher: ActivityResultLauncher<Intent>
    lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    lateinit var openEpubFilePickerLauncher: ActivityResultLauncher<Intent>

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun initLaunchers() {
        saveImageFilePickerLauncher = IntentUnit.createSaveImageFilePickerLauncher(activity)
        createWebArchivePickerLauncher =
            IntentUnit.createResultLauncher(activity) { saveWebArchive(it) }
        writeEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(activity) {
                val uri = backupUnit.preprocessActivityResult(it) ?: return@createResultLauncher
                saveEpub(uri)
            }
        fileChooserLauncher =
            IntentUnit.createResultLauncher(activity) { handleWebViewFileChooser(it) }
        openEpubFilePickerLauncher =
            IntentUnit.createResultLauncher(activity) { handleEpubUri(it) }
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
