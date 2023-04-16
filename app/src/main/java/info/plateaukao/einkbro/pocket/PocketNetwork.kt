package info.plateaukao.einkbro.pocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class PocketNetwork {
    private val client = OkHttpClient()
    private val consumerKey = "106771-19592cda93fd9033c29a31b"

    fun getRequestToken(callback: (String) -> Unit) {
        val requestBody = FormBody.Builder()
            .add("consumer_key", consumerKey)
            .add("redirect_uri", "einkbropocket://pocket-auth")
            .build()

        val request = Request.Builder()
            .url("https://getpocket.com/v3/oauth/request")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle error
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val requestToken = body?.replace("code=", "") ?: return
                    callback(requestToken)
                }
            }
        })
    }

    fun getAccessToken(requestToken: String, callback: (String) -> Unit) {
        val requestBody = FormBody.Builder()
            .add("consumer_key", consumerKey)
            .add("code", requestToken)
            .build()

        val request = Request.Builder()
            .url("https://getpocket.com/v3/oauth/authorize")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle error
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val accessToken = body?.substringAfter("access_token=")?.substringBefore("&")
                    val username = body?.substringAfter("username=")
                    if (accessToken != null) {
                        callback(accessToken)
                    }
                }
            }
        })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun addUrlToPocket(
        accessToken: String,
        url: String,
        title: String? = null,
        tags: String? = null
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            val requestBodyBuilder = FormBody.Builder()
                .add("url", url)
                .add("consumer_key", consumerKey)
                .add("access_token", accessToken)

            title?.let {
                requestBodyBuilder.add("title", it)
            }

            tags?.let {
                requestBodyBuilder.add("tags", it)
            }

            val requestBody = requestBodyBuilder.build()

            val request = Request.Builder()
                .url("https://getpocket.com/v3/add")
                .post(requestBody)
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
                            val jsonResponse = JSONObject(body)
                            val item = jsonResponse.getJSONObject("item")
                            val articleUrl =
                                "https://getpocket.com/read/" + item.getString("item_id")
                            continuation.resume(articleUrl) {}
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