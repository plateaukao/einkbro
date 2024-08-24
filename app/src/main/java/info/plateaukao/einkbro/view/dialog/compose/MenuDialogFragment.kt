package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.AddToPocket
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CloseTab
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CopyLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenEpub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenHome
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Quit
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SaveEpub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SavePdf
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Settings
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ShareLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Shortcut
import org.koin.android.ext.android.inject

class MenuDialogFragment(
    private val url: String,
    private val itemClicked: (MenuItemType) -> Unit,
    private val itemLongClicked: (MenuItemType) -> Unit,
) : ComposeDialogFragment() {
    private val ttsManager: TtsManager by inject()

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            MenuItems(
                config.whiteBackground(url),
                config.boldFontStyle,
                config.blackFontStyle,
                ttsManager.isSpeaking(),
                config.showShareSaveMenu,
                config.showContentMenu,
                config.hasInvertedColor(url),
                { config::showShareSaveMenu.toggle() },
                { config::showContentMenu.toggle() },
                { dialog?.dismiss(); itemClicked(it) },
                this::runItemLongClickAndDismiss
            )
        }
    }

    private fun runItemLongClickAndDismiss(menuItemType: MenuItemType) {
        itemLongClicked(menuItemType)
        // need to use post to prevent the dialog from being dismissed before the long click action
        // without this workaround, it will cause crash.
        activity?.window?.decorView?.post { dialog?.dismiss() }
    }
}


enum class MenuItemType {
    Tts, QuickToggle, OpenHome, CloseTab, Quit,
    SplitScreen, Translate, VerticalRead, ReaderMode, TouchSetting, ToolbarSetting,
    ReceiveData, SendLink, ShareLink, OpenWith, CopyLink, Shortcut,
    SetHome, SaveBookmark, OpenEpub, SaveEpub, SavePdf,
    FontSize, WhiteBknd, BoldFont, Search, Download, Settings, BlackFont,
    SaveArchive, AddToPocket, Highlights, InvertColor
}

