package info.plateaukao.einkbro.view.handlers

import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarActionHandler (
    private val activity: FragmentActivity,
    private val ninjaWebView: NinjaWebView,
): KoinComponent {
    private val config: ConfigManager by inject()
    private val browserController = activity as BrowserController
    private val dialogManager by lazy {  DialogManager(activity) }

    fun handlerLongClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Back -> browserController.openHistoryPage(5)
        ToolbarAction.Refresh -> browserController.fullscreen()
        ToolbarAction.Touch -> TouchAreaDialogFragment().show(
            activity.supportFragmentManager,
            "TouchAreaDialog"
        )

        ToolbarAction.PageUp -> ninjaWebView.jumpToTop()
        ToolbarAction.PageDown -> ninjaWebView.jumpToBottom()
        ToolbarAction.TabCount -> config::isIncognitoMode.toggle()
        ToolbarAction.Settings -> browserController.showFastToggleDialog()
        ToolbarAction.Bookmark -> browserController.saveBookmark()
        ToolbarAction.Translation -> browserController.showTranslationConfigDialog()
        ToolbarAction.NewTab -> IntentUnit.launchNewBrowser(activity, config.favoriteUrl)
        ToolbarAction.Tts ->
            TtsSettingDialogFragment { IntentUnit.gotoSystemTtsSettings(activity) }
            .show(activity.supportFragmentManager, "TtsSettingDialog")

        ToolbarAction.Font -> ninjaWebView.toggleReaderMode()
        else -> {}
    }
}