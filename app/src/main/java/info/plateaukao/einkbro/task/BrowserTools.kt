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
/** Immutable snapshot of the page the user was viewing when they triggered an agent task.
 *  Captured once at entry and exposed through [BrowserTools] so the agent can inspect the
 *  originating tab even after the chat tab has replaced it as the active tab. */
data class InitialPageSnapshot(
    val url: String,
    val title: String,
    val text: String,
    val links: List<BrowserTools.Link>,
)

interface BrowserTools {

    // ── Originating page snapshot ──────────────────────────────────────
    //
    // In agent-chat mode the chat tab itself becomes the active tab, so the methods
    // below replace `activeTab*` for reading "what the user was looking at". All
    // snapshot lookups are zero-cost — no WebView access.

    fun initialPageUrl(): String
    fun initialPageTitle(): String
    fun initialPageText(): String
    fun initialPageLinks(): List<Link>

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

    /**
     * Speak [text] via the shared TTS manager. Fire-and-forget — if no article is
     * currently being read, TTS starts immediately; otherwise [text] is appended to
     * the TTS queue and played after the current utterance finishes. [title] drives
     * the TTS notification and has no effect on spoken output.
     */
    fun speak(text: String, title: String = "")

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
