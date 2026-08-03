package info.plateaukao.einkbro.setting

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.preference.EinkImageAdjustment
import info.plateaukao.einkbro.preference.EinkImageMode
import info.plateaukao.einkbro.preference.ToolbarPosition
import kotlin.reflect.KMutableProperty0

interface SettingItemInterface {
    val titleResId: Int
    val summaryResId: Int
    val iconId: Int
    val span: Int
}

class DividerSettingItem(
    override val titleResId: Int = 0,
) : SettingItemInterface {
    override val summaryResId: Int = 0
    override val iconId: Int = 0
    override val span: Int = 2
}

class BooleanSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    val config: KMutableProperty0<Boolean>,
    override val span: Int = 1,
) : SettingItemInterface

class ListSettingWithEnumItem<T : Enum<T>>(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<T>,
    val options: List<Int>,
    override val span: Int = 1,
) : SettingItemInterface

class ToolbarPositionSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    val config: KMutableProperty0<ToolbarPosition>,
    override val span: Int = 1,
) : SettingItemInterface

class EinkImageSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    val config: KMutableProperty0<EinkImageAdjustment>,
    val modeConfig: KMutableProperty0<EinkImageMode>,
    override val span: Int = 1,
) : SettingItemInterface

class ListSettingWithStrResIdItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<String>,
    val options: List<Int>,
    override val span: Int = 1,
) : SettingItemInterface

class ListSettingWithClassItem<T>(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<String>,
    val options: List<String>,
    override val span: Int = 1,
) : SettingItemInterface

class GestureActionSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    val config: KMutableProperty0<BrowserAction>,
    override val span: Int = 1,
) : SettingItemInterface

open class ActionSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    override val span: Int = 1,
    open val action: () -> Unit,
) : SettingItemInterface

open class NavigateSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    override val span: Int = 1,
    val destination: SettingRoute,
) : SettingItemInterface

class VersionSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    override val span: Int = 1,
    val destination: SettingRoute,
) : SettingItemInterface

class ValueSettingItem<T>(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<T>,
    override val span: Int = 1,
    val showValue: Boolean = false,
) : SettingItemInterface


enum class LinkSettingItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    val url: String,
    override val summaryResId: Int = 0,
    override val span: Int = 1,
) : SettingItemInterface {
    ChangeLogs(
        R.string.changelogs,
        R.drawable.icon_earth,
        "https://github.com/plateaukao/einkbro/blob/main/CHANGELOG.md"
    ),
    Contributors(
        R.string.contributors,
        R.drawable.icon_copyright,
        "https://github.com/plateaukao/einkbro/blob/main/CONTRIBUTORS.md"
    ),
    Manual(R.string.manual, R.drawable.ic_reader, "https://plateaukao.github.io/einkbro/guide.html#overview")
}
