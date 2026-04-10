package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "HISTORY")
data class HistoryRecord(
    val TITLE: String,
    val URL: String,
    val TIME: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    fun toRecord(): Record = Record(
        title = TITLE,
        url = URL,
        time = TIME,
        type = RecordType.History,
    )
}
