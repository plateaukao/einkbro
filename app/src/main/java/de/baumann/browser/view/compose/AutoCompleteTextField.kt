package de.baumann.browser.view.compose

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent.KEYCODE_ENTER
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import de.baumann.browser.unit.ViewUnit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AutoCompleteTextField(
    focusRequester: FocusRequester,
    text: MutableState<TextFieldValue>,
    hasCopiedText: Boolean = false,
    onTextSubmit:(String)->Unit,
    onPasteClick: ()->Unit,
    onDownClick: ()->Unit,
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        TextInput(
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.hasFocus) keyboardController?.show()
                }
                .focusRequester(focusRequester),
            state = text,
            onValueSubmit = onTextSubmit,
        )

        if (hasCopiedText) {
            TextBarIcon(iconResId = R.drawable.ic_paste, onClick = onPasteClick)
        }
        TextBarIcon(iconResId = R.drawable.close_circle, onClick = { text.value = TextFieldValue("") })
        TextBarIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = onDownClick)
    }
}

@Composable
fun TextInput(
    modifier: Modifier,
    state: MutableState<TextFieldValue>,
    onValueSubmit: (String)->Unit,
) {
    TextField(
        value = state.value,
        modifier = modifier.padding(horizontal = 5.dp).onKeyEvent {
            if (it.nativeKeyEvent.keyCode == KEYCODE_ENTER) {
                onValueSubmit(state.value.text)
                true
            }
            false
        }.onFocusChanged { focusState ->
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
            Text(stringResource(R.string.main_omnibox_input_hint), color = MaterialTheme.colors.onBackground)
        },
        onValueChange = { state.value = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { onValueSubmit(state.value.text) },
        ),

    )
}

@Composable
fun TextBarIcon(
    iconResId: Int,
    onClick: ()->Unit,
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

class TextBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
): AbstractComposeView(context, attrs, defStyle) {

    var onTextChanged by mutableStateOf<(String)->Unit>({})
    var onCloseClick by mutableStateOf({})
    var onUpClick by mutableStateOf<(String)->Unit>({})
    var onDownClick by mutableStateOf<(String)->Unit>({})
    var focusRequester by mutableStateOf(FocusRequester())

    @Composable
    override fun Content() {
        MyTheme {
            ComposedSearchBar(
                focusRequester = focusRequester,
                onTextChanged = onTextChanged,
                onCloseClick = onCloseClick,
                onUpClick = onUpClick,
                onDownClick = onDownClick,
            )
        }
    }

    fun getFocus() {
        focusRequester.requestFocus()
        postDelayed({ ViewUnit.showKeyboard(context as Activity) }, 400)
    }
}

@Preview
@Composable
fun PreviewTextBar() {
    MyTheme {
        AutoCompleteTextField(
            text= mutableStateOf(TextFieldValue("")),
            onTextSubmit = {},
            onPasteClick = {},
            onDownClick = {},
            focusRequester = FocusRequester(),
        )
    }
}
