package info.plateaukao.einkbro.database

import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecordRepository : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val config: ConfigManager by inject()

    private val historyDao: HistoryDao by lazy { bookmarkManager.database.historyDao() }
    private val domainListDao: DomainListDao by lazy { bookmarkManager.database.domainListDao() }

    suspend fun addHistory(record: Record) {
        if (record.url.startsWith("data:")) return

        val bookmarks = bookmarkManager.findBy(record.url)
        if (bookmarks.isNotEmpty()) {
            config.addRecentBookmark(bookmarks.first())
        }

        if (record.title.isNullOrBlank() || record.url.isBlank() || record.time < 0L) return

        historyDao.insert(
            HistoryRecord(
                TITLE = record.title.trim(),
                URL = record.url.trim(),
                TIME = record.time,
            )
        )
        purgeOldHistoryItem(14)
    }

    private suspend fun purgeOldHistoryItem(days: Int) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - config.tab.purgeHistoryTimestamp < (1000L * 60 * 60 * 24 * 2)) return

        config.tab.purgeHistoryTimestamp = currentTimestamp
        val tsBefore = currentTimestamp - (1000L * 60 * 60 * 24) * days
        historyDao.deleteOlderThan(tsBefore)
    }

    suspend fun deleteHistoryItem(record: Record?) {
        if (record == null || record.time <= 0) return
        withContext(Dispatchers.IO) {
            historyDao.deleteByTime(record.time)
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            historyDao.deleteAll()
        }
    }

    suspend fun listEntries(listAll: Boolean, amount: Int = 0): List<Record> {
        val list = mutableListOf<Record>()
        if (listAll) {
            bookmarkManager.getAllBookmarksOnly().forEach { list.add(it.toRecord()) }
        }

        val history = withContext(Dispatchers.IO) { historyDao.getAllHistory() }
        for (hr in history) {
            val record = hr.toRecord()
            if (!list.contains(record)) {
                list.add(record)
            }
        }

        return if (amount == 0) list else list.take(amount)
    }

    suspend fun listAllHistory(): List<Record> {
        return withContext(Dispatchers.IO) {
            historyDao.getAllHistory().map { it.toRecord() }
        }
    }

    suspend fun replaceAllHistory(records: List<Record>) {
        withContext(Dispatchers.IO) {
            val entities = records
                .filter { !it.title.isNullOrBlank() && it.url.isNotBlank() && it.time >= 0L }
                .map {
                    HistoryRecord(
                        TITLE = it.title!!.trim(),
                        URL = it.url.trim(),
                        TIME = it.time,
                    )
                }
            historyDao.replaceAll(entities)
        }
    }

    // -- Domain lists --

    suspend fun addDomain(domain: String?, table: String?) {
        if (domain.isNullOrBlank() || table == null) return
        val trimmed = domain.trim()
        withContext(Dispatchers.IO) {
            when (table) {
                TABLE_WHITELIST -> domainListDao.insertWhitelist(WhitelistDomain(trimmed))
                TABLE_JAVASCRIPT -> domainListDao.insertJavascript(JavascriptDomain(trimmed))
                TABLE_COOKIE -> domainListDao.insertCookie(CookieDomain(trimmed))
            }
        }
    }

    suspend fun checkDomain(domain: String?, table: String?): Boolean {
        if (domain.isNullOrBlank() || table == null) return false
        val trimmed = domain.trim()
        return withContext(Dispatchers.IO) {
            when (table) {
                TABLE_WHITELIST -> domainListDao.whitelistContains(trimmed) > 0
                TABLE_JAVASCRIPT -> domainListDao.javascriptContains(trimmed) > 0
                TABLE_COOKIE -> domainListDao.cookieContains(trimmed) > 0
                else -> false
            }
        }
    }

    suspend fun deleteDomain(domain: String?, table: String) {
        if (domain.isNullOrBlank()) return
        val trimmed = domain.trim()
        withContext(Dispatchers.IO) {
            when (table) {
                TABLE_WHITELIST -> domainListDao.deleteWhitelist(trimmed)
                TABLE_JAVASCRIPT -> domainListDao.deleteJavascript(trimmed)
                TABLE_COOKIE -> domainListDao.deleteCookie(trimmed)
            }
        }
    }

    suspend fun deleteAllDomains(dbTable: String) {
        withContext(Dispatchers.IO) {
            when (dbTable) {
                TABLE_WHITELIST -> domainListDao.deleteAllWhitelist()
                TABLE_JAVASCRIPT -> domainListDao.deleteAllJavascript()
                TABLE_COOKIE -> domainListDao.deleteAllCookie()
            }
        }
    }

    suspend fun listDomains(table: String?): List<String> {
        if (table == null) return emptyList()
        return withContext(Dispatchers.IO) {
            when (table) {
                TABLE_WHITELIST -> domainListDao.getAllWhitelistDomains()
                TABLE_JAVASCRIPT -> domainListDao.getAllJavascriptDomains()
                TABLE_COOKIE -> domainListDao.getAllCookieDomains()
                else -> emptyList()
            }
        }
    }

    companion object {
        const val TABLE_WHITELIST = "WHITELIST"
        const val TABLE_JAVASCRIPT = "JAVASCRIPT"
        const val TABLE_COOKIE = "COOKIE"
    }
}

private fun Bookmark.toRecord(): Record = Record(
    title = this.title,
    url = this.url,
    time = 0,
    type = RecordType.Bookmark,
)
