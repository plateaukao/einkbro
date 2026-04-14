package info.plateaukao.einkbro.task.tasks

import info.plateaukao.einkbro.task.BrowserTask
import info.plateaukao.einkbro.task.BrowserTools
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
        tools.info("Selected ${articleLinks.size} article links to summarize.")

        // Pipelined execution:
        //   • Page loads are serial (single off-screen WebView).
        //   • As soon as each page's text is extracted, its LLM summary is launched
        //     in parallel — so the summary for article N runs concurrently with the
        //     page load for article N+1 (and the LLM call for article N-1, etc).
        //   • A per-index CompletableDeferred lets parallel summary jobs fill the
        //     slot out of order; the consumer awaits strictly in input order so the
        //     TTS queue receives summaries in the order the user expects.
        val slots: List<CompletableDeferred<ArticleResult>> =
            List(articleLinks.size) { CompletableDeferred() }

        coroutineScope {
            // Producer: serial fetch + fan-out summarization.
            launch {
                articleLinks.forEachIndexed { index, link ->
                    tools.info("Fetching ${index + 1}/${articleLinks.size}: ${link.text.take(80)}")
                    val opened = tools.openUrlInBg(link.href)
                    val text = if (opened) tools.currentBgPageText().trim() else ""
                    launch {
                        val summary = summarize(tools, text, language)
                        slots[index].complete(ArticleResult(index, link, summary))
                    }
                }
            }

            // Consumer: await in input order; stream each result into markdown and
            // hand it to the TTS queue as soon as it's ready.
            val running = StringBuilder()
            running.append("# ").append(pageTitle).append("\n\n")
            slots.forEachIndexed { i, deferred ->
                val result = deferred.await()
                val idx = i + 1
                running.append("## ").append(idx).append(". ").append(result.link.text).append("\n")
                running.append(result.link.href).append("\n\n")
                running.append(result.summary).append("\n\n")
                tools.info("Ready $idx/${articleLinks.size}: ${result.link.text.take(80)}")
                tools.speak(
                    text = "${result.link.text}. ${result.summary}",
                    title = "$idx. ${result.link.text}",
                )
            }
            tools.finish(running.toString())
        }
    }

    private suspend fun summarize(
        tools: BrowserTools,
        text: String,
        language: String,
    ): String {
        if (text.isBlank()) return "_(failed to load or empty)_"
        val capped = if (text.length > MAX_CHARS_PER_ARTICLE) {
            text.substring(0, MAX_CHARS_PER_ARTICLE)
        } else text
        val summary = tools.askLlm(
            system = "You are a concise summarizer for a busy reader. Reply with 2 sentences at most. " +
                "Reply in $language unless the article content requires otherwise. No preamble.",
            user = "Summarize this article:\n\n$capped",
        )?.trim().orEmpty()
        return summary.ifBlank { "_(no summary)_" }
    }

    private data class ArticleResult(
        val index: Int,
        val link: BrowserTools.Link,
        val summary: String,
    )

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
