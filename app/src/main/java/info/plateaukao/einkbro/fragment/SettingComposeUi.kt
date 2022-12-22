package info.plateaukao.einkbro.fragment

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.view.dialog.DialogManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingItemUi(
    setting: SettingItemInterface,
    isChecked: Boolean = false,
    extraTitlePostfix: String = "",
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (isChecked || pressed) 3.dp else 1.dp
    val height = 80.dp
    var modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .clickable(
            indication = null,
            interactionSource = interactionSource,
        ) { onClick?.invoke() }
    if (showBorder) modifier = modifier.border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = setting.iconId), contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            tint = MaterialTheme.colors.onBackground
        )
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

        if (checked.value) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check), contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .align(if (showBorder) Alignment.TopEnd else Alignment.CenterEnd)
                    .fillMaxHeight(),
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Composable
fun <T> ValueSettingItemUi(
    setting: ValueSettingItem<T>,
    dialogManager: DialogManager,
    showBorder: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentValue = remember { mutableStateOf(setting.config.get()) }
    SettingItemUi(
        setting = setting,
        extraTitlePostfix = ": ${currentValue.value}",
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
