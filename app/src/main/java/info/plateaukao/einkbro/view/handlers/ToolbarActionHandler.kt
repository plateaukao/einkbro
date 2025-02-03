package info.plateaukao.einkbro.view.handlers

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.activity.ToolbarConfigActivity
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarActionHandler(
    private val activity: FragmentActivity,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val browserController = activity as BrowserController

    fun handleLongClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Back -> browserController.openHistoryPage(6)
        ToolbarAction.BoldFont -> browserController.showFontBoldnessDialog()
        ToolbarAction.Bookmark -> browserController.saveBookmark()
        ToolbarAction.Font -> browserController.toggleReaderMode()
        ToolbarAction.NewTab -> IntentUnit.launchNewBrowser(activity, config.favoriteUrl)
        ToolbarAction.PageDown -> browserController.jumpToBottom()
        ToolbarAction.PageInfo -> browserController.summarizeContent()
        ToolbarAction.PageUp -> browserController.jumpToTop()
        ToolbarAction.PapagoByParagraph -> browserController.configureTranslationLanguage(
            TRANSLATE_API.PAPAGO
        )

        ToolbarAction.Refresh -> browserController.toggleFullscreen()
        ToolbarAction.Settings -> browserController.showFastToggleDialog()
        ToolbarAction.TabCount -> config::isIncognitoMode.toggle()
        ToolbarAction.TranslateByParagraph -> browserController.configureTranslationLanguage(
            TRANSLATE_API.GOOGLE
        )

        ToolbarAction.Translation -> browserController.showTranslationConfigDialog(true)
        ToolbarAction.Tts -> TtsSettingDialogFragment().show(activity.supportFragmentManager, "TtsSettingDialog")
        ToolbarAction.Touch -> browserController.showTouchAreaDialog()
        else -> {}
    }

    fun handleClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Back -> browserController.handleBackKey()
        ToolbarAction.BoldFont -> config::boldFontStyle.toggle()
        ToolbarAction.Bookmark -> browserController.openBookmarkPage()
        ToolbarAction.CloseTab -> browserController.removeAlbum()
        ToolbarAction.DecreaseFont -> browserController.decreaseFontSize()
        ToolbarAction.Desktop -> config::desktop.toggle()
        ToolbarAction.DuplicateTab -> browserController.duplicateTab()
        ToolbarAction.Font -> browserController.showFontSizeChangeDialog()
        ToolbarAction.Forward -> browserController.goForward()
        ToolbarAction.FullScreen -> browserController.toggleFullscreen()
        ToolbarAction.GoogleInPlace -> browserController.translate(TranslationMode.GOOGLE_IN_PLACE)
        ToolbarAction.IconSetting ->
            activity.startActivity(Intent(activity, ToolbarConfigActivity::class.java))

        ToolbarAction.IncreaseFont -> browserController.increaseFontSize()
        ToolbarAction.InputUrl -> browserController.focusOnInput()
        ToolbarAction.MoveToBackground -> activity.moveTaskToBack(true)
        ToolbarAction.NewTab -> browserController.newATab()
        ToolbarAction.PageDown -> browserController.pageDown()
        ToolbarAction.PageInfo -> {}
        ToolbarAction.PageUp -> browserController.pageUp()
        ToolbarAction.PapagoByParagraph -> browserController.translate(TranslationMode.PAPAGO_TRANSLATE_BY_PARAGRAPH)
        ToolbarAction.ReaderMode -> browserController.toggleReaderMode()
        ToolbarAction.Refresh -> browserController.refreshAction()
        ToolbarAction.RotateScreen -> browserController.rotateScreen()
        ToolbarAction.Search -> browserController.showSearchPanel()
        ToolbarAction.Settings -> browserController.showMenuDialog()
        ToolbarAction.Spacer1 -> {}
        ToolbarAction.Spacer2 -> {}
        ToolbarAction.TabCount -> browserController.showOverview()
        ToolbarAction.TOC -> browserController.showTocDialog()
        ToolbarAction.Tts -> browserController.handleTtsButton()
        ToolbarAction.Time -> {}
        ToolbarAction.Title -> browserController.focusOnInput()
        ToolbarAction.Touch -> browserController.toggleTouchTurnPageFeature()
        ToolbarAction.TouchDirectionLeftRight -> browserController.toggleSwitchTouchAreaAction()
        ToolbarAction.TouchDirectionUpDown -> browserController.toggleSwitchTouchAreaAction()
        ToolbarAction.Translation -> browserController.showTranslation()
        ToolbarAction.TranslateByParagraph -> browserController.translate(TranslationMode.TRANSLATE_BY_PARAGRAPH)
        ToolbarAction.VerticalLayout -> browserController.toggleVerticalRead()
        ToolbarAction.SaveEpub -> browserController.showSaveEpubDialog()
        ToolbarAction.ShareLink -> browserController.shareLink()
        ToolbarAction.InvertColor -> browserController.invertColors()
    }
}