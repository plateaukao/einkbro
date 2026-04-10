package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "JAVASCRIPT")
data class JavascriptDomain(
    @PrimaryKey
    val DOMAIN: String,
)
