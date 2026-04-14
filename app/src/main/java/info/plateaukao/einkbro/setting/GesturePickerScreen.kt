package info.plateaukao.einkbro.setting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.browser.BrowserActionCatalog
import info.plateaukao.einkbro.browser.BrowserActionEntry
import kotlin.reflect.KMutableProperty0

/**
 * Shared holder for the gesture slot currently being edited.
 * Set when a GestureActionSettingItem is tapped; consumed by GesturePickerScreen.
 */
object GesturePickerState {
    var editingSlot: GestureActionSettingItem? = null
}

@Composable
fun GesturePickerScreen(navController: NavHostController) {
    val slot = GesturePickerState.editingSlot ?: run {
        // Nothing to edit — pop back.
        navController.popBackStack()
        return
    }
    val property: KMutableProperty0<BrowserAction> = slot.config
    var selected by remember { mutableStateOf(property.get()) }
    val selectedId = BrowserActionCatalog.idOf(selected)

    val expanded: SnapshotStateMap<Int, Boolean> = remember { mutableStateMapOf() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item { SelectedHeader(slot.titleResId, selected) }

        // "Nothing" option shown at the top for easy clearing.
        item {
            ActionRow(
                entry = BrowserActionCatalog.nothingEntry,
                isSelected = selectedId == BrowserActionCatalog.nothingEntry.id,
                onClick = {
                    property.set(BrowserAction.Noop)
                    selected = BrowserAction.Noop
                    navController.popBackStack()
                },
            )
        }

        BrowserActionCatalog.categories.forEach { category ->
            val isOpen = expanded[category.titleResId] ?: false
            item {
                CategoryHeader(
                    titleResId = category.titleResId,
                    expanded = isOpen,
                    onClick = { expanded[category.titleResId] = !isOpen },
                )
            }
            if (isOpen) {
                items(category.entries, key = { it.id }) { entry ->
                    ActionRow(
                        entry = entry,
                        isSelected = entry.id == selectedId,
                        onClick = {
                            property.set(entry.action)
                            selected = entry.action
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedHeader(slotTitleResId: Int, action: BrowserAction) {
    val entry = BrowserActionCatalog.entryOf(action)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = slotTitleResId),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            )
            Text(
                text = stringResource(id = R.string.action_selected_label) +
                    ": " + stringResource(id = entry.labelResId),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CategoryHeader(titleResId: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                MaterialTheme.colors.onBackground.copy(alpha = 0.04f),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = titleResId),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
    }
}

@Composable
private fun ActionRow(
    entry: BrowserActionEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.15f)
                else MaterialTheme.colors.surface,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(id = entry.labelResId),
            style = MaterialTheme.typography.body1,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
