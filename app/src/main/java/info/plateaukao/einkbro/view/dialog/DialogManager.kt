package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.DialogEditExtensionBinding
import info.plateaukao.einkbro.databinding.DialogSavedEpubListBinding
import info.plateaukao.einkbro.databinding.ListItemEpubFileBinding
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.NinjaToast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess


class DialogManager(
    private val activity: Activity,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val inflater = LayoutInflater.from(activity)

    fun createProgressDialog(
        titleResId: Int,
    ): ProgressDialog = ProgressDialog(activity, R.style.TouchAreaDialog).apply {
        setTitle(titleResId)
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        isIndeterminate = false
        max = 100
    }

    fun showSaveEpubDialog(
        showAddNewEpub: Boolean = true,
        openEpubAction: (() -> Unit)? = null,
        onNextAction: (Uri?) -> Unit,
    ) {
        val binding = DialogSavedEpubListBinding.inflate(inflater)
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .apply { setView(binding.root) }
            .show()

        setupSavedEpubFileList(binding, dialog, showAddNewEpub, openEpubAction, onNextAction)
    }

    private fun setupSavedEpubFileList(
        binding: DialogSavedEpubListBinding,
        dialog: Dialog,
        shouldAddNewEpub: Boolean = true,
        openEpubAction: (() -> Unit)? = null,
        onNextAction: (Uri?) -> Unit,
    ) {
        if (shouldAddNewEpub) {
            // add first item to handle picker case
            val firstItemBinding = ListItemEpubFileBinding.inflate(inflater).apply {
                buttonHide.visibility = View.GONE
                epubTitle.setText(R.string.new_epub_or_from_picker)
                root.setOnClickListener {
                    onNextAction(null)
                    dialog.dismiss()
                }
            }
            binding.epubInfoContainer.addView(firstItemBinding.root)
        }
        if (openEpubAction != null) {
            val openEpubItemBinding = ListItemEpubFileBinding.inflate(inflater).apply {
                buttonHide.visibility = View.GONE
                epubTitle.setText(R.string.open_epub)
                root.setOnClickListener {
                    openEpubAction()
                    dialog.dismiss()
                }
            }
            binding.epubInfoContainer.addView(openEpubItemBinding.root)
        }

        config.savedEpubFileInfos.reversed().forEach { epubFileInfo ->
            val itemBinding = ListItemEpubFileBinding.inflate(inflater)
            with(itemBinding.epubTitle) {
                text = "${epubFileInfo.title} (${getFileSizeString(epubFileInfo.uri)})"
                setOnClickListener {
                    onNextAction(epubFileInfo.uri.toUri())
                    dialog.dismiss()
                }
            }
            itemBinding.buttonHide.setOnClickListener {
                config.removeSavedEpubFile(epubFileInfo)
                binding.epubInfoContainer.removeView(itemBinding.root)
            }

            binding.epubInfoContainer.addView(itemBinding.root)
        }
    }

    private fun getFileSizeString(uri: String): String {
        val sizeInBytes = DocumentFile.fromSingleUri(activity, Uri.parse(uri))?.length() ?: 0
        val sizeInKB = sizeInBytes / 1024
        val sizeInMB = sizeInKB / 1024F
        return if (sizeInMB > 1) "%.1fMB".format(sizeInMB) else "${sizeInKB}KB"
    }

    fun showSaveFileDialog(
        url: String,
        saveFile: (String, String) -> Unit,
    ) {
        val menuView = DialogEditExtensionBinding.inflate(inflater)
        val editTitle = menuView.dialogEdit.apply {
            setHint(R.string.dialog_title_hint)
            setText(HelperUnit.fileName(url))
        }
        val editExtension = menuView.dialogEditExtension
        val filename = URLUtil.guessFileName(url, null, null)
        val extension = filename.substring(filename.lastIndexOf("."))
        if (extension.length <= 8) {
            editExtension.setText(extension)
        }
        showOkCancelDialog(
            title = activity.getString(R.string.menu_edit),
            view = menuView.root,
            okAction = {
                val title = editTitle.text.toString().trim { it <= ' ' }
                val ext = editExtension.text.toString().trim { it <= ' ' }
                val fileName = title + ext
                if (title.isEmpty() || ext.isEmpty() || !extension.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty))
                } else {
                    saveFile(url, fileName)
                }
            },
            cancelAction = { ViewUnit.hideKeyboard(activity) }
        )
    }

    fun showOkCancelDialog(
        title: String? = null,
        messageResId: Int? = null,
        message: String? = null,
        view: View? = null,
        okAction: () -> Unit,
        cancelAction: (() -> Unit)? = null,
        showInCenter: Boolean = false,
        showNegativeButton: Boolean = true,
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setPositiveButton(android.R.string.ok) { _, _ -> okAction() }
            .apply {
                title?.let { title -> setTitle(title) }
                view?.let { setView(it) }
                messageResId?.let { setMessage(messageResId) }
                message?.let { setMessage(message) }
                if (showNegativeButton) {
                    setNegativeButton(android.R.string.cancel) { _, _ -> cancelAction?.invoke() }
                }
            }
            .create().apply {
                window?.setGravity(if (config.isToolbarOnTop || showInCenter) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }

    fun showOptionDialog(
        view: View,
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setView(view)
            .create().apply {
                window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }

    suspend fun getBookmarkFolderName(): String? =
        TextInputDialog(
            activity,
            activity.getString(R.string.folder_name),
            activity.getString(R.string.folder_name_description),
            ""
        ).show()

    suspend fun <T> getTextInput(
        titleId: Int,
        descriptionId: Int,
        defaultValue: T,
    ): String? =
        TextInputDialog(
            activity,
            activity.getString(titleId),
            if (descriptionId == 0) "" else activity.getString(descriptionId),
            defaultValue.toString()
        ).show()

    suspend fun getSelectedOption(
        titleId: Int,
        listSettings: List<Int>,
        defaultValue: Int,
    ): Int? =
        ListSettingDialog(
            activity,
            titleId,
            listSettings,
            defaultValue
        ).show()

    fun showBookmarkFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_TEXT
        intent.putExtra(Intent.EXTRA_TITLE, "bookmark.txt")
        activity.startActivityForResult(intent, EXPORT_BOOKMARKS_REQUEST_CODE)
    }

    fun showImportBookmarkFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        activity.startActivityForResult(intent, IMPORT_BOOKMARKS_REQUEST_CODE)
    }

    fun showBackupFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.putExtra(Intent.EXTRA_TITLE, "backup.zip")
        activity.startActivityForResult(intent, EXPORT_BACKUP_REQUEST_CODE)
    }

    fun showImportBackupFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        activity.startActivityForResult(intent, IMPORT_BACKUP_REQUEST_CODE)
    }

    fun showRestartConfirmDialog() {
        showOkCancelDialog(
            messageResId = R.string.toast_restart,
            okAction = { restartApp() }
        )
    }

    fun showCreateOrOpenBookmarkFileDialog(
        createFileAction: () -> Unit,
        openFileAction: () -> Unit,
    ) {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setPositiveButton(R.string.bookmark_new_file) { _, _ -> createFileAction() }
            .apply {
                setTitle(context.getString(R.string.dialog_title_bookmark_sync))
                setMessage(context.getString(R.string.dialog_message_sync_bookmark_file))
                setNegativeButton(R.string.bookmark_open_file) { _, _ -> openFileAction() }
            }
        dialog.show()
    }

    private fun restartApp() {
        finishAffinity(activity) // Finishes all activities.
        activity.startActivity(activity.packageManager.getLaunchIntentForPackage(activity.packageName))    // Start the launch activity
        activity.overridePendingTransition(0, 0)
        exitProcess(0)
    }

    companion object {
        const val EXPORT_BOOKMARKS_REQUEST_CODE = 2345
        const val IMPORT_BOOKMARKS_REQUEST_CODE = 2346
        const val EXPORT_BACKUP_REQUEST_CODE = 2347
        const val IMPORT_BACKUP_REQUEST_CODE = 2348
    }
}

fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}
