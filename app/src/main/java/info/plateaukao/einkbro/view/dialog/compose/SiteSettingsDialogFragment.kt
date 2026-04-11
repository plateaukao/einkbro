package info.plateaukao.einkbro.view.dialog.compose

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CodeOff
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.InvertColorsOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.view.compose.MyTheme

class SiteSettingsDialogFragment(
    private val url: String,
    private val onDismissAction: () -> Unit = {},
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            val host = Uri.parse(url)?.host.orEmpty()
            SiteSettingsContent(
                host = host,
                domainConfig = config.getDomainConfig(url),
                globalFontSize = config.display.fontSize,
                globalFontType = config.display.fontType,
                globalBoldFont = config.display.boldFontStyle,
                globalBlackFont = config.display.blackFontStyle,
                globalFontBoldness = config.display.fontBoldness,
                globalDesktopMode = config.browser.desktop,
                globalJavascript = config.browser.enableJavascript,
                globalTranslationMode = config.translation.translationMode,
                onSave = { updatedConfig ->
                    config.updateDomainConfig(updatedConfig)
                    onDismissAction()
                    dialog?.dismiss()
                },
                onDismiss = { dialog?.dismiss() },
            )
        }
    }
}

@Composable
private fun SiteSettingsContent(
    host: String,
    domainConfig: DomainConfigurationData,
    globalFontSize: Int,
    globalFontType: FontType,
    globalBoldFont: Boolean,
    globalBlackFont: Boolean,
    globalFontBoldness: Int,
    globalDesktopMode: Boolean,
    globalJavascript: Boolean,
    globalTranslationMode: TranslationMode,
    onSave: (DomainConfigurationData) -> Unit,
    onDismiss: () -> Unit,
) {
    var fontSize by remember { mutableStateOf(domainConfig.fontSize) }
    var fontType by remember { mutableStateOf(domainConfig.fontType) }
    var boldFont by remember { mutableStateOf(domainConfig.boldFontStyle) }
    var blackFont by remember { mutableStateOf(domainConfig.blackFontStyle) }
    var fontBoldness by remember { mutableStateOf(domainConfig.fontBoldness) }
    var whiteBackground by remember { mutableStateOf(domainConfig.shouldUseWhiteBackground) }
    var invertColor by remember { mutableStateOf(domainConfig.shouldInvertColor) }
    var desktopMode by remember { mutableStateOf(domainConfig.desktopMode) }
    var javascript by remember { mutableStateOf(domainConfig.enableJavascript) }
    var translateSite by remember { mutableStateOf(domainConfig.shouldTranslateSite) }
    var translationMode by remember { mutableStateOf(domainConfig.translationMode) }

    Column(
        modifier = Modifier
            .width(300.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "${stringResource(R.string.site_settings)}: $host",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground,
        )
        Spacer(Modifier.height(12.dp))

        // Font Size
        NullableIntSliderRow(
            label = stringResource(R.string.font_size),
            value = fontSize,
            globalValue = globalFontSize,
            valueRange = 50f..250f,
            steps = 19,
            displayValue = { "${it}%" },
            onValueChange = { fontSize = it },
        )

        // Font Type
        NullableDropdownRow(
            label = stringResource(R.string.font_type),
            value = fontType,
            globalValue = globalFontType,
            options = FontType.entries,
            optionLabel = { stringResource(it.resId) },
            onValueChange = { fontType = it },
        )

        // Bold Font
        NullableBooleanRow(
            label = stringResource(R.string.bold_font),
            value = boldFont,
            globalValue = globalBoldFont,
            defaultOnActivate = true,
            onIconRes = R.drawable.ic_bold_font_active,
            offIconRes = R.drawable.ic_bold_font,
            onValueChange = { boldFont = it },
        )

        // Font Boldness (under bold font, enabled only when bold is on)
        run {
            val boldEnabled = boldFont == true
            val effectiveBoldness = fontBoldness ?: globalFontBoldness
            var sliderValue by remember(effectiveBoldness) { mutableFloatStateOf(effectiveBoldness.toFloat()) }
            val color = if (boldEnabled) MaterialTheme.colors.onBackground
                else MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
            Text(
                text = "${stringResource(R.string.font_boldness)}: ${effectiveBoldness}",
                modifier = Modifier.padding(start = 48.dp),
                fontSize = 13.sp,
                color = color,
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    fontBoldness = it.toInt()
                },
                valueRange = 500f..900f,
                steps = 3,
                enabled = boldEnabled,
                modifier = Modifier.padding(start = 48.dp, end = 16.dp),
            )
        }

        // Black Font
        NullableBooleanRow(
            label = stringResource(R.string.black_font),
            value = blackFont,
            globalValue = globalBlackFont,
            defaultOnActivate = true,
            onIcon = Icons.TwoTone.Copyright,
            offIcon = Icons.Outlined.Copyright,
            onValueChange = { blackFont = it },
        )

        // White Background (always per-site, non-nullable boolean)
        BooleanRow(
            label = stringResource(R.string.white_background),
            value = whiteBackground,
            onIconRes = R.drawable.ic_white_background_active,
            offIconRes = R.drawable.ic_white_background,
            onValueChange = { whiteBackground = it },
        )

        // Invert Color (always per-site, non-nullable boolean)
        BooleanRow(
            label = stringResource(R.string.menu_invert_color),
            value = invertColor,
            onIcon = Icons.Outlined.InvertColorsOff,
            offIcon = Icons.Outlined.InvertColors,
            onValueChange = { invertColor = it },
        )

        // Desktop Mode
        NullableBooleanRow(
            label = stringResource(R.string.desktop_mode),
            value = desktopMode,
            globalValue = globalDesktopMode,
            onIconRes = R.drawable.icon_desktop_activate,
            offIconRes = R.drawable.icon_desktop,
            onValueChange = { desktopMode = it },
        )

        // JavaScript
        NullableBooleanRow(
            label = stringResource(R.string.setting_title_javascript),
            value = javascript,
            globalValue = globalJavascript,
            onIcon = Icons.Outlined.Code,
            offIcon = Icons.Outlined.CodeOff,
            onValueChange = { javascript = it },
        )

        // Translation: checkbox (always translate) + icon + mode dropdown
        TranslationRow(
            translateSite = translateSite,
            translationMode = translationMode,
            globalTranslationMode = globalTranslationMode,
            onTranslateSiteChange = { translateSite = it },
            onTranslationModeChange = { translationMode = it },
        )

        Spacer(Modifier.height(12.dp))
        HorizontalSeparator()
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = {
                    fontSize = null; fontType = null; boldFont = null; blackFont = null
                    fontBoldness = null; desktopMode = null; javascript = null
                    whiteBackground = false; invertColor = false
                    translateSite = false; translationMode = null
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colors.onBackground,
                ),
            ) {
                Text(stringResource(R.string.reset_to_global), fontSize = 13.sp)
            }
            Button(
                onClick = {
                    val updated = domainConfig.copy(
                        fontSize = fontSize,
                        fontType = fontType,
                        boldFontStyle = boldFont,
                        blackFontStyle = blackFont,
                        fontBoldness = fontBoldness,
                        shouldUseWhiteBackground = whiteBackground,
                        shouldInvertColor = invertColor,
                        desktopMode = desktopMode,
                        enableJavascript = javascript,
                        shouldTranslateSite = translateSite,
                        translationMode = translationMode,
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.onBackground,
                    contentColor = MaterialTheme.colors.background,
                ),
            ) {
                Text("OK", fontSize = 13.sp)
            }
        }
    }
}

