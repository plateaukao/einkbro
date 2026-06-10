package info.plateaukao.einkbro.preference

import android.content.SharedPreferences

/**
 * In-memory [SharedPreferences] for JVM unit tests.
 *
 * Unlike a mockk stub it gives real read-after-write semantics, so tests can
 * exercise full round trips through the *Config classes' custom getters and
 * setters. Type mismatches throw [ClassCastException], matching the behavior
 * of the real SharedPreferencesImpl (some production code relies on that,
 * e.g. DisplayConfig.einkImageAdjustment's boolean->int migration).
 */
class FakeSharedPreferences : SharedPreferences {

    val store: MutableMap<String, Any?> = LinkedHashMap()

    override fun getAll(): MutableMap<String, *> = HashMap(store)

    override fun getString(key: String?, defValue: String?): String? {
        val value = store[key] ?: return defValue
        return value as String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val value = store[key] ?: return defValues
        return (value as Set<String>).toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val value = store[key] ?: return defValue
        return value as Int
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val value = store[key] ?: return defValue
        return value as Long
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val value = store[key] ?: return defValue
        return value as Float
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val value = store[key] ?: return defValue
        return value as Boolean
    }

    override fun contains(key: String?): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private val removals = LinkedHashSet<String>()
        private var clearRequested = false

        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: MutableSet<String>?) =
            put(key, values?.toSet())

        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)

        private fun put(key: String, value: Any?): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removals.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) store.clear()
            removals.forEach { store.remove(it) }
            pending.forEach { (key, value) ->
                if (value == null) store.remove(key) else store[key] = value
            }
        }
    }
}
