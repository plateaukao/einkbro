package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferenceDelegatesTest {

    private lateinit var sp: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        editor = mockk(relaxed = true)
        sp = mockk {
            every { edit() } returns editor
            every { getBoolean(any(), any()) } returns false
            every { getInt(any(), any()) } returns 0
            every { getString(any(), any()) } returns ""
        }
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit
    }

    @Test
    fun `BooleanPreference reads default value`() {
        every { sp.getBoolean("test_key", true) } returns true
        val delegate = BooleanPreference(sp, "test_key", true)
        val holder = object { var value by delegate }
        assertTrue(holder.value)
    }

    @Test
    fun `BooleanPreference reads stored value`() {
        every { sp.getBoolean("test_key", false) } returns true
        val delegate = BooleanPreference(sp, "test_key", false)
        val holder = object { var value by delegate }
        assertTrue(holder.value)
    }

    @Test
    fun `IntPreference reads default value`() {
        every { sp.getInt("int_key", 42) } returns 42
        val delegate = IntPreference(sp, "int_key", 42)
        val holder = object { var value by delegate }
        assertEquals(42, holder.value)
    }

    @Test
    fun `IntPreference reads stored value`() {
        every { sp.getInt("int_key", 0) } returns 100
        val delegate = IntPreference(sp, "int_key", 0)
        val holder = object { var value by delegate }
        assertEquals(100, holder.value)
    }

    @Test
    fun `StringPreference reads default value`() {
        every { sp.getString("str_key", "default") } returns "default"
        val delegate = StringPreference(sp, "str_key", "default")
        val holder = object { var value by delegate }
        assertEquals("default", holder.value)
    }

    @Test
    fun `StringPreference handles null from SharedPreferences`() {
        every { sp.getString("str_key", "fallback") } returns null
        val delegate = StringPreference(sp, "str_key", "fallback")
        val holder = object { var value by delegate }
        assertEquals("fallback", holder.value)
    }

    @Test
    fun `toggle extension flips boolean property`() {
        val holder = ToggleTestHolder()
        assertFalse(holder.value)
        holder::value.toggle()
        assertTrue(holder.value)
        holder::value.toggle()
        assertFalse(holder.value)
    }
}

private class ToggleTestHolder {
    var value: Boolean = false
}

private fun kotlin.reflect.KMutableProperty0<Boolean>.toggle() = set(!get())
