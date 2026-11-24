package info.plateaukao.einkbro.activity.delegates

import android.os.Handler
import android.os.Looper
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.preference.ConfigManager
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test

class WebAppTabPersistenceTest {
    @Test
    fun `standalone save never reads or overwrites the main browser session`() {
        val config = mockk<ConfigManager>()
        val container = mockk<BrowserContainer>()
        val state = BrowserState().apply { isWebApp = true }
        mockkStatic(Looper::class)
        mockkConstructor(Handler::class)
        every { Looper.getMainLooper() } returns mockk()
        every { anyConstructed<Handler>().removeCallbacks(any()) } just Runs
        try {
            val manager = TabManager(
                activity = mockk(), config = config, state = state, browserContainer = container,
                albumViewModel = mockk(), bookmarkManager = mockk(), externalSearchViewModel = mockk(),
                createWebView = mockk(), createTouchListener = mockk(), keyHandlerSetWebView = mockk(),
                addHistoryAction = mockk(), adFilterProvider = mockk(), updateLanguageLabel = mockk(),
            )
            manager.updateSavedAlbumInfo()
            verify { config wasNot Called }
            verify { container wasNot Called }
        } finally {
            unmockkConstructor(Handler::class)
            unmockkStatic(Looper::class)
        }
    }
}
