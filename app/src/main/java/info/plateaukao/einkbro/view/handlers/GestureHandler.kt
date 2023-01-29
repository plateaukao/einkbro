package info.plateaukao.einkbro.view.handlers

import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.view.GestureType
import info.plateaukao.einkbro.view.NinjaWebView
import org.koin.core.component.KoinComponent

class GestureHandler(
    private val activity: FragmentActivity,
    private val ninjaWebView: NinjaWebView,
) : KoinComponent {
    private val browserController = activity as BrowserController

    fun handle(gesture: GestureType) = when (gesture) {
        GestureType.NothingHappen -> Unit
        GestureType.Forward -> browserController.goForward()
        GestureType.Backward ->  browserController.handleBackKey()
        GestureType.ScrollToTop -> ninjaWebView.jumpToTop()
        GestureType.ScrollToBottom -> ninjaWebView.pageDownWithNoAnimation()
        GestureType.ToLeftTab -> browserController.gotoLeftTab()
        GestureType.ToRightTab -> browserController.gotoRightTab()
        GestureType.Overview -> browserController.showOverview()
        GestureType.OpenNewTab -> browserController.newATab()
        GestureType.CloseTab -> browserController.removeAlbum()
        GestureType.PageUp -> ninjaWebView.pageUpWithNoAnimation()
        GestureType.PageDown -> ninjaWebView.pageDownWithNoAnimation()
        GestureType.Bookmark -> browserController.openBookmarkPage()
        GestureType.Back -> browserController.handleBackKey()
        GestureType.Fullscreen -> browserController.fullscreen()
        GestureType.Refresh -> ninjaWebView.reload()
        GestureType.Menu -> browserController.showMenuDialog()
    }
}