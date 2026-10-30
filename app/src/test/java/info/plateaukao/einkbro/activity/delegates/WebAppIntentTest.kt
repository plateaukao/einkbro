package info.plateaukao.einkbro.activity.delegates

import android.content.Intent
import android.net.Uri
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ConfigManager
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WebAppIntentTest {
    @Test
    fun `standalone launch skips browser session and repeated launch keeps the current page`() {
        val config = mockk<ConfigManager>()
        val state = mockk<BrowserState>(relaxed = true) {
            every { isWebApp } returns true
            every { currentAlbumController } returns null
        }
        val addAlbum = mockk<(String, String, Boolean, Boolean) -> Unit>(relaxed = true)
        val url = "http://example.com/app?query=1#page"
        val uri = mockk<Uri>()
        every { uri.toString() } returns url
        val intent = mockk<Intent> { every { data } returns uri }
        val delegate = IntentDispatchDelegate(
            activity = mockk(), config = config, state = state,
            externalSearchViewModel = mockk(), remoteConnViewModel = mockk(),
            translationViewModel = mockk(), overviewDialogControllerProvider = mockk(),
            addAlbumAction = addAlbum, addRestoredTab = mockk(), updateAlbum = mockk(),
            showAlbum = mockk(), getUrlMatchedBrowser = mockk(), openHistoryPage = mockk(),
            openBookmarkPage = mockk(), focusOnInput = mockk(), readArticle = mockk(),
            chatWithWeb = mockk(), showTranslationDialog = mockk(),
        )

        delegate.dispatchIntent(intent)
        every { state.currentAlbumController } returns mockk()
        delegate.dispatchIntent(intent)

        verify(exactly = 1) { addAlbum("", url, true, false) }
        verify { config wasNot Called }
        verify(exactly = 0) { intent.action = any() }
    }
}
