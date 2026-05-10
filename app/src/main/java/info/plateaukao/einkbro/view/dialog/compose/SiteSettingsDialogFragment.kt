package info.plateaukao.einkbro.view.dialog.compose

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CodeOff
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.InvertColorsOff
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.DomainConfigurationData
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.view.compose.MyTheme

private const val DEFAULT_DESKTOP_VIEWPORT_WIDTH = 1280

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
                defaultViewportWidth = DEFAULT_DESKTOP_VIEWPORT_WIDTH,
                globalJavascript = config.browser.enableJavascript,
                globalTranslationMode = config.translation.translationMode,
                onEditText = { title, initial, onResult ->
                    TextEditorDialogFragment(title, initial, onResult)
                        .show(parentFragmentManager, "text_editor")
                },
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
    defaultViewportWidth: Int,
    globalJavascript: Boolean,
    globalTranslationMode: TranslationMode,
    onEditText: (title: String, initial: String, onResult: (String) -> Unit) -> Unit,
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
    var viewportWidth by remember { mutableStateOf(domainConfig.desktopViewportWidth) }
    var javascript by remember { mutableStateOf(domainConfig.enableJavascript) }
    var translateSite by remember { mutableStateOf(domainConfig.shouldTranslateSite) }
    var translationMode by remember { mutableStateOf(domainConfig.translationMode) }
    var customCss by remember { mutableStateOf(domainConfig.customCss.orEmpty()) }
    var postLoadJs by remember { mutableStateOf(domainConfig.postLoadJavascript.orEmpty()) }

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp

    val overrideCount = listOf(
        fontSize != null,
        fontType != null,
        boldFont != null,
        blackFont != null,
        fontBoldness != null,
        whiteBackground,
        invertColor,
        desktopMode != null,
        viewportWidth != null,
        javascript != null,
        translateSite,
        customCss.isNotBlank(),
        postLoadJs.isNotBlank(),
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .widthIn(min = 300.dp, max = 420.dp)
            .heightIn(max = maxDialogHeight)
            .padding(16.dp),
    ) {
        // Title block: caption + prominent host + override count
        DialogTitle(host = host, overrideCount = overrideCount)

        Spacer(Modifier.height(8.dp))
        HorizontalSeparator()

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.setting_section_typography))

            // Font Size — stepper
            NullableIntStepperRow(
                label = stringResource(R.string.font_size),
                value = fontSize,
                globalValue = globalFontSize,
                min = 50,
                max = 250,
                step = 10,
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

            // Font Boldness — nested under Bold Font with a left rail
            NestedNullableIntStepper(
                value = fontBoldness,
                globalValue = globalFontBoldness,
                min = 500,
                max = 900,
                step = 100,
                enabled = (boldFont ?: globalBoldFont),
                onValueChange = { fontBoldness = it },
            )

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

            SectionHeader(stringResource(R.string.setting_title_ui))

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

            SectionHeader(stringResource(R.string.setting_title_behavior))

            // Desktop Mode
            NullableBooleanRow(
                label = stringResource(R.string.desktop_mode),
                value = desktopMode,
                globalValue = globalDesktopMode,
                onIconRes = R.drawable.icon_desktop_activate,
                offIconRes = R.drawable.icon_desktop,
                onValueChange = { desktopMode = it },
            )

            // Force Desktop Viewport Width — nested under Desktop Mode
            NestedNullableIntStepper(
                value = viewportWidth,
                globalValue = defaultViewportWidth,
                min = 800,
                max = 2400,
                step = 80,
                enabled = (desktopMode ?: globalDesktopMode),
                onValueChange = { viewportWidth = it },
                label = stringResource(R.string.site_force_viewport_width),
                hint = stringResource(R.string.site_force_viewport_width_hint),
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

            SectionHeader(stringResource(R.string.action_category_translation))

            // Translation: checkbox (always translate) on its own row, mode dropdown nested below
            TranslationRow(
                translateSite = translateSite,
                translationMode = translationMode,
                globalTranslationMode = globalTranslationMode,
                onTranslateSiteChange = { translateSite = it },
                onTranslationModeChange = { translationMode = it },
            )

            SectionHeader(stringResource(R.string.setting_section_advanced))

            val cssLabel = stringResource(R.string.site_custom_css)
            EditTextButtonRow(
                label = cssLabel,
                hasContent = customCss.isNotBlank(),
                onClick = { onEditText(cssLabel, customCss) { customCss = it } },
            )

            val jsLabel = stringResource(R.string.site_post_load_js)
            EditTextButtonRow(
                label = jsLabel,
                hasContent = postLoadJs.isNotBlank(),
                onClick = { onEditText(jsLabel, postLoadJs) { postLoadJs = it } },
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalSeparator()
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = {
                    fontSize = null; fontType = null; boldFont = null; blackFont = null
                    fontBoldness = null; desktopMode = null; viewportWidth = null; javascript = null
                    whiteBackground = false; invertColor = false
                    translateSite = false; translationMode = null
                    customCss = ""; postLoadJs = ""
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
                        desktopViewportWidth = viewportWidth,
                        enableJavascript = javascript,
                        shouldTranslateSite = translateSite,
                        translationMode = translationMode,
                        customCss = customCss.ifBlank { null },
                        postLoadJavascript = postLoadJs.ifBlank { null },
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.onBackground,
                    contentColor = MaterialTheme.colors.background,
                ),
            ) {
                Text(stringResource(android.R.string.ok), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DialogTitle(host: String, overrideCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = host.ifBlank { "—" },
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground,
        )
        if (overrideCount > 0) {
            OverrideBadge(overrideCount)
        }
    }
}

@Composable
private fun OverrideBadge(count: Int) {
    val text = if (count == 1) {
        stringResource(R.string.site_settings_overrides_count, count)
    } else {
        stringResource(R.string.site_settings_overrides_count_plural, count)
    }
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                shape = CircleShape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

/**
 * A row with a nullable boolean override. Left checkbox toggles per-site override.
 * State icon before the label shows current value. Tapping the icon enables override
 * (if not yet) and flips the value.
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
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)
    val defaultHint = stringResource(R.string.default_value_hint)

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
            modifier = Modifier.noRippleClickable {
                if (hasOverride) {
                    onValueChange(!effectiveValue)
                } else {
                    onValueChange(!effectiveValue)
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = color)
            if (!hasOverride) {
                Text(
                    text = defaultHint,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.45f),
                )
            }
        }
    }
}

/**
 * A row for non-nullable per-site booleans (white background, invert color).
 * Aligns to the left edge (no leading checkbox space) — these aren't overrides.
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
            .padding(vertical = 4.dp)
            .noRippleClickable { onValueChange(!value) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StateIcon(
            isOn = value,
            onIcon = onIcon, offIcon = offIcon,
            onIconRes = onIconRes, offIconRes = offIconRes,
            tint = color,
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
 * Translation: checkbox + translate icon + label on one row,
 * mode dropdown nested below with a left rail (only enabled when translateSite is true).
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
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)

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
        Text(
            text = stringResource(R.string.action_category_translation),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = color,
        )
    }

    NestedRail(enabled = translateSite) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { if (translateSite) expanded = true },
                enabled = translateSite,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
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
 * A row with a nullable int stepper override (font size, boldness).
 * E-ink-friendly: discrete [−] value [+] controls instead of a continuous slider.
 */
@Composable
private fun NullableIntStepperRow(
    label: String,
    value: Int?,
    globalValue: Int,
    min: Int,
    max: Int,
    step: Int,
    displayValue: (Int) -> String,
    onValueChange: (Int?) -> Unit,
) {
    val hasOverride = value != null
    val effectiveValue = value ?: globalValue
    val color = if (hasOverride) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)
    val defaultHint = stringResource(R.string.default_value_hint)

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
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = color)
            if (!hasOverride) {
                Text(
                    text = defaultHint,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.45f),
                )
            }
        }
        Stepper(
            value = effectiveValue,
            min = min,
            max = max,
            step = step,
            enabled = hasOverride,
            displayValue = displayValue,
            onValueChange = { onValueChange(it) },
        )
    }
}

