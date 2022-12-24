package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.*
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

class TtsSettingDialogFragment(
    private val gotoSettingAction: () -> Unit
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                MainTtsSettingDialog(
                    selectedSpeedValue = config.ttsSpeedValue,
                    onSpeedValueClick = { config.ttsSpeedValue = it; dismiss() },
                    okAction = { dismiss() },
                    gotoSettingAction = gotoSettingAction
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
    selectedSpeedValue: Int,
    onSpeedValueClick: (Int) -> Unit,
    gotoSettingAction: () -> Unit,
    okAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .width(IntrinsicSize.Max)
    ) {
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
//        Text(
//            stringResource(id = R.string.font_type),
//            modifier = Modifier.padding(vertical = 6.dp),
//            color = MaterialTheme.colors.onBackground,
//            style = MaterialTheme.typography.h6,
//            fontWeight = FontWeight.Bold,
//        )
//        Column {
//            FontType.values().map { fontType ->
//                val isSelect = fontType == selectedFontType
//                SelectableText(
//                    modifier = Modifier.padding(horizontal = 1.dp, vertical = 5.dp),
//                    selected = isSelect,
//                    text = stringResource(id = fontType.resId),
//                ) {
//                    onFontTypeClick(fontType)
//                }
//            }
//        }
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
                Text(stringResource(id = R.string.settings), color = MaterialTheme.colors.onBackground)
            }
            VerticalSeparator()
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = okAction
            ) {
                Text(stringResource(id = android.R.string.cancel), color = MaterialTheme.colors.onBackground)
            }
        }
    }
}

@Preview(widthDp = 600)
@Composable
fun PreviewMainTtsDialog() {
    MyTheme {
        MainTtsSettingDialog(
            selectedSpeedValue = 100,
            onSpeedValueClick = {},
            okAction = {},
            gotoSettingAction = {},
        )
    }
}