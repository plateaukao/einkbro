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
            name = "read_initial_html",
            description = "Return the raw `document.body.innerHTML` of the page the user was " +
                "viewing when they triggered this task. Use this when the user describes a DOM " +
                "element to act on (banner, popup, ad, modal) — read_initial_page returns " +
                "reader-mode text which strips those out. Result is truncated; you may need " +
                "to scan strategically. Zero-cost lookup.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "get_domain_javascript",
            description = "Return the postLoadJavascript currently saved for the originating " +
                "page's host. EinkBro auto-injects this script after every page load on that " +
                "domain. Call this BEFORE set_domain_javascript so you can preserve any " +
                "existing rules and append your new one.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "get_domain_css",
            description = "Return the customCss currently saved for the originating page's " +
                "host. EinkBro auto-injects this stylesheet on every page load on that domain. " +
                "Call before set_domain_css to preserve existing rules.",
            parameters = """{"type":"object","properties":{},"required":[]}""",
        ),
        toolDef(
            name = "set_domain_javascript",
            description = "Replace the postLoadJavascript saved for the originating page's " +
                "host. Persists across reloads and reboots. CAUTION: this is a REPLACE, not an " +
                "append — call get_domain_javascript first and include any pre-existing code. " +
                "Pass an empty string to clear.",
            parameters = """
                {"type":"object","properties":{"code":{"type":"string","description":"Full JS body. Wrap in try/catch; for late-rendering popups use MutationObserver."}},"required":["code"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "set_domain_css",
            description = "Replace the customCss saved for the originating page's host. " +
                "REPLACE not append — call get_domain_css first and include any pre-existing " +
                "rules. Pass an empty string to clear.",
            parameters = """
                {"type":"object","properties":{"code":{"type":"string","description":"Full CSS body."}},"required":["code"]}
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
        "markdown). Be decisive and frugal: fewer tool calls is better.\n\n" +
        "DOM-PATCH WORKFLOW: When the user asks you to remove a banner, popup, modal, ad, " +
        "cookie notice, or any other element from a site, follow this sequence: " +
        "(1) call read_initial_html and locate the offending element by the text the user " +
        "named or a stable structural cue; " +
        "(2) call get_domain_javascript to see what's already saved for this host; " +
        "(3) compose a self-contained JS snippet that finds and removes/hides the element. " +
        "Prefer querySelectorAll plus textContent matching over fragile DOM paths. Always " +
        "wrap in try/catch. For elements that appear after page load (popups, lazy " +
        "modals), use a MutationObserver or a short setInterval re-check. " +
        "(4) call set_domain_javascript with the FULL combined script — your new code " +
        "concatenated with whatever get_domain_javascript returned, so you don't clobber " +
        "earlier rules. EinkBro will auto-run this after every page load on the host. " +
        "Use set_domain_css instead if a CSS rule (e.g. `display:none`) suffices — it's " +
        "lighter and harder to break. Tell the user in finish what selector/text you " +
        "matched on so they can revert via Site Settings if it misfires.\n\n" +
        "Always end the task by calling the finish tool with your final answer as markdown."
}
