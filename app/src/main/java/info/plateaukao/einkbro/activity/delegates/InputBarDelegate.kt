package info.plateaukao.einkbro.activity.delegates

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Rect
import android.os.Build
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.ToolbarPosition
import info.plateaukao.einkbro.search.suggestion.SearchSuggestionViewModel
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.MainActivityLayout
import info.plateaukao.einkbro.view.compose.AutoCompleteTextField
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.viewControllers.ComposeToolbarViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InputBarDelegate(
    private val activity: FragmentActivity,
    private val config: ConfigManager,
    private val state: BrowserState,
    private val bookmarkManager: BookmarkManager,
    private val searchSuggestionViewModel: SearchSuggestionViewModel,
    private val updateAlbum: (url: String) -> Unit,
    private val showToolbar: () -> Unit,
    private val toggleFullscreen: () -> Unit,
) {
    private var searchJob: Job? = null
    var shouldRestoreFullscreen = false

    val inputTextOrUrl = mutableStateOf(TextFieldValue(""))
    val inputRecordList = mutableStateOf(listOf<Record>())
    val inputUrlFocusRequester = FocusRequester()
    var inputIsWideLayout by mutableStateOf(false)
    var inputShouldReverse by mutableStateOf(true)
    var inputHasCopiedText by mutableStateOf(false)

    fun initInputBar() {
        state.binding.inputUrl.apply {
            visibility = INVISIBLE
            setContent {
                MyTheme {
                    AutoCompleteTextField(
                        text = inputTextOrUrl,
                        recordList = inputRecordList,
                        bookmarkManager = bookmarkManager,
                        focusRequester = inputUrlFocusRequester,
                        isWideLayout = inputIsWideLayout,
                        shouldReverse = inputShouldReverse,
                        hasCopiedText = inputHasCopiedText,
                        onTextSubmit = { text ->
                            updateAlbum(text.trim())
                            showToolbar()
                            if (shouldRestoreFullscreen) {
                                toggleFullscreen()
                                shouldRestoreFullscreen = false
                            }
                        },
                        onTextChange = { query ->
                            searchJob?.cancel()
                            searchJob = activity.lifecycleScope.launch {
                                kotlinx.coroutines.delay(300)
                                withContext(Dispatchers.IO) {
                                    searchSuggestionViewModel.updateSuggestions(query)
                                }
                                inputRecordList.value = searchSuggestionViewModel.suggestions.value
                            }
                        },
                        onPasteClick = { updateAlbum(getClipboardText()); showToolbar() },
                        closeAction = {
                            showToolbar()
                            if (shouldRestoreFullscreen) {
                                toggleFullscreen()
                                shouldRestoreFullscreen = false
                            }
                        },
                        onRecordClick = {
                            updateAlbum(it.url)
                            showToolbar()
                            if (shouldRestoreFullscreen) {
                                toggleFullscreen()
                                shouldRestoreFullscreen = false
                            }
                        },
                    )
                }
            }
        }
    }

    fun focusOnInput() {
        val binding = state.binding
        if (binding.appBar.visibility != VISIBLE) {
            shouldRestoreFullscreen = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.setDecorFitsSystemWindows(true)
            } else {
                @Suppress("DEPRECATION")
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        val ebWebView = state.ebWebView
        val textOrUrl = if (ebWebView.url?.startsWith("data:") != true) {
            val url = ebWebView.url.orEmpty()
            TextFieldValue(url, selection = TextRange(0, url.length))
        } else {
            TextFieldValue("")
        }

        inputTextOrUrl.value = textOrUrl
        inputIsWideLayout = ViewUnit.isWideLayout(activity)
        inputShouldReverse = !config.ui.isToolbarOnTop
        inputHasCopiedText = getClipboardText().isNotEmpty()
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                searchSuggestionViewModel.initSuggestions()
                inputRecordList.value = searchSuggestionViewModel.suggestions.value
            }
        }

        if (config.ui.isVerticalToolbar) {
            adjustInputUrlForVerticalToolbar()
        } else {
            state.composeToolbarViewController.hide()
            binding.appBar.visibility = INVISIBLE
        }
        binding.contentSeparator.visibility = INVISIBLE
        binding.inputUrl.visibility = VISIBLE
        binding.inputUrl.postDelayed(
            {
                ViewUnit.showKeyboard(activity)
                try {
                    inputUrlFocusRequester.requestFocus()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }, 200
        )
    }

    private fun adjustInputUrlForVerticalToolbar() {
        val binding = state.binding
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet().apply {
            clone(binding.root)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.TOP, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP)
            connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            if (config.ui.toolbarPosition == ToolbarPosition.Left) {
                connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.START, binding.appBar.id, androidx.constraintlayout.widget.ConstraintSet.END)
                connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END)
            } else {
                connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START)
                connect(binding.inputUrl.id, androidx.constraintlayout.widget.ConstraintSet.END, binding.appBar.id, androidx.constraintlayout.widget.ConstraintSet.START)
            }
        }
        constraintSet.applyTo(binding.root)
    }

    fun getClipboardText(): String =
        (activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

    fun isKeyboardDisplaying(): Boolean {
        val binding = state.binding
        val rect = Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val heightDiff: Int = binding.root.rootView.height - rect.bottom
        return heightDiff > binding.root.rootView.height * 0.15
    }
}
