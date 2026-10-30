package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.viewmodel.ActionModeMenuViewModel
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MouseSelectionTest {
    @Test
    fun `mouse selection shares remote actions and preserves native editing and menus`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val lifecycle = object : Lifecycle() {
                override val currentState = State.RESUMED
                override fun addObserver(observer: LifecycleObserver) {}
                override fun removeObserver(observer: LifecycleObserver) {}
            }
            val activity = mockk<FragmentActivity> {
                every { this@mockk.lifecycle } returns lifecycle
            }
            val config = mockk<ConfigManager>(relaxed = true)
            every { config.ai.gptActionList } returns emptyList()
            val menus = mockk<ActionModeMenuViewModel>(relaxed = true)
            val remote = mockk<RemoteConnViewModel>(relaxed = true)
            val webView = mockk<EBWebView>(relaxed = true)
            coEvery { webView.isEditingText() } returns false
            coEvery { webView.getSelectedText() } returns "selected text"
            coEvery { webView.getSelectedTextWithContext() } returns "surrounding selected text"
            val delegate = ActionModeDelegate(
                activity, config, mockk(relaxed = true), menus,
                mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
                remote, mockk(relaxed = true), mockk(relaxed = true),
                { webView }, {}, {}, {}, { _, _, _ -> },
            )

            // Native-menu preference must not cause a second custom menu or action.
            every { config.ui.showDefaultActionMenu } returns true
            delegate.onMouseSelection(webView)
            runCurrent()
            coVerify(exactly = 0) { webView.getSelectedText() }

            // Remote text-search mode still takes precedence, just as for touch.
            every { remote.isSendingTextSearch } returns true
            delegate.onMouseSelection(webView)
            runCurrent()
            verify(exactly = 1) { remote.sendTextSearch(any()) }

            // Clicking outside a selection dismisses the custom menu.
            coEvery { webView.getSelectedText() } returns ""
            delegate.onMouseSelection(webView)
            runCurrent()
            verify(exactly = 1) { menus.finish() }
            verify(exactly = 1) { remote.sendTextSearch(any()) }

            // Editable fields retain their native selection/copy/paste handling.
            coEvery { webView.isEditingText() } returns true
            delegate.onMouseSelection(webView)
            runCurrent()
            coVerify(exactly = 2) { webView.getSelectedText() }
            verify(exactly = 1) { menus.hide() }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
