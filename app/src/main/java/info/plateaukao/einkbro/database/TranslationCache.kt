package info.plateaukao.einkbro.database

import androidx.room.Entity

const val TRANSLATION_CACHE_EXPIRATION_DAYS = 7L
const val TRANSLATION_CACHE_MAX_ENTRIES = 5000

// Keyed by a SHA-256 hash of the source text rather than the text itself:
// paragraph-sized keys would be stored twice (row + index b-tree) and bloat
// the composite-PK index. translateApi is part of the key because providers
// produce different translations and users switch providers for quality.
@Entity(tableName = "translation_cache", primaryKeys = ["textHash", "targetLanguage", "translateApi"])
data class TranslationCache(
    val textHash: String,
    val targetLanguage: String,
    val translateApi: String,
    val translatedText: String,
    val timestamp: Long
)
