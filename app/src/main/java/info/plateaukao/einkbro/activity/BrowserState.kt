package info.plateaukao.einkbro.activity

import android.graphics.Point
import android.widget.FrameLayout
import android.widget.ProgressBar
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.view.CenterExpandProgressBar
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.StatusbarViewController

/**
 * Shared mutable state for BrowserActivity and its delegates.
 * Replaces the provider/setter lambdas that were passed to each delegate individually.
 */
class BrowserState {
    lateinit var ebWebView: EBWebView
    val isWebViewInitialized: Boolean get() = ::ebWebView.isInitialized
    lateinit var binding: MainActivityLayout

    var currentAlbumController: AlbumController? = null
    var searchOnSite: Boolean = false
    var longPressPoint: Point = Point(0, 0)

    lateinit var mainContentLayout: FrameLayout
    lateinit var progressBar: ProgressBar
    lateinit var progressBarVertical: CenterExpandProgressBar
    lateinit var composeToolbarViewController: ComposeToolbarViewController
    lateinit var fabImageViewController: FabImageViewController
    lateinit var statusbarViewController: StatusbarViewController
}
