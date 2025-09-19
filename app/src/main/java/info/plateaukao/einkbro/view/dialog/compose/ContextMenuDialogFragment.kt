package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.CopyLink
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.SaveAs
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.SelectText
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.Summarize
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.TranslateImage
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.Tts
import java.net.URLDecoder


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

        val urlTextHeight = 30 * density // URL text area
        val separatorHeight = 1 * density // HorizontalSeparator

        val firstRowStart = urlTextHeight + separatorHeight
        val firstRowEnd = firstRowStart + itemHeightPx
        val secondRowStart = firstRowEnd + separatorHeight
        val secondRowEnd = secondRowStart + itemHeightPx

        when {
            y < firstRowStart -> return null // URL text area
            y in firstRowStart..firstRowEnd -> {
                // First row items (fixed 5 items)
                val itemIndex = (x / itemWidthPx).toInt()
                return when (itemIndex) {
                    0 -> ContextMenuItemType.NewTabForeground
                    1 -> ContextMenuItemType.NewTabBackground
                    2 -> ContextMenuItemType.OpenWith
                    3 -> ContextMenuItemType.SplitScreen
                    4 -> ContextMenuItemType.ShareLink
                    else -> null
                }
            }
            y in secondRowStart..secondRowEnd -> {
                // Second row items (variable items based on conditions)
                val itemIndex = (x / itemWidthPx).toInt()
                val secondRowItems = buildList {
                    add(ContextMenuItemType.CopyLink)
                    add(ContextMenuItemType.SelectText)
                    if (shouldShowTranslateImage && (url.lowercase().contains("jpg") || url.lowercase().contains("png"))) {
                        add(ContextMenuItemType.TranslateImage)
                    }
                    add(ContextMenuItemType.Tts)
                    add(ContextMenuItemType.SaveAs)
                    add(ContextMenuItemType.Summarize)
                }
                return secondRowItems.getOrNull(itemIndex)
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
private fun ContextMenuItems(
    url: String = "",
    shouldShowAdBlock: Boolean = true,
    shouldShowTranslateImage: Boolean = false,
    showIcons: Boolean = true,
    hoveredItem: ContextMenuItemType? = null,
    onClicked: (ContextMenuItemType) -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .width(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            URLDecoder.decode(url, "UTF-8"),
            Modifier.padding(4.dp),
            color = MaterialTheme.colors.onBackground,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
        ) {
            ContextMenuItem(
                titleResId = R.string.main_menu_new_tabOpen,
                showIcon = showIcons,
                imageVector = Icons.Outlined.Tab,
                isHovered = hoveredItem == ContextMenuItemType.NewTabForeground
            ) {
                onClicked(ContextMenuItemType.NewTabForeground)
            }
            ContextMenuItem(
                titleResId = R.string.main_menu_new_tab,
                showIcon = showIcons,
                imageVector = Icons.Outlined.TabUnselected,
                isHovered = hoveredItem == ContextMenuItemType.NewTabBackground
            ) {
                onClicked(ContextMenuItemType.NewTabBackground)
            }
            ContextMenuItem(
                titleResId = R.string.menu_open_with,
                showIcon = showIcons,
                imageVector = Icons.Outlined.Apps,
                isHovered = hoveredItem == ContextMenuItemType.OpenWith
            ) { onClicked(OpenWith) }
            ContextMenuItem(
                titleResId = R.string.split_screen,
                showIcon = showIcons,
                imageVector = Icons.Outlined.ViewStream,
                isHovered = hoveredItem == ContextMenuItemType.SplitScreen
            ) {
                onClicked(ContextMenuItemType.SplitScreen)
            }
            ContextMenuItem(
                titleResId = R.string.menu_share_link,
                showIcon = showIcons,
                imageVector = Icons.Outlined.Share,
                isHovered = hoveredItem == ContextMenuItemType.ShareLink
            ) {
                onClicked(ContextMenuItemType.ShareLink)
            }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            ContextMenuItem(
                titleResId = R.string.copy_link,
                showIcon = showIcons,
                imageVector = Icons.Outlined.CopyAll,
                isHovered = hoveredItem == ContextMenuItemType.CopyLink
            ) { onClicked(CopyLink) }
            ContextMenuItem(
                titleResId = R.string.text_select,
                showIcon = showIcons,
                imageVector = Icons.AutoMirrored.Outlined.Segment,
                isHovered = hoveredItem == ContextMenuItemType.SelectText
            ) {
                onClicked(SelectText)
            }
            val lowerCaseUrl = url.lowercase()
            if (shouldShowTranslateImage && (lowerCaseUrl.contains("jpg") || lowerCaseUrl.contains("png"))) {
                ContextMenuItem(
                    titleResId = R.string.translate,
                    showIcon = showIcons,
                    iconResId = R.drawable.ic_papago,
                    isHovered = hoveredItem == ContextMenuItemType.TranslateImage
                ) {
                    onClicked(TranslateImage)
                }
            }
            ContextMenuItem(
                titleResId = R.string.menu_tts,
                showIcon = showIcons,
                imageVector = Icons.Outlined.RecordVoiceOver,
                isHovered = hoveredItem == ContextMenuItemType.Tts
            ) { onClicked(Tts) }
            ContextMenuItem(
                titleResId = R.string.menu_save_as,
                showIcon = showIcons,
                imageVector = Icons.Outlined.Save,
                isHovered = hoveredItem == ContextMenuItemType.SaveAs
            ) { onClicked(SaveAs) }
            ContextMenuItem(
                titleResId = R.string.menu_summarize,
                showIcon = showIcons,
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                isHovered = hoveredItem == ContextMenuItemType.Summarize
            ) { onClicked(Summarize) }
//            if (shouldShowAdBlock) {
//                ContextMenuItem(R.string.setting_title_adblock, showIcons, Icons.Outlined.Block) { onClicked(AdBlock) }
//            }
        }
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
                    .background(MaterialTheme.colors.primary, shape = CircleShape)
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