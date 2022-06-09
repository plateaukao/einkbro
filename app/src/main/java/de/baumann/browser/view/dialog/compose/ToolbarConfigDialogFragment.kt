package de.baumann.browser.view.dialog.compose

import android.os.Bundle
import android.view.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.view.toolbaricons.ToolbarAction
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.intellij.lang.annotations.JdkConstants

class ToolbarConfigDialogFragment(): ComposeDialogFragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupDialog()

        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    Column {
                        val actionInfoList = ToolbarAction.values().map { ToolbarActionItemInfo(it, config.toolbarActions.contains(it))}.sortedBy { !it.isOn }
                        var rememberList by remember { mutableStateOf(actionInfoList) }

                        ToolbarList(
                            Modifier
                                .weight(1f)
                                .padding(2.dp), // for round corner spaces
                            rememberList,
                            onItemClicked = { action ->
                                val actionInfos = rememberList.toMutableList()
                                val actionInfo = actionInfos.first { it.toolbarAction == action }
                                actionInfo.isOn = !actionInfo.isOn
                                rememberList = actionInfos
                            },
                            onItemMoved = { from, to ->
                                rememberList = rememberList.toMutableList().apply { add(to, removeAt(from)) }
                            }
                        )
                        HorizontalSeparator()
                        DialogButtonBar(
                            dismissAction = { dialog?.dismiss() },
                            okAction = {
                                config.toolbarActions = rememberList.filter { it.isOn }.map { it.toolbarAction }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarList(
    modifier: Modifier,
    infos: List<ToolbarActionItemInfo>,
    onItemClicked: (ToolbarAction)->Unit,
    onItemMoved: (Int, Int)->Unit
) {
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        onItemMoved(from.index, to.index)
    })

    LazyColumn(
        state = state.listState,
        modifier = modifier
            .reorderable(state)
            .detectReorderAfterLongPress(state)
    ) {
        items(infos, { it.hashCode() }) { info ->
            ReorderableItem(state, key = info.hashCode()) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                Column(
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colors.surface)
                ) {
                    ToolbarToggleItem(info = info, onClicked = onItemClicked)
                }
            }
        }
    }
}

@Composable
fun ToolbarToggleItem(info: ToolbarActionItemInfo, onClicked: (ToolbarAction)->Unit) {
    ToggleItem(
        state = info.isOn,
        titleResId = info.toolbarAction.titleResId,
        iconResId = info.toolbarAction.iconResId,
        onClicked = { onClicked(info.toolbarAction) }
    )
}

@Composable
fun DialogButtonBar(dismissAction: ()->Unit, okAction: ()->Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = dismissAction) {
            Text("Cancel", color = MaterialTheme.colors.onBackground)
        }
        VerticalSeparator()
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = { dismissAction(); okAction() }) {
            Text("OK", color = MaterialTheme.colors.onBackground)
        }
    }
}

class ToolbarActionItemInfo(val toolbarAction: ToolbarAction, var isOn: Boolean)

@Preview
@Composable
private fun previewToolBar() {
    AppCompatTheme {
        Column {
            ToolbarList(
                Modifier.weight(1f),
                ToolbarAction.values().map { ToolbarActionItemInfo(it, true) },
                {},
                {_, _-> }
            )
            DialogButtonBar({}, {})
        }
    }
}