package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(record: HistoryRecord)

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    suspend fun getAllHistory(): List<HistoryRecord>

    @Query("DELETE FROM HISTORY WHERE TIME = :time")
    suspend fun deleteByTime(time: Long)

    @Query("DELETE FROM HISTORY WHERE TIME <= :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(records: List<HistoryRecord>) {
        deleteAll()
        records.forEach { insert(it) }
    }
}
