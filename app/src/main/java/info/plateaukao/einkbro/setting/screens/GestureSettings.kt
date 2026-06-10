package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.GestureActionSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface

fun buildGestureSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        DividerSettingItem(
            R.string.setting_title_touch_area_actions,
        ),
        GestureActionSettingItem(
            R.string.setting_touch_up_click,
            config = config.touch::upClickGesture,
        ),
        GestureActionSettingItem(
            R.string.setting_touch_up_long_click,
            config = config.touch::upLongClickGesture,
        ),
        GestureActionSettingItem(
            R.string.setting_touch_down_click,
            config = config.touch::downClickGesture,
        ),
        GestureActionSettingItem(
            R.string.setting_touch_down_long_click,
            config = config.touch::downLongClickGesture,
        ),
        BooleanSettingItem(
            R.string.show_touch_area_hint,
            config = config.touch::touchAreaHint,
            span = 2,
        ),
        BooleanSettingItem(
            R.string.hie_touch_area_when_input,
            config = config.touch::hideTouchAreaWhenInput,
            span = 2,
        ),
        BooleanSettingItem(
            R.string.switch_touch_area_action,
            config = config.touch::switchTouchAreaAction,
            span = 2,
        ),
        BooleanSettingItem(
            R.string.enable_touch_area_as_arrow_key,
            config = config.touch::longClickAsArrowKey,
            span = 2,
        ),
        DividerSettingItem(R.string.setting_multitouch_use_title),
        BooleanSettingItem(
            R.string.setting_multitouch_use_title,
            0,
            R.string.setting_multitouch_use_summary,
            config.touch::isMultitouchEnabled,
            span = 2,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_up,
            config = config.touch::multitouchUp,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_down,
            config = config.touch::multitouchDown,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_left,
            config = config.touch::multitouchLeft,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_right,
            config = config.touch::multitouchRight,
        ),
        DividerSettingItem(R.string.gesture_on_floating_button),
        BooleanSettingItem(
            R.string.setting_gestures_use_title,
            0,
            R.string.setting_gestures_use_summary,
            config.touch::enableNavButtonGesture,
            span = 2,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_up,
            config = config.touch::navGestureUp,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_down,
            config = config.touch::navGestureDown,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_left,
            config = config.touch::navGestureLeft,
        ),
        GestureActionSettingItem(
            R.string.setting_gesture_right,
            config = config.touch::navGestureRight,
        ),
        GestureActionSettingItem(
            R.string.setting_floating_button_long_click,
            config = config.touch::navButtonLongClickGesture,
        ),
    )
}
