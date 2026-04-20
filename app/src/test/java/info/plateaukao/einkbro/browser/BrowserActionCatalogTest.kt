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

    @Test
    fun `migrateLegacyId remaps legacy TouchPagination to ToggleTouchTurnPage`() {
        // The old ToggleTouchPagination action was a duplicate and has been
        // removed from the catalog; legacy "18" bindings must survive the trim.
        assertEquals("ToggleTouchTurnPage", BrowserActionCatalog.migrateLegacyId("18"))
    }

    @Test
    fun `entryOf returns nothingEntry for ids removed from catalog`() {
        // ToggleTouchPagination still exists as a BrowserAction subclass but is
        // no longer in the catalog. Callers must treat this as unresolved so
        // the preference delegate can fall back to its declared default.
        assertEquals(
            BrowserActionCatalog.nothingEntry,
            BrowserActionCatalog.entryOf("ToggleTouchPagination"),
        )
    }
}
