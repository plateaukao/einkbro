package info.plateaukao.einkbro.view.toolbaricons

import info.plateaukao.einkbro.R

enum class ToolbarAction(
    val iconResId: Int = 0,
    val titleResId: Int,
    val iconActiveInfo: IconActiveInfo = IconActiveInfo(isActivable = false),
    val isAddable: Boolean = true,
) {
    Title(iconResId = R.drawable.icon_info, titleResId = R.string.toolbar_title), // 0
    Back(iconResId = R.drawable.icon_arrow_left_gest, titleResId = R.string.back),
    Refresh(
        iconResId = R.drawable.icon_refresh,
        titleResId = R.string.refresh,
        iconActiveInfo = IconActiveInfo(true, R.drawable.ic_stop, R.drawable.icon_refresh)
    ),
    Touch(
        iconResId = R.drawable.ic_touch_enabled,
        titleResId = R.string.touch_turn_page,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_enabled,
            R.drawable.ic_touch_disabled
        )
    ),
    PageUp(iconResId = R.drawable.ic_page_up, titleResId = R.string.page_up),
    PageDown(iconResId = R.drawable.ic_page_down, titleResId = R.string.page_down),
    TabCount(iconResId = R.drawable.icon_preview, titleResId = R.string.tab_preview),
    Font(iconResId = R.drawable.icon_size, titleResId = R.string.font_size),
    Settings(iconResId = R.drawable.ic_menu, titleResId = R.string.settings),
    Bookmark(iconResId = R.drawable.ic_bookmarks, titleResId = R.string.bookmarks),
    IconSetting(iconResId = R.drawable.ic_toolbar, titleResId = R.string.toolbars),
    VerticalLayout(iconResId = R.drawable.ic_vertical_read, titleResId = R.string.vertical_read),
    ReaderMode(iconResId = R.drawable.ic_reader, titleResId = R.string.reader_mode),
    BoldFont(
        iconResId = R.drawable.ic_bold_font,
        titleResId = R.string.bold_font,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_bold_font_active,
            R.drawable.ic_bold_font
        )
    ),
    IncreaseFont(iconResId = R.drawable.ic_font_increase, titleResId = R.string.font_size_increase),
    DecreaseFont(iconResId = R.drawable.ic_font_decrease, titleResId = R.string.font_size_decrease),
    FullScreen(iconResId = R.drawable.icon_fullscreen, titleResId = R.string.fullscreen),
    Forward(iconResId = R.drawable.icon_arrow_right_gest, titleResId = R.string.forward),
    RotateScreen(iconResId = R.drawable.ic_rotate, titleResId = R.string.rotate),
    Translation(iconResId = R.drawable.ic_translate, titleResId = R.string.translate),
    CloseTab(iconResId = R.drawable.icon_close, titleResId = R.string.close_tab),
    InputUrl(iconResId = R.drawable.ic_input_url, titleResId = R.string.input_url),
    NewTab(iconResId = R.drawable.icon_plus, titleResId = R.string.open_new_tab),
    Desktop(
        iconResId = R.drawable.icon_desktop,
        titleResId = R.string.desktop_mode,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.icon_desktop_activate,
            R.drawable.icon_desktop
        )
    ),
    TOC(iconResId = R.drawable.ic_toc, titleResId = R.string.title_in_toc, isAddable = false),
    Search(iconResId = R.drawable.icon_search, titleResId = R.string.setting_title_search),
    DuplicateTab(iconResId = R.drawable.ic_copy, titleResId = R.string.duplicate_tab),
    Tts(
        iconResId = R.drawable.ic_tts,
        titleResId = R.string.menu_tts,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_stop,
            R.drawable.ic_tts
        )
    ),
    PageInfo(iconResId = R.drawable.ic_page_count, titleResId = R.string.page_count),
    GoogleInPlace(
        iconResId = R.drawable.ic_translate_google,
        titleResId = R.string.google_in_place
    ),
    TranslateByParagraph(
        iconResId = R.drawable.ic_translate_paragraph,
        titleResId = R.string.inter_translate
    ),
    PapagoByParagraph(
        iconResId = R.drawable.ic_papago,
        titleResId = R.string.papago
    ),
    MoveToBackground(
        iconResId = R.drawable.ic_minimize,
        titleResId = R.string.move_to_background
    ),
    TouchDirectionUpDown(
        iconResId = R.drawable.ic_touch_direction_up,
        titleResId = R.string.switch_touch_area_action_short,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_direction_up,
            R.drawable.ic_touch_direction_down
        )
    ),
    TouchDirectionLeftRight(
        iconResId = R.drawable.ic_touch_direction_left,
        titleResId = R.string.switch_touch_area_action_short,
        iconActiveInfo = IconActiveInfo(
            true,
            R.drawable.ic_touch_direction_left,
            R.drawable.ic_touch_direction_right
        )
    );


    companion object {
        fun fromOrdinal(value: Int) = values().first { it.ordinal == value }
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
    var state: Boolean = false
) {
    fun getCurrentResId(): Int = toolbarAction.getCurrentResId(state)
}
