package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    var title: String,
    var url: String,
    val isDirectory: Boolean = false,
    var parent: Int = 0,
    var order: Int = 0,
) {
    @PrimaryKey (autoGenerate = true)
    var id: Int = 0

    override fun toString(): String {
        return title
    }
}
