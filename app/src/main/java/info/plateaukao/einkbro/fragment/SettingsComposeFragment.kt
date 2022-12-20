package info.plateaukao.einkbro.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.PrinterDocumentPaperSizeDialog
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsComposeFragment : Fragment(), KoinComponent {
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
                SettingsMainContent(mainSettings, dialogManager, {}, 2)
            }
        }
        return composeView
    }

    private val mainSettings = listOf(
        ActionSettingItem(R.string.setting_title_ui, R.drawable.ic_phone) { showFragment(createUiSettingFragment()) },
        ActionSettingItem(R.string.setting_title_toolbar, R.drawable.ic_toolbar) {
            showFragment(createToolbarSettingFragment())
        },
        ActionSettingItem(R.string.setting_title_behavior, R.drawable.icon_ui) {
            showFragment(createBehaviorSettingFragment())
        },
        ActionSettingItem(R.string.setting_title_font, R.drawable.icon_size) { showFragment(FontSettingsFragment()) },
        ActionSettingItem(R.string.setting_gestures, R.drawable.gesture_tap) {
            showFragment(FragmentSettingsGesture())
        },
        ActionSettingItem(R.string.setting_title_data, R.drawable.icon_backup) { showFragment(DataSettingsFragment()) },
        ActionSettingItem(
            R.string.setting_title_pdf_paper_size,
            R.drawable.ic_pdf
        ) { PrinterDocumentPaperSizeDialog(requireContext()).show() },
        ActionSettingItem(
            R.string.setting_title_start_control,
            R.drawable.icon_earth
        ) { showFragment(StartSettingsFragment()) },
        ActionSettingItem(
            R.string.setting_title_clear_control,
            R.drawable.icon_delete
        ) { showFragment(ClearDataFragment()) },
        ActionSettingItem(R.string.setting_title_search, R.drawable.icon_search) {
            showFragment(SearchSettingsFragment())
        },
        ActionSettingItem(
            R.string.setting_title_userAgent,
            R.drawable.icon_useragent
        ) { lifecycleScope.launch { updateUserAgent() } },
        ActionSettingItem(
            R.string.setting_title_edit_homepage,
            R.drawable.icon_edit
        ) { lifecycleScope.launch() { updateHomepage() } },
        VersionSettingItem(R.string.title_about, R.drawable.icon_info) {
            showFragment(createAboutFragment())
        },
    )

    private fun showFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(null)
            .commit()
        activity?.setTitle((fragment as FragmentTitleInterface).getTitleId())
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

    private suspend fun updateHomepage() {
        val newValue = TextInputDialog(
            requireContext(),
            getString(R.string.setting_title_edit_homepage),
            "",
            config.favoriteUrl
        ).show()

        newValue?.let { config.favoriteUrl = it }
    }

    private fun createUiSettingFragment() = UISettingsComposeFragment(
        R.string.setting_title_ui, uiSettingItems
    )

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
        ActionSettingItem(
            R.string.setting_clear_recent_bookmarks,
            R.drawable.ic_bookmarks,
            R.string.setting_summary_clear_recent_bookmarks,
        ) {
            config.clearRecentBookmarks()
        },
    )

    private fun createBehaviorSettingFragment() = UISettingsComposeFragment(
        R.string.setting_title_behavior, behaviorSettingItems
    )

    private val behaviorSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_saveTabs,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_saveTabs,
            config::shouldSaveTabs,
        ),
        BooleanSettingItem(
            R.string.setting_title_background_loading,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_background_loading,
            config::enableWebBkgndLoad,
        ),
        BooleanSettingItem(
            R.string.setting_title_trim_input_url,
            R.drawable.icon_edit,
            R.string.setting_summary_trim_input_url,
            config::shouldTrimInputUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_prune_query_parameter,
            R.drawable.ic_filter,
            R.string.setting_summary_prune_query_parameter,
            config::shouldPruneQueryParameters,
        ),
        BooleanSettingItem(
            R.string.setting_title_screen_awake,
            R.drawable.ic_eye,
            R.string.setting_summary_screen_awake,
            config::keepAwake,
        ),
        BooleanSettingItem(
            R.string.setting_title_confirm_tab_close,
            R.drawable.icon_close,
            R.string.setting_summary_confirm_tab_close,
            config::confirmTabClose,
        ),
        BooleanSettingItem(
            R.string.setting_title_vi_binding,
            R.drawable.ic_keyboard,
            R.string.setting_summary_vi_binding,
            config::enableViBinding,
        ),
        BooleanSettingItem(
            R.string.setting_title_useUpDown,
            R.drawable.ic_page_down,
            R.string.setting_summary_useUpDownKey,
            config::useUpDownPageTurn,
        ),
    )

    private fun createToolbarSettingFragment() = UISettingsComposeFragment(
        R.string.setting_title_toolbar, toolbarSettingItems
    )

    private val toolbarSettingItems = listOf(
        BooleanSettingItem(
            R.string.setting_title_toolbar_top,
            R.drawable.ic_page_height,
            R.string.setting_summary_toolbar_top,
            config::isToolbarOnTop,
        ),
        BooleanSettingItem(
            R.string.setting_title_hideToolbar,
            R.drawable.icon_fullscreen,
            R.string.setting_summary_hide,
            config::shouldHideToolbar,
        ),
        BooleanSettingItem(
            R.string.setting_title_toolbarShow,
            R.drawable.icon_show,
            R.string.setting_summary_toolbarShow,
            config::showToolbarFirst,
        ),
        BooleanSettingItem(
            R.string.setting_title_show_tab_bar,
            R.drawable.icon_tab_plus,
            R.string.setting_summary_show_tab_bar,
            config::shouldShowTabBar,
        ),
    )

    private fun createAboutFragment() = UISettingsComposeFragment(
        R.string.title_about, LinkSettingItem.values().toList()
    )
}