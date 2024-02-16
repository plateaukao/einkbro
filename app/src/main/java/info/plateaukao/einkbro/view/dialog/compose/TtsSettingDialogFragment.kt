package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import java.util.Locale

class TtsSettingDialogFragment(
    private val gotoSettingAction: () -> Unit,
    private val showLocaleDialog: () -> Unit
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                MainTtsSettingDialog(
                    selectedLocale = config.ttsLocale,
                    selectedSpeedValue = config.ttsSpeedValue,
                    onSpeedValueClick = { config.ttsSpeedValue = it; dismiss() },
                    okAction = { dismiss() },
                    gotoSettingAction = gotoSettingAction,
                    showLocaleDialog = showLocaleDialog,
                )
            }
        }
    }
}

private val speedRateValueList = listOf(
    50,
    75,
    100,
    125,
)

private val speedRateValueList2 = listOf(
    150,
    175,
    200
)

@Composable
private fun MainTtsSettingDialog(
    selectedLocale: Locale,
    selectedSpeedValue: Int,
    onSpeedValueClick: (Int) -> Unit,
    gotoSettingAction: () -> Unit,
    okAction: () -> Unit,
    showLocaleDialog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .width(IntrinsicSize.Max)
    ) {
        Text(
            stringResource(id = R.string.setting_tts_locale),
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        SelectableText(
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
            selected = true, text = selectedLocale.displayName
        ) {
            showLocaleDialog()
        }
        Text(
            stringResource(id = R.string.read_speed),
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        Row {
            speedRateValueList.map { speedRate ->
                val isSelect = selectedSpeedValue == speedRate
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = "$speedRate%",
                ) {
                    onSpeedValueClick(speedRate)
                }
            }
        }
        Row {
            speedRateValueList2.map { speedRate ->
                val isSelect = selectedSpeedValue == speedRate
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = "$speedRate%",
                ) {
                    onSpeedValueClick(speedRate)
                }
            }
        }
        TtsDialogButtonBar(
            gotoSettingAction = gotoSettingAction,
            okAction = okAction,
        )
    }
}

@Composable
fun TtsDialogButtonBar(
    gotoSettingAction: () -> Unit,
    okAction: () -> Unit,
) {
    Column {
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = gotoSettingAction
            ) {
                Text(
                    stringResource(id = R.string.settings),
                    color = MaterialTheme.colors.onBackground
                )
            }
            VerticalSeparator()
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = okAction
            ) {
                Text(
                    stringResource(id = android.R.string.ok),
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Preview(widthDp = 600)
@Composable
fun PreviewMainTtsDialog() {
    MyTheme {
        MainTtsSettingDialog(
            selectedLocale = Locale.US,
            selectedSpeedValue = 100,
            onSpeedValueClick = {},
            okAction = {},
            gotoSettingAction = {},
            showLocaleDialog = {},
        )
    }
}