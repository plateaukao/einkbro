package info.plateaukao.einkbro.setting

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute
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

class ListSettingWithStringItem(
    override val titleResId: Int,
    override val iconId: Int = 0,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<String>,
    val options: List<Int>,
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
    ProjectSite(R.string.project_site, R.drawable.ic_home, "https://github.com/plateaukao/browser"),
    LatestRelease(
        R.string.latest_release,
        R.drawable.icon_earth,
        "https://github.com/plateaukao/browser/releases"
    ),
    Facebook(R.string.twitter, R.drawable.icon_earth, "https://twitter.com/einkbro"),
    ChangeLogs(
        R.string.changelogs,
        R.drawable.icon_earth,
        "https://github.com/plateaukao/browser/blob/main/CHANGELOG.md"
    ),
    Contributors(
        R.string.contributors,
        R.drawable.icon_copyright,
        "https://github.com/plateaukao/browser/blob/main/CONTRIBUTORS.md"
    ),
    Medium(R.string.medium_articles, R.drawable.ic_reader, "https://medium.com/einkbro"),
    Manual(R.string.manual, R.drawable.ic_reader, "https://einkbro.github.io/docs/home/")
}
