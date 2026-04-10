package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.view.GestureType
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

class GestureTypePreference(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: GestureType = GestureType.NothingHappen,
) : ReadWriteProperty<Any, GestureType> {

    override fun getValue(thisRef: Any, property: KProperty<*>): GestureType =
        GestureType.from(sharedPreferences.getString(key, defaultValue.value) ?: defaultValue.value)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: GestureType) =
        sharedPreferences.edit { putString(key, value.value) }
}

fun kotlin.reflect.KMutableProperty0<Boolean>.toggle() = set(!get())
