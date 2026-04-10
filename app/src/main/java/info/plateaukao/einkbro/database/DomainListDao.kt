package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DomainListDao {
    // -- Whitelist --
    @Query("SELECT DOMAIN FROM WHITELIST ORDER BY DOMAIN")
    suspend fun getAllWhitelistDomains(): List<String>

    @Query("SELECT COUNT(*) FROM WHITELIST WHERE DOMAIN = :domain")
    suspend fun whitelistContains(domain: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelist(entry: WhitelistDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWhitelistSync(entry: WhitelistDomain)

    @Query("DELETE FROM WHITELIST WHERE DOMAIN = :domain")
    suspend fun deleteWhitelist(domain: String)

    @Query("DELETE FROM WHITELIST")
    suspend fun deleteAllWhitelist()

    // -- Javascript --
    @Query("SELECT DOMAIN FROM JAVASCRIPT ORDER BY DOMAIN")
    suspend fun getAllJavascriptDomains(): List<String>

    @Query("SELECT COUNT(*) FROM JAVASCRIPT WHERE DOMAIN = :domain")
    suspend fun javascriptContains(domain: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJavascript(entry: JavascriptDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJavascriptSync(entry: JavascriptDomain)

    @Query("DELETE FROM JAVASCRIPT WHERE DOMAIN = :domain")
    suspend fun deleteJavascript(domain: String)

    @Query("DELETE FROM JAVASCRIPT")
    suspend fun deleteAllJavascript()

    // -- Cookie --
    @Query("SELECT DOMAIN FROM COOKIE ORDER BY DOMAIN")
    suspend fun getAllCookieDomains(): List<String>

    @Query("SELECT COUNT(*) FROM COOKIE WHERE DOMAIN = :domain")
    suspend fun cookieContains(domain: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookie(entry: CookieDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCookieSync(entry: CookieDomain)

    @Query("DELETE FROM COOKIE WHERE DOMAIN = :domain")
    suspend fun deleteCookie(domain: String)

    @Query("DELETE FROM COOKIE")
    suspend fun deleteAllCookie()
}
