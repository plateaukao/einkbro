package info.plateaukao.einkbro.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.doOnLayout
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.TwoPaneLayoutBinding
import info.plateaukao.einkbro.unit.ViewUnit.dp


class TwoPaneLayout : FrameLayout {
    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)

        initDragHandle()
        doOnLayout { initViews() }
    }

    private val binding: TwoPaneLayoutBinding = TwoPaneLayoutBinding.inflate(LayoutInflater.from(context), this)
    private val separator: View = binding.separator
    private val floatingLine: View = binding.floatingLine
    private val dragHandle: View = binding.middleDragHandle

    private var panel1: View? = null
    private var panel2: View? = null
    private var subPanel: View? = null

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

    fun setOrientation(orientation: Orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation
            initDragHandle()
            binding.root.requestLayout()
            this.requestLayout()
            this.doOnLayout { initViews() }
        }
    }

    fun getOrientation(): Orientation = orientation

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val oldMeasuredWidth = measuredWidth
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (isHorizontal() && measuredWidth != oldMeasuredWidth) {
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
            updateFinalPosition()
            val resizedPosition = if (isHorizontal()) finalX.toInt() else finalY.toInt()
            showSubPanel(resizedPosition)
        } else {
            hideSubPanel()
        }
    }

    private fun isHorizontal(): Boolean = orientation == Orientation.Horizontal

    private fun updateFinalPosition() {
        if (isHorizontal()) {
            if (finalX > measuredWidth) {
                finalX = (measuredWidth / 2).toFloat()
            }
        } else {
            if (finalY > measuredHeight) {
                finalY = (measuredHeight/ 2).toFloat()
            }
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

        if (isHorizontal()) {
            finalX = (measuredWidth / 2).toFloat()
            finalY = (measuredHeight / 2).toFloat()
        } else {
            finalX = (measuredWidth / 2).toFloat()
            finalY = (measuredHeight / 2).toFloat()
        }

        updatePanels()
    }

    private fun showSubPanel(resizedPosition: Int) {
        if (isHorizontal()) {
            // panel 1
            panel1?.visibility = VISIBLE
            val params = LayoutParams(resizedPosition, LayoutParams.MATCH_PARENT)
            panel1?.layoutParams = params
            panel1?.x = 0F
            panel1?.y = 0F

            // panel 2
            panel2?.visibility = VISIBLE
            val params2 = LayoutParams(measuredWidth - resizedPosition, LayoutParams.MATCH_PARENT)
            panel2?.layoutParams = params2
            panel2?.x = resizedPosition.toFloat()
            panel2?.y = 0F

            // accessory views
            separator.x = resizedPosition.toFloat()
            separator.visibility = VISIBLE
            separator.bringToFront()

            val halfDragHandleWidth = dragHandle.measuredWidth / 2
            dragHandle.x = (resizedPosition - halfDragHandleWidth).toFloat()
            dragHandle.y = (measuredHeight / 2 - dragHandle.measuredHeight / 2).toFloat()
            dragHandle.setPadding(halfDragHandleWidth, 0, halfDragHandleWidth, 0)
            dragHandle.visibility = VISIBLE
            dragHandle.bringToFront()
        } else {
            // panel 1
            panel1?.visibility = VISIBLE
            val params = LayoutParams(LayoutParams.MATCH_PARENT, resizedPosition)
            panel1?.layoutParams = params
            panel1?.x = 0F
            panel1?.y = 0F

            // panel 2
            panel2?.visibility = VISIBLE
            val params2 = LayoutParams(LayoutParams.MATCH_PARENT, measuredHeight - resizedPosition)
            panel2?.layoutParams = params2
            panel2?.x = 0F
            panel2?.y = resizedPosition.toFloat()

            // accessory views
            separator.y = resizedPosition.toFloat()
            separator.visibility = VISIBLE
            separator.bringToFront()

            val halfDragHandleHeight = dragHandle.measuredHeight / 2
            dragHandle.x = (measuredWidth/ 2 - dragHandle.measuredWidth/ 2).toFloat()
            dragHandle.y = (resizedPosition - halfDragHandleHeight).toFloat()
            dragHandle.setPadding(0, halfDragHandleHeight, 0, halfDragHandleHeight)
            dragHandle.visibility = VISIBLE
            dragHandle.bringToFront()
        }
    }

    private fun hideSubPanel() {
        subPanel?.visibility = GONE

        val mainPanel = if (subPanel == panel1) panel2  else panel1

        if (isHorizontal()) {
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            mainPanel?.layoutParams = params
            mainPanel?.x = 0F
        } else {
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            mainPanel?.layoutParams = params
            mainPanel?.y = 0F
        }

        dragHandle.visibility = GONE
        separator.visibility = GONE
    }

    private var  dX: Float = 0f
    private var finalX: Float = 0f
    private var  dY: Float = 0f
    private var finalY: Float = 0f
    @SuppressLint("ClickableViewAccessibility")
    private fun initDragHandle() {
        if (isHorizontal()) {
            dY = 0F
            finalY = (measuredHeight / 2).toFloat()

            val params = LayoutParams(1, LayoutParams.MATCH_PARENT)
            separator.layoutParams = params
            separator.y = 0F
            val floatingLineParams = LayoutParams(2.dp(context), LayoutParams.MATCH_PARENT)
            floatingLine.layoutParams = floatingLineParams
            floatingLine.y = 0F
            val dragParams = LayoutParams(12.dp(context), 50.dp(context))
            dragHandle.layoutParams = dragParams

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
        } else {
            dX = 0F
            finalX = (measuredWidth / 2).toFloat()

            val params = LayoutParams(LayoutParams.MATCH_PARENT, 1)
            separator.layoutParams = params
            separator.x = 0F
            val floatingLineParams = LayoutParams(LayoutParams.MATCH_PARENT, 2.dp(context))
            floatingLine.layoutParams = floatingLineParams
            floatingLine.x = 0F
            val dragParams = LayoutParams(50.dp(context), 12.dp(context))
            dragHandle.layoutParams = dragParams

            dragHandle.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        floatingLine.y = view.y + view.height / 2
                        floatingLine.visibility = VISIBLE
                        floatingLine.bringToFront()
                        dragHandle.alpha = 1F
                        dY = view.y - event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        view.animate()
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                        finalY = event.rawY + dY + view.height / 2
                        floatingLine.animate()
                            .y(event.rawY + dY + view.height / 2)
                            .setDuration(0)
                            .start()
                        if (dragResize) adjustPaneSize(finalY.toInt())
                    }
                    MotionEvent.ACTION_UP -> {
                        floatingLine.visibility = GONE
                        dragHandle.alpha = 0.3F
                        adjustPaneSize(finalY.toInt())
                    }
                }
                true
            }
        }

    }

    private fun adjustPaneSize(position: Int) {
        showSubPanel(position)
    }
}

enum class Orientation { Vertical, Horizontal }