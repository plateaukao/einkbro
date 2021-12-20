package de.baumann.browser.browser

import kotlin.jvm.Synchronized
import de.baumann.browser.database.RecordDb
import de.baumann.browser.unit.RecordUnit
import android.annotation.SuppressLint
import android.content.Context
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.util.DebugT
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.Throws

class AdBlock(private val context: Context): KoinComponent {
    private val config: ConfigManager by inject()

    fun isWhite(url: String?): Boolean {
        for (domain in whitelist) {
            if (url != null && url.contains(domain!!)) {
                return true
            }
        }
        return false
    }

    fun isAd(url: String): Boolean {
        val domain: String = try {
            getDomain(url).lowercase(locale)
        } catch (u: URISyntaxException) {
            return false
        }
        return hosts.contains(domain) || isAdExtraSites(url)
    }

    private fun isAdExtraSites(url: String): Boolean {
        return config.adSites.any { url.contains(it, true) }
    }

    @Synchronized
    fun addDomain(domain: String?) {
        with(RecordDb(context)) {
            open(true)
            addDomain(domain, RecordUnit.TABLE_WHITELIST)
            close()
        }
        whitelist.add(domain)
    }

    @Synchronized
    fun removeDomain(domain: String?) {
        with(RecordDb(context)) {
            open(true)
            deleteDomain(domain, RecordUnit.TABLE_WHITELIST)
            close()
        }
        whitelist.remove(domain)
    }

    @Synchronized
    fun clearDomains() {
        with(RecordDb(context)) {
            open(true)
            clearDomains()
            close()
        }
        whitelist.clear()
    }

    private fun loadHosts(context: Context) {
        val thread = Thread {
            val debugT = DebugT("loadHosts")
            val manager = context.assets
            try {
                val reader = BufferedReader(InputStreamReader(manager.open(FILE)))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { hosts.add(it.lowercase(locale)) }
                }
            } catch (i: IOException) {
                Log.w("browser", "Error loading hosts", i)
            }
            debugT.printTime()
        }
        thread.start()
    }


    companion object {
        private const val FILE = "hosts.txt"
        private val hosts: MutableSet<String> = HashSet()
        private val whitelist: MutableList<String?> = ArrayList()

        @SuppressLint("ConstantLocale")
        private val locale = Locale.getDefault()
        @Synchronized
        private fun loadDomains(context: Context) {
            val action = RecordDb(context)
            action.open(false)
            whitelist.clear()
            whitelist.addAll(action.listDomains(RecordUnit.TABLE_WHITELIST))
            action.close()
        }

        @Throws(URISyntaxException::class)
        private fun getDomain(url: String): String {
            var url = url
            url = url.lowercase(locale)
            val index = url.indexOf('/', 8) // -> http://(7) and https://(8)
            if (index != -1) {
                url = url.substring(0, index)
            }
            val uri = URI(url)
            val domain = uri.host ?: return url
            return if (domain.startsWith("www.")) domain.substring(4) else domain
        }
    }

    init {
        if (hosts.isEmpty()) {
            loadHosts(context)
        }
        loadDomains(context)
    }
}