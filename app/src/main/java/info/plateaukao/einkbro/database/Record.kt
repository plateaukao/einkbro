package info.plateaukao.einkbro.database

data class Record(val title: String?, val url: String, val time: Long, val type: RecordType = RecordType.History) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Record -> this.title == other.title && this.url == other.url
        else -> false
    }

    override fun hashCode(): Int = this.title.hashCode() + this.url.hashCode()
}

enum class RecordType { Bookmark, History, Suggestion }