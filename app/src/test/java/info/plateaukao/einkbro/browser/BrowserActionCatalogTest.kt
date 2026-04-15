package info.plateaukao.einkbro.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserActionCatalogTest {

    @Test
    fun `migrateLegacyId returns empty for null so default can apply`() {
        assertEquals("", BrowserActionCatalog.migrateLegacyId(null))
    }

    @Test
    fun `migrateLegacyId returns empty for empty string so default can apply`() {
        assertEquals("", BrowserActionCatalog.migrateLegacyId(""))
    }

    @Test
    fun `migrateLegacyId converts legacy digit codes`() {
        assertEquals("PageUp", BrowserActionCatalog.migrateLegacyId("11"))
        assertEquals("PageDown", BrowserActionCatalog.migrateLegacyId("12"))
        assertEquals("GoForward", BrowserActionCatalog.migrateLegacyId("02"))
        assertEquals("ShowOverview", BrowserActionCatalog.migrateLegacyId("08"))
    }

    @Test
    fun `migrateLegacyId passes through new-format ids`() {
        assertEquals("PageUp", BrowserActionCatalog.migrateLegacyId("PageUp"))
        assertEquals("GoForward", BrowserActionCatalog.migrateLegacyId("GoForward"))
        assertEquals("Noop", BrowserActionCatalog.migrateLegacyId("Noop"))
    }

    @Test
    fun `migrateLegacyId maps NothingHappen code 01 to Noop`() {
        assertEquals("Noop", BrowserActionCatalog.migrateLegacyId("01"))
    }
}
