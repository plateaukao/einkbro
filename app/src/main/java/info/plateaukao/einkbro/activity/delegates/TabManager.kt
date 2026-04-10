package info.plateaukao.einkbro.activity.delegates

import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AlbumController
import info.plateaukao.einkbro.browser.BrowserContainer
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.AlbumInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
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
    private val browserContainer: BrowserContainer,
    private val albumViewModel: AlbumViewModel,
    private val bookmarkManager: BookmarkManager,
    private val externalSearchViewModel: ExternalSearchViewModel,
    private val webViewProvider: () -> EBWebView,
    private val currentAlbumControllerProvider: () -> AlbumController?,
    private val setCurrentAlbumController: (AlbumController?) -> Unit,
    private val setEbWebView: (EBWebView) -> Unit,
    private val mainContentLayoutProvider: () -> FrameLayout,
    private val progressBarProvider: () -> View,
    private val composeToolbarViewControllerProvider: () -> ComposeToolbarViewController,
    private val fabImageViewControllerProvider: () -> FabImageViewController,
    private val createWebView: () -> EBWebView,
    private val createTouchListener: (EBWebView) -> View.OnTouchListener,
    private val keyHandlerSetWebView: (EBWebView) -> Unit,
    private val addHistoryAction: (String, String) -> Unit,
    private val adFilterProvider: () -> AdFilter,
    private val updateLanguageLabel: () -> Unit,
) {
    private var preloadedWebView: EBWebView? = null
    private val dialogManager: DialogManager by lazy { DialogManager(activity) }

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

        when (config.newTabBehavior) {
            info.plateaukao.einkbro.preference.NewTabBehavior.START_INPUT -> {
                addAlbum(activity.getString(R.string.app_name), "")
                focusOnInput()
            }

            info.plateaukao.einkbro.preference.NewTabBehavior.SHOW_HOME -> addAlbum("", config.favoriteUrl)
            info.plateaukao.einkbro.preference.NewTabBehavior.SHOW_RECENT_BOOKMARKS -> {
                addAlbum("", "")
                BrowserUnit.loadRecentlyUsedBookmarks(webViewProvider())
            }
        }
    }

    fun duplicateTab() {
        val webView = currentAlbumControllerProvider() as EBWebView
        val title = webView.title.orEmpty()
        val url = webView.url ?: return
        addAlbum(title, url)
    }

    fun addNewTab(url: String) = addAlbum(url = url)

    fun isCurrentAlbum(albumController: AlbumController): Boolean =
        currentAlbumControllerProvider() == albumController

    @SuppressLint("ClickableViewAccessibility")
    fun addAlbum(
        title: String = "",
        url: String = config.favoriteUrl,
        foreground: Boolean = true,
        incognito: Boolean = false,
        enablePreloadWebView: Boolean = true,
    ) {
        val newWebView = (preloadedWebView ?: createWebView()).apply {
            this.albumTitle = title
            this.incognito = incognito
            setOnTouchListener(createTouchListener(this))
        }

        maybeCreateNewPreloadWebView(enablePreloadWebView, newWebView)

        updateTabPreview(newWebView, url)
        updateWebViewCount()

        loadUrlInWebView(foreground, newWebView, url)

        updateSavedAlbumInfo()

        if (config.adBlock) {
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
        bookmarkManager.findFaviconBy(url)?.getBitmap()?.let {
            newWebView.setAlbumCover(it)
        }

        val album = newWebView.album
        val currentAlbumController = currentAlbumControllerProvider()
        if (currentAlbumController != null) {
            val index = browserContainer.indexOf(currentAlbumController) + 1
            browserContainer.add(newWebView, index)
            albumViewModel.addAlbum(album, index)
        } else {
            browserContainer.add(newWebView)
            albumViewModel.addAlbum(album, browserContainer.size() - 1)
        }
    }

    private fun loadUrlInWebView(foreground: Boolean, webView: EBWebView, url: String) {
        if (!foreground) {
            webView.deactivate()
            if (config.enableWebBkgndLoad) {
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

    fun showAlbum(controller: AlbumController) {
        val currentAlbumController = currentAlbumControllerProvider()
        val ebWebView = webViewProvider()
        if (currentAlbumController != null) {
            if (currentAlbumController == controller) {
                if (ebWebView.isAtTop()) {
                    ebWebView.reload()
                } else {
                    ebWebView.jumpToTop()
                }
                return
            }
            currentAlbumController.deactivate()
        }

        val mainContentLayout = mainContentLayoutProvider()
        val controllerView = controller as View
        if (mainContentLayout.childCount > 0) {
            for (i in 0 until mainContentLayout.childCount) {
                if (mainContentLayout.getChildAt(i) == controllerView) {
                    mainContentLayout.removeView(controllerView)
                    break
                }
            }
        }

        mainContentLayout.addView(
            controller as View,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setCurrentAlbumController(controller)
        controller.activate()

        updateSavedAlbumInfo()
        updateWebViewCount()

        progressBarProvider().visibility = View.GONE
        val newEbWebView = controller as EBWebView
        setEbWebView(newEbWebView)
        keyHandlerSetWebView(newEbWebView)

        updateTitle()
        newEbWebView.updatePageInfo()

        externalSearchViewModel.setButtonVisibility(false)
        activity.runOnUiThread {
            val index = albumViewModel.albums.value.indexOfFirst { it.isActivated }
            composeToolbarViewControllerProvider().updateFocusIndex(index)
            albumViewModel.focusIndex.intValue = index
        }
        updateLanguageLabel()
    }

    fun removeAlbum(albumController: AlbumController, showHome: Boolean) {
        closeTabConfirmation {
            if (config.isSaveHistoryWhenClose()) {
                addHistoryAction(albumController.albumTitle, albumController.albumUrl)
            }

            albumViewModel.removeAlbum(albumController.album)
            val removeIndex = browserContainer.indexOf(albumController)
            val currentIndex = browserContainer.indexOf(currentAlbumControllerProvider())
            browserContainer.remove(albumController)

            updateSavedAlbumInfo()
            updateWebViewCount()

            if (browserContainer.isEmpty()) {
                if (!showHome) {
                    activity.finish()
                } else {
                    webViewProvider().loadUrl(config.favoriteUrl)
                }
            } else {
                if (removeIndex == currentIndex) {
                    showAlbum(browserContainer[getNextAlbumIndexAfterRemoval(removeIndex)])
                }
            }
        }
    }

    fun removeCurrentAlbum() {
        currentAlbumControllerProvider()?.let { removeAlbum(it, showHome = false) }
    }

    fun updateSavedAlbumInfo() {
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
                AlbumInfo(
                    controller.albumTitle,
                    controller.albumUrl.ifBlank { controller.initAlbumUrl },
                )
            }
        config.savedAlbumInfoList = albumInfoList
        config.currentAlbumIndex = browserContainer.indexOf(currentAlbumControllerProvider())
        if (albumInfoList.isNotEmpty() && config.currentAlbumIndex >= albumInfoList.size) {
            config.currentAlbumIndex = albumInfoList.size - 1
        }
    }

    fun updateWebViewCount() {
        val subScript = browserContainer.size()
        val superScript = browserContainer.indexOf(currentAlbumControllerProvider()) + 1
        val countString = ViewUnit.createCountString(superScript, subScript)
        composeToolbarViewControllerProvider().updateTabCount(countString)
        fabImageViewControllerProvider().updateTabCount(countString)
    }

    fun updateAlbum(url: String?) {
        if (url == null) return
        (currentAlbumControllerProvider() as EBWebView).loadUrl(url)
        updateTitle()
        updateSavedAlbumInfo()
    }

    fun updateTitle() {
        val ebWebView = webViewProvider()
        if (ebWebView === currentAlbumControllerProvider()) {
            composeToolbarViewControllerProvider().updateTitle(ebWebView.title.orEmpty())
        }
    }

    fun updateTitle(title: String?) {
        composeToolbarViewControllerProvider().updateTitle(title.orEmpty())
    }

    fun gotoLeftTab() {
        nextAlbumController(false)?.let { showAlbum(it) }
    }

    fun gotoRightTab() {
        nextAlbumController(true)?.let { showAlbum(it) }
    }

    fun getUrlMatchedBrowser(url: String): EBWebView? {
        return browserContainer.list().firstOrNull { it.albumUrl == url } as EBWebView?
    }

    private fun getNextAlbumIndexAfterRemoval(removeIndex: Int): Int =
        if (config.shouldShowNextAfterRemoveTab) min(browserContainer.size() - 1, removeIndex)
        else max(0, removeIndex - 1)

    private fun nextAlbumController(next: Boolean): AlbumController? {
        if (browserContainer.size() <= 1) {
            return currentAlbumControllerProvider()
        }

        val list = browserContainer.list()
        var index = list.indexOf(currentAlbumControllerProvider())
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
        if (!config.confirmTabClose) {
            okAction()
        } else {
            dialogManager.showOkCancelDialog(
                messageResId = R.string.toast_close_tab,
                okAction = okAction,
            )
        }
    }
}
