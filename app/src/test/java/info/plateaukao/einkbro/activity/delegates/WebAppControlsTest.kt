package info.plateaukao.einkbro.activity.delegates

import android.view.View
import android.widget.FrameLayout
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

class WebAppControlsTest {
    @Test
    fun `standalone controls toggle without changing browser preferences or system bars`() {
        val state = mockk<BrowserState>(relaxed = true)
        val config = mockk<ConfigManager>()
        val bar = mockk<FrameLayout>(relaxed = true)
        val fab = mockk<FabImageViewController>(relaxed = true)
        var visibility = View.VISIBLE
        every { state.isWebApp } returns true
        every { state.searchOnSite } returns false
        every { state.binding.appBar } returns bar
        every { state.fabImageViewController } returns fab
        every { bar.visibility } answers { visibility }
        every { bar.visibility = any() } answers { visibility = firstArg() }
        val delegate = FullscreenDelegate(mockk(), config, state, mockk())
        mockkObject(ViewUnit)
        every { ViewUnit.updateSideTabBarVisibility(any()) } just Runs
        every { ViewUnit.hideKeyboard(any()) } just Runs
        try {
            delegate.toggleFullscreen()
            assertEquals(View.GONE, visibility)
            verify { fab.show() }
            verify { state.statusbarViewController.hide() }
            verify { state.binding.inputUrl.visibility = View.INVISIBLE }
            verify { config wasNot Called }

            every { config.ui.hideStatusbar } returns true
            every { config.ui.isVerticalToolbar } returns false
            delegate.toggleFullscreen()
            assertEquals(View.VISIBLE, visibility)
            verify(exactly = 2) { fab.show() }
            verify(exactly = 0) { fab.hide() }
        } finally {
            unmockkObject(ViewUnit)
        }
    }
}
