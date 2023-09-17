package info.plateaukao.einkbro.preference

data class CustomFontInfo(
    val name: String,
    val url: String
) {
    fun toSerializedString(): String = "$name::$url"
}

fun String.toCustomFontInfo(): CustomFontInfo? {
    val segments = this.split("::", limit = 2)
    if (segments.size != 2) return null
    return CustomFontInfo(segments[0], segments[1])
}


