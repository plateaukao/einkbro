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
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.StartPageItem
import info.plateaukao.einkbro.unit.BookmarkRenderer
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Flow behind the "+" tile on the built-in start page: add an item picked from
 * the existing bookmarks or entered manually, or delete a current item.
 */
class StartPageItemDialog(private val ebWebView: EBWebView) : KoinComponent {
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val activity: Activity = ebWebView.context as Activity

    suspend fun show() {
        val options = mutableListOf(
            activity.getString(R.string.start_page_pick_bookmark),
            activity.getString(R.string.start_page_enter_manually),
        )
        if (config.startPageItems.isNotEmpty()) {
            options.add(activity.getString(R.string.menu_delete))
        }

        when (ListSettingWithNameDialog(activity, R.string.whitelist_add, options, -1).show()) {
            0 -> pickFromBookmarks()
            1 -> enterManually()
            2 -> removeItem()
        }
    }

    private suspend fun pickFromBookmarks() {
        val bookmarks = bookmarkManager.getAllBookmarksOnly()
        if (bookmarks.isEmpty()) return
        val index = ListSettingWithNameDialog(
            activity,
            R.string.start_page_pick_bookmark,
            bookmarks.map { it.title },
            -1
        ).show() ?: return
        val bookmark = bookmarks.getOrNull(index) ?: return
        config.addStartPageItem(StartPageItem(bookmark.title, bookmark.url))
        BookmarkRenderer.loadStartPage(ebWebView)
    }

    private fun enterManually() {
        val titleState = mutableStateOf("")
        val urlState = mutableStateOf("")
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

        DialogManager(activity).showOkCancelDialog(
            title = activity.getString(R.string.whitelist_add),
            view = composeView,
            okAction = {
                val rawUrl = urlState.value.trim()
                if (rawUrl.isEmpty()) return@showOkCancelDialog
                val url = if (rawUrl.contains("://")) rawUrl else "https://$rawUrl"
                val title = titleState.value.trim().ifEmpty {
                    runCatching { java.net.URI(url).host }.getOrNull() ?: url
                }
                config.addStartPageItem(StartPageItem(title, url))
                BookmarkRenderer.loadStartPage(ebWebView)
            },
        )
    }

    private suspend fun removeItem() {
        val items = config.startPageItems
        val index = ListSettingWithNameDialog(
            activity,
            R.string.menu_delete,
            items.map { it.title },
            -1
        ).show() ?: return
        val item = items.getOrNull(index) ?: return
        config.removeStartPageItem(item.url)
        BookmarkRenderer.loadStartPage(ebWebView)
    }
}
