package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class ToolbarConfigDialogFragment : ComposeDialogFragment() {
    override fun setupComposeView() {
        val actionInfoList = getCurrentActionList()
        composeView.setContent {
            MyTheme {
                Column(
                    Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.End,
                ) {
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
                            rememberList =
                                rememberList.toMutableList().apply { add(to, removeAt(from)) }
                        }
                    )
                    HorizontalSeparator()
                    DialogButtonBar(
                        dismissAction = { dialog?.dismiss() },
                        okAction = {
                            config.toolbarActions =
                                rememberList.filter { it.isOn }.map { it.toolbarAction }
                        }
                    )
                }
            }
        }
    }

    private fun getCurrentActionList(): List<ToolbarActionItemInfo> =
        config.toolbarActions.map { ToolbarActionItemInfo(it, true) } +
                ToolbarAction.entries
                    // need to filter only addable actions here
                    .filter { it.isAddable }
                    .filterNot { config.toolbarActions.contains(it) }
                    // hide papago action if papago api key is not set
                    .filterNot { config.papagoApiSecret.isBlank() && it == ToolbarAction.PapagoByParagraph }
                    .map { ToolbarActionItemInfo(it, false) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarList(
    modifier: Modifier,
    infos: List<ToolbarActionItemInfo>,
    onItemClicked: (ToolbarAction) -> Unit,
    onItemMoved: (Int, Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val state = rememberReorderableLazyListState(lazyListState) { from, to ->
        onItemMoved(from.index, to.index)
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        items(infos, { it.hashCode() }) { info ->
            ReorderableItem(
                state,
                key = info.hashCode(),
            ) { isDragging ->
                val borderWidth = if (isDragging) 1.5.dp else -1.dp
                Column(
                    modifier = Modifier
                        .border(
                            borderWidth,
                            MaterialTheme.colors.onBackground,
                            RoundedCornerShape(3.dp)
                        )
                        .background(MaterialTheme.colors.background)
                ) {
                    ToolbarToggleItem(
                        modifier = Modifier.draggableHandle(),
                        info = info,
                        onItemClicked = onItemClicked,
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarToggleItem(
    modifier: Modifier,
    info: ToolbarActionItemInfo,
    onItemClicked: (ToolbarAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shouldEnableCheckClick = info.toolbarAction != ToolbarAction.Settings
        Surface(modifier = Modifier.weight(1F), color = MaterialTheme.colors.background) {
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
            modifier = modifier.padding(4.dp),
            painter = painterResource(id = R.drawable.ic_drag), contentDescription = null,
            tint = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
fun DialogButtonBar(
    okResId: Int = android.R.string.ok,
    dismissAction: () -> Unit,
    okAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier.wrapContentWidth(),
            onClick = dismissAction
        ) {
            Text(
                stringResource(id = android.R.string.cancel),
                color = MaterialTheme.colors.onBackground
            )
        }
        VerticalSeparator()
        TextButton(
            modifier = Modifier.wrapContentWidth(),
            onClick = { dismissAction(); okAction() }) {
            Text(
                stringResource(okResId),
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

class ToolbarActionItemInfo(val toolbarAction: ToolbarAction, var isOn: Boolean)

@Preview
@Composable
private fun previewToolBar() {
    MyTheme {
        Column(horizontalAlignment = Alignment.End) {
            ToolbarList(
                Modifier
                    .heightIn(100.dp, 500.dp)
                    .width(300.dp)
                    .padding(2.dp), // for round corner spaces
                ToolbarAction.values().map { ToolbarActionItemInfo(it, true) },
                {},
                { _, _ -> }
            )
            DialogButtonBar(0, {}, {})
        }
    }
}