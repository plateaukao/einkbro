package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM user_scripts ORDER BY `order` ASC, id ASC")
    suspend fun getAll(): List<UserScript>

    @Query("SELECT * FROM user_scripts WHERE id = :id")
    suspend fun getById(id: Long): UserScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userScript: UserScript): Long

    @Update
    suspend fun update(userScript: UserScript)

    @Delete
    suspend fun delete(userScript: UserScript)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_scripts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(userScripts: List<UserScript>)
}

@Dao
interface UserScriptValueDao {
    @Query("SELECT value FROM user_script_values WHERE scriptId = :scriptId AND key = :key")
    fun getValue(scriptId: Long, key: String): String?

    @Query("SELECT key FROM user_script_values WHERE scriptId = :scriptId")
    fun listKeys(scriptId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setValue(value: UserScriptValue)

    @Query("DELETE FROM user_script_values WHERE scriptId = :scriptId AND key = :key")
    fun deleteValue(scriptId: Long, key: String)

    @Query("DELETE FROM user_script_values WHERE scriptId = :scriptId")
    fun deleteAllForScript(scriptId: Long)
}
