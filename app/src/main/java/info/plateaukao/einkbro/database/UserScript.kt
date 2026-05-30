package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Tampermonkey-style userscript. The full script text (including the
 * `// ==UserScript== ... // ==/UserScript==` metadata block) is stored verbatim
 * in [code]; metadata is parsed on demand by UserScriptManager.
 */
@Entity(tableName = "user_scripts")
data class UserScript(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String,
    var enabled: Boolean = true,
    var code: String,
    var sourceUrl: String? = null,
    var order: Int = 0,
)

/**
 * Persistent key/value storage backing GM_setValue / GM_getValue, scoped per script.
 */
@Entity(tableName = "user_script_values", primaryKeys = ["scriptId", "key"])
data class UserScriptValue(
    var scriptId: Long,
    var key: String,
    var value: String,
)
