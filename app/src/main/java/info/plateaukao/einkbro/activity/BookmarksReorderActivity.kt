package info.plateaukao.einkbro.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.compose.BookmarkItem
import info.plateaukao.einkbro.view.dialog.compose.OnBookmarkClick
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import info.plateaukao.einkbro.viewmodel.BookmarkViewModelFactory
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class BookmarksReorderActivity : ComponentActivity() {
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()
    private val bookmarkViewModel: BookmarkViewModel by viewModels {
        BookmarkViewModelFactory(bookmarkManager)
    }

    private val bookmarks = mutableStateOf(emptyList<Bookmark>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            bookmarkViewModel.uiState.collect { bookmarks.value = it }
        }

        val folderId = getFolderIdFromIntent()
        setContent {
            MyTheme {
                Scaffold(
                    topBar = {
                        val currentFolder = bookmarkViewModel.currentFolder.value
                        TopAppBar(
                            title = {
                                Text(
                                    if (currentFolder.id == 0) {
                                        stringResource(R.string.bookmarks)
                                    } else
                                        bookmarkViewModel.currentFolder.value.title
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (bookmarkViewModel.currentFolder.value.id == 0) {
                                        finish()
                                    } else {
                                        bookmarkViewModel.outOfFolder()
                                    }
                                }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                        )
                    }
                ) { _ ->
                    BookmarksReorderContent(
                        bookmarks = bookmarks,
                        reverse = !config.isToolbarOnTop,
                        getFavicon = { bookmarkManager.findFaviconBy(it)?.getBitmap() },
                        onBookmarkClick = { bookmark ->
                            if (bookmark.isDirectory) {
                                bookmarkViewModel.intoFolder(bookmark)
                            }
                        }
                    ) { from, to ->
                        bookmarks.value =
                            bookmarks.value.toMutableList().apply { add(to, removeAt(from)) }
                        bookmarkViewModel.updateBookmarksOrder(bookmarks.value)
                    }
                }
            }

        }
    }

    // get folder id from intent
    private fun getFolderIdFromIntent(): Int = intent.getIntExtra(EXTRA_FOLDER_ID, 0)

    companion object {
        const val EXTRA_FOLDER_ID = "folderId"
        fun start(activity: Activity, folderId: Int) {
            val intent = Intent(activity, BookmarksReorderActivity::class.java)
            intent.putExtra(EXTRA_FOLDER_ID, folderId)
            activity.startActivity(intent)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarksReorderContent(
    bookmarks: MutableState<List<Bookmark>>,
    reverse: Boolean = false,
    getFavicon: (String) -> Bitmap?,
    onBookmarkClick: OnBookmarkClick,
    onItemMoved: (from: Int, to: Int) -> Unit = { _, _ -> },
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onItemMoved(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        reverseLayout = reverse,
    ) {
        items(bookmarks.value, { it.id }) { bookmark ->
            ReorderableItem(
                reorderableLazyListState,
                key = bookmark.id,
            ) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 30.dp else 0.dp, label = "")

                Row(
                    modifier = Modifier.clickable {
                        onBookmarkClick(bookmark)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BookmarkItem(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { onBookmarkClick(bookmark) },
                        bookmark = bookmark,
                        bitmap = getFavicon(bookmark.url),
                        iconClick = { },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        modifier = Modifier
                            .padding(8.dp)
                            .draggableHandle(),
                        painter = painterResource(id = R.drawable.ic_drag),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
        }
    }
}
