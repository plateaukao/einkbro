package info.plateaukao.einkbro.search.suggestion

import okhttp3.HttpUrl
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class GoogleSuggestionsRepository : SearchSuggestionsRepository {
    private val okHttpClient: okhttp3.OkHttpClient by lazy { okhttp3.OkHttpClient() }

    // https://suggestqueries.google.com/complete/search?output=toolbar&hl={language}&q={query}
    private fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("suggestqueries.google.com")
        .encodedPath("/complete/search")
        .addQueryParameter("output", "toolbar")
        .addQueryParameter("hl", language)
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    private fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        parser.setInput(responseBody.byteStream(), UTF8)

        val suggestions = mutableListOf<SearchSuggestion>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                suggestions.add(SearchSuggestion(suggestion, suggestion))
            }
            eventType = parser.next()
        }

        return suggestions
    }

    override suspend fun searchSuggestionResults(query: String): List<SearchSuggestion> {
        val url = createQueryUrl(query, "null")
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

    companion object {
        // Converting to a lambda results in pulling the newInstance call out of the lazy.
        @Suppress("ConvertLambdaToReference")
        private val parser by lazy {
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }.newPullParser()
        }
    }
}