package io.github.edsuns.adfilter.script

import com.anthonycr.mezzanine.FileStream
import com.anthonycr.mezzanine.MezzanineGenerator
import io.github.edsuns.smoothprogress.BuildConfig

/**
 * Created by Edsuns@qq.com on 2021/4/3.
 */
internal object ScriptInjection {

    @FileStream("src/main/js/inject.js")
    interface Injection {
        fun js(): String
    }

    private const val INJECTION = "{{INJECTION}}"
    private const val DEBUG_FLAG = "{{DEBUG}}"
    private const val JS_BRIDGE = "{{BRIDGE}}"

    private val injectJS = parse(MezzanineGenerator.Injection().js())

    private val bridgeRegister = arrayListOf(
        ElementHiding::class.java,
        Scriptlet::class.java
    )

    private val bridgeNamePrefix = randomAlphanumericString()

    fun bridgeNameFor(owner: Any): String {
        val clazz = owner::class.java
        val index = bridgeRegister.indexOf(clazz)
        if (index < 0) {
            error("$clazz isn't registered as a bridge!")
        }
        return bridgeNamePrefix + index
    }

    private fun randomAlphanumericString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val outputStrLength = (10..36).shuffled().first()

        return (1..outputStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun parse(raw: String, bridgeName: String? = null): String {
        var js = raw.replace(DEBUG_FLAG, if (BuildConfig.DEBUG) "" else "//")
        if (bridgeName != null) {
            js = js.replace(JS_BRIDGE, bridgeName)
        }
        return js
    }

    fun parseScript(owner: Any, raw: String, wrapper: Boolean = false): String {
        val js = parse(raw, bridgeNameFor(owner))
        return if (wrapper) injectJS.replace(INJECTION, js) else js
    }
}