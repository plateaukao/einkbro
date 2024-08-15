package info.plateaukao.einkbro.database

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Database(
    entities = [
        Bookmark::class,
        FaviconInfo::class,
        Highlight::class,
        Article::class,
        ChatGptQuery::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun faviconDao(): FaviconDao
    abstract fun highlightDao(): HighlightDao
    abstract fun articleDao(): ArticleDao
    abstract fun chatGptQueryDao(): ChatGptQueryDao
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `favicons` (`domain` TEXT NOT NULL, `icon` BLOB, PRIMARY KEY(`domain`))")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `highlights` (`articleId` INTEGER NOT NULL, `content` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, FOREIGN KEY(`articleId`) REFERENCES `articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS `articles` (`title` TEXT NOT NULL, `url` TEXT NOT NULL, `date` INTEGER NOT NULL, `tags` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_articleId` ON `highlights` (`articleId`)")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `chat_gpt_query` (`date` INTEGER NOT NULL, `url` TEXT NOT NULL, `model` TEXT NOT NULL, `selectedText` TEXT NOT NULL, `result` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles")
    suspend fun getAllArticlesAsync(): List<Article>

    @Query("SELECT * FROM articles WHERE url = :url")
    suspend fun getArticleByUrl(url: String): Article?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: Article): Long

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Int): Article?

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: Int)

    @Transaction
    suspend fun insertAndGetArticle(article: Article): Article {
        val id = insert(article)
        return getArticleById(id.toInt())!!
    }


    @Delete
    suspend fun delete(article: Article)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE articleId = :articleId")
    fun getHighlightsForArticle(articleId: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE articleId = :articleId")
    suspend fun getHighlightsForArticleIdAsync(articleId: Int): List<Highlight>

    fun getHighlightsForArticle(article: Article): Flow<List<Highlight>> =
        getHighlightsForArticle(article.id)

    suspend fun getHighlightsForArticleAsync(articleId: Int): List<Highlight> =
        getHighlightsForArticleIdAsync(articleId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: Highlight)

    @Delete
    suspend fun delete(highlight: Highlight)

    @Query("DELETE FROM highlights")
    suspend fun deleteAll()
}

@Dao
interface FaviconDao {
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

    @Transaction
    suspend fun overwrite(bookmarks: List<Bookmark>) {
        deleteAll()
        bookmarks.forEach { insert(it) }
    }
}

class BookmarkManager(context: Context) : KoinComponent {
    val config: ConfigManager by inject()

    val database = Room.databaseBuilder(context, AppDatabase::class.java, "einkbro_db")
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .build()

    val bookmarkDao = database.bookmarkDao()

    private val faviconDao = database.faviconDao()

    private val highlightDao = database.highlightDao()
    private val articleDao = database.articleDao()
    private val chatGptQueryDao = database.chatGptQueryDao()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            faviconInfos.addAll(getAllFavicons())
        }
    }

    private val faviconInfos: MutableList<FaviconInfo> = mutableListOf()

    private suspend fun getAllFavicons(): List<FaviconInfo> = faviconDao.getAllFavicons()

    suspend fun insertArticle(article: Article): Article = articleDao.insertAndGetArticle(article)
    suspend fun insertHighlight(highlight: Highlight) = highlightDao.insert(highlight)

    suspend fun deleteArticle(articleId: Int) = articleDao.deleteArticleById(articleId)
    suspend fun deleteArticle(article: Article) = articleDao.delete(article)

    suspend fun deleteHighlight(highlight: Highlight) =
        highlightDao.delete(highlight)

    fun getAllArticles(): Flow<List<Article>> = articleDao.getAllArticles()
    suspend fun getAllArticlesAsync(): List<Article> = articleDao.getAllArticlesAsync()

    suspend fun getArticle(articleId: Int): Article? = articleDao.getArticleById(articleId)

    fun getHighlightsForArticle(article: Article): Flow<List<Highlight>> =
        highlightDao.getHighlightsForArticle(article)

    suspend fun getHighlightsForArticleAsync(articleId: Int): List<Highlight> =
        highlightDao.getHighlightsForArticleAsync(articleId)

    fun getHighlightsForArticle(articleId: Int): Flow<List<Highlight>> =
        highlightDao.getHighlightsForArticle(articleId)

    suspend fun getArticleByUrl(url: String): Article? = articleDao.getArticleByUrl(url)

    suspend fun insertFavicon(faviconInfo: FaviconInfo) {
        faviconDao.insert(faviconInfo)
        faviconInfos.add(faviconInfo)
    }

    @Synchronized
    fun findFaviconBy(url: String): FaviconInfo? {
        val host = Uri.parse(url).host ?: return null
        synchronized(faviconInfos) {
            return faviconInfos.firstOrNull { it.domain == host }
        }
    }

    suspend fun deleteFavicon(faviconInfo: FaviconInfo) = faviconDao.delete(faviconInfo)

    suspend fun getAllBookmarks(): List<Bookmark> = bookmarkDao.getAllBookmarks()

    suspend fun getAllBookmarksOnly(): List<Bookmark> = bookmarkDao.getAllBookmarksOnly()

    suspend fun getBookmarks(parentId: Int = 0): List<Bookmark> =
        bookmarkDao.getBookmarksByParent(parentId)

    suspend fun getBookmarksByParent(parent: Int) = bookmarkDao.getBookmarksByParent(parent)

    suspend fun getBookmarkFolders(): List<Bookmark> = bookmarkDao.getBookmarkFolders()

    suspend fun insert(bookmark: Bookmark) = bookmarkDao.insert(bookmark)

    suspend fun insert(title: String, url: String) {
        if (existsUrl(url)) return

        bookmarkDao.insert(Bookmark(title, url))
    }

    suspend fun deleteAll() = bookmarkDao.deleteAll()

    private suspend fun existsUrl(url: String): Boolean = bookmarkDao.existsUrl(url) > 0

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
    suspend fun overwriteBookmarks(bookmarks: List<Bookmark>) {
        if (bookmarks.isNotEmpty()) bookmarkDao.overwrite(bookmarks)
    }

    fun getAllChatGptQueries(): Flow<List<ChatGptQuery>> = chatGptQueryDao.getAllChatGptQueries()
    suspend fun addChatGptQuery(chatGptQuery: ChatGptQuery) = chatGptQueryDao.addChatGptQuery(chatGptQuery)
    suspend fun getChatGptQueryById(id: Int): ChatGptQuery = chatGptQueryDao.getChatGptQueryById(id)
    suspend fun deleteChatGptQuery(chatGptQuery: ChatGptQuery) = chatGptQueryDao.deleteChatGptQuery(chatGptQuery)
}