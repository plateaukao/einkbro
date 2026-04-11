package info.plateaukao.einkbro.activity.delegates

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.epub.EpubManager
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionDisplay
import info.plateaukao.einkbro.preference.GptActionScope
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.HelperUnit.toNormalScheme
import info.plateaukao.einkbro.util.Constants.Companion.ACTION_DICT
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.viewControllers.OverviewDialogController
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class IntentDispatchDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val externalSearchViewModel: ExternalSearchViewModel,
    private val remoteConnViewModel: RemoteConnViewModel,
    private val translationViewModel: TranslationViewModel,
    private val overviewDialogControllerProvider: () -> OverviewDialogController,
    private val addAlbum: (title: String, url: String, foreground: Boolean) -> Unit,
    private val updateAlbum: (url: String) -> Unit,
    private val showAlbum: (controller: info.plateaukao.einkbro.browser.AlbumController) -> Unit,
    private val getUrlMatchedBrowser: (url: String) -> EBWebView?,
    private val openHistoryPage: () -> Unit,
    private val openBookmarkPage: () -> Unit,
    private val focusOnInput: () -> Unit,
    private val readArticle: () -> Unit,
    private val chatWithWeb: (useSplitScreen: Boolean, content: String?, runWithAction: ChatGPTActionInfo?) -> Unit,
    private val showTranslationDialog: (isWholePageMode: Boolean) -> Unit,
) {
    private val epubManager: EpubManager by lazy { EpubManager(activity) }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    @SuppressLint("InlinedApi")
    fun dispatchIntent(intent: Intent) {
        if (overviewDialogControllerProvider().isVisible()) {
            overviewDialogControllerProvider().hide()
        }

        when (intent.action) {
            "", Intent.ACTION_MAIN -> {
                initSavedTabs { addAlbum("", config.favoriteUrl, true) }
            }

            ACTION_VIEW -> {
                initSavedTabs()
                val viewUri = intent.data?.toNormalScheme() ?: return
                if (viewUri.scheme == "content") {
                    activity.lifecycleScope.launch {
                        val (filename, mimeType) = withContext(Dispatchers.IO) {
                            val (fName, _) = HelperUnit.getFileInfoFromContentUri(activity, viewUri)
                            val mType = activity.contentResolver.getType(viewUri)
                            Pair(fName, mType)
                        }

                        if (filename?.endsWith(".srt") == true ||
                            mimeType == "application/x-subrip"
                        ) {
                            addAlbum("", config.favoriteUrl, true)
                            val htmlContent = withContext(Dispatchers.IO) {
                                val stringList =
                                    HelperUnit.readContentAsStringList(activity.contentResolver, viewUri)
                                HelperUnit.srtToHtml(stringList)
                            }
                            val ebWebView = state.ebWebView
                            ebWebView.isPlainText = true
                            ebWebView.rawHtmlCache = htmlContent
                            ebWebView.loadData(htmlContent, "text/html", "utf-8")

                        } else if (mimeType == "application/octet-stream") {
                            val cachedPath = withContext(Dispatchers.IO) {
                                HelperUnit.getCachedPathFromURI(activity, viewUri)
                            }
                            cachedPath.let {
                                addAlbum("", "file://$it", true)
                            }
                        } else if (filename?.endsWith(".mht") == true) {
                            val cachedPath = withContext(Dispatchers.IO) {
                                HelperUnit.getCachedPathFromURI(activity, viewUri)
                            }
                            addAlbum("", "file://$cachedPath", true)
                        } else if (filename?.endsWith(".html") == true || mimeType == "text/html") {
                            updateAlbum(viewUri.toString())
                        } else {
                            epubManager.showEpubReader(viewUri)
                            activity.finish()
                        }
                    }
                } else {
                    val url = viewUri.toString()
                    getUrlMatchedBrowser(url)?.let { showAlbum(it) }
                        ?: addAlbum("", url, true)
                }
            }

            Intent.ACTION_WEB_SEARCH -> {
                initSavedTabs()
                val searchedKeyword = intent.getStringExtra(SearchManager.QUERY).orEmpty()
                if (state.currentAlbumController != null && config.ai.isExternalSearchInSameTab) {
                    state.ebWebView.loadUrl(searchedKeyword)
                } else {
                    addAlbum("", searchedKeyword, true)
                }
            }

            "sc_history" -> {
                addAlbum("", config.favoriteUrl, true); openHistoryPage()
            }

            "sc_home" -> {
                addAlbum("", config.favoriteUrl, true)
            }

            "sc_bookmark" -> {
                addAlbum("", config.favoriteUrl, true); openBookmarkPage()
            }

            "sc_disable_adblock" -> {
                config.adBlock = false
                BrowserUnit.restartApp(activity)
            }

            Intent.ACTION_SEND -> {
                initSavedTabs()
                val sentKeyword = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val url =
                    if (BrowserUnit.isURL(sentKeyword)) sentKeyword else externalSearchViewModel.generateSearchUrl(
                        sentKeyword
                    )
                if (state.currentAlbumController != null && config.ai.isExternalSearchInSameTab) {
                    state.ebWebView.loadUrl(url)
                } else {
                    addAlbum("", url, true)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                initSavedTabs()
                val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: return

                if (remoteConnViewModel.isSendingTextSearch) {
                    remoteConnViewModel.sendTextSearch(
                        externalSearchViewModel.generateSearchUrl(text)
                    )
                    activity.moveTaskToBack(true)
                    return
                }

                val url =
                    if (BrowserUnit.isURL(text)) text else externalSearchViewModel.generateSearchUrl(text)
                if (state.currentAlbumController != null && config.ai.isExternalSearchInSameTab) {
                    state.ebWebView.loadUrl(url)
                } else {
                    addAlbum("", url, true)
                }
                externalSearchViewModel.setButtonVisibility(true)
            }

            ACTION_DICT -> {
                val text = intent.getStringExtra("EXTRA_QUERY") ?: return

                if (remoteConnViewModel.isSendingTextSearch) {
                    remoteConnViewModel.sendTextSearch(
                        externalSearchViewModel.generateSearchUrl(text)
                    )
                    activity.moveTaskToBack(true)
                    return
                }

                initSavedTabs()
                val url = externalSearchViewModel.generateSearchUrl(text)
                if (state.currentAlbumController != null && config.ai.isExternalSearchInSameTab) {
                    state.ebWebView.loadUrl(url)
                } else {
                    addAlbum("", url, true)
                }
                externalSearchViewModel.setButtonVisibility(true)
            }

            info.plateaukao.einkbro.activity.BrowserActivity.ACTION_READ_ALOUD -> readArticle()

            null -> {
                if (state.currentAlbumController == null) {
                    initSavedTabs { addAlbum("", config.favoriteUrl, true) }
                }
            }

            else -> addAlbum("", config.favoriteUrl, true)
        }
        activity.intent.action = ""
    }

    fun initSavedTabs(whenNoSavedTabs: (() -> Unit)? = null) {
        if (state.currentAlbumController == null) {
            if (config.savedAlbumInfoList.isNotEmpty() &&
                (config.shouldSaveTabs || shouldLoadTabState)
            ) {
                if (config.currentAlbumIndex >= config.savedAlbumInfoList.size) {
                    config.currentAlbumIndex = config.savedAlbumInfoList.size - 1
                }
                val albumList = config.savedAlbumInfoList.toList()
                var savedIndex = config.currentAlbumIndex
                if (savedIndex == -1) savedIndex = 0
                albumList.forEachIndexed { index, albumInfo ->
                    addAlbum(
                        albumInfo.title,
                        albumInfo.url,
                        index == savedIndex,
                    )
                }
            } else {
                whenNoSavedTabs?.invoke()
            }
        }
    }

    var shouldLoadTabState: Boolean = false

}
