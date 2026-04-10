package info.plateaukao.einkbro.browser

import android.net.Uri
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API

//region Sub-interface: TabController
interface TabController {
    fun updateProgress(progress: Int)
    fun updateTitle(title: String?)
    fun addNewTab(url: String)
    fun removeAlbum(albumController: AlbumController, showHomePage: Boolean = false)
    fun removeAlbum()
    fun updateAlbum(url: String?)
    fun isCurrentAlbum(albumController: AlbumController): Boolean
    fun showAlbum(albumController: AlbumController)
    fun newATab()
    fun duplicateTab()
    fun gotoLeftTab()
    fun gotoRightTab()
}
//endregion

//region Sub-interface: NavigationController
interface NavigationController {
    // Navigation
    fun isAtTop(): Boolean
    fun loadInSecondPane(url: String): Boolean
    fun goForward()
    fun jumpToTop()
    fun jumpToBottom()
    fun pageDown()
    fun pageUp()
    fun refreshAction()

    // History and Bookmarks
    fun addHistory(title: String, url: String)
    fun saveBookmark(url: String? = null, title: String? = null)
    fun openHistoryPage(amount: Int = 0)
    fun openBookmarkPage()

    // Page Content Interaction
    fun toggleReaderMode()
    fun toggleVerticalRead()
    fun increaseFontSize()
    fun decreaseFontSize()
    fun showFontSizeChangeDialog()
    fun showFontBoldnessDialog()
    fun invertColors()
    fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float)
}
//endregion

//region Sub-interface: ViewStateController
interface ViewStateController {
    fun onShowCustomView(view: View?, callback: CustomViewCallback?)
    fun onHideCustomView(): Boolean
    fun hideOverview()
    fun showOverview()
    fun toggleFullscreen()
    fun toggleSplitScreen(url: String? = null)
}
//endregion

//region Sub-interface: TranslationController
interface TranslationController {
    fun showTranslation(webView: EBWebView? = null)
    fun showTranslationConfigDialog(translateDirectly: Boolean)
    fun translate(translationMode: TranslationMode)
    fun resetTranslateUI()
    fun configureTranslationLanguage(translateApi: TRANSLATE_API)
}
//endregion

//region Sub-interface: TtsController
interface TtsController {
    fun handleTtsButton()
    fun showTtsLanguageDialog()
}
//endregion

//region Sub-interface: InputController
interface InputController {
    // Input Handling
    fun onLongPress(message: Message, event: MotionEvent?)
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun handleBackKey()
    fun focusOnInput()

    // File Handling
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
    fun showEpubDialog()
    fun showWebArchiveFilePicker()
    fun showOpenEpubFilePicker()
    fun savePageForLater()
    fun showSavedPages()

    // Search
    fun showSearchPanel()
    fun toggleTextSearch()
    fun toggleReceiveTextSearch()

    // Sharing and External Services
    fun createShortcut()
    fun sendToRemote(text: String)
    fun shareLink()
    fun addToInstapaper()
    fun configureInstapaper()

    // Gestures and Touch Controls
    fun toggleTouchTurnPageFeature()
    fun toggleSwitchTouchAreaAction()
    fun showTouchAreaDialog()
    fun toggleTouchPagination()

    // E-Reader Specific
    fun showTocDialog() = Unit

    // Page Information
    fun updatePageInfo(info: String)

    // Action Mode
    fun isActionModeActive(): Boolean = false
    fun dismissActionMode() {}

    // Key Sending
    fun sendPageUpKey()
    fun sendPageDownKey()
    fun sendLeftKey()
    fun sendRightKey()

    // Audio/Video
    fun toggleAudioOnlyMode()

    // AI Features
    fun summarizeContent()
    fun chatWithWeb(useSplitScreen: Boolean = false, content: String? = null, runWithAction: ChatGPTActionInfo? = null)
    fun showPageAiActionMenu()

    // UI Toggles and Dialogs
    fun showFastToggleDialog()
    fun showMenuDialog()
    fun rotateScreen()
    fun toggleReceiveLink()
}
//endregion

interface BrowserController : TabController, NavigationController, ViewStateController, TranslationController, TtsController, InputController
