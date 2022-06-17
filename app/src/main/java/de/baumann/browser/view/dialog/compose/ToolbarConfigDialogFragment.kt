package de.baumann.browser.view.dialog.compose

import android.os.Bundle
import android.view.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.view.dialog.compose.OrderDirection.*
import de.baumann.browser.view.toolbaricons.ToolbarAction
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

class ToolbarConfigDialogFragment: ComposeDialogFragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupDialog()

        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    Column(
                        Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.End,
                    ) {
                        val actionInfoList = getCurrentActionList()
                        var rememberList by remember { mutableStateOf(actionInfoList) }

                        ToolbarList(
                            Modifier
                                .weight(1F, fill = false)
                                .width(300.dp)
                                .padding(2.dp), // for round corner spaces
                            rememberList,
                            onItemClicked = { action ->
                                val actionInfos = rememberList.toMutableList()
                                val actionInfo = actionInfos.first { it.toolbarAction == action }
                                actionInfo.isOn = !actionInfo.isOn
                                if (actionInfo.isOn) {
                                    val toIndex = actionInfos.indexOfFirst { !it.isOn }
                                    val fromIndex = actionInfos.indexOf(actionInfo)
                                    if (toIndex != -1 && toIndex < fromIndex)
                                        actionInfos.apply { add(toIndex, removeAt(fromIndex)) }
                                } else {
                                    val toIndex = actionInfos.indexOfLast { it.isOn }
                                    val fromIndex = actionInfos.indexOf(actionInfo)
                                    if (toIndex != -1 && toIndex > fromIndex)
                                        actionInfos.apply { add(toIndex, removeAt(fromIndex)) }
                                }
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

    private fun getCurrentActionList(): List<ToolbarActionItemInfo> =
        config.toolbarActions.map { ToolbarActionItemInfo(it, true) } +
        ToolbarAction.values().filterNot { config.toolbarActions.contains(it) }.map { ToolbarActionItemInfo(it, false) }
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
                    ToolbarToggleItem(
                        info = info,
                        onItemClicked = onItemClicked,
                        onDirectionClicked = { direction ->
                            val index = infos.indexOf(info)
                            when {
                                direction == Up && index != 0 ->
                                    onItemMoved(index, index -1 )
                                direction == Down && index < infos.size -1 ->
                                    onItemMoved(index, index + 1 )
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class OrderDirection { Up, Down}

@Composable
fun ToolbarToggleItem(
    info: ToolbarActionItemInfo,
    onItemClicked: (ToolbarAction)->Unit,
    onDirectionClicked: (OrderDirection)-> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shouldEnableCheckClick = info.toolbarAction != ToolbarAction.Settings
        Surface(modifier = Modifier.weight(1F)){
            ToggleItem(
                state = info.isOn,
                titleResId = info.toolbarAction.titleResId,
                iconResId = info.toolbarAction.iconResId,
                isEnabled = shouldEnableCheckClick,
                // settings should not be clickable, and always there
                onClicked = { if (info.toolbarAction != ToolbarAction.Settings) onItemClicked(info.toolbarAction) }
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.icon_arrow_up_gest), contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onDirectionClicked(Up) },
            tint = MaterialTheme.colors.onBackground
        )
        Icon(
            painter = painterResource(id = R.drawable.icon_arrow_down_gest), contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight()
                .clickable { onDirectionClicked(Down) },
            tint = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
fun DialogButtonBar(dismissAction: ()->Unit, okAction: ()->Unit) {
    Row(modifier = Modifier
        .width(IntrinsicSize.Max)
        .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier.wrapContentWidth(),
            onClick = dismissAction) {
            Text(stringResource(id = android.R.string.cancel), color = MaterialTheme.colors.onBackground)
        }
        VerticalSeparator()
        TextButton(
            modifier = Modifier.wrapContentWidth(),
            onClick = { dismissAction(); okAction() }) {
            Text(stringResource(id = android.R.string.ok), color = MaterialTheme.colors.onBackground)
        }
    }
}

class ToolbarActionItemInfo(val toolbarAction: ToolbarAction, var isOn: Boolean)

@Preview
@Composable
private fun previewToolBar() {
    AppCompatTheme {
        Column (horizontalAlignment = Alignment.End){
            ToolbarList(
                Modifier
                    .heightIn(100.dp, 500.dp)
                    .width(300.dp)
                    .padding(2.dp), // for round corner spaces
                ToolbarAction.values().map { ToolbarActionItemInfo(it, true) },
                {},
                {_, _-> }
            )
            DialogButtonBar({}, {})
        }
    }
}