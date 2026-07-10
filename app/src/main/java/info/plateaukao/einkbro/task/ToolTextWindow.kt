package info.plateaukao.einkbro.task

/**
 * Windows large agent-tool results instead of blindly truncating them. The returned
 * window is prefixed with a `[chars a..b of N]` header so the model knows where it is
 * in the document and how to page for more.
 */
object ToolTextWindow {

    const val DEFAULT_SEARCH_PRE_CONTEXT_CHARS = 200

    /**
     * Returns the window of [content] starting at [offset], capped to [maxChars].
     * When [search] is given, jumps to its first occurrence at/after [offset]
     * (case-insensitive) and starts [searchPreContextChars] before the match so the
     * model sees the enclosing markup; returns a `not found` message if absent.
     */
    fun window(
        content: String,
        maxChars: Int,
        offset: Int = 0,
        search: String? = null,
        searchPreContextChars: Int = DEFAULT_SEARCH_PRE_CONTEXT_CHARS,
    ): String {
        val total = content.length
        var start = offset.coerceIn(0, total)
        if (!search.isNullOrBlank()) {
            val idx = content.indexOf(search, start, ignoreCase = true)
            if (idx < 0) {
                return "not found: \"$search\" (searched from offset $start; document is $total chars)"
            }
            // Cap pre-context to half the window so the match itself always fits.
            val preContext = searchPreContextChars.coerceAtMost(maxChars / 2)
            start = (idx - preContext).coerceAtLeast(0)
        }
        val end = (start + maxChars).coerceAtMost(total)
        val more = if (end < total) "; continue with offset=$end" else "; end of document"
        return "[chars $start..$end of $total$more]\n" + content.substring(start, end)
    }
}
