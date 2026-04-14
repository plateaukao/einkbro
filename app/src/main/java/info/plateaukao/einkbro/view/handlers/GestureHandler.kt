package info.plateaukao.einkbro.view.handlers

import info.plateaukao.einkbro.browser.BrowserAction

class GestureHandler(
    private val dispatch: (BrowserAction) -> Unit,
) {
    fun handle(action: BrowserAction) {
        if (action is BrowserAction.Noop) return
        dispatch(action)
    }
}
