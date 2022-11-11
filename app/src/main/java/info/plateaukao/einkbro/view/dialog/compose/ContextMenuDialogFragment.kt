package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.*
import java.net.URLDecoder

class ContextMenuDialogFragment(
    private val url: String,
    private val shouldShowAdBlock: Boolean,
    private val itemClicked: (ContextMenuItemType) -> Unit
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            ContextMenuItems(url, shouldShowAdBlock) { item ->
                dialog?.dismiss()
                itemClicked(item)
            }
        }
    }
}

@Composable
private fun ContextMenuItems(
    url: String = "",
    shouldShowAdBlock: Boolean = true,
    onClicked: (ContextMenuItemType) -> Unit
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
            maxLines = 3,
        )
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
//            horizontalArrangement = Arrangement.End
        ) {
            ContextMenuItem(R.string.split_screen, R.drawable.ic_split_screen) { onClicked(SplitScreen) }
            ContextMenuItem(R.string.menu_share_link, R.drawable.icon_menu_share) { onClicked(ShareLink) }
            ContextMenuItem(R.string.menu_open_with, R.drawable.icon_exit) { onClicked(OpenWith) }
            ContextMenuItem(R.string.main_menu_new_tabOpen, R.drawable.icon_tab_plus) { onClicked(NewTabForeground) }
            ContextMenuItem(R.string.main_menu_new_tab, R.drawable.icon_tab_unselected) { onClicked(NewTabBackground) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            ContextMenuItem(R.string.menu_save_bookmark, R.drawable.ic_bookmark) { onClicked(SaveBookmark) }
            ContextMenuItem(R.string.menu_save_as, R.drawable.icon_menu_save) { onClicked(SaveAs) }
            ContextMenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
            ContextMenuItem(R.string.copy_text, R.drawable.ic_copy) { onClicked(CopyText) }
            if (shouldShowAdBlock) {
                ContextMenuItem(R.string.setting_title_adblock, R.drawable.ic_block) { onClicked(AdBlock) }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    titleResId: Int,
    iconResId: Int,
    onClicked: () -> Unit = {},
) = MenuItem(
    titleResId = titleResId,
    iconResId = iconResId,
    isLargeType = true,
    onClicked = onClicked
)

enum class ContextMenuItemType {
    NewTabForeground, NewTabBackground,
    ShareLink, CopyLink, CopyText, OpenWith,
    SaveBookmark, SaveAs,
    SplitScreen, AdBlock
}

@Preview
@Composable
fun PreviewContextMenuItems() {
    MyTheme {
        ContextMenuItems("abc") { }
    }
}