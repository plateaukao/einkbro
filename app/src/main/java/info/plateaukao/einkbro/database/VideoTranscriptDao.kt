package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoTranscriptDao {
    @Query("SELECT * FROM video_transcripts WHERE videoId = :videoId")
    suspend fun getTranscript(videoId: String): VideoTranscript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(videoTranscript: VideoTranscript)

    @Query("SELECT * FROM video_transcripts")
    suspend fun getAllTranscripts(): List<VideoTranscript>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videoTranscripts: List<VideoTranscript>)

    @Query("DELETE FROM video_transcripts")
    suspend fun deleteAll()
}
