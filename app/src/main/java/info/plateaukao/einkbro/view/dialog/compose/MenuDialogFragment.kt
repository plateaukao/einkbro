package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
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
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.BlackFont
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.BoldFont
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CloseTab
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CopyLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Download
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.FontSize
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenEpub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenHome
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.QuickToggle
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Quit
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ReaderMode
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ReceiveData
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SaveBookmark
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SaveEpub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SavePdf
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Search
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SendLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SetHome
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Settings
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ShareLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Shortcut
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SplitScreen
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ToolbarSetting
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.TouchSetting
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Translate
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Tts
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.VerticalRead
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.WhiteBknd
import org.koin.android.ext.android.inject

class MenuDialogFragment(
    private val itemClicked: (MenuItemType) -> Unit
) : ComposeDialogFragment() {
    private val ttsManager: TtsManager by inject()

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            MenuItems(
                config.whiteBackground, config.boldFontStyle,
                config.blackFontStyle, ttsManager.isSpeaking()
            ) { item ->
                dialog?.dismiss()
                itemClicked(item)
            }
        }
    }
}

enum class MenuItemType {
    Tts, QuickToggle, OpenHome, CloseTab, Quit,
    SplitScreen, Translate, VerticalRead, ReaderMode, TouchSetting, ToolbarSetting,
    ReceiveData, SendLink, ShareLink, OpenWith, CopyLink, Shortcut,
    SetHome, SaveBookmark, OpenEpub, SaveEpub, SavePdf,
    FontSize, WhiteBknd, BoldFont, Search, Download, Settings, BlackFont
}

@Composable
private fun MenuItems(
    hasWhiteBkd: Boolean,
    boldFont: Boolean,
    blackFont: Boolean,
    isSpeaking: Boolean,
    onClicked: (MenuItemType) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End
        ) {
            val ttsRes = if (isSpeaking) R.drawable.ic_stop else R.drawable.ic_tts
            MenuItem(R.string.menu_tts, ttsRes) { onClicked(Tts) }
            MenuItem(
                R.string.menu_quickToggle,
                R.drawable.ic_quick_toggle
            ) { onClicked(QuickToggle) }
            MenuItem(R.string.menu_openFav, R.drawable.ic_home) { onClicked(OpenHome) }
            MenuItem(R.string.menu_closeTab, R.drawable.icon_close) { onClicked(CloseTab) }
            MenuItem(R.string.menu_quit, R.drawable.icon_exit) { onClicked(Quit) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.toolbar_setting, R.drawable.ic_toolbar) { onClicked(ToolbarSetting) }
            MenuItem(R.string.split_screen, R.drawable.ic_split_screen) { onClicked(SplitScreen) }
            MenuItem(R.string.translate, R.drawable.ic_translate) { onClicked(Translate) }
            MenuItem(
                R.string.vertical_read,
                R.drawable.ic_vertical_read
            ) { onClicked(VerticalRead) }
            MenuItem(R.string.reader_mode, R.drawable.ic_reader) { onClicked(ReaderMode) }
            MenuItem(R.string.touch_area_setting, R.drawable.ic_touch_disabled) {
                onClicked(
                    TouchSetting
                )
            }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_sc, R.drawable.link_plus) { onClicked(Shortcut) }
            MenuItem(R.string.menu_receive, R.drawable.ic_receive) { onClicked(ReceiveData) }
            MenuItem(R.string.menu_send_link, R.drawable.ic_send) { onClicked(SendLink) }
            MenuItem(R.string.menu_open_with, R.drawable.icon_exit) { onClicked(OpenWith) }
            MenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
            MenuItem(R.string.menu_share_link, R.drawable.icon_menu_share) { onClicked(ShareLink) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_fav, R.drawable.ic_home) { onClicked(SetHome) }
            MenuItem(
                R.string.menu_save_bookmark,
                R.drawable.ic_bookmark
            ) { onClicked(SaveBookmark) }
            MenuItem(R.string.menu_open_epub, R.drawable.ic_open_epub) { onClicked(OpenEpub) }
            MenuItem(R.string.menu_save_epub, R.drawable.ic_book) { onClicked(SaveEpub) }
            MenuItem(R.string.menu_save_pdf, R.drawable.ic_pdf) { onClicked(SavePdf) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_download, R.drawable.icon_download) { onClicked(Download) }
            MenuItem(R.string.menu_other_searchSite, R.drawable.icon_search) { onClicked(Search) }
            val whiteRes =
                if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
            MenuItem(R.string.white_background, whiteRes) { onClicked(WhiteBknd) }
            val blackRes =
                if (blackFont) R.drawable.ic_black_font_on else R.drawable.ic_black_font_off
            MenuItem(R.string.black_font, blackRes) { onClicked(BlackFont) }
            val boldRes = if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
            MenuItem(R.string.bold_font, boldRes) { onClicked(BoldFont) }
            MenuItem(R.string.font_size, R.drawable.icon_size) { onClicked(FontSize) }
            MenuItem(R.string.settings, R.drawable.icon_settings) { onClicked(Settings) }
        }
    }
}

@Composable
fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    isLargeType: Boolean = false,
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

    val fontSize = if (configuration.screenWidthDp > 500) 10.sp else 8.sp
    Column(
        modifier = Modifier
            .width(width)
            .height(if (isLargeType) 80.dp else 70.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onClicked() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconResId), contentDescription = null,
            modifier = Modifier
                .size(if (isLargeType) 55.dp else 44.dp)
                .padding(horizontal = 6.dp),
            tint = MaterialTheme.colors.onBackground
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .height(31.dp)
                .offset(y = (-5).dp),
            text = stringResource(id = titleResId),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = fontSize,
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
            MenuItem(R.string.title_appData, R.drawable.ic_copy) {}
            MenuItem(R.string.title, R.drawable.ic_location) {}
        }
    }
}

@Preview
@Composable
private fun PreviewMenuItems() {
    MyTheme {
        MenuItems(hasWhiteBkd = false, boldFont = false, blackFont = false, isSpeaking = false) {}
    }
}
