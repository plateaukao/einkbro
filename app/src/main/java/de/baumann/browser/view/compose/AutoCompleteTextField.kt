package de.baumann.browser.view.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent.KEYCODE_ENTER
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.baumann.browser.Ninja.R
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.database.Record
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.dialog.compose.HorizontalSeparator

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

    var onDownClick: () -> Unit = {}
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
                onDownClick = onDownClick,
                onRecordClick = onRecordClick,
            )
        }
    }

    fun getFocus() {
        focusRequester.requestFocus()
        postDelayed({ ViewUnit.showKeyboard(context as Activity) }, 400)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
    onDownClick: () -> Unit,
    onRecordClick: (Record) -> Unit,
) {
    val requester = remember { focusRequester }

    Column(
        Modifier.background(MaterialTheme.colors.background),
        verticalArrangement = if (shouldReverse) Arrangement.Bottom else Arrangement.Top
    ) {
        if (shouldReverse) {
            BrowseHistoryList(
                modifier = Modifier.weight(1F, fill = false),
                records = recordList.value,
                filteredText = text.value.text,
                bookmarkManager = bookmarkManager,
                shouldReverse = shouldReverse,
                shouldShowTwoColumns = isWideLayout,
                onClick = onRecordClick,
                onLongClick = {}
            )
        }

        TextInputBar(requester, text, onTextSubmit, hasCopiedText, onPasteClick, onDownClick)

        if (!shouldReverse) {
            BrowseHistoryList(
                modifier = Modifier.weight(1F, fill = false),
                records = recordList.value,
                filteredText = text.value.text,
                bookmarkManager = bookmarkManager,
                shouldReverse = shouldReverse,
                shouldShowTwoColumns = isWideLayout,
                onClick = { onRecordClick(it); focusRequester.freeFocus() },
                onLongClick = {}
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
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

        if (hasCopiedText) {
            TextBarIcon(iconResId = R.drawable.ic_paste, onClick = onPasteClick)
        }

        TextBarIcon(
            iconResId = R.drawable.close_circle,
            onClick = { text.value = TextFieldValue("") })
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
    TextField(
        value = state.value,
        modifier = modifier
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
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colors.onBackground,
            backgroundColor = MaterialTheme.colors.background,
        ),
        placeholder = {
            Text(
                stringResource(R.string.main_omnibox_input_hint),
                color = MaterialTheme.colors.onBackground
            )
        },
        onValueChange = { state.value = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onValueSubmit(state.value.text) }),
    )
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
            onDownClick = {},
            focusRequester = FocusRequester(),
            recordList = mutableStateOf(listOf<Record>()),
            onRecordClick = {},
        )
    }
}
