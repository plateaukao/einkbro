package info.plateaukao.einkbro.task.tasks

import android.util.Log
import info.plateaukao.einkbro.data.remote.OpenAiRepository
import info.plateaukao.einkbro.data.remote.ToolCall
import info.plateaukao.einkbro.data.remote.ToolChatMessage
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.task.AgentToolSchema
import info.plateaukao.einkbro.task.BrowserTask
import info.plateaukao.einkbro.task.BrowserTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * LLM-driven free-form task. The user types a natural-language instruction; we run a
 * ReAct-style tool-use loop until the model calls `finish` or we hit the iteration cap.
 *
 * OpenAI / self-hosted OpenAI-compatible backends only — Gemini has a different function-
 * calling schema. The caller is responsible for gating this based on `useGeminiApi`.
 */
class FreeFormAgentTask(
    private val userPrompt: String,
    private val config: ConfigManager,
    private val openAi: OpenAiRepository,
) : BrowserTask {
    override val id = "free_form"
    override val displayName = "Custom task"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun run(tools: BrowserTools) {
        tools.info("Planning task: $userPrompt")

        val actionInfo = ChatGPTActionInfo(
            name = "agent",
            systemMessage = AgentToolSchema.SYSTEM_PROMPT,
            userMessage = "",
            actionType = if (config.ai.useCustomGptUrl) GptActionType.SelfHosted else GptActionType.OpenAi,
            model = if (config.ai.useCustomGptUrl) config.ai.alternativeModel else config.ai.gptModel,
        )

        val messages = mutableListOf(
            ToolChatMessage(role = "system", content = AgentToolSchema.SYSTEM_PROMPT),
            ToolChatMessage(role = "user", content = userPrompt),
        )

        var iter = 0
        var finished = false
        while (iter < MAX_ITERATIONS && !finished) {
            iter++
            val response = withContext(Dispatchers.IO) {
                openAi.chatWithTools(messages, AgentToolSchema.tools, actionInfo)
            }
            if (response == null) {
                tools.error("LLM call failed on turn $iter.")
                tools.finish("Task failed: could not reach the LLM.")
                return
            }
            val message = response.choices.firstOrNull()?.message
            if (message == null) {
                tools.error("Empty response on turn $iter.")
                tools.finish("Task failed: empty response from LLM.")
                return
            }

            val toolCalls = message.toolCalls.orEmpty()
            if (toolCalls.isEmpty()) {
                val text = message.content.orEmpty().ifBlank { "(no response)" }
                tools.finish(text)
                return
            }

            // Echo the assistant message with tool_calls into the conversation so the
            // model keeps context on subsequent turns.
            messages += ToolChatMessage(
                role = "assistant",
                content = message.content,
                toolCalls = toolCalls,
            )

            for (call in toolCalls) {
                tools.tool("${call.function.name}(${call.function.arguments.take(120)})")
                val result = dispatchTool(call, tools)
                messages += ToolChatMessage(
                    role = "tool",
                    toolCallId = call.id,
                    content = result,
                )
                if (call.function.name == "finish") {
                    finished = true
                    break
                }
            }
        }

        if (!finished) {
            tools.error("Agent loop exhausted after $MAX_ITERATIONS iterations.")
            tools.finish("(task did not complete within $MAX_ITERATIONS iterations)")
        }
    }

    private suspend fun dispatchTool(call: ToolCall, tools: BrowserTools): String {
        return try {
            val args: JsonObject = try {
                json.parseToJsonElement(call.function.arguments.ifBlank { "{}" }).jsonObject
            } catch (e: Exception) {
                return "error: invalid JSON arguments: ${e.message}"
            }
            when (call.function.name) {
                "open_url" -> {
                    val url = args["url"]?.jsonPrimitive?.contentOrNull
                        ?: return "error: missing url"
                    val ok = tools.openUrlInBg(url)
                    if (ok) "ok: loaded $url" else "error: failed to load $url"
                }
                "read_current_page" -> {
                    val text = tools.currentBgPageText().trim()
                    if (text.isBlank()) "error: no page loaded or page body is empty"
                    else text.take(MAX_TOOL_RESULT_CHARS)
                }
                "get_page_links" -> {
                    val source = args["source"]?.jsonPrimitive?.contentOrNull ?: "active_tab"
                    val links = if (source == "background") tools.currentBgPageLinks()
                    else tools.activeTabLinks()
                    val array: JsonArray = buildJsonArray {
                        links.take(MAX_LINKS_RETURNED).forEach { link ->
                            add(buildJsonObject {
                                put("text", JsonPrimitive(link.text))
                                put("href", JsonPrimitive(link.href))
                            })
                        }
                    }
                    array.toString()
                }
                "note" -> {
                    val text = args["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    tools.info(text)
                    "ok"
                }
                "finish" -> {
                    val summary = args["summary"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    tools.finish(summary)
                    "ok"
                }
                else -> "error: unknown tool ${call.function.name}"
            }
        } catch (e: Exception) {
            Log.e("FreeFormAgentTask", "Tool dispatch failed", e)
            "error: ${e.message}"
        }
    }

    companion object {
        private const val MAX_ITERATIONS = 12
        private const val MAX_TOOL_RESULT_CHARS = 8_000
        private const val MAX_LINKS_RETURNED = 50
    }
}
