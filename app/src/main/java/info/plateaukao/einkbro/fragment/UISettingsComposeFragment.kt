package info.plateaukao.einkbro.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KMutableProperty0

class UISettingsComposeFragment : Fragment(), KoinComponent, FragmentTitleInterface {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            MyTheme {
                UiSettingsMainContent(uiSettingItems, dialogManager)
            }
        }
        return composeView
    }

    private val uiSettingItems = listOf(
        BooleanSettingItem(
            R.string.desktop_mode,
            R.drawable.icon_desktop,
            R.string.setting_summary_desktop,
            config::desktop,
        ),
        BooleanSettingItem(
            R.string.always_enable_zoom,
            R.drawable.ic_enable_zoom,
            R.string.setting_summary_enable_zoom,
            config::enableZoom,
        ),
        ValueSettingItem(
            R.string.setting_title_page_left_value,
            R.drawable.ic_page_height,
            R.string.setting_summary_page_left_value,
            config::pageReservedOffset
        ),
        ValueSettingItem(
            R.string.setting_title_translated_langs,
            R.drawable.ic_translate,
            R.string.setting_summary_translated_langs,
            config::preferredTranslateLanguageString
        ),
        ListSettingItem(
            R.string.dark_mode,
            R.drawable.ic_dark_mode,
            R.string.setting_summary_dark_mode,
            config::darkMode,
            listOf(
                R.string.dark_mode_follow_system,
                R.string.dark_mode_force_on,
                R.string.dark_mode_disabled,
            )
        ),
        ListSettingItem(
            R.string.setting_title_plus_behavior,
            R.drawable.icon_plus,
            R.string.setting_summary_plus_behavior,
            config::newTabBehavior,
            listOf(
                R.string.plus_start_input_url,
                R.string.plus_show_homepage,
                R.string.plus_show_bookmarks,
            )
        ),
        ListSettingItem(
            R.string.setting_title_nav_pos,
            R.drawable.icon_arrow_expand,
            R.string.setting_summary_nav_pos,
            config::fabPosition,
            listOf(
                R.string.setting_summary_nav_pos_right,
                R.string.setting_summary_nav_pos_left,
                R.string.setting_summary_nav_pos_center,
                R.string.setting_summary_nav_pos_not_show,
                R.string.setting_summary_nav_pos_custom,
            )
        )
    )

    override fun getTitleId(): Int = R.string.setting_title_toolbar
}

@Composable
private fun UiSettingsMainContent(
    settings: List<SettingItemInterface>,
    dialogManager: DialogManager
) {
    val context = LocalContext.current
    val showSummary = ViewUnit.isWideLayout(context)
    val columnCount = if (showSummary) 2 else 1
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(columnCount),
    ) {
        settings.forEach { setting ->
            item {
                when (setting) {
                    is BooleanSettingItem -> BooleanSettingItemUi(setting, showSummary)
                    is ValueSettingItem<*> -> ValueSettingItemUi(
                        setting,
                        dialogManager,
                        showSummary
                    )

                    is ListSettingItem<*> -> ListSettingItemUi(
                        setting = setting,
                        dialogManager,
                        showSummary
                    )
                }
            }
        }
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

class ValueSettingItem<T>(
    override val titleResId: Int,
    override val iconId: Int,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<T>,
) : SettingItemInterface

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

class ListSettingItem<T : Enum<T>>(
    override val titleResId: Int,
    override val iconId: Int,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<T>,
    val options: List<Int>,
) : SettingItemInterface