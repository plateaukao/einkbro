package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.browser.BrowserActionCatalog
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Boolean = false,
) : ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}

class IntPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Int = 0,
) : ReadWriteProperty<Any, Int> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Int =
        sharedPreferences.getInt(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
        sharedPreferences.edit { putInt(key, value) }
}

class StringPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: String = "",
) : ReadWriteProperty<Any, String> {

    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        sharedPreferences.getString(key, defaultValue) ?: defaultValue

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) =
        sharedPreferences.edit { putString(key, value) }
}

class BrowserActionPreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: BrowserAction = BrowserAction.Noop,
) : ReadWriteProperty<Any, BrowserAction> {

    override fun getValue(thisRef: Any, property: KProperty<*>): BrowserAction {
        val stored = sharedPreferences.getString(key, null)
        val id = BrowserActionCatalog.migrateLegacyId(stored)
        if (stored != null && stored != id) {
            // rewrite legacy value to the new format
            sharedPreferences.edit { putString(key, id) }
        }
        return BrowserActionCatalog.entryOf(
            id.ifEmpty { BrowserActionCatalog.idOf(defaultValue) }
        ).action
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: BrowserAction) =
        sharedPreferences.edit { putString(key, BrowserActionCatalog.idOf(value)) }
}

fun kotlin.reflect.KMutableProperty0<Boolean>.toggle() = set(!get())