@Composable
private fun MenuItems(
    hasWhiteBkd: Boolean,
    boldFont: Boolean,
    blackFont: Boolean,
    isSpeaking: Boolean,
    showShareSaveMenu: Boolean,
    showContentMenu: Boolean,
    hasInvertedColor: Boolean,
    toggleShareSaveMenu: () -> Unit,
    toggleContentMenu: () -> Unit,
    onClicked: (MenuItemType) -> Unit,
    onLongClicked: (MenuItemType) -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        var currentShowShare by remember { mutableStateOf(showShareSaveMenu) }
        var currentShowContent by remember { mutableStateOf(showContentMenu) }

        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MenuItem(
                R.string.menu_highlights,
                R.drawable.ic_highlight
            ) { onClicked(MenuItemType.Highlights) }
            MenuItem(R.string.menu_fav, R.drawable.ic_home_set) { onClicked(MenuItemType.SetHome) }
            MenuItem(R.string.menu_openFav, R.drawable.ic_home) { onClicked(OpenHome) }
            MenuItem(R.string.menu_closeTab, R.drawable.icon_close) { onClicked(CloseTab) }
            MenuItem(R.string.menu_quit, R.drawable.icon_exit) { onClicked(Quit) }
        }
        HorizontalSeparator()
        Text(
            stringResource(R.string.share_save),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clickable {
                    currentShowShare = !currentShowShare
                    toggleShareSaveMenu()
                },
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onBackground
        )
        if (currentShowShare) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.menu_receive,
                    R.drawable.ic_receive,
                    onLongClicked = { onLongClicked(MenuItemType.ReceiveData) },
                ) { onClicked(MenuItemType.ReceiveData) }
                MenuItem(
                    R.string.menu_save_bookmark,
                    R.drawable.ic_bookmark
                ) { onClicked(MenuItemType.SaveBookmark) }
                MenuItem(R.string.menu_sc, R.drawable.link_plus) { onClicked(Shortcut) }
                MenuItem(R.string.menu_open_with, R.drawable.icon_exit) { onClicked(OpenWith) }
                MenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
                MenuItem(
                    R.string.menu_share_link,
                    R.drawable.icon_menu_share
                ) { onClicked(ShareLink) }
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.menu_send_link,
                    R.drawable.ic_send,
                    onLongClicked = { onLongClicked(MenuItemType.SendLink) },
                ) { onClicked(MenuItemType.SendLink) }
                MenuItem(
                    R.string.menu_add_to_pocket,
                    R.drawable.ic_pocket
                ) { onClicked(AddToPocket) }
                MenuItem(R.string.menu_save_archive, R.drawable.ic_save_archive) {
                    onClicked(
                        MenuItemType.SaveArchive
                    )
                }
                MenuItem(R.string.menu_open_epub, R.drawable.ic_open_epub) { onClicked(OpenEpub) }
                MenuItem(R.string.menu_save_epub, R.drawable.ic_book) { onClicked(SaveEpub) }
                MenuItem(R.string.menu_save_pdf, R.drawable.ic_pdf) { onClicked(SavePdf) }
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.menu_add_to_pocket,
                    R.drawable.ic_pocket
                ) { onClicked(AddToPocket) }
                MenuItem(R.string.menu_save_epub, R.drawable.ic_book) { onClicked(SaveEpub) }
                MenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
                MenuItem(
                    R.string.menu_share_link,
                    R.drawable.icon_menu_share
                ) { onClicked(ShareLink) }
                MenuItem(
                    R.string.menu_expand_menu,
                    R.drawable.icon_arrow_right_gest,
                ) { currentShowShare = true; toggleShareSaveMenu() }
            }
        }
        HorizontalSeparator()
        Text(
            stringResource(R.string.content_adjustment),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clickable {
                    currentShowContent = !currentShowContent
                    toggleContentMenu()
                },
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onBackground
        )
        if (currentShowContent) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.split_screen,
                    R.drawable.ic_split_screen
                ) { onClicked(MenuItemType.SplitScreen) }
                MenuItem(
                    R.string.translate,
                    R.drawable.ic_translate,
                    onLongClicked = { onLongClicked(MenuItemType.Translate) },
                ) { onClicked(MenuItemType.Translate) }
                MenuItem(
                    R.string.vertical_read,
                    R.drawable.ic_vertical_read
                ) { onClicked(MenuItemType.VerticalRead) }
                MenuItem(
                    R.string.reader_mode,
                    R.drawable.ic_reader
                ) { onClicked(MenuItemType.ReaderMode) }
                MenuItem(
                    R.string.touch_area_setting,
                    R.drawable.ic_touch_disabled,
                    onLongClicked = { onLongClicked(MenuItemType.TouchSetting) },
                ) {
                    onClicked(
                        MenuItemType.TouchSetting
                    )
                }
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val ttsRes = if (isSpeaking) R.drawable.ic_stop else R.drawable.ic_tts
                MenuItem(R.string.menu_tts, ttsRes) { onClicked(MenuItemType.Tts) }
                val invertRes =
                    if (hasInvertedColor) R.drawable.ic_invert_color_off else R.drawable.ic_invert_color
                MenuItem(R.string.menu_invert_color, invertRes) {
                    onClicked(MenuItemType.InvertColor)
                }
                val whiteRes =
                    if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                MenuItem(R.string.white_background, whiteRes) { onClicked(MenuItemType.WhiteBknd) }
                val blackRes =
                    if (blackFont) R.drawable.ic_black_font_on else R.drawable.ic_black_font_off
                MenuItem(R.string.black_font, blackRes) { onClicked(MenuItemType.BlackFont) }
                val boldRes =
                    if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                MenuItem(R.string.bold_font, boldRes,
                    onLongClicked = { onLongClicked(MenuItemType.BoldFont) }
                ) { onClicked(MenuItemType.BoldFont) }
                MenuItem(
                    R.string.font_size,
                    R.drawable.icon_size
                ) { onClicked(MenuItemType.FontSize) }
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.translate,
                    R.drawable.ic_translate,
                    onLongClicked = { onLongClicked(MenuItemType.Translate) },
                ) { onClicked(MenuItemType.Translate) }
                MenuItem(
                    R.string.reader_mode,
                    R.drawable.ic_reader
                ) { onClicked(MenuItemType.ReaderMode) }
                val whiteRes =
                    if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                MenuItem(R.string.white_background, whiteRes) { onClicked(MenuItemType.WhiteBknd) }
                val boldRes =
                    if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                MenuItem(R.string.bold_font, boldRes) { onClicked(MenuItemType.BoldFont) }
                MenuItem(
                    R.string.menu_expand_menu,
                    R.drawable.icon_arrow_right_gest,
                ) { currentShowContent = true; toggleContentMenu() }
            }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MenuItem(R.string.menu_other_searchSite, R.drawable.icon_search) {
                onClicked(
                    MenuItemType.Search
                )
            }
            MenuItem(
                R.string.menu_download,
                R.drawable.icon_download
            ) { onClicked(MenuItemType.Download) }
            MenuItem(
                R.string.toolbar_setting,
                R.drawable.ic_toolbar
            ) { onClicked(MenuItemType.ToolbarSetting) }
            MenuItem(
                R.string.menu_quickToggle,
                R.drawable.ic_quick_toggle
            ) { onClicked(MenuItemType.QuickToggle) }
            MenuItem(R.string.settings, R.drawable.icon_settings) { onClicked(Settings) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    isLargeType: Boolean = false,
    showIcon: Boolean = true,
    onLongClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 0.5.dp else (-1).dp

    val configuration = LocalConfiguration.current
    val width = when {
        isLargeType -> if (configuration.screenWidthDp > 500) 62.dp else 50.dp
        configuration.screenWidthDp > 500 -> 55.dp
        else -> 45.dp
    }

    val fontSize = if (!showIcon) 16.sp else if (configuration.screenWidthDp > 500) 10.sp else 8.sp
    Column(
        modifier = Modifier
            .width(width)
            .height(if (isLargeType) 80.dp else 70.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = { onLongClicked() },
                onClick = { onClicked() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (!showIcon) Arrangement.Center else Arrangement.Top
    ) {
        if (showIcon) {
            Icon(
                painter = painterResource(id = iconResId), contentDescription = null,
                modifier = Modifier
                    .size(if (isLargeType) 55.dp else 44.dp)
                    .padding(horizontal = 6.dp),
                tint = MaterialTheme.colors.onBackground
            )
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = if (showIcon) (-5).dp else 0.dp),
            text = stringResource(id = titleResId),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = if (!showIcon) 20.sp else 12.sp,
            fontSize = fontSize,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Preview
@Composable
private fun PreviewItem() {
    MyTheme {
        Column {
            MenuItem(R.string.title_appData, R.drawable.ic_copy, showIcon = false) {}
            MenuItem(R.string.title, R.drawable.ic_location) {}
        }
    }
}

@Preview
@Composable
private fun PreviewMenuItems() {
    MyTheme {
        MenuItems(
            hasWhiteBkd = false,
            boldFont = false,
            blackFont = false,
            isSpeaking = false,
            showShareSaveMenu = false,
            showContentMenu = false,
            hasInvertedColor = false,
            {},
            {},
            {},
        ) {}
    }
}
