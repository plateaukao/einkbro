package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit

class TabConfig(private val sp: SharedPreferences) {

    var enableWebBkgndLoad by BooleanPreference(sp, K_BKGND_LOAD, true)
    var shouldSaveTabs by BooleanPreference(sp, K_SHOULD_SAVE_TABS, true)
    var shouldShowNextAfterRemoveTab by BooleanPreference(sp, K_SHOW_NEXT_AFTER_REMOVE_TAB, false)
    var closeTabWhenNoMoreBackHistory by BooleanPreference(sp, K_CLOSE_TAB_WHEN_BACK, true)
    var confirmTabClose by BooleanPreference(sp, K_CONFIRM_TAB_CLOSE, false)
    var shouldShowTabBar by BooleanPreference(sp, K_SHOW_TAB_BAR, false)

    var newTabBehavior: NewTabBehavior
        get() = NewTabBehavior.entries[sp.getString(K_NEW_TAB_BEHAVIOR, "0")?.toInt() ?: 0]
        set(value) = sp.edit { putString(K_NEW_TAB_BEHAVIOR, value.ordinal.toString()) }

    var savedAlbumInfoList: List<AlbumInfo>
        get() {
            val string = sp.getString(K_SAVED_ALBUM_INFO, "").orEmpty()
            if (string.isBlank()) return emptyList()

            return try {
                string.split(ALBUM_INFO_SEPARATOR).mapNotNull { it.toAlbumInfo() }
            } catch (exception: Exception) {
                sp.edit { remove(K_SAVED_ALBUM_INFO) }
                emptyList()
            }
        }
        set(value) {
            if (value.containsAll(savedAlbumInfoList) && savedAlbumInfoList.containsAll(value)) {
                return
            }

            sp.edit {
                if (value.isEmpty()) {
                    remove(K_SAVED_ALBUM_INFO)
                } else {
                    putString(
                        K_SAVED_ALBUM_INFO,
                        value.joinToString(ALBUM_INFO_SEPARATOR) { it.toSerializedString() }
                    )
                }
            }
        }

    var currentAlbumIndex: Int
        get() = sp.getInt(K_SAVED_ALBUM_INDEX, 0)
        set(value) {
            if (currentAlbumIndex == value) return
            sp.edit { putInt(K_SAVED_ALBUM_INDEX, value) }
        }

    var saveHistoryMode: SaveHistoryMode
        get() {
            val str = sp.getString(K_SAVE_HISTORY_MODE, "")
            return if (str.isNullOrEmpty()) {
                // For backward compatibility.
                if (sp.getBoolean(K_SAVE_HISTORY, true)) {
                    SaveHistoryMode.SAVE_WHEN_OPEN
                } else {
                    SaveHistoryMode.DISABLED
                }
            } else {
                SaveHistoryMode.entries[str.toInt()]
            }
        }
        set(value) {
            sp.edit {
                putString(K_SAVE_HISTORY_MODE, value.ordinal.toString())
            }
        }

    fun isSaveHistoryWhenLoad() = saveHistoryMode == SaveHistoryMode.SAVE_WHEN_OPEN
    fun isSaveHistoryWhenClose() = saveHistoryMode == SaveHistoryMode.SAVE_WHEN_CLOSE
    fun isSaveHistoryOn() = saveHistoryMode != SaveHistoryMode.DISABLED

    // For tracking state in fast toggling only
    var toggledSaveHistoryMode: SaveHistoryMode = SaveHistoryMode.SAVE_WHEN_OPEN

    var purgeHistoryTimestamp: Long
        get() = sp.getLong(K_HISTORY_PURGE_TS, 0L)
        set(value) = sp.edit { putLong(K_HISTORY_PURGE_TS, value) }

    companion object {
        const val K_BKGND_LOAD = "sp_background_loading"
        const val K_SHOULD_SAVE_TABS = "sp_shouldSaveTabs"
        const val K_SHOW_NEXT_AFTER_REMOVE_TAB = "sp_show_previous_after_remove_tab"
        const val K_CLOSE_TAB_WHEN_BACK = "sp_close_tab_when_no_more_back_history"
        const val K_CONFIRM_TAB_CLOSE = "sp_close_tab_confirm"
        const val K_SHOW_TAB_BAR = "sp_show_tab_bar"
        const val K_NEW_TAB_BEHAVIOR = "sp_plus_behavior"
        const val K_SAVED_ALBUM_INFO = "sp_saved_album_info"
        const val K_SAVED_ALBUM_INDEX = "sp_saved_album_index"
        const val K_SAVE_HISTORY = "saveHistory"
        const val K_SAVE_HISTORY_MODE = "saveHistoryMode"
        const val K_HISTORY_PURGE_TS = "sp_history_purge_ts"
        const val K_IS_INCOGNITO_MODE = "sp_incognito"

        private const val ALBUM_INFO_SEPARATOR = "::::"
    }
}
