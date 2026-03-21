package info.plateaukao.einkbro.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.doOnLayout
import info.plateaukao.einkbro.R
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

    private fun resolveColorControlNormal(): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlNormal, tv, true)
        return tv.data
    }

    private val separator: View = View(context).apply {
        layoutParams = LayoutParams(1, LayoutParams.MATCH_PARENT)
        setBackgroundColor(resolveColorControlNormal())
        this@TwoPaneLayout.addView(this)
    }
    private val floatingLine: View = View(context).apply {
        layoutParams = LayoutParams(2.dp(context), LayoutParams.MATCH_PARENT)
        visibility = GONE
        setBackgroundColor(resolveColorControlNormal())
        this@TwoPaneLayout.addView(this)
    }
    private val dragHandle: View = View(context).apply {
        layoutParams = LayoutParams(16.dp(context), 44.dp(context))
        visibility = GONE
        alpha = 0.5f
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dp(context).toFloat()
            setColor(resolveColorControlNormal())
        }
        this@TwoPaneLayout.addView(this)
    }

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

            dragHandle.x = (resizedPosition - dragHandle.measuredWidth / 2).toFloat()
            dragHandle.y = (measuredHeight / 2 - dragHandle.measuredHeight / 2).toFloat()
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

            dragHandle.x = (measuredWidth / 2 - dragHandle.measuredWidth / 2).toFloat()
            dragHandle.y = (resizedPosition - dragHandle.measuredHeight / 2).toFloat()
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

            val params = LayoutParams(2.dp(context), LayoutParams.MATCH_PARENT)
            separator.layoutParams = params
            separator.y = 0F
            val floatingLineParams = LayoutParams(3.dp(context), LayoutParams.MATCH_PARENT)
            floatingLine.layoutParams = floatingLineParams
            floatingLine.y = 0F
            val dragParams = LayoutParams(16.dp(context), 44.dp(context))
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
                        dragHandle.alpha = 0.5F
                        adjustPaneSize(finalX.toInt())
                    }
                }
                true
            }
        } else {
            dX = 0F
            finalX = (measuredWidth / 2).toFloat()

            val params = LayoutParams(LayoutParams.MATCH_PARENT, 2.dp(context))
            separator.layoutParams = params
            separator.x = 0F
            val floatingLineParams = LayoutParams(LayoutParams.MATCH_PARENT, 3.dp(context))
            floatingLine.layoutParams = floatingLineParams
            floatingLine.x = 0F
            val dragParams = LayoutParams(44.dp(context), 16.dp(context))
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
                        dragHandle.alpha = 0.5F
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