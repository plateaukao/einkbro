package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.databinding.DialogMenuContextListBinding
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.NormalTextModifier
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.dismissWithAction
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

class BookmarksDialogFragment(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val bookmarkViewModel: BookmarkViewModel,
    private val gotoUrlAction: (String) -> Unit,
    private val bookmarkIconClickAction: (String, String, Boolean) -> Unit,
    private val splitScreenAction: (String) -> Unit,
    private val syncBookmarksAction: (Boolean) -> Unit,
    private val linkBookmarksAction: () -> Unit,
) : ComposeDialogFragment(), KoinComponent {
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    private lateinit var bookmarksUpdateJob: Job

    private val bookmarks = mutableStateOf(emptyList<Bookmark>())

    private val shouldShowDragHandle = mutableStateOf(false)

    override fun setupComposeView() {
        bookmarksUpdateJob = lifecycleScope.launch {
            bookmarkViewModel.uiState.collect { bookmarks.value = it }
        }

        composeView.setContent {
            val context = LocalContext.current

            MyTheme {
                DialogPanel(
                    folder = bookmarkViewModel.currentFolder.value,
                    inSortMode = shouldShowDragHandle.value,
                    upParentAction = { bookmarkViewModel.outOfFolder() },
                    syncBookmarksAction = syncBookmarksAction,
                    linkBookmarksAction = linkBookmarksAction,
                    reorderBookmarkAction = {
                        shouldShowDragHandle.value = !shouldShowDragHandle.value
                        if (shouldShowDragHandle.value) {
                            EBToast.show(context, getString(R.string.drag_to_reorder))
                        }
                    },
                    closeAction = { dialog?.dismiss() }) {
                    if (bookmarks.value.isEmpty()) {
                        Text(
                            modifier = NormalTextModifier,
                            text = getString(R.string.no_bookmarks),
                            color = MaterialTheme.colors.onBackground
                        )
                    } else {
                        BookmarkList(
                            bookmarks = bookmarks.value,
                            bookmarkViewModel = bookmarkViewModel,
                            isWideLayout = ViewUnit.isWideLayout(requireContext()),
                            shouldReverse = !config.isToolbarOnTop,
                            shouldShowDragHandle = shouldShowDragHandle.value,
                            onItemMoved = { from, to ->
                                bookmarks.value =
                                    bookmarks.value.toMutableList()
                                        .apply { add(to, removeAt(from)) }
                                bookmarkViewModel.updateBookmarksOrder(bookmarks.value)
                            },
                            onBookmarkClick = {
                                if (!it.isDirectory) {
                                    gotoUrlAction(it.url)
                                    config.addRecentBookmark(it)
                                    dialog?.dismiss()
                                } else {
                                    bookmarkViewModel.intoFolder(it)
                                }
                            },
                            onBookmarkIconClick = {
                                if (!it.isDirectory) bookmarkIconClickAction(
                                    it.title,
                                    it.url,
                                    true
                                ); dialog?.dismiss()
                            },
                            onBookmarkLongClick = { showBookmarkContextMenu(it) }
                        )
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        bookmarksUpdateJob.cancel()
        bookmarkViewModel.toRootFolder()
        super.onDestroy()
    }

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(requireContext()))
        val optionDialog = dialogManager.showOptionDialog(dialogView.root)

        if (bookmark.isDirectory) {
            dialogView.menuContextListNewTab.visibility = View.GONE
            dialogView.menuContextListNewTabOpen.visibility = View.GONE
            dialogView.menuContextListSplitScreen.visibility = View.GONE
        }

        dialogView.menuContextListSplitScreen.setOnClickListener {
            optionDialog.dismissWithAction { splitScreenAction(bookmark.url); dialog?.dismiss() }
        }

        dialogView.menuTitle.text = bookmark.title
        dialogView.menuContextListEdit.visibility = View.VISIBLE
        dialogView.menuContextListNewTab.setOnClickListener {
            optionDialog.dismissWithAction {
                bookmarkIconClickAction(getString(R.string.app_name), bookmark.url, false)
                dialog?.dismiss()
            }
        }
        dialogView.menuContextListNewTabOpen.setOnClickListener {
            optionDialog.dismissWithAction {
                bookmarkIconClickAction(
                    bookmark.title,
                    bookmark.url,
                    true
                )
                dialog?.dismiss()
            }
        }
        dialogView.menuContextListDelete.setOnClickListener {
            lifecycleScope.launch {
                bookmarkViewModel.deleteBookmark(bookmark)
                syncBookmarksAction(true)
            }
            optionDialog.dismiss()
        }

        dialogView.menuContextListEdit.setOnClickListener {
            BookmarkEditDialog(
                requireActivity(),
                bookmarkViewModel,
                bookmark,
                {
                    ViewUnit.hideKeyboard(requireActivity())
                    syncBookmarksAction(true)
                },
                { ViewUnit.hideKeyboard(requireActivity()) }
            ).show()
            optionDialog.dismiss()
        }
    }
}

@Composable
fun DialogPanel(
    folder: Bookmark,
    inSortMode: Boolean = false,
    upParentAction: (Bookmark) -> Unit,
    syncBookmarksAction: (Boolean) -> Unit,
    linkBookmarksAction: () -> Unit,
    closeAction: () -> Unit,
    reorderBookmarkAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.weight(1F, fill = false)) {
            content()
        }
        Divider(thickness = 1.dp, color = MaterialTheme.colors.onBackground)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (folder.id != 0) {
                ActionIcon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    iconResId = R.drawable.icon_arrow_left_gest,
                    action = { upParentAction(folder) }
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Text(
                if (folder.id == 0) stringResource(id = R.string.bookmarks) else folder.title,
                Modifier
                    .weight(1F)
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { if (folder.id != 0) upParentAction(folder) },
                color = MaterialTheme.colors.onBackground
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = if (inSortMode) R.drawable.icon_list else R.drawable.ic_sort,
                action = { reorderBookmarkAction() },
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.ic_sync,
                action = { syncBookmarksAction(false) },
                longClickAction = { linkBookmarksAction() }
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.icon_arrow_down_gest,
                action = closeAction
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkList(
    bookmarks: List<Bookmark>,
    bookmarkViewModel: BookmarkViewModel,
    isWideLayout: Boolean = false,
    shouldReverse: Boolean = true,
    shouldShowDragHandle: Boolean = false,
    onItemMoved: (from: Int, to: Int) -> Unit,
    onBookmarkClick: OnBookmarkClick,
    onBookmarkIconClick: OnBookmarkIconClick,
    onBookmarkLongClick: OnBookmarkLongClick,
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onItemMoved(from.index, to.index)
    }

    LazyVerticalGrid(
        modifier = Modifier.wrapContentHeight(),
        state = lazyGridState,
        columns = GridCells.Fixed(if (isWideLayout) 2 else 1),
        reverseLayout = shouldReverse
    ) {
        itemsIndexed(bookmarks, key = { _, bookmark -> bookmark.id }) { _, bookmark ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            ReorderableItem(reorderableLazyGridState, key = bookmark.id) { isDragging ->
                BookmarkItem(
                    bookmark = bookmark,
                    bitmap = bookmarkViewModel.getFavicon(bookmark),
                    isPressed = isPressed || isDragging,
                    shouldShowDragHandle = shouldShowDragHandle,
                    dragModifier = Modifier.draggableHandle(),
                    modifier = Modifier
                        .then(
                            if (shouldShowDragHandle) {
                                Modifier
                                    .longPressDraggableHandle()
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                    ) { onBookmarkClick(bookmark) }
                            } else {
                                Modifier.combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onBookmarkClick(bookmark) },
                                    onLongClick = { onBookmarkLongClick(bookmark) },
                                )
                            }
                        ),
                    iconClick = {
                        if (!bookmark.isDirectory) onBookmarkIconClick(bookmark)
                        else onBookmarkClick(bookmark)
                    }
                )
            }
        }
    }
}

