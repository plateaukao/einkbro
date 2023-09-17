package info.plateaukao.einkbro.preference


data class RecentBookmark(val name: String, val url: String, var count: Int) {
    fun toSerializedString(): String = "$name::$url::$count"
}

fun String.toRecentBookmark(): RecentBookmark? {
    val segments = this.split("::", limit = 3)
    if (segments.size != 3) return null
    return RecentBookmark(segments[0], segments[1], segments[2].toInt())
}

