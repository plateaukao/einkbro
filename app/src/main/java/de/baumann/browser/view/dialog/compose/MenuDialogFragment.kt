package de.baumann.browser.view.dialog.compose

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.google.android.gms.tasks.Continuation
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.ViewUnit.dp
import de.baumann.browser.view.dialog.compose.MenuItemType.*
import java.io.Reader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MenuDialogFragment(
    private val itemClicked: (MenuItemType) -> Unit
): ComposeDialogFragment(){
    private var clickedItem = Canceled

    lateinit var composeView: ComposeView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupDialog()

        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    MenuItems(config.whiteBackground, config.boldFontStyle) { item ->
                        dialog?.dismiss()
                        itemClicked(item)
                    }
                }
            }
        }
    }
}

enum class MenuItemType {
    Canceled,
    QuickToggle, OpenHome, CloseTab, Quit,
    SplitScreen, Translate, VerticalRead, ReaderMode, TouchSetting, ToolbarSetting,
    ReceiveData, SendLink, ShareLink, OpenWith, CopyLink, Shortcut,
    SetHome, SaveBookmark, OpenEpub, SaveEpub, SavePdf,
    FontSize, WhiteBknd, BoldFont, Search, Download, Settings
}

@Composable
private fun MenuItems(hasWhiteBkd: Boolean, boldFont: Boolean, onClicked: (MenuItemType)-> Unit) {
    Column (
        modifier = Modifier.wrapContentHeight().width(IntrinsicSize.Max).padding(10.dp),
        horizontalAlignment = Alignment.End
   ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_quickToggle, R.drawable.ic_quick_toggle) { onClicked (QuickToggle) }
            MenuItem(R.string.menu_openFav, R.drawable.ic_home) { onClicked(OpenHome) }
            MenuItem(R.string.menu_closeTab, R.drawable.icon_close) { onClicked(CloseTab) }
            MenuItem(R.string.menu_quit, R.drawable.icon_exit) { onClicked(Quit) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.split_screen, R.drawable.ic_split_screen) { onClicked(SplitScreen) }
            MenuItem(R.string.translate, R.drawable.ic_translate) { onClicked(Translate) }
            MenuItem(R.string.vertical_read, R.drawable.ic_vertical_read) { onClicked(VerticalRead) }
            MenuItem(R.string.reader_mode, R.drawable.ic_reader) { onClicked(ReaderMode) }
            MenuItem(R.string.touch_area_setting, R.drawable.ic_touch_disabled) { onClicked(TouchSetting) }
            MenuItem(R.string.toolbar_setting, R.drawable.ic_toolbar) { onClicked(ToolbarSetting) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_receive, R.drawable.ic_receive) { onClicked(ReceiveData) }
            MenuItem(R.string.menu_send_link, R.drawable.ic_send) { onClicked(SendLink) }
            MenuItem(R.string.menu_share_link, R.drawable.icon_menu_share) { onClicked(ShareLink) }
            MenuItem(R.string.menu_open_with, R.drawable.icon_exit) { onClicked(OpenWith) }
            MenuItem(R.string.copy_link, R.drawable.ic_copy) { onClicked(CopyLink) }
            MenuItem(R.string.menu_sc, R.drawable.link_plus) { onClicked(Shortcut) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.menu_fav, R.drawable.ic_home) { onClicked(SetHome) }
            MenuItem(R.string.menu_save_bookmark, R.drawable.ic_bookmark) { onClicked(SaveBookmark) }
            MenuItem(R.string.menu_open_epub, R.drawable.ic_open_epub) { onClicked(OpenEpub) }
            MenuItem(R.string.menu_save_epub, R.drawable.ic_book) { onClicked(SaveEpub) }
            MenuItem(R.string.menu_save_pdf, R.drawable.ic_pdf) { onClicked(SavePdf) }
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(R.string.font_size, R.drawable.icon_size) { onClicked(FontSize) }
            val whiteRes = if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
            MenuItem(R.string.white_background, whiteRes) { onClicked(WhiteBknd) }
            val boldRes = if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
            MenuItem(R.string.bold_font, boldRes) { onClicked(BoldFont) }
            MenuItem(R.string.menu_other_searchSite, R.drawable.icon_search) { onClicked(Search) }
            MenuItem(R.string.menu_download, R.drawable.icon_download) { onClicked(Download) }
            MenuItem(R.string.settings, R.drawable.icon_settings) { onClicked(Settings) }
        }
    }
}

@Composable
private fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    onClicked: ()-> Unit
) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .wrapContentHeight()
            .clickable { onClicked() },
    ) {
        Icon(
            painter = painterResource(id = iconResId), contentDescription = null,
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .padding(top = 2.dp, start=8.dp, end=8.dp),
            tint = MaterialTheme.colors.onBackground
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            text = stringResource(id = titleResId),
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontSize = 10.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Preview
@Composable
private fun previewItem() {
    AppCompatTheme {
        Column {
            MenuItem(R.string.title_appData, R.drawable.ic_copy) {}
            MenuItem(R.string.title, R.drawable.ic_location) {}
        }
    }
}

@Preview
@Composable
private fun previewMenuItems() {
    AppCompatTheme {
        MenuItems(hasWhiteBkd = false, boldFont = false, {})
    }
}
