package info.plateaukao.einkbro.view.handlers

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.activity.ToolbarConfigActivity
import info.plateaukao.einkbro.browser.BrowserAction
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
    private val dispatch: (BrowserAction) -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()

    fun handleLongClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Back -> dispatch(BrowserAction.OpenHistoryPage(6))
        ToolbarAction.BoldFont -> dispatch(BrowserAction.ShowFontBoldnessDialog)
        ToolbarAction.Bookmark -> dispatch(BrowserAction.SaveBookmark())
        ToolbarAction.Font -> dispatch(BrowserAction.ToggleReaderMode)
        ToolbarAction.NewTab -> IntentUnit.launchNewBrowser(activity, config.favoriteUrl)
        ToolbarAction.PageDown -> dispatch(BrowserAction.JumpToBottom)
        ToolbarAction.PageInfo -> dispatch(BrowserAction.SummarizeContent)
        ToolbarAction.PageUp -> dispatch(BrowserAction.JumpToTop)
        ToolbarAction.Refresh -> dispatch(BrowserAction.ToggleFullscreen)
        ToolbarAction.Settings -> dispatch(BrowserAction.ShowFastToggleDialog)
        ToolbarAction.TabCount -> config::isIncognitoMode.toggle()
        ToolbarAction.TranslateByParagraph -> dispatch(BrowserAction.ConfigureTranslationLanguage(
            TRANSLATE_API.GOOGLE
        ))

        ToolbarAction.Translation -> dispatch(BrowserAction.ShowTranslationConfigDialog(true))
        ToolbarAction.Tts -> TtsSettingDialogFragment().show(activity.supportFragmentManager, "TtsSettingDialog")
        ToolbarAction.Touch -> dispatch(BrowserAction.ShowTouchAreaDialog)
        ToolbarAction.ChatWithWeb -> dispatch(BrowserAction.ChatWithWeb(useSplitScreen = true))
        else -> {}
    }

    fun handleClick(toolbarAction: ToolbarAction) = when (toolbarAction) {
        ToolbarAction.Back -> dispatch(BrowserAction.HandleBackKey)
        ToolbarAction.BoldFont -> config::boldFontStyle.toggle()
        ToolbarAction.Bookmark -> dispatch(BrowserAction.OpenBookmarkPage)
        ToolbarAction.CloseTab -> dispatch(BrowserAction.RemoveAlbum)
        ToolbarAction.DecreaseFont -> dispatch(BrowserAction.DecreaseFontSize)
        ToolbarAction.Desktop -> config::desktop.toggle()
        ToolbarAction.DuplicateTab -> dispatch(BrowserAction.DuplicateTab)
        ToolbarAction.Font -> dispatch(BrowserAction.ShowFontSizeChangeDialog)
        ToolbarAction.Forward -> dispatch(BrowserAction.GoForward)
        ToolbarAction.FullScreen -> dispatch(BrowserAction.ToggleFullscreen)
        ToolbarAction.GoogleInPlace -> dispatch(BrowserAction.Translate(TranslationMode.GOOGLE_IN_PLACE))
        ToolbarAction.IconSetting ->
            activity.startActivity(Intent(activity, ToolbarConfigActivity::class.java).apply {
                putExtra(ToolbarConfigActivity.EXTRA_IS_READER_MODE, activity is EpubReaderActivity)
            })

        ToolbarAction.IncreaseFont -> dispatch(BrowserAction.IncreaseFontSize)
        ToolbarAction.InputUrl -> dispatch(BrowserAction.FocusOnInput)
        ToolbarAction.MoveToBackground -> activity.moveTaskToBack(true)
        ToolbarAction.NewTab -> dispatch(BrowserAction.NewATab)
        ToolbarAction.PageDown -> dispatch(BrowserAction.PageDown)
        ToolbarAction.PageInfo -> {}
        ToolbarAction.PageUp -> dispatch(BrowserAction.PageUp)
        ToolbarAction.ReaderMode -> dispatch(BrowserAction.ToggleReaderMode)
        ToolbarAction.Refresh -> dispatch(BrowserAction.RefreshAction)
        ToolbarAction.RotateScreen -> dispatch(BrowserAction.RotateScreen)
        ToolbarAction.Search -> dispatch(BrowserAction.ShowSearchPanel)
        ToolbarAction.Settings -> dispatch(BrowserAction.ShowMenuDialog)
        ToolbarAction.Spacer1 -> {}
        ToolbarAction.Spacer2 -> {}
        ToolbarAction.TabCount -> dispatch(BrowserAction.ShowOverview)
        ToolbarAction.TOC -> dispatch(BrowserAction.ShowTocDialog)
        ToolbarAction.Tts -> dispatch(BrowserAction.HandleTtsButton)
        ToolbarAction.Time -> {}
        ToolbarAction.Title -> dispatch(BrowserAction.FocusOnInput)
        ToolbarAction.Touch -> dispatch(BrowserAction.ToggleTouchTurnPage)
        ToolbarAction.TouchDirectionLeftRight -> dispatch(BrowserAction.ToggleSwitchTouchAreaAction)
        ToolbarAction.TouchDirectionUpDown -> dispatch(BrowserAction.ToggleSwitchTouchAreaAction)
        ToolbarAction.Translation -> dispatch(BrowserAction.ShowTranslation)
        ToolbarAction.TranslateByParagraph -> dispatch(BrowserAction.Translate(TranslationMode.TRANSLATE_BY_PARAGRAPH))
        ToolbarAction.VerticalLayout -> dispatch(BrowserAction.ToggleVerticalRead)
        ToolbarAction.SaveEpub -> dispatch(BrowserAction.ShowEpubDialog)
        ToolbarAction.ShareLink -> dispatch(BrowserAction.ShareLink)
        ToolbarAction.InvertColor -> dispatch(BrowserAction.InvertColors)
        ToolbarAction.ChatWithWeb -> dispatch(BrowserAction.ChatWithWeb())
        ToolbarAction.PageAi -> dispatch(BrowserAction.ShowPageAiActionMenu)
        ToolbarAction.AudioOnly -> dispatch(BrowserAction.ToggleAudioOnlyMode)
    }
}
