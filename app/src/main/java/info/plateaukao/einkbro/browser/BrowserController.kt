package info.plateaukao.einkbro.browser

import android.net.Uri
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import info.plateaukao.einkbro.view.Album

interface BrowserController {
    fun updateProgress(progress: Int)
    fun updateTitle(title: String?)
    fun addNewTab(url: String)
    fun showAlbum(albumController: AlbumController)
    fun removeAlbum(albumController: AlbumController)
    fun removeAlbum()
    fun updateAlbum(url: String?)
    fun onUpdateAlbum(album: Album)
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
    fun onShowCustomView(view: View?, callback: CustomViewCallback?)
    fun onLongPress(message: Message, event: MotionEvent?)
    fun hideOverview()
    fun addHistory(title: String, url: String)
    fun onHideCustomView(): Boolean
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun loadInSecondPane(url: String): Boolean //void updateTabs(Album album);

    // for menu actions
    fun toggleTtsRead()
    fun showFastToggleDialog()
    fun toggleSplitScreen(url: String? = null)
    fun showTranslation()
    fun saveBookmark(url: String? = null, title: String? = null)
    fun showSaveEpubDialog()
    fun showFontSizeChangeDialog()
    fun showSearchPanel()
    fun showWebArchiveFilePicker()
    //for tool bar long click
    fun openHistoryPage(amount: Int = 0)
    fun showTranslationConfigDialog()
    fun fullscreen()

    // toolbar click
    fun focusOnInput()
    fun handleBackKey()
    fun refreshAction()
    fun toggleTouchTurnPageFeature()
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
}