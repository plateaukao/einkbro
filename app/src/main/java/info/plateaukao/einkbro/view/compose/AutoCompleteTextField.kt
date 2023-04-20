package info.plateaukao.einkbro.view.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent.KEYCODE_ENTER
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
        // try catch for IllegalStateException: FocusRequester is already active
        try {
            focusRequester.requestFocus()
            postDelayed({ ViewUnit.showKeyboard(context as Activity) }, 400)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
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
        if (list.isNotEmpty()) list else recordList.value
    }

    Column(
        Modifier
            .background(Color.Transparent)
            .clickable { closeAction() }
            .focusRequester(requester),
        verticalArrangement = if (shouldReverse) Arrangement.Bottom else Arrangement.Top
    ) {
        if (shouldReverse) {
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
                onLongClick = {}
            )
            HorizontalSeparator()
        }

        TextInputBar(requester, text, onTextSubmit, hasCopiedText, onPasteClick, closeAction)

        if (!shouldReverse) {
            HorizontalSeparator()
            BrowseHistoryList(
                modifier = Modifier
                    .weight(1F, fill = false)
                    .background(MaterialTheme.colors.background),
                records = filteredRecordList,
                bookmarkManager = bookmarkManager,
                shouldReverse = shouldReverse,
                shouldShowTwoColumns = isWideLayout,
                onClick = { onRecordClick(it); focusRequester.freeFocus() },
                onLongClick = {}
            )
            HorizontalSeparator()
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
    onDownClick: () -> Unit
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
    Box(modifier = modifier.padding(start = 5.dp)) {
        BasicTextField(
            value = state.value,
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
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
                    }
                },
            textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
            onValueChange = { state.value = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onValueSubmit(state.value.text) }),
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
        painter = painterResource(id = iconResId),
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