/**
 * A row with a nullable boolean override. Left checkbox toggles per-site override.
 * State icon before the label shows current value; clicking the icon toggles it.
 * Accepts either ImageVector pair or drawable resource ID pair for on/off icons.
 */
@Composable
private fun NullableBooleanRow(
    label: String,
    value: Boolean?,
    globalValue: Boolean,
    defaultOnActivate: Boolean = globalValue,
    onIcon: ImageVector? = null,
    offIcon: ImageVector? = null,
    onIconRes: Int = 0,
    offIconRes: Int = 0,
    onValueChange: (Boolean?) -> Unit,
) {
    val hasOverride = value != null
    val effectiveValue = value ?: globalValue
    val color = if (hasOverride) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = hasOverride,
            onCheckedChange = { checked ->
                onValueChange(if (checked) defaultOnActivate else null)
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.onBackground,
                uncheckedColor = MaterialTheme.colors.onBackground,
                checkmarkColor = MaterialTheme.colors.background,
            ),
        )
        StateIcon(
            isOn = effectiveValue,
            onIcon = onIcon, offIcon = offIcon,
            onIconRes = onIconRes, offIconRes = offIconRes,
            tint = color,
            modifier = Modifier.noRippleClickable(enabled = hasOverride) { onValueChange(!effectiveValue) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = color,
        )
    }
}

/**
 * A row for non-nullable per-site booleans (white background, invert color).
 * State icon before the label; clicking the icon toggles it.
 */
