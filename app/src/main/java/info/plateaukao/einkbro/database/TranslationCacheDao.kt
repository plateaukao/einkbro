package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE textHash = :textHash AND targetLanguage = :targetLanguage AND translateApi = :translateApi")
    suspend fun getTranslation(textHash: String, targetLanguage: String, translateApi: String): TranslationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translationCache: TranslationCache)

    @Query("UPDATE translation_cache SET timestamp = :timestamp WHERE textHash = :textHash AND targetLanguage = :targetLanguage AND translateApi = :translateApi")
    suspend fun refreshTimestamp(textHash: String, targetLanguage: String, translateApi: String, timestamp: Long)

    @Query("DELETE FROM translation_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)

    @Query("DELETE FROM translation_cache WHERE rowid NOT IN (SELECT rowid FROM translation_cache ORDER BY timestamp DESC LIMIT :maxEntries)")
    suspend fun trimToMaxEntries(maxEntries: Int)
}
