package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme

class FastToggleDialogFragment(
    val extraAction: () -> Unit
): ComposeDialogFragment(){
    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            FastToggleItemList(config) { needExtraAction ->
                if (needExtraAction) extraAction()
                dialog?.dismiss()
            }
        }
    }
}

@Composable
fun FastToggleItemList(config: ConfigManager? = null, onClicked: ((Boolean) -> Unit)) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        ToggleItem(state = config?.isIncognitoMode ?: false, titleResId = R.string.setting_title_incognito, iconResId=R.drawable.ic_incognito) {
            config?.let { it.isIncognitoMode = it.isIncognitoMode.not() }
            onClicked(true)
        }
        ToggleItem(state = config?.adBlock ?: false, titleResId = R.string.setting_title_adblock, iconResId=R.drawable.icon_block) {
            config?.let { it.adBlock = it.adBlock.not() }
            onClicked(true)
        }
        ToggleItem(state = config?.enableJavascript ?: false, titleResId = R.string.setting_title_javascript, iconResId=R.drawable.icon_java) {
            config?.let { it.enableJavascript= it.enableJavascript.not() }
            onClicked(true)
        }
        ToggleItem(state = config?.cookies ?: false, titleResId = R.string.setting_title_cookie, iconResId=R.drawable.icon_cookie) {
            config?.let { it.cookies = it.cookies.not() }
            onClicked(true)
        }
        ToggleItem(state = config?.saveHistory ?: false, titleResId = R.string.history, iconResId=R.drawable.ic_history) {
            config?.let { it.saveHistory= it.saveHistory.not() }
            onClicked(false)
        }

        Divider(thickness = 1.dp, color = MaterialTheme.colors.onBackground)

        ToggleItem(state = config?.shareLocation ?: false, titleResId = R.string.location, iconResId=R.drawable.ic_location) {
            config?.let { it.shareLocation= it.shareLocation.not() }
            onClicked(false)
        }
        ToggleItem(state = config?.volumePageTurn ?: false, titleResId = R.string.volume_page_turn, iconResId=R.drawable.ic_volume) {
            config?.let { it.volumePageTurn= it.volumePageTurn.not() }
            onClicked(false)
        }
        ToggleItem(state = config?.continueMedia ?: false, titleResId = R.string.media_continue, iconResId=R.drawable.ic_media_continue) {
            config?.let { it.continueMedia= it.continueMedia.not() }
            onClicked(false)
        }
        ToggleItem(state = config?.desktop ?: false, titleResId = R.string.desktop_mode, iconResId=R.drawable.icon_desktop) {
            config?.let { it.desktop= it.desktop.not() }
            onClicked(false)
        }
    }
}

@Composable
fun ToggleItem(
    state: Boolean,
    titleResId: Int,
    iconResId: Int,
    isEnabled: Boolean = true,
    onClicked: (Boolean)-> Unit,
) {
    var currentState by remember { mutableStateOf(state) }

    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(46.dp)
            .padding(8.dp)
            .clickable {
                currentState = !currentState
                if (isEnabled) { onClicked(currentState) }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = currentState,
            enabled = isEnabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.onBackground,
                uncheckedColor = MaterialTheme.colors.onBackground,
                checkmarkColor = MaterialTheme.colors.background,
            ),
            onCheckedChange = {
                currentState = !currentState
                if (isEnabled) { onClicked(currentState) }
            }
        )

        if (iconResId > 0) {
            Icon(
                painter = painterResource(id = iconResId), contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                tint = MaterialTheme.colors.onBackground
            )
        }
        Spacer(modifier = Modifier.width(6.dp).fillMaxHeight())
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = stringResource(id = titleResId),
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Preview
@Composable
private fun PreviewItem() {
    MyTheme {
        ToggleItem(true, R.string.title, R.drawable.ic_location) {}
    }
}

@Preview
@Composable
private fun PreviewItemList() {
    MyTheme {
        FastToggleItemList(onClicked = {})
    }
}
