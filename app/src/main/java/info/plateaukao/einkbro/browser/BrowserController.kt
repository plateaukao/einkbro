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

//region Phase 2: Split InputController into focused sub-interfaces

interface KeyInputController {
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun handleBackKey()
    fun sendPageUpKey()
    fun sendPageDownKey()
    fun sendLeftKey()
    fun sendRightKey()
}

interface FileController {
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
    fun showEpubDialog()
    fun showWebArchiveFilePicker()
    fun showOpenEpubFilePicker()
    fun savePageForLater()
    fun showSavedPages()
}

interface SearchController {
    fun showSearchPanel()
    fun toggleTextSearch()
    fun toggleReceiveTextSearch()
}

interface ShareController {
    fun createShortcut()
    fun sendToRemote(text: String)
    fun shareLink()
    fun addToInstapaper()
    fun configureInstapaper()
}

interface TouchConfigController {
    fun toggleTouchTurnPageFeature()
    fun toggleSwitchTouchAreaAction()
    fun showTouchAreaDialog()
    fun toggleTouchPagination()
}

interface AiFeatureController {
    fun summarizeContent()
    fun chatWithWeb(useSplitScreen: Boolean = false, content: String? = null, runWithAction: ChatGPTActionInfo? = null)
    fun showPageAiActionMenu()
}

interface DialogController {
    fun showFastToggleDialog()
    fun showMenuDialog()
    fun rotateScreen()
    fun toggleReceiveLink()
}

//endregion

//region Sub-interface: InputController (remaining core methods)
interface InputController :
    KeyInputController, FileController, SearchController,
    ShareController, TouchConfigController, AiFeatureController, DialogController {
    fun onLongPress(message: Message, event: MotionEvent?)
    fun focusOnInput()

    // E-Reader Specific
    fun showTocDialog() = Unit

    // Page Information
    fun updatePageInfo(info: String)

    // Action Mode
    fun isActionModeActive(): Boolean = false
    fun dismissActionMode() {}

    // Audio/Video
    fun toggleAudioOnlyMode()
}
//endregion

//region Phase 1: Narrow callback interfaces for specific consumers

/** Narrow interface for EBWebView — only the callbacks it actually uses */
interface WebViewCallback {
    fun updateProgress(progress: Int)
    fun updateTitle(title: String?)
    fun addHistory(title: String, url: String)
    fun loadInSecondPane(url: String): Boolean
    fun resetTranslateUI()
    fun showTranslation(webView: EBWebView?)
    fun handleKeyEvent(event: KeyEvent): Boolean
    fun isActionModeActive(): Boolean
    fun dismissActionMode()
}

/** Narrow interface for Album — only the callbacks it actually uses */
interface AlbumCallback {
    fun isCurrentAlbum(albumController: AlbumController): Boolean
    fun isAtTop(): Boolean
    fun refreshAction()
    fun jumpToTop()
    fun showAlbum(albumController: AlbumController)
    fun removeAlbum(albumController: AlbumController, showHomePage: Boolean)
}

/** Composite of sub-interfaces GestureHandler actually uses */
interface GestureCallback :
    TabController, NavigationController, ViewStateController,
    KeyInputController, TouchConfigController, DialogController {
    fun focusOnInput()
}

/** Composite of sub-interfaces KeyHandler actually uses */
interface KeyHandlerCallback :
    TabController, NavigationController, ViewStateController,
    TranslationController, KeyInputController, SearchController, DialogController {
    fun focusOnInput()
}

/** Narrow interface for EBWebChromeClient — only the browser callbacks it needs */
interface WebChromeCallback {
    fun addNewTab(url: String)
    fun onShowCustomView(view: View?, callback: CustomViewCallback?)
    fun onHideCustomView(): Boolean
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)
}

/** Narrow interface for JsWebInterface — only the browser callbacks it needs */
interface JsBrowserCallback {
    fun updateSelectionRect(left: Float, top: Float, right: Float, bottom: Float)
    fun isActionModeActive(): Boolean
    fun dismissActionMode()
}

//endregion

interface BrowserController :
    TabController, NavigationController, ViewStateController,
    TranslationController, TtsController, InputController,
    WebViewCallback, AlbumCallback,
    GestureCallback, KeyHandlerCallback,
    WebChromeCallback, JsBrowserCallback {
    // Resolve diamond conflicts for methods with default values in multiple parent interfaces
    override fun isActionModeActive(): Boolean = false
    override fun dismissActionMode() {}
}
