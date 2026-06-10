package info.plateaukao.einkbro.preference

import android.content.Context
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.statusbar.StatusbarItem
import info.plateaukao.einkbro.view.statusbar.StatusbarPosition
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UiConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var context: Context
    private lateinit var config: UiConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        context = mockk(relaxed = true)
        config = UiConfig(context, sp)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `toolbarPosition defaults to Bottom`() {
        assertEquals(ToolbarPosition.Bottom, config.toolbarPosition)
    }

    @Test
    fun `toolbarPosition migrates from legacy boolean preference`() {
        sp.store[UiConfig.K_TOOLBAR_TOP] = true
        assertEquals(ToolbarPosition.Top, config.toolbarPosition)
    }

    @Test
    fun `toolbarPosition new ordinal preference wins over legacy boolean`() {
        sp.store[UiConfig.K_TOOLBAR_TOP] = true
        config.toolbarPosition = ToolbarPosition.Left
        assertEquals(ToolbarPosition.Left, config.toolbarPosition)
    }

    @Test
    fun `toolbarPosition round trips through ordinal`() {
        config.toolbarPosition = ToolbarPosition.Right
        assertEquals(ToolbarPosition.Right, config.toolbarPosition)
        assertTrue(config.isVerticalToolbar)
        assertFalse(config.isToolbarOnTop)
    }

    @Test
    fun `toolbarPosition falls back to Bottom for out-of-range ordinal`() {
        sp.store[UiConfig.K_TOOLBAR_POSITION] = 99
        assertEquals(ToolbarPosition.Bottom, config.toolbarPosition)
    }

    @Test
    fun `isToolbarOnTop setter updates toolbarPosition`() {
        config.isToolbarOnTop = true
        assertEquals(ToolbarPosition.Top, config.toolbarPosition)
        config.isToolbarOnTop = false
        assertEquals(ToolbarPosition.Bottom, config.toolbarPosition)
    }

    @Test
    fun `statusbarItems defaults when nothing stored`() {
        assertEquals(StatusbarItem.defaultItems, config.statusbarItems)
    }

    @Test
    fun `statusbarItems round trips`() {
        val items = listOf(StatusbarItem.Battery, StatusbarItem.Time)
        config.statusbarItems = items
        assertEquals(items, config.statusbarItems)
    }

    @Test
    fun `statusbarItems skips invalid ordinals`() {
        sp.store[UiConfig.K_STATUSBAR_ITEMS] = "0,999,abc,2"
        val expected = listOfNotNull(StatusbarItem.fromOrdinal(0), StatusbarItem.fromOrdinal(2))
        assertEquals(expected, config.statusbarItems)
    }

    @Test
    fun `statusbarPosition round trips and defaults to Top`() {
        assertEquals(StatusbarPosition.Top, config.statusbarPosition)
        config.statusbarPosition = StatusbarPosition.Bottom
        assertEquals(StatusbarPosition.Bottom, config.statusbarPosition)
    }

    @Test
    fun `hiddenMenuItems round trips`() {
        assertEquals(emptySet<String>(), config.hiddenMenuItems)
        config.hiddenMenuItems = setOf("a", "b")
        assertEquals(setOf("a", "b"), config.hiddenMenuItems)
    }

    @Test
    fun `menuItemOrder round trips and defaults to empty`() {
        assertEquals(emptyList<String>(), config.menuItemOrder)
        config.menuItemOrder = listOf("first", "second", "third")
        assertEquals(listOf("first", "second", "third"), config.menuItemOrder)
    }

    @Test
    fun `readerToolbarActions defaults when nothing stored`() {
        assertEquals(ToolbarAction.defaultReaderActions, config.readerToolbarActions)
    }

    @Test
    fun `readerToolbarActions round trips`() {
        val actions = ToolbarAction.defaultReaderActions.take(3).reversed()
        config.readerToolbarActions = actions
        assertEquals(actions, config.readerToolbarActions)
    }

    @Test
    fun `toolbarActions round trips in portrait phone layout`() {
        // isLandscape is @JvmStatic (static bridge), isWideLayout is a plain
        // object member — both interception modes are needed.
        mockkObject(ViewUnit)
        mockkStatic(ViewUnit::class)
        every { ViewUnit.isLandscape(any()) } returns false
        every { ViewUnit.isWideLayout(any()) } returns false

        assertEquals(ToolbarAction.defaultActionsForPhone, config.toolbarActions)

        val actions = ToolbarAction.defaultActions.take(4)
        config.toolbarActions = actions
        assertEquals(actions, config.toolbarActions)
    }
}
