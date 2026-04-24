package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.SendToMobile
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.InvertColorsOff
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.HeadsetOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.CloseTab
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Epub
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenHome
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.OpenWith
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Quit
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.SavePdf
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Settings
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.ShareLink
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType.Shortcut

class MenuDialogFragment(
    private val url: String,
    private val isSpeaking: Boolean,
    private val isAudioOnly: Boolean = false,
    private val hasVideo: Boolean = false,
    private val isTouchPaginationEnabled: Boolean = false,
    private val itemClicked: (MenuItemType) -> Unit,
    private val itemLongClicked: (MenuItemType) -> Unit,
) : ComposeDialogFragment() {

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                val hiddenItems = remember {
                    config.ui.hiddenMenuItems.mapNotNull { name ->
                        runCatching { MenuItemType.valueOf(name) }.getOrNull()
                    }.toSet()
                }
                val menuItemOrder = remember { config.ui.menuItemOrder }
                CompositionLocalProvider(
                    LocalMenuHideConfig provides MenuHideConfig(hideMode = false, hiddenItems = hiddenItems)
                ) {
                    MenuItems(
                        hasWhiteBkd = config.whiteBackground(url),
                        boldFont = config.display.boldFontStyle,
                        blackFont = config.display.blackFontStyle,
                        isSpeaking = isSpeaking,
                        isAudioOnly = isAudioOnly,
                        hasVideo = hasVideo,
                        hasInvertedColor = config.hasInvertedColor(url),
                        isTouchPaginationEnabled = isTouchPaginationEnabled,
                        onClicked = { dialog?.dismiss(); itemClicked(it) },
                        onLongClicked = this::runItemLongClickAndDismiss,
                        menuItemOrder = menuItemOrder,
                    )
                }
            }
        }
    }

    private fun runItemLongClickAndDismiss(menuItemType: MenuItemType) {
        itemLongClicked(menuItemType)
        // Dismiss via post so the long-click action fires before the dialog tears down — avoids a crash.
        activity?.window?.decorView?.post { dialog?.dismiss() }
    }

    companion object {
        /**
         * Pre-inflate the menu's Compose content into an off-screen, 0x0 ComposeView so that the
         * first real open skips Compose runtime boot, Material icon vector parsing, and theme setup.
         */
        fun prewarm(activity: FragmentActivity) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            val warmView = ComposeView(activity).apply {
                visibility = View.INVISIBLE
                layoutParams = ViewGroup.LayoutParams(0, 0)
                setContent {
                    MyTheme {
                        MenuItems(
                            hasWhiteBkd = false,
                            boldFont = false,
                            blackFont = false,
                            isSpeaking = false,
                            isAudioOnly = false,
                            hasVideo = true,
                            hasInvertedColor = false,
                            isTouchPaginationEnabled = false,
                            onClicked = {},
                            onLongClicked = {},
                        )
                    }
                }
            }
            root.addView(warmView)
            root.postDelayed({ root.removeView(warmView) }, 3000)
        }
    }
}


enum class MenuItemType {
    Tts, QuickToggle, OpenHome, CloseTab, Quit,
    SplitScreen, Translate, VerticalRead, ReaderMode, TouchSetting, ToolbarSetting,
    ReceiveData, SendLink, ShareLink, OpenWith, Shortcut,
    SetHome, SaveBookmark, Epub, SavePdf,
    FontSize, WhiteBknd, BoldFont, Search, Download, Settings, BlackFont,
    SaveArchive, SaveMht, Highlights, InvertColor, PageAiActions, Instapaper, AudioOnly,
    SiteSettings
}

enum class MenuSection(val headerRes: Int?) {
    Top(null),
    Share(R.string.share_save),
    Content(R.string.content_adjustment),
    Bottom(null),
}

// Flat sequence of items with Boundary markers dividing sections. Boundaries stay fixed
// in relative order; dragging an Item past a Boundary moves it into the other section.
// Spacer is display-only (empty grid cell used to pad the overflow row in bottom-first layouts).
sealed class MenuEntry {
    data class Item(val type: MenuItemType) : MenuEntry()
    data class Boundary(val sectionStart: MenuSection) : MenuEntry()
    object Spacer : MenuEntry()
}

