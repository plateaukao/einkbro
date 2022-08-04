package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    var title: String,
    var url: String,
    var time: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    override fun toString(): String {
        return title
    }
}