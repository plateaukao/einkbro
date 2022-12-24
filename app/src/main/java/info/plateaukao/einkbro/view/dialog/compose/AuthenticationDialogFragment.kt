package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme

class AuthenticationDialogFragment(
    val okAction: (String, String) -> Unit,
) : ComposeDialogFragment() {
    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            AuthenticationContent(
                onOk = { username, password ->
                    okAction(username, password)
                    dismiss()
                },
                onCancel = { dismiss() }
            )
        }
    }
}

@Composable
private fun AuthenticationContent(
    onOk: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.authentication),
            style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground),
            modifier = Modifier.padding(16.dp),
        )
        OutlinedTextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text(stringResource(R.string.user_name)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onBackground),
        )
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onBackground),
        )
        Row {
            TextButton(
                onClick = { onCancel() },
                modifier = Modifier.padding(8.dp),
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }
            TextButton(
                onClick = { onOk(username.value, password.value) },
                modifier = Modifier.padding(8.dp),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

@Preview
@Composable
private fun AuthenticationContentPreview() {
    MyTheme {
        AuthenticationContent({ _, _ -> }, {})
    }
}