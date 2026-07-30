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
}
