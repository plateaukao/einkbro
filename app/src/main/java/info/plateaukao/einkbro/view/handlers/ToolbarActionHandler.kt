package info.plateaukao.einkbro.view.handlers

import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.NinjaWebView
import info.plateaukao.einkbro.view.dialog.compose.ToolbarConfigDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TouchAreaDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarActionHandler(
    private val activity: FragmentActivity,
    private val ninjaWebView: NinjaWebView,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val browserController = activity as BrowserController

    fun handleLongClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
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

    fun handleClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Title -> browserController.focusOnInput()
        ToolbarAction.Back -> browserController.handleBackKey()
        ToolbarAction.Refresh -> browserController.refreshAction()
        ToolbarAction.Touch -> browserController.toggleTouchTurnPageFeature()
        ToolbarAction.PageUp -> ninjaWebView.pageUpWithNoAnimation()
        ToolbarAction.PageDown -> ninjaWebView.pageDownWithNoAnimation()
        ToolbarAction.TabCount -> browserController.showOverview()
        ToolbarAction.Font -> browserController.showFontSizeChangeDialog()
        ToolbarAction.Settings -> browserController.showMenuDialog()
        ToolbarAction.Bookmark -> browserController.openBookmarkPage()
        ToolbarAction.IconSetting -> ToolbarConfigDialogFragment().show(
            activity.supportFragmentManager,
            "toolbar_config"
        )

        ToolbarAction.VerticalLayout -> ninjaWebView.toggleVerticalRead()
        ToolbarAction.ReaderMode -> ninjaWebView.toggleReaderMode()
        ToolbarAction.BoldFont -> config::boldFontStyle.toggle()
        ToolbarAction.IncreaseFont -> browserController.increaseFontSize()
        ToolbarAction.DecreaseFont -> browserController.decreaseFontSize()
        ToolbarAction.FullScreen -> browserController.fullscreen()
        ToolbarAction.Forward -> if (ninjaWebView.canGoForward()) ninjaWebView.goForward() else {
        }

        ToolbarAction.RotateScreen -> browserController.rotateScreen()
        ToolbarAction.Translation -> browserController.showTranslation()
        ToolbarAction.CloseTab -> browserController.removeAlbum()
        ToolbarAction.InputUrl -> browserController.focusOnInput()
        ToolbarAction.NewTab -> browserController.newATab()
        ToolbarAction.Desktop -> config::desktop.toggle()
        ToolbarAction.Search -> browserController.showSearchPanel()
        ToolbarAction.DuplicateTab -> browserController.duplicateTab()
        ToolbarAction.Tts -> browserController.toggleTtsRead()
        ToolbarAction.TOC -> browserController.showTocDialog()
        else -> {}
    }
}