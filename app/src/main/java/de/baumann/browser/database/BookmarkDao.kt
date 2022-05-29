package de.baumann.browser.database

import android.content.Context
import android.graphics.Bitmap
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.baumann.browser.preference.ConfigManager
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Database(entities = [Bookmark::class, FaviconInfo::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun faviconDao(): FaviconDao
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `favicons` (`domain` TEXT NOT NULL, `icon` BLOB, PRIMARY KEY(`domain`))")
    }
}

@Dao
interface  FaviconDao {
    @Query("SELECT * FROM favicons")
    suspend fun getAllFavicons(): List<FaviconInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(faviconInfo: FaviconInfo)

    @Delete
    suspend fun delete(faviconInfo: FaviconInfo)

    @Query("SELECT * FROM favicons WHERE domain = :host")
    suspend fun findBy(host: String): List<FaviconInfo>
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllBookmarks(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE isDirectory = 0 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllBookmarksOnly(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE isDirectory = 1 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getBookmarkFolders(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE parent = :parentId ORDER BY title COLLATE NOCASE ASC")
    suspend fun getBookmarksByParent(parentId: Int): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE parent = :parentId ORDER BY title COLLATE NOCASE ASC")
    fun getBookmarksByParentFlow(parentId: Int): Flow<List<Bookmark>>

    @Query("SELECT COUNT(id) FROM bookmarks WHERE url = :url")
    suspend fun existsUrl(url: String): Int

    @Query("SELECT * FROM bookmarks WHERE url = :url")
    suspend fun findBy(url: String): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Update
    suspend fun update(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

class BookmarkManager(private val context: Context) : KoinComponent {
    val config: ConfigManager by inject()

    val database = Room.databaseBuilder(context, AppDatabase::class.java, "einkbro_db")
            .addMigrations(MIGRATION_1_2)
            .build()

    val bookmarkDao = database.bookmarkDao()

    val faviconDao = database.faviconDao()

    suspend fun migrateOldData() {
        if (config.dbVersion != 0) return

        //add bookmarks
        val db = BookmarkList(context).apply { open() }
        val cursor = db.fetchAllData(context)
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            insert(
                    title = cursor.getString(1),
                    url = cursor.getString(2),
            )
            cursor.moveToNext()
        }
        cursor.close()
        db.close()

        config.dbVersion = 1
    }

    suspend fun getAllFavicons(): List<FaviconInfo> = faviconDao.getAllFavicons()

    suspend fun insertFavicon(faviconInfo: FaviconInfo) = faviconDao.insert(faviconInfo)

    suspend fun findFaviconBy(url: String): List<FaviconInfo> = faviconDao.findBy(url)

    suspend fun deleteFavicon(faviconInfo: FaviconInfo) = faviconDao.delete(faviconInfo)

    suspend fun getAllBookmarks(): List<Bookmark> = bookmarkDao.getAllBookmarks()

    suspend fun getAllBookmarksOnly(): List<Bookmark> = bookmarkDao.getAllBookmarks()

    suspend fun getBookmarks(parentId: Int = 0): List<Bookmark> = bookmarkDao.getBookmarksByParent(parentId)

    suspend fun getBookmarksByParent(parent: Int) = bookmarkDao.getBookmarksByParent(parent)

    suspend fun getBookmarkFolders(): List<Bookmark> = bookmarkDao.getBookmarkFolders()

    suspend fun insert(bookmark: Bookmark) = bookmarkDao.insert(bookmark)

    suspend fun insert(title: String, url: String) {
        if (existsUrl(url)) return

        bookmarkDao.insert(Bookmark(title, url))
    }

    suspend fun deleteAll() = bookmarkDao.deleteAll()

    suspend fun existsUrl(url: String): Boolean = bookmarkDao.existsUrl(url) > 0

    suspend fun findBy(url: String): List<Bookmark> = bookmarkDao.findBy(url)

    suspend fun insertDirectory(title: String, parentId: Int = 0) {
        bookmarkDao.insert(
                Bookmark(
                        title = title,
                        url = "",
                        isDirectory = true,
                        parent = parentId,
                )
        )
    }

    suspend fun delete(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    suspend fun update(bookmark: Bookmark) = bookmarkDao.update(bookmark)

    companion object {
        const val BOOKMARK_ROOT_FOLDER_ID = 0
    }

}