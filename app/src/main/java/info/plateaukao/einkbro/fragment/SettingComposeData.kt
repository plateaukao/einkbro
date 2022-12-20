package info.plateaukao.einkbro.fragment

import kotlin.reflect.KMutableProperty0
import info.plateaukao.einkbro.R

interface SettingItemInterface {
    val titleResId: Int
    val summaryResId: Int
    val iconId: Int
}

class BooleanSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    override val summaryResId: Int = 0,
    val booleanPreference: KMutableProperty0<Boolean>,
) : SettingItemInterface

class ListSettingItem<T : Enum<T>>(
    override val titleResId: Int,
    override val iconId: Int,
    override val summaryResId: Int = 0,
    var config: KMutableProperty0<T>,
    val options: List<Int>,
) : SettingItemInterface

class ActionSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    override val summaryResId: Int = 0,
    val action: () -> Unit,
) : SettingItemInterface

enum class LinkSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    val url: String,
    override val summaryResId: Int = 0,
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
    Medium(R.string.medium_articles, R.drawable.ic_reader, "https://medium.com/einkbro")
}

enum class AboutSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    val url: String,
    override val summaryResId: Int = 0,
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
    Medium(R.string.medium_articles, R.drawable.ic_reader, "https://medium.com/einkbro")
}
