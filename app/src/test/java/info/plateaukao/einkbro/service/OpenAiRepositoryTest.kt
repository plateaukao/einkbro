package info.plateaukao.einkbro.service

import android.content.SharedPreferences
import android.util.Log
import info.plateaukao.einkbro.data.remote.ApiResult
import info.plateaukao.einkbro.data.remote.ChatMessage
import info.plateaukao.einkbro.data.remote.ChatRole
import info.plateaukao.einkbro.data.remote.FunctionDef
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.data.remote.ToolChatMessage
import info.plateaukao.einkbro.data.remote.ToolDefinition
import info.plateaukao.einkbro.preference.AiConfig
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * JVM unit tests for [OpenAiRepository] using OkHttp MockWebServer.
 *
 * The repository resolves its base URL from `config.ai.gptUrl` when the action type
 * is [GptActionType.SelfHosted], which lets the tests point it at a local
 * MockWebServer without touching production code. The Gemini and TTS endpoints are
 * hardcoded to remote hosts, so only their non-network early-return paths are tested.
 */
class OpenAiRepositoryTest {

    private val server = MockWebServer()

    private val selfHostedAction = ChatGPTActionInfo(
        actionType = GptActionType.SelfHosted,
        model = "test-model",
    )

    private val userMessage = ChatMessage(content = "Hello", role = ChatRole.User)

    @Before
    fun setUp() {
        // android.util.Log is not available in JVM unit tests
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        stopKoin()
        unmockkStatic(Log::class)
    }

    /**
     * Starts Koin with a ConfigManager whose AiConfig is backed by a mocked
     * SharedPreferences, then constructs the repository (it reads the API key
     * at construction time).
     */
    private fun createRepository(
        apiKey: String = "test-key",
        geminiApiKey: String = "",
    ): OpenAiRepository {
        val baseUrl = server.url("/").toString().removeSuffix("/")
        val sp: SharedPreferences = mockk(relaxed = true) {
            every { getString(any(), any()) } answers { secondArg() }
            every { getString(AiConfig.K_GPT_API_KEY, any()) } returns apiKey
            every { getString(AiConfig.K_GEMINI_API_KEY, any()) } returns geminiApiKey
            every { getString("sp_gpt_server_url", any()) } returns baseUrl
        }
        val configManager: ConfigManager = mockk {
            every { ai } returns AiConfig(sp)
        }
        startKoin {
            modules(module { single { configManager } })
        }
        return OpenAiRepository()
    }

    // ── chatCompletion ────────────────────────────────────────────────────

