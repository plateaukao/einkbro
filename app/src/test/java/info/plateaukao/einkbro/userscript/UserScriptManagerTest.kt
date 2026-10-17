package info.plateaukao.einkbro.userscript

import android.content.Context
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.UserScript
import info.plateaukao.einkbro.database.UserScriptDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class UserScriptManagerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `native support requires an enabled script with explicit API and URL permissions`() = runBlocking {
        var rows = emptyList<UserScript>()
        val dao = mockk<UserScriptDao> {
            coEvery { getAll() } answers { rows }
        }
        val bookmarks = mockk<BookmarkManager> {
            every { userScriptDao } returns dao
        }
        val context = mockk<Context> {
            every { filesDir } returns tempFolder.root
        }
        // Reload explicitly so startup IO cannot race the tested transitions.
        val scope = CoroutineScope(Job().apply { cancel() })
        startKoin {
            modules(module {
                single { bookmarks }
                single<CoroutineScope> { scope }
            })
        }
        try {
            val manager = UserScriptManager(context)
            assertFalse(manager.hasNativeScripts.value)
            manager.reload()
            assertFalse(manager.hasNativeScripts.value)

            val script = UserScript(id = 1, name = "Test", code = """
                // ==UserScript==
                // @name Test
                // @match https://example.com/*
                // @exclude https://example.com/private/*
                // @grant GM_getValue
                // ==/UserScript==
            """.trimIndent())
            rows = listOf(script)
            manager.reload()
            assertTrue(manager.hasNativeScripts.value)
            assertTrue(manager.isApiAllowed(1, "https://example.com/article", "getValue"))
            assertFalse(manager.isApiAllowed(1, "https://other.com/", "getValue"))
            assertFalse(manager.isApiAllowed(1, "https://example.com/private/a", "getValue"))
            assertFalse(manager.isApiAllowed(1, "https://example.com/article", "setValue"))
            assertFalse(manager.isApiAllowed(2, "https://example.com/article", "getValue"))

            rows = listOf(script.copy(enabled = false))
            manager.reload()
            assertFalse(manager.hasNativeScripts.value)
            assertFalse(manager.isApiAllowed(1, "https://example.com/article", "getValue"))

            rows = listOf(script.copy(enabled = false), script.copy(id = 2))
            manager.reload()
            assertTrue(manager.hasNativeScripts.value)

            rows = listOf(script.copy(code = script.code.replace("GM_getValue", "none")))
            manager.reload()
            assertFalse(manager.hasNativeScripts.value)
            assertFalse(manager.isApiAllowed(1, "https://example.com/article", "getValue"))
            assertTrue(manager.getMatchingScripts("https://example.com/article", RunAt.DOCUMENT_END).isNotEmpty())

            rows = emptyList()
            manager.reload()
            assertFalse(manager.hasNativeScripts.value)
        } finally {
            stopKoin()
        }
    }
}
