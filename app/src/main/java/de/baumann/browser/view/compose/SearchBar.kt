package de.baumann.browser.view.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.baumann.browser.Ninja.R

@Composable
fun ComposedSearchBar(
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

        val focusRequester = remember { FocusRequester() }
        SearchInput(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .clickable {
                    focusRequester.requestFocus()
                }
                .focusable(),
            text = text,
            onValueChanged = onTextChanged,
        )

        SearchBarIcon(iconResId = R.drawable.icon_close, onClick = { text.value = "" ; onCloseClick() })
        SearchBarIcon(iconResId = R.drawable.icon_arrow_down_gest, onClick = { onDownClick(text.value) })
        SearchBarIcon(iconResId = R.drawable.icon_arrow_up_gest, onClick = { onUpClick(text.value) })
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
            .width(46.dp)
            .clickable { onClick() }
            .padding(12.dp),
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@Preview
@Composable
fun PreviewSearchBar() {
    MyTheme {
        ComposedSearchBar(
            onTextChanged = {},
            onCloseClick = {},
            onUpClick = {},
            onDownClick = {}
        )
    }
}
