package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "chat_gpt_query")
data class ChatGptQuery(
    var date: Long,
    var url: String,
    var model: String,
    var selectedText: String,
    var result: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}