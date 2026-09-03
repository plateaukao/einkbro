package info.plateaukao.einkbro.setting

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.EinkImageAdjustment
import info.plateaukao.einkbro.preference.EinkImageMode
import info.plateaukao.einkbro.preference.ToolbarPosition
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.EinkImageProcessor
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import info.plateaukao.einkbro.view.compose.ThemedDialogWindowFrame
import info.plateaukao.einkbro.view.compose.ebItemFrame

// Rows keep this as a minimum but can grow when a larger system font scale
// makes the title/summary wrap (issue #623).
private val settingItemMinHeight = 80.dp

@Composable
fun SettingItemUi(
    setting: SettingItemInterface,
    isChecked: Boolean = false,
    extraTitlePostfix: String = "",
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Keep a thin border for the resting (incl. toggled-on) state; only show the
    // bold border as transient press feedback. isChecked is retained for callers
    // but no longer thickens the border (see discussion #605).
    val borderWidth = if (pressed) 3.dp else 1.dp
    var rowModifier = modifier
        .fillMaxWidth()
        .testTag(stringResource(setting.titleResId))
        .heightIn(min = settingItemMinHeight)
        .clickable(
            indication = null,
            interactionSource = interactionSource,
        ) { onClick?.invoke() }
    if (showBorder) rowModifier =
        rowModifier.ebItemFrame(borderWidth)

    Row(
        modifier = rowModifier.then(
            if (setting is BooleanSettingItem) Modifier.padding(
                0.dp,
                0.dp,
                55.dp,
                0.dp
            ) else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (setting.iconId != 0) {
            Icon(
                imageVector = ImageVector.vectorResource(id = setting.iconId), contentDescription = null,
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
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = stringResource(id = setting.titleResId) + extraTitlePostfix,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onBackground
            )
            if (setting.summaryResId != 0) {
                Spacer(
                    modifier = Modifier
                        .height(5.dp)
                        .fillMaxWidth()
                )
                Text(
                    modifier = Modifier.wrapContentWidth(),
                    text = stringResource(id = setting.summaryResId),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Composable
fun DividerSettingItemUi(
    title: Int = 0,
    supportTwoSpan: Boolean = false,
) {
    if (!supportTwoSpan) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            HorizontalSeparator()
            if (title != 0) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(5.dp),
                    text = stringResource(title),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    } else {
        if (title != 0) {
            Text(
                modifier = Modifier
                    .padding(5.dp),
                text = stringResource(title),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }
    }
}

@Composable
fun BooleanSettingItemUi(
    setting: BooleanSettingItem,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val checked = remember(setting) { mutableStateOf(setting.config.get()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SettingItemUi(
            setting = setting, checked.value,
            showBorder = showBorder,
            modifier = Modifier.fillMaxHeight(),
        ) {
            checked.value = !checked.value
            setting.config.toggle()
        }

        Switch(
            checked = checked.value,
            onCheckedChange = {
                checked.value = it
                setting.config.set(it)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 3.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colors.primary,
            )
        )
    }
}

@Composable
fun <T> ValueSettingItemUi(
    setting: ValueSettingItem<T>,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
    showValue: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentValue = remember(setting) { mutableStateOf(setting.config.get()) }
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = if (showValue) ": ${currentValue.value}" else "",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        coroutineScope.launch {
            val value = dialogManager.getTextInput(
                setting.titleResId,
                setting.summaryResId,
                setting.config.get()
            ) ?: return@launch
            @Suppress("UNCHECKED_CAST")
            if (setting.config.get() is Int) {
                val intValue = value.toIntOrNull() ?: return@launch
                setting.config.set(intValue as T)
                currentValue.value = intValue as T
            } else {
                setting.config.set(value as T)
                currentValue.value = value as T
            }
        }
    }
}

@Composable
fun GestureActionSettingItemUi(
    setting: GestureActionSettingItem,
    navController: NavHostController,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val entry = info.plateaukao.einkbro.browser.BrowserActionCatalog.entryOf(setting.config.get())
    val label = context.getString(entry.labelResId)
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": $label",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        GesturePickerState.editingSlot = setting
        navController.navigate(info.plateaukao.einkbro.activity.SettingRoute.GesturePicker.name)
    }
}

@Composable
fun <T : Enum<T>> ListSettingItemUi(
    setting: ListSettingWithEnumItem<T>,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentValueString =
        remember(setting) { mutableStateOf(context.getString(setting.options[setting.config.get().ordinal])) }
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValueString.value}",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        coroutineScope.launch {
            val selectedIndex = dialogManager.getSelectedOption(
                setting.titleResId,
                setting.options,
                setting.config.get().ordinal
            ) ?: return@launch
            setting.config.get().javaClass.enumConstants?.let {
                setting.config.set(it[selectedIndex])
            }
            currentValueString.value = context.getString(setting.options[selectedIndex])
        }
    }
}

@Composable
fun ToolbarPositionSettingItemUi(
    setting: ToolbarPositionSettingItem,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val current = remember(setting) { mutableStateOf(setting.config.get()) }
    var showDialog by remember(setting) { mutableStateOf(false) }
    val label = stringResource(toolbarPositionLabelResId(current.value))

    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": $label",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        showDialog = true
    }

    if (showDialog) {
        ToolbarPositionDialog(
            titleResId = setting.titleResId,
            initial = current.value,
            onDismiss = { showDialog = false },
            onConfirm = { pos ->
                setting.config.set(pos)
                current.value = pos
                showDialog = false
            },
        )
    }
}

private fun toolbarPositionLabelResId(position: ToolbarPosition): Int = when (position) {
    ToolbarPosition.Top -> info.plateaukao.einkbro.R.string.toolbar_position_top
    ToolbarPosition.Bottom -> info.plateaukao.einkbro.R.string.toolbar_position_bottom
    ToolbarPosition.Left -> info.plateaukao.einkbro.R.string.toolbar_position_left
    ToolbarPosition.Right -> info.plateaukao.einkbro.R.string.toolbar_position_right
}

@Composable
private fun ToolbarPositionDialog(
    titleResId: Int,
    initial: ToolbarPosition,
    onDismiss: () -> Unit,
    onConfirm: (ToolbarPosition) -> Unit,
) {
    var pending by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // the ThemedBorders window frame draws the border and fill
        backgroundColor = Color.Transparent,
        title = {
            Text(
                text = stringResource(titleResId) + ": " +
                    stringResource(toolbarPositionLabelResId(pending)),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
            )
        },
        text = {
            ThemedDialogWindowFrame()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                ToolbarPositionDiagram(
                    selected = pending,
                    onSelect = { pending = it },
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(0.75f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pending) }) {
                Text(
                    text = stringResource(android.R.string.ok),
                    color = MaterialTheme.colors.onBackground,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = MaterialTheme.colors.onBackground,
                )
            }
        },
    )
}