private val defaultSectionItems: Map<MenuSection, List<MenuItemType>> = mapOf(
    MenuSection.Top to listOf(
        MenuItemType.Highlights, MenuItemType.SetHome, MenuItemType.OpenHome,
        MenuItemType.CloseTab, MenuItemType.Quit,
    ),
    MenuSection.Share to listOf(
        MenuItemType.ReceiveData, MenuItemType.SaveBookmark, MenuItemType.Shortcut,
        MenuItemType.OpenWith, MenuItemType.ShareLink,
        MenuItemType.SendLink, MenuItemType.Instapaper, MenuItemType.SaveArchive,
        MenuItemType.SaveMht, MenuItemType.Epub, MenuItemType.SavePdf,
    ),
    MenuSection.Content to listOf(
        MenuItemType.PageAiActions, MenuItemType.SplitScreen, MenuItemType.Translate,
        MenuItemType.VerticalRead, MenuItemType.ReaderMode, MenuItemType.TouchSetting,
        MenuItemType.Tts, MenuItemType.InvertColor, MenuItemType.WhiteBknd,
        MenuItemType.BlackFont, MenuItemType.BoldFont, MenuItemType.FontSize,
    ),
    MenuSection.Bottom to listOf(
        MenuItemType.AudioOnly, MenuItemType.Search, MenuItemType.Download,
        MenuItemType.ToolbarSetting, MenuItemType.QuickToggle, MenuItemType.SiteSettings,
        MenuItemType.Settings,
    ),
)

val defaultMenuEntries: List<MenuEntry> = buildList {
    addAll(defaultSectionItems[MenuSection.Top]!!.map { MenuEntry.Item(it) })
    add(MenuEntry.Boundary(MenuSection.Share))
    addAll(defaultSectionItems[MenuSection.Share]!!.map { MenuEntry.Item(it) })
    add(MenuEntry.Boundary(MenuSection.Content))
    addAll(defaultSectionItems[MenuSection.Content]!!.map { MenuEntry.Item(it) })
    add(MenuEntry.Boundary(MenuSection.Bottom))
    addAll(defaultSectionItems[MenuSection.Bottom]!!.map { MenuEntry.Item(it) })
}

private const val BOUNDARY_PREFIX = "#"

// Spacers are display-only; never persisted.
fun encodeMenuEntries(entries: List<MenuEntry>): List<String> = entries.mapNotNull { e ->
    when (e) {
        is MenuEntry.Item -> e.type.name
        is MenuEntry.Boundary -> BOUNDARY_PREFIX + e.sectionStart.name
        is MenuEntry.Spacer -> null
    }
}

private fun decodeMenuEntries(tokens: List<String>): List<MenuEntry> = tokens.mapNotNull { token ->
    if (token.startsWith(BOUNDARY_PREFIX)) {
        val name = token.removePrefix(BOUNDARY_PREFIX)
        runCatching { MenuSection.valueOf(name) }.getOrNull()?.let { MenuEntry.Boundary(it) }
    } else {
        runCatching { MenuItemType.valueOf(token) }.getOrNull()?.let { MenuEntry.Item(it) }
    }
}

/**
 * Transforms underlying entries into display entries with bottom-first layout:
 * within each section, the last chunk (partial overflow) is moved to the top, preceded
 * by Spacer entries so the top row's leftmost cells are empty.
 */
fun menuDisplayEntries(underlying: List<MenuEntry>, cols: Int = MENU_GRID_COLUMNS): List<MenuEntry> {
    val out = mutableListOf<MenuEntry>()
    val buf = mutableListOf<MenuEntry.Item>()

    fun flush() {
        if (buf.isEmpty()) return
        val chunks = buf.chunked(cols)
        val reversed = chunks.asReversed()
        val topChunk = reversed.first()
        val padding = cols - topChunk.size
        if (padding > 0) repeat(padding) { out.add(MenuEntry.Spacer) }
        out.addAll(topChunk)
        reversed.drop(1).forEach { chunk -> out.addAll(chunk) }
        buf.clear()
    }

    underlying.forEach { e ->
        when (e) {
            is MenuEntry.Item -> buf.add(e)
            is MenuEntry.Boundary -> { flush(); out.add(e) }
            is MenuEntry.Spacer -> Unit
        }
    }
    flush()
    return out
}

