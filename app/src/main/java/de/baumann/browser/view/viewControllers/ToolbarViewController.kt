package de.baumann.browser.view.viewControllers

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.iterator
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.toolbaricons.ToolbarAction

class ToolbarViewController(
    private val context: Context,
    private val toolbarScroller: HorizontalScrollView,
) {
    private val iconBar: ViewGroup = toolbarScroller.findViewById(R.id.icon_bar)
    private val config: ConfigManager by lazy { ConfigManager(context) }

    fun isDisplayed(): Boolean = toolbarScroller.visibility == VISIBLE

    fun show() = toggleIconsOnOmnibox(true)

    fun hide() = toggleIconsOnOmnibox(false)

    fun reorderIcons() {
        toolbarActionViews.size

        val iconEnums = config.toolbarActions
        if (iconEnums.isNotEmpty()) {
            iconBar.removeAllViews()
            iconEnums.forEach { actionEnum ->
                iconBar.addView(toolbarActionViews[actionEnum.ordinal])
            }
            if (ToolbarAction.Settings !in iconEnums) {
                iconBar.addView(toolbarActionViews[ToolbarAction.Settings.ordinal])
            }
            iconBar.requestLayout()
            toolbarScroller.post {
                modifyTitleControlWidth()
                toolbarScroller.fullScroll(View.FOCUS_RIGHT)
            }
        }

    }

    private fun modifyTitleControlWidth() {
        if (ToolbarAction.Title in config.toolbarActions) {
            val textView = iconBar.findViewById<TextView>(R.id.omnibox_title)
            if (isIconsWidthLargerThanScreenWidth()) {
                val params = LinearLayout.LayoutParams( getRestToolbarWidth(), LinearLayout.LayoutParams.MATCH_PARENT)
                textView.layoutParams = params
                textView.minimumWidth = 0
            } else {
                val params = LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                textView.layoutParams = params
                textView.minimumWidth = ViewUnit.dpToPixel(context, 100).toInt()
            }
        }
    }

    private fun isIconsWidthLargerThanScreenWidth(): Boolean = getRestToolbarWidth() >= ViewUnit.dpToPixel(context, 40).toInt()

    private fun getRestToolbarWidth(): Int =
        ViewUnit.getWindowWidth(context) -
                iconBar.iterator().asSequence()
                    .filter { it.id != R.id.omnibox_title }
                    .map { it.measuredWidth }
                    .sum()


    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        toolbarScroller.visibility = if (shouldShow) VISIBLE else GONE
    }

    private val toolbarActionViews: List<View> by lazy {
        val childCount = iconBar.childCount
        val children = mutableListOf<View>()
        for (i in 0 until childCount) {
            children.add(iconBar.getChildAt(i))
        }

        children
    }
}