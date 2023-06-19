package info.plateaukao.einkbro.service

import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OpenAiRepository(
    private val apiKey: String
) : KoinComponent {

    private val config: ConfigManager by inject()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val factory by lazy { EventSources.createFactory(client) }

    private val json = Json { ignoreUnknownKeys = true }

    fun chatStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        failureAction: () -> Unit,
    ) {
        val request = createRequest(messages, true)

        factory.newEventSource(request, object : okhttp3.sse.EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource, id: String?, type: String?, data: String
            ) {
                if(data == "[DONE]") {
                    doneAction()
                    eventSource.cancel()
                    return
                }
                if (data == null || data.isEmpty()) return
                try {
                    val chatCompletion = json.decodeFromString<ChatCompletionDelta>(data)
                    appendResponseAction(chatCompletion.choices.first().delta.content ?: "")
                } catch (e: Exception) {
                    failureAction()
                    eventSource.cancel()
                }
            }
        })
    }

    suspend fun chatCompletion(
        messages: List<ChatMessage>
    ): ChatCompletion? = suspendCoroutine { continuation ->
        val request = createRequest(messages)
        client.newCall(request).execute().use { response ->
            if (response.code != 200 || response.body == null) {
                return@use continuation.resume(null)
            }

            val responseString = response.body?.string() ?: ""
            try {
                val chatCompletion = json.decodeFromString<ChatCompletion>(responseString)
                Log.d("OpenAiRepository", "chatCompletion: $chatCompletion")
                continuation.resume(chatCompletion)
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    private fun createRequest(
        messages: List<ChatMessage>,
        stream: Boolean = false,
    ): Request = Request.Builder()
        .url(endpoint)
        .post(
            json.encodeToString(ChatRequest(config.gptModel, messages, stream))
                .toRequestBody(mediaType)
        )
        .header("Authorization", "Bearer $apiKey")
        .build()

    companion object {
        private const val endpoint = "https://api.openai.com/v1/chat/completions"
        private val mediaType = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
data class ChatCompletion(
    val id: String,
    val created: Int,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage = ChatUsage(0, 0, 0)
)

@Serializable
data class ChatCompletionDelta(
    val id: String,
    val created: Int,
    val model: String,
    val choices: List<ChatChoiceDelta>,
)

@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completeTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double = 0.5,
)

@Serializable
data class ChatChoiceDelta(
    val index: Int,
    val delta: ChatDelta,
    @kotlinx.serialization.Transient
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @kotlinx.serialization.Transient
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

enum class ChatRole {
    @SerialName("user")
    User,

    @SerialName("system")
    System,

    @SerialName("assistant")
    Assistant
}

@Serializable
data class ChatDelta(
    val content: String? = null,
)

@Serializable
data class ChatMessage(
    val content: String,
    val role: ChatRole
)