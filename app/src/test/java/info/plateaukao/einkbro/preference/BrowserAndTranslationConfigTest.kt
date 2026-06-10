package info.plateaukao.einkbro.preference

import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.Orientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrowserConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: BrowserConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = BrowserConfig(sp)
    }

    @Test
    fun `boolean preferences expose expected defaults`() {
        assertTrue(config.enableJavascript)
        assertTrue(config.adBlock)
        assertTrue(config.cookies)
        assertFalse(config.desktop)
        assertTrue(config.enablePullToRefresh)
        assertFalse(config.enableViBinding)
    }

    @Test
    fun `shareLongPressAction defaults to COPY_LINK and round trips`() {
        assertEquals(ShareLongPressAction.COPY_LINK, config.shareLongPressAction)
        config.shareLongPressAction = ShareLongPressAction.LAST_SHARE_TARGET
        assertEquals(ShareLongPressAction.LAST_SHARE_TARGET, config.shareLongPressAction)
    }

    @Test
    fun `shareLongPressAction tolerates non-numeric stored value`() {
        sp.store[BrowserConfig.K_SHARE_LONG_PRESS_ACTION] = "garbage"
        assertEquals(ShareLongPressAction.COPY_LINK, config.shareLongPressAction)
    }

    @Test
    fun `supernoteFolderUri treats blank as null`() {
        assertNull(config.supernoteFolderUri)
        config.supernoteFolderUri = "content://tree/primary"
        assertEquals("content://tree/primary", config.supernoteFolderUri)
        config.supernoteFolderUri = null
        assertNull(config.supernoteFolderUri)
    }

    @Test
    fun `searchEngineUrl has google default and round trips`() {
        assertEquals("https://www.google.com/search?q=%s", config.searchEngineUrl)
        config.searchEngineUrl = "https://duckduckgo.com/?q=%s"
        assertEquals("https://duckduckgo.com/?q=%s", config.searchEngineUrl)
    }

    @Test
    fun `adSites round trips`() {
        assertEquals(mutableSetOf<String>(), config.adSites)
        config.adSites = mutableSetOf("ads.example.com", "track.example.org")
        assertEquals(setOf("ads.example.com", "track.example.org"), config.adSites)
    }
}

class TranslationConfigTest {

    private lateinit var sp: FakeSharedPreferences
    private lateinit var config: TranslationConfig

    @Before
    fun setUp() {
        sp = FakeSharedPreferences()
        config = TranslationConfig(sp)
    }

    @Test
    fun `language defaults are EN target and KO source`() {
        assertEquals(TranslationLanguage.EN, config.translationLanguage)
        assertEquals(TranslationLanguage.KO, config.sourceLanguage)
    }

    @Test
    fun `translationLanguage round trips`() {
        config.translationLanguage = TranslationLanguage.JA
        assertEquals(TranslationLanguage.JA, config.translationLanguage)
    }

    @Test
    fun `translationMode defaults to TRANSLATE_BY_PARAGRAPH and round trips`() {
        assertEquals(TranslationMode.TRANSLATE_BY_PARAGRAPH, config.translationMode)
        config.translationMode = TranslationMode.GOOGLE_IN_PLACE
        assertEquals(TranslationMode.GOOGLE_IN_PLACE, config.translationMode)
    }

    @Test
    fun `translationMode falls back for out-of-range stored ordinal`() {
        sp.store[TranslationConfig.K_TRANSLATION_MODE] = 999
        assertEquals(TranslationMode.TRANSLATE_BY_PARAGRAPH, config.translationMode)
    }

    @Test
    fun `translationOrientation defaults to Horizontal and round trips`() {
        assertEquals(Orientation.Horizontal, config.translationOrientation)
        config.translationOrientation = Orientation.Vertical
        assertEquals(Orientation.Vertical, config.translationOrientation)
    }

    @Test
    fun `translationTextStyle defaults to DASHED_BORDER`() {
        assertEquals(TranslationTextStyle.DASHED_BORDER, config.translationTextStyle)
    }
}
