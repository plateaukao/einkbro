package info.plateaukao.einkbro.preference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TabConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: TabConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = TabConfig(sp)
    }

    @Test
    fun `savedAlbumInfoList defaults to empty`() {
        assertEquals(emptyList<AlbumInfo>(), config.savedAlbumInfoList)
    }

    @Test
    fun `savedAlbumInfoList round trips`() {
        val albums = listOf(
            AlbumInfo("EinkBro", "https://github.com/plateaukao/einkbro"),
            AlbumInfo("Search", "https://www.google.com/search?q=a%20b"),
        )
        config.savedAlbumInfoList = albums
        assertEquals(albums, config.savedAlbumInfoList)
    }

    @Test
    fun `savedAlbumInfoList set to empty removes stored value`() {
        config.savedAlbumInfoList = listOf(AlbumInfo("t", "https://a.com"))
        config.savedAlbumInfoList = emptyList()
        assertEquals(emptyList<AlbumInfo>(), config.savedAlbumInfoList)
        assertFalse(sp.contains(TabConfig.K_SAVED_ALBUM_INFO))
    }

    @Test
    fun `saveHistoryMode defaults to SAVE_WHEN_OPEN`() {
        assertEquals(SaveHistoryMode.SAVE_WHEN_OPEN, config.saveHistoryMode)
    }

    @Test
    fun `saveHistoryMode honors legacy boolean preference`() {
        sp.store[TabConfig.K_SAVE_HISTORY] = false
        assertEquals(SaveHistoryMode.DISABLED, config.saveHistoryMode)

        sp.store[TabConfig.K_SAVE_HISTORY] = true
        assertEquals(SaveHistoryMode.SAVE_WHEN_OPEN, config.saveHistoryMode)
    }

    @Test
    fun `saveHistoryMode round trips and overrides legacy boolean`() {
        sp.store[TabConfig.K_SAVE_HISTORY] = false
        config.saveHistoryMode = SaveHistoryMode.SAVE_WHEN_CLOSE
        assertEquals(SaveHistoryMode.SAVE_WHEN_CLOSE, config.saveHistoryMode)
        assertTrue(config.isSaveHistoryWhenClose())
        assertFalse(config.isSaveHistoryWhenLoad())
        assertTrue(config.isSaveHistoryOn())
    }

    @Test
    fun `newTabBehavior round trips through string ordinal`() {
        assertEquals(NewTabBehavior.START_INPUT, config.newTabBehavior)
        config.newTabBehavior = NewTabBehavior.SHOW_RECENT_BOOKMARKS
        assertEquals(NewTabBehavior.SHOW_RECENT_BOOKMARKS, config.newTabBehavior)
        assertEquals("2", sp.store[TabConfig.K_NEW_TAB_BEHAVIOR])
    }

    @Test
    fun `currentAlbumIndex round trips`() {
        assertEquals(0, config.currentAlbumIndex)
        config.currentAlbumIndex = 3
        assertEquals(3, config.currentAlbumIndex)
    }

    @Test
    fun `purgeHistoryTimestamp round trips`() {
        assertEquals(0L, config.purgeHistoryTimestamp)
        config.purgeHistoryTimestamp = 1234567890L
        assertEquals(1234567890L, config.purgeHistoryTimestamp)
    }
}
