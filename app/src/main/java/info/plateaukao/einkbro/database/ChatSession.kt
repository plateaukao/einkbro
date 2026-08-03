package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// One AI chat-with-web-content conversation, moved out of the chat page's
// localStorage so it survives WebView storage clearing and rides along in
// backups. [messages] holds the JSON array the chat page renders
// ([{content, isUser, timestamp}, ...]) verbatim; the page is the only
// reader, so Kotlin never parses it.
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val title: String,
    val created: Long,
    val lastUpdated: Long,
    val webTitle: String,
    val webUrl: String,
    val messages: String,
    // The page text captured when the session was created, so a restored
    // session keeps answering about ITS page. Written natively (never sent
    // through the JS bridge — it can be hundreds of KB). Blank for sessions
    // saved before this column existed.
    val webContent: String = "",
)
