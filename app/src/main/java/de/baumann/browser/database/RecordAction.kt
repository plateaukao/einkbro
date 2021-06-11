package de.baumann.browser.database

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.baumann.browser.unit.RecordUnit
import java.util.*

class RecordAction(context: Context?) {
    private lateinit var database: SQLiteDatabase
    private val helper: RecordHelper = RecordHelper(context)

    fun open(rw: Boolean) {
        database = if (rw) helper.writableDatabase else helper.readableDatabase
    }

    fun close() {
        helper.close()
    }

    fun addHistory(record: Record?) {
        if (record?.title == null || record.title.trim { it <= ' ' }.isEmpty()
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

    fun checkHistory(url: String?): Boolean {
        if (url == null || url.trim { it <= ' ' }.isEmpty()) {
            return false
        }

        val cursor = database.query(
            RecordUnit.TABLE_HISTORY,
            arrayOf(RecordUnit.COLUMN_URL),
            RecordUnit.COLUMN_URL + "=?",
            arrayOf(url.trim { it <= ' ' }),
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

    fun deleteHistoryItemByURL(domain: String?) {
        if (domain == null || domain.trim { it <= ' ' }.isEmpty()) {
            return
        }
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim { it <= ' ' } + "\"")
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

    fun clearHome() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_GRID)
    }

    fun clearHistory() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_HISTORY)
    }

    fun clearDomains() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_WHITELIST)
    }

    fun clearDomainsJS() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_JAVASCRIPT)
    }

    fun clearDomainsCookie() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_COOKIE)
    }

    private fun getRecord(cursor: Cursor, type: RecordType = RecordType.History): Record {
        return Record(
            cursor.getString(0),
            cursor.getString(1),
            cursor.getLong(2),
            type
        )
    }

    suspend fun listEntries(activity: Activity, listAll: Boolean): List<Record> {
        val bookmarkManager = BookmarkManager(activity)
        val list: MutableList<Record> = ArrayList()
        if (listAll) {
            //add bookmarks
            bookmarkManager.getAllBookmarksOnly().forEach { list.add(it.toRecord()) }
            bookmarkManager.release()
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
            RecordUnit.COLUMN_TIME + " desc"
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
        return list
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
}

private fun Bookmark.toRecord(): Record = Record(
    title = this.title,
    url = this.url,
    time = 0,
    type = RecordType.Bookmark
)