package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pages")
data class SavedPage(
    val title: String,
    val url: String,
    val filePath: String,
    val savedAt: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
