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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
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
    )

    override fun getTitleId(): Int = R.string.setting_title_toolbar
}

@Composable
private fun UiSettingsMainContent(
    settings: List<SettingItemInterface>,
    dialogManager: DialogManager
) {
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(1),
    ) {
        settings.forEach { setting ->
            item {
                when (setting) {
                    is BooleanSettingItem -> BooleanSettingItemUi(setting, true)
                    is ValueSettingItem<*> -> ValueSettingItemUi(setting, dialogManager)
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
    SettingItemUi(setting = setting, showSummary = showSummary) {
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