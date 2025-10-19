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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.TabUnselected
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

private fun createMenuLayout(): MenuLayout {
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

    val secondRowItems = listOf(
        MenuItemConfig(
            ContextMenuItemType.CopyLink,
            R.string.copy_link,
            Icons.Outlined.CopyAll
        ),
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
    private val itemClicked: (ContextMenuItemType) -> Unit,
) : ComposeDialogFragment() {

    private val hoveredItemState = mutableStateOf<ContextMenuItemType?>(null)
    var hoveredItem: ContextMenuItemType?
        get() = hoveredItemState.value
        set(value) { hoveredItemState.value = value }

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            ContextMenuItems(
                url,
                shouldShowAdBlock,
                shouldShowTranslateImage,
                showIcons = config.showActionMenuIcons,
                hoveredItem = hoveredItemState.value
            ) { item ->
                dialog?.dismiss()
                itemClicked(item)
            }
        }
    }

    fun updateHoveredItem(screenX: Float, screenY: Float) {
        // Convert screen coordinates to dialog coordinates and determine hovered item
        val dialogLocation = IntArray(2)
        composeView.getLocationOnScreen(dialogLocation)

        val dialogX = screenX - dialogLocation[0]
        val dialogY = screenY - dialogLocation[1]

        // Simple hit testing - this is a basic implementation
        // You might need to adjust based on your exact layout
        hoveredItem = determineHoveredItem(dialogX, dialogY)
    }

    fun onFingerLifted() {
        hoveredItem?.let { item ->
            dialog?.dismiss()
            itemClicked(item)
        }
    }

    private fun determineHoveredItem(x: Float, y: Float): ContextMenuItemType? {
        val menuLayout = createMenuLayout()

        // Calculate precise dimensions based on MenuItem logic
        val screenWidthDp = resources.configuration.screenWidthDp
        val isLargeType = true // ContextMenuItem uses isLargeType = true
        val showIcon = config.showActionMenuIcons

        // Calculate item width (same logic as MenuItem)
        val itemWidthDp = when {
            isLargeType -> if (screenWidthDp > 500) 62 else 50
            screenWidthDp > 500 -> 55
            else -> 45
        }

        // Calculate item height
        val itemHeightDp = if (!showIcon) 50 else if (isLargeType) 80 else 70

        // Convert dp to pixels (approximate density)
        val density = resources.displayMetrics.density
        val itemWidthPx = itemWidthDp * density
        val itemHeightPx = itemHeightDp * density

        // Layout structure:
        // 1. URL text with 4dp padding = ~8dp height + text height (~20dp) = ~28dp
        // 2. HorizontalSeparator = 1dp
        // 3. First Row = itemHeightPx
        // 4. HorizontalSeparator = 1dp
        // 5. Second Row = itemHeightPx

        //val urlTextHeight = 30 * density // URL text area
        val separatorHeight = 1 * density // HorizontalSeparator

        val firstRowStart = 0F // urlTextHeight + separatorHeight
        val firstRowEnd = firstRowStart + itemHeightPx
        val secondRowStart = firstRowEnd + separatorHeight
        val secondRowEnd = secondRowStart + itemHeightPx

        when {
            y < firstRowStart -> return null // URL text area
            y in firstRowStart..firstRowEnd -> {
                // First row items
                val itemIndex = (x / itemWidthPx).toInt()
                return menuLayout.firstRowItems.getOrNull(itemIndex)?.type
            }
            y in secondRowStart..secondRowEnd -> {
                // Second row items (filter based on conditions)
                val visibleSecondRowItems = menuLayout.secondRowItems.filter { item ->
                    item.shouldShow(url, shouldShowAdBlock, shouldShowTranslateImage)
                }
                val itemIndex = (x / itemWidthPx).toInt()
                return visibleSecondRowItems.getOrNull(itemIndex)?.type
            }
            else -> return null
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
    hoveredItem: ContextMenuItemType? = null,
    onClicked: (ContextMenuItemType) -> Unit,
) {
    val menuLayout = createMenuLayout()

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
                    isHovered = hoveredItem == item.type
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
                    isHovered = hoveredItem == item.type
                ) {
                    onClicked(item.type)
                }
            }
        }
        HorizontalSeparator()
        Text(
            URLDecoder.decode(url, "UTF-8"),
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
    onClicked: () -> Unit = {},
) {
    Box {
        if (isHovered) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colors.onBackground, shape = CircleShape)
                    .align(Alignment.TopCenter)
            )
        }
        MenuItem(
            titleResId = titleResId,
            iconResId = iconResId,
            imageVector = imageVector,
            isLargeType = true,
            showIcon = showIcon,
            onClicked = onClicked
        )
    }
}

enum class ContextMenuItemType {
    NewTabForeground, NewTabBackground,
    ShareLink, CopyLink, SelectText, OpenWith,
    SaveBookmark, SaveAs,
    SplitScreen, AdBlock, TranslateImage, Tts, Edit, Delete, Summarize
}

@Preview(showBackground = true)
@Composable
fun PreviewContextMenuItems() {
    MyTheme {
        ContextMenuItems("abc", showIcons = false) { }
    }
}