package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.userscript.UserScriptManager
import info.plateaukao.einkbro.view.EBWebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class UserScriptBridgeTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `native calls require a matching site and API grant even after being queued`() = runTest {
        val scope = this
        val manager = mockk<UserScriptManager>(relaxed = true)
        var pageUrl = "https://example.com/article"
        val grants = mutableSetOf("getValue")
        val webView = mockk<EBWebView>(relaxed = true) {
            every { currentPageUrl } answers { pageUrl }
        }
        every { manager.isApiAllowed(1, any(), any()) } answers {
            secondArg<String>() == "https://example.com/article" && thirdArg<String>() in grants
        }
        every { manager.gmGetValue(1, "key") } returns "42"
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin {
            modules(module {
                single { manager }
                single<CoroutineScope> { scope }
            })
        }
        try {
            val bridge = UserScriptBridge(webView)
            bridge.registerToken("token", 1)
            assertEquals("42", bridge.gmGetValue("token", "key"))
            assertNull(bridge.gmGetValue("unknown", "key"))
            bridge.gmSetValue("token", "key", "7")
            verify(exactly = 0) { manager.gmSetValue(any(), any(), any()) }

            pageUrl = "https://other.com/"
            assertNull(bridge.gmGetValue("token", "key"))
            pageUrl = "https://example.com/article"

            grants.add("openInTab")
            bridge.gmOpenInTab("token", "https://example.org/", true)
            grants.remove("openInTab")
            runCurrent()
            verify(exactly = 0) { webView.openInNewTab(any()) }

            grants.add("openInTab")
            bridge.gmOpenInTab("token", "https://example.org/", true)
            runCurrent()
            verify(exactly = 1) { webView.openInNewTab("https://example.org/") }

            bridge.clearTokens()
            assertNull(bridge.gmGetValue("token", "key"))
            verify(exactly = 1) { manager.gmGetValue(1, "key") }
        } finally {
            stopKoin()
            Dispatchers.resetMain()
        }
    }

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
