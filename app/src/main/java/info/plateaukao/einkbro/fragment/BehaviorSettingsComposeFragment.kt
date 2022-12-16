package info.plateaukao.einkbro.fragment

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BehaviorSettingsComposeFragment : Fragment(), KoinComponent, FragmentTitleInterface {
    private val config: ConfigManager by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            MyTheme {
                BehaviorSettingsMainContent(behaviorSettingItems)
            }
        }
        return composeView
    }

    private fun showFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(null)
            .commit()
        activity?.setTitle((fragment as FragmentTitleInterface).getTitleId())
    }

    private val behaviorSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_saveTabs,
            R.drawable.icon_tab_plus,
            config.shouldSaveTabs,
            R.string.setting_summary_saveTabs,
        ) { config.shouldSaveTabs = !config.shouldSaveTabs },
        BooleanSettingItem(
            R.string.setting_title_background_loading,
            R.drawable.icon_tab_plus,
            config.enableWebBkgndLoad,
            R.string.setting_summary_background_loading,
        ) { config.enableWebBkgndLoad = !config.enableWebBkgndLoad },
        BooleanSettingItem(
            R.string.setting_title_trim_input_url,
            R.drawable.icon_edit,
            config.shouldTrimInputUrl,
            R.string.setting_summary_trim_input_url,
        ) { config.shouldTrimInputUrl = !config.shouldTrimInputUrl },
        BooleanSettingItem(
            R.string.setting_title_prune_query_parameter,
            R.drawable.ic_filter,
            config.shouldPruneQueryParameters,
            R.string.setting_summary_prune_query_parameter,
        ) { config.shouldPruneQueryParameters = !config.shouldPruneQueryParameters },
        BooleanSettingItem(
            R.string.setting_title_screen_awake,
            R.drawable.ic_eye,
            config.keepAwake,
            R.string.setting_summary_screen_awake,
        ) { config.keepAwake = !config.keepAwake },
        BooleanSettingItem(
            R.string.setting_title_confirm_tab_close,
            R.drawable.icon_close,
            config.confirmTabClose,
            R.string.setting_summary_confirm_tab_close,
        ) { config.confirmTabClose = !config.confirmTabClose },
        BooleanSettingItem(
            R.string.setting_title_vi_binding,
            R.drawable.ic_keyboard,
            config.enableViBinding,
            R.string.setting_summary_vi_binding,
        ) { config.enableViBinding = !config.enableViBinding },
        BooleanSettingItem(
            R.string.setting_title_useUpDown,
            R.drawable.ic_page_down,
            config.useUpDownPageTurn,
            R.string.setting_summary_useUpDownKey,
        ) { config.useUpDownPageTurn = !config.useUpDownPageTurn },
    )

    override fun getTitleId(): Int = R.string.setting_title_behavior
}

@Composable
private fun BehaviorSettingsMainContent(settings: List<BooleanSettingItem>) {
    val context = LocalContext.current
    val showSummary = ViewUnit.isWideLayout(context)
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(2),
    ) {
        settings.forEach { setting ->
            item { SettingItem(setting, showSummary) }
        }
    }
}

@Composable
fun SettingItem(setting: BooleanSettingItem, showSummary: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val checked = remember { mutableStateOf(setting.booleanPreference) }
    val borderWidth = if (checked.value || pressed) 3.dp else 1.dp
    val height = if (showSummary) 80.dp else 70.dp
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(height)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) {
                setting.onClick()
                checked.value = !checked.value
            },
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
                text = stringResource(id = setting.titleResId),
                fontSize = 16.sp,
                color = MaterialTheme.colors.onBackground
            )
            if (showSummary) {
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


class BooleanSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    val booleanPreference: Boolean,
    val summaryResId: Int = 0,
    val onClick: () -> Unit,
) : SettingItemInterface

@Preview
@Composable
fun PreviewBehaviorSettingsMainContent() {
    MyTheme {
        BehaviorSettingsMainContent(
            listOf(
                BooleanSettingItem(
                    R.string.setting_title_saveTabs,
                    R.drawable.icon_tab_plus,
                    false
                ) {}
            )
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewNightBehaviorSettingsMainContent() {
    MyTheme {
        BehaviorSettingsMainContent(
            listOf(
                BooleanSettingItem(
                    R.string.setting_title_saveTabs,
                    R.drawable.icon_tab_plus,
                    false
                ) {}
            )
        )
    }
}
