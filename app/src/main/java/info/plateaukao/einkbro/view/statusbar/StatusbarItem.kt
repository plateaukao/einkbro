package info.plateaukao.einkbro.view.statusbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import info.plateaukao.einkbro.R

enum class StatusbarItem(
    val titleResId: Int,
    val previewIcon: ImageVector? = null,
    val previewIconResId: Int = 0,
) {
    Time(titleResId = R.string.toolbar_time, previewIcon = Icons.Outlined.AccessTime),
    PageInfo(titleResId = R.string.page_count, previewIconResId = R.drawable.ic_page_count),
    Battery(titleResId = R.string.statusbar_item_battery, previewIcon = Icons.Outlined.BatteryFull),
    Wifi(titleResId = R.string.statusbar_item_wifi, previewIcon = Icons.Outlined.Wifi),
    TouchPagination(
        titleResId = R.string.touch_turn_page,
        previewIconResId = R.drawable.ic_touch_enabled,
    ),
    VolumePagination(
        titleResId = R.string.statusbar_item_volume_pagination,
        previewIcon = Icons.AutoMirrored.Outlined.VolumeUp,
    );

    companion object {
        val defaultItems: List<StatusbarItem> = listOf(Time, PageInfo, Battery, Wifi, TouchPagination, VolumePagination)

        fun fromOrdinal(value: Int): StatusbarItem? = entries.getOrNull(value)
    }
}

enum class StatusbarPosition(val titleResId: Int) {
    Top(R.string.statusbar_position_top),
    Bottom(R.string.statusbar_position_bottom),
}
