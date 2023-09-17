package info.plateaukao.einkbro.preference

data class AlbumInfo(
    val title: String,
    val url: String
) {
    fun toSerializedString(): String = "$title::$url"
}

fun String.toAlbumInfo(): AlbumInfo? {
    val segments = this.split("::", limit = 2)
    if (segments.size != 2) return null
    return AlbumInfo(segments[0], segments[1])
}


