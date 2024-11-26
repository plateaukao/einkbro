package info.plateaukao.einkbro.view.compose

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposedSearchBar(
    focusRequester: FocusRequester,
    onTextChanged:(String)->Unit,
    onCloseClick: ()->Unit,
    onUpClick: (String)->Unit,
    onDownClick: (String)->Unit,
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val text = remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        SearchInput(
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.hasFocus) keyboardController?.show()
                }
                .focusRequester(focusRequester),
            text = text,
            onValueChanged = onTextChanged,
        )

        SearchBarIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = { onDownClick(text.value) })
        SearchBarIcon(iconResId = R.drawable.icon_arrow_up_gest, onClick = { onUpClick(text.value) })
        SearchBarIcon(iconResId = R.drawable.icon_close, onClick = { text.value = "" ; onCloseClick() })
    }
}

@Composable
fun SearchInput(
    modifier: Modifier,
    text: MutableState<String>,
    onValueChanged: (String)->Unit,
) {
    TextField(
        value = text.value,
        modifier = modifier.padding(horizontal = 5.dp),
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colors.onBackground,
            backgroundColor = MaterialTheme.colors.background,
        ),
        placeholder = {
            Text(stringResource(R.string.search_hint), color = MaterialTheme.colors.onBackground)
        },
        onValueChange = {
            text.value = it
            onValueChanged(it)
        },
    )
}

@Composable
fun SearchBarIcon(
    iconResId: Int,
    onClick: ()->Unit,
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

class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
): AbstractComposeView(context, attrs, defStyle) {

    var onTextChanged: (String)->Unit = {}
    var onCloseClick: ()->Unit = {}
    var onUpClick: (String)->Unit = {}
    var onDownClick: (String)->Unit = {}
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
fun PreviewSearchBar() {
    MyTheme {
        ComposedSearchBar(
            onTextChanged = {},
            onCloseClick = {},
            onUpClick = {},
            onDownClick = {},
            focusRequester = FocusRequester(),
        )
    }
}
