package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.TabUnselected
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import java.net.URLDecoder

data class MenuItemConfig(
    val type: ContextMenuItemType,
    val titleResId: Int,
    val imageVector: ImageVector? = null,
    val iconResId: Int = 0,
    val shouldShow: (url: String, shouldShowAdBlock: Boolean, shouldShowTranslateImage: Boolean) -> Boolean = { _, _, _ -> true }
)

data class MenuLayout(
    val firstRowItems: List<MenuItemConfig>,
    val secondRowItems: List<MenuItemConfig>
)

private fun createMenuLayout(isEbookMode: Boolean = false): MenuLayout {
    val firstRowItems = listOf(
        MenuItemConfig(
            ContextMenuItemType.NewTabForeground,
            R.string.main_menu_new_tabOpen,
            Icons.Outlined.Tab
        ),
        MenuItemConfig(
            ContextMenuItemType.NewTabBackground,
            R.string.main_menu_new_tab,
            Icons.Outlined.TabUnselected
        ),
        MenuItemConfig(
            ContextMenuItemType.OpenWith,
            R.string.menu_open_with,
            Icons.Outlined.Apps
        ),
        MenuItemConfig(
            ContextMenuItemType.SplitScreen,
            R.string.split_screen,
            Icons.Outlined.ViewStream
        ),
        MenuItemConfig(
            ContextMenuItemType.ShareLink,
            R.string.menu_share_link,
            Icons.Outlined.Share
        )
    )

    val secondRowItems = listOfNotNull(
        if (isEbookMode) MenuItemConfig(
            ContextMenuItemType.GotoLink,
            R.string.go_to,
            Icons.Outlined.Fingerprint
        ) else null,
        MenuItemConfig(
            ContextMenuItemType.SelectText,
            R.string.text_select,
            Icons.AutoMirrored.Outlined.Segment
        ),
        MenuItemConfig(
            ContextMenuItemType.TranslateImage,
            R.string.translate,
            iconResId = R.drawable.ic_papago,
            shouldShow = { url, _, shouldShowTranslateImage ->
                shouldShowTranslateImage && (url.lowercase().contains("jpg") || url.lowercase().contains("png"))
            }
        ),
        MenuItemConfig(
            ContextMenuItemType.Tts,
            R.string.menu_tts,
            Icons.Outlined.RecordVoiceOver
        ),
        MenuItemConfig(
            ContextMenuItemType.SaveAs,
            R.string.menu_save_as,
            Icons.Outlined.Save
        ),
        MenuItemConfig(
            ContextMenuItemType.Summarize,
            R.string.menu_summarize,
            Icons.AutoMirrored.Outlined.Chat
        )
    )

    return MenuLayout(firstRowItems, secondRowItems)
}

