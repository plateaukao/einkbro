package info.plateaukao.einkbro.view.viewControllers

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("ClickableViewAccessibility")
class FabImageViewController(
    private var orientation: Int,
    private val textView: TextView,
    private val clickAction: () -> Unit,
    private val longClickAction: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()

    var defaultTouchListener: OnTouchListener? = null

    init {
        initialize()
    }

    fun initialize() {
        textView.alpha = 0.5f

        // Reset absolute positioning when not in custom mode
        if (config.fabPosition != FabPosition.Custom) {
            textView.x = 0f
            textView.y = 0f
        }

        val params = ConstraintLayout.LayoutParams(
            textView.layoutParams.width,
            textView.layoutParams.height
        )

        when (config.fabPosition) {
            FabPosition.Custom -> {
                textView.layoutParams = params.apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = R.id.main_content
                }
            }

            FabPosition.Left -> {
                textView.layoutParams = params.apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = R.id.main_content
                }
            }

            FabPosition.Right -> {
                textView.layoutParams = params.apply {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = R.id.main_content
                }
            }

            FabPosition.Center -> {
                textView.layoutParams = params.apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = R.id.main_content
                }
            }

            FabPosition.NotShow -> {}
        }

        ViewUnit.expandViewTouchArea(textView, 20.dp(textView.context))
        setClickActions()
        textView.setOnTouchListener(defaultTouchListener)
        updateImagePosition(orientation)
    }

    fun show() {
        if (config.fabPosition == FabPosition.Custom) {
            positionValidation()
            updateImagePosition(orientation)
        }

        textView.visibility = View.VISIBLE
        // don't know why sometimes the fab gesture is not working.
        // set it again when showing it
        textView.setOnTouchListener(defaultTouchListener)
    }

    fun hide() {
        textView.visibility = View.INVISIBLE
    }

    fun updateImagePosition(orientation: Int) {
        this.orientation = orientation
        if (config.fabPosition != FabPosition.Custom) {
            return
        }

        textView.postDelayed({
            val currentFabCustomPosition =
                if (orientation == ORIENTATION_PORTRAIT) config.fabCustomPosition else config.fabCustomPositionLandscape

            if (currentFabCustomPosition.x != 0 && currentFabCustomPosition.y != 0) {
                textView.post {
                    textView.x = currentFabCustomPosition.x.toFloat()
                    textView.y = currentFabCustomPosition.y.toFloat()
                }
            }

            val maxWidth = (textView.parent as View).width
            val maxHeight = (textView.parent as View).height
            if (textView.x < 0) textView.x = 0f
            if (textView.y < 0) textView.y = 0f

            if (textView.x + textView.width > maxWidth) {
                textView.x = maxWidth - textView.width.toFloat()
            }
            if (textView.y + textView.height > maxHeight) {
                textView.y = maxHeight - textView.height.toFloat()
            }
        }, 1000L)
    }

    fun updateTabCount(countString: String) {
        textView.text = countString
    }

    private fun setClickActions() {
        textView.setOnClickListener { clickAction() }
        textView.setOnLongClickListener {
            if (config.fabPosition == FabPosition.Custom) {
                textView.scaleX = 2.0f
                textView.scaleY = 2.0f
                textView.setOnTouchListener { view, event -> customOnTouch(view, event) }
                false
            } else {
                longClickAction()
                true
            }
        }
    }

    private fun customOnTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                // need to consider whether top part height is occupied by toolbar
                val currentViewY = event.rawY - textView.height * 2 / 3
                val currentViewX = event.rawX - textView.width * 2 / 3
                updateFabImageCustomizePosition(currentViewX.toInt(), currentViewY.toInt())
            }

            MotionEvent.ACTION_UP -> {
                textView.scaleX = 1.0f
                textView.scaleY = 1.0f
                textView.setOnTouchListener(defaultTouchListener)
                setClickActions()
            }
        }
        return true
    }

    private fun updateFabImageCustomizePosition(x: Int, y: Int) {
        textView.x = x.toFloat()
        textView.y = y.toFloat()
        if (orientation == ORIENTATION_PORTRAIT) {
            config.fabCustomPosition = Point(x, y)
        } else {
            config.fabCustomPositionLandscape = Point(x, y)
        }

        positionValidation()
    }

    private fun positionValidation() {
        val maxWidth = (textView.parent as View).width
        val maxHeight = (textView.parent as View).height

            if (orientation == ORIENTATION_PORTRAIT &&
                (config.fabCustomPosition.x > maxWidth || config.fabCustomPosition.y > maxHeight)){
                config.fabCustomPosition = Point(0, 0)
            } else if (orientation != ORIENTATION_LANDSCAPE &&
                (config.fabCustomPositionLandscape.x > maxWidth || config.fabCustomPositionLandscape.y > maxHeight)){
                config.fabCustomPositionLandscape = Point(0, 0)
            }
    }
}