@Composable
private fun ToolbarPositionDiagram(
    selected: ToolbarPosition,
    onSelect: (ToolbarPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colors.onBackground
    val toolbarFill = MaterialTheme.colors.onBackground.copy(alpha = 0.25f)
    val edgeFraction = 0.18f
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val edgeX = w * edgeFraction
            val edgeY = h * edgeFraction
            val thin = 1.dp.toPx()
            val bold = 3.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // Gray fill the toolbar strip on the selected edge.
            when (selected) {
                ToolbarPosition.Top ->
                    drawRect(toolbarFill, Offset.Zero, Size(w, edgeY))
                ToolbarPosition.Bottom ->
                    drawRect(toolbarFill, Offset(0f, h - edgeY), Size(w, edgeY))
                ToolbarPosition.Left ->
                    drawRect(toolbarFill, Offset.Zero, Size(edgeX, h))
                ToolbarPosition.Right ->
                    drawRect(toolbarFill, Offset(w - edgeX, 0f), Size(edgeX, h))
            }

            // Outer rectangle.
            drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = Stroke(width = thin),
            )

            // Inner gridlines: dashed thin by default, solid bold when selected.
            fun line(start: Offset, end: Offset, isSelected: Boolean) {
                if (isSelected) drawLine(color, start, end, bold)
                else drawLine(color, start, end, thin, pathEffect = dash)
            }
            line(Offset(0f, edgeY), Offset(w, edgeY), selected == ToolbarPosition.Top)
            line(
                Offset(0f, h - edgeY),
                Offset(w, h - edgeY),
                selected == ToolbarPosition.Bottom,
            )
            line(Offset(edgeX, 0f), Offset(edgeX, h), selected == ToolbarPosition.Left)
            line(
                Offset(w - edgeX, 0f),
                Offset(w - edgeX, h),
                selected == ToolbarPosition.Right,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(edgeFraction)
                .clickable { onSelect(ToolbarPosition.Top) },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(edgeFraction)
                .clickable { onSelect(ToolbarPosition.Bottom) },
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(1f - 2 * edgeFraction)
                .fillMaxWidth(edgeFraction)
                .clickable { onSelect(ToolbarPosition.Left) },
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(1f - 2 * edgeFraction)
                .fillMaxWidth(edgeFraction)
                .clickable { onSelect(ToolbarPosition.Right) },
        )
    }
}

