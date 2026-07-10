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
                "translated/analyzed. Zero-cost lookup. Long documents are windowed — " +
                "pass offset to continue reading.",
            parameters = """
                {"type":"object","properties":{"offset":{"type":"integer","description":"Character offset to start from (default 0)"}},"required":[]}
            """.trimIndent(),
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
            name = "web_search",
            description = "Search the web using the user's configured search engine. Loads the " +
                "results page in the off-screen WebView and returns its links (text + href). " +
                "Follow up with read_current_page for result snippets, or open_url to visit a " +
                "result. Use whenever you need a page you don't have a URL for.",
            parameters = """
                {"type":"object","properties":{"query":{"type":"string","description":"Search terms"}},"required":["query"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "run_javascript",
            description = "Evaluate JavaScript and return its completion value (JSON-encoded). " +
                "target 'tab' (default) runs in the LIVE tab the user was viewing — DOM " +
                "changes are immediately visible to the user, so use it to TEST a selector " +
                "or element removal before persisting it with set_domain_javascript. " +
                "target 'background' runs in the off-screen WebView (call open_url first) — " +
                "use it to extract structured data (prices, dates, table rows) that " +
                "reader-mode text loses. Make the last statement an expression that yields " +
                "the value; use JSON.stringify for objects/arrays.",
            parameters = """
                {"type":"object","properties":{"code":{"type":"string","description":"JavaScript source to evaluate"},"target":{"type":"string","enum":["tab","background"],"description":"'tab' = live originating tab (default); 'background' = off-screen WebView"}},"required":["code"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "save_epub",
            description = "Fetch one or more pages and save their reader-mode content as " +
                "chapters of a new EPUB file in the device's Downloads folder. Use when the " +
                "user wants to keep articles for offline/e-reader reading (e.g. 'save these " +
                "three articles as a book'). Each chapter loads in the off-screen WebView, " +
                "so allow a few seconds per page. Returns the saved file location.",
            parameters = """
                {"type":"object","properties":{"book_title":{"type":"string","description":"EPUB book title, also used as the file name"},"chapters":{"type":"array","description":"Pages to save, in reading order","items":{"type":"object","properties":{"url":{"type":"string","description":"Absolute http(s) URL of the page"},"title":{"type":"string","description":"Chapter title; defaults to the page title"}},"required":["url"]}}},"required":["book_title","chapters"]}
            """.trimIndent(),
        ),
        toolDef(
            name = "read_current_page",
            description = "Return the reader-mode text of the currently loaded off-screen page. " +
                "Call open_url first. Long documents are windowed — pass offset to continue " +
                "reading.",
            parameters = """
                {"type":"object","properties":{"offset":{"type":"integer","description":"Character offset to start from (default 0)"}},"required":[]}
            """.trimIndent(),
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
            description = "Return the rendered `document.body.innerHTML` of the page the user " +
                "was viewing — the live DOM AFTER JavaScript ran. Use it to inspect elements " +
                "that are injected at runtime (popups, late-loading ads, cookie banners). For " +
                "the original served markup use read_page_source instead. Long documents are " +
                "windowed: page through with offset, or jump straight to an element with " +
                "search (e.g. visible text the user quoted, or a class/id fragment).",
            parameters = """
                {"type":"object","properties":{"offset":{"type":"integer","description":"Character offset to start from (default 0)"},"search":{"type":"string","description":"Jump to the first occurrence of this string at/after offset (case-insensitive)"}},"required":[]}
            """.trimIndent(),
        ),
        toolDef(
            name = "read_page_source",
            description = "Download and return the page's REAL HTML source — the exact markup " +
                "the server sent, before any JavaScript ran (view-source equivalent, fetched " +
                "with the browser's cookies and user agent). Defaults to the originating page; " +
                "pass url for any other page. This is the ground truth for writing precise CSS " +
                "selectors and DOM patches: ad containers, class/id names, hidden elements, " +
                "inline scripts. Long documents are windowed: page through with offset, or " +
                "jump straight to an element with search.",
            parameters = """
                {"type":"object","properties":{"url":{"type":"string","description":"Absolute http(s) URL; omit to read the originating page"},"offset":{"type":"integer","description":"Character offset to start from (default 0)"},"search":{"type":"string","description":"Jump to the first occurrence of this string at/after offset (case-insensitive)"}},"required":[]}
            """.trimIndent(),
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
        "looking at. Use open_url only when you need to navigate somewhere new, and " +
        "web_search when you need a page you don't have a URL for. Never ask " +
        "the user to paste content; fetch it via tools. When the user asks you to read, " +
        "speak, or narrate something out loud, call the speak tool (plain-text form, no " +
        "markdown). When the user wants articles kept for offline or e-reader reading, " +
        "call save_epub with the URLs as chapters. Be decisive and frugal: fewer tool " +
        "calls is better.\n\n" +
        "DOM-PATCH WORKFLOW: When the user asks you to remove a banner, popup, modal, ad, " +
        "cookie notice, or any other element from a site (or show/hide specific elements), " +
        "follow this sequence: " +
        "(1) locate the offending element in the page's HTML. read_page_source gives the " +
        "REAL served markup (best for stable selectors: ad containers, class/id names); " +
        "read_initial_html gives the rendered DOM after JavaScript (best for elements " +
        "injected at runtime). Use the search parameter with text the user quoted or a " +
        "likely class/id fragment (e.g. 'ad', 'banner', 'cookie') instead of paging " +
        "blindly through offsets; " +
        "(2) call get_domain_javascript to see what's already saved for this host; " +
        "(3) compose a self-contained JS snippet that finds and removes/hides the element. " +
        "Prefer querySelectorAll plus textContent matching over fragile DOM paths. Always " +
        "wrap in try/catch. For elements that appear after page load (popups, lazy " +
        "modals), use a MutationObserver or a short setInterval re-check. " +
        "(4) TEST the snippet first: call run_javascript with target 'tab' to run it on " +
        "the live page the user is viewing, returning a before/after element count so " +
        "you can verify it actually matched and removed something (the user sees the " +
        "element vanish immediately). Wrap the test snippet in try/catch and return the " +
        "caught error message as a string — a thrown error otherwise looks identical to " +
        "zero matches. If it matched nothing, refine the selector and re-test instead " +
        "of persisting a broken rule. " +
        "(5) once verified, call set_domain_javascript with the FULL combined script — " +
        "your new code concatenated with whatever get_domain_javascript returned, so you " +
        "don't clobber earlier rules. EinkBro will auto-run this after every page load " +
        "on the host. Use set_domain_css instead if a CSS rule (e.g. `display:none`) " +
        "suffices — it's lighter and harder to break. Tell the user in finish what " +
        "selector/text you matched on so they can revert via Site Settings if it " +
        "misfires.\n\n" +
        "Always end the task by calling the finish tool with your final answer as markdown."
}
