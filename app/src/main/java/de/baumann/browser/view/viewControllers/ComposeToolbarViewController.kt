package de.baumann.browser.view.viewControllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.ComposeView
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.compose.ComposedToolbar
import de.baumann.browser.view.toolbaricons.ToolbarAction
import de.baumann.browser.view.toolbaricons.ToolbarAction.*
import de.baumann.browser.view.toolbaricons.ToolbarActionInfo
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

    private var isReader: Boolean = false

    fun isDisplayed(): Boolean = composeView.visibility == VISIBLE

    fun show() = toggleIconsOnOmnibox(true)

    fun hide() = toggleIconsOnOmnibox(false)

    fun updateTabCount(text: String) {
        if (tabCount == text) return

        tabCount = text
        updateIcons()
    }

    fun updateRefresh(isLoadingWeb: Boolean) {
        if (isLoadingWeb == isLoading) return

        isLoading = isLoadingWeb
        updateIcons()
    }

    private val readerToolbarActions: List<ToolbarAction> = listOf(
            RotateScreen,
            FullScreen,
            BoldFont,
            Font,
            Touch,
            TabCount,
            Settings,
            CloseTab,
    )

    fun setEpubReaderMode() {
        isReader = true
        updateIcons()
    }

    fun updateIcons() {
        val iconEnums = if(isReader) readerToolbarActions else config.toolbarActions
        if (iconEnums.isEmpty()) return

        composeView.setContent {
            AppCompatTheme {
                ComposedToolbar(iconEnums.toToolbarActionInfoList(),
                    title = title,
                    tabCount = tabCount,
                    isIncognito = config.isIncognitoMode,
                    isReader = isReader,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
            }
        }
    }

    private fun List<ToolbarAction>.toToolbarActionInfoList(): List<ToolbarActionInfo>  {
        return this.map { toolbarAction ->
            when(toolbarAction) {
                BoldFont -> ToolbarActionInfo(toolbarAction, config.boldFontStyle)
                Refresh -> ToolbarActionInfo(toolbarAction,isLoading)
                Desktop -> ToolbarActionInfo(toolbarAction, config.desktop)
                Touch -> ToolbarActionInfo(toolbarAction, config.enableTouchTurn)
                else -> ToolbarActionInfo(toolbarAction, false)
            }
        }
    }

    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        composeView.visibility = if (shouldShow) VISIBLE else GONE
    }

    fun updateTitle(title: String) {
        if (this.title == title) return

        this.title = title
        updateIcons()
    }
}