/**
 * Inverse of [menuDisplayEntries]: turns a display list (possibly edited by the user's drag)
 * back into underlying Item + Boundary entries. Spacers are dropped; per-section chunks are
 * reversed so the section is stored in natural top-down order.
 */
fun menuDisplayToUnderlying(display: List<MenuEntry>, cols: Int = MENU_GRID_COLUMNS): List<MenuEntry> {
    val out = mutableListOf<MenuEntry>()
    val buf = mutableListOf<MenuEntry>()

    fun flush() {
        if (buf.isEmpty()) return
        val chunks = buf.chunked(cols)
        chunks.asReversed().forEach { chunk ->
            chunk.forEach { e -> if (e is MenuEntry.Item) out.add(e) }
        }
        buf.clear()
    }

    display.forEach { e ->
        when (e) {
            is MenuEntry.Boundary -> { flush(); out.add(e) }
            else -> buf.add(e)
        }
    }
    flush()
    return out
}

// Resolve effective entries: stored order first, then append any items & missing boundaries
// from defaults so newly-added items show up (appended to their default section).
fun effectiveMenuEntries(stored: List<String>): List<MenuEntry> {
    val storedEntries = decodeMenuEntries(stored)
    // Fall back to defaults if nothing is stored, or if the stored data predates Boundary tokens.
    if (storedEntries.isEmpty() || storedEntries.none { it is MenuEntry.Boundary }) return defaultMenuEntries
    val presentItems = storedEntries.filterIsInstance<MenuEntry.Item>().map { it.type }.toSet()
    val presentBoundaries = storedEntries.filterIsInstance<MenuEntry.Boundary>().map { it.sectionStart }.toSet()

    // Ensure every boundary exists; if a boundary is missing, add it at the corresponding
    // default position relative to existing items.
    val result = storedEntries.toMutableList()
    MenuSection.entries.drop(1).forEach { section ->
        if (section !in presentBoundaries) result.add(MenuEntry.Boundary(section))
    }
    // Append missing items at the end of their default section if possible, else at end.
    val missing = defaultMenuEntries.filterIsInstance<MenuEntry.Item>()
        .map { it.type }.filter { it !in presentItems }
    missing.forEach { type ->
        val defaultSection = defaultSectionItems.entries.first { type in it.value }.key
        val insertIdx = findSectionEnd(result, defaultSection)
        result.add(insertIdx, MenuEntry.Item(type))
    }
    return result
}

private fun findSectionEnd(entries: List<MenuEntry>, section: MenuSection): Int {
    // Section starts at the Boundary for it (or index 0 for Top) and ends just before the next Boundary.
    val start = if (section == MenuSection.Top) 0
    else entries.indexOfFirst { it is MenuEntry.Boundary && it.sectionStart == section }.let { if (it < 0) return entries.size else it + 1 }
    val end = entries.drop(start).indexOfFirst { it is MenuEntry.Boundary }
    return if (end < 0) entries.size else start + end
}

const val MENU_GRID_COLUMNS = 6

data class MenuHideConfig(
    val hideMode: Boolean = false,
    val reorderMode: Boolean = false,
    val hiddenItems: Set<MenuItemType> = emptySet(),
    val onToggleHide: (MenuItemType) -> Unit = {},
)

val LocalMenuHideConfig = staticCompositionLocalOf { MenuHideConfig() }

data class MenuActions(
    val onClicked: (MenuItemType) -> Unit = {},
    val onLongClicked: (MenuItemType) -> Unit = {},
)

val LocalMenuActions = staticCompositionLocalOf { MenuActions() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HideableSlot(type: MenuItemType, content: @Composable () -> Unit) {
    val cfg = LocalMenuHideConfig.current
    val isHidden = type in cfg.hiddenItems
    when {
        // Reorder mode: render grayed but don't intercept clicks (drag handle on the item handles input).
        cfg.reorderMode -> Box(modifier = Modifier.alpha(if (isHidden) 0.3f else 1f)) { content() }
        cfg.hideMode -> {
            Box(modifier = Modifier.alpha(if (isHidden) 0.3f else 1f)) {
                content()
                // Overlay absorbs both tap and long-press so underlying MenuItem handlers don't fire.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onLongClick = { cfg.onToggleHide(type) },
                            onClick = { cfg.onToggleHide(type) },
                        )
                )
            }
        }
        isHidden -> Unit
        else -> content()
    }
}

