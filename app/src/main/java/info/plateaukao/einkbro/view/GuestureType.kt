package info.plateaukao.einkbro.view

import info.plateaukao.einkbro.R

enum class GestureType(val value: String, val resId: Int) {
    NothingHappen("01", R.string.nothing),
    Forward("02", R.string.forward_in_history),
    Backward("03", R.string.back_in_history),
    ScrollToTop("04", R.string.scroll_to_top),
    ScrollToBottom("05", R.string.scroll_to_bottom),
    ToLeftTab("06", R.string.switch_to_left_tab),
    ToRightTab("07", R.string.switch_to_right_tab),
    Overview("08", R.string.show_overview),
    OpenNewTab("09", R.string.open_new_tab),
    CloseTab("10", R.string.close_tab),
    PageUp("11", R.string.page_up),
    PageDown("12", R.string.page_down),
    Bookmark("13", R.string.bookmarks),
    Back("14", R.string.back),
    Fullscreen("15", R.string.fullscreen),
    Refresh("16", R.string.refresh),
    Menu("17", R.string.menu),
    TouchPagination("18", R.string.toggle_touch_turn_page),
    KeyPageUp("19", R.string.key_page_up),
    KeyPageDown("20", R.string.key_page_down),
    KeyLeft("21", R.string.key_left),
    KeyRight("22", R.string.key_right),
    ;

    companion object {
        fun from(value: String): GestureType =
            values().firstOrNull { it.value == value } ?: NothingHappen
    }

}