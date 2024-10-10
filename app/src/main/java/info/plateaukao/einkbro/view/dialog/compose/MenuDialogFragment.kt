package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.SendToMobile
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.InvertColorsOff
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.toggle
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

class MenuDialogFragment(
    private val url: String,
    private val isSpeaking: Boolean,
    private val itemClicked: (MenuItemType) -> Unit,
    private val itemLongClicked: (MenuItemType) -> Unit,
) : ComposeDialogFragment() {

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            MenuItems(
                config.whiteBackground(url),
                config.boldFontStyle,
                config.blackFontStyle,
                isSpeaking,
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
            .verticalScroll(rememberScrollState())
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
            MenuItem(R.string.menu_highlights, 0, Icons.Outlined.EditNote) {
                onClicked(MenuItemType.Highlights)
            }
            MenuItem(R.string.menu_fav, 0, Icons.Outlined.AddHome) { onClicked(MenuItemType.SetHome) }
            MenuItem(R.string.menu_openFav, 0, Icons.Outlined.Home) { onClicked(OpenHome) }
            MenuItem(R.string.menu_closeTab, 0, Icons.Outlined.CancelPresentation) { onClicked(CloseTab) }
            MenuItem(R.string.menu_quit, 0, Icons.AutoMirrored.Outlined.Logout) { onClicked(Quit) }
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
                    Icons.Outlined.InstallMobile,
                    onLongClicked = { onLongClicked(MenuItemType.ReceiveData) },
                ) { onClicked(MenuItemType.ReceiveData) }
                MenuItem(
                    R.string.menu_save_bookmark,
                    Icons.Outlined.BookmarkAdd
                ) { onClicked(MenuItemType.SaveBookmark) }
                MenuItem(R.string.menu_sc, 0, Icons.Outlined.AddLink) { onClicked(Shortcut) }
                MenuItem(R.string.menu_open_with, 0, Icons.Outlined.Apps) { onClicked(OpenWith) }
                MenuItem(R.string.copy_link, 0, Icons.Outlined.CopyAll) { onClicked(CopyLink) }
                MenuItem(R.string.menu_share_link, 0, Icons.Outlined.Share) { onClicked(ShareLink) }
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(
                    R.string.menu_send_link,
                    Icons.AutoMirrored.Outlined.SendToMobile,
                    onLongClicked = { onLongClicked(MenuItemType.SendLink) },
                ) { onClicked(MenuItemType.SendLink) }
                MenuItem(
                    R.string.menu_add_to_pocket,
                    R.drawable.ic_pocket
                ) { onClicked(AddToPocket) }
                MenuItem(R.string.menu_save_archive, 0, Icons.Outlined.Save) {
                    onClicked(
                        MenuItemType.SaveArchive
                    )
                }
                MenuItem(R.string.menu_open_epub, Icons.AutoMirrored.Outlined.LibraryBooks) { onClicked(OpenEpub) }
                MenuItem(R.string.menu_save_epub, Icons.AutoMirrored.Outlined.Article) { onClicked(SaveEpub) }
                MenuItem(R.string.menu_save_pdf, Icons.Outlined.PictureAsPdf) { onClicked(SavePdf) }
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
                MenuItem(R.string.menu_save_epub, Icons.AutoMirrored.Outlined.Feed) { onClicked(SaveEpub) }
                MenuItem(R.string.copy_link, Icons.Outlined.CopyAll) { onClicked(CopyLink) }
                MenuItem(
                    R.string.menu_share_link,
                    Icons.Outlined.Share
                ) { onClicked(ShareLink) }
                MenuItem(
                    R.string.menu_expand_menu,
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight
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
                    Icons.Outlined.ViewStream,
                ) { onClicked(MenuItemType.SplitScreen) }
                MenuItem(
                    R.string.translate,
                    Icons.Outlined.Translate,
                    onLongClicked = { onLongClicked(MenuItemType.Translate) },
                ) { onClicked(MenuItemType.Translate) }
                MenuItem(
                    R.string.vertical_read,
                    Icons.Outlined.ViewColumn,
                ) { onClicked(MenuItemType.VerticalRead) }
                MenuItem(
                    R.string.reader_mode,
                    Icons.AutoMirrored.Outlined.ChromeReaderMode,
                ) { onClicked(MenuItemType.ReaderMode) }
                MenuItem(
                    R.string.touch_area_setting,
                    Icons.Outlined.TouchApp,
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
                val ttsRes = if (isSpeaking) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver
                MenuItem(R.string.menu_tts, ttsRes,
                    onLongClicked = { onLongClicked(MenuItemType.Tts) }) { onClicked(MenuItemType.Tts) }
                val invertRes =
                    if (hasInvertedColor) Icons.Outlined.InvertColorsOff else Icons.Outlined.InvertColors
                MenuItem(R.string.menu_invert_color, invertRes) {
                    onClicked(MenuItemType.InvertColor)
                }
                val whiteRes =
                    if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                MenuItem(R.string.white_background, whiteRes) { onClicked(MenuItemType.WhiteBknd) }
                val blackRes =
                    if (blackFont) Icons.TwoTone.Copyright else Icons.Outlined.Copyright
                MenuItem(R.string.black_font, blackRes) { onClicked(MenuItemType.BlackFont) }
                val boldRes =
                    if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                MenuItem(R.string.bold_font, boldRes,
                    onLongClicked = { onLongClicked(MenuItemType.BoldFont) }
                ) { onClicked(MenuItemType.BoldFont) }
                MenuItem(
                    R.string.font_size,
                    Icons.Outlined.FormatSize
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
                    Icons.Outlined.Translate,
                    onLongClicked = { onLongClicked(MenuItemType.Translate) },
                ) { onClicked(MenuItemType.Translate) }
                MenuItem(
                    R.string.reader_mode,
                    Icons.AutoMirrored.Outlined.ChromeReaderMode
                ) { onClicked(MenuItemType.ReaderMode) }
                val whiteRes =
                    if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                MenuItem(R.string.white_background, whiteRes) { onClicked(MenuItemType.WhiteBknd) }
                val boldRes =
                    if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                MenuItem(R.string.bold_font, boldRes) { onClicked(MenuItemType.BoldFont) }
                MenuItem(
                    R.string.menu_expand_menu,
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
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
            MenuItem(R.string.menu_other_searchSite, Icons.Outlined.Search) {
                onClicked(
                    MenuItemType.Search
                )
            }
            MenuItem(
                R.string.menu_download,
                Icons.Outlined.Download
            ) { onClicked(MenuItemType.Download) }
            MenuItem(
                R.string.toolbar_icons,
                Icons.Outlined.Straighten
            ) { onClicked(MenuItemType.ToolbarSetting) }
            MenuItem(
                R.string.menu_quickToggle,
                Icons.Outlined.SettingsSuggest,
            ) { onClicked(MenuItemType.QuickToggle) }
            MenuItem(
                R.string.settings,
                Icons.Outlined.Settings,
                onLongClicked = { onLongClicked(Settings) }) { onClicked(Settings) }
        }
    }
}

