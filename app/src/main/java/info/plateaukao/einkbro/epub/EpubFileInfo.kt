package info.plateaukao.einkbro.epub

data class EpubFileInfo(
        val title: String,
        val uri: String
) {
    fun toPrefString(): String = "$title$SEPARATOR$uri"

    companion object {
        private const val SEPARATOR = ":$:"
        fun fromString(fileString: String): EpubFileInfo {
            val terms = fileString.split(SEPARATOR)
            return EpubFileInfo(terms[0], terms[1])
        }
    }
}
