package info.plateaukao.einkbro.view

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureTypeTest {

    @Test
    fun `from returns correct type for valid value`() {
        assertEquals(GestureType.Forward, GestureType.from("02"))
        assertEquals(GestureType.Backward, GestureType.from("03"))
        assertEquals(GestureType.PageDown, GestureType.from("12"))
        assertEquals(GestureType.Menu, GestureType.from("17"))
    }

    @Test
    fun `from returns NothingHappen for unknown value`() {
        assertEquals(GestureType.NothingHappen, GestureType.from("99"))
        assertEquals(GestureType.NothingHappen, GestureType.from(""))
        assertEquals(GestureType.NothingHappen, GestureType.from("invalid"))
    }

    @Test
    fun `all gesture types have unique values`() {
        val values = GestureType.values().map { it.value }
        assertEquals(values.size, values.distinct().size)
    }

    @Test
    fun `NothingHappen has value 01`() {
        assertEquals("01", GestureType.NothingHappen.value)
    }

    @Test
    fun `round trip from value to type`() {
        for (type in GestureType.values()) {
            assertEquals(type, GestureType.from(type.value))
        }
    }
}
