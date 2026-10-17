package info.plateaukao.einkbro.userscript

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserScriptMetadataTest {
    private fun metadata(vararg grants: String) = UserScriptMetadata.parse(
        "// ==UserScript==\n" + grants.joinToString("\n") { "// @grant $it" } +
            "\n// ==/UserScript==",
    )

    @Test
    fun `native APIs require an explicit legacy or modern grant`() {
        assertTrue(metadata("GM_getValue").allowsApi("getValue"))
        assertTrue(metadata("GM.getValue").allowsApi("getValue"))
        assertFalse(metadata("GM_getValue").allowsApi("setValue"))
        assertFalse(metadata().requiresNativeBridge)
        assertFalse(metadata("none", "GM_getValue").requiresNativeBridge)
        assertFalse(metadata("GM_addStyle", "GM_info", "unsafeWindow").requiresNativeBridge)
        assertFalse(metadata("*").requiresNativeBridge)
    }

    @Test
    fun `XHR grant aliases share only the XHR capability`() {
        for (grant in listOf("GM_xmlhttpRequest", "GM.xmlHttpRequest", "GM.xmlhttpRequest")) {
            val metadata = metadata(grant)
            assertTrue(metadata.requiresNativeBridge)
            assertTrue(metadata.allowsApi("xmlhttpRequest"))
            assertFalse(metadata.allowsApi("setClipboard"))
        }
    }
}
