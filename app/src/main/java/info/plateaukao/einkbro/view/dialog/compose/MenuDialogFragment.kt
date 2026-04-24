package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
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
                val hiddenItems = remember {
                    config.ui.hiddenMenuItems.mapNotNull { name ->
                        runCatching { MenuItemType.valueOf(name) }.getOrNull()
                    }.toSet()
                }
                CompositionLocalProvider(
                    LocalMenuHideConfig provides MenuHideConfig(hideMode = false, hiddenItems = hiddenItems)
                ) {
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

data class MenuHideConfig(
    val hideMode: Boolean = false,
    val hiddenItems: Set<MenuItemType> = emptySet(),
    val onToggleHide: (MenuItemType) -> Unit = {},
)

val LocalMenuHideConfig = staticCompositionLocalOf { MenuHideConfig() }

data class MenuActions(
    val onClicked: (MenuItemType) -> Unit = {},
    val onLongClicked: (MenuItemType) -> Unit = {},
)

val LocalMenuActions = staticCompositionLocalOf { MenuActions() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HideableSlot(type: MenuItemType, content: @Composable () -> Unit) {
    val cfg = LocalMenuHideConfig.current
    val isHidden = type in cfg.hiddenItems
    when {
        cfg.hideMode -> {
            Box(modifier = Modifier.alpha(if (isHidden) 0.3f else 1f)) {
                content()
                // Overlay absorbs both tap and long-press so underlying MenuItem handlers don't fire.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onLongClick = { cfg.onToggleHide(type) },
                            onClick = { cfg.onToggleHide(type) },
                        )
                )
            }
        }
        isHidden -> Unit
        else -> content()
    }
}

@Composable
private fun HideableMenuItem(
    type: MenuItemType,
    titleResId: Int,
    imageVector: ImageVector? = null,
    iconResId: Int = 0,
    supportsLongClick: Boolean = false,
) {
    val actions = LocalMenuActions.current
    HideableSlot(type) {
        MenuItem(
            titleResId = titleResId,
            iconResId = iconResId,
            imageVector = imageVector,
            onLongClicked = if (supportsLongClick) ({ actions.onLongClicked(type) }) else ({}),
            onClicked = { actions.onClicked(type) },
        )
    }
}

