package info.plateaukao.einkbro.activity

import android.view.KeyEvent
import android.webkit.WebView
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.EBWebView

class KeyHandler(
    private val browserController: BrowserController,
    private var ebWebView: EBWebView,
    private val config: ConfigManager
) {
    private var previousKeyEvent: KeyEvent? = null

    fun setWebView(webView: EBWebView) {
        ebWebView = webView
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (config.useUpDownPageTurn) ebWebView.pageDownWithNoAnimation()
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (config.useUpDownPageTurn) ebWebView.pageUpWithNoAnimation()
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> return handleVolumeDownKey()
            KeyEvent.KEYCODE_VOLUME_UP -> return handleVolumeUpKey()
            KeyEvent.KEYCODE_MENU -> {
                browserController.showMenuDialog()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                browserController.handleBackKey()
                return true
            }
        }
        return false
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (ebWebView.hitTestResult.type == WebView.HitTestResult.EDIT_TEXT_TYPE) return false

        // process dpad navigation
        if (config.useUpDownPageTurn) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    ebWebView.pageDownWithNoAnimation()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    ebWebView.pageUpWithNoAnimation()
                    return true
                }
            }
        }

        if (!config.enableViBinding) return false
        // vim bindings
        if (event.isShiftPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_J -> {
                    browserController.gotoRightTab()
                    return true
                }

                KeyEvent.KEYCODE_K -> {
                    browserController.gotoLeftTab()
                    return true
                }

                KeyEvent.KEYCODE_G -> {
                    ebWebView.jumpToBottom()
                    return true
                }
                else -> return false
            }
        } else { // non-capital
            when (event.keyCode) {
                KeyEvent.KEYCODE_B -> {
                    browserController.openBookmarkPage()
                    return true
                }
                KeyEvent.KEYCODE_O -> {
                    if (previousKeyEvent?.keyCode == KeyEvent.KEYCODE_V) {
                        browserController.decreaseFontSize()
                        previousKeyEvent = null
                    } else {
                        browserController.focusOnInput()
                    }
                    return true
                }

                KeyEvent.KEYCODE_J -> {
                    ebWebView.pageDownWithNoAnimation()
                    return true
                }
                KeyEvent.KEYCODE_K -> {
                    ebWebView.pageUpWithNoAnimation()
                    return true
                }
                KeyEvent.KEYCODE_H -> {
                    ebWebView.goBack()
                    return true
                }
                KeyEvent.KEYCODE_L -> {
                    ebWebView.goForward()
                    return true
                }
                KeyEvent.KEYCODE_R -> {
                    browserController.showTranslation()
                    return true
                }
                KeyEvent.KEYCODE_D -> {
                    browserController.removeAlbum()
                    return true
                }
                KeyEvent.KEYCODE_T -> {
                    browserController.newATab()
                    browserController.focusOnInput()
                    return true
                }

                KeyEvent.KEYCODE_SLASH -> {
                    browserController.showSearchPanel()
                    return true
                }
                KeyEvent.KEYCODE_G -> {
                    previousKeyEvent = when {
                        previousKeyEvent == null -> event
                        previousKeyEvent?.keyCode == KeyEvent.KEYCODE_G -> {
                            // gg
                            browserController.jumpToTop()
                            null
                        }

                        else -> null
                    }
                    return true
                }

                KeyEvent.KEYCODE_V -> {
                    previousKeyEvent = if (previousKeyEvent == null) event else null
                    return true
                }

                KeyEvent.KEYCODE_I -> {
                    if (previousKeyEvent?.keyCode == KeyEvent.KEYCODE_V) {
                        browserController.increaseFontSize()
                        previousKeyEvent = null
                    }
                    return true
                }

                KeyEvent.KEYCODE_F -> {
                    browserController.toggleFullscreen()
                    return true
                }

                else -> return false
            }
        }
    }

    private fun handleVolumeDownKey(): Boolean {
        return if (config.volumePageTurn) {
            if (ebWebView.isVerticalRead) {
                ebWebView.pageUpWithNoAnimation()
            } else {
                ebWebView.pageDownWithNoAnimation()
            }
            true
        } else {
            false
        }
    }

    private fun handleVolumeUpKey(): Boolean {
        return if (config.volumePageTurn) {
            if (ebWebView.isVerticalRead) {
                ebWebView.pageDownWithNoAnimation()
            } else {
                ebWebView.pageUpWithNoAnimation()
            }
            true
        } else {
            false
        }
    }
}
