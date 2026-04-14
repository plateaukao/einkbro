package info.plateaukao.einkbro.task

import info.plateaukao.einkbro.preference.ChatGPTActionInfo

/**
 * Tool façade over EinkBro primitives (off-screen WebView + OpenAI repository).
 *
 * Both the deterministic built-in tasks and the free-form LLM agent call into this
 * same surface so the set of capabilities is defined in exactly one place.
 *
 * All navigation methods operate on a dedicated off-screen WebView — the user's
 * active tab is never disturbed. Cookies are shared process-wide via Android's
 * CookieManager, so auth-walled sites work automatically.
 */
interface BrowserTools {

    // ── Navigation / content ────────────────────────────────────────────

    /** Loads [url] in the off-screen WebView and awaits page load. Returns false on timeout. */
    suspend fun openUrlInBg(url: String): Boolean

    /** Reader-mode text of the off-screen WebView's current page (empty if none loaded). */
    suspend fun currentBgPageText(): String

    /** Extracted `{text, href}` links from the off-screen WebView's current page. */
    suspend fun currentBgPageLinks(): List<Link>

    /** Reader-mode text of the user's *active* tab. */
    suspend fun activeTabText(): String

    /** Links extracted from the user's *active* tab. */
    suspend fun activeTabLinks(): List<Link>

    /** URL of the user's active tab. */
    fun activeTabUrl(): String

    /** Title of the user's active tab. */
    fun activeTabTitle(): String

    // ── LLM ─────────────────────────────────────────────────────────────

    /**
     * Non-streaming LLM call using the user's configured default OpenAI/Gemini action.
     * Returns the assistant's text content, or null on failure.
     */
    suspend fun askLlm(system: String, user: String): String?

    /** Returns the [ChatGPTActionInfo] configured for summarization (used by templates). */
    fun summarizeActionInfo(): ChatGPTActionInfo

    /**
     * Human-readable name of the app's current UI locale (e.g. "Chinese (Taiwan)", "English").
     * Templates inject this into LLM prompts so summaries default to the user's language
     * unless the caller specifies otherwise.
     */
    fun defaultLanguageName(): String

    // ── Progress reporting ──────────────────────────────────────────────

    /** Emit an informational step line into the task progress stream. */
    fun info(text: String)

    /** Emit a tool-call step line (used by the agent loop to surface what the LLM did). */
    fun tool(text: String)

    /** Emit an error line; task continues unless the caller throws. */
    fun error(text: String)

    /** Commit the final markdown result. The runner marks the task Done after this. */
    fun finish(markdown: String)

    /** Releases the off-screen WebView. Called by TaskRunner when done. */
    fun dispose()

    data class Link(val text: String, val href: String)
}
