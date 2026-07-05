package info.plateaukao.einkbro.preference

data class SavedFileInfo(
        val title: String,
        val uri: String
) {
    fun toPrefString(): String = "$title$SEPARATOR$uri"

    companion object {
        private const val SEPARATOR = ":$:"
        fun fromString(fileString: String): SavedFileInfo {
            val terms = fileString.split(SEPARATOR)
            return SavedFileInfo(terms[0], terms[1])
        }
    }
}
