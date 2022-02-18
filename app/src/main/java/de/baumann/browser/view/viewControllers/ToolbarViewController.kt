package de.baumann.browser.view.viewControllers

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.toolbaricons.ToolbarAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolbarViewController(
    private val iconContainer: ViewGroup
): KoinComponent {
    private val config: ConfigManager by inject()

    fun isDisplayed(): Boolean = iconContainer.visibility == VISIBLE

    fun show() = toggleIconsOnOmnibox(true)

    fun hide() = toggleIconsOnOmnibox(false)

    private val readerToolbarActions: List<ToolbarAction> = listOf(
            ToolbarAction.RotateScreen,
            ToolbarAction.FullScreen,
            ToolbarAction.BoldFont,
            ToolbarAction.Font,
            ToolbarAction.Touch,
            ToolbarAction.TabCount,
            ToolbarAction.Settings,
            ToolbarAction.CloseTab,
    )

    fun setEpubReaderMode() {
        reorderIcons(readerToolbarActions)
    }

    fun reorderIcons(list: List<ToolbarAction>? = null) {
        toolbarActionViews.size

        val iconEnums = list ?: config.toolbarActions
        if (iconEnums.isNotEmpty()) {
            iconContainer.removeAllViews()
            iconEnums.forEach { actionEnum ->
                iconContainer.addView(toolbarActionViews[actionEnum.ordinal])
            }
            if (ToolbarAction.Settings !in iconEnums) {
                iconContainer.addView(toolbarActionViews[ToolbarAction.Settings.ordinal])
            }
            iconContainer.requestLayout()
        }

    }

    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        iconContainer.visibility = if (shouldShow) VISIBLE else GONE
    }

    private val toolbarActionViews: List<View> by lazy {
        val childCount = iconContainer.childCount
        val children = mutableListOf<View>()
        for (i in 0 until childCount) {
            children.add(iconContainer.getChildAt(i))
        }

        children
    }
}