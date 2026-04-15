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
 * article links, then open each surviving link in the background and hand its full
 * reader-mode text to the TTS queue — playing each article one after another.
 */
class ReadArticleListTask : BrowserTask {
    override val id = "read_article_list"
    override val displayName = "News anchor"

    override suspend fun run(tools: BrowserTools) {
        val pageTitle = tools.activeTabTitle().ifBlank { tools.activeTabUrl() }
        tools.info("Reading links from: $pageTitle")

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
        tools.info("Selected ${articleLinks.size} article links to read.")

        // Serial flow: page loads share a single off-screen WebView, and the TTS
        // engine queues articles internally — so fetching article N+1 while N is
        // still being spoken is fine and keeps the speaker always supplied.
        //
        // Full article text is held locally and streamed into the TTS queue; only
        // a compact index (title + URL + status) lands in the finish markdown, so
        // if the result is ever fed back to an LLM the body text doesn't become
        // tokens.
        val articleBodies = ArrayList<String>(articleLinks.size)
        val indexMd = StringBuilder()
        indexMd.append("# ").append(pageTitle).append("\n\n")

        articleLinks.forEachIndexed { index, link ->
            val idx = index + 1
            tools.info("Fetching $idx/${articleLinks.size}: ${link.text.take(80)}")
            val opened = tools.openUrlInBg(link.href)
            val text = if (opened) tools.currentBgPageText().trim() else ""
            indexMd.append("## ").append(idx).append(". ").append(link.text).append("\n")
            indexMd.append(link.href).append("\n")
            if (text.isBlank()) {
                tools.error("Failed to load or empty: ${link.href}")
                indexMd.append("_(failed to load or empty)_\n\n")
                articleBodies.add("")
                return@forEachIndexed
            }
            articleBodies.add(text)
            indexMd.append("_(${text.length} chars queued for TTS)_\n\n")
            tools.info("Queued $idx/${articleLinks.size} for TTS")
            tools.speak(
                text = "${link.text}. $text",
                title = "$idx. ${link.text}",
            )
        }
        tools.finish(indexMd.toString())
    }

    private suspend fun filterArticleLinks(
        tools: BrowserTools,
        links: List<BrowserTools.Link>,
    ): List<BrowserTools.Link> {
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
        private const val MAX_ARTICLES = 25
    }
}
