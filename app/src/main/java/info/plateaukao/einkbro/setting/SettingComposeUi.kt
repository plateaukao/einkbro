package info.plateaukao.einkbro.setting

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import kotlinx.coroutines.launch

@Composable
fun SettingItemUi(
    setting: SettingItemInterface,
    isChecked: Boolean = false,
    extraTitlePostfix: String = "",
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (isChecked || pressed) 3.dp else 1.dp
    val height = 80.dp
    var modifier = Modifier
        .fillMaxWidth()
        .testTag(stringResource(setting.titleResId))
        .height(height)
        .clickable(
            indication = null,
            interactionSource = interactionSource,
        ) { onClick?.invoke() }
    if (showBorder) modifier =
        modifier.border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))

    Row(
        modifier = modifier.then(
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
        Column {
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
) {
    val checked = remember { mutableStateOf(setting.config.get()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SettingItemUi(
            setting = setting, checked.value,
            showBorder = showBorder
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
                checkedThumbColor = MaterialTheme.colors.onBackground,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colors.onBackground,
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
) {
    val coroutineScope = rememberCoroutineScope()
    val currentValue = remember { mutableStateOf(setting.config.get()) }
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = if (showValue) ": ${currentValue.value}" else "",
        showBorder = showBorder,
    ) {
        coroutineScope.launch {
            val value = dialogManager.getTextInput(
                setting.titleResId,
                setting.summaryResId,
                setting.config.get()
            ) ?: return@launch
            if (setting.config.get() is Int) {
                setting.config.set(value.toInt() as T)
            } else {
                setting.config.set(value as T)
            }
            currentValue.value = value as T
        }
    }
}

@Composable
fun <T : Enum<T>> ListSettingItemUi(
    setting: ListSettingWithEnumItem<T>,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
) {
    val context = LocalContext.current
    var currentValueString =
        remember { mutableStateOf(context.getString(setting.options[setting.config.get().ordinal])) }
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValueString.value}",
        showBorder = showBorder,
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
fun ListSettingWithStringItemUi(
    setting: ListSettingWithStringItem,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
) {
    val context = LocalContext.current
    val currentIndex = setting.config.get().toInt()
    var currentValueString =
        remember { mutableStateOf(context.getString(setting.options[currentIndex])) }
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValueString.value}",
        showBorder = showBorder,
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
        settings.forEach { setting ->
            item(span = { GridItemSpan(if (supportTwoSpan) setting.span else 1) }) {
                when (setting) {
                    is NavigateSettingItem -> SettingItemUi(setting, showBorder = showBorder) {
                        navController.navigate(setting.destination.name)
                    }

                    is ActionSettingItem -> SettingItemUi(
                        setting,
                        showBorder = showBorder
                    ) { setting.action() }

                    is BooleanSettingItem -> BooleanSettingItemUi(setting, showBorder)
                    is ValueSettingItem<*> -> ValueSettingItemUi(
                        setting,
                        dialogManager,
                        showBorder,
                        setting.showValue
                    )

                    is DividerSettingItem ->
                        DividerSettingItemUi(setting.titleResId, supportTwoSpan)

                    is ListSettingWithEnumItem<*> -> ListSettingItemUi(
                        setting,
                        dialogManager,
                        showBorder
                    )

                    is ListSettingWithStringItem -> ListSettingWithStringItemUi(
                        setting,
                        dialogManager,
                        showBorder
                    )

                    is LinkSettingItem -> SettingItemUi(
                        setting,
                        showBorder = showBorder
                    ) { linkAction(setting.url) }

                    is VersionSettingItem -> {
                        val version = " v${BuildConfig.VERSION_NAME} (${BuildConfig.builtDateTime})"
                        SettingItemUi(setting, false, version, showBorder) {
                            navController.navigate(setting.destination.name)
                        }
                    }
                }
            }
        }
    }
}