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
import kotlin.reflect.KMutableProperty0
import info.plateaukao.einkbro.view.dialog.DialogManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import info.plateaukao.einkbro.BuildConfig

@Composable
fun SettingItemUi(
    setting: SettingItemInterface,
    showSummary: Boolean = false,
    isChecked: Boolean = false,
    extraTitlePostfix: String = "",
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (isChecked || pressed) 3.dp else 1.dp
    val height = if (showSummary) 80.dp else 70.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onClick?.invoke() },
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
            if (showSummary && setting.summaryResId != 0) {
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
    showSummary: Boolean = false
) {
    val checked = remember { mutableStateOf(setting.booleanPreference.get()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SettingItemUi(setting = setting, showSummary = showSummary, checked.value) {
            checked.value = !checked.value
            setting.booleanPreference.toggle()
        }

        if (checked.value)
            Icon(
                painter = painterResource(id = R.drawable.ic_check), contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(),
                tint = MaterialTheme.colors.onBackground
            )
    }
}

@Composable
fun <T> ValueSettingItemUi(
    setting: ValueSettingItem<T>,
    dialogManager: DialogManager,
    showSummary: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        showSummary = showSummary,
        extraTitlePostfix = ": ${setting.config.get()}",

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
        }
    }
}

@Composable
fun <T : Enum<T>> ListSettingItemUi(
    setting: ListSettingItem<T>,
    dialogManager: DialogManager,
    showSummary: Boolean = false,
) {
    val context = LocalContext.current
    val currentValueString = context.getString(setting.options[setting.config.get().ordinal])
    val coroutineScope = rememberCoroutineScope()
    SettingItemUi(
        setting = setting,
        showSummary = showSummary,
        extraTitlePostfix = ": $currentValueString"
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
        }
    }
}

@Composable
fun <T : SettingItemInterface> SettingItemUi(
    setting: T,
    onItemClick: (T) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 3.dp else 1.dp
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(60.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onItemClick(setting) },
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
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = stringResource(id = setting.titleResId),
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
fun VersionItemUi(
    setting: SettingItemInterface,
    onItemClick: () -> Unit
) {
    val version = """v${BuildConfig.VERSION_NAME}"""

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 3.dp else 1.dp
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(60.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onItemClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 15.dp),
            text = stringResource(id = setting.titleResId) + " " + version,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

