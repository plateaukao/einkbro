package info.plateaukao.einkbro.database

import androidx.room.Entity

@Entity(tableName = "translation_cache", primaryKeys = ["originalText", "targetLanguage"])
data class TranslationCache(
    val originalText: String,
    val targetLanguage: String,
    val translatedText: String,
    val timestamp: Long
)
