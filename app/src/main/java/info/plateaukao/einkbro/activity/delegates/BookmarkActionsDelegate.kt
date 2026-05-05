package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.unit.pruneWebTitle
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.compose.BookmarksDialogFragment
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel

class BookmarkActionsDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val bookmarkViewModel: BookmarkViewModel,
    private val overviewDialogControllerProvider: () -> OverviewDialogController,
    private val updateAlbum: (String?) -> Unit,
    private val addAlbum: (String, String, Boolean) -> Unit,
    private val toggleSplitScreen: (String?) -> Unit,
) {
    fun saveBookmark(url: String?, title: String?) {
        val currentUrl = url ?: state.ebWebView.url ?: return
        val nonNullTitle = title ?: HelperUnit.secString(state.ebWebView.title)
        try {
            BookmarkEditDialog(
                bookmarkViewModel,
                Bookmark(
                    nonNullTitle.pruneWebTitle(),
                    currentUrl,
                    order = if (ViewUnit.isWideLayout(activity)) 999 else 0,
                ),
                { ViewUnit.hideKeyboard(activity); EBToast.show(activity, R.string.toast_edit_successful) },
                { ViewUnit.hideKeyboard(activity) },
            ).show(activity.supportFragmentManager, "bookmark_edit")
        } catch (e: Exception) {
            e.printStackTrace()
            EBToast.show(activity, R.string.toast_error)
        }
    }

    fun createShortcut() = BrowserUnit.createShortcut(activity, state.ebWebView)

    fun shareLink() = IntentUnit.share(activity, state.ebWebView.title, state.ebWebView.url)

    fun openHistoryPage(amount: Int) = overviewDialogControllerProvider().openHistoryPage(amount)

    fun openBookmarkPage() {
        BookmarksDialogFragment(
            activity.lifecycleScope,
            bookmarkViewModel,
            gotoUrlAction = { url -> updateAlbum(url) },
            bookmarkIconClickAction = { title, url, isForeground -> addAlbum(title, url, isForeground) },
            splitScreenAction = { url -> toggleSplitScreen(url) },
        ).show(activity.supportFragmentManager, "bookmarks dialog")
    }

    fun prepareRecord(): Boolean {
        val webView = state.currentAlbumController as EBWebView
        val title = webView.title
        val url = webView.url
        return (title.isNullOrEmpty() || url.isNullOrEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT))
    }
}
