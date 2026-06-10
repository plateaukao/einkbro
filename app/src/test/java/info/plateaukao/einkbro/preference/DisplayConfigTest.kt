package info.plateaukao.einkbro.preference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DisplayConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: DisplayConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = DisplayConfig(sp)
    }

    @Test
    fun `fontSize defaults to 100 and round trips as string`() {
        assertEquals(100, config.fontSize)
        config.fontSize = 130
        assertEquals(130, config.fontSize)
        assertEquals("130", sp.store[DisplayConfig.K_FONT_SIZE])
    }

    @Test
    fun `readerFontSize falls back to fontSize when unset`() {
        config.fontSize = 120
        assertEquals(120, config.readerFontSize)
        config.readerFontSize = 150
        assertEquals(150, config.readerFontSize)
    }

    @Test
    fun `customFontInfo defaults to null and round trips`() {
        assertNull(config.customFontInfo)
        val info = CustomFontInfo("My Font", "content://fonts/my.ttf")
        config.customFontInfo = info
        assertEquals(info, config.customFontInfo)
    }

    @Test
    fun `setting customFontInfo to null clears stored value`() {
        config.customFontInfo = CustomFontInfo("f", "u")
        config.customFontInfo = null
        assertNull(config.customFontInfo)
    }

    @Test
    fun `darkMode defaults to DISABLED and round trips`() {
        assertEquals(DarkMode.DISABLED, config.darkMode)
        config.darkMode = DarkMode.FORCE_ON
        assertEquals(DarkMode.FORCE_ON, config.darkMode)
        assertEquals("1", sp.store[DisplayConfig.K_DARK_MODE])
    }

    @Test
    fun `fontType round trips and readerFontType falls back to fontType`() {
        config.fontType = FontType.SERIF
        assertEquals(FontType.SERIF, config.fontType)
        assertEquals(FontType.SERIF, config.readerFontType)
        config.readerFontType = FontType.GOOGLE_SERIF
        assertEquals(FontType.GOOGLE_SERIF, config.readerFontType)
    }

    @Test
    fun `einkImageAdjustment defaults to OFF and round trips`() {
        assertEquals(EinkImageAdjustment.OFF, config.einkImageAdjustment)
        config.einkImageAdjustment = EinkImageAdjustment.LEVEL_30
        assertEquals(EinkImageAdjustment.LEVEL_30, config.einkImageAdjustment)
    }

    @Test
    fun `einkImageAdjustment migrates legacy boolean preference`() {
        sp.store[DisplayConfig.K_ENABLE_IMAGE_ADJUSTMENT] = true
        assertEquals(EinkImageAdjustment.OFF, config.einkImageAdjustment)
        // legacy boolean value must be removed so subsequent reads succeed
        assertFalse(sp.contains(DisplayConfig.K_ENABLE_IMAGE_ADJUSTMENT))
    }

    @Test
    fun `einkImageAdjustment falls back to OFF for out-of-range ordinal`() {
        sp.store[DisplayConfig.K_ENABLE_IMAGE_ADJUSTMENT] = 99
        assertEquals(EinkImageAdjustment.OFF, config.einkImageAdjustment)
    }

    @Test
    fun `highlightStyle round trips`() {
        assertEquals(HighlightStyle.UNDERLINE, config.highlightStyle)
        config.highlightStyle = HighlightStyle.BACKGROUND_GREEN
        assertEquals(HighlightStyle.BACKGROUND_GREEN, config.highlightStyle)
    }

    @Test
    fun `fontBoldness defaults to 700`() {
        assertEquals(700, config.fontBoldness)
        config.fontBoldness = 500
        assertEquals(500, config.fontBoldness)
    }
}
