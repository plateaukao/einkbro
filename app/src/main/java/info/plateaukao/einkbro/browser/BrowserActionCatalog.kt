package info.plateaukao.einkbro.browser

import androidx.annotation.StringRes
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.GestureType

data class BrowserActionEntry(
    val action: BrowserAction,
    @StringRes val labelResId: Int,
) {
    val id: String get() = action::class.simpleName.orEmpty()
}

data class BrowserActionCategory(
    @StringRes val titleResId: Int,
    val entries: List<BrowserActionEntry>,
)

object BrowserActionCatalog {
    val categories: List<BrowserActionCategory> = listOf(
        BrowserActionCategory(
            R.string.action_category_navigation,
            listOf(
                BrowserActionEntry(BrowserAction.GoForward, R.string.forward_in_history),
                BrowserActionEntry(BrowserAction.HandleBackKey, R.string.back),
                BrowserActionEntry(BrowserAction.RefreshAction, R.string.refresh),
                BrowserActionEntry(BrowserAction.JumpToTop, R.string.scroll_to_top),
                BrowserActionEntry(BrowserAction.JumpToBottom, R.string.scroll_to_bottom),
                BrowserActionEntry(BrowserAction.PageUp, R.string.page_up),
                BrowserActionEntry(BrowserAction.PageDown, R.string.page_down),
                BrowserActionEntry(BrowserAction.SendPageUpKey, R.string.key_page_up),
                BrowserActionEntry(BrowserAction.SendPageDownKey, R.string.key_page_down),
                BrowserActionEntry(BrowserAction.SendLeftKey, R.string.key_left),
                BrowserActionEntry(BrowserAction.SendRightKey, R.string.key_right),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_tab,
            listOf(
                BrowserActionEntry(BrowserAction.NewATab, R.string.open_new_tab),
                BrowserActionEntry(BrowserAction.DuplicateTab, R.string.duplicate_tab),
                BrowserActionEntry(BrowserAction.RemoveAlbum, R.string.close_tab),
                BrowserActionEntry(BrowserAction.GotoLeftTab, R.string.switch_to_left_tab),
                BrowserActionEntry(BrowserAction.GotoRightTab, R.string.switch_to_right_tab),
                BrowserActionEntry(BrowserAction.ShowOverview, R.string.show_overview),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_content,
            listOf(
                BrowserActionEntry(BrowserAction.ToggleReaderMode, R.string.reader_mode),
                BrowserActionEntry(BrowserAction.ToggleVerticalRead, R.string.vertical_read),
                BrowserActionEntry(BrowserAction.IncreaseFontSize, R.string.font_size_increase),
                BrowserActionEntry(BrowserAction.DecreaseFontSize, R.string.font_size_decrease),
                BrowserActionEntry(BrowserAction.ShowFontSizeChangeDialog, R.string.font_size),
                BrowserActionEntry(BrowserAction.ShowFontBoldnessDialog, R.string.bold_font),
                BrowserActionEntry(BrowserAction.InvertColors, R.string.menu_invert_color),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_view,
            listOf(
                BrowserActionEntry(BrowserAction.ToggleFullscreen, R.string.fullscreen),
                BrowserActionEntry(BrowserAction.RotateScreen, R.string.rotate),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_translation,
            listOf(
                BrowserActionEntry(BrowserAction.ShowTranslation, R.string.translate),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_tts,
            listOf(
                BrowserActionEntry(BrowserAction.HandleTtsButton, R.string.menu_tts),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_bookmarks,
            listOf(
                BrowserActionEntry(BrowserAction.OpenBookmarkPage, R.string.bookmarks),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_search,
            listOf(
                BrowserActionEntry(BrowserAction.ShowSearchPanel, R.string.setting_title_search),
                BrowserActionEntry(BrowserAction.ToggleTextSearch, R.string.action_text_search),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_share,
            listOf(
                BrowserActionEntry(BrowserAction.ShareLink, R.string.menu_share_link),
                BrowserActionEntry(BrowserAction.AddToInstapaper, R.string.menu_instapaper),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_touch,
            listOf(
                BrowserActionEntry(BrowserAction.ToggleTouchTurnPage, R.string.toggle_touch_turn_page),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_ai,
            listOf(
                BrowserActionEntry(BrowserAction.SummarizeContent, R.string.action_summarize_content),
                BrowserActionEntry(BrowserAction.ChatWithWeb(), R.string.chat_with_web),
                BrowserActionEntry(BrowserAction.ShowPageAiActionMenu, R.string.page_ai),
                BrowserActionEntry(BrowserAction.ShowTaskMenu, R.string.task_menu_title),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_file,
            listOf(
                BrowserActionEntry(BrowserAction.ShowEpubDialog, R.string.menu_save_epub),
                BrowserActionEntry(BrowserAction.SavePageForLater, R.string.menu_save_archive),
                BrowserActionEntry(BrowserAction.SaveWebArchive, R.string.action_save_web_archive),
            ),
        ),
        BrowserActionCategory(
            R.string.action_category_dialog,
            listOf(
                BrowserActionEntry(BrowserAction.FocusOnInput, R.string.input_url),
                BrowserActionEntry(BrowserAction.ShowMenuDialog, R.string.menu),
                BrowserActionEntry(BrowserAction.ShowFastToggleDialog, R.string.action_fast_toggle),
                BrowserActionEntry(BrowserAction.ShowSiteSettingsDialog, R.string.site_settings),
            ),
        ),
    )

    private val entriesById: Map<String, BrowserActionEntry> =
        categories.flatMap { it.entries }.associateBy { it.id }

    val nothingEntry: BrowserActionEntry =
        BrowserActionEntry(BrowserAction.Noop, R.string.nothing)

    fun entryOf(id: String): BrowserActionEntry =
        if (id == nothingEntry.id) nothingEntry else (entriesById[id] ?: nothingEntry)

    fun entryOf(action: BrowserAction): BrowserActionEntry =
        entryOf(action::class.simpleName.orEmpty())

    fun idOf(action: BrowserAction): String = action::class.simpleName.orEmpty()

    fun migrateLegacyId(stored: String?): String {
        if (stored.isNullOrEmpty()) return ""
        // New format: BrowserAction subclass simple name (non-numeric).
        if (stored.firstOrNull()?.isDigit() != true) return stored
        // Legacy format: 2-char GestureType code like "01".
        val legacy = GestureType.from(stored)
        return legacyGestureToActionId[legacy] ?: nothingEntry.id
    }

    private val legacyGestureToActionId: Map<GestureType, String> = mapOf(
        GestureType.NothingHappen to nothingEntry.id,
        GestureType.Forward to idOf(BrowserAction.GoForward),
        GestureType.Backward to idOf(BrowserAction.HandleBackKey),
        GestureType.ScrollToTop to idOf(BrowserAction.JumpToTop),
        GestureType.ScrollToBottom to idOf(BrowserAction.JumpToBottom),
        GestureType.ToLeftTab to idOf(BrowserAction.GotoLeftTab),
        GestureType.ToRightTab to idOf(BrowserAction.GotoRightTab),
        GestureType.Overview to idOf(BrowserAction.ShowOverview),
        GestureType.OpenNewTab to idOf(BrowserAction.NewATab),
        GestureType.CloseTab to idOf(BrowserAction.RemoveAlbum),
        GestureType.PageUp to idOf(BrowserAction.PageUp),
        GestureType.PageDown to idOf(BrowserAction.PageDown),
        GestureType.Bookmark to idOf(BrowserAction.OpenBookmarkPage),
        GestureType.Back to idOf(BrowserAction.HandleBackKey),
        GestureType.Fullscreen to idOf(BrowserAction.ToggleFullscreen),
        GestureType.Refresh to idOf(BrowserAction.RefreshAction),
        GestureType.Menu to idOf(BrowserAction.ShowMenuDialog),
        // ToggleTouchPagination was a duplicate of ToggleTouchTurnPage and has
        // been removed from the catalog — remap legacy bindings to the survivor.
        GestureType.TouchPagination to idOf(BrowserAction.ToggleTouchTurnPage),
        GestureType.KeyPageUp to idOf(BrowserAction.SendPageUpKey),
        GestureType.KeyPageDown to idOf(BrowserAction.SendPageDownKey),
        GestureType.KeyLeft to idOf(BrowserAction.SendLeftKey),
        GestureType.KeyRight to idOf(BrowserAction.SendRightKey),
        GestureType.InputUrl to idOf(BrowserAction.FocusOnInput),
    )
}
