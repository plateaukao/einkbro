package info.plateaukao.einkbro.view.handlers

import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.view.GestureType

class GestureHandler(
    private val dispatch: (BrowserAction) -> Unit,
) {

    fun handle(gesture: GestureType) = when (gesture) {
        GestureType.NothingHappen -> Unit
        GestureType.Forward -> dispatch(BrowserAction.GoForward)
        GestureType.Backward -> dispatch(BrowserAction.HandleBackKey)
        GestureType.ScrollToTop -> dispatch(BrowserAction.JumpToTop)
        GestureType.ScrollToBottom -> dispatch(BrowserAction.JumpToBottom)
        GestureType.ToLeftTab -> dispatch(BrowserAction.GotoLeftTab)
        GestureType.ToRightTab -> dispatch(BrowserAction.GotoRightTab)
        GestureType.Overview -> dispatch(BrowserAction.ShowOverview)
        GestureType.OpenNewTab -> dispatch(BrowserAction.NewATab)
        GestureType.CloseTab -> dispatch(BrowserAction.RemoveAlbum)
        GestureType.PageUp -> dispatch(BrowserAction.PageUp)
        GestureType.PageDown -> dispatch(BrowserAction.PageDown)
        GestureType.Bookmark -> dispatch(BrowserAction.OpenBookmarkPage)
        GestureType.Back -> dispatch(BrowserAction.HandleBackKey)
        GestureType.Fullscreen -> dispatch(BrowserAction.ToggleFullscreen)
        GestureType.Refresh -> dispatch(BrowserAction.RefreshAction)
        GestureType.Menu -> dispatch(BrowserAction.ShowMenuDialog)
        GestureType.TouchPagination -> dispatch(BrowserAction.ToggleTouchPagination)
        GestureType.KeyPageUp -> dispatch(BrowserAction.SendPageUpKey)
        GestureType.KeyPageDown -> dispatch(BrowserAction.SendPageDownKey)
        GestureType.KeyLeft -> dispatch(BrowserAction.SendLeftKey)
        GestureType.KeyRight -> dispatch(BrowserAction.SendRightKey)
        GestureType.InputUrl -> dispatch(BrowserAction.FocusOnInput)
    }
}
