package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.browser.BrowserAction

class TouchConfig(private val sp: SharedPreferences) {

    var enableTouchTurn by BooleanPreference(sp, K_ENABLE_TOUCH, false)
    var isMultitouchEnabled by BooleanPreference(sp, K_MULTITOUCH, false)
    var useUpDownPageTurn by BooleanPreference(sp, K_UPDOWN_PAGE_TURN, false)
    var disableLongPressTouchArea by BooleanPreference(
        sp,
        "sp_disable_long_press_touch_area",
        false
    )
    var touchAreaHint by BooleanPreference(sp, K_TOUCH_HINT, true)
    var volumePageTurn by BooleanPreference(sp, K_VOLUME_PAGE_TURN, true)
    var switchTouchAreaAction by BooleanPreference(sp, K_TOUCH_AREA_ACTION_SWITCH, false)
    var longClickAsArrowKey by BooleanPreference(sp, K_TOUCH_AREA_ARROW_KEY, false)
    var hideTouchAreaWhenInput by BooleanPreference(sp, K_TOUCH_AREA_HIDE_WHEN_INPUT, false)
    var enableNavButtonGesture by BooleanPreference(sp, K_NAV_BUTTON_GESTURE, false)

    var touchAreaCustomizeY by IntPreference(sp, K_TOUCH_AREA_OFFSET, 0)

    var touchAreaType: TouchAreaType
        get() = TouchAreaType.entries[sp.getInt(K_TOUCH_AREA_TYPE, 0)]
        set(value) {
            sp.edit(true) { putInt(K_TOUCH_AREA_TYPE, value.ordinal) }
        }

    val isEbookModeActive: Boolean
        get() = touchAreaType == TouchAreaType.Ebook && enableTouchTurn

    var enableDragUrlToAction by BooleanPreference(sp, "K_ENABLE_DRAG_URL_TO_ACTION", true)

    var pageReservedOffset: Int by IntPreference(sp, K_PRESERVE_HEIGHT, 80)

    var pageReservedOffsetInString: String by StringPreference(
        sp,
        K_PRESERVE_HEIGHT_IN_STRING,
        pageReservedOffset.toString()
    )

    var multitouchUp by BrowserActionPreference(sp, K_MULTITOUCH_UP)
    var multitouchDown by BrowserActionPreference(sp, K_MULTITOUCH_DOWN)
    var multitouchLeft by BrowserActionPreference(sp, K_MULTITOUCH_LEFT)
    var multitouchRight by BrowserActionPreference(sp, K_MULTITOUCH_RIGHT)
    var navGestureUp by BrowserActionPreference(sp, K_GESTURE_NAV_UP)
    var navGestureDown by BrowserActionPreference(sp, K_GESTURE_NAV_DOWN)
    var navGestureLeft by BrowserActionPreference(sp, K_GESTURE_NAV_LEFT)
    var navGestureRight by BrowserActionPreference(sp, K_GESTURE_NAV_RIGHT)
    var navButtonLongClickGesture by BrowserActionPreference(
        sp,
        K_GESTURE_NAV_LONG_CLICK,
        defaultValue = BrowserAction.ShowOverview
    )

    var upClickGesture by BrowserActionPreference(
        sp, "K_UP_CLICK_GESTURE", BrowserAction.PageUp
    )
    var downClickGesture by BrowserActionPreference(
        sp, "K_DOWN_CLICK_GESTURE", BrowserAction.PageDown
    )
    var upLongClickGesture by BrowserActionPreference(
        sp, "K_UP_LONG_CLICK_GESTURE", BrowserAction.JumpToTop
    )
    var downLongClickGesture by BrowserActionPreference(
        sp, "K_DOWN_LONG_CLICK_GESTURE", BrowserAction.JumpToBottom
    )

    companion object {
        const val K_TOUCH_AREA_TYPE = "sp_touch_area_type"
        const val K_ENABLE_TOUCH = "sp_enable_touch"
        const val K_TOUCH_HINT = "sp_touch_area_hint"
        const val K_VOLUME_PAGE_TURN = "volume_page_turn"
        const val K_UPDOWN_PAGE_TURN = "sp_useUpDownForPageTurn"
        const val K_MULTITOUCH = "sp_multitouch"
        const val K_TOUCH_AREA_OFFSET = "sp_touch_area_offset"
        const val K_TOUCH_AREA_ACTION_SWITCH = "sp_touch_area_action_switch"
        const val K_TOUCH_AREA_ARROW_KEY = "sp_touch_area_arrow_key"
        const val K_TOUCH_AREA_HIDE_WHEN_INPUT = "sp_touch_area_hide_when_input"
        const val K_NAV_BUTTON_GESTURE = "sp_gestures_use"

        private val K_PRESERVE_HEIGHT = "sp_page_turn_left_value"
        private val K_PRESERVE_HEIGHT_IN_STRING = "sp_page_turn_left_value_in_string"

        const val K_MULTITOUCH_UP = "sp_multitouch_up"
        const val K_MULTITOUCH_DOWN = "sp_multitouch_down"
        const val K_MULTITOUCH_LEFT = "sp_multitouch_left"
        const val K_MULTITOUCH_RIGHT = "sp_multitouch_right"

        const val K_GESTURE_NAV_UP = "setting_gesture_nav_up"
        const val K_GESTURE_NAV_DOWN = "setting_gesture_nav_down"
        const val K_GESTURE_NAV_LEFT = "setting_gesture_nav_left"
        const val K_GESTURE_NAV_RIGHT = "setting_gesture_nav_right"
        const val K_GESTURE_NAV_LONG_CLICK = "setting_gesture_nav_long_click"
    }
}
