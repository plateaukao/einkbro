package info.plateaukao.einkbro.view.dialog.compose

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.DataListActivity
import info.plateaukao.einkbro.activity.WhiteListType
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.SaveHistoryMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.view.compose.MyTheme

class FastToggleDialogFragment(
    val extraAction: () -> Unit,
) : ComposeDialogFragment() {
    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            FastToggleItemList(requireContext(), config) { needExtraAction ->
                if (needExtraAction) extraAction()
                dialog?.dismiss()
            }
        }
    }
}

@Composable
fun FastToggleItemList(context: Context, config: ConfigManager, onClicked: ((Boolean) -> Unit)) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        ToggleItem(
            state = config.isIncognitoMode,
            titleResId = R.string.setting_title_incognito, iconResId = R.drawable.ic_incognito
        ) {
            config::isIncognitoMode.toggle()
            onClicked(true)
        }
        ToggleItem(
            state = config.adBlock,
            titleResId = R.string.setting_title_adblock, iconResId = R.drawable.ic_block,
            onEditAction = {
                context.startActivity(
                    DataListActivity.createIntent(
                        context,
                        WhiteListType.Adblock
                    )
                )
            }
        ) {
            config::adBlock.toggle()
            onClicked(true)
        }
        ToggleItem(
            state = config.enableJavascript,
            titleResId = R.string.setting_title_javascript, iconResId = R.drawable.icon_java,
            onEditAction = {
                context.startActivity(
                    DataListActivity.createIntent(
                        context,
                        WhiteListType.Javascript
                    )
                )
            }
        ) {
            config::enableJavascript.toggle()
            onClicked(true)
        }
        ToggleItem(
            state = config.cookies,
            titleResId = R.string.setting_title_cookie, iconResId = R.drawable.icon_cookie,
            onEditAction = {
                context.startActivity(DataListActivity.createIntent(context, WhiteListType.Cookie))
            }
        ) {
            config::cookies.toggle()
            onClicked(true)
        }
        ToggleItem(
            state = config.isSaveHistoryOn(),
            titleResId = R.string.history, iconResId = R.drawable.ic_history
        ) { on ->
            if (on) {
                config.saveHistoryMode = config.toggledSaveHistoryMode
            } else {
                config.toggledSaveHistoryMode = config.saveHistoryMode
                config.saveHistoryMode = SaveHistoryMode.DISABLED
            }
            onClicked(false)
        }

        Divider(thickness = 1.dp, color = MaterialTheme.colors.onBackground)

        ToggleItem(
            state = config.shareLocation,
            titleResId = R.string.location, iconResId = R.drawable.ic_location
        ) {
            config::shareLocation.toggle()
            onClicked(false)
        }
        ToggleItem(
            state = config.volumePageTurn,
            titleResId = R.string.volume_page_turn, iconResId = R.drawable.ic_volume
        ) {
            config::volumePageTurn.toggle()
            onClicked(false)
        }
        ToggleItem(
            state = config.continueMedia,
            titleResId = R.string.media_continue, iconResId = R.drawable.ic_media_continue
        ) {
            config::continueMedia.toggle()
            onClicked(false)
        }
        ToggleItem(
            state = config.desktop,
            titleResId = R.string.desktop_mode, iconResId = R.drawable.icon_desktop
        ) {
            config::desktop.toggle()
            onClicked(false)
        }
    }
}

@Composable
fun ToggleItem(
    state: Boolean,
    titleResId: Int,
    iconResId: Int,
    imageVector: ImageVector? = null,
    isEnabled: Boolean = true,
    onEditAction: (() -> Unit)? = null,
    onClicked: (Boolean) -> Unit,
) {
    var currentState by remember { mutableStateOf(state) }

    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(46.dp)
            .padding(4.dp)
            .clickable {
                if (isEnabled) {
                    currentState = !currentState
                    onClicked(currentState)
                }
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
                if (isEnabled) {
                    currentState = !currentState
                    onClicked(currentState)
                }
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
        if (imageVector != null) {
            Icon(
                imageVector = imageVector, contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                tint = MaterialTheme.colors.onBackground
            )
        }
        Spacer(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
        )
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = stringResource(id = titleResId),
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground
        )
        if (onEditAction != null) {
            Icon(
                painter = painterResource(id = R.drawable.icon_edit), contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight()
                    .clickable { onEditAction() },
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Preview
@Composable
private fun PreviewItem() {
    MyTheme {
        ToggleItem(true, R.string.title, R.drawable.ic_location, onEditAction = {}) {}
    }
}

@Preview
@Composable
private fun PreviewItemList() {
    MyTheme {
        //FastToggleItemList(config = ConfigManager(), onClicked = {})
    }
}
