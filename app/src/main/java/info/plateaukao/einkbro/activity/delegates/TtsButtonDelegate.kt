package info.plateaukao.einkbro.activity.delegates

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import kotlinx.coroutines.launch

class TtsButtonDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val ttsViewModel: TtsViewModel,
    private val composeToolbarViewControllerProvider: () -> ComposeToolbarViewController,
) {
    fun init() {
        activity.lifecycleScope.launch {
            ttsViewModel.readingState.collect { composeToolbarViewControllerProvider().updateIcons() }
        }
    }

    fun handleTtsButton() {
        if (ttsViewModel.isReading()) {
            TtsSettingDialogFragment().show(activity.supportFragmentManager, "TtsSettingDialog")
        } else {
            readArticle()
        }
    }

    fun showTtsLanguageDialog() {
        TtsLanguageDialog(activity).show(ttsViewModel.getAvailableLanguages())
    }

    fun readArticle() {
        activity.lifecycleScope.launch {
            ttsViewModel.readArticle(state.ebWebView.getRawText(), state.ebWebView.title.orEmpty())
        }
    }
}
