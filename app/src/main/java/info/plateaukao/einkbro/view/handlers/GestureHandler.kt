package info.plateaukao.einkbro.view.handlers

import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.view.GestureType

class GestureHandler(
    private val browserController: BrowserController,
) {

    fun handle(gesture: GestureType) = when (gesture) {
        GestureType.NothingHappen -> Unit
        GestureType.Forward -> browserController.goForward()
        GestureType.Backward -> browserController.handleBackKey()
        GestureType.ScrollToTop -> browserController.jumpToTop()
        GestureType.ScrollToBottom -> browserController.jumpToBottom()
        GestureType.ToLeftTab -> browserController.gotoLeftTab()
        GestureType.ToRightTab -> browserController.gotoRightTab()
        GestureType.Overview -> browserController.showOverview()
        GestureType.OpenNewTab -> browserController.newATab()
        GestureType.CloseTab -> browserController.removeAlbum()
        GestureType.PageUp -> browserController.pageUp()
        GestureType.PageDown -> browserController.pageDown()
        GestureType.Bookmark -> browserController.openBookmarkPage()
        GestureType.Back -> browserController.handleBackKey()
        GestureType.Fullscreen -> browserController.toggleFullscreen()
        GestureType.Refresh -> browserController.refreshAction()
        GestureType.Menu -> browserController.showMenuDialog()
        GestureType.TouchPagination -> browserController.toggleTouchPagination()
        GestureType.KeyPageUp -> browserController.sendPageUpKey()
        GestureType.KeyPageDown -> browserController.sendPageDownKey()
        GestureType.KeyLeft -> browserController.sendLeftKey()
        GestureType.KeyRight -> browserController.sendRightKey()
    }
}