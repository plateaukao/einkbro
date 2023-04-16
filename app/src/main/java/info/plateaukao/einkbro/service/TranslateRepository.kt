package info.plateaukao.einkbro.service

import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup

class TranslateRepository {
    private val client = OkHttpClient()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun gTranslate(
        text: String,
        targetLanguage: String = "en",
        sourceLanguage: String = "auto",
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("translate.google.com")
                .addPathSegment("m")
                .addQueryParameter("tl", targetLanguage)
                .addQueryParameter("sl", sourceLanguage)
                .addQueryParameter("q", text)
                .build()

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resume(null) {}
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        val body = response.body?.string()
                        if (body != null) {
                            Jsoup.parse(body)
                                .body()
                                .getElementsByClass("result-container")
                                .first()?.text()?.let {
                                    continuation.resume(StringEscapeUtils.unescapeJava(Uri.decode(it))) {}
                                } ?: continuation.resume(null) {}
                        } else {
                            continuation.resume(null) {}
                        }
                    }
                }
            })

            continuation.invokeOnCancellation {
                client.dispatcher.executorService.shutdownNow()
            }
        }
    }
}