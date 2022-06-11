package de.baumann.browser.view.viewControllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.ui.platform.ComposeView
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.compose.ComposedToolbar
import de.baumann.browser.view.toolbaricons.ToolbarAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ComposeToolbarViewController(
    private val composeView: ComposeView,
    private val onItemClick: (ToolbarAction)->Unit,
    private val onItemLongClick: (ToolbarAction)->Unit,
): KoinComponent {
    private val config: ConfigManager by inject()

    fun isDisplayed(): Boolean = composeView.visibility == VISIBLE

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

        val iconEnums = list ?: config.toolbarActions
        if (iconEnums.isNotEmpty()) {
            composeView.setContent {
                ComposedToolbar(iconEnums, "Hi, title",
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }

    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        composeView.visibility = if (shouldShow) VISIBLE else GONE
    }
}