package info.plateaukao.einkbro.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class TranslateRepository : KoinComponent {
    private val client = OkHttpClient()
    private val config: ConfigManager by inject()

    suspend fun gTranslateWithWeb(
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

    private fun getICount(translateText: String): Int {
        return translateText.split("i").size - 1
    }

    private fun getRandomNumber(): Int {
        val rand = (Math.random() * 99999).toInt() + 200000
        return rand * 1000
    }

    private fun getTimeStamp(iCount: Int): Long {
        val ts = System.currentTimeMillis()
        return if (iCount != 0) {
            val adjustedICount = iCount + 1
            ts - (ts % adjustedICount) + adjustedICount
        } else {
            ts
        }
    }

    private fun processPostData(postData: JSONObject, id: Int): String {
        var postStr = postData.toString()

        if ((id + 5) % 29 == 0 || (id + 3) % 13 == 0) {
            postStr = postStr.replace("\"method\":\"", "\"method\" : \"")
        } else {
            postStr = postStr.replace("\"method\":\"", "\"method\": \"")
        }

        return postStr
    }

    suspend fun deepLTranslate(
        text: String,
        targetLanguage: String = "zh",
        sourceLanguage: String = "auto",
    ): String? {
        val headers = Headers.Builder()
            .add("content-type", "application/json")
            .build()

        val id = getRandomNumber()
        val data = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", "LMT_handle_texts")
            put("id", id)
            put("params", JSONObject().apply {
                put("splitting", "newlines")
                put("lang", JSONObject().apply {
                    put("source_lang_user_selected", sourceLanguage.uppercase(Locale.getDefault()))
                    put("target_lang", targetLanguage.uppercase(Locale.getDefault()))
                })
                put("texts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", text)
                        put("requestAlternatives", 1)
                    })
                })
                put("timestamp", getTimeStamp(getICount(text)))
            })
        }

        val body =
            RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                processPostData(data, id)
            )

        val request = Request.Builder()
            .url("https://www2.deepl.com/jsonrpc")
            .headers(headers)
            .post(body)
            .build()

        return withContext(IO) {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use null
                try {
                    val result = JSONObject(body).getJSONObject("result")
                    return@use result.getJSONArray("texts").getJSONObject(0).getString("text")
                } catch (e: Exception) {
                    Log.d("TranslateRepository", "deepLTranslate: $e")
                    return@use null
                }
            }
        }
    }

    suspend fun gTranslateWithApi(
        text: String,
        targetLanguage: String = "en",
        sourceLanguage: String = "auto",
    ): String? {
        return withContext(IO) {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("translate.googleapis.com")
                .addPathSegment("translate_a")
                .addPathSegment("single")
                .addQueryParameter("client", "gtx")
                .addQueryParameter("tl", targetLanguage)
                .addQueryParameter("sl", sourceLanguage)
                .addQueryParameter("dt", "t")
                .addQueryParameter("q", text)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://translate.google.com/")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use null

                try {
                    val result = StringBuilder()
                    val array: JSONArray = JSONArray(body).get(0) as JSONArray
                    for (i in 0 until array.length()) {
                        val item = array[i] as JSONArray
                        result.append(item[0].toString())
                    }
                    result.toString()
                } catch (e: Exception) {
                    null
                }
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
        sourceLanguage: String = "auto",
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

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null

                    val body = JSONObject(response.body?.string() ?: return@use null)
                    body.getString("translatedText")
                }
            } catch (e: Exception) {
                Log.d("TranslateRepository", "ppTranslate: $e")
                null
            }
        }
    }

    suspend fun pDetectLanguage(text: String): String? {
        if (authKey == null) {
            authKey = getAuthKey()
        }
        val key = authKey?.toByteArray(Charsets.UTF_8) ?: return ""

        val guid = UUID.randomUUID()
        val timestamp = System.currentTimeMillis()
        val code = "$guid\n$DETECT_LANGUAGE_URL\n$timestamp".toByteArray(Charsets.UTF_8)
        val hmac = Mac.getInstance("HmacMD5")
        val secretKeySpec = SecretKeySpec(key, "HmacMD5")
        hmac.init(secretKeySpec)
        val token = Base64.encodeToString(hmac.doFinal(code), Base64.DEFAULT)

        return withContext(IO) {
            val request = Request.Builder()
                .url(DETECT_LANGUAGE_URL)
                .addHeader("device-type", "pc")
                .addHeader("x-apigw-partnerid", "papago")
                .addHeader("Origin", "https://papago.naver.com")
                .addHeader("Referer", "https://papago.naver.com")
                .addHeader("Authorization", "PPG $guid:$token".replace("\n", ""))
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Timestamp", timestamp.toString())
                .post(
                    FormBody.Builder()
                        .add("query", text)
                        .build()
                )
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null

                try {
                    val body = JSONObject(response.body?.string() ?: return@use null)
                    body.getString("langCode")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }


    private val sid: String by lazy { "${P_IMAGE_API_VERSION}${UUID.randomUUID()}" }

    private fun signUrl(url: String): Signature {
        val ts = System.currentTimeMillis()

        val urlToSign = url.take(255)
        val data = "$urlToSign$ts".toByteArray()

        val hmac: Mac = try {
            Mac.getInstance("HmacSHA1")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get HmacSHA1 Mac instance", e)
        }
        val keySpec = SecretKeySpec(config.imageApiKey.toByteArray(), "HmacSHA1")
        try {
            hmac.init(keySpec)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to initialize HMAC", e)
        }
        val result = hmac.doFinal(data)

        val encodedMsg = Base64.encodeToString(result, Base64.NO_WRAP)

        return Signature(ts, encodedMsg)
    }

    suspend fun translateBitmap(
        bitmap: Bitmap,
        srcLang: String,
        dstLang: String,
        langDetect: Boolean,
    ): ImageTranslateResult? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val builder = createMultipartBuilder(srcLang, dstLang, langDetect)
        builder.addFormDataPart(
            "image", "image.jpg",
            byteArray.toRequestBody("image/*".toMediaType())
        )
        return getImageTranslateResult(builder.build())
    }

    private fun createMultipartBuilder(
        srcLang: String,
        dstLang: String,
        langDetect: Boolean,
    ): MultipartBody.Builder = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("lang", "ko")
        .addFormDataPart("upload", "true")
        .addFormDataPart("sid", sid)
        .addFormDataPart("source", srcLang)
        .addFormDataPart("target", dstLang)
        .addFormDataPart("langDetect", if (langDetect) "true" else "false")
        .addFormDataPart("imageId", "")
        .addFormDataPart("reqType", "")

    private suspend fun getImageTranslateResult(requestBody: RequestBody): ImageTranslateResult? {
        val sig = signUrl(IMAGE_API_URL)
        val finalUrl = HttpUrl.Builder()
            .scheme("https")
            .host("apis.naver.com")
            .addPathSegments("papago/papago_app/ocr/detect")
            .addQueryParameter("msgpad", sig.ts.toString())
            .addQueryParameter("md", sig.msg)
            .build().toString()
        val request = Request.Builder()
            .url(finalUrl)
            .post(requestBody)
            .build()

        return withContext(IO) {
            client.newCall(request)
                .execute()
                .use { response ->
                    if (!response.isSuccessful) null
                    else {
                        response.body?.let {
                            val jsonObject = JSONObject(it.string())
                            ImageTranslateResult(
                                jsonObject.getString("imageId"),
                                jsonObject.getString("renderedImage")
                            )
                        }
                    }
                }
        }

    }

    suspend fun translateImageFromUrl(
        referer: String,
        url: String,
        srcLang: String,
        dstLang: String,
        langDetect: Boolean,
    ): ImageTranslateResult? {
        val file = downloadImage(url, referer) ?: return null
        val builder = createMultipartBuilder(srcLang, dstLang, langDetect)
        builder.addFormDataPart(
            "image", file.name,
            file.asRequestBody("image/*".toMediaType())
        )
        return getImageTranslateResult(builder.build())
    }

    private suspend fun downloadImage(url: String, referer: String): File? {
        return withContext(IO) {
            val file = File.createTempFile("image", ".jpg")
            val request = Request.Builder().url(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Mobile Safari/537.36"
                )
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Referer", referer)
                .build()
            client.newCall(request).execute().use { response ->
                response.body?.byteStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream?.copyTo(outputStream)
                        file
                    }
                }
            }
        }
    }

    companion object {
        const val P_IMAGE_API_VERSION = "1.9.9"
        private const val API_URL = "https://papago.naver.com/apis/n2mt/translate"
        private const val IMAGE_API_URL = "https://apis.naver.com/papago/papago_app/ocr/detect"
        private const val DETECT_LANGUAGE_URL = "https://papago.naver.com/apis/langs/dect"
    }
}

data class Signature(val ts: Long, val msg: String)

data class ImageTranslateResult(
    val imageId: String,
    val renderedImage: String
)

