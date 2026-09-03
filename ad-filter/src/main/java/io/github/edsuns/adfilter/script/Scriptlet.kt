package io.github.edsuns.adfilter.script

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.github.edsuns.adfilter.impl.Detector
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/4/3.
 */
internal class Scriptlet constructor(private val detector: Detector) {

    private val scriptletsJS: String by lazy(LazyThreadSafetyMode.NONE) {
        var js = JsAssets.load("scriptlets.min.js")
        js += ScriptInjection.parseScript(this, JsAssets.load("scriptlets_inject.js"), true)
        js
    }

    fun perform(webView: WebView?, url: String?) {
        // The scriptlets library is 147 KB of JS; most pages have no scriptlet
        // rules, and the native lookup deciding that costs far less than the
        // renderer parsing the library.
        if (url == null || detector.getScriptlets(url).isEmpty()) return
        webView?.evaluateJavascript(scriptletsJS, null)
        Timber.v("Evaluated Scriptlets Javascript for $url")
    }

    @JavascriptInterface
    fun getScriptlets(documentUrl: String): String {
        val list = detector.getScriptlets(documentUrl)
        val json = list.toScriptletsJSON()
        Timber.v("offer scriptlets: $json")
        return json
    }

    private fun Collection<String>.toScriptletsJSON(): String {
        val builder = StringBuilder()
        for (str in this) {
            if (builder.isNotEmpty()) {
                builder.append(',')
            }
            builder.append('[').append(str).append(']')
        }
        builder.insert(0, '[')
        builder.append(']')
        return builder.toString().replace('\'', '"')// only allow double quotes
    }
}