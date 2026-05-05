package info.plateaukao.einkbro.activity.delegates

import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageButton
import androidx.compose.ui.focus.FocusRequester
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.GptActionDisplay
import info.plateaukao.einkbro.preference.GptActionScope
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.compose.ComposedSearchBar
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.viewControllers.FabImageViewController
import info.plateaukao.einkbro.viewmodel.RemoteConnViewModel
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SearchPanelDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val remoteConnViewModel: RemoteConnViewModel,
    private val translationViewModel: TranslationViewModel,
    private val fabImageViewControllerProvider: () -> FabImageViewController,
    private val showTranslationDialog: (Boolean) -> Unit,
    private val chatWithWeb: (Boolean, String?, ChatGPTActionInfo?) -> Unit,
    private val hideSearchPanel: () -> Unit,
) {
    private val focusRequester = FocusRequester()

    fun initSearchPanel() {
        state.binding.mainSearchPanel.apply {
            visibility = INVISIBLE
            setContent {
                MyTheme {
                    ComposedSearchBar(
                        focusRequester = focusRequester,
                        onTextChanged = { (state.currentAlbumController as EBWebView?)?.findAllAsync(it) },
                        onCloseClick = { hideSearchPanel() },
                        onUpClick = { searchUp(it) },
                        onDownClick = { searchDown(it) },
                    )
                }
            }
        }
    }

    fun showSearchPanel() {
        state.searchOnSite = true
        fabImageViewControllerProvider().hide()
        val binding = state.binding
        binding.mainSearchPanel.visibility = VISIBLE
        binding.mainSearchPanel.post {
            focusRequester.requestFocus()
            ViewUnit.showKeyboard(activity)
        }
        binding.appBar.visibility = VISIBLE
        binding.contentSeparator.visibility = VISIBLE
        ViewUnit.showKeyboard(activity)
    }

    private fun searchUp(text: String) {
        if (text.isEmpty()) {
            EBToast.show(activity, activity.getString(R.string.toast_input_empty))
            return
        }
        ViewUnit.hideKeyboard(activity)
        (state.currentAlbumController as EBWebView).findNext(false)
    }

    private fun searchDown(text: String) {
        if (text.isEmpty()) {
            EBToast.show(activity, activity.getString(R.string.toast_input_empty))
            return
        }
        ViewUnit.hideKeyboard(activity)
        (state.currentAlbumController as EBWebView).findNext(true)
    }

    fun initTextSearchButton() {
        val remoteTextSearch = activity.findViewById<ImageButton>(R.id.remote_text_search)
        remoteTextSearch.setOnClickListener { remoteConnViewModel.reset() }
        activity.lifecycleScope.launch {
            remoteConnViewModel.remoteConnected.collect { connected ->
                remoteTextSearch.setImageResource(
                    if (remoteConnViewModel.isSendingTextSearch) R.drawable.ic_send else R.drawable.ic_receive
                )
                remoteTextSearch.isVisible = connected
            }
        }
    }

    fun toggleReceiveTextSearch() {
        if (remoteConnViewModel.isReceivingLink) {
            remoteConnViewModel.toggleReceiveLink {}
        } else {
            remoteConnViewModel.toggleReceiveLink {
                if (it.startsWith("action")) {
                    val (_, content, actionString) = it.split("|||")
                    val action = Json.decodeFromString<ChatGPTActionInfo>(actionString)
                    if (action.display == GptActionDisplay.Popup) {
                        translationViewModel.setupGptAction(action)
                        translationViewModel.updateMessageWithContext(content)
                        translationViewModel.updateInputMessage(content)
                        showTranslationDialog(action.scope == GptActionScope.WholePage)
                    } else {
                        chatWithWeb(false, content, action)
                    }
                } else {
                    state.ebWebView.loadUrl(it)
                }
            }
        }
    }
}
