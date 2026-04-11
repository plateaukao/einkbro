package info.plateaukao.einkbro.view.dialog.compose

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.CustomFontInfo
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.view.compose.MyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FontItem(
    val name: String,
    val uri: Uri,
)

class FontBrowserDialogFragment(
    private val isReaderMode: Boolean,
) : ComposeDialogFragment() {

    private val folderUri = mutableStateOf<String?>(null)

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            config.display.fontFolderUri = uri.toString()
            folderUri.value = uri.toString()
        }
    }

    override fun setupComposeView() {
        shouldShowInCenter = true
        folderUri.value = config.display.fontFolderUri

        composeView.setContent {
            MyTheme {
                FontBrowserScreen(
                    folderUri = folderUri.value,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onFontSelected = { fontInfo ->
                        if (isReaderMode) {
                            config.display.readerCustomFontInfo = fontInfo
                            config.display.readerFontType = FontType.CUSTOM
                        } else {
                            config.display.customFontInfo = fontInfo
                            config.display.fontType = FontType.CUSTOM
                        }
                        dismiss()
                    },
                    onDismiss = { dismiss() },
                )
            }
        }
    }
}

private val fontExtensions = setOf("ttf", "otf", "woff", "woff2")

private fun listFontsFromFolder(context: Context, folderUri: String): List<FontItem> {
    val treeUri = folderUri.toUri()
    val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
    return folder.listFiles()
        .filter { file ->
            val ext = file.name?.substringAfterLast('.', "")?.lowercase()
            ext in fontExtensions
        }
        .sortedBy { it.name?.lowercase() }
        .map { file -> FontItem(name = file.name ?: "Unknown", uri = file.uri) }
}

private fun loadTypeface(context: Context, uri: Uri): Typeface? {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            Typeface.Builder(pfd.fileDescriptor).build()
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun FontBrowserScreen(
    folderUri: String?,
    onSelectFolder: () -> Unit,
    onFontSelected: (CustomFontInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var fontItems by remember { mutableStateOf<List<FontItem>>(emptyList()) }

    LaunchedEffect(folderUri) {
        fontItems = if (folderUri.isNullOrBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                listFontsFromFolder(context, folderUri)
            }
        }
    }

    val filteredFonts = remember(fontItems, searchQuery) {
        if (searchQuery.isBlank()) fontItems
        else fontItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .width(320.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier.clickable { onDismiss() },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.custom_font),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSelectFolder) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (folderUri.isNullOrBlank()) stringResource(R.string.select_font_folder)
                    else stringResource(R.string.change_folder),
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        if (!folderUri.isNullOrBlank()) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        stringResource(R.string.search_fonts),
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colors.onBackground,
                    backgroundColor = MaterialTheme.colors.background,
                    cursorColor = MaterialTheme.colors.onBackground,
                    focusedIndicatorColor = MaterialTheme.colors.onBackground,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )

            HorizontalSeparator()

            if (filteredFonts.isEmpty()) {
                Text(
                    stringResource(R.string.no_fonts_found),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                ) {
                    items(filteredFonts, key = { it.uri.toString() }) { fontItem ->
                        FontItemRow(
                            fontItem = fontItem,
                            onClick = {
                                onFontSelected(
                                    CustomFontInfo(fontItem.name, fontItem.uri.toString())
                                )
                            },
                        )
                    }
                }
            }
        } else {
            // No folder selected prompt
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.select_font_folder),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }

        DialogOkButtonBar(okAction = onDismiss)
    }
}

@Composable
private fun FontItemRow(
    fontItem: FontItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val typeface = remember(fontItem.uri) {
        loadTypeface(context, fontItem.uri)
    }
    val fontFamily = remember(typeface) {
        typeface?.let { FontFamily(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = (-1).dp,
                color = MaterialTheme.colors.onBackground,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(
            text = fontItem.name,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (fontFamily != null) {
            Text(
                text = stringResource(R.string.font_preview_text),
                color = MaterialTheme.colors.onBackground,
                fontFamily = fontFamily,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        HorizontalSeparator()
    }
}
