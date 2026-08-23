package info.plateaukao.einkbro.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserScriptBridgeTest {

    @Test
    fun `isWebUrl accepts absolute http and https urls`() {
        assertTrue(UserScriptBridge.isWebUrl("https://example.com/a?b=c"))
        assertTrue(UserScriptBridge.isWebUrl("http://example.com"))
        assertTrue(UserScriptBridge.isWebUrl("  HTTPS://example.com  "))
    }

    @Test
    fun `isWebUrl rejects local and app-level schemes`() {
        assertFalse(UserScriptBridge.isWebUrl("file:///sdcard/Android/data/info.plateaukao.einkbro/files/poc.html"))
        assertFalse(UserScriptBridge.isWebUrl("file:///data/data/info.plateaukao.einkbro/shared_prefs/x.xml"))
        assertFalse(UserScriptBridge.isWebUrl("content://media/external/file/1"))
        assertFalse(UserScriptBridge.isWebUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(UserScriptBridge.isWebUrl("javascript:alert(1)"))
        assertFalse(UserScriptBridge.isWebUrl("einkbro://config_start_page"))
        assertFalse(UserScriptBridge.isWebUrl("data:text/html,hi"))
    }

    @Test
    fun `isWebUrl rejects relative, empty and malformed urls`() {
        assertFalse(UserScriptBridge.isWebUrl(""))
        assertFalse(UserScriptBridge.isWebUrl("/etc/passwd"))
        assertFalse(UserScriptBridge.isWebUrl("example.com"))
        assertFalse(UserScriptBridge.isWebUrl("http://exa mple.com"))
        assertFalse(UserScriptBridge.isWebUrl("ht tp://x"))
    }
}
