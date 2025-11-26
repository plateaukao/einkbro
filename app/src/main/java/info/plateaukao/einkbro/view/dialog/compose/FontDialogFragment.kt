package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.launch

class FontDialogFragment(
    private val onFontTypeChanged: () -> Unit
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                val customFontName = remember { mutableStateOf(config.customFontInfo?.name.orEmpty()) }
                val fontSizeState = remember { mutableIntStateOf(config.fontSize) }
                config.registerOnSharedPreferenceChangeListener { _, key ->
                    if (key == ConfigManager.K_CUSTOM_FONT) {
                        customFontName.value = config.customFontInfo?.name.orEmpty()
                    }
                }
                MainFontDialog(
                    selectedFontSizeValue = fontSizeState.value,
                    customFontSizeValue = config.customFontSize,
                    selectedFontType = config.fontType,
                    customFontName = customFontName.value,
                    onFontSizeClick = {
                        config.fontSize = it
                        fontSizeState.value = it
                        dismiss()
                    },
                    onFontTypeClick = {
                        if (it == FontType.CUSTOM && config.customFontInfo == null) {
                            onFontTypeChanged()
                        } else {
                            config.fontType = it
                            dismiss()
                        }
                    },
                    onFontTypeChanged = {
                        onFontTypeChanged()
                        config.registerOnSharedPreferenceChangeListener { _, key ->
                            if (key == ConfigManager.K_CUSTOM_FONT) {
                                customFontName.value = config.customFontInfo?.name.orEmpty()
                            }
                        }
                    },
                    onCustomFontSizeClick = {
                        lifecycleScope.launch {
                            TextInputDialog(
                                requireContext(),
                                getString(R.string.custom_scale),
                                getString(R.string.custom_scale_desc),
                                config.customFontSize.toString()
                            ).show()?.toIntOrNull()?.let {
                                config.fontSize = it
                                config.customFontSize = it
                                fontSizeState.value = it
                            }
                        }
                    },
                    okAction = { dismiss() },
                )
            }
        }
    }
}

val fontSizeList1 = listOf(
    75,
    90,
    100,
    110,
)

val fontSizeList2 = listOf(
    125,
    150,
    175,
    200,
)

val fontSizeList3 = listOf(
    -1 // Custom
)

@Composable
fun MainFontDialog(
    selectedFontSizeValue: Int,
    customFontSizeValue: Int,
    selectedFontType: FontType,
    customFontName: String,
    onFontSizeClick: (Int) -> Unit,
    onFontTypeClick: (FontType) -> Unit,
    onFontTypeChanged: () -> Unit,
    onCustomFontSizeClick: () -> Unit,
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
        Row {
            fontSizeList3.map { fontSize ->
                val isSelect = selectedFontSizeValue !in fontSizeList1 && selectedFontSizeValue !in fontSizeList2
                val text = if (customFontSizeValue !in fontSizeList1 && customFontSizeValue !in fontSizeList2) {
                    "$customFontSizeValue%"
                } else {
                    stringResource(id = R.string.custom_scale)
                }
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = text,
                ) {
                    onCustomFontSizeClick()
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
                if (fontType != FontType.CUSTOM) {
                    SelectableText(
                        modifier = Modifier.padding(horizontal = 1.dp, vertical = 5.dp),
                        selected = isSelect,
                        text = stringResource(id = fontType.resId),
                    ) {
                        onFontTypeClick(fontType)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fontName = customFontName.ifBlank {
                            stringResource(id = R.string.nothing)
                        }
                        SelectableText(
                            modifier = Modifier
                                .padding(horizontal = 1.dp, vertical = 5.dp)
                                .weight(1f),
                            selected = isSelect,
                            text = stringResource(id = fontType.resId) + " ($fontName)",
                        ) {
                            onFontTypeClick(fontType)
                        }
                        IconButton(
                            onClick = onFontTypeChanged,
                        ) {
                            Icon(
                                tint = MaterialTheme.colors.onBackground,
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(id = R.string.settings),
                            )
                        }
                    }
                }
            }
        }
        DialogOkButtonBar(
            okAction = okAction,
        )
    }
}

@Composable
fun DialogOkButtonBar(
    okAction: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
    ) {
        HorizontalSeparator()
        TextButton(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxWidth(),
            onClick = okAction
        ) {
            Text(
                stringResource(id = android.R.string.ok),
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Preview(widthDp = 350)
@Composable
fun PreviewMainFontDialog() {
    MyTheme {
        MainFontDialog(
            selectedFontSizeValue = 100,
            customFontSizeValue = 120,
            selectedFontType = FontType.SYSTEM_DEFAULT,
            customFontName = "Noto Sans CJK TC",
            onFontSizeClick = {},
            onFontTypeClick = {},
            onFontTypeChanged = {},
            onCustomFontSizeClick = {},
            okAction = {},
        )
    }
}