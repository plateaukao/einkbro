package info.plateaukao.einkbro.activity.delegates

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.browser.AlbumCallback
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.browser.LazyAlbumController
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.AlbumInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.viewmodel.AlbumViewModel
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import io.github.edsuns.adfilter.AdFilter
import kotlin.math.max
import kotlin.math.min

class TabManager(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val browserContainer: BrowserContainer,
    private val albumViewModel: AlbumViewModel,
    private val bookmarkManager: BookmarkManager,
    private val externalSearchViewModel: ExternalSearchViewModel,
    private val createWebView: () -> EBWebView,
    private val createTouchListener: (EBWebView) -> View.OnTouchListener,
    private val keyHandlerSetWebView: (EBWebView) -> Unit,
    private val addHistoryAction: (String, String) -> Unit,
    private val adFilterProvider: () -> AdFilter,
    private val updateLanguageLabel: () -> Unit,
) {
    private var preloadedWebView: EBWebView? = null
    private val dialogManager: DialogManager by lazy { DialogManager(activity) }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val saveAlbumInfoRunnable = Runnable { updateSavedAlbumInfo() }

    fun destroyPreloadedWebView() {
        preloadedWebView?.destroy()
        preloadedWebView = null
    }

    fun newATab(
        searchOnSite: Boolean,
        hideSearchPanel: () -> Unit,
        focusOnInput: () -> Unit,
    ) {
        if (searchOnSite) {
            hideSearchPanel()
        }

        when (config.tab.newTabBehavior) {
            info.plateaukao.einkbro.preference.NewTabBehavior.START_INPUT -> {
                addAlbum(activity.getString(R.string.app_name), "")
                focusOnInput()
            }

            info.plateaukao.einkbro.preference.NewTabBehavior.SHOW_HOME -> addAlbum("", config.favoriteUrl)
            info.plateaukao.einkbro.preference.NewTabBehavior.SHOW_RECENT_BOOKMARKS -> {
                addAlbum("", "")
                BrowserUnit.loadRecentlyUsedBookmarks(state.ebWebView)
            }

            info.plateaukao.einkbro.preference.NewTabBehavior.SHOW_START_PAGE ->
                addAlbum("", Constants.START_PAGE_URL)
        }
    }

    fun duplicateTab() {
        val webView = state.currentAlbumController as EBWebView
        val title = webView.title.orEmpty()
        val url = webView.url ?: return
        addAlbum(title, url)
    }

    fun addNewTab(url: String) = addAlbum(url = url)

    fun isCurrentAlbum(albumController: AlbumController): Boolean =
        state.currentAlbumController == albumController

    @SuppressLint("ClickableViewAccessibility")
    fun addAlbum(
        title: String = "",
        url: String = config.favoriteUrl,
        foreground: Boolean = true,
        incognito: Boolean = false,
        enablePreloadWebView: Boolean = true,
        lazyLoad: Boolean = false,
    ) {
        // the preloaded webview snapshots settings at creation time; desktop
        // mode may have been toggled since, so refresh the user agent
        val newWebView = (preloadedWebView?.also { it.updateUserAgentString() }
            ?: createWebView()).apply {
            this.albumTitle = title
            this.incognito = incognito
            setOnTouchListener(createTouchListener(this))
        }

        maybeCreateNewPreloadWebView(enablePreloadWebView, newWebView)

        updateTabPreview(newWebView, url)
        updateWebViewCount()

        loadUrlInWebView(foreground, newWebView, url, lazyLoad)

        updateSavedAlbumInfo()

        if (config.browser.adBlock) {
            adFilterProvider().setupWebView(newWebView)
        }
    }

    private fun maybeCreateNewPreloadWebView(
        enablePreloadWebView: Boolean,
        newWebView: EBWebView,
    ) {
        preloadedWebView = null
        if (enablePreloadWebView) {
            newWebView.postDelayed({
                if (preloadedWebView == null) {
                    preloadedWebView = createWebView()
                }
            }, 500)
        }
    }

    private fun updateTabPreview(newWebView: EBWebView, url: String) {
        bookmarkManager.findFaviconBitmapBy(url)?.let {
            newWebView.setAlbumCover(it)
        }

        val album = newWebView.album
        val currentAlbumController = state.currentAlbumController
        if (currentAlbumController != null) {
            val index = browserContainer.indexOf(currentAlbumController) + 1
            browserContainer.add(newWebView, index)
            albumViewModel.addAlbum(album, index)
        } else {
            browserContainer.add(newWebView)
            albumViewModel.addAlbum(album, browserContainer.size() - 1)
        }
    }

    private fun loadUrlInWebView(
        foreground: Boolean,
        webView: EBWebView,
        url: String,
        lazyLoad: Boolean = false,
    ) {
        if (!foreground) {
            webView.deactivate()
            if (!lazyLoad && config.tab.enableWebBkgndLoad) {
                webView.loadUrl(url)
            } else {
                webView.initAlbumUrl = url
            }
        } else {
            showAlbum(webView)
            if (url.isNotEmpty() && url != BrowserUnit.URL_ABOUT_BLANK) {
                webView.loadUrl(url)
            } else if (url == BrowserUnit.URL_ABOUT_BLANK) {
            }
        }
    }

    /**
     * Adds a restored background tab without creating its WebView: the saved
     * title/url shows in the tab list, and the real EBWebView is built on
     * first activation (see [materializeIfNeeded]).
     */
    fun addRestoredTab(title: String, url: String) {
        val controller = LazyAlbumController(title, url, activity as? AlbumCallback)
        browserContainer.add(controller)
        albumViewModel.addAlbum(controller.album, browserContainer.size() - 1)
        updateWebViewCount()
        // The foreground tab's addAlbum saves mid-restore with a partial list;
        // one debounced save after the loop settles makes the list whole again.
        updateSavedAlbumInfoDebounced()
    }

    /** Swaps a [LazyAlbumController] for a real EBWebView, keeping its Album. */
    private fun materializeIfNeeded(controller: AlbumController): AlbumController {
        val lazy = controller as? LazyAlbumController ?: return controller
        val webView = (preloadedWebView?.also { it.updateUserAgentString() }
            ?: createWebView()).apply {
            setOnTouchListener(createTouchListener(this))
        }
        maybeCreateNewPreloadWebView(true, webView)

        lazy.album.attachController(webView)
        webView.album = lazy.album
        webView.albumTitle = lazy.albumTitle
        webView.initAlbumUrl = lazy.initAlbumUrl
        browserContainer.replace(lazy, webView)

        if (config.browser.adBlock) {
            adFilterProvider().setupWebView(webView)
        }
        return webView
    }

    fun showAlbum(controller: AlbumController) {
        @Suppress("NAME_SHADOWING")
        val controller = materializeIfNeeded(controller)
        val currentAlbumController = state.currentAlbumController
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
                val ebWebView = state.ebWebView
                if (ebWebView.isAtTop()) {
                    ebWebView.reload()
                } else {
                    ebWebView.jumpToTop()
                }
                return
            }
            currentAlbumController.deactivate()
            // GONE views skip measure/layout/draw; a VISIBLE background tab is
            // still fully drawn on every layout pass (FrameLayout doesn't cull).
            (currentAlbumController as? View)?.visibility = View.GONE
        }

        val mainContentLayout = state.mainContentLayout
        val controllerView = controller as View
        // Re-parenting a Chromium WebView is a full detach/attach cycle; a tab
        // that is already a (GONE) child only needs its visibility flipped.
        // Z-order doesn't matter since every sibling is GONE.
        if (controllerView.parent !== mainContentLayout) {
            (controllerView.parent as? ViewGroup)?.removeView(controllerView)
            mainContentLayout.addView(
                controllerView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        controllerView.visibility = View.VISIBLE

        state.currentAlbumController = (controller)
        controller.activate()

        updateSavedAlbumInfo()
        updateWebViewCount()

        state.progressBar.visibility = View.GONE
        state.progressBarVertical.visibility = View.GONE
        val newEbWebView = controller as EBWebView
        state.ebWebView = (newEbWebView)
        keyHandlerSetWebView(newEbWebView)

        updateTitle()
        newEbWebView.updatePageInfo()
        // Re-apply the current style config: font/style changes made while this
        // tab was in the background only touched the foreground tab. The slot
        // update is idempotent and skips the DOM write when nothing changed.
        newEbWebView.updateCssStyle()

        externalSearchViewModel.setButtonVisibility(false)
        activity.runOnUiThread {
            val index = albumViewModel.albums.value.indexOfFirst { it.isActivated }
            state.composeToolbarViewController.updateFocusIndex(index)
            albumViewModel.focusIndex.intValue = index
        }
        updateLanguageLabel()
    }

    fun removeAlbum(albumController: AlbumController, showHome: Boolean) {
        closeTabConfirmation {
            if (config.tab.isSaveHistoryWhenClose() && !albumController.isAIPage) {
                addHistoryAction(albumController.albumTitle, albumController.albumUrl)
            }

            albumViewModel.removeAlbum(albumController.album)
            val removeIndex = browserContainer.indexOf(albumController)
            val currentIndex = browserContainer.indexOf(state.currentAlbumController)
            browserContainer.remove(albumController)

            updateSavedAlbumInfo()
            updateWebViewCount()

            if (browserContainer.isEmpty()) {
                if (!showHome) {
                    activity.finish()
                } else {
                    state.ebWebView.loadUrl(config.favoriteUrl)
                }
            } else {
                if (removeIndex == currentIndex) {
                    showAlbum(browserContainer[getNextAlbumIndexAfterRemoval(removeIndex)])
                } else {
                    // Removing another tab shifts indices; refresh the focus highlight
                    // (showAlbum does this for the removeIndex == currentIndex case).
                    val index = albumViewModel.albums.value.indexOfFirst { it.isActivated }
                    state.composeToolbarViewController.updateFocusIndex(index)
                    albumViewModel.focusIndex.intValue = index
                }
            }
        }
    }

    /**
     * Rebuilds the tab whose WebView lost its renderer (see
     * EBWebViewClient.onRenderProcessGone). A fresh EBWebView takes over the dead
     * one's Album, so the tab keeps its slot and title in the tab list, and its URL
     * is loaded again. Returns false when [crashed] is not a tab of this container.
     */
    // Album id -> time of its last renderer-crash recovery. Per tab, because one
    // renderer serves every tab, so a single kill legitimately lands here once per
    // affected tab in quick succession.
    private val crashRecoveryTimes = mutableMapOf<Int, Long>()

    fun recoverCrashedTab(crashed: EBWebView): Boolean {
        if (crashed === preloadedWebView) {
            preloadedWebView = null
            crashed.destroy()
            return true
        }
        if (browserContainer.indexOf(crashed) < 0) return false

        // A page that kills its renderer on every load (chrome://crash, or one that
        // reliably exhausts memory) would otherwise reload itself forever; after a
        // quick second death the tab comes back empty instead.
        val album = crashed.album
        val now = System.currentTimeMillis()
        val lastRecovery = crashRecoveryTimes[album.id] ?: 0L
        val reloadUrl = if (now - lastRecovery > CRASH_LOOP_WINDOW_MS) {
            crashed.albumUrl.ifBlank { crashed.initAlbumUrl }
        } else ""
        crashRecoveryTimes[album.id] = now
        val wasCurrent = state.currentAlbumController === crashed
        val replacement = (preloadedWebView?.also { it.updateUserAgentString() }
            ?: createWebView()).apply {
            incognito = crashed.incognito
            setOnTouchListener(createTouchListener(this))
        }
        maybeCreateNewPreloadWebView(true, replacement)

        album.attachController(replacement)
        replacement.album = album
        browserContainer.replace(crashed, replacement)
        (crashed.parent as? ViewGroup)?.removeView(crashed)
        crashed.destroy()

        if (config.browser.adBlock) {
            adFilterProvider().setupWebView(replacement)
        }

        if (wasCurrent) {
            // The dead tab must not be deactivated or compared against by showAlbum.
            state.currentAlbumController = null
            showAlbum(replacement)
            if (reloadUrl.isNotEmpty()) replacement.loadUrl(reloadUrl)
        } else {
            replacement.deactivate()
            // Load on next activation, the same way a restored background tab does.
            album.isLoaded = false
            replacement.initAlbumUrl = reloadUrl
            updateSavedAlbumInfo()
        }
        EBToast.show(activity, R.string.page_crashed_reloading)
        return true
    }

    fun removeCurrentAlbum() {
        state.currentAlbumController?.let { removeAlbum(it, showHome = false) }
    }

    // Batch close: one confirmation for all tabs. Going through removeAlbum
    // per tab popped one dialog per tab and ended with loadUrl on a
    // just-destroyed WebView.
    fun closeAllTabs(onAllClosed: () -> Unit) {
        closeTabConfirmation {
            if (config.tab.isSaveHistoryWhenClose()) {
                browserContainer.list().forEach {
                    if (!it.isAIPage) addHistoryAction(it.albumTitle, it.albumUrl)
                }
            }
            albumViewModel.clearAlbums()
            browserContainer.clear()
            state.currentAlbumController = null
            updateSavedAlbumInfo()
            updateWebViewCount()
            onAllClosed()
        }
    }

    fun updateSavedAlbumInfoDebounced() {
        uiHandler.removeCallbacks(saveAlbumInfoRunnable)
        uiHandler.postDelayed(saveAlbumInfoRunnable, SAVE_ALBUM_INFO_DEBOUNCE_MS)
    }

    fun updateSavedAlbumInfo() {
        uiHandler.removeCallbacks(saveAlbumInfoRunnable)
        // Fall back to the last saved title while a page is still loading, so a kill
        // mid-load doesn't replace a resolved title with the "..." placeholder
        val previousTitles = config.tab.savedAlbumInfoList.associate { it.url to it.title }
        val albumControllers = browserContainer.list()
        val albumInfoList = albumControllers
            .filter { !it.isTranslatePage }
            .filter { !it.isAIPage }
            .filter { !it.albumUrl.startsWith("data") }
            .filter {
                (it.albumUrl.isNotBlank() && it.albumUrl != BrowserUnit.URL_ABOUT_BLANK) ||
                        it.initAlbumUrl.isNotBlank()
            }
            .map { controller ->
                val url = controller.albumUrl.ifBlank { controller.initAlbumUrl }
                val title = controller.albumTitle
                    .takeUnless { it.isBlank() || it == EBWebView.LOADING_TITLE }
                    ?: previousTitles[url].orEmpty()
                AlbumInfo(title, url)
            }
        config.tab.savedAlbumInfoList = albumInfoList
        config.tab.currentAlbumIndex = browserContainer.indexOf(state.currentAlbumController)
        if (albumInfoList.isNotEmpty() && config.tab.currentAlbumIndex >= albumInfoList.size) {
            config.tab.currentAlbumIndex = albumInfoList.size - 1
        }
    }

    fun updateWebViewCount() {
        val subScript = browserContainer.size()
        val superScript = browserContainer.indexOf(state.currentAlbumController) + 1
        val countString = ViewUnit.createCountString(superScript, subScript)
        state.composeToolbarViewController.updateTabCount(countString)
        state.fabImageViewController.updateTabCount(countString)
    }

    fun updateAlbum(url: String?) {
        if (url == null) return
        (state.currentAlbumController as EBWebView).loadUrl(url)
        updateTitle()
        updateSavedAlbumInfo()
    }

    fun updateTitle() {
        val ebWebView = state.ebWebView
        if (ebWebView === state.currentAlbumController) {
            state.composeToolbarViewController.updateTitle(ebWebView.title.orEmpty())
        }
    }

    fun updateTitle(title: String?) {
        state.composeToolbarViewController.updateTitle(title.orEmpty())
    }

    fun gotoLeftTab() {
        nextAlbumController(false)?.let { showAlbum(it) }
    }

    fun gotoRightTab() {
        nextAlbumController(true)?.let { showAlbum(it) }
    }

    fun getUrlMatchedBrowser(url: String): EBWebView? {
        val matched = browserContainer.list().firstOrNull {
            it.albumUrl == url || (it.albumUrl.isBlank() && it.initAlbumUrl == url)
        } ?: return null
        return materializeIfNeeded(matched) as EBWebView
    }

    private fun getNextAlbumIndexAfterRemoval(removeIndex: Int): Int =
        if (config.tab.shouldShowNextAfterRemoveTab) min(browserContainer.size() - 1, removeIndex)
        else max(0, removeIndex - 1)

    private fun nextAlbumController(next: Boolean): AlbumController? {
        if (browserContainer.size() <= 1) {
            return state.currentAlbumController
        }

        val list = browserContainer.list()
        var index = list.indexOf(state.currentAlbumController)
        if (next) {
            index++
            if (index >= list.size) {
                return list.first()
            }
        } else {
            index--
            if (index < 0) {
                return list.last()
            }
        }
        return list[index]
    }

    private fun closeTabConfirmation(okAction: () -> Unit) {
        if (!config.tab.confirmTabClose) {
            okAction()
        } else {
            dialogManager.showOkCancelDialog(
                messageResId = R.string.toast_close_tab,
                okAction = okAction,
            )
        }
    }

    companion object {
        private const val CRASH_LOOP_WINDOW_MS = 10_000L
        private const val SAVE_ALBUM_INFO_DEBOUNCE_MS = 500L
    }
}