@Composable
fun EinkImageSettingItemUi(
    setting: EinkImageSettingItem,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val current = remember(setting) { mutableStateOf(setting.config.get()) }
    var showDialog by remember(setting) { mutableStateOf(false) }

    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${stringResource(current.value.labelResId)}",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        showDialog = true
    }

    if (showDialog) {
        EinkImageAdjustmentDialog(
            titleResId = setting.titleResId,
            initial = current.value,
            initialMode = setting.modeConfig.get(),
            onDismiss = { showDialog = false },
            onConfirm = { adjustment, mode ->
                setting.config.set(adjustment)
                setting.modeConfig.set(mode)
                current.value = adjustment
                showDialog = false
            },
        )
    }
}

/**
 * Approximates the FAST mode CSS filter (brightness/contrast/saturate) so the
 * preview shows what pages will render. Keep the factors in sync with
 * WebViewReaderHelper.einkImageFilterCss().
 */
private fun cssFilterColorMatrix(strength: Int): ColorMatrix {
    val t = strength / 100f
    val b = 1f + 0.15f * t
    val c = 1f + 0.2f * t
    val s = 1f + 0.8f * t
    val matrix = android.graphics.ColorMatrix()
    matrix.setScale(b, b, b, 1f) // brightness
    val offset = 127.5f * (1f - c)
    matrix.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, offset,
                0f, c, 0f, 0f, offset,
                0f, 0f, c, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    ) // contrast
    matrix.postConcat(android.graphics.ColorMatrix().apply { setSaturation(s) })
    return ColorMatrix(matrix.array)
}

