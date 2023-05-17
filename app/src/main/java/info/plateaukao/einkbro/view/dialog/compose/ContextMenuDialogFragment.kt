package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.AdBlock
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.CopyLink
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.CopyText
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.SaveAs
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.SaveBookmark
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType.TranslateImage
import java.net.URLDecoder


class ContextMenuDialogFragment(
    private val url: String,
    private val shouldShowAdBlock: Boolean,
    private val shouldShowTranslateImage: Boolean,
    private val anchorPoint: Point,
    private val itemClicked: (ContextMenuItemType) -> Unit
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            ContextMenuItems(url, shouldShowAdBlock, shouldShowTranslateImage) { item ->
                dialog?.dismiss()
                itemClicked(item)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            maxLines = 1,
        )
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
        ) {
            ContextMenuItem(R.string.main_menu_new_tabOpen, R.drawable.icon_tab_plus) {
                onClicked(
                    ContextMenuItemType.NewTabForeground
                )
            }
            ContextMenuItem(R.string.main_menu_new_tab, R.drawable.icon_tab_unselected) {
                onClicked(
                    ContextMenuItemType.NewTabBackground
                )
            }
            ContextMenuItem(R.string.menu_open_with, R.drawable.icon_exit) { onClicked(OpenWith) }
            ContextMenuItem(R.string.split_screen, R.drawable.ic_split_screen) {
                onClicked(
                    ContextMenuItemType.SplitScreen
                )
            }
            ContextMenuItem(R.string.menu_share_link, R.drawable.icon_menu_share) {
                onClicked(
                    ContextMenuItemType.ShareLink
                )
            }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            if (shouldShowTranslateImage) {
                ContextMenuItem(R.string.translate, R.drawable.ic_papago) {
                    onClicked(TranslateImage)
                }
            } else {
                ContextMenuItem(R.string.menu_save_bookmark, R.drawable.ic_bookmark) {
                    onClicked(SaveBookmark)
                }
            }
            ContextMenuItem(R.string.menu_save_as, R.drawable.icon_menu_save) { onClicked(SaveAs) }
            ContextMenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
            ContextMenuItem(R.string.copy_text, R.drawable.ic_copy) { onClicked(CopyText) }
            if (shouldShowAdBlock) {
                ContextMenuItem(R.string.setting_title_adblock, R.drawable.ic_block) {
                    onClicked(
                        AdBlock
                    )
                }
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
    SplitScreen, AdBlock, TranslateImage
}

@Preview
@Composable
fun PreviewContextMenuItems() {
    MyTheme {
        ContextMenuItems("abc") { }
    }
}