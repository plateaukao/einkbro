package info.plateaukao.einkbro.view.viewControllers

import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.Album
import info.plateaukao.einkbro.view.compose.ComposedToolbar
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.BoldFont
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.CloseTab
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Desktop
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Font
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.FullScreen
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.PageInfo
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Refresh
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.RotateScreen
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Settings
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.TOC
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Touch
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.TouchDirectionLeftRight
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.TouchDirectionUpDown
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.AudioOnly
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction.Tts
import info.plateaukao.einkbro.view.toolbaricons.ToolbarActionInfo
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ComposeToolbarViewController(
    private val composeView: ComposeView,
    private val albums: MutableState<List<Album>>,
    private val ttsViewModel: TtsViewModel,
    private val onIconClick: (ToolbarAction) -> Unit,
    private val onIconLongClick: (ToolbarAction) -> Unit,
    onTabClick: (Album) -> Unit,
    onTabLongClick: (Album) -> Unit,
    private val isAudioOnlyMode: () -> Boolean = { false },
) : KoinComponent {
    private val config: ConfigManager by inject()

    private val readerToolbarActions: List<ToolbarAction> = listOf(
        RotateScreen,
        FullScreen,
        BoldFont,
        Font,
        Touch,
        TOC,
        PageInfo,
        Settings,
        CloseTab,
    )

    private var isLoading: Boolean = false

    private var isReader: Boolean = false

    // State previously held by ToolbarComposeView
    var toolbarActionInfoList: List<ToolbarActionInfo> by mutableStateOf(emptyList())
    var shouldShowTabs by mutableStateOf(false)
    var title by mutableStateOf("")
    var tabCount by mutableStateOf("")
    var pageInfo by mutableStateOf("")
    var isIncognito by mutableStateOf(false)
    var albumFocusIndex = mutableStateOf(0)

    private val onTabClick: (Album) -> Unit = onTabClick
    private val onTabLongClick: (Album) -> Unit = onTabLongClick

    init {
        val iconEnums = if (isReader) readerToolbarActions else config.toolbarActions
        toolbarActionInfoList = iconEnums.toToolbarActionInfoList()
        shouldShowTabs = config.shouldShowTabBar

        composeView.setContent {
            MyTheme {
                ComposedToolbar(
                    showTabs = shouldShowTabs,
                    toolbarActionInfoList,
                    title = title,
                    tabCount = tabCount,
                    pageInfo = pageInfo,
                    isIncognito = isIncognito,
                    onIconClick = onIconClick,
                    onIconLongClick = onIconLongClick,
                    albumList = albums,
                    albumFocusIndex = albumFocusIndex,
                    onAlbumClick = this.onTabClick,
                    onAlbumLongClick = this.onTabLongClick,
                )
            }
        }
    }

    private fun containsPageInfo(): Boolean = toolbarActionInfoList
        .map { it.toolbarAction }.contains(PageInfo)

    fun showTabbar(shouldShow: Boolean) {
        shouldShowTabs = shouldShow
    }

    fun isDisplayed(): Boolean = composeView.visibility == VISIBLE

    fun show() = toggleIconsOnOmnibox(true)

    fun hide() = toggleIconsOnOmnibox(false)

    fun updateTabCount(text: String) {
        tabCount = text
    }

    fun updatePageInfo(text: String) {
        if (containsPageInfo()) pageInfo = text
    }

    fun updateRefresh(isLoadingWeb: Boolean) {
        if (isLoadingWeb == isLoading) return

        isLoading = isLoadingWeb
        updateIcons()
    }

    fun setEpubReaderMode() {
        isReader = true
        updateIcons()
    }

    fun updateIcons(actionToUpdate: ToolbarAction? = null) {
        val iconEnums = if (isReader) readerToolbarActions else config.toolbarActions
        // only update specific icon is specified.
        if (actionToUpdate != null && !iconEnums.contains(actionToUpdate)) {
            return
        }

        toolbarActionInfoList = iconEnums.toToolbarActionInfoList()
        isIncognito = config.isIncognitoMode
    }

    private fun List<ToolbarAction>.toToolbarActionInfoList(): List<ToolbarActionInfo> {
        return this.map { toolbarAction ->
            when (toolbarAction) {
                BoldFont -> ToolbarActionInfo(toolbarAction, config.boldFontStyle)
                Refresh -> ToolbarActionInfo(toolbarAction, isLoading)
                Desktop -> ToolbarActionInfo(toolbarAction, config.desktop)
                Touch -> ToolbarActionInfo(toolbarAction, config.enableTouchTurn)
                TouchDirectionUpDown -> ToolbarActionInfo(
                    toolbarAction,
                    config.switchTouchAreaAction
                )

                TouchDirectionLeftRight -> ToolbarActionInfo(
                    toolbarAction,
                    config.switchTouchAreaAction
                )

                Tts -> ToolbarActionInfo(toolbarAction, ttsViewModel.isReading())
                AudioOnly -> ToolbarActionInfo(toolbarAction, isAudioOnlyMode())
                else -> ToolbarActionInfo(toolbarAction, false)
            }
        }
    }

    private fun toggleIconsOnOmnibox(shouldShow: Boolean) {
        composeView.visibility = if (shouldShow) VISIBLE else INVISIBLE
    }

    fun updateTitle(title: String) {
        this.title = title
    }

    fun updateFocusIndex(index: Int) {
        albumFocusIndex.value = index
    }
}