@Composable
fun MenuItems(
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
    CompositionLocalProvider(
        LocalMenuActions provides MenuActions(onClicked, onLongClicked)
    ) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        val hideMode = LocalMenuHideConfig.current.hideMode
        var currentShowShare by remember { mutableStateOf(showShareSaveMenu) }
        var currentShowContent by remember { mutableStateOf(showContentMenu) }
        val effectiveShowShare = hideMode || currentShowShare
        val effectiveShowContent = hideMode || currentShowContent

        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HideableMenuItem(MenuItemType.Highlights, R.string.menu_highlights, Icons.Outlined.EditNote)
            HideableMenuItem(MenuItemType.SetHome, R.string.menu_fav, Icons.Outlined.AddHome)
            HideableMenuItem(MenuItemType.OpenHome, R.string.menu_openFav, Icons.Outlined.Home)
            HideableMenuItem(MenuItemType.CloseTab, R.string.menu_closeTab, Icons.Outlined.CancelPresentation)
            HideableMenuItem(MenuItemType.Quit, R.string.menu_quit, Icons.AutoMirrored.Outlined.Logout)
        }
        HorizontalSeparator()
        Text(
            stringResource(R.string.share_save),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .then(
                    if (hideMode) Modifier
                    else Modifier.clickable {
                        currentShowShare = !currentShowShare
                        toggleShareSaveMenu()
                    }
                ),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onBackground
        )
        if (effectiveShowShare) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HideableMenuItem(MenuItemType.ReceiveData, R.string.menu_receive, Icons.Outlined.InstallMobile, supportsLongClick = true)
                HideableMenuItem(MenuItemType.SaveBookmark, R.string.menu_save_bookmark, Icons.Outlined.BookmarkAdd)
                HideableMenuItem(MenuItemType.Shortcut, R.string.menu_sc, Icons.Outlined.AddLink)
                HideableMenuItem(MenuItemType.OpenWith, R.string.menu_open_with, Icons.Outlined.Apps)
                HideableMenuItem(MenuItemType.ShareLink, R.string.menu_share_link, Icons.Outlined.Share, supportsLongClick = true)
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HideableMenuItem(MenuItemType.SendLink, R.string.menu_send_link, Icons.AutoMirrored.Outlined.SendToMobile, supportsLongClick = true)
                HideableMenuItem(MenuItemType.Instapaper, R.string.menu_instapaper, Icons.Outlined.CloudUpload, supportsLongClick = true)
                HideableMenuItem(MenuItemType.SaveArchive, R.string.menu_save_archive, Icons.Outlined.Save, supportsLongClick = true)
                HideableMenuItem(MenuItemType.SaveMht, R.string.menu_save_mht, Icons.Outlined.Save)
                HideableMenuItem(MenuItemType.Epub, R.string.menu_epub, Icons.AutoMirrored.Outlined.Article)
                HideableMenuItem(MenuItemType.SavePdf, R.string.menu_save_pdf, Icons.Outlined.PictureAsPdf)
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HideableMenuItem(MenuItemType.Epub, R.string.menu_epub, Icons.AutoMirrored.Outlined.Feed)
                HideableMenuItem(MenuItemType.ShareLink, R.string.menu_share_link, Icons.Outlined.Share, supportsLongClick = true)
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
                .then(
                    if (hideMode) Modifier
                    else Modifier.clickable {
                        currentShowContent = !currentShowContent
                        toggleContentMenu()
                    }
                ),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onBackground
        )
        if (effectiveShowContent) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HideableMenuItem(MenuItemType.PageAiActions, R.string.page_ai, iconResId = R.drawable.ic_robot)
                HideableMenuItem(MenuItemType.SplitScreen, R.string.split_screen, Icons.Outlined.ViewStream)
                HideableMenuItem(MenuItemType.Translate, R.string.translate, Icons.Outlined.Translate, supportsLongClick = true)
                HideableMenuItem(MenuItemType.VerticalRead, R.string.vertical_read, Icons.Outlined.ViewColumn)
                HideableMenuItem(MenuItemType.ReaderMode, R.string.reader_mode, Icons.AutoMirrored.Outlined.ChromeReaderMode)
                val touchRes = if (isTouchPaginationEnabled) R.drawable.ic_touch_enabled else R.drawable.ic_touch_disabled
                HideableMenuItem(MenuItemType.TouchSetting, R.string.touch_area_setting, iconResId = touchRes, supportsLongClick = true)
            }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val ttsRes = if (isSpeaking) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver
                HideableMenuItem(MenuItemType.Tts, R.string.menu_tts, ttsRes, supportsLongClick = true)
                val invertRes = if (hasInvertedColor) Icons.Outlined.InvertColorsOff else Icons.Outlined.InvertColors
                HideableMenuItem(MenuItemType.InvertColor, R.string.menu_invert_color, invertRes)
                val whiteRes = if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                HideableMenuItem(MenuItemType.WhiteBknd, R.string.white_background, iconResId = whiteRes)
                val blackRes = if (blackFont) Icons.TwoTone.Copyright else Icons.Outlined.Copyright
                HideableMenuItem(MenuItemType.BlackFont, R.string.black_font, blackRes)
                val boldRes = if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                HideableMenuItem(MenuItemType.BoldFont, R.string.bold_font, iconResId = boldRes, supportsLongClick = true)
                HideableMenuItem(MenuItemType.FontSize, R.string.font_size, Icons.Outlined.FormatSize)
            }
        } else {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HideableMenuItem(MenuItemType.Translate, R.string.translate, Icons.Outlined.Translate, supportsLongClick = true)
                HideableMenuItem(MenuItemType.ReaderMode, R.string.reader_mode, Icons.AutoMirrored.Outlined.ChromeReaderMode)
                val whiteRes = if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
                HideableMenuItem(MenuItemType.WhiteBknd, R.string.white_background, iconResId = whiteRes)
                val boldRes = if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
                HideableMenuItem(MenuItemType.BoldFont, R.string.bold_font, iconResId = boldRes)
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
                HideableMenuItem(MenuItemType.AudioOnly, R.string.audio_only_mode, audioOnlyIcon)
            }
            HideableMenuItem(MenuItemType.Search, R.string.menu_other_searchSite, Icons.Outlined.Search)
            HideableMenuItem(MenuItemType.Download, R.string.menu_download, Icons.Outlined.Download)
            HideableMenuItem(MenuItemType.ToolbarSetting, R.string.toolbar_icons, Icons.Outlined.Straighten)
            HideableMenuItem(MenuItemType.QuickToggle, R.string.menu_quickToggle, Icons.Outlined.SettingsSuggest)
            HideableMenuItem(MenuItemType.SiteSettings, R.string.site_settings, Icons.Outlined.Tune)
            HideableMenuItem(MenuItemType.Settings, R.string.settings, Icons.Outlined.Settings, supportsLongClick = true)
        }
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
