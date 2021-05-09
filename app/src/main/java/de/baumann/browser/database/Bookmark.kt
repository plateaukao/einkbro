package de.baumann.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    val title: String,
    val url: String,
    val isDirectory: Boolean,
    val parent: Int
) {
    @PrimaryKey (autoGenerate = true)
    var id: Int = 0
}
