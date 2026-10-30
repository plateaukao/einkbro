package info.plateaukao.einkbro.activity.delegates

import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WebAppControlsTest(private val isWebApp: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isWebApp={0}")
        fun modes() = listOf(false, true)
    }

    @Test
    fun `fullscreen always shows its exit button and restoring the toolbar hides it`() {
        val state = mockk<BrowserState>(relaxed = true)
        val config = mockk<ConfigManager>()
        val bar = mockk<FrameLayout>(relaxed = true)
        val fab = mockk<FabImageViewController>(relaxed = true)
        val activity = mockk<FragmentActivity>()
        val window = mockk<Window>(relaxed = true)
        every { activity.window } returns window
        var visibility = View.VISIBLE
        every { state.isWebApp } returns isWebApp
        every { state.searchOnSite } returns false
        every { state.binding.appBar } returns bar
        every { state.fabImageViewController } returns fab
        every { bar.visibility } answers { visibility }
        every { bar.visibility = any() } answers { visibility = firstArg() }
        val delegate = FullscreenDelegate(activity, config, state, mockk())
        mockkObject(ViewUnit)
        every { ViewUnit.updateSideTabBarVisibility(any()) } just Runs
        every { ViewUnit.hideKeyboard(any()) } just Runs
        try {
            delegate.toggleFullscreen()
            assertEquals(View.GONE, visibility)
            verify { fab.show() }
            verify { state.statusbarViewController.show() }
            verify { config wasNot Called }
            verify { window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) }

            every { config.ui.hideStatusbar } returns false
            every { config.ui.isVerticalToolbar } returns false
            delegate.toggleFullscreen()
            assertEquals(View.VISIBLE, visibility)
            verify(exactly = 1) { fab.show() }
            verify(exactly = 1) { fab.hide() }
            verify { state.statusbarViewController.hide() }
            verify { window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) }
        } finally {
            unmockkObject(ViewUnit)
        }
    }
}
