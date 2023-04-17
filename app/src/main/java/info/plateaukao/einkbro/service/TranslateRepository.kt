package info.plateaukao.einkbro.service

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class TranslateRepository {
    private val client = OkHttpClient()

    suspend fun gTranslate(
        text: String,
        targetLanguage: String = "en",
        sourceLanguage: String = "auto",
    ): String? {
        return withContext(IO) {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("translate.google.com")
                .addPathSegment("m")
                .addQueryParameter("tl", targetLanguage)
                .addQueryParameter("sl", sourceLanguage)
                .addQueryParameter("q", text)
                .build()

            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use null

                Jsoup.parse(body)
                    .body()
                    .getElementsByClass("result-container")
                    .first()?.text()
            }
        }
    }
}