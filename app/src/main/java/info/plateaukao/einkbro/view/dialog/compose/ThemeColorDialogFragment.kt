package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.BorderStyle
import info.plateaukao.einkbro.preference.UiStyle
import info.plateaukao.einkbro.preference.UiTheme
import info.plateaukao.einkbro.preference.gradientSpec
import info.plateaukao.einkbro.preference.palette
import info.plateaukao.einkbro.view.ThemedBorders
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.UiThemeState
import info.plateaukao.einkbro.view.compose.dashedBorder
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
            onSelectStyle = { style ->
                config.display.uiStyle = style
                retintWindowFrame()
            },
            onToggleInvert = { inverted ->
                config.display.uiThemeInverted = inverted
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
    onSelectStyle: (UiStyle) -> Unit,
    onToggleInvert: (Boolean) -> Unit,
    onCustomColorPreview: (Color) -> Unit,
    onCustomColorPicked: (Color) -> Unit,
    onClose: () -> Unit,
) {
    val current by UiThemeState.current
    var showCustomWheel by remember { mutableStateOf(current == UiTheme.CUSTOM) }
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            stringResource(R.string.theme_section_color),
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        UiTheme.entries.chunked(4).forEach { rowThemes ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowThemes.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        isSelected = theme == current,
                        onClick = {
                            // tapping Custom again toggles the wheel visibility
                            showCustomWheel =
                                if (theme == UiTheme.CUSTOM && current == UiTheme.CUSTOM) {
                                    !showCustomWheel
                                } else {
                                    theme == UiTheme.CUSTOM
                                }
                            if (theme != current) onSelect(theme)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowThemes.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        if (current == UiTheme.CUSTOM && showCustomWheel) {
            CustomColorWheel(
                onPreview = onCustomColorPreview,
                onPicked = onCustomColorPicked,
            )
        }
        val currentStyle by UiThemeState.uiStyle
        Text(
            stringResource(R.string.setting_title_ui_style),
            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        UiStyle.entries.chunked(4).forEach { rowStyles ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowStyles.forEach { style ->
                    StyleSwatch(
                        uiStyle = style,
                        isSelected = style == currentStyle,
                        onClick = { onSelectStyle(style) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowStyles.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        val inverted by UiThemeState.inverted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.menu_invert_color),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = inverted,
                onCheckedChange = onToggleInvert,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray,
                    checkedTrackColor = MaterialTheme.colors.primary,
                ),
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
 * Color wheel picker: hue around the circle, saturation from center to edge,
 * plus one brightness slider. The derived palette keeps any pick readable.
 */
@Composable
private fun CustomColorWheel(
    onPreview: (Color) -> Unit,
    onPicked: (Color) -> Unit,
) {
    val initial = remember {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(UiThemeState.customColor.value.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var sat by remember { mutableFloatStateOf(initial[1]) }
    var value by remember { mutableFloatStateOf(initial[2]) }
    fun currentColor() =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    val wheelSize = 220.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .requiredSize(wheelSize)
                .pointerInput(Unit) {
                    fun update(x: Float, y: Float, finished: Boolean) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val radius = kotlin.math.min(cx, cy)
                        hue = ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()) + 360.0) % 360.0)
                            .toFloat()
                        sat = (kotlin.math.sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
                        onPreview(currentColor())
                        if (finished) onPicked(currentColor())
                    }
                    detectDragGestures(
                        onDragEnd = { onPicked(currentColor()) },
                    ) { change, _ ->
                        change.consume()
                        update(change.position.x, change.position.y, finished = false)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> 
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val radius = kotlin.math.min(cx, cy)
                        hue = ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()) + 360.0) % 360.0)
                            .toFloat()
                        sat = (kotlin.math.sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
                        onPreview(currentColor())
                        onPicked(currentColor())
                    }
                },
        ) {
            val radius = kotlin.math.min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                        Color.Blue, Color.Magenta, Color.Red,
                    ),
                    center = center,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color(0x00FFFFFF)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            // thumb at the current hue/saturation position
            val rad = Math.toRadians(hue.toDouble())
            val thumb = Offset(
                center.x + radius * sat * kotlin.math.cos(rad).toFloat(),
                center.y + radius * sat * kotlin.math.sin(rad).toFloat(),
            )
            drawCircle(Color.White, radius = 11.dp.toPx(), center = thumb)
            drawCircle(
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))),
                radius = 8.dp.toPx(),
                center = thumb,
            )
            drawCircle(
                Color.Black,
                radius = 11.dp.toPx(),
                center = thumb,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "B",
                modifier = Modifier.width(20.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground,
            )
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    onPreview(currentColor())
                },
                onValueChangeFinished = { onPicked(currentColor()) },
            )
        }
    }
}

@Composable
private fun StyleSwatch(
    uiStyle: UiStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = uiStyle.style
    val accent = MaterialTheme.colors.primary
    val shape = RoundedCornerShape(minOf(style.itemRadiusDp, 12f).dp)
    val previewModifier = Modifier
        .requiredSize(width = 52.dp, height = 32.dp)
        .clip(shape)
        .let { base ->
            when (style.borderStyle) {
                BorderStyle.NONE -> base.background(
                    lerp(
                        MaterialTheme.colors.background,
                        accent,
                        if (MaterialTheme.colors.isLight) 0.14f else 0.2f,
                    ),
                    shape,
                )
                BorderStyle.DASHED ->
                    base.dashedBorder(style.borderWidthDp.dp, minOf(style.itemRadiusDp, 12f).dp, accent)
                BorderStyle.DOUBLE -> base
                    .border(style.borderWidthDp.dp, accent, shape)
                    .padding(3.dp)
                    .border(style.borderWidthDp.dp, accent, shape)
                BorderStyle.GRADIENT, BorderStyle.GRADIENT_FLAT, BorderStyle.GRADIENT_DEEP -> {
                    val (start, end, hasBorder) = style.borderStyle.gradientSpec()!!
                    val filled = base.background(
                        Brush.linearGradient(
                            listOf(
                                lerp(MaterialTheme.colors.background, accent, start + 0.08f),
                                lerp(MaterialTheme.colors.background, accent, end + 0.12f),
                            ),
                        ),
                        shape,
                    )
                    if (hasBorder) filled.border(style.borderWidthDp.dp, accent, shape) else filled
                }
                BorderStyle.SOLID -> base.border(style.borderWidthDp.dp, accent, shape)
            }
        }
    val label = stringResource(uiStyle.titleResId) + uiStyle.labelSuffix
    Column(
        modifier = modifier
            .clickable(onClick = onClick, onClickLabel = label)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = previewModifier, contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = label,
                    tint = accent,
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
    modifier: Modifier = Modifier,
) {
    val darkMode = isAppInDarkTheme()
    val customColor by UiThemeState.customColor
    val palette = theme.palette(customColor)
    val swatchColor = if (darkMode) palette.accentDark else palette.accent
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(44.dp)
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
        ThemeColorContent(onSelect = {}, onSelectStyle = {}, onToggleInvert = {}, onCustomColorPreview = {}, onCustomColorPicked = {}, onClose = {})
    }
}
