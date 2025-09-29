package info.plateaukao.einkbro.search.suggestion

/**
 * A repository for search suggestions.
 */
interface SearchSuggestionsRepository {
    /**
     * Creates a [Single] that fetches the search suggestion results for the provided query.
     *
     * @param rawQuery the raw query to retrieve the results for.
     * @return a [Single] that emits the list of results for the query.
     */
    suspend fun searchSuggestionResults(query: String): List<SearchSuggestion>
}

data class SearchSuggestion(
    override val url: String,
    override val title: String
): WebPage(url, title)

/**
 * A data type that represents a page that can be loaded.
 *
 * @param url The URL of the web page.
 * @param title The title of the web page.
 */
sealed class WebPage(
    open val url: String,
    open val title: String
)

const val UTF8 = "UTF-8"