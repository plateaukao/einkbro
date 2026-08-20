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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Native side of the GM_* API. Registered on the WebView as `window.einkbroGM`.
 * All @JavascriptInterface methods run on the WebView's private JS-bridge thread
 * (never the main thread), so synchronous Room access here is safe.
 *
 * A WebView JavaScript interface cannot be scoped to a single origin, so this bridge
 * is present on *every* page. To stop arbitrary page JS from reaching these
 * capabilities, no method acts on a bare caller-supplied script id; every call must
 * present a random per-injection [token]. A token is minted only when the GM shim is
 * injected for a userscript that matches the page (see NinjaWebViewClient), and it
 * lives inside that shim's IIFE closure — page scripts (and other pages) never see it
 * and so cannot forge a call. The token also pins the exact script the caller may act
 * as, which closes the confused-deputy hole where a page could borrow another script's
 * @connect allow-list or stored values by naming its id.
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
     * token is unknown or its script no longer matches the page currently loaded. The
     * re-match guard stops a token from being reused after the JS context outlived the
     * navigation it was minted for (e.g. a lingering timer firing after the page changed).
     */
    private fun scriptIdFor(token: String?): Long? {
        val scriptId = tokens[token] ?: return null
        val pageUrl = webView.currentPageUrl ?: return null
        return if (userScriptManager.matches(scriptId, pageUrl)) scriptId else null
    }

    // endregion

    // region GM value storage (synchronous)

    @JavascriptInterface
    fun gmGetValue(token: String, key: String): String? {
        val scriptId = scriptIdFor(token) ?: return null
        return userScriptManager.gmGetValue(scriptId, key)
    }

    @JavascriptInterface
    fun gmSetValue(token: String, key: String, value: String) {
        val scriptId = scriptIdFor(token) ?: return
        userScriptManager.gmSetValue(scriptId, key, value)
    }

    @JavascriptInterface
    fun gmDeleteValue(token: String, key: String) {
        val scriptId = scriptIdFor(token) ?: return
        userScriptManager.gmDeleteValue(scriptId, key)
    }

    @JavascriptInterface
    fun gmListValues(token: String): String {
        val scriptId = scriptIdFor(token) ?: return "[]"
        val keys = userScriptManager.gmListValues(scriptId)
        val arr = org.json.JSONArray()
        keys.forEach { arr.put(it) }
        return arr.toString()
    }

    // endregion

    // region GM_xmlhttpRequest

    @JavascriptInterface
    fun gmXhr(token: String, reqId: String, detailsJson: String) {
        val scriptId = scriptIdFor(token)
        if (scriptId == null) {
            Log.w(TAG, "gmXhr rejected: invalid token")
            deliverXhrError(reqId, "unauthorized")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val details = JSONObject(detailsJson)
                val url = details.getString("url")

                if (!isConnectAllowed(scriptId, url)) {
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
    fun gmRegisterMenuCommand(token: String, caption: String, fnId: String) {
        if (scriptIdFor(token) == null) return
        coroutineScope.launch(Dispatchers.Main) {
            webView.registerUserScriptMenuCommand(caption, fnId)
        }
    }

    @JavascriptInterface
    fun gmUnregisterMenuCommand(token: String, fnId: String) {
        if (scriptIdFor(token) == null) return
        coroutineScope.launch(Dispatchers.Main) {
            webView.unregisterUserScriptMenuCommand(fnId)
        }
    }

    @JavascriptInterface
    fun gmOpenInTab(token: String, url: String, active: Boolean) {
        if (scriptIdFor(token) == null) return
        coroutineScope.launch(Dispatchers.Main) {
            webView.openInNewTab(url)
        }
    }

    @JavascriptInterface
    fun gmSetClipboard(token: String, text: String) {
        if (scriptIdFor(token) == null) return
        coroutineScope.launch(Dispatchers.Main) {
            val cm = webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("userscript", text))
        }
    }

    @JavascriptInterface
    fun gmNotification(token: String, text: String) {
        if (scriptIdFor(token) == null) return
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(webView.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun gmLog(token: String, message: String) {
        if (scriptIdFor(token) == null) return
        Log.d(TAG, "userscript: $message")
    }

    // endregion

    companion object {
        private const val TAG = "UserScriptBridge"
    }
}