@Composable
private fun HideableMenuItem(
    type: MenuItemType,
    titleResId: Int,
    imageVector: ImageVector? = null,
    iconResId: Int = 0,
    supportsLongClick: Boolean = false,
) {
    val actions = LocalMenuActions.current
    HideableSlot(type) {
        MenuItem(
            titleResId = titleResId,
            iconResId = iconResId,
            imageVector = imageVector,
            onLongClicked = if (supportsLongClick) ({ actions.onLongClicked(type) }) else ({}),
            onClicked = { actions.onClicked(type) },
        )
    }
}

/**
 * Renders a HideableMenuItem for a given MenuItemType, applying any state-dependent icon.
 * Centralizing this lets row rendering be data-driven (iterate over an ordered List<MenuItemType>).
 */
@Composable
fun MenuItemForType(
    type: MenuItemType,
    hasWhiteBkd: Boolean = false,
    boldFont: Boolean = false,
    blackFont: Boolean = false,
    isSpeaking: Boolean = false,
    isAudioOnly: Boolean = false,
    hasInvertedColor: Boolean = false,
    isTouchPaginationEnabled: Boolean = false,
) {
    when (type) {
        MenuItemType.Highlights -> HideableMenuItem(type, R.string.menu_highlights, Icons.Outlined.EditNote)
        MenuItemType.SetHome -> HideableMenuItem(type, R.string.menu_fav, Icons.Outlined.AddHome)
        MenuItemType.OpenHome -> HideableMenuItem(type, R.string.menu_openFav, Icons.Outlined.Home)
        MenuItemType.CloseTab -> HideableMenuItem(type, R.string.menu_closeTab, Icons.Outlined.CancelPresentation)
        MenuItemType.Quit -> HideableMenuItem(type, R.string.menu_quit, Icons.AutoMirrored.Outlined.Logout)
        MenuItemType.ReceiveData -> HideableMenuItem(type, R.string.menu_receive, Icons.Outlined.InstallMobile, supportsLongClick = true)
        MenuItemType.SaveBookmark -> HideableMenuItem(type, R.string.menu_save_bookmark, Icons.Outlined.BookmarkAdd)
        MenuItemType.Shortcut -> HideableMenuItem(type, R.string.menu_sc, Icons.Outlined.AddLink)
        MenuItemType.OpenWith -> HideableMenuItem(type, R.string.menu_open_with, Icons.Outlined.Apps)
        MenuItemType.ShareLink -> HideableMenuItem(type, R.string.menu_share_link, Icons.Outlined.Share, supportsLongClick = true)
        MenuItemType.SendLink -> HideableMenuItem(type, R.string.menu_send_link, Icons.AutoMirrored.Outlined.SendToMobile, supportsLongClick = true)
        MenuItemType.Instapaper -> HideableMenuItem(type, R.string.menu_instapaper, Icons.Outlined.CloudUpload, supportsLongClick = true)
        MenuItemType.SaveArchive -> HideableMenuItem(type, R.string.menu_save_archive, Icons.Outlined.Save, supportsLongClick = true)
        MenuItemType.SaveMht -> HideableMenuItem(type, R.string.menu_save_mht, Icons.Outlined.Save)
        MenuItemType.Epub -> HideableMenuItem(type, R.string.menu_epub, Icons.AutoMirrored.Outlined.Article)
        MenuItemType.SavePdf -> HideableMenuItem(type, R.string.menu_save_pdf, Icons.Outlined.PictureAsPdf)
        MenuItemType.PageAiActions -> HideableMenuItem(type, R.string.page_ai, iconResId = R.drawable.ic_robot)
        MenuItemType.SplitScreen -> HideableMenuItem(type, R.string.split_screen, Icons.Outlined.ViewStream)
        MenuItemType.Translate -> HideableMenuItem(type, R.string.translate, Icons.Outlined.Translate, supportsLongClick = true)
        MenuItemType.VerticalRead -> HideableMenuItem(type, R.string.vertical_read, Icons.Outlined.ViewColumn)
        MenuItemType.ReaderMode -> HideableMenuItem(type, R.string.reader_mode, Icons.AutoMirrored.Outlined.ChromeReaderMode)
        MenuItemType.TouchSetting -> {
            val touchRes = if (isTouchPaginationEnabled) R.drawable.ic_touch_enabled else R.drawable.ic_touch_disabled
            HideableMenuItem(type, R.string.touch_area_setting, iconResId = touchRes, supportsLongClick = true)
        }
        MenuItemType.Tts -> {
            val ttsRes = if (isSpeaking) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver
            HideableMenuItem(type, R.string.menu_tts, ttsRes, supportsLongClick = true)
        }
        MenuItemType.InvertColor -> {
            val invertRes = if (hasInvertedColor) Icons.Outlined.InvertColorsOff else Icons.Outlined.InvertColors
            HideableMenuItem(type, R.string.menu_invert_color, invertRes)
        }
        MenuItemType.WhiteBknd -> {
            val whiteRes = if (hasWhiteBkd) R.drawable.ic_white_background_active else R.drawable.ic_white_background
            HideableMenuItem(type, R.string.white_background, iconResId = whiteRes)
        }
        MenuItemType.BlackFont -> {
            val blackRes = if (blackFont) Icons.TwoTone.Copyright else Icons.Outlined.Copyright
            HideableMenuItem(type, R.string.black_font, blackRes)
        }
        MenuItemType.BoldFont -> {
            val boldRes = if (boldFont) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font
            HideableMenuItem(type, R.string.bold_font, iconResId = boldRes, supportsLongClick = true)
        }
        MenuItemType.FontSize -> HideableMenuItem(type, R.string.font_size, Icons.Outlined.FormatSize)
        MenuItemType.AudioOnly -> {
            val audioOnlyIcon = if (isAudioOnly) Icons.Outlined.Headset else Icons.Outlined.HeadsetOff
            HideableMenuItem(type, R.string.audio_only_mode, audioOnlyIcon)
        }
        MenuItemType.Search -> HideableMenuItem(type, R.string.menu_other_searchSite, Icons.Outlined.Search)
        MenuItemType.Download -> HideableMenuItem(type, R.string.menu_download, Icons.Outlined.Download)
        MenuItemType.ToolbarSetting -> HideableMenuItem(type, R.string.toolbar_icons, Icons.Outlined.Straighten)
        MenuItemType.QuickToggle -> HideableMenuItem(type, R.string.menu_quickToggle, Icons.Outlined.SettingsSuggest)
        MenuItemType.SiteSettings -> HideableMenuItem(type, R.string.site_settings, Icons.Outlined.Tune)
        MenuItemType.Settings -> HideableMenuItem(type, R.string.settings, Icons.Outlined.Settings, supportsLongClick = true)
    }
}

