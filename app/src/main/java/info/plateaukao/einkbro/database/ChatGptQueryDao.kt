package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatGptQueryDao {
    @Query("SELECT * FROM chat_gpt_query ORDER BY date DESC")
    fun getAllChatGptQueries(): Flow<List<ChatGptQuery>>

    @Query("SELECT * FROM chat_gpt_query WHERE id = :id")
    suspend fun getChatGptQueryById(id: Int): ChatGptQuery

    @Delete
    suspend fun deleteChatGptQuery(chatGptQuery: ChatGptQuery)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChatGptQuery(chatGptQuery: ChatGptQuery)
}