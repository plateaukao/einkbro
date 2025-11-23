package info.plateaukao.einkbro.search.suggestion

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.ResponseBody

class DuckDuckGoSuggestionsRepository : SearchSuggestionsRepository {
    private val okHttpClient: okhttp3.OkHttpClient by lazy { okhttp3.OkHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }

    // https://duckduckgo.com/ac/?q={query}&type=list
    private fun createQueryUrl(query: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("duckduckgo.com")
        .encodedPath("/ac/")
        .addQueryParameter("q", query)
        .addQueryParameter("type", "list")
        .build()

    @Throws(Exception::class)
    private fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        val stringBody = responseBody.string()
        val jsonElement = json.parseToJsonElement(stringBody)
        val jsonArray = jsonElement.jsonArray

        if (jsonArray.size < 2) {
            return emptyList()
        }

        val suggestionsArray = jsonArray[1].jsonArray
        val suggestions = mutableListOf<SearchSuggestion>()

        for (element in suggestionsArray) {
            val suggestion = element.jsonPrimitive.content
            suggestions.add(SearchSuggestion(suggestion, suggestion))
        }

        return suggestions
    }

    override suspend fun searchSuggestionResults(query: String): List<SearchSuggestion> {
        val url = createQueryUrl(query)
        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Unexpected code $response")
            }

            val body = response.body ?: throw Exception("Response body is null")
            return parseResults(body)
        }
    }
}