@Composable
fun MenuItems(
    hasWhiteBkd: Boolean,
    boldFont: Boolean,
    blackFont: Boolean,
    isSpeaking: Boolean,
    isAudioOnly: Boolean,
    hasVideo: Boolean,
    hasInvertedColor: Boolean,
    isTouchPaginationEnabled: Boolean,
    onClicked: (MenuItemType) -> Unit,
    onLongClicked: (MenuItemType) -> Unit,
    menuItemOrder: List<String> = emptyList(),
) {
    val hiddenItems = LocalMenuHideConfig.current.hiddenItems
    val hideMode = LocalMenuHideConfig.current.hideMode
    val entries = effectiveMenuEntries(menuItemOrder)

    // Partition into sections by scanning for Boundary markers.
    data class Section(val section: MenuSection, val items: List<MenuItemType>)
    val sections = buildList {
        var current = MenuSection.Top
        val buf = mutableListOf<MenuItemType>()
        fun flush() { add(Section(current, buf.toList())); buf.clear() }
        entries.forEach { e ->
            when (e) {
                is MenuEntry.Item -> buf.add(e.type)
                is MenuEntry.Boundary -> { flush(); current = e.sectionStart }
                is MenuEntry.Spacer -> Unit
            }
        }
        flush()
    }

    // Apply hide + hasVideo filtering per section.
    val sectionsFiltered = sections.map { sec ->
        val filtered = sec.items
            .let { if (hasVideo) it else it.filter { t -> t != MenuItemType.AudioOnly } }
            .let { if (hideMode) it else it.filter { t -> t !in hiddenItems } }
        sec.copy(items = filtered)
    }

    CompositionLocalProvider(
        LocalMenuActions provides MenuActions(onClicked, onLongClicked)
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.End,
        ) {
            val renderItem: @Composable (MenuItemType) -> Unit = { type ->
                MenuItemForType(
                    type = type,
                    hasWhiteBkd = hasWhiteBkd,
                    boldFont = boldFont,
                    blackFont = blackFont,
                    isSpeaking = isSpeaking,
                    isAudioOnly = isAudioOnly,
                    hasInvertedColor = hasInvertedColor,
                    isTouchPaginationEnabled = isTouchPaginationEnabled,
                )
            }
            var renderedAnySection = false
            sectionsFiltered.forEach { sec ->
                if (sec.items.isEmpty()) return@forEach
                if (renderedAnySection) HorizontalSeparator()
                sec.section.headerRes?.let { res ->
                    Text(
                        stringResource(res),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.onBackground,
                    )
                }
                // Reverse the chunk order so the last chunk (full row) ends up visually at
                // the bottom and any partial chunk (overflow) ends up at the top. For 11 items
                // with 6 columns, this renders as 5 items on top and 6 on bottom instead of
                // the default 6 on top and 5 on bottom.
                sec.items.chunked(MENU_GRID_COLUMNS).asReversed().forEach { rowItems ->
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        rowItems.forEach { renderItem(it) }
                    }
                }
                renderedAnySection = true
            }
        }
    }
}

