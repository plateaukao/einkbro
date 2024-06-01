package info.plateaukao.einkbro.service

import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.data.Content
import info.plateaukao.einkbro.service.data.ContentPart
import info.plateaukao.einkbro.service.data.RequestData
import info.plateaukao.einkbro.service.data.ResponseData
import info.plateaukao.einkbro.service.data.SafetySetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OpenAiRepository : KoinComponent {

    private val config: ConfigManager by inject()

    private val apiKey: String =  config.gptApiKey

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val factory by lazy { EventSources.createFactory(client) }

    private val json = Json { ignoreUnknownKeys = true }

    private var eventSource: EventSource? = null
    fun cancel() {
        eventSource?.cancel()
        eventSource = null
    }

    fun chatStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        failureAction: () -> Unit,
    ) {
        val request = createCompletionRequest(messages, true)

        eventSource?.cancel()
        eventSource = factory.newEventSource(request, object : okhttp3.sse.EventSourceListener() {
            override fun onEvent(
                es: EventSource, id: String?, type: String?, data: String
            ) {
                if (data == "[DONE]") {
                    doneAction()
                    es.cancel()
                    eventSource = null
                    return
                }
                if (data.isEmpty()) return
                try {
                    val chatCompletion = json.decodeFromString<ChatCompletionDelta>(data)
                    appendResponseAction(chatCompletion.choices.first().delta.content ?: "")
                } catch (e: Exception) {
                    failureAction()
                    es.cancel()
                    eventSource = null
                }
            }
        })
    }

    suspend fun tts(text: String): ByteArray? = suspendCoroutine { continuation ->
        val request = createTtsRequest(text)

        client.newCall(request).execute().use { response ->
            if (response.code != 200 || response.body == null) {
                return@use continuation.resume(null)
            }
            try {
                continuation.resume(response.body?.bytes())
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    suspend fun chatCompletion(
        messages: List<ChatMessage>
    ): ChatCompletion? = suspendCoroutine { continuation ->
        val request = createCompletionRequest(messages)
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

    suspend fun queryGemini(contextMessage: String, apiKey: String): String {
        val apiUrl =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"
        val json = Json { ignoreUnknownKeys = true }

        val headers = mapOf(
            "Content-Type" to "application/json"
        )

        val data = RequestData(
            contents = listOf(
                Content(parts = listOf(ContentPart(text = contextMessage)))
            ),
            safety_settings = listOf(
                SafetySetting(
                    category = "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    threshold = "BLOCK_ONLY_HIGH"
                ),
                SafetySetting(
                    category = "HARM_CATEGORY_HATE_SPEECH",
                    threshold = "BLOCK_ONLY_HIGH"
                ),
                SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_ONLY_HIGH"),
                SafetySetting(
                    category = "HARM_CATEGORY_DANGEROUS_CONTENT",
                    threshold = "BLOCK_ONLY_HIGH"
                )
            )
        )

        val requestBody =
            json.encodeToString(data).toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext "Error querying Gemini API: ${response.code}"
                }

                val responseBody =
                    response.body?.string() ?: return@withContext "Empty response from Gemini API"
                val responseData = json.decodeFromString<ResponseData>(responseBody)
                responseData.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No content available"
            } catch (exception: Exception) {
                "something wrong"
            }
        }
    }

    private fun createCompletionRequest(
        messages: List<ChatMessage>,
        stream: Boolean = false,
    ): Request = Request.Builder()
        .url("${getCurrentServerUrl()}$completionPath")
        .post(
            json.encodeToString(ChatRequest(config.gptModel, messages, stream))
                .toRequestBody(mediaType)
        )
        .header("Authorization", "Bearer $apiKey")
        .build()

    private fun getCurrentServerUrl(): String {
        return if (config.useCustomGptUrl && config.gptUrl.isNotBlank()) {
            config.gptUrl
        } else {
            "https://api.openai.com"
        }
    }

    private fun createTtsRequest(
        text: String,
        hd: Boolean = false,
        speed: Double = 1.0,
    ): Request = Request.Builder()
        .url("${getCurrentServerUrl()}$ttsPath")
        .post(
            json.encodeToString(
                TTSRequest(
                    text,
                    if (hd) "tts-1-hd" else "tts-1",
                    "alloy"
                )
            )
                .toRequestBody(mediaType)
        )
        .header("Authorization", "Bearer $apiKey")
        .build()

    companion object {
        private const val completionPath = "/v1/chat/completions"
        private const val ttsPath = "/v1/audio/speech"
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

@Serializable
data class TTSRequest(
    val input: String,
    val model: String,
    val voice: String,
    val speed: Double = 1.0,
    val format: String = "aac"
)
