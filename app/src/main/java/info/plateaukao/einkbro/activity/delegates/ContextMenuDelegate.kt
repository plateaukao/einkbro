package info.plateaukao.einkbro.activity.delegates

import android.graphics.Point
import android.os.Message
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.toRawPoint
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuDialogFragment
import info.plateaukao.einkbro.view.dialog.compose.ContextMenuItemType
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import android.net.Uri
import kotlinx.coroutines.launch

class ContextMenuDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val ttsViewModel: TtsViewModel,
    private val webViewProvider: () -> EBWebView,
    private val addAlbum: (title: String, url: String, foreground: Boolean) -> Unit,
    private val prepareRecord: () -> Boolean,
    private val saveBookmark: (url: String?, title: String?) -> Unit,
    private val toggleSplitScreen: (url: String?) -> Unit,
    private val summarizeLinkContent: (url: String) -> Unit,
    private val translateImage: (url: String) -> Unit,
    private val translateAllImages: (url: String) -> Unit,
    private val saveFile: (url: String, fileName: String) -> Unit,
) {
    private val dialogManager: DialogManager by lazy { DialogManager(activity) }

    var longPressPoint: Point = Point(0, 0)
    var activeContextMenuDialog: ContextMenuDialogFragment? = null
    var isInLongPressMode = false

    fun onLongPress(message: Message, event: MotionEvent?) {
        val ebWebView = webViewProvider()
        if (ebWebView.isSelectingText) return

        longPressPoint = Point(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
        val rawPoint = event?.toRawPoint() ?: Point(0, 0)

        val url = BrowserUnit.getWebViewLinkUrl(ebWebView, message)
        if (url.isNotBlank()) {
            val linkImageUrl = BrowserUnit.getWebViewLinkImageUrl(ebWebView, message)
            BrowserUnit.getWebViewLinkTitle(ebWebView) { linkTitle ->
                val titleText = linkTitle.ifBlank { url }.toString()
                val contextMenuDialog = ContextMenuDialogFragment(
                    url,
                    linkImageUrl.isNotBlank(),
                    config.imageApiKey.isNotBlank(),
                    rawPoint,
                    isEbookMode = config.isEbookModeActive,
                    itemClicked = {
                        handleContextMenuItem(it, titleText, url, linkImageUrl)
                        activeContextMenuDialog = null
                        isInLongPressMode = false
                    },
                    itemLongClicked = {
                        if (it == ContextMenuItemType.TranslateImage) {
                            translateAllImages(linkImageUrl)
                        }
                        activeContextMenuDialog = null
                        isInLongPressMode = false
                    }
                )
                activeContextMenuDialog = contextMenuDialog
                isInLongPressMode = true
                contextMenuDialog.show(activity.supportFragmentManager, "contextMenu")
            }
        } else if (config.isEbookModeActive) {
            ebWebView.clickLinkElement(longPressPoint)
        }
    }

    private fun handleContextMenuItem(
        contextMenuItemType: ContextMenuItemType,
        title: String,
        url: String,
        imageUrl: String,
    ) {
        val ebWebView = webViewProvider()
        when (contextMenuItemType) {
            ContextMenuItemType.NewTabForeground -> addAlbum(title, url, true)
            ContextMenuItemType.NewTabBackground -> addAlbum(title, url, false)
            ContextMenuItemType.ShareLink -> {
                if (prepareRecord()) EBToast.show(activity, activity.getString(R.string.toast_share_failed))
                else IntentUnit.share(activity, title, url)
            }

            ContextMenuItemType.CopyLink -> ShareUtil.copyToClipboard(
                activity,
                BrowserUnit.stripUrlQuery(url)
            )

            ContextMenuItemType.GotoLink -> ebWebView.clickLinkElement(longPressPoint)

            ContextMenuItemType.SelectText -> ebWebView.post {
                ebWebView.selectLinkText(longPressPoint)
            }

            ContextMenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                activity,
                url,
                activity.getString(R.string.menu_open_with)
            )

            ContextMenuItemType.SaveBookmark -> saveBookmark(url, title)
            ContextMenuItemType.SplitScreen -> toggleSplitScreen(url)
            ContextMenuItemType.AdBlock -> confirmAdSiteAddition(imageUrl)

            ContextMenuItemType.TranslateImage -> translateImage(imageUrl)
            ContextMenuItemType.Tts -> addContentToReadList(url)
            ContextMenuItemType.Summarize -> summarizeLinkContent(url)
            ContextMenuItemType.SaveAs -> {
                if (url.startsWith("data:image")) {
                    saveFile(url, "")
                } else {
                    if (imageUrl.isNotBlank()) {
                        dialogManager.showSaveFileDialog(url = imageUrl, saveFile = saveFile)
                    } else {
                        dialogManager.showSaveFileDialog(url = url, saveFile = saveFile)
                    }
                }
            }

            else -> Unit
        }
    }

    private fun confirmAdSiteAddition(url: String) {
        val host = Uri.parse(url).host.orEmpty()
        if (config.adSites.contains(host)) {
            confirmRemoveAdSite(host)
        } else {
            activity.lifecycleScope.launch {
                val domain = TextInputDialog(
                    activity,
                    "Ad domain to be blocked",
                    "",
                    host,
                ).show().orEmpty()

                if (domain.isNotBlank()) {
                    config.adSites = config.adSites.apply { add(domain) }
                    webViewProvider().reload()
                }
            }
        }
    }

    private fun confirmRemoveAdSite(url: String) {
        dialogManager.showOkCancelDialog(
            title = "remove this url from blacklist?",
            okAction = {
                config.adSites = config.adSites.apply { remove(url) }
                webViewProvider().reload()
            }
        )
    }

    private val toBeReadWebView: EBWebView by lazy {
        EBWebView(activity, activity as info.plateaukao.einkbro.browser.WebViewCallback).apply {
            setOnPageFinishedAction {
                activity.lifecycleScope.launch {
                    val content = toBeReadWebView.getRawText()
                    if (content.isNotEmpty()) {
                        ttsViewModel.readArticle(content, toBeReadWebView.title.orEmpty())
                    }
                    if (toBeReadProcessUrlList.isNotEmpty()) {
                        toBeReadProcessUrlList.removeAt(0)
                    }

                    if (toBeReadProcessUrlList.isNotEmpty()) {
                        toBeReadWebView.loadUrl(toBeReadProcessUrlList.removeAt(0))
                    } else {
                        toBeReadWebView.loadUrl("about:blank")
                    }
                }
            }
        }
    }

    private var toBeReadProcessUrlList: MutableList<String> = mutableListOf()
    private fun addContentToReadList(url: String) {
        toBeReadProcessUrlList.add(url)
        if (toBeReadProcessUrlList.size == 1) {
            toBeReadWebView.loadUrl(url)
        }
        EBToast.show(activity, R.string.added_to_read_list)
    }
}
