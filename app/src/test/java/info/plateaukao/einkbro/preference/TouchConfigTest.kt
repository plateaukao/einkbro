package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.browser.BrowserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TouchConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: TouchConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = TouchConfig(sp)
    }

    @Test
    fun `touchAreaType defaults to BottomLeftRight and round trips`() {
        assertEquals(TouchAreaType.BottomLeftRight, config.touchAreaType)
        config.touchAreaType = TouchAreaType.Ebook
        assertEquals(TouchAreaType.Ebook, config.touchAreaType)
    }

    @Test
    fun `isEbookModeActive requires Ebook type and touch turn enabled`() {
        config.touchAreaType = TouchAreaType.Ebook
        assertFalse(config.isEbookModeActive)

        config.enableTouchTurn = true
        assertTrue(config.isEbookModeActive)

        config.touchAreaType = TouchAreaType.Left
        assertFalse(config.isEbookModeActive)
    }

    @Test
    fun `pageReservedOffset defaults to 80`() {
        assertEquals(80, config.pageReservedOffset)
    }

    @Test
    fun `browser action gestures expose declared defaults`() {
        assertEquals(BrowserAction.PageUp, config.upClickGesture)
        assertEquals(BrowserAction.PageDown, config.downClickGesture)
        assertEquals(BrowserAction.JumpToTop, config.upLongClickGesture)
        assertEquals(BrowserAction.JumpToBottom, config.downLongClickGesture)
        assertEquals(BrowserAction.ShowOverview, config.navButtonLongClickGesture)
        assertEquals(BrowserAction.Noop, config.multitouchUp)
    }

    @Test
    fun `browser action gesture round trips through stable id`() {
        config.multitouchLeft = BrowserAction.PageDown
        assertEquals(BrowserAction.PageDown, config.multitouchLeft)
        assertEquals("PageDown", sp.store[TouchConfig.K_MULTITOUCH_LEFT])
    }

    @Test
    fun `corrupted stored gesture id is restored to declared default`() {
        sp.store["K_UP_CLICK_GESTURE"] = "a\$ObfuscatedName"
        assertEquals(BrowserAction.PageUp, config.upClickGesture)
        // delegate self-heals the stored value
        assertEquals("PageUp", sp.store["K_UP_CLICK_GESTURE"])
    }

    @Test
    fun `legacy numeric gesture codes are migrated`() {
        sp.store["K_UP_CLICK_GESTURE"] = "11" // legacy PageUp code
        assertEquals(BrowserAction.PageUp, config.upClickGesture)
        assertEquals("PageUp", sp.store["K_UP_CLICK_GESTURE"])
    }
}
