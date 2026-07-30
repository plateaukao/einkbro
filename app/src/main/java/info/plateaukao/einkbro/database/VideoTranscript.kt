package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Gemini-generated transcript of a caption-less video, kept permanently so a video
// is only ever transcribed once (each transcription costs minutes and API quota).
@Entity(tableName = "video_transcripts")
data class VideoTranscript(
    @PrimaryKey val videoId: String,
    val transcript: String,
    val timestamp: Long,
)
