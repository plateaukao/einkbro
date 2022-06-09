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
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager

class MenuDialogFragment(): ComposeDialogFragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupDialog()

        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    MenuItems()
                }
            }
        }
    }
}

@Composable
fun MenuItems() {
    Column (
        modifier = Modifier
            .wrapContentHeight()
   ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(titleResId = R.string.menu_quickToggle, iconResId = R.drawable.ic_quick_toggle, onClicked = {})
            MenuItem(titleResId = R.string.menu_openFav, iconResId = R.drawable.ic_home, onClicked = {})
            MenuItem(titleResId = R.string.menu_closeTab, iconResId = R.drawable.icon_close, onClicked = {})
            MenuItem(titleResId = R.string.menu_quit, iconResId = R.drawable.icon_exit, onClicked = {})
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(titleResId = R.string.split_screen, iconResId = R.drawable.ic_split_screen, onClicked = {})
            MenuItem(titleResId = R.string.translate, iconResId = R.drawable.ic_translate, onClicked = {})
            MenuItem(titleResId = R.string.vertical_read, iconResId = R.drawable.ic_vertical_read, onClicked = {})
            MenuItem(titleResId = R.string.reader_mode, iconResId = R.drawable.ic_reader, onClicked = {})
            MenuItem(titleResId = R.string.touch_area_setting, iconResId = R.drawable.ic_touch_disabled, onClicked = {})
            MenuItem(titleResId = R.string.toolbar_setting, iconResId = R.drawable.ic_toolbar, onClicked = {})
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(titleResId = R.string.menu_receive, iconResId = R.drawable.ic_receive, onClicked = {})
            MenuItem(titleResId = R.string.menu_send_link, iconResId = R.drawable.ic_send, onClicked = {})
            MenuItem(titleResId = R.string.menu_share_link, iconResId = R.drawable.icon_menu_share, onClicked = {})
            MenuItem(titleResId = R.string.menu_open_with, iconResId = R.drawable.icon_exit, onClicked = {})
            MenuItem(titleResId = R.string.copy_link, iconResId = R.drawable.ic_copy, onClicked = {})
            MenuItem(titleResId = R.string.menu_sc, iconResId = R.drawable.link_plus, onClicked = {})
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(titleResId = R.string.menu_fav, iconResId = R.drawable.ic_home, onClicked = {})
            MenuItem(titleResId = R.string.menu_save_bookmark, iconResId = R.drawable.ic_bookmark, onClicked = {})
            MenuItem(titleResId = R.string.menu_open_epub, iconResId = R.drawable.ic_open_epub, onClicked = {})
            MenuItem(titleResId = R.string.menu_save_epub, iconResId = R.drawable.ic_book, onClicked = {})
            MenuItem(titleResId = R.string.menu_save_pdf, iconResId = R.drawable.ic_pdf, onClicked = {})
        }
        HorizontalSeparator()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            MenuItem(titleResId = R.string.font_size, iconResId = R.drawable.icon_size, onClicked = {})
            MenuItem(titleResId = R.string.white_background, iconResId = R.drawable.ic_white_background, onClicked = {})
            MenuItem(titleResId = R.string.bold_font, iconResId = R.drawable.ic_bold_font, onClicked = {})
            MenuItem(titleResId = R.string.menu_other_searchSite, iconResId = R.drawable.icon_search, onClicked = {})
            MenuItem(titleResId = R.string.menu_download, iconResId = R.drawable.icon_download, onClicked = {})
            MenuItem(titleResId = R.string.settings, iconResId = R.drawable.icon_settings, onClicked = {})
        }
    }
}

@Composable
private fun firstRow() {

}
@Composable
fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    onClicked: ()-> Unit
) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .wrapContentHeight()
            .padding(5.dp)
            .clickable {
                onClicked()
            },
    ) {
        Icon(
            painter = painterResource(id = iconResId), contentDescription = null,
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .padding(5.dp)
                .fillMaxHeight(),
            tint = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier
            .height(2.dp)
            .width(0.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
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
        MenuItem(R.string.title, R.drawable.ic_location) {}
    }
}

@Preview
@Composable
private fun previewMenuItems() {
    AppCompatTheme {
        MenuItems()
    }
}
