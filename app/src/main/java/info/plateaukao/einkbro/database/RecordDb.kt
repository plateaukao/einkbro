package info.plateaukao.einkbro.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.RecordUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecordDb(
    context: Context
) : KoinComponent {
    private val helper: RecordHelper = RecordHelper(context)
    private val database: SQLiteDatabase by lazy { helper.writableDatabase }
    private val bookmarkManager: BookmarkManager by inject()
    private val config: ConfigManager by inject()

    suspend fun addHistory(record: Record) {
        if (record.url.startsWith("data:")) return

        // optimize loading page speed. when no showing recent bookmarks don't update it.
        val bookmarks = bookmarkManager.findBy(record.url)
        if (bookmarks.isNotEmpty()) {
            config.addRecentBookmark(bookmarks.first())
        }

        database.transaction {
            // FIXME: there's no index for the table, so don't check url existence. just add it
//            if (checkHistory(record.url)) {
//                deleteHistoryItemByURL(record.url)
//            }

            internalAddHistory(record)
            purgeOldHistoryItem(14)
        }
    }

    private fun internalAddHistory(record: Record) {
        if (record.title == null || record.title.trim { it <= ' ' }.isEmpty()
            || record.url == null || record.url.trim { it <= ' ' }.isEmpty()
            || record.time < 0L
        ) {
            return
        }

        val values = ContentValues().apply {
            put(RecordUnit.COLUMN_TITLE, record.title.trim { it <= ' ' })
            put(RecordUnit.COLUMN_URL, record.url.trim { it <= ' ' })
            put(RecordUnit.COLUMN_TIME, record.time)
        }
        database.insert(RecordUnit.TABLE_HISTORY, null, values)
    }

    fun addDomain(domain: String?, table: String?) {
        if (domain == null || domain.trim { it <= ' ' }.isEmpty()) {
            return
        }
        val values = ContentValues().apply {
            put(RecordUnit.COLUMN_DOMAIN, domain.trim { it <= ' ' })
        }
        database.insert(table, null, values)
    }


    fun checkDomain(domain: String?, table: String?): Boolean {
        if (domain == null || domain.trim { it <= ' ' }.isEmpty()) {
            return false
        }
        val cursor = database.query(
            table,
            arrayOf(RecordUnit.COLUMN_DOMAIN),
            RecordUnit.COLUMN_DOMAIN + "=?",
            arrayOf(domain.trim { it <= ' ' }),
            null,
            null,
            null
        )
        if (cursor != null) {
            val result = cursor.moveToFirst()
            cursor.close()
            return result
        }
        return false
    }

    private fun purgeOldHistoryItem(days: Int) {
        val currentTimestamp = System.currentTimeMillis()
        // if it's purged within two days, don't do it again
        if (currentTimestamp - config.purgeHistoryTimestamp < (1000 * 60 * 60 * 24 * 2)) return

        config.purgeHistoryTimestamp = currentTimestamp
        val tsBefore = currentTimestamp - (1000 * 60 * 60 * 24) * days
        val sql =
            "DELETE FROM ${RecordUnit.TABLE_HISTORY} WHERE ${RecordUnit.COLUMN_TIME} <= $tsBefore"
        database.execSQL(sql)
    }

    fun deleteHistoryItem(record: Record?) {
        if (record == null || record.time <= 0) {
            return
        }
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_TIME + " = " + record.time)
    }

    fun deleteDomain(domain: String?, table: String) {
        if (domain == null || domain.trim { it <= ' ' }.isEmpty()) {
            return
        }
        database.execSQL("DELETE FROM " + table + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim { it <= ' ' } + "\"")
    }

    fun clearHistory() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_HISTORY)
    }

    fun deleteAllDomains(dbTable: String) {
        database.execSQL("DELETE FROM $dbTable")
    }

    private fun getRecord(cursor: Cursor, type: RecordType = RecordType.History): Record {
        return Record(
            cursor.getString(0),
            cursor.getString(1),
            cursor.getLong(2),
            type
        )
    }

    suspend fun listEntries(listAll: Boolean, amount: Int = 0): List<Record> {
        val list: MutableList<Record> = ArrayList()
        if (listAll) {
            //add bookmarks
            bookmarkManager.getAllBookmarksOnly().forEach { list.add(it.toRecord()) }
        }

        //add history
        val cursor = database.query(
            RecordUnit.TABLE_HISTORY, arrayOf(
                RecordUnit.COLUMN_TITLE,
                RecordUnit.COLUMN_URL,
                RecordUnit.COLUMN_TIME
            ),
            null,
            null,
            null,
            null,
            RecordUnit.COLUMN_TIME + (if (config.isToolbarOnTop) " asc" else " desc")
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val record = getRecord(cursor, RecordType.History)
            if (!list.contains(record)) {
                list.add(record)
            }
            cursor.moveToNext()
        }
        cursor.close()

        return if (amount == 0) {
            list
        } else {
            list.take(amount)
        }
    }

    fun listDomains(table: String?): List<String> {
        val list: MutableList<String> = ArrayList()
        val cursor = database.query(
            table, arrayOf(RecordUnit.COLUMN_DOMAIN),
            null,
            null,
            null,
            null,
            RecordUnit.COLUMN_DOMAIN
        ) ?: return list
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            list.add(cursor.getString(0))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    fun close() = database.close()
}

private fun Bookmark.toRecord(): Record = Record(
    title = this.title,
    url = this.url,
    time = 0,
    type = RecordType.Bookmark
)