class ContextMenuDialogFragment(
    private val url: String,
    private val shouldShowAdBlock: Boolean,
    private val shouldShowTranslateImage: Boolean,
    private val anchorPoint: Point,
    private val isEbookMode: Boolean = false,
    private val itemClicked: (ContextMenuItemType) -> Unit,
    private val itemLongClicked: (ContextMenuItemType) -> Unit = {},
) : ComposeDialogFragment() {

    private val hoveredItemState = mutableStateOf<ContextMenuItemType?>(null)
    var hoveredItem: ContextMenuItemType?
        get() = hoveredItemState.value
        set(value) { hoveredItemState.value = value }

    private val itemScreenBounds = mutableMapOf<ContextMenuItemType, Rect>()

    init {
        shouldShowInCenter = true
    }

    @Composable
    override fun Content() {
        ContextMenuItems(
            url,
            shouldShowAdBlock,
            shouldShowTranslateImage,
            showIcons = config.ui.showActionMenuIcons,
            isEbookMode = isEbookMode,
            hoveredItem = hoveredItemState.value,
            onItemPositioned = { type, bounds -> itemScreenBounds[type] = bounds },
            onClicked = { item ->
                dialog?.dismiss()
                itemClicked(item)
            },
            onLongClicked = { item ->
                composeView.post {
                    dialog?.dismiss()
                    itemLongClicked(item)
                }
            }
        )
    }

    fun updateHoveredItem(screenX: Float, screenY: Float) {
        if (!isAdded) return

        val position = Offset(screenX, screenY)
        hoveredItem = itemScreenBounds.entries.firstOrNull { it.value.contains(position) }?.key
    }

    fun onFingerLifted() {
        hoveredItem?.let { item ->
            dialog?.dismiss()
            itemClicked(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupDialogPosition(anchorPoint)
        return view
    }

    private fun setupDialogPosition(position: Point) {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.START)

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
private fun ContextMenuItems(
    url: String = "",
    shouldShowAdBlock: Boolean = true,
    shouldShowTranslateImage: Boolean = false,
    showIcons: Boolean = true,
    isEbookMode: Boolean = false,
    hoveredItem: ContextMenuItemType? = null,
    onItemPositioned: (ContextMenuItemType, Rect) -> Unit = { _, _ -> },
    onClicked: (ContextMenuItemType) -> Unit,
    onLongClicked: (ContextMenuItemType) -> Unit = {},
) {
    // hoveredItem changes on every touch-move during long-press drag; don't
    // rebuild the menu model (or re-decode the url) per hover change.
    val menuLayout = remember(isEbookMode) { createMenuLayout(isEbookMode) }
    val decodedUrl = remember(url) { URLDecoder.decode(url, "UTF-8") }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .width(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
        ) {
            menuLayout.firstRowItems.forEach { item ->
                ContextMenuItem(
                    titleResId = item.titleResId,
                    showIcon = showIcons,
                    imageVector = item.imageVector,
                    iconResId = item.iconResId,
                    isHovered = hoveredItem == item.type,
                    modifier = Modifier.onGloballyPositioned {
                        onItemPositioned(item.type, Rect(it.positionOnScreen(), it.size.toSize()))
                    },
                    onLongClicked = { onLongClicked(item.type) }
                ) {
                    onClicked(item.type)
                }
            }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            menuLayout.secondRowItems.filter { item ->
                item.shouldShow(url, shouldShowAdBlock, shouldShowTranslateImage)
            }.forEach { item ->
                ContextMenuItem(
                    titleResId = item.titleResId,
                    showIcon = showIcons,
                    imageVector = item.imageVector,
                    iconResId = item.iconResId,
                    isHovered = hoveredItem == item.type,
                    modifier = Modifier.onGloballyPositioned {
                        onItemPositioned(item.type, Rect(it.positionOnScreen(), it.size.toSize()))
                    },
                    onLongClicked = { onLongClicked(item.type) }
                ) {
                    onClicked(item.type)
                }
            }
        }
        HorizontalSeparator()
        Text(
            decodedUrl,
            Modifier.padding(4.dp),
            color = MaterialTheme.colors.onBackground,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
fun ContextMenuItem(
    titleResId: Int,
    showIcon: Boolean = false,
    imageVector: ImageVector? = null,
    iconResId: Int = 0,
    isHovered: Boolean = false,
    modifier: Modifier = Modifier,
    onLongClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
) {
    Box(modifier) {
        if (isHovered) {
            // Top padding keeps the dot clear of the dialog border.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .background(MaterialTheme.colors.onBackground, shape = CircleShape)
            )
        }
        MenuItem(
            titleResId = titleResId,
            iconResId = iconResId,
            imageVector = imageVector,
            isLargeType = true,
            showIcon = showIcon,
            onLongClicked = onLongClicked,
            onClicked = onClicked
        )
    }
}

enum class ContextMenuItemType {
    NewTabForeground, NewTabBackground,
    ShareLink, SelectText, OpenWith,
    SaveBookmark, SaveAs,
    SplitScreen, AdBlock, TranslateImage, Tts, Edit, Delete, Summarize, GotoLink
}

@Preview(showBackground = true)
@Composable
fun PreviewContextMenuItems() {
    MyTheme {
        ContextMenuItems("abc", showIcons = false, onClicked = { })
    }
}