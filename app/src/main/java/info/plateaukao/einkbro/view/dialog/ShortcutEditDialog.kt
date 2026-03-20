package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.compose.MyTheme

class ShortcutEditDialog(
    private val activity: Activity,
    private val title: String,
    private val url: String,
    private val bitmap: Bitmap?,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) {
    private val dialogManager: DialogManager = DialogManager(activity)

    private val titleState = mutableStateOf(title)
    private val urlState = mutableStateOf(url)

    fun show() {
        val composeView = ComposeView(activity).apply {
            setContent {
                MyTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp)
                    ) {
                        OutlinedTextField(
                            value = titleState.value,
                            onValueChange = { titleState.value = it },
                            label = { Text(stringResource(R.string.dialog_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        OutlinedTextField(
                            value = urlState.value,
                            onValueChange = { urlState.value = it },
                            label = { Text(stringResource(R.string.dialog_url_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                    }
                }
            }
        }

        dialogManager.showOkCancelDialog(
            title = activity.getString(R.string.menu_sc),
            view = composeView,
            okAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createShortcut()
                }
            },
            cancelAction = { cancelAction.invoke() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createShortcut() {
        HelperUnit.createShortcut(
            activity,
            titleState.value.trim(),
            urlState.value.trim(),
            bitmap
        )
        okAction.invoke()
    }
}