@Composable
fun BookmarkItem(
    modifier: Modifier,
    bitmap: Bitmap? = null,
    isPressed: Boolean = false,
    shouldShowDragHandle: Boolean = false,
    bookmark: Bookmark,
    dragModifier: Modifier = Modifier,
    iconClick: () -> Unit,
) {
    val borderWidth = if (isPressed) 1.dp else (-1).dp

    Row(
        modifier = modifier
            .height(54.dp)
            .padding(4.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (shouldShowDragHandle) {
            Icon(
                modifier = dragModifier.padding(8.dp),
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        }
        if (bitmap != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
                    .padding(end = 5.dp)
                    .clickable { iconClick() },
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            ActionIcon(
                modifier = Modifier.align(Alignment.CenterVertically),
                iconResId = if (bookmark.isDirectory) R.drawable.ic_folder else R.drawable.icon_plus,
                action = iconClick
            )
        }
        Text(
            modifier = Modifier
                .weight(1.0f)
                .align(Alignment.CenterVertically),
            text = bookmark.title,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionIcon(
    modifier: Modifier,
    iconResId: Int,
    action: (() -> Unit)? = null,
    longClickAction: (() -> Unit)? = null,
) {
    Icon(
        modifier = modifier
            .size(36.dp)
            .padding(end = 5.dp)
            .combinedClickable(
                onClick = { action?.invoke() },
                onLongClick = { longClickAction?.invoke() },
            ),
        imageVector = ImageVector.vectorResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@Preview
@Composable
private fun PreviewBookmarkList() {
    MyTheme {
        BookmarkList(
            bookmarks = listOf(Bookmark("test 1", "https://www.google.com", false)),
            BookmarkViewModel(bookmarkManager = BookmarkManager(LocalContext.current)),
            isWideLayout = true,
            shouldReverse = true,
            shouldShowDragHandle = false,
            { _, _ -> },
            {},
            {},
            {}
        )
    }
}

// preview dialog panel
@Preview
@Composable
private fun PreviewDialogPanel() {
    MyTheme {
        DialogPanel(
            folder = Bookmark("test 1", "https://www.google.com", false),
            //{},
            false,
            {},
            {},
            {},
            {},
            {},
        ) {
            BookmarkList(
                bookmarks = listOf(Bookmark("test 1", "https://www.google.com", false)),
                BookmarkViewModel(bookmarkManager = BookmarkManager(LocalContext.current)),
                isWideLayout = true,
                shouldReverse = true,
                shouldShowDragHandle = false,
                { _, _ -> },
                {},
                {},
                {}
            )
        }
    }
}

typealias OnBookmarkClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkLongClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkIconClick = (bookmark: Bookmark) -> Unit
