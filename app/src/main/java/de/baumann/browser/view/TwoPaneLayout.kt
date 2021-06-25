package de.baumann.browser.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.doOnLayout
import de.baumann.browser.Ninja.R


class TwoPaneLayout : FrameLayout {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    init {
        inflate(context, R.layout.two_pane_layout, this)
        separator = findViewById(R.id.separator)
        floatingLine = findViewById(R.id.floating_line)
        dragHandle = findViewById(R.id.drag_handle)
        initDragHandle()
        this.doOnLayout {
            initViews()
        }
    }

    private var panel1: View? = null
    private var panel2: View? = null
    private var subPanel: View? = null

    private val separator: View
    private val floatingLine: View
    private val dragHandle: View

    var shouldShowSecondPane = false
        set(value) {
            field = value
            updatePanels()
        }

    fun switchPanels() {
        // replace view position
        val tempPanel = panel1
        panel1 = panel2
        panel2 = tempPanel

        updatePanels()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val oldMeasuredHeight = measuredWidth
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (measuredWidth != oldMeasuredHeight) {
            updatePanels()
        }
    }

    private var orientation: Orientation = Orientation.Horizontal
    var dragResize = false

    private fun initAttributes(attrs: AttributeSet?) {
        attrs ?: return

        val attributeValues = context.obtainStyledAttributes(attrs, R.styleable.TwoPaneLayout)
        with(attributeValues) {
            try {
                shouldShowSecondPane = getBoolean(R.styleable.TwoPaneLayout_show_second_pane, false)
                orientation = Orientation.values()[getInt(R.styleable.TwoPaneLayout_orientation, Orientation.Horizontal.ordinal)]
                dragResize = getBoolean(R.styleable.TwoPaneLayout_drag_resize, false)
            } catch (ex: Exception) {
                // TwoPaneLayout configuration error
                Log.d("TwoPaneLayout", ex.toString())
            } finally {
                recycle()
            }
        }
    }

    private fun updatePanels() {
        if (shouldShowSecondPane) {
            if (finalX > measuredWidth) {
                finalX = (measuredWidth / 2).toFloat()
            }
            showSubPanel(finalX.toInt())
        } else {
            hideSubPanel()
        }
    }

    private fun initViews() {
        val userAddedViews = children.iterator().asSequence().filter {
            !listOf(separator, floatingLine, dragHandle).contains(it)
        }.toList()

        if (userAddedViews.size != 2) {
            // print errors
        }

        panel1 = userAddedViews[0]
        panel2 = userAddedViews[1]
        subPanel = panel2

        updatePanels()

        finalX = (measuredWidth / 2).toFloat()
    }

    private fun showSubPanel(resizedX: Int = measuredWidth / 2) {
        // panel 1
        panel1?.visibility = VISIBLE
        val params = LayoutParams(resizedX, LayoutParams.MATCH_PARENT)
        panel1?.layoutParams = params
        panel1?.x = 0F

        // panel 2
        panel2?.visibility = VISIBLE
        val params2 = LayoutParams(measuredWidth - resizedX, LayoutParams.MATCH_PARENT)
        panel2?.layoutParams = params2
        panel2?.x = resizedX.toFloat()

        // accessory views
        separator.x = resizedX.toFloat()
        separator.visibility = VISIBLE
        separator.bringToFront()

        val halfDragHandleWidth = dragHandle.measuredWidth / 2
        dragHandle.x = (resizedX - halfDragHandleWidth).toFloat()
        dragHandle.y = (measuredHeight / 2 - dragHandle.measuredHeight / 2).toFloat()
        dragHandle.setPadding(halfDragHandleWidth, 0, halfDragHandleWidth, 0)
        dragHandle.visibility = VISIBLE
        dragHandle.bringToFront()
    }

    private fun hideSubPanel() {
        subPanel?.visibility = GONE

        val mainPanel = if (subPanel == panel1) panel2  else panel1

        val params = LayoutParams(measuredWidth, LayoutParams.MATCH_PARENT)
        mainPanel?.layoutParams = params
        mainPanel?.x = 0F

        dragHandle.visibility = GONE
        separator.visibility = GONE
    }

    private var  dX: Float = 0f
    private var finalX: Float = 0f
    @SuppressLint("ClickableViewAccessibility")
    private fun initDragHandle() {
        dragHandle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    floatingLine.x = view.x + view.width / 2
                    floatingLine.visibility = VISIBLE
                    floatingLine.bringToFront()
                    dragHandle.alpha = 1F
                    dX = view.x - event.rawX
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .setDuration(0)
                        .start()
                    finalX = event.rawX + dX + view.width / 2
                    floatingLine.animate()
                        .x(event.rawX + dX + view.width / 2)
                        .setDuration(0)
                        .start()
                    if (dragResize) adjustPaneSize(finalX.toInt())
                }
                MotionEvent.ACTION_UP -> {
                    floatingLine.visibility = GONE
                    dragHandle.alpha = 0.3F
                    adjustPaneSize(finalX.toInt())
                }
            }
            true
        }
    }

    private fun adjustPaneSize(width: Int) {
        showSubPanel(width)
    }
}

private enum class Orientation { Vertical, Horizontal }