/**
 * Stepper used standalone, nested under a parent row (e.g. font boldness under bold font).
 * Renders inside a NestedRail so the dependency is visible.
 */
@Composable
private fun NestedNullableIntStepper(
    value: Int?,
    globalValue: Int,
    min: Int,
    max: Int,
    step: Int,
    enabled: Boolean,
    onValueChange: (Int?) -> Unit,
    label: String? = null,
    hint: String? = null,
) {
    val hasOverride = value != null
    val effectiveValue = value ?: globalValue
    val color = if (enabled && hasOverride) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)

    NestedRail(enabled = enabled) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = hasOverride,
                onCheckedChange = { checked ->
                    if (enabled) onValueChange(if (checked) effectiveValue else null)
                },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colors.onBackground,
                    uncheckedColor = MaterialTheme.colors.onBackground,
                    checkmarkColor = MaterialTheme.colors.background,
                ),
            )
            if (label != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontSize = 13.sp, color = color)
                    if (!hasOverride && hint != null) {
                        Text(
                            text = hint,
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.45f),
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Stepper(
                value = effectiveValue,
                min = min,
                max = max,
                step = step,
                enabled = enabled && hasOverride,
                displayValue = { it.toString() },
                onValueChange = { onValueChange(it) },
            )
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    enabled: Boolean,
    displayValue: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            icon = Icons.Outlined.Remove,
            contentDescription = "−",
            tint = tint,
            enabled = enabled && value > min,
            onClick = { onValueChange((value - step).coerceAtLeast(min)) },
        )
        Text(
            text = displayValue(value),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.width(48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(
            icon = Icons.Outlined.Add,
            contentDescription = "+",
            tint = tint,
            enabled = enabled && value < max,
            onClick = { onValueChange((value + step).coerceAtMost(max)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
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
    val color = if (hasOverride) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)
    val defaultHint = stringResource(R.string.default_value_hint)

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
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = color)
            if (!hasOverride) {
                Text(
                    text = defaultHint,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.45f),
                )
            }
        }
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

/**
 * A row that opens a full-screen editor for a per-site CSS / JS override.
 */
@Composable
private fun EditTextButtonRow(
    label: String,
    hasContent: Boolean,
    onClick: () -> Unit,
) {
    val color = if (hasContent) MaterialTheme.colors.onBackground
        else MaterialTheme.colors.onBackground.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = color,
        )
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) {
            Text(
                stringResource(if (hasContent) R.string.menu_edit else R.string.whitelist_add),
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * Wraps nested content with a left vertical rail to indicate dependence on the row above.
 * Uses drawBehind so we don't introduce IntrinsicSize.Min, which collapses weight(1f)
 * children to their min intrinsic width inside verticalScroll.
 */
@Composable
private fun NestedRail(enabled: Boolean, content: @Composable () -> Unit) {
    val railColor = if (enabled) MaterialTheme.colors.onBackground.copy(alpha = 0.35f)
        else MaterialTheme.colors.onBackground.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val railX = 20.dp.toPx()
                val railW = 2.dp.toPx()
                drawRect(
                    color = railColor,
                    topLeft = androidx.compose.ui.geometry.Offset(railX, 0f),
                    size = androidx.compose.ui.geometry.Size(railW, size.height),
                )
            }
            .padding(start = 38.dp, end = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
