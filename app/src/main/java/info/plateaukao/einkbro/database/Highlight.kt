package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "highlights",
    indices = [Index(value = ["articleId"], unique = false)],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)

data class Highlight(
    var articleId: Int,
    var content: String,
) {
    @PrimaryKey (autoGenerate = true)
    var id: Int = 0
}

@Entity(tableName = "articles")
data class Article(
    var title: String,
    var url: String,
    var date: Long,
    var tags: String,
) {
    @PrimaryKey (autoGenerate = true)
    var id: Int = 0
}