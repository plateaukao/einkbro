package info.plateaukao.einkbro.view.dialog.compose

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.view.compose.MyTheme

class TranslationConfigDlgFragment(
    private val url: String,
    private val onToggledAction: (Boolean) -> Unit,
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            val translationMode = remember { mutableStateOf(config.translationMode) }
            val site = Uri.parse(url).host ?: "Unknown"
            MyTheme {
                TranslationConfigScreen(
                    site = site,
                    translationMode = translationMode.value,
                    shouldTranslateThisSite = config.shouldTranslateSite(url),
                    toggleTranslateThisSite = {
                        onToggledAction(config.toggleTranslateSite(url))
                        dismiss()
                    },
                    translationModeChanged = {
                        config.translationMode = it
                        translationMode.value = it
                        if (config.shouldTranslateSite(url)) {
                            onToggledAction(true)
                            dismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TranslationConfigScreen(
    site: String,
    translationMode: TranslationMode = TranslationMode.GOOGLE_IN_PLACE,
    shouldTranslateThisSite: Boolean = false,
    toggleTranslateThisSite: () -> Unit = {},
    translationModeChanged: (TranslationMode) -> Unit = {},
) {
    val context = LocalContext.current
    var actionExpanded by remember { mutableStateOf(false) }

    val textLabel = "Mode: ${context.getString(translationMode.labelResId)}"
    Column {
        Text(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .align(
                    alignment = Alignment.CenterHorizontally
                ),
            text = site,
            color = MaterialTheme.colors.onBackground
        )
        ToggleItem(
            state = shouldTranslateThisSite,
            titleResId = R.string.translate_this_site,
        ) {
            toggleTranslateThisSite()
        }
        TextButton(onClick = { actionExpanded = true }) {
            Text(
                modifier = Modifier.padding(10.dp),
                text = textLabel,
                color = MaterialTheme.colors.onBackground
            )
        }
        DropdownMenu(
            modifier = Modifier.padding(8.dp),
            expanded = actionExpanded,
            onDismissRequest = { actionExpanded = false }
        ) {
            TranslationMode.entries//.toMutableList().apply { remove(TranslationMode.DEEPL_BY_PARAGRAPH) }
                .forEach { type ->
                    val text = context.getString(type.labelResId)
                    DropdownMenuItem(onClick = {
                        translationModeChanged(type)
                        actionExpanded = false
                    }) {
                        Text(text = text)
                    }
                }
        }
    }
}