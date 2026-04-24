package info.plateaukao.einkbro.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.LocaleManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.LocalMenuActions
import info.plateaukao.einkbro.view.dialog.compose.LocalMenuHideConfig
import info.plateaukao.einkbro.view.dialog.compose.MENU_GRID_COLUMNS
import info.plateaukao.einkbro.view.dialog.compose.MenuActions
import info.plateaukao.einkbro.view.dialog.compose.MenuEntry
import info.plateaukao.einkbro.view.dialog.compose.MenuHideConfig
import info.plateaukao.einkbro.view.dialog.compose.MenuItemForType
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType
import info.plateaukao.einkbro.view.dialog.compose.effectiveMenuEntries
import info.plateaukao.einkbro.view.dialog.compose.encodeMenuEntries
import info.plateaukao.einkbro.view.dialog.compose.menuDisplayEntries
import info.plateaukao.einkbro.view.dialog.compose.menuDisplayToUnderlying
import org.koin.android.ext.android.inject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

class MenuItemHideActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyTheme {
                var hidden by remember {
                    mutableStateOf(
                        config.ui.hiddenMenuItems.mapNotNull { name ->
                            runCatching { MenuItemType.valueOf(name) }.getOrNull()
                        }.toSet()
                    )
                }
                // We keep the *display* list as the source of truth during a session so the
                // user's drags don't produce micro-jitter from re-normalisation; on every
                // change we extract the underlying order to persist.
                var display by remember {
                    mutableStateOf(
                        menuDisplayEntries(effectiveMenuEntries(config.ui.menuItemOrder))
                    )
                }
                var reorderMode by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.setting_title_hide_menu_items)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { reorderMode = !reorderMode }) {
                                    Icon(
                                        imageVector = if (reorderMode) Icons.Filled.Check else Icons.Outlined.SwapHoriz,
                                        contentDescription = stringResource(R.string.menu_reorder),
                                    )
                                }
                            },
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (reorderMode) R.string.menu_hide_hint_reorder
                                else R.string.menu_hide_hint_tap
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colors.onBackground,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        CompositionLocalProvider(
                            LocalMenuHideConfig provides MenuHideConfig(
                                hideMode = !reorderMode,
                                reorderMode = reorderMode,
                                hiddenItems = hidden,
                                onToggleHide = { type ->
                                    hidden = if (type in hidden) hidden - type else hidden + type
                                    config.ui.hiddenMenuItems = hidden.map { it.name }.toSet()
                                },
                            ),
                            LocalMenuActions provides MenuActions(),
                        ) {
                            ReorderableMenuGrid(
                                display = display,
                                reorderMode = reorderMode,
                                onReorder = { newDisplay ->
                                    display = newDisplay
                                    config.ui.menuItemOrder = encodeMenuEntries(
                                        menuDisplayToUnderlying(newDisplay)
                                    )
                                },
                            )
                        }
                    }
                }
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
@androidx.compose.runtime.Composable
private fun ReorderableMenuGrid(
    display: List<MenuEntry>,
    reorderMode: Boolean,
    onReorder: (List<MenuEntry>) -> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        val fromEntry = display.getOrNull(from.index) ?: return@rememberReorderableLazyGridState
        if (fromEntry !is MenuEntry.Item) return@rememberReorderableLazyGridState
        val newList = display.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onReorder(newList)
    }
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(MENU_GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
    ) {
        display.forEachIndexed { index, entry ->
            when (entry) {
                is MenuEntry.Item -> item(key = "item_${entry.type.name}") {
                    ReorderableItem(reorderableState, key = "item_${entry.type.name}") { _ ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .then(if (reorderMode) Modifier.longPressDraggableHandle() else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            MenuItemForType(type = entry.type)
                        }
                    }
                }
                is MenuEntry.Boundary -> item(
                    key = "boundary_${entry.sectionStart.name}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    ReorderableItem(reorderableState, key = "boundary_${entry.sectionStart.name}") { _ ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            Divider(color = MaterialTheme.colors.onBackground, thickness = 1.dp)
                            entry.sectionStart.headerRes?.let { res ->
                                Text(
                                    text = stringResource(res),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colors.onBackground,
                                )
                            }
                        }
                    }
                }
                // Display-only empty cell. Must be a ReorderableItem so drags can cross it.
                is MenuEntry.Spacer -> item(key = "spacer_$index") {
                    ReorderableItem(reorderableState, key = "spacer_$index") { _ ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .fillMaxWidth(),
                        ) {}
                    }
                }
            }
        }
    }
}
