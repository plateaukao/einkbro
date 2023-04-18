package info.plateaukao.einkbro.view.dialog.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.Gravity
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager

abstract class DraggableComposeDialogFragment: ComposeDialogFragment() {
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var initialX: Int = 0
    private var initialY: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    protected fun setupDialogPosition(position: Point) {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.LEFT)

        if (position.isValid()) {
            val params = window.attributes.apply {
                x = position.x
                y = position.y
            }
            window.attributes = params
        }

        supportDragToMove(window)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun supportDragToMove(window: Window) {
        val windowManager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        window.decorView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Get the initial touch position and dialog window position
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = window.attributes.x
                    initialY = window.attributes.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Calculate the new position of the dialog window
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()

                    // Update the position of the dialog window
                    window.attributes.x = newX
                    window.attributes.y = newY
                    windowManager.updateViewLayout(window.decorView, window.attributes)
                    true
                }

                else -> false
            }
        }
    }

    private fun Point.isValid() = x != 0 && y != 0
}