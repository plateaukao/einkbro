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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Native side of the GM_* API. Registered on the WebView as `window.einkbroGM`.
 * All @JavascriptInterface methods run on the WebView's private JS-bridge thread
 * (never the main thread), so synchronous Room access here is safe.
 *
 * Each call requires a per-injection token, an enabled script matching the current
 * page URL, and an explicit @grant for that API. Tokens are cleared on navigation.
 * The shim keeps API closures private to each userscript instead of publishing them
 * on window. Scripts still run in the page's JavaScript world; this is capability
 * enforcement, not an isolated JavaScript sandbox.
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

    // region capability tokens

    /** token -> script id, populated as shims are injected, cleared on each navigation. */
    private val tokens = ConcurrentHashMap<String, Long>()

    fun registerToken(token: String, scriptId: Long) {
        tokens[token] = scriptId
    }

    fun clearTokens() {
        tokens.clear()
    }

    /**
     * Resolve a caller-presented token to the script id it authorizes, or null when the
     * token is unknown, its script no longer matches the page, or the API is not granted. The
     * re-match guard stops a token from being reused after the JS context outlived the
     * navigation it was minted for (e.g. a lingering timer firing after the page changed).
     */
    private fun scriptIdFor(token: String?, api: String): Long? {
        val scriptId = token?.let { tokens[it] } ?: return null
        val pageUrl = webView.currentPageUrl ?: return null
        return if (userScriptManager.isApiAllowed(scriptId, pageUrl, api)) scriptId else null
    }

    // endregion

    // region GM value storage (synchronous)

    @JavascriptInterface
    fun gmGetValue(token: String, key: String): String? {
        val scriptId = scriptIdFor(token, "getValue") ?: return null
        return userScriptManager.gmGetValue(scriptId, key)
    }

    @JavascriptInterface
    fun gmSetValue(token: String, key: String, value: String) {
        val scriptId = scriptIdFor(token, "setValue") ?: return
        userScriptManager.gmSetValue(scriptId, key, value)
    }

    @JavascriptInterface
    fun gmDeleteValue(token: String, key: String) {
        val scriptId = scriptIdFor(token, "deleteValue") ?: return
        userScriptManager.gmDeleteValue(scriptId, key)
    }

    @JavascriptInterface
    fun gmListValues(token: String): String {
        val scriptId = scriptIdFor(token, "listValues") ?: return "[]"
        val keys = userScriptManager.gmListValues(scriptId)
        val arr = org.json.JSONArray()
        keys.forEach { arr.put(it) }
        return arr.toString()
    }

    // endregion

    // region GM_xmlhttpRequest

    @JavascriptInterface
    fun gmXhr(token: String, reqId: String, detailsJson: String) {
        val scriptId = scriptIdFor(token, "xmlhttpRequest")
        if (scriptId == null) {
            Log.w(TAG, "gmXhr rejected: missing script or API permission")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            if (scriptIdFor(token, "xmlhttpRequest") == null) return@launch
            try {
                val details = JSONObject(detailsJson)
                val url = details.getString("url")

                if (!isConnectAllowed(scriptId, url)) {
                    Log.w(TAG, "gmXhr blocked (not in @connect): $url")
                    deliverXhrError(token, reqId, "url not in @connect allow-list: $url")
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
                    deliverXhr(token, reqId, "load", payload.toString())
                }
            } catch (e: java.io.InterruptedIOException) {
                Log.w(TAG, "gmXhr timeout: ${e.message}")
                deliverXhr(token, reqId, "timeout", JSONObject().apply {
                    put("readyState", 4); put("status", 0); put("statusText", "timeout")
                }.toString())
            } catch (e: Exception) {
                Log.w(TAG, "gmXhr failed: ${e.message}")
                deliverXhrError(token, reqId, e.message ?: "request failed")
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

    private fun deliverXhrError(token: String, reqId: String, message: String) {
        val payload = JSONObject().apply {
            put("readyState", 4)
            put("status", 0)
            put("statusText", "error")
            put("error", message)
        }
        deliverXhr(token, reqId, "error", payload.toString())
    }

    private fun deliverXhr(token: String, reqId: String, event: String, payloadJson: String) {
        val js = "window.__einkbroGM && window.__einkbroGM.handleXhr(" +
            "${JSONObject.quote(reqId)}, ${JSONObject.quote(event)}, ${JSONObject.quote(payloadJson)});"
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "xmlhttpRequest") != null && webView.isAttachedToWindow) {
                webView.evaluateJavascript(js, null)
            }
        }
    }

    // endregion

    // region menu / misc

    @JavascriptInterface
    fun gmRegisterMenuCommand(token: String, caption: String, fnId: String) {
        if (scriptIdFor(token, "registerMenuCommand") == null) return
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "registerMenuCommand") == null) return@launch
            webView.registerUserScriptMenuCommand(caption, fnId)
        }
    }

    @JavascriptInterface
    fun gmUnregisterMenuCommand(token: String, fnId: String) {
        if (scriptIdFor(token, "unregisterMenuCommand") == null) return
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "unregisterMenuCommand") == null) return@launch
            webView.unregisterUserScriptMenuCommand(fnId)
        }
    }

    @JavascriptInterface
    fun gmOpenInTab(token: String, url: String, active: Boolean) {
        if (scriptIdFor(token, "openInTab") == null) return
        // A userscript only ever opens web pages. Refusing every other scheme keeps the
        // bridge from becoming a way to navigate a tab to file:, content: or intent:
        // targets that Chromium would never let page JS reach on its own.
        if (!isWebUrl(url)) return
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "openInTab") == null) return@launch
            webView.openInNewTab(url)
        }
    }

    @JavascriptInterface
    fun gmSetClipboard(token: String, text: String) {
        if (scriptIdFor(token, "setClipboard") == null) return
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "setClipboard") == null) return@launch
            val cm = webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("userscript", text))
        }
    }

    @JavascriptInterface
    fun gmNotification(token: String, text: String) {
        if (scriptIdFor(token, "notification") == null) return
        coroutineScope.launch(Dispatchers.Main) {
            if (scriptIdFor(token, "notification") == null) return@launch
            Toast.makeText(webView.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun gmLog(token: String, message: String) {
        if (scriptIdFor(token, "log") == null) return
        Log.d(TAG, "userscript: $message")
    }

    // endregion

    companion object {
        private const val TAG = "UserScriptBridge"

        /** True only for absolute http(s) URLs — the sole targets GM_openInTab may open. */
        fun isWebUrl(url: String): Boolean {
            val scheme = runCatching { URI(url.trim()).scheme }.getOrNull() ?: return false
            return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
        }
    }
}
