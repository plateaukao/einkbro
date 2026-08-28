package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.drawBehind
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
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.preference.UiFill
import info.plateaukao.einkbro.preference.UiTheme
import info.plateaukao.einkbro.preference.palette
import info.plateaukao.einkbro.view.ThemedBorders
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.UiThemeState
import info.plateaukao.einkbro.view.compose.dashedBorder
import info.plateaukao.einkbro.view.compose.stampShape
import info.plateaukao.einkbro.view.compose.tonalFillColor
import info.plateaukao.einkbro.view.compose.sketchShape
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
            onSelectBorder = { border ->
                config.display.uiBorder = border
                retintWindowFrame()
            },
            onSelectFill = { fill ->
                config.display.uiFill = fill
                retintWindowFrame()
            },
            onToggleInvert = { inverted ->
                config.display.uiThemeInverted = inverted
                retintWindowFrame()
            },
            onGradientPreview = { angle, level ->
                UiThemeState.gradientAngle.value = angle
                UiThemeState.gradientLevel.value = level
                retintWindowFrame()
            },
            onGradientPicked = { angle, level ->
                config.display.gradientAngle = angle
                config.display.gradientLevel = level
                retintWindowFrame()
            },
            onCustomColorPreview = { color ->
                // live preview while dragging; not yet persisted
                UiThemeState.customColor.value = color
                retintWindowFrame()
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
    onSelectBorder: (UiBorder) -> Unit,
    onSelectFill: (UiFill) -> Unit,
    onToggleInvert: (Boolean) -> Unit,
    onGradientPreview: (Int, Int) -> Unit,
    onGradientPicked: (Int, Int) -> Unit,
    onCustomColorPreview: (Color) -> Unit,
    onCustomColorPicked: (Color) -> Unit,
    onClose: () -> Unit,
) {
    val current by UiThemeState.current
    // hidden by default; shown by tapping the Custom swatch
    var showCustomWheel by remember { mutableStateOf(false) }
    // cap below the screen height so the dialog window never grows to the
    // window maximum, which would push its border frame out of view
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .heightIn(max = maxDialogHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            stringResource(R.string.theme_section_color),
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        val inverted by UiThemeState.inverted
        // color swatches plus the invert chip (half dark / half light circle)
        val colorCells = UiTheme.entries.size + 1
        (0 until colorCells).chunked(4).forEach { rowIndices ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowIndices.forEach { index ->
                    if (index < UiTheme.entries.size) {
                        val theme = UiTheme.entries[index]
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
                    } else {
                        InvertSwatch(
                            isSelected = inverted,
                            onClick = { onToggleInvert(!inverted) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(4 - rowIndices.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        if (current == UiTheme.CUSTOM && showCustomWheel) {
            CustomColorWheel(
                onPreview = onCustomColorPreview,
                onPicked = onCustomColorPicked,
            )
        }
        val currentBorder by UiThemeState.uiBorder
        val currentFill by UiThemeState.uiFill
        Text(
            stringResource(R.string.setting_title_border),
            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        UiBorder.entries.chunked(4).forEach { rowBorders ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowBorders.forEach { border ->
                    BorderSwatch(
                        border = border,
                        isSelected = border == currentBorder,
                        onClick = { if (border != currentBorder) onSelectBorder(border) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowBorders.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        Text(
            stringResource(R.string.setting_title_fill),
            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
        )
        var showGradientAdjust by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth()) {
            UiFill.entries.forEach { fill ->
                FillSwatch(
                    fill = fill,
                    isSelected = fill == currentFill,
                    onClick = {
                        // tapping the selected Gradient fill again toggles the
                        // direction/level dial
                        showGradientAdjust =
                            if (fill == currentFill && fill == UiFill.GRADIENT) !showGradientAdjust
                            else false
                        if (fill != currentFill) onSelectFill(fill)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        if (showGradientAdjust && currentFill == UiFill.GRADIENT) {
            GradientAdjust(
                onPreview = onGradientPreview,
                onPicked = onGradientPicked,
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
                    // consume from the first down so a press applies instantly
                    // and the dialog's scroll container never steals the drag
                    awaitEachGesture {
                        fun apply(x: Float, y: Float) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = x - cx
                            val dy = y - cy
                            hue = ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()) + 360.0) % 360.0)
                                .toFloat()
                            sat = (kotlin.math.sqrt(dx * dx + dy * dy) / kotlin.math.min(cx, cy))
                                .coerceIn(0f, 1f)
                            onPreview(currentColor())
                        }
                        val down = awaitFirstDown()
                        down.consume()
                        apply(down.position.x, down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            apply(change.position.x, change.position.y)
                            if (!change.pressed) break
                        }
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

/**
 * One dial for the gradient: the angle sets the direction, the distance
 * from the center sets the blend level. The fill previews the result.
 */
@Composable
private fun GradientAdjust(
    onPreview: (Int, Int) -> Unit,
    onPicked: (Int, Int) -> Unit,
) {
    val angle by UiThemeState.gradientAngle
    val level by UiThemeState.gradientLevel
    val accent = MaterialTheme.colors.primary
    val bg = MaterialTheme.colors.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .requiredSize(140.dp)
                .pointerInput(Unit) {
                    // consume from the first down: a press applies direction and
                    // level immediately, and the dialog's scroll never intercepts
                    awaitEachGesture {
                        fun apply(x: Float, y: Float) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = x - cx
                            val dy = y - cy
                            val a = ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()) + 360.0) % 360.0)
                                .toInt()
                            val frac = (kotlin.math.sqrt(dx * dx + dy * dy) / kotlin.math.min(cx, cy))
                                .coerceIn(0f, 1f)
                            onPreview(a, (40 + frac * 140).toInt())
                        }
                        val down = awaitFirstDown()
                        down.consume()
                        apply(down.position.x, down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            apply(change.position.x, change.position.y)
                            if (!change.pressed) break
                        }
                        onPicked(
                            UiThemeState.gradientAngle.value,
                            UiThemeState.gradientLevel.value,
                        )
                    }
                },
        ) {
            val radius = kotlin.math.min(size.width, size.height) / 2f - 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val rad = Math.toRadians(angle.toDouble())
            val dir = Offset(
                kotlin.math.cos(rad).toFloat(),
                kotlin.math.sin(rad).toFloat(),
            )
            // fill previews the gradient with the chosen direction and level
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(bg, lerp(bg, accent, (0.28f * level / 100f).coerceIn(0f, 0.9f))),
                    start = center - Offset(dir.x * radius, dir.y * radius),
                    end = center + Offset(dir.x * radius, dir.y * radius),
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = accent,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            // needle: length shows the level, direction the flow
            val frac = ((level - 40) / 140f).coerceIn(0f, 1f)
            val tip = center + Offset(dir.x * radius * frac, dir.y * radius * frac)
            drawLine(accent, center, tip, strokeWidth = 2.dp.toPx())
            drawCircle(accent, radius = 7.dp.toPx(), center = tip)
            drawCircle(bg, radius = 3.dp.toPx(), center = tip)
        }
    }
}

@Composable
private fun SwatchCell(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    preview: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            preview()
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
private fun BorderSwatch(
    border: UiBorder,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colors.primary
    val bgColor = MaterialTheme.colors.background
    val shape = RoundedCornerShape(minOf(border.itemRadiusDp, 12f).dp)
    val base = Modifier.requiredSize(width = 52.dp, height = 32.dp)
    val previewModifier = when (border) {
        // "none": faint outline with a diagonal strike-through
        UiBorder.NONE -> base
            .border(1.dp, accent.copy(alpha = 0.25f), shape)
            .drawBehind {
                drawLine(
                    accent.copy(alpha = 0.6f),
                    start = Offset(4.dp.toPx(), size.height - 4.dp.toPx()),
                    end = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        UiBorder.CLASSIC, UiBorder.ROUND, UiBorder.SHARP ->
            base.border(border.widthDp.dp, accent, shape)
        UiBorder.DASHED ->
            base.dashedBorder(border.widthDp.dp, minOf(border.itemRadiusDp, 12f).dp, accent)
        UiBorder.PAPER -> base
            .border(border.widthDp.dp, accent, shape)
            .padding(3.dp)
            .border(border.widthDp.dp, accent, shape)
        UiBorder.STAMP -> base.border(border.widthDp.dp, accent, stampShape(3.dp))
        UiBorder.SKETCH -> base.border(1.dp, accent, sketchShape(1.5.dp))
        UiBorder.CERTIFICATE -> base
            .border(2.dp, accent, shape)
            .padding(3.dp)
            .border(1.dp, accent, shape)
        UiBorder.STICKER -> base.drawBehind {
            // front box inset by the shadow offset so the shadow reads clearly
            val off = 5.dp.toPx()
            val corner = CornerRadius(8.dp.toPx())
            val boxSize = androidx.compose.ui.geometry.Size(size.width - off, size.height - off)
            drawRoundRect(
                accent,
                topLeft = Offset(off, off),
                size = boxSize,
                cornerRadius = corner,
            )
            drawRoundRect(bgColor, size = boxSize, cornerRadius = corner)
            drawRoundRect(
                accent,
                size = boxSize,
                cornerRadius = corner,
                style = Stroke(1.5.dp.toPx()),
            )
        }
    }
    SwatchCell(isSelected, onClick, modifier) { Box(modifier = previewModifier) }
}

@Composable
private fun FillSwatch(
    fill: UiFill,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colors.primary
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier
        .requiredSize(width = 52.dp, height = 32.dp)
        .clip(shape)
    val previewModifier = when (fill) {
        // "none": faint outline with a diagonal strike-through
        UiFill.NONE -> base
            .border(1.dp, accent.copy(alpha = 0.25f), shape)
            .drawBehind {
                drawLine(
                    accent.copy(alpha = 0.6f),
                    start = Offset(4.dp.toPx(), size.height - 4.dp.toPx()),
                    end = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        UiFill.TONAL -> base.background(tonalFillColor(), shape)
        UiFill.GRADIENT -> base.background(
            Brush.linearGradient(
                listOf(
                    lerp(MaterialTheme.colors.background, accent, 0.10f),
                    lerp(MaterialTheme.colors.background, accent, 0.40f),
                ),
            ),
            shape,
        )
    }
    SwatchCell(isSelected, onClick, modifier) { Box(modifier = previewModifier) }
}

/** Half-dark / half-light circle: tap to toggle inverted colors. */
@Composable
private fun InvertSwatch(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .drawBehind {
                    drawArc(Color.Black, 90f, 180f, useCenter = true)
                    drawArc(Color.White, 270f, 180f, useCenter = true)
                }
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
                    tint = MaterialTheme.colors.primary,
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
                .let { base ->
                    if (theme == UiTheme.CUSTOM) {
                        // rainbow ring marks the adjustable custom color
                        base.border(
                            width = if (isSelected) 4.dp else 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                    Color.Blue, Color.Magenta, Color.Red,
                                ),
                            ),
                            shape = CircleShape,
                        )
                    } else {
                        base.border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colors.onBackground
                            else MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
                            shape = CircleShape,
                        )
                    }
                },
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
    }
}

@Preview
@Composable
private fun PreviewThemeColorContent() {
    MyTheme {
        ThemeColorContent(onSelect = {}, onSelectBorder = {}, onSelectFill = {}, onToggleInvert = {}, onGradientPreview = { _, _ -> }, onGradientPicked = { _, _ -> }, onCustomColorPreview = {}, onCustomColorPicked = {}, onClose = {})
    }
}
