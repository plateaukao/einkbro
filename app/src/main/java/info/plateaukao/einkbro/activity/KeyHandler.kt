package info.plateaukao.einkbro.activity

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.webkit.WebView
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.KeyHandlerCallback
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KeyHandler(
    private val keyCallback: KeyHandlerCallback,
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
                if (config.touch.useUpDownPageTurn) ebWebView.pageDownWithNoAnimation()
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (config.touch.useUpDownPageTurn) ebWebView.pageUpWithNoAnimation()
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> return handleVolumeDownKey(event)
            KeyEvent.KEYCODE_VOLUME_UP -> return handleVolumeUpKey(event)
            KeyEvent.KEYCODE_MENU -> {
                keyCallback.showMenuDialog()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                keyCallback.handleBackKey()
                return true
            }
        }
        return false
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (ebWebView.hitTestResult.type == WebView.HitTestResult.EDIT_TEXT_TYPE) return false

        // process dpad navigation
        if (config.touch.useUpDownPageTurn) {
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

        if (!config.browser.enableViBinding) return false
        // vim bindings
        if (event.isShiftPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_J -> {
                    keyCallback.gotoRightTab()
                    return true
                }

                KeyEvent.KEYCODE_K -> {
                    keyCallback.gotoLeftTab()
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
                    keyCallback.openBookmarkPage()
                    return true
                }
                KeyEvent.KEYCODE_O -> {
                    if (previousKeyEvent?.keyCode == KeyEvent.KEYCODE_V) {
                        keyCallback.decreaseFontSize()
                        previousKeyEvent = null
                    } else {
                        keyCallback.focusOnInput()
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
                    keyCallback.showTranslation()
                    return true
                }
                KeyEvent.KEYCODE_D -> {
                    keyCallback.removeAlbum()
                    return true
                }
                KeyEvent.KEYCODE_T -> {
                    keyCallback.newATab()
                    keyCallback.focusOnInput()
                    return true
                }

                KeyEvent.KEYCODE_SLASH -> {
                    keyCallback.showSearchPanel()
                    return true
                }
                KeyEvent.KEYCODE_G -> {
                    previousKeyEvent = when {
                        previousKeyEvent == null -> event
                        previousKeyEvent?.keyCode == KeyEvent.KEYCODE_G -> {
                            // gg
                            keyCallback.jumpToTop()
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
                        keyCallback.increaseFontSize()
                        previousKeyEvent = null
                    }
                    return true
                }

                KeyEvent.KEYCODE_F -> {
                    keyCallback.toggleFullscreen()
                    return true
                }

                else -> return false
            }
        }
    }

    private var isVolumeLongPress = false
    private var isVolumeTemporarilyDisabled = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reenableJob: Job? = null

    private fun scheduleReenableVolumePageTurn() {
        reenableJob?.cancel()
        reenableJob = scope.launch {
            delay(5000)
            isVolumeTemporarilyDisabled = false
            val context = keyCallback as? Context ?: return@launch
            EBToast.show(context, R.string.volume_page_turn_resumed)
        }
    }

    fun dispose() {
        scope.cancel()
    }

    fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (config.touch.volumePageTurn) {
                isVolumeLongPress = true
                isVolumeTemporarilyDisabled = true
                scheduleReenableVolumePageTurn()
                val context = keyCallback as? Context ?: return true
                EBToast.show(context, R.string.volume_page_turn_paused)
                adjustVolume(keyCode)
                return true
            }
        }
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (config.touch.volumePageTurn) {
                isVolumeLongPress = false
                return !isVolumeTemporarilyDisabled
            }
        }
        return false
    }

    private fun extendVolumeDisablePeriod() {
        scheduleReenableVolumePageTurn()
    }

    private fun adjustVolume(keyCode: Int) {
        val context = keyCallback as? Context ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun handleVolumeDownKey(event: KeyEvent): Boolean {
        if (config.touch.volumePageTurn) {
            if (isVolumeTemporarilyDisabled) {
                if (event.repeatCount == 0) extendVolumeDisablePeriod()
                return false
            }
            if (event.repeatCount == 0) {
                isVolumeLongPress = false
                event.startTracking()
                if (ebWebView.isVerticalRead) {
                    ebWebView.pageUpWithNoAnimation()
                } else {
                    ebWebView.pageDownWithNoAnimation()
                }
                return true
            } else {
                if (isVolumeLongPress) {
                    adjustVolume(KeyEvent.KEYCODE_VOLUME_DOWN)
                    return true
                }
                return true
            }
        }
        return false
    }

    private fun handleVolumeUpKey(event: KeyEvent): Boolean {
        if (config.touch.volumePageTurn) {
            if (isVolumeTemporarilyDisabled) {
                if (event.repeatCount == 0) extendVolumeDisablePeriod()
                return false
            }
            if (event.repeatCount == 0) {
                isVolumeLongPress = false
                event.startTracking()
                if (ebWebView.isVerticalRead) {
                    ebWebView.pageDownWithNoAnimation()
                } else {
                    ebWebView.pageUpWithNoAnimation()
                }
                return true
            } else {
                if (isVolumeLongPress) {
                    adjustVolume(KeyEvent.KEYCODE_VOLUME_UP)
                    return true
                }
                return true
            }
        }
        return false
    }
}
