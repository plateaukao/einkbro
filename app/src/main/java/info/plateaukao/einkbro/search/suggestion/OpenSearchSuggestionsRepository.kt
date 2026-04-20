package info.plateaukao.einkbro.search.suggestion

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

/**
 * Generic repository that consumes the OpenSearch suggestions JSON format:
 * `["query", ["suggestion1", "suggestion2", ...], ...]`.
 *
 * Most search engines (DuckDuckGo, Bing, Ecosia, Startpage, Yandex, ...) expose
 * an endpoint that conforms to this shape, so each engine only needs to provide
 * a URL builder rather than a full repository implementation.
 */
class OpenSearchSuggestionsRepository(
    private val buildUrl: (String) -> HttpUrl,
) : SearchSuggestionsRepository {
    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }

    @Throws(Exception::class)
    private fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        val array = json.parseToJsonElement(responseBody.string()).jsonArray
        if (array.size < 2) return emptyList()

        return array[1].jsonArray.map { element ->
            val value = element.jsonPrimitive.content
            SearchSuggestion(value, value)
        }
    }

    override suspend fun searchSuggestionResults(query: String): List<SearchSuggestion> {
        val request = Request.Builder().url(buildUrl(query)).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val body = response.body ?: throw Exception("Response body is null")
            return parseResults(body)
        }
    }

    companion object {
        fun duckDuckGo(): OpenSearchSuggestionsRepository =
            OpenSearchSuggestionsRepository { query ->
                HttpUrl.Builder()
                    .scheme("https")
                    .host("duckduckgo.com")
                    .encodedPath("/ac/")
                    .addQueryParameter("q", query)
                    .addQueryParameter("type", "list")
                    .build()
            }

        fun bing(): OpenSearchSuggestionsRepository =
            OpenSearchSuggestionsRepository { query ->
                HttpUrl.Builder()
                    .scheme("https")
                    .host("www.bing.com")
                    .encodedPath("/osjson.hint")
                    .addQueryParameter("query", query)
                    .build()
            }

        fun ecosia(): OpenSearchSuggestionsRepository =
            OpenSearchSuggestionsRepository { query ->
                HttpUrl.Builder()
                    .scheme("https")
                    .host("ac.ecosia.org")
                    .encodedPath("/")
                    .addQueryParameter("q", query)
                    .addQueryParameter("type", "list")
                    .build()
            }

        fun startpage(): OpenSearchSuggestionsRepository =
            OpenSearchSuggestionsRepository { query ->
                HttpUrl.Builder()
                    .scheme("https")
                    .host("www.startpage.com")
                    .encodedPath("/suggestions")
                    .addQueryParameter("q", query)
                    .addQueryParameter("format", "opensearch")
                    .build()
            }

        fun yandex(): OpenSearchSuggestionsRepository =
            OpenSearchSuggestionsRepository { query ->
                HttpUrl.Builder()
                    .scheme("https")
                    .host("suggest.yandex.com")
                    .encodedPath("/suggest-ya.cgi")
                    .addQueryParameter("v", "4")
                    .addQueryParameter("part", query)
                    .build()
            }
    }
}
