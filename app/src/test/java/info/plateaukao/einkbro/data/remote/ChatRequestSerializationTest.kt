package info.plateaukao.einkbro.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down the wire format of the reasoning fields: a request without an
 * explicit reasoning choice must not mention reasoning at all (some servers
 * reject unknown or null parameters), while an explicit choice must produce
 * the exact keys the various backends understand.
 */
class ChatRequestSerializationTest {

    // Same configuration as OpenAiRepository's instances.
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalSerializationApi::class)
    private val toolJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val messages = listOf(ChatMessage("hi", ChatRole.User))

    @Test
    fun `default ChatRequest omits every reasoning key`() {
        val encoded = json.encodeToString(ChatRequest("m", messages))
        assertFalse(encoded.contains("reasoning"))
        assertFalse(encoded.contains("enable_thinking"))
        assertFalse(encoded.contains("chat_template_kwargs"))
    }

    @Test
    fun `explicit effort produces the OpenAI-style key`() {
        val encoded = json.encodeToString(
            ChatRequest("m", messages, reasoningEffort = "none")
        )
        assertTrue(encoded.contains(""""reasoning_effort":"none""""))
        assertFalse(encoded.contains("enable_thinking"))
    }

    @Test
    fun `self-hosted thinking switch encodes both forms`() {
        val encoded = json.encodeToString(
            ChatRequest(
                "m", messages,
                reasoningEffort = "none",
                enableThinking = false,
                chatTemplateKwargs = ChatTemplateKwargs(false),
            )
        )
        assertTrue(encoded.contains(""""enable_thinking":false"""))
        assertTrue(encoded.contains(""""chat_template_kwargs":{"enable_thinking":false}"""))
    }

    @Test
    fun `tool request omits null reasoning fields despite encodeDefaults`() {
        val encoded = toolJson.encodeToString(
            ToolChatRequest("m", listOf(ToolChatMessage(role = "user", content = "hi")))
        )
        assertFalse(encoded.contains("reasoning"))
        assertFalse(encoded.contains("enable_thinking"))
    }

    @Test
    fun `tool request carries reasoning fields when set`() {
        val encoded = toolJson.encodeToString(
            ToolChatRequest(
                "m",
                listOf(ToolChatMessage(role = "user", content = "hi")),
                reasoningEffort = "high",
                enableThinking = true,
                chatTemplateKwargs = ChatTemplateKwargs(true),
            )
        )
        assertTrue(encoded.contains(""""reasoning_effort":"high""""))
        assertTrue(encoded.contains(""""chat_template_kwargs":{"enable_thinking":true}"""))
    }

    @Test
    fun `stream flag still round trips`() {
        val encoded = json.encodeToString(ChatRequest("m", messages, stream = true))
        assertTrue(encoded.contains(""""stream":true"""))
        val decoded = json.decodeFromString(ChatRequest.serializer(), encoded)
        assertEquals(true, decoded.stream)
    }
}
