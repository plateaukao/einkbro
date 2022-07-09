package de.baumann.browser.view.dialog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
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
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.FontType
import de.baumann.browser.view.compose.MyTheme
import de.baumann.browser.view.compose.SelectableText

class FontDialogFragment(
    private val onFontCustomizeClick: () -> Unit
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                MainFontDialog(
                    selectedFontSizeValue = config.fontSize,
                    selectedFontType = config.fontType,
                    onFontSizeClick = {
                        config.fontSize = it
                        dismiss()
                    },
                    onFontTypeClick = {
                        config.fontType = it
                        dismiss()
                    },
                    onFontCustomizeClick = onFontCustomizeClick,
                    okAction = { dismiss() },
                )
            }
        }
    }
}

private val fontSizeList1 = listOf(
    75,
    90,
    100,
    110,
)

private val fontSizeList2 = listOf(
    125,
    150,
    175,
    200,
)

@Composable
private fun MainFontDialog(
    selectedFontSizeValue: Int,
    selectedFontType: FontType,
    onFontSizeClick: (Int) -> Unit,
    onFontTypeClick: (FontType) -> Unit,
    onFontCustomizeClick: () -> Unit,
    okAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .width(IntrinsicSize.Max)
    ) {
        Text(
            stringResource(id = R.string.font_size),
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        Row {
            fontSizeList1.map { fontSize ->
                val isSelect = selectedFontSizeValue == fontSize
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = "$fontSize%",
                ) {
                    onFontSizeClick(fontSize)
                }
            }
        }
        Row {
            fontSizeList2.map { fontSize ->
                val isSelect = selectedFontSizeValue == fontSize
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = "$fontSize%",
                ) {
                    onFontSizeClick(fontSize)
                }
            }
        }
        Text(
            stringResource(id = R.string.font_type),
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        Column {
            FontType.values().map { fontType ->
                val isSelect = fontType == selectedFontType
                SelectableText(
                    modifier = Modifier.padding(horizontal = 1.dp, vertical = 5.dp),
                    selected = isSelect,
                    text = stringResource(id = fontType.resId),
                ) {
                    onFontTypeClick(fontType)
                }
            }
        }
        FontDialogButtonBar(
            okAction = okAction,
            editFontAction = onFontCustomizeClick
        )
    }
}

@Composable
fun FontDialogButtonBar(
    okAction: () -> Unit,
    editFontAction: () -> Unit
) {
    Column {
       HorizontalSeparator()
        Row(
            modifier = Modifier.wrapContentHeight().fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = editFontAction
            ) {
                Text(stringResource(id = R.string.edit_custom_font), color = MaterialTheme.colors.onBackground)
            }
            VerticalSeparator()
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick =okAction
            ) {
                Text(stringResource(id = android.R.string.ok), color = MaterialTheme.colors.onBackground)
            }
        }
    }
}

@Preview(widthDp = 600)
@Composable
fun PreviewMainFontDialog() {
    MyTheme {
        MainFontDialog(125, FontType.CUSTOM, {}, {}, {}, {})
    }
}