package info.plateaukao.einkbro.activity.delegates

import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Build
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FabPosition
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.view.ZoomableFrameLayout
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController

class FullscreenDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val searchPanelHideAction: () -> Unit,
) {
    private var videoView: VideoView? = null
    private var customView: View? = null
    var fullscreenHolder: FrameLayout? = null
        private set
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation = 0

    private inner class VideoCompletionListener : MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = false
        override fun onCompletion(mp: MediaPlayer) {
            onHideCustomView()
        }
    }

    fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null) return

        if (customView != null && callback != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        originalOrientation = activity.requestedOrientation
        fullscreenHolder = ZoomableFrameLayout(activity).apply {
            enableZoom = config.display.zoomInCustomView
            addView(
                customView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        ViewUnit.invertColor(view, config.hasInvertedColor(state.ebWebView.url.orEmpty()))

        val decorView = activity.window.decorView as FrameLayout
        decorView.addView(
            fullscreenHolder,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        customView?.keepScreenOn = true
        (state.currentAlbumController as View?)?.visibility = View.INVISIBLE
        ViewUnit.setCustomFullscreen(
            activity.window,
            true,
            config.ui.hideStatusbar,
            ViewUnit.isEdgeToEdgeEnabled(activity.resources)
        )
        if (view is FrameLayout) {
            if (view.focusedChild is VideoView) {
                videoView = view.focusedChild as VideoView
                videoView?.setOnErrorListener(VideoCompletionListener())
                videoView?.setOnCompletionListener(VideoCompletionListener())
            }
        }
        customViewCallback = callback
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    fun onHideCustomView(): Boolean {
        if (customView == null || customViewCallback == null || state.currentAlbumController == null) {
            return false
        }

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        fullscreenHolder?.visibility = GONE
        customView?.visibility = GONE
        (activity.window.decorView as FrameLayout).removeView(fullscreenHolder)
        customView?.keepScreenOn = false
        (state.currentAlbumController as View).visibility = VISIBLE
        ViewUnit.setCustomFullscreen(
            activity.window,
            false,
            config.ui.hideStatusbar,
            ViewUnit.isEdgeToEdgeEnabled(activity.resources)
        )
        fullscreenHolder = null
        customView = null

        if (videoView != null) {
            videoView?.visibility = GONE
            videoView?.setOnErrorListener(null)
            videoView?.setOnCompletionListener(null)
            videoView = null
        }
        activity.requestedOrientation = originalOrientation
        return true
    }

    fun toggleFullscreen() {
        if (state.searchOnSite) return

        val binding = state.binding
        if (binding.appBar.visibility == VISIBLE) {
            if (config.ui.fabPosition != FabPosition.NotShow) {
                state.fabImageViewController.show()
            }
            binding.mainSearchPanel.visibility = View.INVISIBLE
            binding.appBar.visibility = GONE
            binding.contentSeparator.visibility = GONE
            hideStatusBar()
        } else {
            showToolbar()
        }
    }

    fun showToolbar() {
        if (state.searchOnSite) return

        showStatusBar()
        state.fabImageViewController.hide()
        val binding = state.binding
        binding.mainSearchPanel.visibility = View.INVISIBLE
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        binding.inputUrl.visibility = View.INVISIBLE
        state.composeToolbarViewController.show()
        ViewUnit.hideKeyboard(activity)

        if (config.ui.isVerticalToolbar) {
            resetInputUrlConstraints()
        }
    }

    @Suppress("DEPRECATION")
    fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
            if (ViewUnit.isEdgeToEdgeEnabled(activity.resources))
                activity.window.insetsController?.hide(WindowInsets.Type.navigationBars())
            state.binding.root.setPadding(0, 0, 0, 0)
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    @Suppress("DEPRECATION")
    fun showStatusBar() {
        if (config.ui.hideStatusbar) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(true)
            activity.window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    fun hideSearchPanel() {
        state.ebWebView.clearMatches()
        searchPanelHideAction()
        showToolbar()
    }

    private fun resetInputUrlConstraints() {
        val binding = state.binding
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet().apply {
            clone(binding.root)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END)
        }
        constraintSet.applyTo(binding.root)
    }
}
