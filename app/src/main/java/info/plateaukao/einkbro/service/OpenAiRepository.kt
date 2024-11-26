package info.plateaukao.einkbro.service

import android.util.Log
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
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

    private val apiKey: String = config.gptApiKey

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
        gptActionInfo: ChatGPTActionInfo,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        failureAction: () -> Unit,
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
        failureAction: () -> Unit,
    ) {
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
                    failureAction()
                    eventSource.cancel()
                    this@OpenAiRepository.eventSource = null
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                if(response?.code == 200) {
                    doneAction()
                    this@OpenAiRepository.eventSource = null
                } else {
                    failureAction()
                }
            }
        })
    }

    private fun geminiStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        gptActionInfo: ChatGPTActionInfo,
        doneAction: () -> Unit = {},
        failureAction: () -> Unit,
    ) {
        val request = createGeminiRequest(messages, gptActionInfo, true)
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    failureAction()
                    return
                }
                val inputStream = response.body?.byteStream() ?: return
                inputStream.source().buffer().use { source ->
                    while (!source.exhausted()) {
                        val chunk = source.readUtf8Line()
                        if (chunk == null) {
                            failureAction()
                            return
                        }
                        Log.d("OpenAiRepository", "chunk: $chunk")
                        val textField = "\"text\": \""
                        if (chunk.contains(textField)) {
                            var text =
                                chunk.substringAfter(textField).removeSuffix("\"")
                            Log.d("OpenAiRepository", "text: $text")
                            appendResponseAction(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error fetching Gemini stream", e)
            failureAction()
            return
        }
    }

    suspend fun tts(text: String): ByteArray? = suspendCoroutine { continuation ->
        val request = createTtsRequest(
            text,
            speed = (config.ttsSpeedValue / 100F).toDouble(),
            voiceOption = config.gptVoiceOption,
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

    suspend fun queryGemini(messages: List<ChatMessage>, gptActionInfo: ChatGPTActionInfo): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = createGeminiRequest(messages, gptActionInfo, false)
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext "Error querying Gemini API: ${response.code}"
                }

                val responseBody =
                    response.body?.string() ?: return@withContext "Empty response from Gemini API"
                val responseData = json.decodeFromString(ResponseData.serializer(), responseBody)
                responseData.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No content available"
            } catch (exception: Exception) {
                Log.e("OpenAiRepository", "Error querying Gemini API", exception)
                "something wrong"
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
            "$apiPrefix$model:streamGenerateContent?key=${config.geminiApiKey}"
        else
            "$apiPrefix$model:generateContent?key=${config.geminiApiKey}"

        val json = Json { ignoreUnknownKeys = true }

        val headers = mapOf(
            "Content-Type" to "application/json"
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
            config.gptUrl
        } else {
            "https://api.openai.com"
        }
    }

    private fun createTtsRequest(
        text: String,
        hd: Boolean = false,
        speed: Double = 1.0,
        voiceOption: GptVoiceOption = GptVoiceOption.Alloy,
    ): Request = Request.Builder()
        .url("${getServerUrl(GptActionType.OpenAi)}$ttsPath")
        .post(
            json.encodeToString(
                TTSRequest(
                    text,
                    if (hd) "tts-1-hd" else "tts-1",
                    voiceOption.name.lowercase(Locale("en")),
                    speed
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
    val role: ChatRole,
)

@Serializable
data class TTSRequest(
    val input: String,
    val model: String,
    val voice: String,
    val speed: Double = 1.0,
    val format: String = "aac",
)

enum class GptVoiceOption {
    Alloy, Echo, Fable, Onyx, Nova, Shimmer
}