@Composable
fun MenuItem(
    titleResId: Int,
    imageVector: ImageVector,
    isLargeType: Boolean = false,
    showIcon: Boolean = true,
    onLongClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
) {
    MenuItem(
        titleResId,
        0,
        imageVector,
        isLargeType,
        showIcon,
        onLongClicked,
        onClicked,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    imageVector: ImageVector? = null,
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
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector, contentDescription = null,
                    modifier = Modifier
                        .size(if (isLargeType) 55.dp else 44.dp)
                        .padding(horizontal = 6.dp),
                    tint = MaterialTheme.colors.onBackground
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconResId), contentDescription = null,
                    modifier = Modifier
                        .size(if (isLargeType) 55.dp else 44.dp)
                        .padding(horizontal = 6.dp),
                    tint = MaterialTheme.colors.onBackground
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = if (showIcon) (-5).dp else 10.dp),
                text = stringResource(id = titleResId),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = if (!showIcon) 20.sp else 12.sp,
                fontSize = fontSize,
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewItem() {
    MyTheme {
        Column {
            MenuItem(R.string.title_appData, Icons.Outlined.Backup, showIcon = false) {}
            MenuItem(R.string.title, 0, Icons.Outlined.Translate) {}
        }
    }
}

@Preview(showBackground = true)
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
