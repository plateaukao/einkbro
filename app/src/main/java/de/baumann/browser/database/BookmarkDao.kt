package de.baumann.browser.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import de.baumann.browser.preference.ConfigManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Database(entities = [Bookmark::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE isDirectory != 1")
    fun getAllBookmarks(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE parent = :parentId")
    fun getBookmarksByParent(parentId: Int): List<Bookmark>

    @Query("SELECT COUNT(id) FROM bookmarks WHERE url = :url")
    fun existsUrl(url: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: Bookmark)

    @Delete
    fun delete(bookmark: Bookmark)

    @Update
    fun update(bookmark: Bookmark)
}

class BookmarkManger(private val context: Context) {
    val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "einkbro_db"
    ).build()

    val bookmarkDao = database.bookmarkDao()

    fun migrateOldData() {
        val config = ConfigManager(context)
        if (config.dbVersion != 1) return

        GlobalScope.launch {

            //add bookmarks
            val db = BookmarkList(context).apply { open() }
            val cursor = db.fetchAllData(context)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                insert(
                    Bookmark(
                        title = cursor.getString(1),
                        url = cursor.getString(2),
                        isDirectory = false,
                        parent = 0
                    )
                )
                cursor.moveToNext()
            }
            cursor.close()
            db.close()
        }

        config.dbVersion = 1
    }

    fun getBookmarks(): List<Bookmark> = bookmarkDao.getAllBookmarks()

    fun getBookmarksByParent(parent: Int) = bookmarkDao.getBookmarksByParent(parent)

    fun insert(bookmark: Bookmark) {
        if (bookmarkDao.existsUrl(bookmark.url) > 0) return

        bookmarkDao.insert(bookmark)
    }

    fun insertDirectory(title: String, parentId: Int = 0) {
        bookmarkDao.insert(
            Bookmark(
                title = title,
                url = "",
                isDirectory = true,
                parent = parentId,
            )
        )
    }

    fun delete(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    fun update(bookmark: Bookmark) = bookmarkDao.update(bookmark)
}