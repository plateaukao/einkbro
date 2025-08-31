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

interface BrowserController {
    //region Tab and Album Management
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
    //endregion

    //region View Management
    fun onShowCustomView(view: View?, callback: CustomViewCallback?)
    fun onHideCustomView(): Boolean
    fun hideOverview()
    fun showOverview()
    fun toggleFullscreen()
    fun toggleSplitScreen(url: String? = null)
    //endregion

    //region History and Bookmarks
    fun addHistory(title: String, url: String)
    fun saveBookmark(url: String? = null, title: String? = null)
    fun openHistoryPage(amount: Int = 0)
    fun openBookmarkPage()
    //endregion

    //region File Handling
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
    fun showSaveEpubDialog()
    fun showWebArchiveFilePicker()
    fun showOpenEpubFilePicker()
    //endregion

    //region Input Handling
    fun onLongPress(message: Message, event: MotionEvent?)
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun handleBackKey()
    fun focusOnInput()
    //endregion

    //region Navigation
    fun isAtTop(): Boolean
    fun loadInSecondPane(url: String): Boolean //void updateTabs(Album album);
    fun goForward()
    fun jumpToTop()
    fun jumpToBottom()
    fun pageDown()
    fun pageUp()
    fun refreshAction()
    //endregion

    //region Text-to-Speech (TTS)
    fun handleTtsButton()
    fun showTtsLanguageDialog()
    //endregion

    //region Translation
    fun showTranslation(webView: EBWebView? = null)
    fun showTranslationConfigDialog(translateDirectly: Boolean)
    fun translate(translationMode: TranslationMode)
    fun resetTranslateUI()
    fun configureTranslationLanguage(translateApi: TRANSLATE_API)
    //endregion

    //region Page Content Interaction
    fun toggleReaderMode()
    fun toggleVerticalRead()
    fun increaseFontSize()
    fun decreaseFontSize()
    fun showFontSizeChangeDialog()
    fun showFontBoldnessDialog()
    fun invertColors()
    fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float)
    //endregion

    //region Sharing and External Services
    fun createShortcut()
    fun sendToRemote(text: String)
    fun shareLink()
    fun addToInstapaper()
    fun configureInstapaper()
    //endregion

    //region Search
    fun showSearchPanel()
    fun toggleTextSearch()
    fun toggleReceiveTextSearch()
    //endregion

    //region E-Reader Specific
    fun showTocDialog() = Unit // Specific to EReaderActivity, default implementation
    //endregion

    //region Gestures and Touch Controls
    fun toggleTouchTurnPageFeature()
    fun toggleSwitchTouchAreaAction()
    fun showTouchAreaDialog()
    fun toggleTouchPagination()
    //endregion

    //region Page Information
    fun updatePageInfo(info: String)
    //endregion

    //region Key Sending
    fun sendPageUpKey()
    fun sendPageDownKey()
    fun sendLeftKey()
    fun sendRightKey()
    //endregion

    //region AI Features
    fun summarizeContent()
    fun chatWithWeb(useSplitScreen: Boolean = false, content: String? = null, runWithAction: ChatGPTActionInfo? = null)
    //endregion

    //region UI Toggles and Dialogs
    fun showFastToggleDialog()
    fun showMenuDialog()
    fun rotateScreen()
    fun toggleReceiveLink()
    //endregion
}
