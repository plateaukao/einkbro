@file:Suppress("DEPRECATION")

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit.restartApp
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class DialogManager(
    private val activity: Activity,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val inflater = LayoutInflater.from(activity)

    @Suppress("DEPRECATION")
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
        val epubFiles = mutableStateOf(config.savedEpubFileInfos.reversed())
        var dialogRef: Dialog? = null

        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
            setContent {
                MyTheme {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showAddNewEpub) {
                            EpubListItem(
                                title = stringResource(R.string.new_epub_or_from_picker),
                                showRemove = false,
                                onClick = { onNextAction(null); dialogRef?.dismiss() },
                            )
                        }
                        if (openEpubAction != null) {
                            EpubListItem(
                                title = stringResource(R.string.open_epub),
                                showRemove = false,
                                onClick = { openEpubAction(); dialogRef?.dismiss() },
                            )
                        }
                        epubFiles.value.forEach { epubFileInfo ->
                            EpubListItem(
                                title = "${epubFileInfo.title} (${getFileSizeString(epubFileInfo.uri)})",
                                showRemove = true,
                                onClick = {
                                    onNextAction(epubFileInfo.uri.toUri())
                                    dialogRef?.dismiss()
                                },
                                onRemove = {
                                    config.removeSavedEpubFile(epubFileInfo)
                                    epubFiles.value = config.savedEpubFileInfos.reversed()
                                },
                            )
                        }
                    }
                }
            }
        }

        dialogRef = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .apply { setView(composeView) }
            .create().apply {
                window?.decorView?.setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
                window?.decorView?.setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
            }
        dialogRef!!.show()
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
        val filename = URLUtil.guessFileName(url, null, null)
        val extension = filename.substring(filename.lastIndexOf("."))
        val titleState = mutableStateOf(HelperUnit.fileName(url))
        val extensionState = mutableStateOf(if (extension.length <= 8) extension else "")

        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
            setContent {
                MyTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp)
                    ) {
                        OutlinedTextField(
                            value = titleState.value,
                            onValueChange = { titleState.value = it },
                            label = { Text(stringResource(R.string.dialog_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        OutlinedTextField(
                            value = extensionState.value,
                            onValueChange = { extensionState.value = it },
                            label = { Text(stringResource(R.string.dialog_extension_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                    }
                }
            }
        }

        showOkCancelDialog(
            title = activity.getString(R.string.menu_edit),
            view = composeView,
            okAction = {
                val title = titleState.value.trim()
                val ext = extensionState.value.trim()
                val fileName = title + ext
                if (title.isEmpty() || ext.isEmpty() || !extension.startsWith(".")) {
                    EBToast.show(activity, activity.getString(R.string.toast_input_empty))
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
                window?.decorView?.setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
                window?.decorView?.setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
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
                window?.decorView?.setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
                window?.decorView?.setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
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

    suspend fun getSelectedOptionWithString(
        titleId: Int,
        listSettings: List<String>,
        defaultValue: Int,
    ): Int? =
        ListSettingWithNameDialog(
            activity,
            titleId,
            listSettings,
            defaultValue
        ).show()

    fun showBookmarkFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_TEXT
        intent.putExtra(Intent.EXTRA_TITLE, "bookmarks.json")
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

//    fun showRemoveHighlightConfirmDialog(
//        action: () -> Unit,
//    ) {
//        showOkCancelDialog(
//            messageResId = R.string.dialog_message_remove_highlight,
//            okAction = action,
//        )
//    }

    fun showRestartConfirmDialog() {
        showOkCancelDialog(
            messageResId = R.string.toast_restart,
            okAction = { restartApp(activity) }
        )
    }

    fun showInstapaperCredentialsDialog(
        confirmAction: (username: String, password: String) -> Unit,
    ) {
        val usernameState = mutableStateOf(config.instapaperUsername)
        val passwordState = mutableStateOf(config.instapaperPassword)
        var dialogRef: Dialog? = null

        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as androidx.lifecycle.LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as androidx.savedstate.SavedStateRegistryOwner)
            setContent {
                MyTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp)
                    ) {
                        OutlinedTextField(
                            value = usernameState.value,
                            onValueChange = { usernameState.value = it },
                            label = { Text(stringResource(R.string.instapaper_username_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        OutlinedTextField(
                            value = passwordState.value,
                            onValueChange = { passwordState.value = it },
                            label = { Text(stringResource(R.string.instapaper_password_hint)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.instapaper_create_account),
                            color = MaterialTheme.colors.primary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clickable {
                                    IntentUnit.launchUrl(
                                        activity,
                                        "https://www.instapaper.com/user/register"
                                    )
                                    dialogRef?.dismiss()
                                }
                        )
                    }
                }
            }
        }

        dialogRef = showOkCancelDialog(
            title = activity.getString(R.string.menu_instapaper),
            view = composeView,
            okAction = {
                val username = usernameState.value.trim()
                val password = passwordState.value.trim()

                if (username.isEmpty() || password.isEmpty()) {
                    EBToast.show(activity, activity.getString(R.string.toast_input_empty))
                } else {
                    config.instapaperUsername = username
                    config.instapaperPassword = password
                    confirmAction(username, password)
                }
            },
            cancelAction = { ViewUnit.hideKeyboard(activity) }
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

@Composable
private fun EpubListItem(
    title: String,
    showRemove: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 15.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colors.onBackground,
        )
        if (showRemove && onRemove != null) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_remove),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable { onRemove() },
            )
        }
    }
}
