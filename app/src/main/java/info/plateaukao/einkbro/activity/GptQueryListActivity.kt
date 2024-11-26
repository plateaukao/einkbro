package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.ChatGptQuery
import info.plateaukao.einkbro.unit.BackupUnit
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.GptQueryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GptQueryListActivity : ComponentActivity()  {
    private val gptQueryViewModel: GptQueryViewModel by viewModels()
    private val backupUnit: BackupUnit by lazy { BackupUnit(this) }
    private var currentKeyCode: MutableState<Int> = mutableIntStateOf(INVALID_KEYCODE)

    private lateinit var exportGptQueriesLauncher: ActivityResultLauncher<Intent>

    private fun showExportFileChooser() {
        BrowserUnit.createFilePicker(exportGptQueriesLauncher, "gpt_queries.html")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        ) {
            currentKeyCode.value = keyCode
            // workaround to reset the key code so that launchedeffect can be triggered
            window.decorView.postDelayed({ currentKeyCode.value = INVALID_KEYCODE }, 50)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportGptQueriesLauncher =
            IntentUnit.createResultLauncher(this) {
                it.data?.data?.let { uri -> exportGptQueries(uri) }
            }

        setContent {
            MyTheme {
                // Scaffold with a top bar and back button
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Gpt Results") }, // Set your desired title
                            navigationIcon = {
                                IconButton(onClick = {
                                    // Handle back button press
                                    onBackPressedDispatcher.onBackPressed()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                // Add a button to export highlights
                                IconButton(onClick = {
                                    showExportFileChooser()
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.icon_export),
                                        contentDescription = "Export"
                                    )
                                }
                            }
                        )
                    }
                ) { _ ->
                    GptQueriesScreen(
                        gptQueryViewModel,
                        keyCode = currentKeyCode.value,
                        onLinkClick = {
                            IntentUnit.launchUrl(this, it.url)
                        }
                    )
                }
            }
        }
    }

    private fun exportGptQueries(contentUri: Uri) {
        gptQueryViewModel.viewModelScope.launch(Dispatchers.IO) {
            val data = gptQueryViewModel.dumpGptQueriesAsHtml()
            backupUnit.exportDataToFileUri(contentUri, data)

            withContext(Dispatchers.Main) {
                EBToast.show(this@GptQueryListActivity, R.string.toast_backup_successful)
                IntentUnit.showFile(this@GptQueryListActivity, contentUri)
            }
        }
    }

    companion object {
        const val INVALID_KEYCODE: Int = 999

        fun createIntent(context: Context) = Intent(
            context,
            GptQueryListActivity::class.java
        )
    }
}

@Composable
fun GptQueriesScreen(
    gptQueryViewModel: GptQueryViewModel,
    keyCode: Int,
    onLinkClick: (ChatGptQuery) -> Unit,
) {
    val gptQueries by gptQueryViewModel.getGptQueries().collectAsState(emptyList())
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val forceExpandIndex = remember { mutableIntStateOf(-1) }

    LaunchedEffect(keyCode) {
        coroutineScope.launch {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (listState.firstVisibleItemIndex > 0) {
                        listState.scrollToItem(listState.firstVisibleItemIndex - 1)
                        forceExpandIndex.value = listState.firstVisibleItemIndex
                    }
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (listState.firstVisibleItemIndex < gptQueries.size) {
                        listState.scrollToItem(listState.firstVisibleItemIndex + 1)
                        forceExpandIndex.value = listState.firstVisibleItemIndex
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
    ) {
        items(gptQueries.size) { index ->
            val gptQuery = gptQueries[index]
            QueryItem(
                modifier = Modifier.padding(10.dp),
                gptQuery = gptQuery,
                forceExpand = forceExpandIndex.value == index,
                postAction = { coroutineScope.launch { listState.scrollToItem(index) } },
                onLinkClick = { onLinkClick(gptQuery) },
                deleteQuery = { gptQueryViewModel.deleteGptQuery(gptQuery) }
            )
            if (index < gptQueries.lastIndex) Divider(
                thickness = 8.dp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueryItem(
    modifier: Modifier,
    gptQuery: ChatGptQuery,
    forceExpand: Boolean = false,
    postAction: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    deleteQuery: () -> Unit,
) {
    // use a remember to toggle result widget visibility
    var showResult by remember { mutableStateOf(false) }
    val queryString = if (gptQuery.selectedText.contains("<<") &&
        gptQuery.selectedText.contains(">>")
    ) {
        HelperUnit.parseMarkdown(gptQuery.selectedText.replace("<<", "**").replace(">>", "**"))
    } else {
        AnnotatedString(gptQuery.selectedText)
    }

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    showResult = !showResult
                    postAction()
                },
                onLongClick = { deleteQuery() }
            )
    ) {
        Text(
            modifier = Modifier.padding(bottom = 5.dp),
            text = queryString,
            color = MaterialTheme.colors.onBackground
        )
        if (showResult || forceExpand) {
            Divider(
                modifier = Modifier.padding(horizontal = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = HelperUnit.parseMarkdown(gptQuery.result),
                color = MaterialTheme.colors.onBackground
            )
        }
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = gptQuery.model,
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onBackground
                )
            )
            if (gptQuery.url.isNotEmpty()) {
                Spacer(modifier = Modifier.size(10.dp))
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            onLinkClick()
                        },
                    imageVector = ImageVector.vectorResource(id = R.drawable.icon_exit),
                    contentDescription = "link",
                    tint = MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(gptQuery.date),
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onBackground
                )
            )
            Spacer(modifier = Modifier.size(10.dp))
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { deleteQuery() },
                imageVector = ImageVector.vectorResource(id = R.drawable.icon_delete),
                contentDescription = "delete",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

// theme white
@Preview(showBackground = true)
@Composable
fun PreviewQueryItem() {
    MyTheme {
        QueryItem(
            modifier = Modifier.padding(10.dp),
            gptQuery = ChatGptQuery(
                selectedText = "selected text",
                result = "result",
                date = System.currentTimeMillis(),
                url = "https://example.com",
                model = "model",
            ),
            onLinkClick = {},
            deleteQuery = {}
        )
    }
}
