package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "COOKIE")
data class CookieDomain(
    @PrimaryKey
    val DOMAIN: String,
)
