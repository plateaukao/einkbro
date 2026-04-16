package info.plateaukao.einkbro.data.remote

import android.util.Log
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.data.remote.model.Content
import info.plateaukao.einkbro.data.remote.model.ContentPart
import info.plateaukao.einkbro.data.remote.model.RequestData
import info.plateaukao.einkbro.data.remote.model.ResponseData
import info.plateaukao.einkbro.data.remote.model.SafetySetting
import info.plateaukao.einkbro.viewmodel.unescape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import okio.buffer
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OpenAiRepository : KoinComponent {

    private val config: ConfigManager by inject()

    private val apiKey: String = config.ai.gptApiKey

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val factory by lazy { EventSources.createFactory(client) }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Separate Json instance for tool-calling requests. `explicitNulls = false` omits
     * null properties (e.g. `tool_call_id` / `tool_calls` on user/system messages) —
     * OpenAI's API is strict about those fields only being present on the correct
     * roles, and emitting them as explicit null can cause the server to ignore tools
     * silently.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private val toolJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private var eventSource: EventSource? = null
    fun cancel() {
        eventSource?.cancel()
        eventSource = null
    }

    fun chatStream(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        failureAction: (ApiResult.Failure) -> Unit,
    ) {
        if (gptActionInfo.actionType == GptActionType.Gemini) {
            geminiStream(messages, appendResponseAction, gptActionInfo, doneAction, failureAction)
        } else {
            openAiStream(messages, appendResponseAction, doneAction, gptActionInfo, failureAction)
        }
    }

    private fun openAiStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        gptActionInfo: ChatGPTActionInfo,
        failureAction: (ApiResult.Failure) -> Unit,
    ) {
        if (apiKey.isEmpty() && gptActionInfo.actionType == GptActionType.OpenAi) {
            failureAction(ApiResult.Failure(ApiResult.Kind.MissingKey, "OpenAI API key not set"))
            return
        }
        val request = createCompletionRequest(messages, gptActionInfo, true)

        eventSource?.cancel()
        eventSource = factory.newEventSource(request, object : okhttp3.sse.EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource, id: String?, type: String?, data: String,
            ) {
                if (data == "[DONE]") {
                    doneAction()
                    eventSource.cancel()
                    this@OpenAiRepository.eventSource = null
                    return
                }
                if (data.isEmpty()) return
                try {
                    val chatCompletion =
                        json.decodeFromString(ChatCompletionDelta.serializer(), data)
                    appendResponseAction(chatCompletion.choices.first().delta.content.orEmpty())
                } catch (e: Exception) {
                    Log.e("OpenAiRepository", "Error parsing chat completion: $data", e)
                    failureAction(ApiResult.Failure(ApiResult.Kind.Parse, "Could not parse AI response", cause = e))
                    eventSource.cancel()
                    this@OpenAiRepository.eventSource = null
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                val code = response?.code
                when {
                    code == 200 -> {
                        doneAction()
                        this@OpenAiRepository.eventSource = null
                    }
                    code == 429 -> {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull()
                        failureAction(
                            ApiResult.Failure(
                                ApiResult.Kind.RateLimited,
                                "AI provider rate limit reached",
                                retryAfterSeconds = retryAfter,
                                cause = t,
                            )
                        )
                    }
                    code == 401 || code == 403 -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.MissingKey, "AI provider rejected the API key", cause = t)
                    )
                    code != null && code in 500..599 -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.ServerError, "AI provider error ($code)", cause = t)
                    )
                    t != null -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.Network, t.message ?: "Network error", cause = t)
                    )
                    else -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.Unknown, "AI request failed")
                    )
                }
            }
        })
    }

    private fun geminiStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        gptActionInfo: ChatGPTActionInfo,
        doneAction: () -> Unit = {},
        failureAction: (ApiResult.Failure) -> Unit,
    ) {
        if (config.ai.geminiApiKey.isEmpty()) {
            failureAction(ApiResult.Failure(ApiResult.Kind.MissingKey, "Gemini API key not set"))
            return
        }
        val request = createGeminiRequest(messages, gptActionInfo, true)
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val kind = when (response.code) {
                        401, 403 -> ApiResult.Kind.MissingKey
                        429 -> ApiResult.Kind.RateLimited
                        in 500..599 -> ApiResult.Kind.ServerError
                        else -> ApiResult.Kind.Unknown
                    }
                    failureAction(ApiResult.Failure(kind, "Gemini request failed (${response.code})"))
                    return
                }
                val inputStream = response.body?.byteStream() ?: return
                val textField = "\"text\": \""
                val finishReasonString = "\"finishReason\": \""
                inputStream.source().buffer().use { source ->
                    while (!source.exhausted()) {
                        val chunk = source.readUtf8Line()
                        if (chunk == null) {
                            failureAction(
                                ApiResult.Failure(ApiResult.Kind.Network, "Gemini stream ended unexpectedly")
                            )
                            return
                        }
                        Log.d("OpenAiRepository", "chunk: $chunk")
                        if (chunk.contains(textField)) {
                            var text =
                                chunk.substringAfter(textField).removeSuffix("\"")
                            Log.d("OpenAiRepository", "text: $text")
                            appendResponseAction(text.unescape())
                        } else if (chunk.contains(finishReasonString)) {
                            val finishReason = chunk.substringAfter(finishReasonString)
                            Log.d("OpenAiRepository", "finishReason: $finishReason")
                            if (finishReason.contains("STOP")) {
                                doneAction()
                                eventSource?.cancel()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error fetching Gemini stream", e)
            failureAction(
                ApiResult.Failure(ApiResult.Kind.Network, e.message ?: "Network error", cause = e)
            )
            return
        }
    }

    suspend fun tts(text: String): ByteArray? = suspendCoroutine { continuation ->
        val request = createTtsRequest(
            text,
            speed = (config.tts.ttsSpeedValue / 100F).toDouble(),
            voiceOption = config.ai.gptVoiceOption,
        )

        try {
            client.newCall(request).execute().use { response ->
                if (response.code != 200 || response.body == null) {
                    return@use continuation.resume(null)
                }
                continuation.resume(response.body?.bytes())
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error fetching TTS", e)
            continuation.resume(null)
        }
    }

    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
    ): ChatCompletion? = suspendCoroutine { continuation ->
        val request = createCompletionRequest(messages, gptActionInfo)
        try {
            client.newCall(request).execute().use { response ->
                if (response.code != 200 || response.body == null) {
                    return@use continuation.resume(null)
                }

                val responseString = response.body?.string().orEmpty()
                val chatCompletion =
                    json.decodeFromString(ChatCompletion.serializer(), responseString)
                Log.d("OpenAiRepository", "chatCompletion: $chatCompletion")
                continuation.resume(chatCompletion)
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error fetching chat completion", e)
            continuation.resume(null)
        }
    }

    /**
     * Non-streaming chat completion with tool-calling support. Used by the free-form task
     * agent loop. OpenAI-compatible backends only (the Gemini path has a different schema).
     */
    suspend fun chatWithTools(
        messages: List<ToolChatMessage>,
        tools: List<ToolDefinition>,
        gptActionInfo: ChatGPTActionInfo,
    ): ToolChatCompletion? = suspendCoroutine { continuation ->
        val payload = ToolChatRequest(
            model = gptActionInfo.model,
            messages = messages,
            tools = tools,
            toolChoice = "auto",
        )
        val body = toolJson.encodeToString(payload)
        Log.d("OpenAiRepository", "chatWithTools request: $body")
        val request = Request.Builder()
            .url("${getServerUrl(gptActionInfo.actionType)}$completionPath")
            .post(body.toRequestBody(mediaType))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.code != 200) {
                    Log.e("OpenAiRepository", "chatWithTools ${response.code}: $responseBody")
                    return@use continuation.resume(null)
                }
                Log.d("OpenAiRepository", "chatWithTools response: $responseBody")
                val parsed = toolJson.decodeFromString(ToolChatCompletion.serializer(), responseBody)
                continuation.resume(parsed)
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error in chatWithTools", e)
            continuation.resume(null)
        }
    }

    suspend fun queryGemini(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            if (config.ai.geminiApiKey.isEmpty()) {
                return@withContext ApiResult.Failure(ApiResult.Kind.MissingKey, "Gemini API key not set")
            }
            try {
                val request = createGeminiRequest(messages, gptActionInfo, false)
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val kind = when (response.code) {
                        401, 403 -> ApiResult.Kind.MissingKey
                        429 -> ApiResult.Kind.RateLimited
                        in 500..599 -> ApiResult.Kind.ServerError
                        else -> ApiResult.Kind.Unknown
                    }
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()
                    return@withContext ApiResult.Failure(
                        kind,
                        "Gemini request failed (${response.code})",
                        retryAfterSeconds = retryAfter,
                    )
                }

                val responseBody = response.body?.string()
                    ?: return@withContext ApiResult.Failure(
                        ApiResult.Kind.Parse, "Empty response from Gemini"
                    )
                val responseData = json.decodeFromString(ResponseData.serializer(), responseBody)
                val text = responseData.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text.isNullOrEmpty()) {
                    ApiResult.Failure(ApiResult.Kind.Parse, "Gemini returned no content")
                } else {
                    ApiResult.Success(text)
                }
            } catch (exception: Exception) {
                Log.e("OpenAiRepository", "Error querying Gemini API", exception)
                ApiResult.Failure(
                    ApiResult.Kind.Network,
                    exception.message ?: "Network error",
                    cause = exception,
                )
            }
        }
    }

    private fun createGeminiRequest(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
        isStream: Boolean,
    ): Request {
        val apiPrefix = "https://generativelanguage.googleapis.com/v1beta/models/"
        val model = gptActionInfo.model
        val apiUrl = if (isStream)
            "$apiPrefix$model:streamGenerateContent"
        else
            "$apiPrefix$model:generateContent"

        val json = Json { ignoreUnknownKeys = true }

        // Pass the key in the x-goog-api-key header rather than the URL query string so it
        // doesn't leak to proxy logs, referrer headers, or crash-report URL captures.
        val headers = mapOf(
            "Content-Type" to "application/json",
            "x-goog-api-key" to config.ai.geminiApiKey,
        )

        val data = RequestData(
            contents = listOf(
                Content(parts = listOf(ContentPart(text = messages.joinToString(" ") { it.content })))
            ),
            safety_settings = listOf(
                SafetySetting(
                    category = "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    threshold = "BLOCK_NONE"
                ),
                SafetySetting(
                    category = "HARM_CATEGORY_HATE_SPEECH",
                    threshold = "BLOCK_NONE"
                ),
                SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_ONLY_HIGH"),
                SafetySetting(
                    category = "HARM_CATEGORY_DANGEROUS_CONTENT",
                    threshold = "BLOCK_NONE"
                )
            )
        )

        val requestBody =
            json.encodeToString(data).toRequestBody("application/json".toMediaTypeOrNull())

        return Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()
    }

    private fun createCompletionRequest(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
        stream: Boolean = false,
    ): Request = Request.Builder()
        .url("${getServerUrl(gptActionInfo.actionType)}$completionPath")
        .post(
            json.encodeToString(ChatRequest(gptActionInfo.model, messages, stream))
                .toRequestBody(mediaType)
        )
        .header("Authorization", "Bearer $apiKey")
        .build()

    private fun getServerUrl(gptActionType: GptActionType): String {
        return if (gptActionType == GptActionType.SelfHosted) {
            config.ai.gptUrl
        } else {
            "https://api.openai.com"
        }
    }

    private fun createTtsRequest(
        text: String,
        speed: Double = 1.0,
        voiceOption: GptVoiceOption = GptVoiceOption.Alloy,
    ): Request = Request.Builder()
        .url("${getServerUrl(GptActionType.OpenAi)}$ttsPath")
        .post(
            json.encodeToString(
                TTSRequest(
                    text,
                    config.ai.gptVoiceModel,
                    voiceOption.name.lowercase(Locale("en")),
                    speed,
                    instructions = config.ai.gptVoicePrompt
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
    val usage: ChatUsage = ChatUsage(0, 0, 0),
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
    val totalTokens: Int,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val reasoning: Reasoning = Reasoning(),
)

@Serializable
data class Reasoning(
    val effort: String = "none",
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
    val role: ChatRole,
)

@Serializable
data class TTSRequest(
    val input: String,
    val model: String,
    val voice: String,
    val speed: Double = 1.0,
    val format: String = "aac",
    val instructions: String = "",
)

enum class GptVoiceOption {
    Alloy, Echo, Fable, Onyx, Nova, Shimmer
}

// ── Tool-calling types (parallel to ChatRequest/ChatMessage) ──────────────
// OpenAI function-calling uses nullable `content` and an extra `tool_calls` array
// on assistant messages, plus a `tool` role for results. We keep these separate
// from ChatMessage so the existing streaming/Gemini code remains untouched.

@Serializable
data class ToolChatRequest(
    val model: String,
    val messages: List<ToolChatMessage>,
    val tools: List<ToolDefinition>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    val stream: Boolean = false,
)

@Serializable
data class ToolChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDef,
)

@Serializable
data class FunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonElement,
)

@Serializable
data class ToolChatCompletion(
    val choices: List<ToolChatChoice>,
)

@Serializable
data class ToolChatChoice(
    val index: Int = 0,
    val message: ToolChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)
