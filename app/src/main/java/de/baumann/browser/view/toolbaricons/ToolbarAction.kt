package de.baumann.browser.view.toolbaricons

enum class ToolbarAction {
    Title, // 0
    Back, // 1
    Refresh, // 2
    Touch, // 3
    PageUp, // 4
    PageDown, // 5
    TabCount, // 6
    Font, // 7
    Settings, // 8
    Bookmark, // 9
    IconSetting, // 10
    VerticalLayout, // 11
    ReaderMode, // 12
    BoldFont; // 13

    companion object {
        fun fromOrdinal(value: Int) = values().first { it.ordinal == value }
    }
}