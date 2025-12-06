package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE originalText = :originalText AND targetLanguage = :targetLanguage")
    suspend fun getTranslation(originalText: String, targetLanguage: String): TranslationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translationCache: TranslationCache)

    @Query("DELETE FROM translation_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}
