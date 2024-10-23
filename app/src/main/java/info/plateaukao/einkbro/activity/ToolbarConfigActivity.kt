package info.plateaukao.einkbro.activity

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

class ToolbarConfigActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val iconEnums = config.toolbarActions
        val toolbarActionInfoList = iconEnums.toToolbarActionInfoList()

        setContent {
            MyTheme {
                var list = remember { mutableStateOf(toolbarActionInfoList) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = "123")
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
                                    config.toolbarActions = list.value.map { it.toolbarAction }
                                    finish()
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = null)
                                }
                            },
                        )
                    },
                    content = { padding ->
                        ToolbarConfigPanel(list)
                    }
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarConfigPanel(list: MutableState<List<ToolbarActionInfo>>) {
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        list.value = list.value.toMutableList().apply {
            val item = removeAt(from.index)
            add(to.index, item)
        }
    }

    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = "Preview",
            style = MaterialTheme.typography.h6
        )
        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = "no interaction here",
            style = MaterialTheme.typography.caption
        )
        Box(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colors.onBackground),
            contentAlignment = Alignment.CenterEnd
        ) {
            ComposedIconBar(
                toolbarActionInfos = list.value,
                title = "Toolbar Configuration",
                tabCount = "7",
                pageInfo = "1",
                isIncognito = false,
                onClick = {},
                onLongClick = {},
            )
        }
        Text(
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
            text = "Added Actions",
            style = MaterialTheme.typography.h6
        )
        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = "click to remove; drag to reorder",
            style = MaterialTheme.typography.caption
        )
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth(),
            state = lazyGridState,
            columns = GridCells.Adaptive(64.dp),
        ) {
            val actionList = list.value.map { it.toolbarAction }
            itemsIndexed(actionList, key = { _, item -> item.ordinal }) { index, action ->
                ReorderableItem(reorderableLazyGridState, key = action.ordinal) { isDragging ->
                    val borderWidth = if (isDragging) 1.5.dp else (-1).dp
                    Box(
                        modifier = Modifier
                            .border(
                                borderWidth,
                                MaterialTheme.colors.onBackground,
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 6.dp)
                            .longPressDraggableHandle()
                            .clickable {
                                if (action == ToolbarAction.Settings) {
                                    // do nothing
                                    return@clickable
                                }

                                list.value = actionList.toMutableList().apply {
                                    remove(action)
                                }.toToolbarActionInfoList()
                            },
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(64.dp)
                                .padding(horizontal = 6.dp),
                            imageVector = action.imageVector
                                ?: ImageVector.vectorResource(id = action.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        }
        Text(
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
            text = "Other Actions",
            style = MaterialTheme.typography.h6
        )
        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = "click to add",
            style = MaterialTheme.typography.caption
        )
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth(),
            columns = GridCells.Adaptive(84.dp),
        ) {
            val selectedActions = list.value.map { it.toolbarAction }
            val otherActionInfos = ToolbarAction.entries
                .filter { it !in selectedActions }
                .toToolbarActionInfoList()
            itemsIndexed(otherActionInfos) { index, info ->
                Column(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .clickable {
                            list.value = list.value.toMutableList().apply {
                                add(info)
                            }
                        },
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
        }
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
