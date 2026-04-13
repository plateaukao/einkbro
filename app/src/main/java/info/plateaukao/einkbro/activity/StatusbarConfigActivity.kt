package info.plateaukao.einkbro.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.statusbar.StatusbarItem
import org.koin.android.ext.android.inject
import sh.calvin.reorderable.ReorderableRow

class StatusbarConfigActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initial = config.ui.statusbarItems
        setContent {
            MyTheme {
                val list = remember { mutableStateOf(initial) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = stringResource(id = R.string.setting_title_statusbar_items)) },
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
                                    config.ui.statusbarItems = list.value
                                    finish()
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = null)
                                }
                            },
                        )
                    },
                    content = { _ -> StatusbarConfigPanel(list) }
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        if (config.uiLocaleLanguage.isNotEmpty()) {
            super.attachBaseContext(LocaleManager.setLocale(newBase, config.uiLocaleLanguage))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusbarConfigPanel(list: MutableState<List<StatusbarItem>>) {
    val onRemove: (StatusbarItem) -> Unit = { item ->
        list.value = list.value.toMutableList().apply { remove(item) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 1.dp, end = 1.dp, bottom = 50.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
                text = stringResource(id = R.string.statusbar_config_available),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6,
            )
            Text(
                modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                text = stringResource(id = R.string.statusbar_config_available_hint),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.caption,
            )
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Adaptive(84.dp),
            ) {
                val selected = list.value
                val remaining = StatusbarItem.entries.filter { it !in selected }
                itemsIndexed(remaining) { _, item ->
                    AvailableItem(item) {
                        list.value = list.value.toMutableList().apply { add(item) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
            text = stringResource(id = R.string.statusbar_config_preview),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
            text = stringResource(id = R.string.statusbar_config_preview_hint),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colors.onBackground),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ReorderableStatusbarRow(list = list, onClick = onRemove)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableStatusbarRow(
    list: MutableState<List<StatusbarItem>>,
    onClick: (StatusbarItem) -> Unit,
) {
    ReorderableRow(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background),
        list = list.value,
        onSettle = { from, to ->
            list.value = list.value.toMutableList().apply { add(to, removeAt(from)) }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) { _, item, isDragging ->
        key(item) {
            Box(
                modifier = Modifier
                    .longPressDraggableHandle()
                    .border(
                        if (isDragging) 1.5.dp else (-1).dp,
                        MaterialTheme.colors.onBackground,
                        RoundedCornerShape(3.dp),
                    )
                    .clickable { onClick(item) }
                    .padding(horizontal = 6.dp),
            ) {
                StatusbarPreviewIcon(item)
            }
        }
    }
}

@Composable
private fun AvailableItem(item: StatusbarItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
    ) {
        StatusbarPreviewIcon(
            item = item,
            modifier = Modifier
                .size(48.dp)
                .padding(horizontal = 6.dp)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = item.titleResId),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Composable
private fun StatusbarPreviewIcon(
    item: StatusbarItem,
    modifier: Modifier = Modifier.size(24.dp),
) {
    val vector: ImageVector = item.previewIcon
        ?: ImageVector.vectorResource(id = item.previewIconResId)
    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground,
        modifier = modifier,
    )
}
