package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API

sealed class BrowserAction {
    // Tab management
    object NewATab : BrowserAction()
    object DuplicateTab : BrowserAction()
    object RemoveAlbum : BrowserAction()
    object GotoLeftTab : BrowserAction()
    object GotoRightTab : BrowserAction()
    data class AddNewTab(val url: String) : BrowserAction()
    data class UpdateAlbum(val url: String?) : BrowserAction()

    // Navigation
    object GoForward : BrowserAction()
    object HandleBackKey : BrowserAction()
    object RefreshAction : BrowserAction()
    object JumpToTop : BrowserAction()
    object JumpToBottom : BrowserAction()
    object PageUp : BrowserAction()
    object PageDown : BrowserAction()
    object SendPageUpKey : BrowserAction()
    object SendPageDownKey : BrowserAction()
    object SendLeftKey : BrowserAction()
    object SendRightKey : BrowserAction()

    // Content
    object ToggleReaderMode : BrowserAction()
    object ToggleVerticalRead : BrowserAction()
    object IncreaseFontSize : BrowserAction()
    object DecreaseFontSize : BrowserAction()
    object ShowFontSizeChangeDialog : BrowserAction()
    object ShowFontBoldnessDialog : BrowserAction()
    object InvertColors : BrowserAction()

    // View state
    object ShowOverview : BrowserAction()
    object ToggleFullscreen : BrowserAction()
    data class ToggleSplitScreen(val url: String? = null) : BrowserAction()

    // Translation
    object ShowTranslation : BrowserAction()
    data class ShowTranslationConfigDialog(val translateDirectly: Boolean) : BrowserAction()
    data class Translate(val mode: TranslationMode) : BrowserAction()
    data class ConfigureTranslationLanguage(val api: TRANSLATE_API) : BrowserAction()

    // TTS
    object HandleTtsButton : BrowserAction()

    // Bookmarks / History
    object OpenBookmarkPage : BrowserAction()
    data class OpenHistoryPage(val amount: Int = 0) : BrowserAction()
    data class SaveBookmark(val url: String? = null, val title: String? = null) : BrowserAction()

    // Search
    object ShowSearchPanel : BrowserAction()
    object ToggleTextSearch : BrowserAction()
    object ToggleReceiveTextSearch : BrowserAction()

    // Share
    object CreateShortcut : BrowserAction()
    object ShareLink : BrowserAction()
    data class SendToRemote(val text: String) : BrowserAction()
    object AddToInstapaper : BrowserAction()
    object ConfigureInstapaper : BrowserAction()
    object ToggleReceiveLink : BrowserAction()

    // Touch config
    object ToggleTouchTurnPage : BrowserAction()
    object ToggleSwitchTouchAreaAction : BrowserAction()
    object ShowTouchAreaDialog : BrowserAction()
    object ToggleTouchPagination : BrowserAction()

    // AI
    object SummarizeContent : BrowserAction()
    data class ChatWithWeb(
        val useSplitScreen: Boolean = false,
        val content: String? = null,
        val runWithAction: ChatGPTActionInfo? = null,
    ) : BrowserAction()
    object ShowPageAiActionMenu : BrowserAction()

    // File
    object ShowEpubDialog : BrowserAction()
    object SavePageForLater : BrowserAction()
    object ShowSavedPages : BrowserAction()
    object SaveWebArchive : BrowserAction()

    // Dialog / UI
    object FocusOnInput : BrowserAction()
    object ShowMenuDialog : BrowserAction()
    object ShowFastToggleDialog : BrowserAction()
    object ShowTocDialog : BrowserAction()
    object RotateScreen : BrowserAction()
    object ToggleAudioOnlyMode : BrowserAction()
}
