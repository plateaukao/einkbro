package de.baumann.browser.view.toolbaricons

import de.baumann.browser.Ninja.R

enum class ToolbarAction(val iconResId: Int = 0, val titleResId: Int) {
    Title(iconResId = 0, titleResId = R.string.toolbar_title), // 0
    Back(iconResId = R.drawable.icon_arrow_left_gest, titleResId = R.string.back),
    Refresh(iconResId = R.drawable.icon_refresh, titleResId = R.string.refresh),
    Touch(iconResId = R.drawable.ic_touch_enabled, titleResId = R.string.touch_turn_page),
    PageUp(iconResId = R.drawable.ic_page_up, titleResId = R.string.page_up),
    PageDown(iconResId = R.drawable.ic_page_down, titleResId = R.string.page_down),
    TabCount(iconResId = R.drawable.icon_preview, titleResId = R.string.tab_preview),
    Font(iconResId = R.drawable.icon_size, titleResId = R.string.font_size),
    Settings(iconResId = R.drawable.icon_settings, titleResId = R.string.settings),
    Bookmark(iconResId = R.drawable.ic_bookmarks, titleResId = R.string.bookmarks),
    IconSetting(iconResId = R.drawable.ic_toolbar, titleResId = R.string.toolbars),
    VerticalLayout(iconResId = R.drawable.ic_vertical_read, titleResId = R.string.vertical_read),
    ReaderMode(iconResId = R.drawable.ic_reader, titleResId = R.string.reader_mode),
    BoldFont(iconResId = R.drawable.ic_bold_font, titleResId = R.string.bold_font),
    IncreaseFont(iconResId = R.drawable.ic_font_increase, titleResId = R.string.font_size_increase),
    DecreaseFont(iconResId = R.drawable.ic_font_decrease, titleResId = R.string.font_size_decrease),
    FullScreen(iconResId = R.drawable.icon_fullscreen, titleResId = R.string.fullscreen),
    Forward(iconResId = R.drawable.icon_arrow_right_gest, titleResId = R.string.forward),
    RotateScreen(iconResId = R.drawable.ic_rotate, titleResId = R.string.rotate),
    Translation(iconResId = R.drawable.ic_translate, titleResId = R.string.translate),
    CloseTab(iconResId = R.drawable.icon_close, titleResId = R.string.close_tab),
    InputUrl(iconResId = R.drawable.ic_input_url, titleResId = R.string.input_url),
    NewTab(iconResId = R.drawable.icon_plus, titleResId = R.string.open_new_tab);

    companion object {
        fun fromOrdinal(value: Int) = values().first { it.ordinal == value }
        val defaultActions: List<ToolbarAction> = listOf(
                Title,
                TabCount,
                Back,
                Refresh,
                Touch,
                Font,
                ReaderMode,
                Settings,
        )
    }
}
