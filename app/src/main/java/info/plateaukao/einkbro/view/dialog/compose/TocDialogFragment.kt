package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class TocDialogFragment(
    private val chapters: List<TocItem>,
    private val isEditable: Boolean,
    private val onNavigate: (Int) -> Unit,
    private val onTocChanged: (List<TocItem>) -> Unit,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                Column(
                    Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.End,
                ) {
                    var items by remember { mutableStateOf(chapters) }

                    Text(
                        text = stringResource(R.string.dialog_toc_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground,
                    )
                    HorizontalSeparator()
                    TocList(
                        modifier = Modifier
                            .weight(1F, fill = false)
                            .width(300.dp)
                            .padding(2.dp),
                        items = items,
                        isEditable = isEditable,
                        onNavigate = { index ->
                            onNavigate(items[index].originalIndex)
                            dialog?.dismiss()
                        },
                        onDelete = { index ->
                            if (items.size > 1) {
                                items = items.toMutableList().apply { removeAt(index) }
                                onTocChanged(items)
                            }
                        },
                        onItemMoved = { from, to ->
                            items = items.toMutableList().apply { add(to, removeAt(from)) }
                            onTocChanged(items)
                        },
                    )
                }
            }
        }
    }
}

data class TocItem(
    val title: String,
    val originalIndex: Int,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TocList(
    modifier: Modifier,
    items: List<TocItem>,
    isEditable: Boolean,
    onNavigate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onItemMoved: (Int, Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onItemMoved(from.index, to.index)
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        itemsIndexed(items, key = { _, item -> item.hashCode() }) { index, item ->
            if (isEditable) {
                ReorderableItem(
                    reorderableState,
                    key = item.hashCode(),
                ) { isDragging ->
                    val borderWidth = if (isDragging) 1.5.dp else (-1).dp
                    TocRow(
                        modifier = Modifier
                            .border(
                                borderWidth,
                                MaterialTheme.colors.onBackground,
                                RoundedCornerShape(3.dp)
                            )
                            .background(MaterialTheme.colors.background),
                        dragModifier = Modifier.draggableHandle(),
                        item = item,
                        isEditable = true,
                        onClick = { onNavigate(index) },
                        onDelete = { onDelete(index) },
                    )
                }
            } else {
                TocRow(
                    modifier = Modifier.background(MaterialTheme.colors.background),
                    dragModifier = Modifier,
                    item = item,
                    isEditable = false,
                    onClick = { onNavigate(index) },
                    onDelete = {},
                )
            }
        }
    }
}

@Composable
private fun TocRow(
    modifier: Modifier,
    dragModifier: Modifier,
    item: TocItem,
    isEditable: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditable) {
            Icon(
                modifier = dragModifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground,
            )
        }
        Text(
            text = item.title,
            modifier = Modifier
                .weight(1F)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 15.sp,
            color = MaterialTheme.colors.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (isEditable) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.toc_delete),
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
