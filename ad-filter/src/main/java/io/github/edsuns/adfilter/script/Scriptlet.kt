package io.github.edsuns.adfilter.script

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.anthonycr.mezzanine.FileStream
import com.anthonycr.mezzanine.MezzanineGenerator
import io.github.edsuns.adfilter.impl.Detector
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/4/3.
 */
internal class Scriptlet constructor(private val detector: Detector) {

    @FileStream("src/main/js/scriptlets.min.js")
    interface Scriptlets {
        fun js(): String
    }

    @FileStream("src/main/js/scriptlets_inject.js")
    interface ScriptletsInjection {
        fun js(): String
    }

    private val scriptletsJS: String by lazy(LazyThreadSafetyMode.NONE) {
        var js = MezzanineGenerator.Scriptlets().js()
        js += ScriptInjection.parseScript(this, MezzanineGenerator.ScriptletsInjection().js(), true)
        js
    }

    fun perform(webView: WebView?, url: String?) {
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