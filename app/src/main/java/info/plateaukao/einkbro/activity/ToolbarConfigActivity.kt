package info.plateaukao.einkbro.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.ComposedIconBar
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.toolbaricons.ToolbarActionInfo
import org.koin.android.ext.android.inject
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ToolbarPosition
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.ReorderableComposedIconBar
import info.plateaukao.einkbro.view.compose.ReorderableComposedIconColumn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

class ToolbarConfigActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isReaderMode = intent.getBooleanExtra(EXTRA_IS_READER_MODE, false)
        val iconEnums = if (isReaderMode) config.readerToolbarActions else config.toolbarActions
        val toolbarActionInfoList = iconEnums.toToolbarActionInfoList()

        setContent {
            MyTheme {
                var list = remember { mutableStateOf(toolbarActionInfoList) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(id = if (isReaderMode) R.string.reader_toolbar else R.string.toolbars))
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                                IconButton(onClick = {
                                    // save the toolbarActionInfoList
                                    if (isReaderMode) {
                                        config.readerToolbarActions = list.value.map { it.toolbarAction }
                                    } else {
                                        config.toolbarActions = list.value.map { it.toolbarAction }
                                    }
                                    finish()
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = null)
                                }
                            },
                        )
                    },
                    content = { padding ->
                        ToolbarConfigPanel(
                            list = list,
                            isVerticalPreview = config.isVerticalToolbar,
                        )
                    }
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(
                LocaleManager.setLocale(newBase, config.uiLocaleLanguage)
            )
        } else {
            super.attachBaseContext(newBase)
        }
    }

    companion object {
        const val EXTRA_IS_READER_MODE = "extra_is_reader_mode"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarConfigPanel(
    list: MutableState<List<ToolbarActionInfo>>,
    isVerticalPreview: Boolean = false,
) {
    val isLandscape = ViewUnit.isLandscape(LocalContext.current)

    val onRemoveAction: (ToolbarAction) -> Unit = { action ->
        if (action != ToolbarAction.Settings) {
            list.value = list.value.toMutableList().apply {
                val info = find { it.toolbarAction == action }
                remove(info)
            }
        }
    }

    if (isVerticalPreview) {
        // Side preview layout: preview column on left/right, available actions in center
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 50.dp),
        ) {
            // Vertical preview bar on the left side
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(50.dp)
                    .border(1.dp, MaterialTheme.colors.onBackground),
            ) {
                ReorderableComposedIconColumn(
                    list = list,
                    title = "Toolbar Configuration",
                    tabCount = "7",
                    pageInfo = "4/21",
                    onClick = onRemoveAction,
                )
            }
            // Available actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 1.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    modifier = Modifier.padding(start = 10.dp, top = 5.dp, bottom = 5.dp),
                    text = "Available Actions",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
                    text = "click icon to add; click preview to remove; long click to reorder",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.caption
                )
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Adaptive(84.dp),
                ) {
                    val selectedActions = list.value.map { it.toolbarAction }
                    val otherActionInfos = ToolbarAction.entries
                        .filter { it !in selectedActions }
                        .toToolbarActionInfoList()
                    itemsIndexed(otherActionInfos) { index, info ->
                        AvailableActionItem(info) {
                            list.value = list.value.toMutableList().apply { add(info) }
                        }
                    }
                }
            }
        }
    } else {
        // Original horizontal preview layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, start = 1.dp, end = 1.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
                        text = "Available Actions",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.h6
                    )
                    if (isLandscape) {
                        Text(
                            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                            text = "click icon to add it to the toolbar",
                            color = MaterialTheme.colors.onBackground,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
                if (!isLandscape) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        text = "click icon to add it to the toolbar",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.caption
                    )
                }
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Adaptive(84.dp),
                ) {
                    val selectedActions = list.value.map { it.toolbarAction }
                    val otherActionInfos = ToolbarAction.entries
                        .filter { it !in selectedActions }
                        .toToolbarActionInfoList()
                    itemsIndexed(otherActionInfos) { index, info ->
                        AvailableActionItem(info) {
                            list.value = list.value.toMutableList().apply { add(info) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = "Preview",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.h6
                )
                if (isLandscape) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        text = "click icon to remove it from the toolbar; long click to drag icon to reorder",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            if (!isLandscape) {
                Text(
                    modifier = Modifier.padding(start = 10.dp, bottom = 20.dp),
                    text = "click icon to remove it from the toolbar; long click to drag icon to reorder",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colors.onBackground),
                contentAlignment = Alignment.CenterEnd
            ) {
                ReorderableComposedIconBar(
                    list = list,
                    title = "Toolbar Configuration",
                    tabCount = "7",
                    pageInfo = "4/21",
                    onClick = onRemoveAction,
                )
            }
        }
    }
}

@Composable
private fun AvailableActionItem(info: ToolbarActionInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = info.toolbarAction.imageVector
                ?: ImageVector.vectorResource(id = info.toolbarAction.iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(horizontal = 6.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colors.onBackground
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = info.toolbarAction.titleResId),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

private fun List<ToolbarAction>.toToolbarActionInfoList(): List<ToolbarActionInfo> =
    this.map { ToolbarActionInfo(it, false) }

@Preview(showBackground = true)
@Composable
fun PreviewToolbarConfigPanel() {
    MyTheme {
        val toolbarActionInfoList = ToolbarAction.defaultActions.toToolbarActionInfoList()
        var list = remember { mutableStateOf(toolbarActionInfoList) }
        ToolbarConfigPanel(
            list = list,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewToolbar() {
    MyTheme {
        val toolbarActionInfoList = listOf(ToolbarAction.Time).toToolbarActionInfoList()
        ComposedIconBar(
            toolbarActionInfos = toolbarActionInfoList,
            title = "Toolbar Configuration",
            tabCount = "7",
            pageInfo = "4/21",
            isIncognito = false,
            onClick = { },
        )
    }
}
