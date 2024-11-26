package info.plateaukao.einkbro.browser

import android.net.Uri
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API

interface BrowserController {
    fun updateProgress(progress: Int)
    fun updateTitle(title: String?)
    fun addNewTab(url: String)
    fun isAtTop(): Boolean
    fun isCurrentAlbum(albumController: AlbumController): Boolean
    fun showAlbum(albumController: AlbumController)

    // showHomePage: true -> show home page if it's the last tab
    fun removeAlbum(albumController: AlbumController, showHomePage: Boolean = false)
    fun removeAlbum()
    fun updateAlbum(url: String?)
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
    fun onShowCustomView(view: View?, callback: CustomViewCallback?)
    fun onLongPress(message: Message, event: MotionEvent?)
    fun hideOverview()
    fun addHistory(title: String, url: String)
    fun onHideCustomView(): Boolean
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun loadInSecondPane(url: String): Boolean //void updateTabs(Album album);

    // for menu actions
    fun handleTtsButton()
    fun showTtsLanguageDialog()
    fun showFastToggleDialog()
    fun toggleSplitScreen(url: String? = null)
    fun showTranslation(webView: EBWebView? = null)
    fun saveBookmark(url: String? = null, title: String? = null)
    fun createShortcut()
    fun showSaveEpubDialog()
    fun showFontSizeChangeDialog()
    fun showSearchPanel()
    fun showWebArchiveFilePicker()
    fun showOpenEpubFilePicker()

    //for tool bar long click
    fun openHistoryPage(amount: Int = 0)

    //
    fun showTranslationConfigDialog(translateDirectly: Boolean)

    fun showTouchAreaDialog()
    fun toggleFullscreen()

    // toolbar click
    fun focusOnInput()
    fun handleBackKey()
    fun refreshAction()
    fun toggleTouchTurnPageFeature()
    fun toggleSwitchTouchAreaAction()
    fun showOverview()
    fun showMenuDialog()
    fun openBookmarkPage()
    fun increaseFontSize()
    fun decreaseFontSize()
    fun rotateScreen()
    fun newATab()
    fun duplicateTab()
    fun toggleReaderMode()
    fun toggleVerticalRead()

    // ereader activity
    fun showTocDialog() = Unit

    // gesture
    fun gotoLeftTab()
    fun gotoRightTab()
    fun goForward()

    fun jumpToTop()
    fun jumpToBottom()
    fun pageDown()
    fun pageUp()
    fun updatePageInfo(info: String)

    fun sendPageUpKey()
    fun sendPageDownKey()
    fun sendLeftKey()
    fun sendRightKey()

    fun addToPocket(url: String)
    fun handlePocketRequestToken(requestToken: String)

    fun translate(translationMode: TranslationMode)

    fun resetTranslateUI()

    fun configureTranslationLanguage(translateApi: TRANSLATE_API)
    fun toggleTouchPagination()
    fun showFontBoldnessDialog()

    fun sendToRemote(text: String)
    fun toggleReceiveLink()
    fun toggleTextSearch()
    fun toggleReceiveTextSearch()

    fun summarizeContent()

    fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float)
}