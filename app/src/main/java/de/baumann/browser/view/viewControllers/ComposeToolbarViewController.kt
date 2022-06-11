package de.baumann.browser.view.viewControllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.ui.platform.ComposeView
import com.google.accompanist.appcompattheme.AppCompatTheme
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

    private var title: String = ""

    private var tabCount: String = ""

    private var isLoading: Boolean = false

    fun isDisplayed(): Boolean = composeView.visibility == VISIBLE

    fun show() = toggleIconsOnOmnibox(true)

    fun hide() = toggleIconsOnOmnibox(false)

    fun updateTabCount(text: String) {
        tabCount = text
    }

    fun updateRefresh(isLoadingWeb: Boolean) {
        isLoading = isLoadingWeb
    }

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
        updateIcons(readerToolbarActions)
    }

    fun updateIcons(list: List<ToolbarAction>? = null) {
        val iconEnums = list ?: config.toolbarActions
        if (iconEnums.isNullOrEmpty()) return

        composeView.setContent {
            AppCompatTheme {
                ComposedToolbar(iconEnums,
                    title = title,
                    tabCount = tabCount,
                    enableTouch =  config.enableTouchTurn,
                    isIncognito = config.isIncognitoMode,
                    isDesktopMode = config.desktop,
                    isBoldFont = config.boldFontStyle,
                    isLoading = isLoading,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
            }
        }
    }

    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        composeView.visibility = if (shouldShow) VISIBLE else GONE
    }

    fun updateTitle(title: String) {
        this.title = title
        updateIcons()
    }
}