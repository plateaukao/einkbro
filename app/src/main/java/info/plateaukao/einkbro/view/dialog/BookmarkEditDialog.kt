package info.plateaukao.einkbro.view.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.ComposeDialogFragment
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import kotlinx.coroutines.launch

class BookmarkEditDialog(
    private val bookmarkViewModel: BookmarkViewModel,
    private val bookmark: Bookmark,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                BookmarkEditContent(
                    bookmarkViewModel = bookmarkViewModel,
                    bookmark = bookmark,
                    okAction = {
                        okAction()
                        dismiss()
                    },
                    dismissAction = {
                        cancelAction()
                        dismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun BookmarkEditContent(
    bookmarkViewModel: BookmarkViewModel,
    bookmark: Bookmark,
    okAction: () -> Unit,
    dismissAction: () -> Unit,
) {
    val titleState = remember { mutableStateOf(bookmark.title) }
    val urlState = remember { mutableStateOf(bookmark.url) }
    val foldersState = remember { mutableStateOf(listOf<Bookmark>()) }
    val selectedFolderIndex = remember { mutableStateOf(0) }
    val dropdownExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val folders = bookmarkViewModel.getBookmarkFolders().toMutableList()
            .apply { add(0, Bookmark("Top", "", true)) }
        if (bookmark.isDirectory) folders.remove(bookmark)
        foldersState.value = folders
        selectedFolderIndex.value = folders.indexOfFirst { it.id == bookmark.parent }
            .coerceAtLeast(0)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dialogManager = remember { DialogManager(context as android.app.Activity) }

    AlertDialog(
        modifier = Modifier
            .padding(2.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        title = { Text(stringResource(R.string.menu_save_bookmark)) },
        text = {
            (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text(stringResource(R.string.dialog_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
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
                val folders = foldersState.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Folder:",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    if (folders.isNotEmpty()) {
                        val selectedName =
                            folders.getOrNull(selectedFolderIndex.value)?.title ?: ""
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dropdownExpanded.value = true },
                            enabled = false,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                disabledTextColor = MaterialTheme.colors.onBackground,
                                disabledBorderColor = MaterialTheme.colors.onBackground.copy(
                                    alpha = 0.5f
                                ),
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
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_add_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(46.dp)
                            .padding(8.dp)
                            .clickable {
                                coroutineScope.launch {
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
                                    if (bookmark.isDirectory) updatedFolders.remove(bookmark)
                                    foldersState.value = updatedFolders
                                    selectedFolderIndex.value =
                                        updatedFolders.indexOfFirst { it.title == folderName }
                                            .coerceAtLeast(0)
                                }
                            }
                    )
                }
            }
        },
        onDismissRequest = { dismissAction() },
        confirmButton = {
            TextButton(
                onClick = {
                    val newBookmark = bookmark.copy(
                        title = titleState.value.trim(),
                        url = urlState.value.trim()
                    ).apply { id = bookmark.id }
                    bookmarkViewModel.insertBookmark(newBookmark) { okAction() }
                }
            ) {
                Text(
                    stringResource(id = android.R.string.ok),
                    color = MaterialTheme.colors.onBackground
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissAction() }) {
                Text(
                    stringResource(id = android.R.string.cancel),
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    )
}
