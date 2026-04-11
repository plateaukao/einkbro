package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPageDao {
    @Query("SELECT * FROM saved_pages ORDER BY savedAt DESC")
    fun getAllSavedPages(): Flow<List<SavedPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedPage: SavedPage)

    @Delete
    suspend fun delete(savedPage: SavedPage)

    @Query("DELETE FROM saved_pages WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM saved_pages ORDER BY savedAt DESC")
    suspend fun getAllSavedPagesAsync(): List<SavedPage>

    @Query("DELETE FROM saved_pages")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<SavedPage>)
}