    @Test
    fun `chatCompletion parses successful response`() = runBlocking {
        val repository = createRepository()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "chatcmpl-123",
                  "created": 1700000000,
                  "model": "test-model",
                  "choices": [
                    {"index": 0, "message": {"role": "assistant", "content": "Hi there"}}
                  ],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 2, "total_tokens": 7}
                }
                """.trimIndent()
            )
        )

        val completion = repository.chatCompletion(listOf(userMessage), selfHostedAction)

        assertNotNull(completion)
        assertEquals("chatcmpl-123", completion!!.id)
        assertEquals("Hi there", completion.choices.first().message.content)
        assertEquals(ChatRole.Assistant, completion.choices.first().message.role)
        assertEquals(7, completion.usage.totalTokens)
    }

    @Test
    fun `chatCompletion sends correct request path headers and body`() = runBlocking {
        val repository = createRepository(apiKey = "secret-key")
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"1","created":0,"model":"m","choices":[{"index":0,"message":{"role":"assistant","content":"ok"}}]}"""
            )
        )

        repository.chatCompletion(listOf(userMessage), selfHostedAction)

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("POST", recorded.method)
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer secret-key", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"model\":\"test-model\""))
        assertTrue(body.contains("\"content\":\"Hello\""))
        assertTrue(body.contains("\"role\":\"user\""))
        // stream=false is the default and the Json instance does not encode defaults,
        // so a non-streaming request must not contain stream:true
        assertTrue(!body.contains("\"stream\":true"))
    }

    @Test
    fun `chatCompletion returns null on http 500`() = runBlocking {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(500).setBody("internal error"))

        val completion = repository.chatCompletion(listOf(userMessage), selfHostedAction)

        assertNull(completion)
    }

    @Test
    fun `chatCompletion returns null on http 404`() = runBlocking {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val completion = repository.chatCompletion(listOf(userMessage), selfHostedAction)

        assertNull(completion)
    }

    @Test
    fun `chatCompletion returns null on malformed json`() = runBlocking {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not valid json"))

        val completion = repository.chatCompletion(listOf(userMessage), selfHostedAction)

        assertNull(completion)
    }

    @Test
    fun `chatCompletion ignores unknown json fields`() = runBlocking {
        val repository = createRepository()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "chatcmpl-1",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "system_fingerprint": "fp_abc",
                  "choices": [
                    {"index": 0, "message": {"role": "assistant", "content": "ok"}, "logprobs": null}
                  ]
                }
                """.trimIndent()
            )
        )

        val completion = repository.chatCompletion(listOf(userMessage), selfHostedAction)

        assertNotNull(completion)
        assertEquals("ok", completion!!.choices.first().message.content)
    }

    // ── chatWithTools ─────────────────────────────────────────────────────

    private val weatherTool = ToolDefinition(
        function = FunctionDef(
            name = "get_weather",
            description = "Get weather for a city",
            parameters = Json.parseToJsonElement("""{"type":"object","properties":{}}"""),
        )
    )

    @Test
    fun `chatWithTools parses tool call response`() = runBlocking {
        val repository = createRepository()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {"id": "call_1", "type": "function",
                           "function": {"name": "get_weather", "arguments": "{\"city\":\"Taipei\"}"}}
                        ]
                      },
                      "finish_reason": "tool_calls"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.chatWithTools(
            messages = listOf(ToolChatMessage(role = "user", content = "weather?")),
            tools = listOf(weatherTool),
            gptActionInfo = selfHostedAction,
        )

        assertNotNull(result)
        val message = result!!.choices.first().message
        assertNull(message.content)
        assertEquals("tool_calls", result.choices.first().finishReason)
        val toolCall = message.toolCalls!!.first()
        assertEquals("call_1", toolCall.id)
        assertEquals("get_weather", toolCall.function.name)
        assertTrue(toolCall.function.arguments.contains("Taipei"))
    }

    @Test
    fun `chatWithTools omits null fields in request body`() = runBlocking {
        val repository = createRepository()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"choices":[{"index":0,"message":{"role":"assistant","content":"hi"}}]}"""
            )
        )

        repository.chatWithTools(
            messages = listOf(ToolChatMessage(role = "user", content = "hello")),
            tools = listOf(weatherTool),
            gptActionInfo = selfHostedAction,
        )

        val body = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()
        // explicitNulls = false: user messages must not carry tool fields
        assertTrue(!body.contains("tool_call_id"))
        assertTrue(!body.contains("\"tool_calls\""))
        assertTrue(body.contains("\"tool_choice\":\"auto\""))
        assertTrue(body.contains("\"get_weather\""))
    }

    @Test
    fun `chatWithTools returns null on http error`() = runBlocking {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"bad request"}"""))

        val result = repository.chatWithTools(
            messages = listOf(ToolChatMessage(role = "user", content = "hello")),
            tools = listOf(weatherTool),
            gptActionInfo = selfHostedAction,
        )

        assertNull(result)
    }

    @Test
    fun `chatWithTools returns null on malformed json`() = runBlocking {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(200).setBody("oops"))

        val result = repository.chatWithTools(
            messages = listOf(ToolChatMessage(role = "user", content = "hello")),
            tools = listOf(weatherTool),
            gptActionInfo = selfHostedAction,
        )

        assertNull(result)
    }

    // ── chatStream (SSE) ──────────────────────────────────────────────────

    private fun sseEvent(content: String): String =
        "data: {\"id\":\"1\",\"created\":0,\"model\":\"m\"," +
            "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"$content\"}}]}\n\n"

    @Test
    fun `chatStream appends streamed chunks and signals done`() {
        val repository = createRepository()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseEvent("Hello") + sseEvent(" world") + "data: [DONE]\n\n")
        )

        val chunks = Collections.synchronizedList(mutableListOf<String>())
        val doneLatch = CountDownLatch(1)
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = selfHostedAction,
            appendResponseAction = { chunks.add(it) },
            doneAction = { doneLatch.countDown() },
            failureAction = { failure.set(it) },
        )

        assertTrue("stream did not complete", doneLatch.await(5, TimeUnit.SECONDS))
        assertNull(failure.get())
        assertEquals(listOf("Hello", " world"), chunks)
    }

    @Test
    fun `chatStream reports parse failure on malformed chunk`() {
        val repository = createRepository()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: this is not json\n\n")
        )

        val failureLatch = CountDownLatch(1)
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = selfHostedAction,
            appendResponseAction = {},
            failureAction = {
                failure.set(it)
                failureLatch.countDown()
            },
        )

        assertTrue("failure not reported", failureLatch.await(5, TimeUnit.SECONDS))
        assertEquals(ApiResult.Kind.Parse, failure.get()!!.kind)
    }

    @Test
    fun `chatStream reports rate limit with retry after on http 429`() {
        val repository = createRepository()
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setBody("rate limited")
        )

        val failureLatch = CountDownLatch(1)
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = selfHostedAction,
            appendResponseAction = {},
            failureAction = {
                failure.set(it)
                failureLatch.countDown()
            },
        )

        assertTrue("failure not reported", failureLatch.await(5, TimeUnit.SECONDS))
        assertEquals(ApiResult.Kind.RateLimited, failure.get()!!.kind)
        assertEquals(30L, failure.get()!!.retryAfterSeconds)
    }

    @Test
    fun `chatStream reports missing key on http 401`() {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        val failureLatch = CountDownLatch(1)
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = selfHostedAction,
            appendResponseAction = {},
            failureAction = {
                failure.set(it)
                failureLatch.countDown()
            },
        )

        assertTrue("failure not reported", failureLatch.await(5, TimeUnit.SECONDS))
        assertEquals(ApiResult.Kind.MissingKey, failure.get()!!.kind)
    }

    @Test
    fun `chatStream reports server error on http 500`() {
        val repository = createRepository()
        server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))

        val failureLatch = CountDownLatch(1)
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = selfHostedAction,
            appendResponseAction = {},
            failureAction = {
                failure.set(it)
                failureLatch.countDown()
            },
        )

        assertTrue("failure not reported", failureLatch.await(5, TimeUnit.SECONDS))
        assertEquals(ApiResult.Kind.ServerError, failure.get()!!.kind)
    }

    @Test
    fun `chatStream fails fast when openai key is missing`() {
        val repository = createRepository(apiKey = "")
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = ChatGPTActionInfo(actionType = GptActionType.OpenAi, model = "gpt"),
            appendResponseAction = {},
            failureAction = { failure.set(it) },
        )

        assertNotNull(failure.get())
        assertEquals(ApiResult.Kind.MissingKey, failure.get()!!.kind)
        // no request should have been made
        assertEquals(0, server.requestCount)
    }

    // ── Gemini early-return paths (endpoint is hardcoded; only the
    //    non-network key checks are reachable from a unit test) ───────────

    @Test
    fun `queryGemini fails fast when gemini key is missing`() = runBlocking {
        val repository = createRepository(geminiApiKey = "")

        val result = repository.queryGemini(
            listOf(userMessage),
            ChatGPTActionInfo(actionType = GptActionType.Gemini, model = "gemini-pro"),
        )

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiResult.Kind.MissingKey, (result as ApiResult.Failure).kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `chatStream with gemini action fails fast when gemini key is missing`() {
        val repository = createRepository(geminiApiKey = "")
        val failure = AtomicReference<ApiResult.Failure?>()

        repository.chatStream(
            messages = listOf(userMessage),
            gptActionInfo = ChatGPTActionInfo(actionType = GptActionType.Gemini, model = "gemini-pro"),
            appendResponseAction = {},
            failureAction = { failure.set(it) },
        )

        assertNotNull(failure.get())
        assertEquals(ApiResult.Kind.MissingKey, failure.get()!!.kind)
        assertEquals(0, server.requestCount)
    }
}
