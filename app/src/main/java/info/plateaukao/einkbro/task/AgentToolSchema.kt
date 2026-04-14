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
            name = "open_url",
            description = "Load a URL in an off-screen WebView. Call this before read_current_page " +
                "or get_page_links to fetch a page. Returns success/failure.",
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
            description = "Return the anchor links (text + href) on the currently loaded off-screen " +
                "page, or on the user's active tab if no page has been opened yet.",
            parameters = """
                {"type":"object","properties":{"source":{"type":"string","enum":["active_tab","background"],"description":"Which WebView to read from. Defaults to active_tab."}},"required":[]}
            """.trimIndent(),
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
        "You help the user execute multi-step browsing tasks by calling the provided tools. " +
        "Be decisive and frugal: fewer tool calls is better. Always end with the finish tool."
}
