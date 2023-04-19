package info.plateaukao.einkbro.service

import android.util.Base64
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
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TranslateRepository : KoinComponent {
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

    private fun getAuthKey(): String? {
        val url = "https://papago.naver.com"
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val html = response.body?.string() ?: return null

        val pattern1 = "/vendors~main.*chunk.js".toRegex()

        var path = ""
        Jsoup.parse(html).getElementsByTag("script").forEach { element ->
            val matchedElement = pattern1.find(element.toString())
            if (matchedElement != null) {
                path = matchedElement.value
            }
        }

        val jsUrl = "$url$path"
        val rest = client.newCall(Request.Builder().url(jsUrl).build()).execute()
        val org = rest.body?.string() ?: return null
        val pattern2 = "AUTH_KEY:\\s*\"[\\w.]+\"".toRegex()

        return pattern2.find(org)?.value?.split("\"")?.get(1)
    }

    private var authKey: String? = null
    suspend fun ppTranslate(
        text: String,
        targetLanguage: String = "en",
        sourceLanguage: String = "ko",
    ): String? {
        if (authKey == null) {
            authKey = getAuthKey()
        }
        val key = authKey?.toByteArray(Charsets.UTF_8) ?: return ""

        val guid = UUID.randomUUID()
        val timestamp = System.currentTimeMillis()
        val code = "$guid\n$API_URL\n$timestamp".toByteArray(Charsets.UTF_8)
        val hmac = Mac.getInstance("HmacMD5")
        val secretKeySpec = SecretKeySpec(key, "HmacMD5")
        hmac.init(secretKeySpec)
        val token = Base64.encodeToString(hmac.doFinal(code), Base64.DEFAULT)

        return withContext(IO) {
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("device-type", "pc")
                .addHeader("x-apigw-partnerid", "papago")
                .addHeader("Origin", "https://papago.naver.com")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Authorization", "PPG $guid:$token".replace("\n", ""))
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Timestamp", timestamp.toString())
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
                    body.getString("translatedText")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

//    suspend fun pDetectLanguage(text: String): String? {
//        if (authKey == null) {
//            authKey = getAuthKey()
//        }
//        val key = authKey?.toByteArray(Charsets.UTF_8) ?: return ""
//
//        val guid = UUID.randomUUID()
//        val timestamp = System.currentTimeMillis()
//        val code = "$guid\n$API_URL\n$timestamp".toByteArray(Charsets.UTF_8)
//        val hmac = Mac.getInstance("HmacMD5")
//        val secretKeySpec = SecretKeySpec(key, "HmacMD5")
//        hmac.init(secretKeySpec)
//        val token = Base64.encodeToString(hmac.doFinal(code), Base64.DEFAULT)
//
//        return withContext(IO) {
//            val request = Request.Builder()
//                .url(DETECT_LANGUAGE_URL)
//                .addHeader("device-type", "pc")
//                .addHeader("x-apigw-partnerid", "papago")
//                .addHeader("Origin", "https://papago.naver.com")
//                .addHeader("Sec-Fetch-Site", "same-origin")
//                .addHeader("Sec-Fetch-Mode", "cors")
//                .addHeader("Sec-Fetch-Dest", "empty")
//                .addHeader("Authorization", "PPG $guid:$token".replace("\n", ""))
//                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
//                .addHeader("Timestamp", timestamp.toString())
//                .post(
//                    FormBody.Builder()
//                        .add("query", text)
//                        .build()
//                )
//                .build()
//
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) return@use null
//
//                try {
//                    val body = JSONObject(response.body?.string() ?: return@use null)
//                    body.getString("langCode")
//                } catch (e: Exception) {
//                    null
//                }
//            }
//        }
//    }

    companion object {
        private const val API_URL = "https://papago.naver.com/apis/n2mt/translate"
        private const val DETECT_LANGUAGE_URL = "https://papago.naver.com/apis/langs/dect"
    }
}