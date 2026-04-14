package info.plateaukao.einkbro.task

import info.plateaukao.einkbro.data.remote.FunctionDef
import info.plateaukao.einkbro.data.remote.ToolDefinition
import kotlinx.serialization.json.Json

/**
 * JSON-schema tool definitions exposed to the free-form LLM agent. Keep this set small —
 * each extra tool is another way for the model to misfire, and longer descriptions burn
 * tokens on every turn.
 */
object AgentToolSchema {

    private val json = Json { ignoreUnknownKeys = true }

    val tools: List<ToolDefinition> = listOf(
        toolDef(
            name = "get_initial_page_links",
            description = "Return the anchor links (text + href) from the page the user was " +
                "viewing when they triggered this task. Use this FIRST when the user asks you " +
                "to do something with 'this page', 'the articles', 'top stories', etc. Zero-cost " +
                "lookup — no network fetch.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "read_initial_page",
            description = "Return the reader-mode text of the page the user was viewing when " +
                "they triggered this task. Use when the user wants the current page summarized/" +
                "translated/analyzed. Zero-cost lookup.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "open_url",
            description = "Load a URL in an off-screen WebView. Call this before read_current_page " +
                "or get_page_links to fetch a new page (not the originating one — use " +
                "read_initial_page for that). Returns success/failure.",
            parameters = """
                {"type":"object","properties":{"url":{"type":"string","description":"Absolute http(s) URL"}},"required":["url"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "read_current_page",
            description = "Return the reader-mode text of the currently loaded off-screen page. " +
                "Call open_url first.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "get_page_links",
            description = "Return the anchor links (text + href) on the currently loaded " +
                "off-screen page. Call open_url first. For the ORIGINATING page the user was " +
                "viewing, use get_initial_page_links instead.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "note",
            description = "Append a short progress note visible to the user. Use sparingly to " +
                "explain multi-step reasoning.",
            parameters = """
                {"type":"object","properties":{"text":{"type":"string"}},"required":["text"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "speak",
            description = "Enqueue text into the text-to-speech queue. Playback starts " +
                "immediately if nothing is currently being read, otherwise the text is " +
                "appended to the queue and played after the current utterance. Call this " +
                "whenever the user asks you to read, speak, narrate, or say something out " +
                "loud — pass only the clean spoken-form text (no markdown, headers, or " +
                "URLs). You can call it multiple times to stream chunks as they become " +
                "ready. Calling speak does NOT end the task — still call finish afterwards.",
            parameters = """
                {"type":"object","properties":{"text":{"type":"string","description":"Plain text to speak"},"title":{"type":"string","description":"Optional short label shown in the TTS notification"}},"required":["text"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "finish",
            description = "Call this exactly once when the task is complete. Provide the final " +
                "user-facing result as markdown. After calling this no more tools will run.",
            parameters = """
                {"type":"object","properties":{"summary":{"type":"string","description":"Markdown result shown to the user"}},"required":["summary"]}
            """.trimIndent(),
        ),
    )

    private fun toolDef(name: String, description: String, parameters: String): ToolDefinition =
        ToolDefinition(
            function = FunctionDef(
                name = name,
                description = description,
                parameters = json.parseToJsonElement(parameters),
            ),
        )

    const val SYSTEM_PROMPT =
        "You are EinkBro, an automation agent running inside an Android web browser. " +
        "You help the user with multi-step browsing tasks by calling the provided tools. " +
        "The user triggered this task while viewing a specific page — call " +
        "get_initial_page_links or read_initial_page FIRST to inspect what they were " +
        "looking at. Use open_url only when you need to navigate somewhere new. Never ask " +
        "the user to paste content; fetch it via tools. When the user asks you to read, " +
        "speak, or narrate something out loud, call the speak tool (plain-text form, no " +
        "markdown). Be decisive and frugal: fewer tool calls is better. Always end the " +
        "task by calling the finish tool with your final answer as markdown."
}
