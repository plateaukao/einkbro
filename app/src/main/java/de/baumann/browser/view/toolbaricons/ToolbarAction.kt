package de.baumann.browser.view.toolbaricons

import de.baumann.browser.Ninja.R

enum class ToolbarAction(val iconResId: Int = 0, val title: String) {
    Title(iconResId = 0, title = "title"), // 0
    Back(iconResId = R.drawable.icon_arrow_left_gest, title = "back"),
    Refresh(iconResId = R.drawable.icon_refresh, title = "refresh"),
    Touch(iconResId = R.drawable.ic_touch_enabled, title = "touch turn page"),
    PageUp(iconResId = R.drawable.ic_page_up, title = "page up"),
    PageDown(iconResId = R.drawable.ic_page_down, title = "page down"),
    TabCount(iconResId = R.drawable.icon_preview, title = "tab count"),
    Font(iconResId = R.drawable.icon_size, title = "font size"),
    Settings(iconResId = R.drawable.icon_settings, title = "settings"),
    Bookmark(iconResId = R.drawable.ic_bookmarks, title = "bookmarks"),
    IconSetting(iconResId = R.drawable.ic_toolbar, title = "toolbar"),
    VerticalLayout(iconResId = R.drawable.ic_vertical_read, title = "vertical read"),
    ReaderMode(iconResId = R.drawable.ic_reader, title = "reader mode"),
    BoldFont(iconResId = R.drawable.ic_bold_font, title = "bold font"),
    IncreaseFont(iconResId = R.mipmap.font_size_increase, title = "font size increase"),
    DecreaseFont(iconResId = R.mipmap.font_size_decrease, title = "font size decrease"),
    FullScreen(iconResId = R.drawable.icon_fullscreen, title = "fullscreen"),
    Forward(iconResId = R.drawable.icon_arrow_right_gest, title = "forward"),
    RotateScreen(iconResId = R.drawable.ic_rotate, title = "rotate");

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