package de.baumann.browser.view.viewControllers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TouchAreaType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class TouchAreaViewController(
        private val context: Context,
        private val rootView: View,
        private val pageUpAction: () -> Unit,
        private val pageTopAction: () -> Unit,
        private val pageDownAction: () -> Unit,
        private val pageBottomAction: () -> Unit,
) : KoinComponent {
    private lateinit var touchAreaPageUp: View
    private lateinit var touchAreaPageDown: View
    private val config: ConfigManager by inject()

    private val touchAreaChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ConfigManager.K_TOUCH_HINT) {
            if (config.touchAreaHint) {
                showTouchAreaHint()
            } else {
                hideTouchAreaHint()
            }
        }

        if (key == ConfigManager.K_TOUCH_AREA_TYPE) {
            updateTouchAreaType()
        }
    }

    init {
        config.registerOnSharedPreferenceChangeListener(touchAreaChangeListener)
        updateTouchAreaType()
    }

    fun updateTouchAreaType() {
        // hide current one, and reset listener
        if (this::touchAreaPageUp.isInitialized) {
            with(touchAreaPageUp) {
                visibility = View.INVISIBLE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
            with(touchAreaPageDown) {
                visibility = View.INVISIBLE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
        }

        when (config.touchAreaType) {
            TouchAreaType.BottomLeftRight -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_bottom_left)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_bottom_right)
            }
            TouchAreaType.MiddleLeftRight -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_middle_left)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_middle_right)
            }
            TouchAreaType.Left -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_left_1)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_left_2)
            }
            TouchAreaType.Right -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_right_1)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_right_2)
            }
            else -> {
            }
        }

        val isTouchEnabled = config.enableTouchTurn
        with(touchAreaPageUp) {
            if (isTouchEnabled) visibility = View.VISIBLE
            setOnClickListener { pageUpAction.invoke() }
            setOnLongClickListener { pageTopAction.invoke(); true }
        }
        with(touchAreaPageDown) {
            if (isTouchEnabled) visibility = View.VISIBLE
            setOnClickListener { pageDownAction.invoke() }
            setOnLongClickListener { pageBottomAction.invoke(); true }
        }
    }

    fun hideTouchAreaHint() {
        touchAreaPageUp.setBackgroundColor(Color.TRANSPARENT)
        touchAreaPageDown.setBackgroundColor(Color.TRANSPARENT)
    }

    fun showTouchAreaHint() {
        touchAreaPageUp.setBackgroundResource(R.drawable.touch_area_border)
        touchAreaPageDown.setBackgroundResource(R.drawable.touch_area_border)
        if (!config.touchAreaHint) {
            Timer("showTouchAreaHint", false)
                    .schedule(object : TimerTask() {
                        override fun run() {
                            hideTouchAreaHint()
                        }
                    }, 500)
        }
    }

    fun toggleTouchPageTurn(enabled: Boolean) {
        touchAreaPageUp.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        touchAreaPageDown.visibility = if (enabled) View.VISIBLE else View.INVISIBLE

        if (enabled) showTouchAreaHint()
    }
}