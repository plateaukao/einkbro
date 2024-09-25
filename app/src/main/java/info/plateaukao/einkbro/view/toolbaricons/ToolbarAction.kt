package info.plateaukao.einkbro.view.toolbaricons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.ui.graphics.vector.ImageVector
import info.plateaukao.einkbro.R

enum class ToolbarAction(
    val iconResId: Int = 0,
    val imageVector: ImageVector? = null,
    val titleResId: Int,
    val iconActiveInfo: IconActiveInfo = IconActiveInfo(isActivable = false),
    val isAddable: Boolean = true,
) {
    Title(imageVector = Icons.Outlined.Info, titleResId = R.string.toolbar_title), // 0
    Back(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft, titleResId = R.string.back),
    Refresh(
        imageVector = Icons.Outlined.Refresh,
        titleResId = R.string.refresh,
        iconActiveInfo = IconActiveInfo(true, R.drawable.ic_stop, R.drawable.icon_refresh)
    ),
    Touch(
        imageVector = Icons.Outlined.TouchApp,
        titleResId = R.string.touch_turn_page,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_enabled,
            R.drawable.ic_touch_disabled
        )
    ),
    PageUp(imageVector = Icons.Outlined.Upload, titleResId = R.string.page_up),
    PageDown(imageVector = Icons.Outlined.Download, titleResId = R.string.page_down),
    TabCount(imageVector = Icons.Outlined.LooksOne, titleResId = R.string.tab_preview),
    Font(imageVector = Icons.Outlined.FormatSize, titleResId = R.string.font_size),
    Settings(imageVector = Icons.Outlined.Menu, titleResId = R.string.settings),
    Bookmark(imageVector = Icons.Outlined.Bookmarks, titleResId = R.string.bookmarks),
    IconSetting(imageVector = Icons.Outlined.Straighten, titleResId = R.string.toolbars),
    VerticalLayout(imageVector = Icons.Outlined.ViewColumn, titleResId = R.string.vertical_read),
    ReaderMode(imageVector = Icons.AutoMirrored.Outlined.ChromeReaderMode, titleResId = R.string.reader_mode),
    BoldFont(
        iconResId = R.drawable.ic_bold_font,
        titleResId = R.string.bold_font,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_bold_font_active,
            R.drawable.ic_bold_font
        )
    ),
    IncreaseFont(imageVector = Icons.Outlined.TextIncrease, titleResId = R.string.font_size_increase),
    DecreaseFont(imageVector = Icons.Outlined.TextDecrease, titleResId = R.string.font_size_decrease),
    FullScreen(imageVector = Icons.Outlined.Fullscreen, titleResId = R.string.fullscreen),
    Forward(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight, titleResId = R.string.forward),
    RotateScreen(imageVector = Icons.AutoMirrored.Outlined.RotateRight, titleResId = R.string.rotate),
    Translation(imageVector = Icons.Outlined.Translate, titleResId = R.string.translate),
    CloseTab(imageVector = Icons.Outlined.CancelPresentation, titleResId = R.string.close_tab),
    InputUrl(imageVector = Icons.Outlined.ModeEdit, titleResId = R.string.input_url),
    NewTab(imageVector = Icons.Outlined.LibraryAdd, titleResId = R.string.open_new_tab),
    Desktop(
        imageVector = Icons.Outlined.DesktopWindows,
        titleResId = R.string.desktop_mode,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.icon_desktop_activate,
            R.drawable.icon_desktop
        )
    ),
    TOC(imageVector = Icons.AutoMirrored.Outlined.Toc, titleResId = R.string.title_in_toc, isAddable = false),
    Search(imageVector = Icons.Outlined.Search, titleResId = R.string.setting_title_search),
    DuplicateTab(imageVector = Icons.Outlined.FolderCopy, titleResId = R.string.duplicate_tab),
    Tts(
        imageVector = Icons.Outlined.RecordVoiceOver,
        titleResId = R.string.menu_tts,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_tts,
            R.drawable.ic_voice_off
        )
    ),
    PageInfo(iconResId = R.drawable.ic_page_count, titleResId = R.string.page_count),
    GoogleInPlace(
        imageVector = Icons.Outlined.GTranslate,
        titleResId = R.string.google_in_place
    ),
    TranslateByParagraph(
        imageVector = Icons.AutoMirrored.Outlined.Segment,
        titleResId = R.string.inter_translate
    ),
    PapagoByParagraph(
        iconResId = R.drawable.ic_papago,
        titleResId = R.string.papago
    ),
    MoveToBackground(
        imageVector = Icons.Outlined.Minimize,
        titleResId = R.string.move_to_background
    ),
    TouchDirectionUpDown(
        imageVector = Icons.Outlined.SwipeVertical,
        titleResId = R.string.switch_touch_area_action_short,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_direction_up,
            R.drawable.ic_touch_direction_down
        )
    ),
    TouchDirectionLeftRight(
        imageVector = Icons.Outlined.Swipe,
        titleResId = R.string.switch_touch_area_action_short,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_direction_left,
            R.drawable.ic_touch_direction_right
        )
    ),
    Time(
        imageVector = Icons.Outlined.AccessTime,
        titleResId = R.string.toolbar_time,
    ),
    Spacer1(
        imageVector = Icons.Outlined.SpaceBar,
        titleResId = R.string.expand_space,
    ),
    Spacer2(
        imageVector = Icons.Outlined.SpaceBar,
        titleResId = R.string.expand_space,
    ),
    ;


    companion object {
        fun fromOrdinal(value: Int) = values().first { it.ordinal == value }
        val defaultActionsForPhone: List<ToolbarAction> = listOf(
            Bookmark,
            TabCount,
            InputUrl,
            NewTab,
            Back,
            Refresh,
            Touch,
            ReaderMode,
            Settings,
        )
        val defaultActions: List<ToolbarAction> = listOf(
            Title,
            Bookmark,
            TabCount,
            NewTab,
            Back,
            Refresh,
            Touch,
            ReaderMode,
            Settings,
        )
    }

    fun getCurrentResId(state: Boolean): Int =
        if (iconActiveInfo.isActivable) {
            if (state) iconActiveInfo.activeResId else iconActiveInfo.inactiveResId
        } else {
            iconResId
        }
}

data class IconActiveInfo(
    val isActivable: Boolean = false,
    val activeResId: Int = 0,
    val inactiveResId: Int = 0,
)

// a data class to wrap a state in it
class ToolbarActionInfo(
    val toolbarAction: ToolbarAction,
    var state: Boolean = false,
) {
    fun getCurrentResId(): Int = toolbarAction.getCurrentResId(state)
}
