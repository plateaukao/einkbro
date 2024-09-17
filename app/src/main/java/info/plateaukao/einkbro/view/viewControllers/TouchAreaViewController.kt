package info.plateaukao.einkbro.view.viewControllers

import android.annotation.SuppressLint
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.databinding.ActivityMainContentBinding
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.TouchAreaType
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.GestureType
import info.plateaukao.einkbro.view.handlers.GestureHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Timer
import java.util.TimerTask

class TouchAreaViewController(
    private val binding: ActivityMainContentBinding,
    private val browserController: BrowserController,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val gestureHandler: GestureHandler = GestureHandler(browserController)

    private lateinit var touchAreaPageUp: View
    private lateinit var touchAreaPageDown: View
    private lateinit var touchAreaDragCustomize: View

    private val pageUpAction = { gestureHandler.handle(config.upClickGesture) }
    private val pageTopAction = { gestureHandler.handle(config.upLongClickGesture) }
    private val pageDownAction = { gestureHandler.handle(config.downClickGesture) }
    private val pageBottomAction = { gestureHandler.handle(config.downLongClickGesture) }
    private val keyLeftAction = { gestureHandler.handle(GestureType.KeyLeft) }
    private val keyRightAction = { gestureHandler.handle(GestureType.KeyRight) }

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
        if (config.touchAreaCustomizeY != 0 && allowMoveTouchArea()) {
            binding.root.post {
                updateTouchAreaCustomizeY(config.touchAreaCustomizeY)
            }
        }
        // after optimization, don't know why registration is gone.
        // do it after controller is created.
        binding.root.post {
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
                touchAreaPageUp = binding.touchAreaBottomLeft
                touchAreaPageDown = binding.touchAreaBottomRight
                touchAreaDragCustomize = binding.touchAreaBottomDrag
            }

            TouchAreaType.MiddleLeftRight -> {
                touchAreaPageUp = binding.touchAreaMiddleLeft
                touchAreaPageDown = binding.touchAreaMiddleRight
                touchAreaDragCustomize = binding.touchAreaMiddleDrag
            }

            TouchAreaType.Left -> {
                touchAreaPageUp = binding.touchAreaLeft1
                touchAreaPageDown = binding.touchAreaLeft2
                touchAreaDragCustomize = binding.touchAreaLeftDrag
            }

            TouchAreaType.Right -> {
                touchAreaPageUp = binding.touchAreaRight1
                touchAreaPageDown = binding.touchAreaRight2
                touchAreaDragCustomize = binding.touchAreaRightDrag
            }

            TouchAreaType.Long -> {
                touchAreaPageUp = binding.touchAreaLongLeft
                touchAreaPageDown = binding.touchAreaLongRight
                // need to hide drag area
                touchAreaDragCustomize = binding.touchAreaMiddleDrag
                touchAreaDragCustomize.visibility = View.GONE
            }

            TouchAreaType.LongLeftRight -> {}
        }

        with(touchAreaPageUp) {
            setOnClickListener { if (!config.switchTouchAreaAction) pageUpAction() else pageDownAction() }
            setOnLongClickListener {
                if (config.disableLongPressTouchArea) return@setOnLongClickListener false

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
                if (config.disableLongPressTouchArea) return@setOnLongClickListener false

                if (config.longClickAsArrowKey) {
                    keyRightAction()
                    return@setOnLongClickListener true
                }
                if (!config.switchTouchAreaAction) pageBottomAction() else pageTopAction(); true
            }
        }
        with(touchAreaDragCustomize) {
            setOnTouchListener { view, event -> customOnTouch(view, event) }
        }

        if (config.enableTouchTurn) {
            touchAreaPageUp.visibility = View.VISIBLE
            touchAreaPageDown.visibility = View.VISIBLE
            touchAreaDragCustomize.visibility =
                if (config.touchAreaHint && allowMoveTouchArea()) View.VISIBLE else View.GONE
            showTouchAreaHint()
        }
    }

    fun hideTouchAreaHint() {
        binding.root.post {
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
                }, 1000)
        }
    }

    fun toggleTouchPageTurn(enabled: Boolean) {
        touchAreaPageUp.visibility = if (enabled) View.VISIBLE else View.GONE
        touchAreaPageDown.visibility = if (enabled) View.VISIBLE else View.GONE
        touchAreaDragCustomize.visibility =
            if (enabled && allowMoveTouchArea()) View.VISIBLE else View.GONE

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

    private fun allowMoveTouchArea(): Boolean = config.touchAreaType != TouchAreaType.Long
}