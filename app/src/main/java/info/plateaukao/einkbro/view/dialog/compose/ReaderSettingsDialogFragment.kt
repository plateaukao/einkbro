package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reader mode layout settings, shown on long-clicking the reader mode icon.
 * All changes are persisted and applied to the current page immediately.
 */
class ReaderSettingsDialogFragment(
    private val onSettingChanged: () -> Unit,
    private val onFontConfigClick: () -> Unit,
) : ComposeDialogFragment() {

    @Composable
    override fun Content() {
        ReaderSettingsContent(
            initPageMargin = config.display.paddingForReaderMode,
            initLineSpacing = config.display.readerLineSpacing,
            initTwoColumn = config.display.readerTwoColumnInLandscape,
            onPageMarginChanged = {
                config.display.paddingForReaderMode = it
                onSettingChanged()
            },
            onLineSpacingChanged = {
                config.display.readerLineSpacing = it
                onSettingChanged()
            },
            onTwoColumnChanged = {
                config.display.readerTwoColumnInLandscape = it
                onSettingChanged()
            },
            onFontConfigClick = {
                dismiss()
                onFontConfigClick()
            },
        )
    }
}

@Composable
fun ReaderSettingsContent(
    initPageMargin: Int,
    initLineSpacing: Int,
    initTwoColumn: Boolean,
    onPageMarginChanged: (Int) -> Unit,
    onLineSpacingChanged: (Int) -> Unit,
    onTwoColumnChanged: (Boolean) -> Unit,
    onFontConfigClick: () -> Unit,
) {
    var pageMargin by remember { mutableFloatStateOf(initPageMargin.toFloat()) }
    var lineSpacing by remember { mutableFloatStateOf(initLineSpacing.toFloat()) }
    var twoColumn by remember { mutableStateOf(initTwoColumn) }

    Column(
        modifier = Modifier
            .width(300.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reader_settings),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6,
            )
            IconButton(onClick = onFontConfigClick) {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = stringResource(R.string.font_size),
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        }

        HorizontalSeparator()

        Text(
            text = "${stringResource(R.string.page_margin)}: ${pageMargin.roundToInt()}px",
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 10.dp),
        )
        Slider(
            value = pageMargin,
            valueRange = 0f..100f,
            steps = 19,
            onValueChange = {
                pageMargin = it
                onPageMarginChanged(it.roundToInt())
            },
        )

        Text(
            text = "${stringResource(R.string.line_spacing)}: " +
                    String.format(Locale.ROOT, "%.1f", lineSpacing / 10f),
            color = MaterialTheme.colors.onBackground,
        )
        Slider(
            value = lineSpacing,
            valueRange = 10f..25f,
            steps = 14,
            onValueChange = {
                lineSpacing = it
                onLineSpacingChanged(it.roundToInt())
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.two_column_in_landscape),
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = twoColumn,
                onCheckedChange = {
                    twoColumn = it
                    onTwoColumnChanged(it)
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewReaderSettingsContent() {
    MyTheme {
        ReaderSettingsContent(
            initPageMargin = 10,
            initLineSpacing = 15,
            initTwoColumn = false,
            onPageMarginChanged = {},
            onLineSpacingChanged = {},
            onTwoColumnChanged = {},
            onFontConfigClick = {},
        )
    }
}
