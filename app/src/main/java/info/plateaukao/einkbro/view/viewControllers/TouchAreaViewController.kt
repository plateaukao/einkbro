package info.plateaukao.einkbro.view.viewControllers

import android.annotation.SuppressLint
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TouchAreaType
import info.plateaukao.einkbro.unit.ViewUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class TouchAreaViewController(
    private val rootView: View,
    private val pageUpAction: () -> Unit,
    private val pageTopAction: () -> Unit,
    private val pageDownAction: () -> Unit,
    private val pageBottomAction: () -> Unit,
    private val keyLeftAction: () -> Unit,
    private val keyRightAction: () -> Unit,
) : KoinComponent {
    private lateinit var touchAreaPageUp: View
    private lateinit var touchAreaPageDown: View
    private lateinit var touchAreaDragCustomize: View

    private val config: ConfigManager by inject()

    private val touchAreaChangeListener: OnSharedPreferenceChangeListener by lazy {
        OnSharedPreferenceChangeListener { _, key ->
            if (key == ConfigManager.K_TOUCH_HINT) {
                if (config.touchAreaHint) {
                    showTouchAreaHint()
                } else {
                    hideTouchAreaHint()
                }
                // for configuring custom drag area
                updateTouchAreaType()
            }

            if (key == ConfigManager.K_TOUCH_AREA_TYPE) {
                updateTouchAreaType()
                // reset offset when type is changed
                config.touchAreaCustomizeY = 0
            }
        }
    }

    init {
        updateTouchAreaType()
        if (config.touchAreaCustomizeY != 0) {
            rootView.post {
                updateTouchAreaCustomizeY(config.touchAreaCustomizeY)
            }
        }
        // after optimization, don't know why registration is gone.
        // do it after controller is created.
        rootView.post {
            config.registerOnSharedPreferenceChangeListener(touchAreaChangeListener)
        }
    }

    private fun customOnTouch(view: View, event: MotionEvent): Boolean {
        var dY = 0F
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dY = view.y - event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                // need to consider whether top part height is occupied by toolbar
                val currentViewY =
                    event.rawY - dY - view.height - (if (config.isToolbarOnTop) ViewUnit.dpToPixel(
                        rootView.context,
                        50
                    ) else 0).toInt()
                val customizeY = currentViewY + view.height / 2
                updateTouchAreaCustomizeY(customizeY.toInt())
            }

            MotionEvent.ACTION_UP -> {}
        }
        return true
    }

    private fun updateTouchAreaCustomizeY(customizeY: Int) {
        config.touchAreaCustomizeY = customizeY
        val upDownDiffY = touchAreaPageDown.y - touchAreaPageUp.y
        touchAreaDragCustomize.y = (customizeY - touchAreaDragCustomize.height / 2).toFloat()
        touchAreaPageUp.y = customizeY.toFloat()
        touchAreaPageDown.y = touchAreaPageUp.y + upDownDiffY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTouchAreaType() {
        // hide current one, and reset listener
        if (this::touchAreaPageUp.isInitialized) {
            with(touchAreaPageUp) {
                visibility = View.GONE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
            with(touchAreaPageDown) {
                visibility = View.GONE
                setOnLongClickListener(null)
                setOnClickListener(null)
            }
            with(touchAreaDragCustomize) {
                visibility = View.GONE
                setOnTouchListener(null)
            }
        }

        when (config.touchAreaType) {
            TouchAreaType.BottomLeftRight -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_bottom_left)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_bottom_right)
                touchAreaDragCustomize = rootView.findViewById(R.id.touch_area_bottom_drag)
            }

            TouchAreaType.MiddleLeftRight -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_middle_left)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_middle_right)
                touchAreaDragCustomize = rootView.findViewById(R.id.touch_area_middle_drag)
            }

            TouchAreaType.Left -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_left_1)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_left_2)
                touchAreaDragCustomize = rootView.findViewById(R.id.touch_area_left_drag)
            }

            TouchAreaType.Right -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_right_1)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_right_2)
                touchAreaDragCustomize = rootView.findViewById(R.id.touch_area_right_drag)
            }

            TouchAreaType.Long -> {
                touchAreaPageUp = rootView.findViewById(R.id.touch_area_long_left)
                touchAreaPageDown = rootView.findViewById(R.id.touch_area_long_right)
                // need to hide drag area
                touchAreaDragCustomize = rootView.findViewById(R.id.touch_area_middle_drag)
                touchAreaDragCustomize.visibility = View.GONE
            }

            TouchAreaType.LongLeftRight -> {}
        }

        with(touchAreaPageUp) {
            setOnClickListener { if (!config.switchTouchAreaAction) pageUpAction() else pageDownAction() }
            setOnLongClickListener {
                if (config.longClickAsArrowKey) {
                    keyLeftAction()
                    return@setOnLongClickListener true
                }
                if (!config.switchTouchAreaAction) pageTopAction() else pageBottomAction(); true
            }
        }
        with(touchAreaPageDown) {
            setOnClickListener { if (!config.switchTouchAreaAction) pageDownAction() else pageUpAction() }
            setOnLongClickListener {
                if (config.longClickAsArrowKey) {
                    keyRightAction()
                    return@setOnLongClickListener true
                }
                if (!config.switchTouchAreaAction) pageBottomAction() else pageUpAction(); true
            }
        }
        with(touchAreaDragCustomize) {
            setOnTouchListener { view, event -> customOnTouch(view, event) }
        }

        if (config.enableTouchTurn) {
            touchAreaPageUp.visibility = View.VISIBLE
            touchAreaPageDown.visibility = View.VISIBLE
            touchAreaDragCustomize.visibility =
                if (config.touchAreaHint) View.VISIBLE else View.GONE
            showTouchAreaHint()
        }
    }

    fun hideTouchAreaHint() {
        rootView.post {
            touchAreaPageUp.setBackgroundColor(Color.TRANSPARENT)
            touchAreaPageDown.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showTouchAreaHint() {
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
        touchAreaPageUp.visibility = if (enabled) View.VISIBLE else View.GONE
        touchAreaPageDown.visibility = if (enabled) View.VISIBLE else View.GONE
        touchAreaDragCustomize.visibility = if (enabled) View.VISIBLE else View.GONE

        if (enabled) showTouchAreaHint()
    }

    private var disabledTemporarily = false
    fun maybeDisableTemporarily() {
        if (config.enableTouchTurn && config.touchAreaHint && config.hideTouchAreaWhenInput) {
            if (this::touchAreaPageUp.isInitialized) {
                disabledTemporarily = true
                with(touchAreaPageUp) {
                    visibility = View.GONE
                    setOnLongClickListener(null)
                    setOnClickListener(null)
                }
                with(touchAreaPageDown) {
                    visibility = View.GONE
                    setOnLongClickListener(null)
                    setOnClickListener(null)
                }
                with(touchAreaDragCustomize) {
                    visibility = View.GONE
                    setOnTouchListener(null)
                }
            }
        }
    }

    fun maybeEnableAgain() {
        if (
            disabledTemporarily &&
            config.enableTouchTurn &&
            config.touchAreaHint &&
            config.hideTouchAreaWhenInput
        ) {
            disabledTemporarily = false
            updateTouchAreaType()
        }
    }
}