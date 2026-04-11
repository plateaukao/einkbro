package info.plateaukao.einkbro.preference

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import androidx.core.content.edit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction

class UiConfig(private val context: Context, private val sp: SharedPreferences) {

    var toolbarPosition: ToolbarPosition
        get() {
            val posOrdinal = sp.getInt(K_TOOLBAR_POSITION, -1)
            if (posOrdinal >= 0) return ToolbarPosition.entries.getOrElse(posOrdinal) { ToolbarPosition.Bottom }
            // migrate from old boolean preference
            return if (sp.getBoolean(K_TOOLBAR_TOP, false)) ToolbarPosition.Top else ToolbarPosition.Bottom
        }
        set(value) = sp.edit { putInt(K_TOOLBAR_POSITION, value.ordinal) }

    var isToolbarOnTop: Boolean
        get() = toolbarPosition == ToolbarPosition.Top
        set(value) { toolbarPosition = if (value) ToolbarPosition.Top else ToolbarPosition.Bottom }

    val isVerticalToolbar: Boolean
        get() = toolbarPosition == ToolbarPosition.Left || toolbarPosition == ToolbarPosition.Right

    var shouldHideToolbar by BooleanPreference(sp, K_HIDE_TOOLBAR, false)
    var showToolbarFirst by BooleanPreference(sp, K_SHOW_TOOLBAR_FIRST, true)
    var hideStatusbar by BooleanPreference(sp, K_HIDE_STATUSBAR, false)
    var keepAwake by BooleanPreference(sp, K_KEEP_AWAKE, false)
    var isBookmarkGridView by BooleanPreference(sp, K_BOOKMARK_GRID_VIEW, false)
    var showShareSaveMenu by BooleanPreference(sp, K_SHOW_SHARE_SAVE_MENU, false)
    var showContentMenu by BooleanPreference(sp, K_SHOW_CONTENT_MENU, false)
    var showDefaultActionMenu by BooleanPreference(sp, K_SHOW_DEFAULT_ACTION_MENU, false)
    var showActionMenuIcons by BooleanPreference(sp, K_SHOW_ACTION_MENU_ICONS, true)

    var fabPosition: FabPosition
        get() = FabPosition.entries[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {
            sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) }
        }

    var fabCustomPosition: Point
        get() {
            val str = sp.getString(K_FAB_POSITION, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION, "${value.x},${value.y}") }
        }

    var fabCustomPositionLandscape: Point
        get() {
            val str = sp.getString(K_FAB_POSITION_LAND, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION_LAND, "${value.x},${value.y}") }
        }

    val shouldUseLargeToolbarConfig: Boolean
        get() = if (isVerticalToolbar) !ViewUnit.isLandscape(context) else ViewUnit.isLandscape(context)

    var toolbarActions: List<ToolbarAction>
        get() {
            val key =
                if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
            val iconListString =
                sp.getString(key, sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings())).orEmpty()
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key =
                    if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

    var readerToolbarActions: List<ToolbarAction>
        get() {
            val iconListString =
                sp.getString(K_READER_TOOLBAR_ICONS, "").orEmpty()
            if (iconListString.isBlank()) return ToolbarAction.defaultReaderActions
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                putString(K_READER_TOOLBAR_ICONS, value.map { it.ordinal }.joinToString(","))
            }
        }

    private fun iconStringToEnumList(iconListString: String): List<ToolbarAction> {
        if (iconListString.isBlank()) return listOf()
        return iconListString.split(",").map { ToolbarAction.fromOrdinal(it.toInt()) }
    }

    private fun getDefaultIconStrings(): String =
        if (ViewUnit.isWideLayout(context)) {
            ToolbarAction.defaultActions.joinToString(",") { action ->
                action.ordinal.toString()
            }
        } else {
            ToolbarAction.defaultActionsForPhone.joinToString(",") { action ->
                action.ordinal.toString()
            }
        }

    companion object {
        const val K_TOOLBAR_POSITION = "sp_toolbar_position"
        const val K_TOOLBAR_TOP = "sp_toolbar_top"
        const val K_HIDE_TOOLBAR = "hideToolbar"
        const val K_SHOW_TOOLBAR_FIRST = "sp_toolbarShow"
        const val K_HIDE_STATUSBAR = "sp_hide_statusbar"
        const val K_KEEP_AWAKE = "sp_screen_awake"
        const val K_BOOKMARK_GRID_VIEW = "sp_bookmark_grid_view"
        const val K_SHOW_SHARE_SAVE_MENU = "sp_show_share_save_menu"
        const val K_SHOW_CONTENT_MENU = "sp_show_content_menu"
        const val K_SHOW_DEFAULT_ACTION_MENU = "sp_show_default_action_menu"
        const val K_SHOW_ACTION_MENU_ICONS = "sp_show_action_menu_icons"
        const val K_NAV_POSITION = "nav_position"
        const val K_FAB_POSITION = "sp_fab_position"
        const val K_FAB_POSITION_LAND = "sp_fab_position_land"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_TOOLBAR_ICONS_FOR_LARGE = "sp_toolbar_icons_for_large"
        const val K_READER_TOOLBAR_ICONS = "sp_reader_toolbar_icons"
    }
}