@Composable
private fun BooleanRow(
    label: String,
    value: Boolean,
    onIcon: ImageVector? = null,
    offIcon: ImageVector? = null,
    onIconRes: Int = 0,
    offIconRes: Int = 0,
    onValueChange: (Boolean) -> Unit,
) {
    val color = MaterialTheme.colors.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(48.dp))
        StateIcon(
            isOn = value,
            onIcon = onIcon, offIcon = offIcon,
            onIconRes = onIconRes, offIconRes = offIconRes,
            tint = color,
            modifier = Modifier.noRippleClickable { onValueChange(!value) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = color,
        )
    }
}

@Composable
private fun StateIcon(
    isOn: Boolean,
    onIcon: ImageVector?,
    offIcon: ImageVector?,
    onIconRes: Int,
    offIconRes: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val iconModifier = modifier.size(28.dp)
    if (onIcon != null && offIcon != null) {
        Icon(
            imageVector = if (isOn) onIcon else offIcon,
            contentDescription = null,
            modifier = iconModifier,
            tint = tint,
        )
    } else {
        Icon(
            imageVector = ImageVector.vectorResource(id = if (isOn) onIconRes else offIconRes),
            contentDescription = null,
            modifier = iconModifier,
            tint = tint,
        )
    }
}

/**
 * Single row: checkbox (always translate) + translate icon + translation mode dropdown.
 */
@Composable
private fun TranslationRow(
    translateSite: Boolean,
    translationMode: TranslationMode?,
    globalTranslationMode: TranslationMode,
    onTranslateSiteChange: (Boolean) -> Unit,
    onTranslationModeChange: (TranslationMode?) -> Unit,
) {
    val effectiveMode = translationMode ?: globalTranslationMode
    var expanded by remember { mutableStateOf(false) }
    val color = if (translateSite) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = translateSite,
            onCheckedChange = { onTranslateSiteChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.onBackground,
                uncheckedColor = MaterialTheme.colors.onBackground,
                checkmarkColor = MaterialTheme.colors.background,
            ),
        )
        Icon(
            imageVector = Icons.Outlined.Translate,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = color,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { if (translateSite) expanded = true },
            enabled = translateSite,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = color,
            ),
        ) {
            Text(stringResource(effectiveMode.labelResId), fontSize = 12.sp)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                TranslationMode.entries.forEach { mode ->
                    DropdownMenuItem(onClick = {
                        onTranslationModeChange(mode)
                        expanded = false
                    }) {
                        Text(stringResource(mode.labelResId))
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )

/**
 * A row with a nullable int slider override (font size, boldness).
 */
@Composable
private fun NullableIntSliderRow(
    label: String,
    value: Int?,
    globalValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: (Int) -> String,
    onValueChange: (Int?) -> Unit,
) {
    val hasOverride = value != null
    val effectiveValue = value ?: globalValue
    var sliderValue by remember(effectiveValue) { mutableFloatStateOf(effectiveValue.toFloat()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = hasOverride,
                onCheckedChange = { checked ->
                    onValueChange(if (checked) effectiveValue else null)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colors.onBackground,
                    uncheckedColor = MaterialTheme.colors.onBackground,
                    checkmarkColor = MaterialTheme.colors.background,
                ),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (hasOverride) MaterialTheme.colors.onBackground
                else MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
            )
            Text(
                text = displayValue(effectiveValue),
                fontSize = 14.sp,
                color = if (hasOverride) MaterialTheme.colors.onBackground
                else MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(horizontal = 16.dp),
            enabled = hasOverride,
        )
    }
}

/**
 * A row with a nullable enum dropdown override.
 */
@Composable
private fun <T> NullableDropdownRow(
    label: String,
    value: T?,
    globalValue: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onValueChange: (T?) -> Unit,
) {
    val hasOverride = value != null
    val effectiveValue = value ?: globalValue
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = hasOverride,
            onCheckedChange = { checked ->
                onValueChange(if (checked) effectiveValue else null)
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.onBackground,
                uncheckedColor = MaterialTheme.colors.onBackground,
                checkmarkColor = MaterialTheme.colors.background,
            ),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = if (hasOverride) MaterialTheme.colors.onBackground
            else MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
        )
        OutlinedButton(
            onClick = { if (hasOverride) expanded = true },
            enabled = hasOverride,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.onBackground,
            ),
        ) {
            Text(optionLabel(effectiveValue), fontSize = 12.sp)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        onValueChange(option)
                        expanded = false
                    }) {
                        Text(optionLabel(option))
                    }
                }
            }
        }
    }
}
