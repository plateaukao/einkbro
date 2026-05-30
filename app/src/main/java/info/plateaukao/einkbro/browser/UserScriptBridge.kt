package info.plateaukao.einkbro.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import info.plateaukao.einkbro.userscript.UserScriptManager
import info.plateaukao.einkbro.view.EBWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Native side of the GM_* API. Registered on the WebView as `window.einkbroGM`.
 * All @JavascriptInterface methods run on the WebView's private JS-bridge thread
 * (never the main thread), so synchronous Room access here is safe.
 */
class UserScriptBridge(
    private val webView: EBWebView,
) : KoinComponent {
    private val userScriptManager: UserScriptManager by inject()
    private val coroutineScope: CoroutineScope by inject()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // region GM value storage (synchronous)

    @JavascriptInterface
    fun gmGetValue(scriptId: String, key: String): String? =
        userScriptManager.gmGetValue(scriptId.toLong(), key)

    @JavascriptInterface
    fun gmSetValue(scriptId: String, key: String, value: String) =
        userScriptManager.gmSetValue(scriptId.toLong(), key, value)

    @JavascriptInterface
    fun gmDeleteValue(scriptId: String, key: String) =
        userScriptManager.gmDeleteValue(scriptId.toLong(), key)

    @JavascriptInterface
    fun gmListValues(scriptId: String): String {
        val keys = userScriptManager.gmListValues(scriptId.toLong())
        val arr = org.json.JSONArray()
        keys.forEach { arr.put(it) }
        return arr.toString()
    }

    // endregion

    // region GM_xmlhttpRequest

    @JavascriptInterface
    fun gmXhr(scriptId: String, reqId: String, detailsJson: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val details = JSONObject(detailsJson)
                val url = details.getString("url")

                if (!isConnectAllowed(scriptId.toLong(), url)) {
                    Log.w(TAG, "gmXhr blocked (not in @connect): $url")
                    deliverXhrError(reqId, "url not in @connect allow-list: $url")
                    return@launch
                }

                val method = details.optString("method", "GET").uppercase()
                val headers = details.optJSONObject("headers")
                val data = if (details.isNull("data")) null else details.optString("data")
                val timeoutMs = details.optLong("timeout", 0L)

                val builder = Request.Builder().url(url)
                var contentType: String? = null
                headers?.keys()?.forEach { name ->
                    val v = headers.getString(name)
                    if (name.equals("Content-Type", ignoreCase = true)) contentType = v
                    builder.header(name, v)
                }

                if (method == "GET" || method == "HEAD") {
                    builder.method(method, null)
                } else {
                    val body = (data ?: "").toRequestBody(contentType?.toMediaTypeOrNull())
                    builder.method(method, body)
                }

                // Honor the script's per-request timeout (Tampermonkey semantics). When it
                // fires, deliver a distinct "timeout" event so the script's ontimeout runs
                // instead of leaving the request pending forever.
                val client = if (timeoutMs > 0) {
                    httpClient.newBuilder().callTimeout(timeoutMs, TimeUnit.MILLISECONDS).build()
                } else {
                    httpClient
                }

                client.newCall(builder.build()).execute().use { resp ->
                    val bodyText = resp.body?.string().orEmpty()
                    val headerText = resp.headers.joinToString("\r\n") { "${it.first}: ${it.second}" }
                    val payload = JSONObject().apply {
                        put("readyState", 4)
                        put("status", resp.code)
                        put("statusText", resp.message)
                        put("responseText", bodyText)
                        put("response", bodyText)
                        put("responseHeaders", headerText)
                        put("finalUrl", resp.request.url.toString())
                    }
                    deliverXhr(reqId, "load", payload.toString())
                }
            } catch (e: java.io.InterruptedIOException) {
                Log.w(TAG, "gmXhr timeout: ${e.message}")
                deliverXhr(reqId, "timeout", JSONObject().apply {
                    put("readyState", 4); put("status", 0); put("statusText", "timeout")
                }.toString())
            } catch (e: Exception) {
                Log.w(TAG, "gmXhr failed: ${e.message}")
                deliverXhrError(reqId, e.message ?: "request failed")
            }
        }
    }

    private fun isConnectAllowed(scriptId: Long, url: String): Boolean {
        val connects = userScriptManager.getById(scriptId)?.metadata?.connects ?: return false
        if (connects.any { it == "*" }) return true
        val host = try {
            URI(url).host?.lowercase() ?: return false
        } catch (e: Exception) {
            return false
        }
        // page's own host is always allowed
        val pageHost = try {
            Uri.parse(webView.currentPageUrl ?: "").host?.lowercase()
        } catch (e: Exception) {
            null
        }
        if (pageHost != null && host == pageHost) return true
        return connects.any { entry ->
            val e = entry.lowercase()
            e == host || host == e.removePrefix("*.") || host.endsWith(".$e") ||
                (e.startsWith("*.") && host.endsWith(e.substring(1)))
        }
    }

    private fun deliverXhrError(reqId: String, message: String) {
        val payload = JSONObject().apply {
            put("readyState", 4)
            put("status", 0)
            put("statusText", "error")
            put("error", message)
        }
        deliverXhr(reqId, "error", payload.toString())
    }

    private fun deliverXhr(reqId: String, event: String, payloadJson: String) {
        val js = "window.__einkbroGM && window.__einkbroGM.handleXhr(" +
            "${JSONObject.quote(reqId)}, ${JSONObject.quote(event)}, ${JSONObject.quote(payloadJson)});"
        coroutineScope.launch(Dispatchers.Main) {
            if (webView.isAttachedToWindow) webView.evaluateJavascript(js, null)
        }
    }

    // endregion

    // region menu / misc

    @JavascriptInterface
    fun gmRegisterMenuCommand(scriptId: String, caption: String, fnId: String) {
        coroutineScope.launch(Dispatchers.Main) {
            webView.registerUserScriptMenuCommand(caption, fnId)
        }
    }

    @JavascriptInterface
    fun gmUnregisterMenuCommand(fnId: String) {
        coroutineScope.launch(Dispatchers.Main) {
            webView.unregisterUserScriptMenuCommand(fnId)
        }
    }

    @JavascriptInterface
    fun gmOpenInTab(url: String, active: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            webView.openInNewTab(url)
        }
    }

    @JavascriptInterface
    fun gmSetClipboard(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val cm = webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("userscript", text))
        }
    }

    @JavascriptInterface
    fun gmNotification(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(webView.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun gmLog(message: String) {
        Log.d(TAG, "userscript: $message")
    }

    // endregion

    companion object {
        private const val TAG = "UserScriptBridge"
    }
}
