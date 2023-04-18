package info.plateaukao.einkbro.service

import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslateRepository:KoinComponent {
    private val client = OkHttpClient()
    private val config: ConfigManager by inject()

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

    suspend fun pTranslate(
        text: String,
        targetLanguage: String = "en",
        sourceLanguage: String = "auto",
    ): String? {
        return withContext(IO) {
            val request = Request.Builder()
                .url("https://openapi.naver.com/v1/papago/n2mt")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("X-Naver-Client-Id", "wCrZZHNa1x_wi8tkGERB")
                .addHeader("X-Naver-Client-Secret", config.papagoApiSecret)
                .post(
                    FormBody.Builder()
                        .add("source", sourceLanguage)
                        .add("target", targetLanguage)
                        .add("text", text)
                        .build()
                )
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null

                try {
                    val body = JSONObject(response.body?.string() ?: return@use null)
                    body.getJSONObject("message")
                        .getJSONObject("result")
                        .getString("translatedText")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}