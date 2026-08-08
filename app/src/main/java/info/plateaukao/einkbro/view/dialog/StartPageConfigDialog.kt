package info.plateaukao.einkbro.view.dialog

import android.app.Activity
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BookmarkRenderer
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Flow behind tapping the wordmark on the built-in start page: rename the
 * heading or set/remove a background image picked from the device.
 */
class StartPageConfigDialog(private val ebWebView: EBWebView) : KoinComponent {
    private val config: ConfigManager by inject()
    private val activity: Activity = ebWebView.context as Activity

    suspend fun show() {
        val options = mutableListOf(
            activity.getString(R.string.start_page_edit_title),
            activity.getString(R.string.start_page_set_background),
        )
        if (BookmarkRenderer.startPageBackgroundFile(activity).exists()) {
            options.add(activity.getString(R.string.start_page_clear_background))
        }

        when (activity.showPlainListDialog(null, options)) {
            0 -> editTitle()
            // picker result lands in BrowserActivity, which reloads the page
            1 -> ebWebView.webViewCallback?.chooseStartPageBackground()
            2 -> clearBackground()
        }
    }

    private fun editTitle() {
        val titleState = mutableStateOf(
            config.startPageTitle.ifBlank { activity.getString(R.string.app_name) }
        )
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
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
                                // the default cursor color is primary, which is
                                // black on the dark theme's black surface
                                cursorColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                    }
                }
            }
        }

        DialogManager(activity).showOkCancelDialog(
            title = activity.getString(R.string.start_page_edit_title),
            view = composeView,
            okAction = {
                val title = titleState.value.trim()
                // typing the default back means "no customization"
                config.startPageTitle =
                    if (title == activity.getString(R.string.app_name)) "" else title
                BookmarkRenderer.loadStartPage(ebWebView)
            },
        ).allowImeForComposeContent()
    }

    private fun clearBackground() {
        BookmarkRenderer.startPageBackgroundFile(activity).delete()
        BookmarkRenderer.loadStartPage(ebWebView)
    }
}
