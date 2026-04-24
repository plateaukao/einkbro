package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.SendToMobile
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.CloudUpload
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
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.HeadsetOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CloseTab
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Epub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenHome
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Quit
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SavePdf
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Settings
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ShareLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Shortcut

class MenuDialogFragment(
    private val url: String,
    private val isSpeaking: Boolean,
    private val isAudioOnly: Boolean = false,
    private val hasVideo: Boolean = false,
    private val isTouchPaginationEnabled: Boolean = false,
    private val itemClicked: (MenuItemType) -> Unit,
    private val itemLongClicked: (MenuItemType) -> Unit,
) : ComposeDialogFragment() {

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                MenuItems(
                    config.whiteBackground(url),
                    config.display.boldFontStyle,
                    config.display.blackFontStyle,
                    isSpeaking,
                    isAudioOnly,
                    hasVideo,
                    config.ui.showShareSaveMenu,
                    config.ui.showContentMenu,
                    config.hasInvertedColor(url),
                    isTouchPaginationEnabled,
                    { config.ui::showShareSaveMenu.toggle() },
                    { config.ui::showContentMenu.toggle() },
                    { dialog?.dismiss(); itemClicked(it) },
                    this::runItemLongClickAndDismiss
                )
            }
        }
    }

    private fun runItemLongClickAndDismiss(menuItemType: MenuItemType) {
        itemLongClicked(menuItemType)
        // Dismiss via post so the long-click action fires before the dialog tears down — avoids a crash.
        activity?.window?.decorView?.post { dialog?.dismiss() }
    }

    companion object {
        /**
         * Pre-inflate the menu's Compose content into an off-screen, 0x0 ComposeView so that the
         * first real open skips Compose runtime boot, Material icon vector parsing, and theme setup.
         */
        fun prewarm(activity: FragmentActivity) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            val warmView = ComposeView(activity).apply {
                visibility = View.INVISIBLE
                layoutParams = ViewGroup.LayoutParams(0, 0)
                setContent {
                    MyTheme {
                        MenuItems(
                            hasWhiteBkd = false,
                            boldFont = false,
                            blackFont = false,
                            isSpeaking = false,
                            isAudioOnly = false,
                            hasVideo = true,
                            showShareSaveMenu = true,
                            showContentMenu = true,
                            hasInvertedColor = false,
                            isTouchPaginationEnabled = false,
                            toggleShareSaveMenu = {},
                            toggleContentMenu = {},
                            onClicked = {},
                            onLongClicked = {},
                        )
                    }
                }
            }
            root.addView(warmView)
            root.postDelayed({ root.removeView(warmView) }, 3000)
        }
    }
}


enum class MenuItemType {
    Tts, QuickToggle, OpenHome, CloseTab, Quit,
    SplitScreen, Translate, VerticalRead, ReaderMode, TouchSetting, ToolbarSetting,
    ReceiveData, SendLink, ShareLink, OpenWith, Shortcut,
    SetHome, SaveBookmark, Epub, SavePdf,
    FontSize, WhiteBknd, BoldFont, Search, Download, Settings, BlackFont,
    SaveArchive, SaveMht, Highlights, InvertColor, PageAiActions, Instapaper, AudioOnly,
    SiteSettings
}

@Composable
private fun MenuItems(
    hasWhiteBkd: Boolean,
    boldFont: Boolean,
    blackFont: Boolean,
    isSpeaking: Boolean,
    isAudioOnly: Boolean,
    hasVideo: Boolean,
    showShareSaveMenu: Boolean,
    showContentMenu: Boolean,
    hasInvertedColor: Boolean,
    isTouchPaginationEnabled: Boolean,
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
                MenuItem(
                    R.string.menu_share_link,
                    0,
                    Icons.Outlined.Share,
                    onLongClicked = { onLongClicked(ShareLink) },
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
                    Icons.AutoMirrored.Outlined.SendToMobile,
                    onLongClicked = { onLongClicked(MenuItemType.SendLink) },
                ) { onClicked(MenuItemType.SendLink) }
                MenuItem(
                    R.string.menu_instapaper,
                    Icons.Outlined.CloudUpload,
                    onLongClicked = { onLongClicked(MenuItemType.Instapaper) },
                ) { onClicked(MenuItemType.Instapaper) }
                MenuItem(
                    R.string.menu_save_archive,
                    0,
                    Icons.Outlined.Save,
                    onLongClicked = { onLongClicked(MenuItemType.SaveArchive) },
                ) { onClicked(MenuItemType.SaveArchive) }
                MenuItem(
                    R.string.menu_save_mht,
                    0,
                    Icons.Outlined.Save,
                ) { onClicked(MenuItemType.SaveMht) }
                MenuItem(R.string.menu_epub, Icons.AutoMirrored.Outlined.Article) { onClicked(Epub) }
                MenuItem(R.string.menu_save_pdf, Icons.Outlined.PictureAsPdf) { onClicked(SavePdf) }
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuItem(R.string.menu_epub, Icons.AutoMirrored.Outlined.Feed) { onClicked(Epub) }
                MenuItem(
                    R.string.menu_share_link,
                    Icons.Outlined.Share,
                    onLongClicked = { onLongClicked(ShareLink) },
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
                    R.string.page_ai,
                    R.drawable.ic_robot,
                ) { onClicked(MenuItemType.PageAiActions) }
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
                val touchRes =
                    if (isTouchPaginationEnabled) R.drawable.ic_touch_enabled else R.drawable.ic_touch_disabled
                MenuItem(
                    R.string.touch_area_setting,
                    touchRes,
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
                MenuItem(
                    R.string.menu_tts, ttsRes,
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
                MenuItem(
                    R.string.bold_font, boldRes,
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
            if (hasVideo) {
                val audioOnlyIcon = if (isAudioOnly) Icons.Outlined.Headset else Icons.Outlined.HeadsetOff
                MenuItem(R.string.audio_only_mode, audioOnlyIcon) {
                    onClicked(MenuItemType.AudioOnly)
                }
            }
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
                R.string.site_settings,
                Icons.Outlined.Tune,
            ) { onClicked(MenuItemType.SiteSettings) }
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

    val configuration = LocalConfiguration.current
    val width = when {
        isLargeType -> if (configuration.screenWidthDp > 500) 62.dp else 50.dp
        configuration.screenWidthDp > 500 -> 55.dp
        else -> 45.dp
    }

    val fontSize = if (!showIcon) 16.sp else if (configuration.screenWidthDp > 500) 10.sp else 8.sp

    Box {
        if (pressed) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colors.onBackground, shape = CircleShape)
                    .align(Alignment.TopCenter)
            )
        }
        Column(
            modifier = Modifier
                .width(width)
                .height(if (!showIcon) 50.dp else if (isLargeType) 80.dp else 70.dp)
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
            isAudioOnly = false,
            hasVideo = false,
            showShareSaveMenu = false,
            showContentMenu = false,
            hasInvertedColor = false,
            isTouchPaginationEnabled = false,
            {},
            {},
            {},
        ) {}
    }
}
