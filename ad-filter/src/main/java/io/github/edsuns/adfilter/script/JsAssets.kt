package io.github.edsuns.adfilter.script

import android.content.Context

/**
 * Loads the injected-JS sources from this module's assets. They used to be
 * inlined into the dex string pool by Mezzanine (kapt): dex strings are stored
 * uncompressed both in the APK and on device, while assets deflate to about a
 * third of the size — and dropping Mezzanine removed the module's last kapt
 * dependency. Sources are cached after first read; the big ones (scriptlets,
 * extended-css) are only read when a page actually needs them.
 */
internal object JsAssets {
    private lateinit var appContext: Context
    private val cache = HashMap<String, String>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun load(name: String): String = cache.getOrPut(name) {
        appContext.assets.open("adfilter/$name").bufferedReader().use { it.readText() }
    }
}
