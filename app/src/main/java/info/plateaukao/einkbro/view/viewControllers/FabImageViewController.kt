package info.plateaukao.einkbro.view.viewControllers

import android.annotation.SuppressLint
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.RelativeLayout
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.ViewUnit.dp
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("ClickableViewAccessibility")
class FabImageViewController(
    private val imageView: ImageView,
    private val clickAction: () -> Unit,
    private val longClickAction: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()

    var defaultTouchListener: OnTouchListener? = null

    init {
        imageView.alpha = 0.6f
        val params = RelativeLayout.LayoutParams(
            imageView.layoutParams.width,
            imageView.layoutParams.height
        )

        when (config.fabPosition) {
            FabPosition.Custom -> {
                imageView.layoutParams = params.apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
                if (config.fabCustomPosition.x != 0 && config.fabCustomPosition.y != 0) {
                    imageView.post {
                        imageView.x = config.fabCustomPosition.x.toFloat()
                        imageView.y = config.fabCustomPosition.y.toFloat()
                    }
                }
            }

            FabPosition.Left -> {
                imageView.layoutParams = params.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }

            FabPosition.Right -> {
                imageView.layoutParams = params.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }

            FabPosition.Center -> {
                imageView.layoutParams = params.apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_content)
                }
            }

            FabPosition.NotShow -> {}
        }

        ViewUnit.expandViewTouchArea(imageView, 20.dp(imageView.context))
        setClickActions()
        if (config.fabPosition != FabPosition.Custom) {
            imageView.setOnTouchListener(defaultTouchListener)
        }
        updateImage()
    }

    fun show() {
        imageView.visibility = View.VISIBLE
    }

    fun hide() {
        imageView.visibility = View.INVISIBLE
    }

    fun updateImage() {
        val fabResourceId =
            if (config.enableTouchTurn) R.drawable.ic_touch_disabled else R.drawable.icon_overflow_fab
        imageView.setImageResource(fabResourceId)
    }

    private fun setClickActions() {
        imageView.setOnClickListener { clickAction() }
        imageView.setOnLongClickListener {
            if (config.fabPosition == FabPosition.Custom) {
                imageView.scaleX = 2.0f
                imageView.scaleY = 2.0f
                imageView.setOnTouchListener { view, event -> customOnTouch(view, event) }
            } else {
                longClickAction()
            }
            false
        }
    }

    private var isDragging = false
    private fun customOnTouch(view: View, event: MotionEvent): Boolean {
        var dY = 0F
        var dX = 0F
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    isDragging = true
                    dY = view.y - event.rawY
                    dX = view.x - event.rawX
                    return true
                }

                // need to consider whether top part height is occupied by toolbar
                val currentViewY = event.rawY + dY - imageView.height * 2 / 3
                val currentViewX = event.rawX + dX - imageView.width * 2 / 3
                updateFabImageCustomizePosition(currentViewX.toInt(), currentViewY.toInt())
            }

            MotionEvent.ACTION_UP -> {
                imageView.scaleX = 1.0f
                imageView.scaleY = 1.0f
                imageView.setOnTouchListener(defaultTouchListener)
                setClickActions()
                isDragging = false
            }
        }
        return true
    }

    private fun updateFabImageCustomizePosition(x: Int, y: Int) {
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        config.fabCustomPosition = Point(x, y)
    }
}