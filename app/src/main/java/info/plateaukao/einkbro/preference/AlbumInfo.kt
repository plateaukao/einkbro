package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

@Serializable
data class AlbumInfo(
    val title: String,
    val url: String
)

// Legacy "title::url" format; kept only to migrate entries saved before the JSON format.
fun String.toAlbumInfo(): AlbumInfo? {
    val segments = this.split("::", limit = 2)
    if (segments.size != 2) return null
    return AlbumInfo(segments[0], segments[1])
}
