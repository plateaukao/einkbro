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
                        ToolbarList(config.toolbarActions)
                        DialogButtonBar()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarList(onActions: List<ToolbarAction>) {
    val offActions = ToolbarAction.values().filterNot { onActions.contains(it) }
    val data = remember { mutableStateOf(onActions + offActions) }

    LazyColumn {
        items(data.value) { tac ->
            val isChecked = onActions.contains(tac)
            ToggleItem(
                state = isChecked,
                titleResId = tac.titleResId,
                iconResId = tac.iconResId,
                onClicked = {}
            )
        }
    }
}

@Composable
fun DialogButtonBar() {
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = {}) {
            Text("Cancel")
        }
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = {}) {
            Text("OK")
        }
    }
}

@Preview
@Composable
private fun previewToolBar() {
    AppCompatTheme {
        ToolbarList(listOf(ToolbarAction.Back, ToolbarAction.Touch, ToolbarAction.Bookmark))
    }
}

@Preview
@Composable
private fun previewBar() {
    AppCompatTheme {
        DialogButtonBar()
    }
}