package info.plateaukao.einkbro.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import info.plateaukao.einkbro.database.RecordRepository
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class AdBlock(context: Context) : BaseWebConfig(context) {
    override val dbTable: String = RecordRepository.TABLE_WHITELIST
    override val hostsFile: String = "hosts.txt"
}

class Javascript(context: Context) : BaseWebConfig(context) {
    override val dbTable: String = RecordRepository.TABLE_JAVASCRIPT
    override val hostsFile: String = "javaHosts.txt"
}

class Cookie(context: Context) : BaseWebConfig(context) {
    override val dbTable: String = RecordRepository.TABLE_COOKIE
    override val hostsFile: String = "cookieHosts.txt"
}

abstract class BaseWebConfig(private val context: Context) : KoinComponent, DomainInterface {
    private val config: ConfigManager by inject()
    private val recordDb: RecordRepository by inject()
    private val coroutineScope: CoroutineScope by inject()
    protected val hosts: MutableSet<String> = HashSet()
    private val whitelist: MutableList<String> = ArrayList()

    @SuppressLint("ConstantLocale")
    private val locale = Locale.getDefault()

    abstract val dbTable: String
    abstract val hostsFile: String

    init {
        coroutineScope.launch {
            loadDomains()
            loadHosts(hostsFile)
        }
    }

    fun isWhite(url: String): Boolean {
        for (domain in whitelist) {
            if (url.contains(domain)) {
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
        return hosts.contains(domain) || isAdExtraSites(domain)
    }

    private fun isAdExtraSites(domain: String): Boolean {
        return config.browser.adSites.any { it.contains(domain, true) }
    }

    override suspend fun getDomains() = recordDb.listDomains(dbTable)

    override suspend fun addDomain(domain: String) {
        recordDb.addDomain(domain, dbTable)
        whitelist.add(domain)
    }

    override suspend fun deleteDomain(domain: String) {
        recordDb.deleteDomain(domain, dbTable)
        whitelist.remove(domain)
    }

    override suspend fun deleteAllDomains() {
        recordDb.deleteAllDomains(dbTable)
        whitelist.clear()
    }

    private suspend fun loadHosts(filename: String) {
        withContext(Dispatchers.IO) {
            val manager = context.assets
            try {
                val reader = BufferedReader(InputStreamReader(manager.open(filename)))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { hosts.add(it.lowercase(locale)) }
                }
            } catch (i: IOException) {
                Log.w("browser", "Error loading hosts", i)
            }
        }
    }

    private suspend fun loadDomains() {
        val domains = recordDb.listDomains(dbTable)
        synchronized(whitelist) {
            whitelist.clear()
            whitelist.addAll(domains)
        }
    }

    @Throws(URISyntaxException::class)
    private fun getDomain(url: String): String {
        var url = url.lowercase(locale)
        val index = url.indexOf('/', 8) // -> http://(7) and https://(8)
        if (index != -1) {
            url = url.substring(0, index)
        }
        val uri = URI(url)
        val domain = uri.host ?: return url
        return if (domain.startsWith("www.")) domain.substring(4) else domain
    }
}
