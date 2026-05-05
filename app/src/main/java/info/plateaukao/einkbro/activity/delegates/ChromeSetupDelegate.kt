package info.plateaukao.einkbro.activity.delegates

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.BrowserAction
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.DarkMode
import info.plateaukao.einkbro.preference.ToolbarPosition
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MultitouchListener
import info.plateaukao.einkbro.view.SwipeTouchListener
import info.plateaukao.einkbro.view.TranslationPanelView
import info.plateaukao.einkbro.view.handlers.GestureHandler
import info.plateaukao.einkbro.view.statusbar.StatusbarPosition
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.view.viewControllers.TouchAreaViewController
import info.plateaukao.einkbro.view.viewControllers.TwoPaneController
import info.plateaukao.einkbro.viewmodel.AlbumViewModel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class ChromeSetupDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val config: ConfigManager,
    private val displayConfigDelegate: DisplayConfigDelegate,
    private val fullscreenDelegate: FullscreenDelegate,
    private val gestureHandler: GestureHandler,
    private val contextMenuDelegate: ContextMenuDelegate,
    private val actionModeDelegate: ActionModeDelegate,
    private val inputBarDelegate: InputBarDelegate,
    private val albumViewModel: AlbumViewModel,
    private val composeToolbarViewControllerProvider: () -> ComposeToolbarViewController,
    private val twoPaneControllerProvider: () -> TwoPaneController?,
    private val dispatch: (BrowserAction) -> Unit,
    private val updateAlbum: (String?) -> Unit,
    private val addAlbum: (String, String, Boolean) -> Unit,
    private val addIncognitoAlbum: () -> Unit,
    private val newATab: () -> Unit,
    private val toggleSplitScreen: (String?) -> Unit,
    private val focusOnInput: () -> Unit,
    private val showFastToggleDialog: () -> Unit,
    private val toggleFullscreen: () -> Unit,
) {
    lateinit var overviewDialogController: OverviewDialogController
        private set

    private var touchControllerInternal: TouchAreaViewController? = null
    val touchController: TouchAreaViewController?
        get() = touchControllerInternal

    fun initContentViews() {
        val binding = state.binding
        val swipeRefreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        state.swipeRefreshLayout = swipeRefreshLayout
        state.mainContentLayout = activity.findViewById(R.id.main_content)
        val translationPanelView = TranslationPanelView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        state.translationPanelView = translationPanelView
        binding.twoPanelLayout.addView(translationPanelView)

        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            val ebWebView = state.ebWebView
            ebWebView.isTouchOnInnerScrollable
                || !ebWebView.wasAtTopOnTouchStart
                || ebWebView.scrollY > 0
                || !ebWebView.isInnerScrollAtTop
        }
        swipeRefreshLayout.setOnRefreshListener {
            if (state.currentAlbumController != null) state.ebWebView.reload()
            else swipeRefreshLayout.isRefreshing = false
        }
        ViewUnit.updateAppbarPosition(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initToolbar() {
        state.progressBar = activity.findViewById(R.id.main_progress_bar)
        state.progressBarVertical = activity.findViewById(R.id.main_progress_bar_vertical)
        if (config.display.darkMode == DarkMode.FORCE_ON) {
            val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_NO) {
                state.progressBar.progressTintMode = PorterDuff.Mode.LIGHTEN
                state.progressBarVertical.setFillColor(android.graphics.Color.WHITE)
            }
        }
        initFAB()
        if (config.touch.enableNavButtonGesture) {
            val onNavButtonTouchListener = object : SwipeTouchListener(activity) {
                override fun onSwipeTop() = gestureHandler.handle(config.touch.navGestureUp)
                override fun onSwipeBottom() = gestureHandler.handle(config.touch.navGestureDown)
                override fun onSwipeRight() = gestureHandler.handle(config.touch.navGestureRight)
                override fun onSwipeLeft() = gestureHandler.handle(config.touch.navGestureLeft)
            }
            state.fabImageViewController.defaultTouchListener = onNavButtonTouchListener
        }
        composeToolbarViewControllerProvider().updateIcons()
    }

    private fun initFAB() {
        val controller = FabImageViewController(
            displayConfigDelegate.orientation,
            activity.findViewById(R.id.fab_imageButtonNav),
            { fullscreenDelegate.showToolbar() },
            longClickAction = {
                if (config.touch.enableNavButtonGesture) gestureHandler.handle(config.touch.navButtonLongClickGesture)
                else showFastToggleDialog()
            },
        )
        state.fabImageViewController = controller
    }

    fun initOverview() {
        overviewDialogController = OverviewDialogController(
            activity,
            albumViewModel.albums,
            albumViewModel.focusIndex,
            state.binding.layoutOverview,
            gotoUrlAction = { url -> updateAlbum(url) },
            addTabAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            addIncognitoTabAction = { addIncognitoAlbum() },
            splitScreenAction = { url -> toggleSplitScreen(url) },
            addEmptyTabAction = { newATab() },
        )
    }

    fun initTouchArea() = composeToolbarViewControllerProvider().updateIcons()

    fun initTouchAreaViewController() {
        touchControllerInternal = TouchAreaViewController(state.binding.activityMainContent) { dispatch(it) }
    }

    fun applyStatusbarConstraints(position: StatusbarPosition) {
        val root = state.binding.root
        val cs = androidx.constraintlayout.widget.ConstraintSet().apply { clone(root) }
        val statusBarId = state.binding.statusBar.id
        val twoPanelId = state.binding.twoPanelLayout.id
        val appBarId = state.binding.appBar.id
        val top = androidx.constraintlayout.widget.ConstraintSet.TOP
        val bottom = androidx.constraintlayout.widget.ConstraintSet.BOTTOM
        val parent = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
        // Vertical toolbar: appBar spans full height (TOP & BOTTOM → parent).
        // Top toolbar: appBar.top pinned to parent.top.
        // In both cases, anchoring twoPanel.bottom to appBar.top collapses the webview.
        val isVertical = config.ui.isVerticalToolbar
        val isToolbarTop = config.ui.toolbarPosition == ToolbarPosition.Top

        cs.clear(statusBarId, top)
        cs.clear(statusBarId, bottom)
        cs.clear(twoPanelId, top)
        cs.clear(twoPanelId, bottom)

        when (position) {
            StatusbarPosition.Top -> when {
                isToolbarTop -> {
                    // Stack: appBar (top) → statusBar → twoPanel → parent.bottom
                    cs.connect(statusBarId, top, appBarId, bottom)
                    cs.connect(twoPanelId, top, statusBarId, bottom)
                    cs.connect(twoPanelId, bottom, parent, bottom)
                }
                isVertical -> {
                    cs.connect(statusBarId, top, parent, top)
                    cs.connect(twoPanelId, top, statusBarId, bottom)
                    cs.connect(twoPanelId, bottom, parent, bottom)
                }
                else -> { // horizontal toolbar at Bottom
                    cs.connect(statusBarId, top, parent, top)
                    cs.connect(twoPanelId, top, statusBarId, bottom)
                    cs.connect(twoPanelId, bottom, appBarId, top)
                }
            }
            StatusbarPosition.Bottom -> when {
                isToolbarTop -> {
                    // Stack: appBar (top) → twoPanel → statusBar (bottom)
                    cs.connect(statusBarId, bottom, parent, bottom)
                    cs.connect(twoPanelId, top, appBarId, bottom)
                    cs.connect(twoPanelId, bottom, statusBarId, top)
                }
                isVertical -> {
                    cs.connect(statusBarId, bottom, parent, bottom)
                    cs.connect(twoPanelId, top, parent, top)
                    cs.connect(twoPanelId, bottom, statusBarId, top)
                }
                else -> { // horizontal toolbar at Bottom
                    cs.connect(statusBarId, bottom, appBarId, top)
                    cs.connect(twoPanelId, top, parent, top)
                    cs.connect(twoPanelId, bottom, statusBarId, top)
                }
            }
        }
        cs.applyTo(root)
    }

    fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(state.binding.root) { view, windowInsets ->
            val insetsNavigationBar: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val insetsKeyboard: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as FrameLayout.LayoutParams
            if (config.ui.hideStatusbar) {
                params.bottomMargin = when {
                    insetsKeyboard.bottom > 0 -> insetsKeyboard.bottom
                    insetsNavigationBar.bottom > 0 -> insetsNavigationBar.bottom
                    else -> 0
                }
                view.layoutParams = params
            }
            windowInsets
        }
    }

    fun listenKeyboardShowHide() {
        val binding = state.binding
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val controller = touchControllerInternal
            if (inputBarDelegate.isKeyboardDisplaying()) controller?.maybeDisableTemporarily()
            else controller?.maybeEnableAgain()

            @Suppress("DEPRECATION")
            val isFullscreen = (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && isFullscreen) {
                val rect = Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val params = binding.root.layoutParams as FrameLayout.LayoutParams
                if (keypadHeight > screenHeight * 0.15) {
                    if (params.bottomMargin != keypadHeight) {
                        params.bottomMargin = keypadHeight
                        binding.root.layoutParams = params
                    }
                } else {
                    if (params.bottomMargin != 0) {
                        params.bottomMargin = 0
                        binding.root.layoutParams = params
                    }
                }
            }
        }
    }

    fun scrollChange() {
        val ebWebView = state.ebWebView
        ebWebView.setScrollChangeListener(object : EBWebView.OnScrollChangeListener {
            override fun onScrollChange(scrollY: Int, oldScrollY: Int) {
                ebWebView.updatePageInfo()
                twoPaneControllerProvider()?.scrollChange(scrollY - oldScrollY)
                if (!config.ui.shouldHideToolbar) return
                val height = floor(ebWebView.contentHeight * ebWebView.resources.displayMetrics.density.toDouble()).toInt()
                val webViewHeight = ebWebView.height
                val cutoff = height - webViewHeight - 112 * activity.resources.displayMetrics.density.roundToInt()
                if (scrollY in (oldScrollY + 1)..cutoff) {
                    if (state.binding.appBar.isVisible) toggleFullscreen()
                }
            }
        })
    }

    fun createMultiTouchTouchListener(ebWebView: EBWebView): MultitouchListener =
        object : MultitouchListener(activity, ebWebView) {
            private var longPressStartPoint: Point? = null
            override fun onSwipeTop() = gestureHandler.handle(config.touch.multitouchUp)
            override fun onSwipeBottom() = gestureHandler.handle(config.touch.multitouchDown)
            override fun onSwipeRight() = gestureHandler.handle(config.touch.multitouchRight)
            override fun onSwipeLeft() = gestureHandler.handle(config.touch.multitouchLeft)
            override fun onLongPressMove(motionEvent: MotionEvent) {
                super.onLongPressMove(motionEvent)
                if (config.touch.enableDragUrlToAction && contextMenuDelegate.isInLongPressMode && contextMenuDelegate.activeContextMenuDialog != null) {
                    contextMenuDelegate.activeContextMenuDialog?.updateHoveredItem(motionEvent.rawX, motionEvent.rawY)
                    return
                }
                if (longPressStartPoint == null) {
                    longPressStartPoint = Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                    return
                }
                if (abs(motionEvent.x - (longPressStartPoint?.x ?: 0)) > ViewUnit.dpToPixel(15) ||
                    abs(motionEvent.y - (longPressStartPoint?.y ?: 0)) > ViewUnit.dpToPixel(15)
                ) {
                    actionModeDelegate.actionModeViewRef?.visibility = INVISIBLE
                    longPressStartPoint = null
                }
            }
            override fun onMoveDone(motionEvent: MotionEvent) {
                if (contextMenuDelegate.isInLongPressMode && contextMenuDelegate.activeContextMenuDialog != null) {
                    contextMenuDelegate.activeContextMenuDialog?.onFingerLifted()
                    contextMenuDelegate.activeContextMenuDialog = null
                    contextMenuDelegate.isInLongPressMode = false
                    return
                }
            }
        }.apply { activity.lifecycle.addObserver(this) }

    fun dispose() {
        touchControllerInternal?.dispose()
    }
}
