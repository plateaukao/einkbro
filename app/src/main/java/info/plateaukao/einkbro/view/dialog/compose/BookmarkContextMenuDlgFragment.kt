package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.TabUnselected
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark

class BookmarkContextMenuDlgFragment(
    private val bookmark: Bookmark,
    private val allowEdit: Boolean = true,
    private val anchorPoint: Point? = null,
    private val onClicked: (ContextMenuItemType) -> Unit,
) : ComposeDialogFragment() {

    private val hoveredItemState = mutableStateOf<ContextMenuItemType?>(null)

    // Kept as coordinates and resolved to screen rects per hit-test: the dialog
    // window can settle after layout, so rects captured at layout time go stale.
    private val itemCoordinates = mutableMapOf<ContextMenuItemType, LayoutCoordinates>()

    override fun adjustHorizontalPosition() {
        // Uses own anchor-point positioning
    }

    fun updateHoveredItem(screenX: Float, screenY: Float) {
        if (!isAdded) return
        if (isFingerAnchorPending) {
            hoveredItemState.value = null
            return
        }

        val position = Offset(screenX, screenY)
        hoveredItemState.value = itemCoordinates.entries.firstOrNull { (_, coordinates) ->
            coordinates.isAttached &&
                Rect(coordinates.positionOnScreen(), coordinates.size.toSize()).contains(position)
        }?.key
    }

    fun onFingerLifted() {
        if (!isAdded) return

        hoveredItemState.value?.let { item ->
            dismiss()
            onClicked(item)
        }
    }

    @Composable
    override fun Content() {
        BookmarkContextMenuScreen(
            bookmark = bookmark,
            allowEdit = allowEdit,
            hoveredItem = hoveredItemState.value,
            onItemPositioned = { type, coordinates -> itemCoordinates[type] = coordinates },
            onClicked = { onClicked(it); dismiss() })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (anchorPoint?.isValidAnchor() == true) prepareFingerAnchor()
        return view
    }

    override fun onStart() {
        super.onStart()
        anchorPoint?.takeIf { it.isValidAnchor() }?.let { positionAboveFinger(it) }
    }
}

@Composable
fun BookmarkContextMenuScreen(
    bookmark: Bookmark,
    allowEdit: Boolean = true,
    hoveredItem: ContextMenuItemType? = null,
    onItemPositioned: (ContextMenuItemType, LayoutCoordinates) -> Unit = { _, _ -> },
    onClicked: (ContextMenuItemType) -> Unit,
) {
    val items = buildList {
        if (!bookmark.isDirectory) {
            add(Triple(ContextMenuItemType.NewTabForeground, R.string.main_menu_new_tabOpen, Icons.Outlined.Tab))
            add(Triple(ContextMenuItemType.NewTabBackground, R.string.main_menu_new_tab, Icons.Outlined.TabUnselected))
            add(Triple(ContextMenuItemType.SplitScreen, R.string.split_screen, Icons.Outlined.ViewStream))
        }
        if (allowEdit) {
            add(Triple(ContextMenuItemType.Edit, R.string.menu_edit, Icons.Outlined.Edit))
        }
        if (!bookmark.isDirectory) {
            add(Triple(ContextMenuItemType.RefreshIcon, R.string.menu_refresh_icon, Icons.Outlined.Refresh))
        }
        add(Triple(ContextMenuItemType.Delete, R.string.menu_delete, Icons.Outlined.Delete))
    }
    // The dialog window caps the row's width; cells shrink evenly to fit it so
    // the last actions never end up cut off at the edge.
    BoxWithConstraints {
        val cellWidth = fittedMenuItemWidth(items.size, maxWidth)
        Row(modifier = Modifier.wrapContentHeight()) {
            items.forEach { (type, titleResId, icon) ->
                BookmarkMenuItem(type, titleResId, icon, cellWidth, hoveredItem, onItemPositioned, onClicked)
            }
        }
    }
}

@Composable
private fun BookmarkMenuItem(
    type: ContextMenuItemType,
    titleResId: Int,
    imageVector: ImageVector,
    cellWidth: Dp,
    hoveredItem: ContextMenuItemType?,
    onPositioned: (ContextMenuItemType, LayoutCoordinates) -> Unit,
    onClicked: (ContextMenuItemType) -> Unit,
) {
    ContextMenuItem(
        titleResId = titleResId,
        showIcon = true,
        imageVector = imageVector,
        isHovered = hoveredItem == type,
        modifier = Modifier.onGloballyPositioned { onPositioned(type, it) },
        cellWidth = cellWidth,
    ) { onClicked(type) }
}
