package info.plateaukao.einkbro.data.remote

import android.util.Log
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.preference.ReasoningEffort
import info.plateaukao.einkbro.data.remote.model.Content
import info.plateaukao.einkbro.data.remote.model.ContentPart
import info.plateaukao.einkbro.data.remote.model.GenerationConfig
import info.plateaukao.einkbro.data.remote.model.RequestData
import info.plateaukao.einkbro.data.remote.model.ResponseData
import info.plateaukao.einkbro.data.remote.model.SafetySetting
import info.plateaukao.einkbro.data.remote.model.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    // Agent tool calls are non-streaming, so a reasoning model's whole thinking pass
    // has to fit inside the read timeout — 30s is not enough at higher efforts.
    private val toolClient by lazy {
        client.newBuilder().readTimeout(180, TimeUnit.SECONDS).build()
    }

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
        thinkingAction: () -> Unit = {},
    ) {
        if (gptActionInfo.actionType == GptActionType.Gemini) {
            geminiStream(messages, appendResponseAction, gptActionInfo, doneAction, failureAction, thinkingAction)
        } else {
            openAiStream(messages, appendResponseAction, doneAction, gptActionInfo, failureAction, thinkingAction)
        }
    }

    private fun openAiStream(
        messages: List<ChatMessage>,
        appendResponseAction: (String) -> Unit,
        doneAction: () -> Unit = {},
        gptActionInfo: ChatGPTActionInfo,
        failureAction: (ApiResult.Failure) -> Unit,
        thinkingAction: () -> Unit = {},
        dropReasoningEffort: Boolean = false,
    ) {
        if (apiKey.isEmpty() && gptActionInfo.actionType == GptActionType.OpenAi) {
            failureAction(ApiResult.Failure(ApiResult.Kind.MissingKey, "OpenAI API key not set"))
            return
        }
        val request = createCompletionRequest(messages, gptActionInfo, true, dropReasoningEffort)

        val thinkTagFilter = ThinkTagFilter()
        eventSource?.cancel()
        eventSource = factory.newEventSource(request, object : okhttp3.sse.EventSourceListener() {
            // Cancelling in onEvent after [DONE] makes OkHttp also report
            // onFailure(canceled) with the original 200 response, which used to
            // run doneAction a second time — the chat UI then appended a stray
            // empty assistant bubble. Track completion like geminiStream does.
            private var finished = false

            private fun finish(eventSource: EventSource) {
                if (finished) return
                finished = true
                doneAction()
                eventSource.cancel()
                this@OpenAiRepository.eventSource = null
            }

            override fun onEvent(
                eventSource: EventSource, id: String?, type: String?, data: String,
            ) {
                if (data == "[DONE]") {
                    finish(eventSource)
                    return
                }
                if (data.isEmpty()) return
                try {
                    val chatCompletion =
                        json.decodeFromString(ChatCompletionDelta.serializer(), data)
                    val delta = chatCompletion.choices.first().delta
                    // Dedicated reasoning channel (DeepSeek/Qwen-style servers).
                    if (!delta.reasoningContent.isNullOrEmpty()) thinkingAction()
                    val visible = thinkTagFilter.filter(delta.content.orEmpty())
                    if (thinkTagFilter.thinking) thinkingAction()
                    if (visible.isNotEmpty()) appendResponseAction(visible)
                } catch (e: Exception) {
                    Log.e("OpenAiRepository", "Error parsing chat completion: $data", e)
                    failureAction(ApiResult.Failure(ApiResult.Kind.Parse, "Could not parse AI response", cause = e))
                    finished = true
                    eventSource.cancel()
                    this@OpenAiRepository.eventSource = null
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (finished) return
                val code = response?.code
                val errorBody = if (code != null && code != 200) {
                    try {
                        response.body?.string().orEmpty()
                    } catch (e: Exception) {
                        ""
                    }
                } else ""
                if (!dropReasoningEffort && isEffortRejection(gptActionInfo, code, errorBody)) {
                    // Pre-reasoning models 400 on the reasoning_effort argument itself;
                    // learn the quirk and restart the stream once without it.
                    modelsRejectingEffort.add(gptActionInfo.model)
                    openAiStream(
                        messages, appendResponseAction, doneAction, gptActionInfo,
                        failureAction, thinkingAction, dropReasoningEffort = true,
                    )
                    return
                }
                when {
                    code == 200 -> finish(eventSource)
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
                        ApiResult.Failure(
                            ApiResult.Kind.Unknown,
                            if (errorBody.isNotBlank()) extractApiError(code ?: 0, errorBody)
                            else "AI request failed",
                        )
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
        thinkingAction: () -> Unit = {},
    ) {
        if (config.ai.geminiApiKey.isEmpty()) {
            failureAction(ApiResult.Failure(ApiResult.Kind.MissingKey, "Gemini API key not set"))
            return
        }
        val request = createGeminiRequest(messages, gptActionInfo, true)
        eventSource?.cancel()
        eventSource = factory.newEventSource(request, object : okhttp3.sse.EventSourceListener() {
            // Gemini's SSE stream has no [DONE] sentinel: the last chunk carries a
            // finishReason and then the server closes the connection. Cancelling in
            // onEvent makes OkHttp report onFailure(canceled), so track completion.
            private var finished = false

            private fun finish(eventSource: EventSource) {
                if (finished) return
                finished = true
                doneAction()
                eventSource.cancel()
                this@OpenAiRepository.eventSource = null
            }

            override fun onEvent(
                eventSource: EventSource, id: String?, type: String?, data: String,
            ) {
                if (data.isEmpty()) return
                try {
                    val chunk = json.decodeFromString(ResponseData.serializer(), data)
                    val candidate = chunk.candidates.firstOrNull() ?: return
                    if (candidate.content.parts.any { it.thought }) thinkingAction()
                    val text = candidate.content.parts
                        .filterNot { it.thought }
                        .joinToString("") { it.text }
                    if (text.isNotEmpty()) appendResponseAction(text)
                    // Any finishReason (STOP, MAX_TOKENS, SAFETY, ...) means the
                    // model is done talking — finalize the UI either way.
                    if (candidate.finishReason != null) finish(eventSource)
                } catch (e: Exception) {
                    Log.e("OpenAiRepository", "Error parsing Gemini chunk: $data", e)
                    failureAction(
                        ApiResult.Failure(ApiResult.Kind.Parse, "Could not parse AI response", cause = e)
                    )
                    finished = true
                    eventSource.cancel()
                    this@OpenAiRepository.eventSource = null
                }
            }

            override fun onClosed(eventSource: EventSource) {
                finish(eventSource)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (finished) return
                val code = response?.code
                when {
                    code == 200 -> finish(eventSource)
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
                        ApiResult.Failure(ApiResult.Kind.MissingKey, "Gemini rejected the API key", cause = t)
                    )
                    code != null && code in 500..599 -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.ServerError, "Gemini error ($code)", cause = t)
                    )
                    t != null -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.Network, t.message ?: "Network error", cause = t)
                    )
                    else -> failureAction(
                        ApiResult.Failure(ApiResult.Kind.Unknown, "Gemini request failed")
                    )
                }
            }
        })
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
    ): ChatCompletion? {
        val first = chatCompletionOnce(messages, gptActionInfo, dropReasoningEffort = false)
        if (first.completion != null) return first.completion
        if (isEffortRejection(gptActionInfo, first.code, first.errorBody)) {
            modelsRejectingEffort.add(gptActionInfo.model)
            return chatCompletionOnce(messages, gptActionInfo, dropReasoningEffort = true).completion
        }
        return null
    }

    private class ChatHttpResponse(
        val completion: ChatCompletion?,
        val code: Int?,
        val errorBody: String,
    )

    private suspend fun chatCompletionOnce(
        messages: List<ChatMessage>,
        gptActionInfo: ChatGPTActionInfo,
        dropReasoningEffort: Boolean,
    ): ChatHttpResponse = suspendCoroutine { continuation ->
        val request = createCompletionRequest(
            messages, gptActionInfo, stream = false, dropReasoningEffort = dropReasoningEffort,
        )
        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string().orEmpty()
                if (response.code != 200) {
                    Log.e("OpenAiRepository", "chatCompletion ${response.code}: $responseString")
                    return@use continuation.resume(ChatHttpResponse(null, response.code, responseString))
                }
                val chatCompletion =
                    json.decodeFromString(ChatCompletion.serializer(), responseString)
                Log.d("OpenAiRepository", "chatCompletion: $chatCompletion")
                continuation.resume(ChatHttpResponse(chatCompletion, 200, ""))
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error fetching chat completion", e)
            continuation.resume(ChatHttpResponse(null, null, ""))
        }
    }

    /**
     * Tool-calling entry point for the free-form task agent. OpenAI now splits its
     * models across two APIs: reasoning-first models (gpt-5.6*) reject function tools
     * on /v1/chat/completions and require /v1/responses, while older models only speak
     * chat/completions (and some reject the reasoning_effort parameter outright). The
     * split isn't discoverable up front, so it is learned from the server's own 400
     * guidance and persisted; known-Responses models route there directly. Gemini
     * (OpenAI-compat layer) and self-hosted servers always use chat/completions.
     */
    suspend fun chatWithTools(
        messages: List<ToolChatMessage>,
        tools: List<ToolDefinition>,
        gptActionInfo: ChatGPTActionInfo,
    ): ToolChatOutcome {
        val isOpenAi = gptActionInfo.actionType != GptActionType.Gemini &&
            gptActionInfo.actionType != GptActionType.SelfHosted
        val model = gptActionInfo.model
        if (isOpenAi && modelNeedsResponsesApi(model)) {
            return responsesWithTools(messages, tools, gptActionInfo)
        }
        // Learned-behavior sets are keyed by bare model name, so only consult/teach
        // them for api.openai.com — a self-hosted or Gemini model reusing an OpenAI
        // model name must keep its wire behavior untouched.
        val dropEffort = isOpenAi && model in modelsRejectingEffort
        val result = chatCompletionsWithTools(messages, tools, gptActionInfo, dropEffort)
        if (result is ToolHttpResult.Ok) return ToolChatOutcome.Success(result.completion)
        val error = result as ToolHttpResult.Error
        if (isOpenAi && error.body.contains("/v1/responses")) {
            // "Function tools with reasoning_effort are not supported for <model> in
            // /v1/chat/completions. To use function tools, use /v1/responses or set
            // reasoning_effort to 'none'." — happens even with the parameter omitted,
            // because the model's default effort counts too.
            rememberResponsesApiModel(model)
            return responsesWithTools(messages, tools, gptActionInfo)
        }
        if (!dropEffort && isEffortRejection(gptActionInfo, error.code, error.body)) {
            // Pre-reasoning models: "Unrecognized request argument supplied:
            // reasoning_effort". Retry without it; the model can't honor the
            // setting either way.
            modelsRejectingEffort.add(model)
            val retry = chatCompletionsWithTools(messages, tools, gptActionInfo, true)
            if (retry is ToolHttpResult.Ok) return ToolChatOutcome.Success(retry.completion)
            return ToolChatOutcome.Failure((retry as ToolHttpResult.Error).toUserMessage())
        }
        return ToolChatOutcome.Failure(error.toUserMessage())
    }

    private suspend fun chatCompletionsWithTools(
        messages: List<ToolChatMessage>,
        tools: List<ToolDefinition>,
        gptActionInfo: ChatGPTActionInfo,
        dropReasoningEffort: Boolean,
    ): ToolHttpResult = suspendCoroutine { continuation ->
        val isGemini = gptActionInfo.actionType == GptActionType.Gemini
        val isSelfHosted = gptActionInfo.actionType == GptActionType.SelfHosted
        val effort = config.ai.resolveReasoningEffort(gptActionInfo)
        val enableThinking = if (isSelfHosted) effort.toEnableThinking() else null
        val payload = ToolChatRequest(
            model = gptActionInfo.model,
            messages = messages,
            tools = tools,
            toolChoice = "auto",
            // Gemini's OpenAI-compat layer only understands low/medium/high, so
            // "off" falls back to the model default there rather than erroring.
            reasoningEffort = effort.toOpenAiEffort()
                ?.takeUnless { isGemini && effort == ReasoningEffort.Off }
                ?.takeUnless { dropReasoningEffort },
            enableThinking = enableThinking,
            chatTemplateKwargs = enableThinking?.let { ChatTemplateKwargs(it) },
        )
        val body = toolJson.encodeToString(payload)
        Log.d("OpenAiRepository", "chatWithTools request: $body")
        val url = if (isGemini) "$geminiOpenAiCompatUrl/chat/completions"
        else "${getServerUrl(gptActionInfo.actionType)}$completionPath"
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(mediaType))
            .header("Authorization", "Bearer ${if (isGemini) config.ai.geminiApiKey else apiKey}")
            .build()
        try {
            toolClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.code != 200) {
                    Log.e("OpenAiRepository", "chatWithTools ${response.code}: $responseBody")
                    return@use continuation.resume(ToolHttpResult.Error(response.code, responseBody))
                }
                Log.d("OpenAiRepository", "chatWithTools response: $responseBody")
                val parsed = toolJson.decodeFromString(ToolChatCompletion.serializer(), responseBody)
                continuation.resume(ToolHttpResult.Ok(parsed))
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error in chatWithTools", e)
            continuation.resume(ToolHttpResult.Error(-1, e.message ?: e.javaClass.simpleName))
        }
    }

    /**
     * Tool-calling via OpenAI's /v1/responses API — the only place reasoning-first
     * models accept function tools. The wire schema differs from chat/completions
     * (flat tool objects, typed input items) but the result is folded back into
     * [ToolChatCompletion] so the agent loop stays API-agnostic. Each assistant turn
     * keeps its raw output items ([ToolChatMessage.rawItems]) and replays them
     * verbatim on the next call, preserving item ids and any reasoning items the
     * server expects to see back.
     */
    private suspend fun responsesWithTools(
        messages: List<ToolChatMessage>,
        tools: List<ToolDefinition>,
        gptActionInfo: ChatGPTActionInfo,
    ): ToolChatOutcome = suspendCoroutine { continuation ->
        val effort = config.ai.resolveReasoningEffort(gptActionInfo)
        val payload = buildJsonObject {
            put("model", gptActionInfo.model)
            put("input", buildResponsesInput(messages))
            put("tools", buildJsonArray {
                tools.forEach { tool ->
                    add(buildJsonObject {
                        put("type", "function")
                        put("name", tool.function.name)
                        put("description", tool.function.description)
                        put("parameters", tool.function.parameters)
                    })
                }
            })
            put("tool_choice", "auto")
            effort.toOpenAiEffort()?.let {
                put("reasoning", buildJsonObject { put("effort", it) })
            }
        }
        val body = payload.toString()
        Log.d("OpenAiRepository", "responsesWithTools request: $body")
        val request = Request.Builder()
            .url("${getServerUrl(gptActionInfo.actionType)}$responsesPath")
            .post(body.toRequestBody(mediaType))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            toolClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.code != 200) {
                    Log.e("OpenAiRepository", "responsesWithTools ${response.code}: $responseBody")
                    return@use continuation.resume(
                        ToolChatOutcome.Failure(ToolHttpResult.Error(response.code, responseBody).toUserMessage())
                    )
                }
                Log.d("OpenAiRepository", "responsesWithTools response: $responseBody")
                continuation.resume(parseResponsesOutput(responseBody))
            }
        } catch (e: Exception) {
            Log.e("OpenAiRepository", "Error in responsesWithTools", e)
            continuation.resume(ToolChatOutcome.Failure(e.message ?: e.javaClass.simpleName))
        }
    }

    private fun buildResponsesInput(messages: List<ToolChatMessage>): JsonArray = buildJsonArray {
        messages.forEach { m ->
            when {
                // Assistant turn that came from a previous /v1/responses call:
                // replay its output items untouched.
                m.rawItems != null -> m.rawItems.forEach { add(it) }
                m.role == "tool" -> add(buildJsonObject {
                    put("type", "function_call_output")
                    put("call_id", m.toolCallId.orEmpty())
                    put("output", m.content.orEmpty())
                })
                else -> {
                    if (!m.content.isNullOrBlank() || m.toolCalls.isNullOrEmpty()) {
                        add(buildJsonObject {
                            put("role", m.role)
                            put("content", m.content.orEmpty())
                        })
                    }
                    // Assistant turns recorded by the chat-completions path: synthesize
                    // function_call items so a mid-session API switch keeps a transcript
                    // the Responses API can pair with its function_call_output items.
                    m.toolCalls?.forEach { call ->
                        add(buildJsonObject {
                            put("type", "function_call")
                            put("call_id", call.id)
                            put("name", call.function.name)
                            put("arguments", call.function.arguments)
                        })
                    }
                }
            }
        }
    }

    private fun parseResponsesOutput(responseBody: String): ToolChatOutcome {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val output = root["output"] as? JsonArray ?: JsonArray(emptyList())
        val toolCalls = output.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            if (obj["type"]?.jsonPrimitive?.contentOrNull != "function_call") return@mapNotNull null
            ToolCall(
                id = obj["call_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                function = FunctionCall(
                    name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    arguments = obj["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}",
                ),
            )
        }
        val text = output.filterIsInstance<JsonObject>()
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "message" }
            .flatMap { msg -> (msg["content"] as? JsonArray ?: emptyList()).filterIsInstance<JsonObject>() }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "output_text" }
            .mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
            .joinToString("")
        if (toolCalls.isEmpty() && text.isBlank()) {
            val status = root["status"]?.jsonPrimitive?.contentOrNull
            return ToolChatOutcome.Failure("empty response (status=$status)")
        }
        val message = ToolChatMessage(
            role = "assistant",
            content = text.ifBlank { null },
            toolCalls = toolCalls.ifEmpty { null },
            rawItems = output,
        )
        return ToolChatOutcome.Success(ToolChatCompletion(listOf(ToolChatChoice(message = message))))
    }

    /**
     * Models that must call /v1/responses for function tools. gpt-5.6* is known from
     * the server's own 400 guidance; anything else is learned the same way at runtime
     * and remembered in prefs so later sessions skip the doomed chat/completions call.
     */
    private fun modelNeedsResponsesApi(model: String): Boolean =
        model.startsWith("gpt-5.6") || config.ai.responsesApiModels.contains(model)

    private fun rememberResponsesApiModel(model: String) {
        config.ai.responsesApiModels = config.ai.responsesApiModels + model
    }

    private fun ToolHttpResult.Error.toUserMessage(): String =
        if (code == -1) body.take(300) else extractApiError(code, body)

    private fun extractApiError(code: Int, body: String): String {
        val apiMessage = try {
            json.parseToJsonElement(body).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }
        return apiMessage?.take(300)
            ?: "HTTP $code${if (body.isBlank()) "" else ": ${body.take(200)}"}"
    }

    /**
     * True when a 400 is OpenAI's "Unrecognized request argument supplied:
     * reasoning_effort" quirk (pre-reasoning models like gpt-4.1) — worth one retry
     * without the parameter. Scoped to api.openai.com action types so Gemini and
     * self-hosted wire behavior is never second-guessed.
     */
    private fun isEffortRejection(
        gptActionInfo: ChatGPTActionInfo,
        code: Int?,
        body: String,
    ): Boolean = gptActionInfo.actionType != GptActionType.Gemini &&
        gptActionInfo.actionType != GptActionType.SelfHosted &&
        code == 400 && body.contains("reasoning_effort")

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
                val text = responseData.candidates.firstOrNull()?.content?.parts
                    ?.filterNot { it.thought }
                    ?.joinToString("") { it.text }
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

    /**
     * One-shot config check for the settings screens: sends a trivial prompt with the
     * given engine's key/model and reports the concrete failure (HTTP status, parse,
     * network) instead of a bare null, so the user can verify a key or model name
     * right after entering it.
     */
    suspend fun testConnection(gptActionInfo: ChatGPTActionInfo): ApiResult<String> {
        if (gptActionInfo.actionType == GptActionType.Gemini) {
            return queryGemini(
                listOf(ChatMessage(TEST_PROMPT, ChatRole.User)),
                gptActionInfo,
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = createCompletionRequest(
                    listOf(ChatMessage(TEST_PROMPT, ChatRole.User)),
                    gptActionInfo,
                )
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val kind = when (response.code) {
                            401, 403 -> ApiResult.Kind.MissingKey
                            429 -> ApiResult.Kind.RateLimited
                            in 500..599 -> ApiResult.Kind.ServerError
                            else -> ApiResult.Kind.Unknown
                        }
                        return@use ApiResult.Failure(kind, "HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val text = json.decodeFromString(ChatCompletion.serializer(), body)
                        .choices.firstOrNull()?.message?.content
                    if (text.isNullOrBlank()) {
                        ApiResult.Failure(ApiResult.Kind.Parse, "Empty response")
                    } else {
                        ApiResult.Success(text)
                    }
                }
            } catch (e: Exception) {
                ApiResult.Failure(ApiResult.Kind.Network, e.message ?: "Network error", cause = e)
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
        // alt=sse gives one complete JSON object per SSE data line, so the stream can
        // be parsed with the regular serializers instead of scraping the pretty-printed
        // JSON array format.
        val apiUrl = if (isStream)
            "$apiPrefix$model:streamGenerateContent?alt=sse"
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
            // includeThoughts makes Gemini stream thought summaries, which the UI
            // uses purely as a "still thinking" signal (they are filtered from the
            // visible answer either way).
            generationConfig = config.ai.resolveReasoningEffort(gptActionInfo)
                .toGeminiThinkingBudget()
                ?.let { GenerationConfig(ThinkingConfig(thinkingBudget = it, includeThoughts = it > 0)) },
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
        dropReasoningEffort: Boolean = false,
    ): Request {
        val effort = config.ai.resolveReasoningEffort(gptActionInfo)
        // The enable_thinking pair is self-hosted-only: api.openai.com rejects
        // requests with parameters it doesn't know.
        val isSelfHosted = gptActionInfo.actionType == GptActionType.SelfHosted
        val enableThinking = if (isSelfHosted) effort.toEnableThinking() else null
        // Consult the learned quirk set (api.openai.com models only) so a model
        // that already 400'd on reasoning_effort never gets it again.
        val dropEffort = dropReasoningEffort || (!isSelfHosted &&
            gptActionInfo.actionType != GptActionType.Gemini &&
            gptActionInfo.model in modelsRejectingEffort)
        val chatRequest = ChatRequest(
            model = gptActionInfo.model,
            messages = messages,
            stream = stream,
            reasoningEffort = effort.toOpenAiEffort()?.takeUnless { dropEffort },
            enableThinking = enableThinking,
            chatTemplateKwargs = enableThinking?.let { ChatTemplateKwargs(it) },
        )
        return Request.Builder()
            .url("${getServerUrl(gptActionInfo.actionType)}$completionPath")
            .post(json.encodeToString(chatRequest).toRequestBody(mediaType))
            .header("Authorization", "Bearer $apiKey")
            .build()
    }

    private fun getServerUrl(gptActionType: GptActionType): String {
        return if (gptActionType == GptActionType.SelfHosted) {
            config.ai.gptUrl
        } else {
            "https://api.openai.com"
        }
    }

    // null = model default: leave the parameter out entirely.
    private fun ReasoningEffort.toOpenAiEffort(): String? = when (this) {
        ReasoningEffort.Default -> null
        ReasoningEffort.Off -> "none"
        ReasoningEffort.Low -> "low"
        ReasoningEffort.Medium -> "medium"
        ReasoningEffort.High -> "high"
    }

    private fun ReasoningEffort.toEnableThinking(): Boolean? = when (this) {
        ReasoningEffort.Default -> null
        ReasoningEffort.Off -> false
        else -> true
    }

    // thinkingBudget in tokens: 0 disables thinking; the tiers follow the
    // low/medium/high budgets Google uses for its own effort mapping.
    private fun ReasoningEffort.toGeminiThinkingBudget(): Int? = when (this) {
        ReasoningEffort.Default -> null
        ReasoningEffort.Off -> 0
        ReasoningEffort.Low -> 1024
        ReasoningEffort.Medium -> 8192
        ReasoningEffort.High -> 24576
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
        private const val responsesPath = "/v1/responses"

        // Models that 400 on the reasoning_effort argument itself (pre-reasoning
        // families like gpt-4.1). Learned per process; a wrong entry only costs
        // sending the user's effort setting to a model that ignores it anyway.
        private val modelsRejectingEffort =
            java.util.Collections.synchronizedSet(mutableSetOf<String>())

        // Google's OpenAI-compatible surface for the Gemini API: same request/response
        // schema as /v1/chat/completions (including tools), Bearer auth with the Gemini key.
        private const val geminiOpenAiCompatUrl =
            "https://generativelanguage.googleapis.com/v1beta/openai"
        private const val ttsPath = "/v1/audio/speech"
        private const val TEST_PROMPT = "Reply with one word: ok"
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
    // Reasoning controls. All default to null and are then omitted from the JSON,
    // so a "model default" request is byte-identical to what the app sent before
    // reasoning became configurable.
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    @SerialName("enable_thinking") val enableThinking: Boolean? = null,
    @SerialName("chat_template_kwargs") val chatTemplateKwargs: ChatTemplateKwargs? = null,
)

// Qwen3-style thinking switch for self-hosted servers: vLLM/SGLang/llama.cpp take
// it inside chat_template_kwargs, DashScope-like servers take enable_thinking at
// the top level. Both are sent so either kind of server picks it up.
@Serializable
data class ChatTemplateKwargs(
    @SerialName("enable_thinking") val enableThinking: Boolean,
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatDelta(
    val content: String? = null,
    // Separate reasoning channel used by DeepSeek/Qwen-style OpenAI-compatible
    // servers; llama.cpp and vLLM call it reasoning_content, some proxies just
    // reasoning. Only its presence matters — the text itself is never shown.
    @SerialName("reasoning_content")
    @JsonNames("reasoning")
    val reasoningContent: String? = null,
)

/**
 * Strips inline think blocks (Qwen3-style servers without a reasoning parser
 * stream them as regular content) and remembers whether the stream is currently
 * inside one. The tags arrive as whole tokens in practice, so no cross-chunk
 * tag reassembly is attempted.
 */
internal class ThinkTagFilter {
    var thinking = false
        private set
    private var answerStarted = false

    /** Returns the visible part of [chunk] — empty while inside a think block. */
    fun filter(chunk: String): String {
        var content = chunk
        if (!answerStarted && !thinking && content.trimStart().startsWith("<think>")) {
            thinking = true
            content = content.substringAfter("<think>")
        }
        if (thinking) {
            if (!content.contains("</think>")) return ""
            thinking = false
            content = content.substringAfter("</think>").trimStart('\n')
        }
        if (content.isNotEmpty()) answerStarted = true
        return content
    }
}

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
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    @SerialName("enable_thinking") val enableThinking: Boolean? = null,
    @SerialName("chat_template_kwargs") val chatTemplateKwargs: ChatTemplateKwargs? = null,
)

@Serializable
data class ToolChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    /**
     * Raw /v1/responses output items this assistant turn was parsed from, replayed
     * verbatim as input items on later turns. @Transient keeps it out of every JSON
     * encoding — chat/completions payloads must not carry it, and agent transcripts
     * are never persisted.
     */
    @Transient val rawItems: JsonArray? = null,
)

/** Result of a [OpenAiRepository.chatWithTools] turn, with the API's error text on failure. */
sealed class ToolChatOutcome {
    data class Success(val completion: ToolChatCompletion) : ToolChatOutcome()
    data class Failure(val message: String) : ToolChatOutcome()
}

private sealed class ToolHttpResult {
    class Ok(val completion: ToolChatCompletion) : ToolHttpResult()
    class Error(val code: Int, val body: String) : ToolHttpResult()
}

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
    /**
     * Opaque provider extras echoed back verbatim when the assistant turn is replayed.
     * Gemini's OpenAI-compatible endpoint returns its (required) thought_signature here
     * as {"google":{"thought_signature":...}} and rejects tool-call histories that drop
     * it. Null for OpenAI/self-hosted, and omitted on encode (explicitNulls=false).
     */
    @SerialName("extra_content") val extraContent: JsonElement? = null,
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
