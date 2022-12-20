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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import org.koin.core.component.KoinComponent
import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import androidx.compose.foundation.lazy.grid.GridItemSpan
import info.plateaukao.einkbro.activity.BrowserActivity

class UISettingsComposeFragment(
    private val titleResId: Int,
    private val settingItems: List<SettingItemInterface>,
    private val defaultGridSize: Int = 1
) : Fragment(), KoinComponent, FragmentTitleInterface {
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            MyTheme {
                SettingsMainContent(settingItems, dialogManager, this::handleLink, defaultGridSize)
            }
        }
        return composeView
    }

    private fun handleLink(url: String) {
        requireContext().startActivity(
            Intent(requireContext(), BrowserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(EXTRA_TEXT, url)
            }
        )
        requireActivity().finish()
    }


    override fun getTitleId(): Int = titleResId
}

@Composable
fun SettingsMainContent(
    settings: List<SettingItemInterface>,
    dialogManager: DialogManager,
    linkAction: (String) -> Unit,
    defaultGridSize: Int = 1,
) {
    val context = LocalContext.current
    val showSummary = ViewUnit.isWideLayout(context)
    val columnCount = if (showSummary || defaultGridSize == 2) 2 else 1
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(columnCount),
    ) {
        settings.forEach { setting ->
            item(span = { GridItemSpan(setting.span) }) {
                when (setting) {
                    is ActionSettingItem -> SettingItemUi(setting, showSummary = showSummary) { setting.action() }
                    is BooleanSettingItem -> BooleanSettingItemUi(setting, showSummary)
                    is ValueSettingItem<*> -> ValueSettingItemUi(setting, dialogManager, showSummary)
                    is ListSettingItem<*> -> ListSettingItemUi(setting, dialogManager, showSummary)
                    is LinkSettingItem -> SettingItemUi<LinkSettingItem>(setting) { linkAction(it.url) }
                    is VersionSettingItem -> VersionItemUi(setting) { setting.action() }
                }
            }
        }
    }
}

