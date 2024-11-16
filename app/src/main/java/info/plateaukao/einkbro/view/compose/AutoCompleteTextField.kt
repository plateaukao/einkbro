package info.plateaukao.einkbro.view.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent.KEYCODE_ENTER
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.dialog.compose.HorizontalSeparator
import kotlinx.coroutines.launch

class AutoCompleteTextComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    var inputTextOrUrl = mutableStateOf(TextFieldValue(""))
    var recordList = mutableStateOf(listOf<Record>())
    var focusRequester by mutableStateOf(FocusRequester())
    var bookmarkManager: BookmarkManager? = null
    var isWideLayout by mutableStateOf(false)
    var shouldReverse by mutableStateOf(true)
    var hasCopiedText by mutableStateOf(false)

    var closeAction: () -> Unit = {}
    var onTextSubmit: (String) -> Unit = {}
    var onPasteClick: () -> Unit = {}
    var onRecordClick: (Record) -> Unit = {}

    @Composable
    override fun Content() {
        MyTheme {
            AutoCompleteTextField(
                text = inputTextOrUrl,
                recordList = recordList,
                bookmarkManager = bookmarkManager,
                focusRequester = focusRequester,
                isWideLayout = isWideLayout,
                shouldReverse = shouldReverse,
                hasCopiedText = hasCopiedText,
                onTextSubmit = onTextSubmit,
                onPasteClick = onPasteClick,
                closeAction = closeAction,
                onRecordClick = onRecordClick,
            )
        }
    }

    fun getFocus() {
        postDelayed(
            {
                ViewUnit.showKeyboard(context as Activity)
                try {
                    // need to call it after some delay to make sure the view is ready
                    focusRequester.requestFocus()
                } catch (e: IllegalStateException) {
                    // try catch for IllegalStateException: FocusRequester is already active
                    e.printStackTrace()
                }
            }, 200
        )
    }
}

@Composable
fun AutoCompleteTextField(
    focusRequester: FocusRequester,
    bookmarkManager: BookmarkManager? = null,
    shouldReverse: Boolean = false,
    isWideLayout: Boolean = false,
    text: MutableState<TextFieldValue>,
    recordList: MutableState<List<Record>>,
    hasCopiedText: Boolean = false,
    onTextSubmit: (String) -> Unit,
    onPasteClick: () -> Unit,
    closeAction: () -> Unit,
    onRecordClick: (Record) -> Unit,
) {
    val requester = remember { focusRequester }
    val filteredRecordList = if (text.value.text.isEmpty()) {
        recordList.value
    } else {
        val list = recordList.value.filter {
            it.title?.contains(text.value.text, ignoreCase = true) == true
                    || it.url.contains(text.value.text, ignoreCase = true)
        }
        list.ifEmpty { recordList.value }
    }

    Column(
        Modifier
            .background(Color.Transparent)
            .clickable { closeAction() },
        verticalArrangement = if (shouldReverse) Arrangement.Bottom else Arrangement.Top
    ) {
        if (!shouldReverse) {
            TextInputBar(requester, text, onTextSubmit, hasCopiedText, onPasteClick, closeAction)
        }

        HorizontalSeparator()
        BrowseHistoryList(
            modifier = Modifier
                .weight(1F, fill = false)
                .background(MaterialTheme.colors.background),
            records = filteredRecordList,
            bookmarkManager = bookmarkManager,
            shouldReverse = shouldReverse,
            shouldShowTwoColumns = isWideLayout,
            onClick = onRecordClick,
            onLongClick = { _, _ -> }
        )
        HorizontalSeparator()

        if (shouldReverse) {
            TextInputBar(requester, text, onTextSubmit, hasCopiedText, onPasteClick, closeAction)
        }
    }
}

@Composable
private fun TextInputBar(
    focusRequester: FocusRequester,
    text: MutableState<TextFieldValue>,
    onTextSubmit: (String) -> Unit,
    hasCopiedText: Boolean,
    onPasteClick: () -> Unit,
    onDownClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        TextInput(
            modifier = Modifier.weight(1F),
            focusRequester,
            state = text,
            onValueSubmit = onTextSubmit,
        )

        TextBarIcon(
            iconResId = R.drawable.icon_close,
            onClick = { text.value = TextFieldValue("") })
        if (hasCopiedText) {
            TextBarIcon(iconResId = R.drawable.ic_paste, onClick = onPasteClick)
        }
        TextBarIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = onDownClick)
    }
}

@Composable
fun TextInput(
    modifier: Modifier,
    focusRequester: FocusRequester,
    state: MutableState<TextFieldValue>,
    onValueSubmit: (String) -> Unit,
) {
    val scrollState = remember { androidx.compose.foundation.ScrollState(0) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.padding(start = 5.dp)) {
        BasicTextField(
            value = state.value,
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .horizontalScroll(scrollState)
                .onKeyEvent {
                    if (it.nativeKeyEvent.keyCode == KEYCODE_ENTER) {
                        onValueSubmit(state.value.text)
                        true
                    }
                    false
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        val text = state.value.text
                        state.value = state.value.copy(
                            selection = TextRange(0, text.length)
                        )
                        coroutineScope.launch {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }
                },
            textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
            onValueChange = { state.value = it },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                autoCorrect = false,
            ),
            keyboardActions = KeyboardActions(onSearch = { onValueSubmit(state.value.text) }),
        )
        if (state.value.text.isEmpty()) {
            Text(
                stringResource(R.string.main_omnibox_input_hint),
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Composable
fun TextBarIcon(
    iconResId: Int,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
            .clickable { onClick() }
            .padding(8.dp),
        imageVector = ImageVector.vectorResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
fun PreviewTextBar() {
    MyTheme {
        AutoCompleteTextField(
            text = mutableStateOf(TextFieldValue("")),
            onTextSubmit = {},
            onPasteClick = {},
            closeAction = {},
            focusRequester = FocusRequester(),
            recordList = mutableStateOf(listOf()),
            onRecordClick = {},
        )
    }
}
