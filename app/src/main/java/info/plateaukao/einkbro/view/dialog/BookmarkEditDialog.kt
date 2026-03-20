package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import kotlinx.coroutines.launch

class BookmarkEditDialog(
    private val activity: Activity,
    private val bookmarkViewModel: BookmarkViewModel,
    private val bookmark: Bookmark,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) {
    private val dialogManager: DialogManager = DialogManager(activity)

    private val titleState = mutableStateOf(bookmark.title)
    private val urlState = mutableStateOf(bookmark.url)
    private val foldersState = mutableStateOf(listOf<Bookmark>())
    private val selectedFolderIndex = mutableStateOf(0)
    private val dropdownExpanded = mutableStateOf(false)

    fun show() {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope

        // Load folders
        lifecycleScope.launch {
            val folders = bookmarkViewModel.getBookmarkFolders().toMutableList()
                .apply { add(0, Bookmark("Top", "", true)) }
            if (bookmark.isDirectory) folders.remove(bookmark)
            foldersState.value = folders
            selectedFolderIndex.value = folders.indexOfFirst { it.id == bookmark.parent }
                .coerceAtLeast(0)
        }

        val composeView = ComposeView(activity).apply {
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
                            minLines = 2,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        if (!bookmark.isDirectory) {
                            OutlinedTextField(
                                value = urlState.value,
                                onValueChange = { urlState.value = it },
                                label = { Text(stringResource(R.string.dialog_url_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = MaterialTheme.colors.onBackground,
                                ),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Folder:",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colors.onBackground,
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_add_folder),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onBackground,
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(8.dp)
                                    .clickable {
                                        lifecycleScope.launch {
                                            val folderName =
                                                dialogManager.getBookmarkFolderName()
                                                    ?: return@launch
                                            bookmarkViewModel.insertDirectory(folderName)
                                            val updatedFolders =
                                                bookmarkViewModel.getBookmarkFolders()
                                                    .toMutableList()
                                                    .apply {
                                                        add(0, Bookmark("Top", "", true))
                                                    }
                                            if (bookmark.isDirectory) updatedFolders.remove(
                                                bookmark
                                            )
                                            foldersState.value = updatedFolders
                                            selectedFolderIndex.value =
                                                updatedFolders.indexOfFirst { it.title == folderName }
                                                    .coerceAtLeast(0)
                                        }
                                    }
                            )
                        }
                        // Dropdown for folder selection
                        val folders = foldersState.value
                        if (folders.isNotEmpty()) {
                            val selectedName = folders.getOrNull(selectedFolderIndex.value)?.title ?: ""
                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded.value = true },
                                enabled = false,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    disabledTextColor = MaterialTheme.colors.onBackground,
                                    disabledBorderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                                ),
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded.value,
                                onDismissRequest = { dropdownExpanded.value = false },
                            ) {
                                folders.forEachIndexed { index, folder ->
                                    DropdownMenuItem(onClick = {
                                        selectedFolderIndex.value = index
                                        bookmark.parent = folder.id
                                        dropdownExpanded.value = false
                                    }) {
                                        Text(folder.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        dialogManager.showOkCancelDialog(
            title = activity.getString(R.string.menu_save_bookmark),
            view = composeView,
            okAction = {
                val newBookmark = bookmark.copy(
                    title = titleState.value.trim(),
                    url = urlState.value.trim()
                ).apply { id = bookmark.id }
                upsertBookmark(newBookmark)
            },
            cancelAction = { cancelAction.invoke() }
        )
    }

    private fun upsertBookmark(bookmark: Bookmark) {
        try {
            bookmarkViewModel.insertBookmark(bookmark) { okAction.invoke() }
        } catch (e: Exception) {
            e.printStackTrace()
            EBToast.show(activity, R.string.toast_error)
        }
    }
}
