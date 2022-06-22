package de.baumann.browser.fragment

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.BuildConfig
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.compose.MyTheme
import de.baumann.browser.view.dialog.PrinterDocumentPaperSizeDialog
import de.baumann.browser.view.dialog.TextInputDialog
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsComposeFragment: Fragment(), KoinComponent {
    private val config: ConfigManager by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val composeView = ComposeView(requireContext())
        val version = "Daniel Kao, v" + BuildConfig.VERSION_NAME
        composeView.setContent {
            MyTheme {
                SettingsMainContent(
                    FirstLayerSettingItem.values().toList(),
                    onItemClick = { handleSettingItem(it) },
                    version = version,
                    onVersionClick = {}
                )
            }
        }
        return composeView
    }

    private fun handleSettingItem(setting: FirstLayerSettingItem) {
        when(setting) {
            FirstLayerSettingItem.Ui -> showFragment(UiSettingsFragment())
            FirstLayerSettingItem.Font -> showFragment(FontSettingsFragment())
            FirstLayerSettingItem.Gesture -> showFragment(FragmentSettingsGesture())
            FirstLayerSettingItem.Backup -> showFragment(DataSettingsFragment())
            FirstLayerSettingItem.PdfSize -> PrinterDocumentPaperSizeDialog(requireContext()).show()
            FirstLayerSettingItem.StartControl -> showFragment(StartSettingsFragment())
            FirstLayerSettingItem.ClearControl -> showFragment(ClearDataFragment())
            FirstLayerSettingItem.Search -> showFragment(SearchSettingsFragment())
            FirstLayerSettingItem.UserAgent -> lifecycleScope.launch { updateUserAgent() }
            FirstLayerSettingItem.About -> showFragment(AboutSettingComposeFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    private suspend fun updateUserAgent() {
        val newValue = TextInputDialog(
                requireContext(),
                getString(R.string.setting_title_userAgent),
                "",
                config.customUserAgent
            ).show()

        newValue?.let { config.customUserAgent = it }
    }

}

@Composable
fun <T: SettingItemInterface> SettingsMainContent(
    settings: List<T>,
    onItemClick: (T)->Unit,
    version: String? = null,
    onVersionClick:()->Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(2),
    ){
        itemsIndexed(settings) { _: Int, setting: T ->
            SettingItem(setting, onItemClick)
        }
        if (version != null) {
            item(span = {GridItemSpan(2)}) {
                VersionItem(version = version, onItemClick = onVersionClick)
            }
        }
    }
}

@Composable
private fun <T: SettingItemInterface> SettingItem(
    setting: T,
    onItemClick: (T)->Unit
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
        Spacer(modifier = Modifier
            .width(6.dp)
            .fillMaxHeight())
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = stringResource(id = setting.titleResId),
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
private fun VersionItem(
    version: String,
    onItemClick: ()->Unit
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
            ) { onItemClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.wrapContentWidth().padding(horizontal = 15.dp),
            text = version,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

interface SettingItemInterface {
    val titleResId: Int
    val iconId: Int
}

private enum class FirstLayerSettingItem(
    override val titleResId: Int,
    override val iconId: Int
): SettingItemInterface {
    Ui(R.string.setting_title_ui, R.drawable.icon_ui),
    Font(R.string.setting_title_font, R.drawable.icon_size),
    Gesture(R.string.setting_gestures, R.drawable.gesture_tap),
    Backup(R.string.setting_title_data, R.drawable.icon_backup),
    PdfSize(R.string.setting_title_pdf_paper_size, R.drawable.ic_pdf),
    StartControl(R.string.setting_title_start_control, R.drawable.icon_earth),
    ClearControl(R.string.setting_title_clear_control, R.drawable.icon_delete),
    Search(R.string.setting_title_search, R.drawable.icon_search),
    UserAgent(R.string.setting_title_userAgent, R.drawable.icon_useragent),
    About(R.string.menu_other_info, R.drawable.icon_info),
}

@Preview
@Composable
fun PreviewSettingsMainContent() {
    MyTheme {
        SettingsMainContent(FirstLayerSettingItem.values().toList(), onItemClick = {}, version = "v1.2.3", {})
    }
}