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
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.compose.ReorderableComposedIconBar

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
                                Text(text = stringResource(id = R.string.toolbars))
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
        } else {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarConfigPanel(list: MutableState<List<ToolbarActionInfo>>) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 1.dp, end = 1.dp, bottom = 50.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
                text = "Available Actions",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6
            )
            Text(
                modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                text = "click icon to add it to the toolbar",
                color = MaterialTheme.colors.onBackground,
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
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            modifier = Modifier.padding(10.dp),
            text = "Preview",
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 20.dp),
            text = "click icon to remove it from the toolbar; long click to drag icon to reorder",
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption
        )
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
                onClick = { action ->
                    if (action != ToolbarAction.Settings) {
                        list.value = list.value.toMutableList().apply {
                            val info = find { it.toolbarAction == action }
                            remove(info)
                        }
                    }
                },
            )
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