@Composable
private fun RowScope.EinkOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier
            .weight(1f)
            .ebItemFrame(if (selected) 2.dp else null)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun EinkImageAdjustmentDialog(
    titleResId: Int,
    initial: EinkImageAdjustment,
    initialMode: EinkImageMode,
    onDismiss: () -> Unit,
    onConfirm: (EinkImageAdjustment, EinkImageMode) -> Unit,
) {
    var pending by remember { mutableStateOf(initial) }
    var pendingMode by remember { mutableStateOf(initialMode) }
    val context = LocalContext.current
    val original = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.eink_image_preview)
    }
    var preview by remember { mutableStateOf(original.asImageBitmap()) }
    LaunchedEffect(pending, pendingMode) {
        // FAST mode is previewed with a ColorFilter on the original instead
        preview = if (pending.strength <= 0 || pendingMode == EinkImageMode.FAST) {
            original.asImageBitmap()
        } else {
            withContext(Dispatchers.Default) {
                EinkImageProcessor
                    .process(original.copy(Bitmap.Config.ARGB_8888, true), pending.strength)
                    .asImageBitmap()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        // the ThemedBorders window frame draws the border and fill
        ThemedDialogWindowFrame()
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                bitmap = preview,
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(1.dp, MaterialTheme.colors.primary),
                contentScale = ContentScale.Fit,
                colorFilter = if (pendingMode == EinkImageMode.FAST && pending.strength > 0) {
                    ColorFilter.colorMatrix(cssFilterColorMatrix(pending.strength))
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EinkImageAdjustment.entries.forEach { adjustment ->
                    EinkOptionChip(
                        text = stringResource(adjustment.labelResId),
                        selected = adjustment == pending,
                        onClick = { pending = adjustment },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EinkImageMode.entries.forEach { mode ->
                    EinkOptionChip(
                        text = stringResource(mode.labelResId),
                        selected = mode == pendingMode,
                        onClick = { pendingMode = mode },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = MaterialTheme.colors.onBackground,
                    )
                }
                TextButton(onClick = { onConfirm(pending, pendingMode) }) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = MaterialTheme.colors.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
fun ListSettingWithStringItemUi(
    setting: ListSettingWithStrResIdItem,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentIndex = setting.config.get().toInt()
    var currentValueString =
        remember(setting) { mutableStateOf(context.getString(setting.options[currentIndex])) }
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValueString.value}",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        coroutineScope.launch {
            val selectedIndex = dialogManager.getSelectedOption(
                setting.titleResId,
                setting.options,
                currentIndex
            ) ?: return@launch
            setting.config.set(selectedIndex.toString())
            currentValueString.value = context.getString(setting.options[selectedIndex])
        }
    }
}

@Composable
fun <T> ListSettingWithClassItemUi(
    setting: ListSettingWithClassItem<T>,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val configString = setting.config.get()
    var currentValueString = remember(setting) { mutableStateOf(configString) }
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValueString.value}",
        showBorder = showBorder,
        modifier = modifier,
    ) {
        coroutineScope.launch {
            val selectedIndex = dialogManager.getSelectedOptionWithString(
                setting.titleResId,
                setting.options,
                setting.options.indexOf(configString)
            ) ?: return@launch
            val selectedValue = setting.options[selectedIndex]
            setting.config.set(selectedValue)
            currentValueString.value = selectedValue
        }
    }
}

@Composable
fun SearchSettingScreen(
    query: String,
    allSettings: List<Pair<Int, SettingItemInterface>>,
    navController: NavHostController,
    dialogManager: DialogManager,
    linkAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val englishContext = remember {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.ENGLISH)
        context.createConfigurationContext(config)
    }
    val filteredSettings = remember(query) {
        if (query.isBlank()) emptyList()
        else allSettings.filter { (_, setting) ->
            val title = context.getString(setting.titleResId)
            val summary =
                if (setting.summaryResId != 0) context.getString(setting.summaryResId) else ""
            val enTitle = englishContext.getString(setting.titleResId)
            val enSummary =
                if (setting.summaryResId != 0) englishContext.getString(setting.summaryResId) else ""
            title.contains(query, ignoreCase = true) || summary.contains(query, ignoreCase = true) ||
                enTitle.contains(query, ignoreCase = true) || enSummary.contains(query, ignoreCase = true)
        }
    }

    val columnCount = if (ViewUnit.isWideLayout(context)) 2 else 1
    val showBorder = columnCount == 2
    val supportTwoSpan = columnCount == 2

    // Group into consecutive category runs so items can be paired per category
    // in two-column mode while keeping the dividers between categories.
    val categoryRuns = remember(filteredSettings) {
        buildList<Pair<Int, MutableList<SettingItemInterface>>> {
            filteredSettings.forEach { (categoryResId, setting) ->
                if (isEmpty() || last().first != categoryResId) add(categoryResId to mutableListOf())
                last().second.add(setting)
            }
        }
    }

    LazyVerticalGrid(
        modifier = modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(columnCount),
    ) {
        categoryRuns.forEachIndexed { index, (categoryResId, categorySettings) ->
            if (index > 0) {
                item(
                    key = "divider-$categoryResId",
                    span = { GridItemSpan(if (supportTwoSpan) 2 else 1) },
                ) {
                    DividerSettingItemUi(categoryResId, supportTwoSpan)
                }
            }
            if (columnCount == 1) {
                categorySettings.forEach { setting ->
                    // Keyed so per-item remember state follows the setting, not the grid
                    // position, as the filtered list morphs while typing.
                    item(key = "$categoryResId-${setting.titleResId}") {
                        SettingItemCell(setting, navController, dialogManager, linkAction, showBorder)
                    }
                }
            } else {
                pairSettingLines(categorySettings).forEach { line ->
                    item(
                        key = "$categoryResId-${line.first().titleResId}",
                        span = { GridItemSpan(2) },
                    ) {
                        SettingLineUi(line, navController, dialogManager, linkAction, showBorder)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingScreen(
    navController: NavHostController,
    settings: List<SettingItemInterface>,
    dialogManager: DialogManager,
    linkAction: (String) -> Unit,
    defaultGridSize: Int = 1,
) {
    val context = LocalContext.current
    val columnCount = if (ViewUnit.isWideLayout(context) || defaultGridSize == 2) 2 else 1
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(columnCount),
    ) {
        val showBorder = columnCount == 2
        val supportTwoSpan = columnCount == 2
        if (columnCount == 1) {
            settings.forEach { setting ->
                item {
                    if (setting is DividerSettingItem) {
                        DividerSettingItemUi(setting.titleResId, supportTwoSpan)
                    } else {
                        SettingItemCell(setting, navController, dialogManager, linkAction, showBorder)
                    }
                }
            }
        } else {
            pairSettingLines(settings).forEach { line ->
                item(span = { GridItemSpan(2) }) {
                    val single = line.singleOrNull()
                    if (single is DividerSettingItem) {
                        DividerSettingItemUi(single.titleResId, supportTwoSpan)
                    } else {
                        SettingLineUi(line, navController, dialogManager, linkAction, showBorder)
                    }
                }
            }
        }
    }
}

// Groups span-1 settings two per grid line; span-2 items (dividers) get their
// own full-width line.
private fun pairSettingLines(settings: List<SettingItemInterface>): List<List<SettingItemInterface>> {
    val lines = mutableListOf<List<SettingItemInterface>>()
    var pending: SettingItemInterface? = null
    settings.forEach { setting ->
        when {
            setting.span == 2 -> {
                pending?.let { lines.add(listOf(it)) }
                pending = null
                lines.add(listOf(setting))
            }

            pending == null -> pending = setting
            else -> {
                lines.add(listOf(pending!!, setting))
                pending = null
            }
        }
    }
    pending?.let { lines.add(listOf(it)) }
    return lines
}

// One two-column grid line. IntrinsicSize.Max + fillMaxHeight stretch both cells
// to the taller one so their borders stay aligned when font scaling wraps text.
@Composable
private fun SettingLineUi(
    line: List<SettingItemInterface>,
    navController: NavHostController,
    dialogManager: DialogManager,
    linkAction: (String) -> Unit,
    showBorder: Boolean,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        line.forEach { setting ->
            SettingItemCell(
                setting, navController, dialogManager, linkAction, showBorder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        if (line.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingItemCell(
    setting: SettingItemInterface,
    navController: NavHostController,
    dialogManager: DialogManager,
    linkAction: (String) -> Unit,
    showBorder: Boolean,
    modifier: Modifier = Modifier,
) {
    when (setting) {
        is NavigateSettingItem -> SettingItemUi(setting, showBorder = showBorder, modifier = modifier) {
            navController.navigate(setting.destination.name)
        }

        is ActionSettingItem -> SettingItemUi(
            setting,
            showBorder = showBorder,
            modifier = modifier,
        ) { setting.action() }

        is GestureActionSettingItem -> GestureActionSettingItemUi(
            setting, navController, showBorder, modifier
        )

        is BooleanSettingItem -> BooleanSettingItemUi(setting, showBorder, modifier)
        is ValueSettingItem<*> -> ValueSettingItemUi(
            setting,
            dialogManager,
            showBorder,
            setting.showValue,
            modifier,
        )

        is ListSettingWithEnumItem<*> -> ListSettingItemUi(
            setting,
            dialogManager,
            showBorder,
            modifier,
        )

        is ToolbarPositionSettingItem -> ToolbarPositionSettingItemUi(
            setting,
            showBorder,
            modifier,
        )

        is EinkImageSettingItem -> EinkImageSettingItemUi(
            setting,
            showBorder,
            modifier,
        )

        is ListSettingWithStrResIdItem -> ListSettingWithStringItemUi(
            setting,
            dialogManager,
            showBorder,
            modifier,
        )

        is ListSettingWithClassItem<*> -> ListSettingWithClassItemUi(
            setting,
            dialogManager,
            showBorder,
            modifier,
        )

        is LinkSettingItem -> {
            val url = if (setting.urlResId != 0) stringResource(setting.urlResId) else setting.url
            SettingItemUi(
                setting,
                showBorder = showBorder,
                modifier = modifier,
            ) { linkAction(url) }
        }

        is VersionSettingItem -> {
            val version = " v${BuildConfig.VERSION_NAME} (${BuildConfig.lastCommitTime})"
            SettingItemUi(setting, false, version, showBorder, modifier) {
                navController.navigate(setting.destination.name)
            }
        }

        else -> {}
    }
}