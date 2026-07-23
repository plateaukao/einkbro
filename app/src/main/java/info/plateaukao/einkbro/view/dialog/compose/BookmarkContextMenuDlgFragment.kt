package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
        anchorPoint?.let { setupDialogPosition(it) }
        return view
    }

    private fun setupDialogPosition(position: Point) {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.LEFT)

        if (position.isValid()) {
            val params = window.attributes.apply {
                x = position.x
                y = position.y
            }
            window.attributes = params
        }
    }

    private fun Point.isValid() = x != 0 && y != 0
}

@Composable
fun BookmarkContextMenuScreen(
    bookmark: Bookmark,
    allowEdit: Boolean = true,
    hoveredItem: ContextMenuItemType? = null,
    onItemPositioned: (ContextMenuItemType, LayoutCoordinates) -> Unit = { _, _ -> },
    onClicked: (ContextMenuItemType) -> Unit,
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .width(IntrinsicSize.Max)
            .horizontalScroll(rememberScrollState()),
    ) {
        if (!bookmark.isDirectory) {
            BookmarkMenuItem(
                ContextMenuItemType.NewTabForeground, R.string.main_menu_new_tabOpen,
                Icons.Outlined.Tab, hoveredItem, onItemPositioned, onClicked
            )
            BookmarkMenuItem(
                ContextMenuItemType.NewTabBackground, R.string.main_menu_new_tab,
                Icons.Outlined.TabUnselected, hoveredItem, onItemPositioned, onClicked
            )
            BookmarkMenuItem(
                ContextMenuItemType.SplitScreen, R.string.split_screen,
                Icons.Outlined.ViewStream, hoveredItem, onItemPositioned, onClicked
            )
        }
        if (allowEdit) {
            BookmarkMenuItem(
                ContextMenuItemType.Edit, R.string.menu_edit,
                Icons.Outlined.Edit, hoveredItem, onItemPositioned, onClicked
            )
        }
        BookmarkMenuItem(
            ContextMenuItemType.Delete, R.string.menu_delete,
            Icons.Outlined.Delete, hoveredItem, onItemPositioned, onClicked
        )
    }
}

@Composable
private fun BookmarkMenuItem(
    type: ContextMenuItemType,
    titleResId: Int,
    imageVector: ImageVector,
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
    ) { onClicked(type) }
}
