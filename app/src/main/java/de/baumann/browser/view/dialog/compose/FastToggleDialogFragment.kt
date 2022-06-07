package de.baumann.browser.view.dialog.compose

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FastToggleDialogFragment(
    val okAction: () -> Unit
): DialogFragment(), KoinComponent {
    private val config: ConfigManager by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.apply {
            setStyle(STYLE_NO_TITLE, R.style.TouchAreaDialog)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        return ComposeView(requireContext()).apply {
            setContent {
                AppCompatTheme {
                    FastToggleItemList(config, dialog, okAction)
                }
            }
        }
    }
}

@Composable
fun FastToggleItemList(config: ConfigManager? = null, dialog: Dialog? = null, onClicked: (() -> Unit)) {
    Column {
        FastToggleItem(state = config?.isIncognitoMode ?: false, titleResId = R.string.setting_title_incognito, iconResId=R.drawable.ic_incognito) {
            config ?: return@FastToggleItem
            config.isIncognitoMode = config.isIncognitoMode.not()
            onClicked.invoke()
            dialog?.dismiss()
        }
        FastToggleItem(state = config?.adBlock ?: false, titleResId = R.string.setting_title_adblock, iconResId=R.drawable.icon_block) {
            config ?: return@FastToggleItem
            config.adBlock = config.adBlock.not()
            onClicked.invoke()
            dialog?.dismiss()
        }
        FastToggleItem(state = config?.enableJavascript ?: false, titleResId = R.string.setting_title_javascript, iconResId=R.drawable.icon_java) {
            config ?: return@FastToggleItem
            config.enableJavascript= config.enableJavascript.not()
            onClicked.invoke()
            dialog?.dismiss()
        }
        FastToggleItem(state = config?.cookies ?: false, titleResId = R.string.setting_title_cookie, iconResId=R.drawable.icon_cookie) {
            config ?: return@FastToggleItem
            config.cookies= config.cookies.not()
            onClicked.invoke()
            dialog?.dismiss()
        }
        FastToggleItem(state = config?.saveHistory ?: false, titleResId = R.string.history, iconResId=R.drawable.ic_history) {
            config ?: return@FastToggleItem ;   config.saveHistory= config.saveHistory.not() ; dialog?.dismiss()
        }

        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(color = MaterialTheme.colors.onPrimary))

        FastToggleItem(state = config?.shareLocation ?: false, titleResId = R.string.location, iconResId=R.drawable.ic_location) {
            config ?: return@FastToggleItem ;   config.shareLocation= config.shareLocation.not() ; dialog?.dismiss()
        }
        FastToggleItem(state = config?.volumePageTurn ?: false, titleResId = R.string.volume_page_turn, iconResId=R.drawable.ic_volume) {
            config ?: return@FastToggleItem ;   config.volumePageTurn= config.volumePageTurn.not() ; dialog?.dismiss()
        }
        FastToggleItem(state = config?.continueMedia ?: false, titleResId = R.string.media_continue, iconResId=R.drawable.ic_media_continue) {
            config ?: return@FastToggleItem ;   config.continueMedia= config.continueMedia.not() ; dialog?.dismiss()
        }
        FastToggleItem(state = config?.desktop ?: false, titleResId = R.string.desktop_mode, iconResId=R.drawable.icon_desktop) {
            config ?: return@FastToggleItem ;   config.desktop= config.desktop.not() ; dialog?.dismiss()
        }
    }
}

@Composable
fun FastToggleItem(
    state: Boolean,
    titleResId: Int,
    iconResId: Int,
    onClicked: (Boolean)-> Unit
) {
    var currentState by remember { mutableStateOf(state)}

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .height(46.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = currentState, onCheckedChange = { boolean ->
            currentState = boolean
            onClicked(boolean)
        })

        Icon(
            painter = painterResource(id = iconResId), contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            tint = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.width(6.dp).fillMaxHeight())
        Text(
            text = stringResource(id = titleResId),
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Preview
@Composable
fun previewItem() {
    AppCompatTheme {
        FastToggleItem(true, R.string.title, R.drawable.ic_location) {}
    }
}

@Preview
@Composable
fun previewItemList() {
    AppCompatTheme {
        FastToggleItemList(onClicked = {})
    }
}
