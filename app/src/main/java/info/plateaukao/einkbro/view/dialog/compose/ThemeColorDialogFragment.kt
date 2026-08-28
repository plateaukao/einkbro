package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.UiTheme
import info.plateaukao.einkbro.preference.palette
import info.plateaukao.einkbro.view.ThemedBorders
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.UiThemeState
import info.plateaukao.einkbro.view.compose.isAppInDarkTheme

/**
 * Theme picker with color swatches. Selecting a swatch applies the theme
 * immediately (the whole app retints live); the dialog stays open so the
 * user can preview themes and closes with OK.
 */
class ThemeColorDialogFragment : ComposeDialogFragment() {
    init {
        shouldShowInCenter = true
    }

    @Composable
    override fun Content() {
        ThemeColorContent(
            onSelect = { theme ->
                config.display.uiTheme = theme
                retintWindowFrame()
            },
            onCustomColorPreview = { color ->
                // live preview while dragging the sliders; not yet persisted
                UiThemeState.customColor.value = color
            },
            onCustomColorPicked = { color ->
                config.display.customThemeColor = color.toArgb()
                retintWindowFrame()
            },
            onClose = { dismiss() },
        )
    }

    private fun retintWindowFrame() {
        dialog?.window?.setBackgroundDrawable(ThemedBorders.dialogFrame(requireContext()))
    }
}

@Composable
private fun ThemeColorContent(
    onSelect: (UiTheme) -> Unit,
    onCustomColorPreview: (Color) -> Unit,
    onCustomColorPicked: (Color) -> Unit,
    onClose: () -> Unit,
) {
    val current by UiThemeState.current
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
        Text(
            stringResource(R.string.setting_title_ui_theme),
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        UiTheme.entries.chunked(4).forEach { rowThemes ->
            Row {
                rowThemes.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        isSelected = theme == current,
                        onClick = { onSelect(theme) },
                    )
                }
            }
        }
        if (current == UiTheme.CUSTOM) {
            CustomColorSliders(
                onPreview = onCustomColorPreview,
                onPicked = onCustomColorPicked,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClose) {
                Text(
                    stringResource(android.R.string.ok),
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

/**
 * Minimal HSB color picker: three sliders. The palette (accent, background
 * tint, text shades) is derived from the picked color with contrast-safe
 * adjustments, so any pick stays readable.
 */
@Composable
private fun CustomColorSliders(
    onPreview: (Color) -> Unit,
    onPicked: (Color) -> Unit,
) {
    val customColor by UiThemeState.customColor
    val hsv = remember(Unit) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(customColor.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    fun currentColor() =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        listOf(
            Triple("H", hue / 360f) { v: Float -> hue = v * 360f },
            Triple("S", sat) { v: Float -> sat = v },
            Triple("B", value) { v: Float -> value = v },
        ).forEach { (label, sliderValue, update) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.width(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onBackground,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        update(it)
                        onPreview(currentColor())
                    },
                    onValueChangeFinished = { onPicked(currentColor()) },
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: UiTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val darkMode = isAppInDarkTheme()
    val customColor by UiThemeState.customColor
    val palette = theme.palette(customColor)
    val swatchColor = if (darkMode) palette.accentDark else palette.accent
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colors.onBackground
                    else MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (darkMode) Color.Black else Color.White,
                )
            }
        }
        Text(
            stringResource(theme.titleResId),
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Preview
@Composable
private fun PreviewThemeColorContent() {
    MyTheme {
        ThemeColorContent(onSelect = {}, onCustomColorPreview = {}, onCustomColorPicked = {}, onClose = {})
    }
}
