package io.github.edsuns.adfilter.script

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.anthonycr.mezzanine.FileStream
import com.anthonycr.mezzanine.MezzanineGenerator
import io.github.edsuns.adfilter.impl.Detector
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by Edsuns@qq.com on 2021/1/22.
 */
internal class ElementHiding constructor(private val detector: Detector) {

    @FileStream("src/main/js/elemhide_blocked.js")
    interface ElemhideBlockedInjection {
        fun js(): String
    }

    @FileStream("src/main/js/extended-css.min.js")
    interface ExtendedCssInjection {
        fun js(): String
    }

    @FileStream("src/main/js/element_hiding.js")
    interface EleHidingInjection {
        fun js(): String
    }

    private val eleHidingJS by lazy {
        var js = MezzanineGenerator.ExtendedCssInjection().js()
        js += ScriptInjection.parseScript(this, MezzanineGenerator.EleHidingInjection().js(), true)
        js
    }

    private val elemhideBlockedJs by lazy {
        val js = MezzanineGenerator.ElemhideBlockedInjection().js()
        ScriptInjection.parseScript(this, js)
    }

    internal fun elemhideBlockedResource(webView: WebView?, resourceUrl: String?) {
        var filenameWithQuery: String
        try {
            filenameWithQuery = extractPathWithQuery(resourceUrl)
            if (filenameWithQuery.startsWith("/")) {
                filenameWithQuery = filenameWithQuery.substring(1)
            }
        } catch (e: MalformedURLException) {
            Timber.e("Failed to parse URI for blocked resource:$resourceUrl. Skipping element hiding")
            return
        }
        Timber.d("Trying to elemhide visible blocked resource with url `$resourceUrl`")

        // It finds all the elements with source URLs ending with ... and then compare full paths.
        // We do this trick because the paths in JS (code) can be relative and in DOM tree they are absolute.
        val selectorBuilder = StringBuilder()
            .append("[src$='").append(filenameWithQuery)
            .append("'], [srcset$='")
            .append(filenameWithQuery)
            .append("']")

        // all UI views including WebView can be touched from UI thread only
        webView?.post {
            val scriptBuilder = StringBuilder(elemhideBlockedJs)
                .append("\n\n")
                .append("elemhideForSelector(\"")
                .append(resourceUrl)// 1st argument

            scriptBuilder.append("\", \"")
            scriptBuilder.append(escapeJavaScriptString(selectorBuilder.toString()))// 2nd argument

            scriptBuilder.append("\", 0)")// attempt #0

            webView.evaluateJavascript(scriptBuilder.toString(), null)
        }
    }

    fun perform(webView: WebView?, url: String?) {
        webView?.evaluateJavascript(eleHidingJS, null)
        Timber.v("Evaluated element hiding Javascript for $url")
    }

    private fun List<String>.joinString(): String {
        val builder = StringBuilder()
        for (s in this) {
            builder.append(s)
        }
        return builder.toString()
    }

    @JavascriptInterface
    fun getStyleSheet(documentUrl: String): String {
        val result = StringBuilder()
        val selectors = detector.getElementHidingSelectors(documentUrl)
        val customSelectors = detector.getCustomElementHidingSelectors(documentUrl)
        val cssRules = detector.getCssRules(documentUrl).joinString()
        if (selectors.isNotBlank()) {
            result.append(selectors).append(HIDING_CSS)
        }
        if (customSelectors.isNotBlank()) {
            result.append(customSelectors).append(HIDING_CSS)
        }
        if (cssRules.isNotBlank()) {
            result.append(cssRules)
        }
        // stylesheet has a limit of length, split it into smaller pieces by the replacement
        return result.toString().replace(", ", HIDING_CSS, 200)
    }

    @JavascriptInterface
    fun getExtendedCssStyleSheet(documentUrl: String): String {
        val extendedCss = detector.getExtendedCssSelectors(documentUrl)
        if (extendedCss.isNotEmpty()) {
            // join to String with ", "
            return extendedCss.joinToString() + HIDING_CSS
        }
        return ""
    }

    /**
     * Extract path with query from URL
     * @param urlString URL
     * @return path with optional query part
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun extractPathWithQuery(urlString: String?): String {
        val url = URL(urlString)
        val sb = StringBuilder(url.path)
        if (url.query != null) {
            sb.append("?")
            sb.append(url.query)
        }
        return sb.toString()
    }

    /**
     * Escape JavaString string
     * @param line unescaped string
     * @return escaped string
     */
    private fun escapeJavaScriptString(line: String): String {
        val sb = StringBuilder()
        for (c in line) {
            when (c) {
                '"', '\'', '\\' -> {
                    sb.append('\\')
                    sb.append(c)
                }
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
            .replace(U2028, "\u2028")
            .replace(U2029, "\u2029")
    }

    /**
     * Modified from String.replace(String, String, Boolean)
     */
    private fun String.replace(
        oldValue: String,
        newValue: String,
        every: Int,
        ignoreCase: Boolean = false
    ): String {
        run {
            var occurrenceIndex: Int = indexOf(oldValue, 0, ignoreCase)
            // FAST PATH: no match
            if (occurrenceIndex < 0) return this

            val oldValueLength = oldValue.length
            val searchStep = oldValueLength.coerceAtLeast(1)
            val newLengthHint = length - oldValueLength + newValue.length
            if (newLengthHint < 0) throw OutOfMemoryError()
            val stringBuilder = StringBuilder(newLengthHint)

            var count = 0
            var i = 0
            do {
                count++
                stringBuilder.append(this, i, occurrenceIndex)
                if (count == every) {
                    stringBuilder.append(newValue)
                    count = 0
                } else {
                    stringBuilder.append(oldValue)
                }
                i = occurrenceIndex + oldValueLength
                if (occurrenceIndex >= length) break
                occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, ignoreCase)
            } while (occurrenceIndex > 0)
            return stringBuilder.append(this, i, length).toString()
        }
    }

    companion object {
        private val U2028 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA8.toByte()))
        private val U2029 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA9.toByte()))

        private const val HIDING_CSS = "{display: none !important; visibility: hidden !important;}"
    }
}