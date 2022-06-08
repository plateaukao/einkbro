package de.baumann.browser.view.dialog.compose

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.toolbaricons.ToolbarAction
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarConfigDialogFragment(
    val extraAction: () -> Unit
): ComposeDialogFragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupDialog()

        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    Column {
                        val actionInfoList = ToolbarAction.values().map { ToolbarActionItemInfo(it, config.toolbarActions.contains(it))}.sortedBy { !it.isOn }
                        var rememberList by remember { mutableStateOf(actionInfoList) }
                        ToolbarList(Modifier.weight(1f), rememberList) { action ->
                            val actionInfos = rememberList.toMutableList()
                            val actionInfo = actionInfos.first { it.toolbarAction == action }
                            actionInfo.isOn = !actionInfo.isOn
                            rememberList = actionInfos
                        }
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
    toolbarActionItemInfos: List<ToolbarActionItemInfo>,
    onClicked: (ToolbarAction)->Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(toolbarActionItemInfos) { info ->
            ToggleItem(
                state = info.isOn,
                titleResId = info.toolbarAction.titleResId,
                iconResId = info.toolbarAction.iconResId,
                onClicked = { onClicked(info.toolbarAction) }
            )
        }
    }
}

@Composable
fun DialogButtonBar(dismissAction: ()->Unit, okAction: ()->Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = dismissAction) {
            Text("Cancel", color = MaterialTheme.colors.onBackground)
        }
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
                {}
            )
            DialogButtonBar({}, {})
        }
    }
}