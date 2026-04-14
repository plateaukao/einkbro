package info.plateaukao.einkbro.task.tasks

import info.plateaukao.einkbro.task.BrowserTask
import info.plateaukao.einkbro.task.BrowserTools
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

/**
 * Built-in template: from the user's *current tab* (assumed to be an article index
 * page like a news front page), extract all links, ask the LLM which ones look like
 * article links, then open each surviving link in the background and summarize it.
 *
 * The LLM is used at two points:
 *   1. to filter the raw link list into "article links" (arbitrary layouts supported)
 *   2. to produce a 2-sentence summary of each article's reader-mode text
 */
class ReadArticleListTask : BrowserTask {
    override val id = "read_article_list"
    override val displayName = "Read article list & summarize"

    override suspend fun run(tools: BrowserTools) {
        val language = tools.defaultLanguageName()
        tools.info("Reading links from: ${tools.activeTabTitle().ifBlank { tools.activeTabUrl() }}")
        val links = tools.activeTabLinks()
        if (links.isEmpty()) {
            tools.error("No links found on this page.")
            tools.finish("No links found on the current page.")
            return
        }
        tools.info("Extracted ${links.size} links. Filtering via LLM…")

        val articleLinks = filterArticleLinks(tools, links).take(MAX_ARTICLES)
        if (articleLinks.isEmpty()) {
            tools.error("LLM found no article links on this page.")
            tools.finish("No article links identified on the current page.")
            return
        }
        tools.info("Selected ${articleLinks.size} article links to summarize.")

        val results = StringBuilder()
        results.append("# Summaries from ").append(tools.activeTabTitle().ifBlank { tools.activeTabUrl() })
            .append("\n\n")

        articleLinks.forEachIndexed { index, link ->
            val idx = index + 1
            tools.info("Fetching $idx/${articleLinks.size}: ${link.text.take(80)}")
            val opened = tools.openUrlInBg(link.href)
            if (!opened) {
                tools.error("Failed to load: ${link.href}")
                results.append("## ${idx}. ${link.text}\n").append(link.href).append("\n\n")
                    .append("_(failed to load)_\n\n")
                return@forEachIndexed
            }
            val text = tools.currentBgPageText().trim()
            if (text.isBlank()) {
                results.append("## ${idx}. ${link.text}\n").append(link.href).append("\n\n")
                    .append("_(empty article body)_\n\n")
                return@forEachIndexed
            }
            val capped = if (text.length > MAX_CHARS_PER_ARTICLE) {
                text.substring(0, MAX_CHARS_PER_ARTICLE)
            } else text
            val summary = tools.askLlm(
                system = "You are a concise summarizer for a busy reader. Reply with 2 sentences at most. " +
                    "Reply in $language unless the article content requires otherwise. No preamble.",
                user = "Summarize this article:\n\n$capped",
            )?.trim().orEmpty()
            results.append("## ${idx}. ${link.text}\n")
                .append(link.href).append("\n\n")
                .append(if (summary.isNotBlank()) summary else "_(no summary)_")
                .append("\n\n")
        }
        tools.finish(results.toString())
    }

    private suspend fun filterArticleLinks(
        tools: BrowserTools,
        links: List<BrowserTools.Link>,
    ): List<BrowserTools.Link> {
        // Feed the LLM a numbered list and ask for a JSON array of indices.
        val numbered = links.mapIndexed { i, l -> "$i\t${l.text.take(120)}\t${l.href}" }
            .joinToString("\n")
        val prompt = """
            Below is a numbered list of links extracted from a news/article-index web page.
            Return a JSON array of the indices (numbers) of the links that point to individual
            news articles (not navigation, menus, login, category pages, author profiles,
            or pagination). Reply with ONLY the JSON array. No prose.

            Links:
            $numbered
        """.trimIndent()
        val response = tools.askLlm(
            system = "You output ONLY valid JSON arrays of integers. No code fences, no prose.",
            user = prompt,
        ) ?: return emptyList()
        val indices = parseIndexArray(response)
        return indices.mapNotNull { idx -> links.getOrNull(idx) }
    }

    private fun parseIndexArray(raw: String): List<Int> {
        val cleaned = raw
            .substringAfter("```json", raw)
            .substringAfter("```", raw)
            .substringBefore("```")
            .trim()
        val bracketStart = cleaned.indexOf('[')
        val bracketEnd = cleaned.lastIndexOf(']')
        if (bracketStart == -1 || bracketEnd <= bracketStart) return emptyList()
        val jsonSlice = cleaned.substring(bracketStart, bracketEnd + 1)
        return try {
            val parsed = Json.parseToJsonElement(jsonSlice)
            val array: JsonArray = parsed.jsonArray
            array.mapNotNull { el ->
                when (el) {
                    is JsonPrimitive -> el.intOrNull
                    else -> null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_ARTICLES = 10
        private const val MAX_CHARS_PER_ARTICLE = 12_000
    }
}