@Composable
fun MenuItem(
    titleResId: Int,
    imageVector: ImageVector,
    isLargeType: Boolean = false,
    showIcon: Boolean = true,
    onLongClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
) {
    MenuItem(
        titleResId,
        0,
        imageVector,
        isLargeType,
        showIcon,
        onLongClicked,
        onClicked,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    titleResId: Int,
    iconResId: Int,
    imageVector: ImageVector? = null,
    isLargeType: Boolean = false,
    showIcon: Boolean = true,
    onLongClicked: () -> Unit = {},
    onClicked: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val configuration = LocalConfiguration.current
    val width = when {
        isLargeType -> if (configuration.screenWidthDp > 500) 62.dp else 50.dp
        configuration.screenWidthDp > 500 -> 55.dp
        else -> 45.dp
    }

    val fontSize = if (!showIcon) 16.sp else if (configuration.screenWidthDp > 500) 10.sp else 8.sp

    // In reorder mode we hand gesture control to the outer ReorderableItem's
    // longPressDraggableHandle — combinedClickable here would consume long-press first.
    val reorderMode = LocalMenuHideConfig.current.reorderMode

    Box {
        if (pressed) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colors.onBackground, shape = CircleShape)
                    .align(Alignment.TopCenter)
            )
        }
        Column(
            modifier = Modifier
                .width(width)
                .height(if (!showIcon) 50.dp else if (isLargeType) 80.dp else 70.dp)
                .then(
                    if (reorderMode) Modifier
                    else Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onLongClick = { onLongClicked() },
                        onClick = { onClicked() },
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (!showIcon) Arrangement.Center else Arrangement.Top
        ) {
            if (showIcon) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector, contentDescription = null,
                        modifier = Modifier
                            .size(if (isLargeType) 55.dp else 44.dp)
                            .padding(horizontal = 6.dp),
                        tint = MaterialTheme.colors.onBackground
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = iconResId), contentDescription = null,
                        modifier = Modifier
                            .size(if (isLargeType) 55.dp else 44.dp)
                            .padding(horizontal = 6.dp),
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = if (showIcon) (-5).dp else 0.dp),
                    text = stringResource(id = titleResId),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = if (!showIcon) 20.sp else 12.sp,
                    fontSize = fontSize,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewItem() {
    MyTheme {
        Column {
            MenuItem(R.string.title_appData, Icons.Outlined.Backup, showIcon = false) {}
            MenuItem(R.string.title, 0, Icons.Outlined.Translate) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMenuItems() {
    MyTheme {
        MenuItems(
            hasWhiteBkd = false,
            boldFont = false,
            blackFont = false,
            isSpeaking = false,
            isAudioOnly = false,
            hasVideo = false,
            hasInvertedColor = false,
            isTouchPaginationEnabled = false,
            onClicked = {},
            onLongClicked = {},
        )
    }
}
