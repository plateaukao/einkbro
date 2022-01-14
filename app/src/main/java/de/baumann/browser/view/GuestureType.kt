package de.baumann.browser.view

enum class GestureType(val value: String) {
    NothingHappen("01"),
    Forward("02"),
    Backward("03"),
    ScrollToTop("04"),
    ScrollToBottom("05"),
    ToLeftTab("06"),
    ToRightTab("07"),
    Overview("08"),
    OpenNewTab("09"),
    CloseTab("10"),
    PageUp("11"),
    PageDown("12");

    companion object {
        fun from(value: String): GestureType = values().firstOrNull { it.value == value } ?: NothingHappen
    }

}