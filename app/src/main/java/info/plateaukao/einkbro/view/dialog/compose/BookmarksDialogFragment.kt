package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Bitmap
import android.graphics.Point
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.NormalTextModifier
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
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

    private lateinit var bookmarksUpdateJob: Job

    private val bookmarks = mutableStateOf(emptyList<Bookmark>())

    private val shouldShowDragHandle = mutableStateOf(false)

    override fun setupComposeView() {
        bookmarksUpdateJob = lifecycleScope.launch {
            bookmarkViewModel.uiState.collect { bookmarks.value = it }
        }

        composeView.setContent {
            val context = LocalContext.current
            val showTwoColumn = ViewUnit.isWideLayout(context)

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
                            showTwoColumn = showTwoColumn,
                            shouldReverse = !config.isToolbarOnTop,
                            shouldShowDragHandle = shouldShowDragHandle.value,
                            onItemMoved = { from, to ->
                                bookmarks.value =
                                    if (showTwoColumn) moveItemInTwoColumns(bookmarks.value, from, to)
                                    else bookmarks.value.toMutableList().apply { add(to, removeAt(from)) }
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
                            onBookmarkLongClick = { bookmark, offSet -> showBookmarkContextMenu(bookmark, offSet) }
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

    private fun showBookmarkContextMenu(bookmark: Bookmark, point: Point) {
        BookmarkContextMenuDlgFragment(
            bookmark,
            anchorPoint = point
        ) {
            when (it) {
                ContextMenuItemType.NewTabForeground -> {
                    bookmarkIconClickAction(
                        bookmark.title,
                        bookmark.url,
                        true
                    )
                    dialog?.dismiss()
                }

                ContextMenuItemType.NewTabBackground -> {
                    bookmarkIconClickAction(
                        bookmark.title,
                        bookmark.url,
                        false
                    )
                    dialog?.dismiss()
                }

                ContextMenuItemType.SplitScreen -> {
                    splitScreenAction(bookmark.url)
                    dialog?.dismiss()
                }

                ContextMenuItemType.Edit -> BookmarkEditDialog(
                    requireActivity(),
                    bookmarkViewModel,
                    bookmark,
                    {
                        ViewUnit.hideKeyboard(requireActivity())
                        syncBookmarksAction(true)
                    },
                    { ViewUnit.hideKeyboard(requireActivity()) }
                ).show()

                ContextMenuItemType.Delete -> lifecycleScope.launch {
                    bookmarkViewModel.deleteBookmark(bookmark)
                    syncBookmarksAction(true)
                }

                else -> Unit
            }
        }.show(parentFragmentManager, "bookmark_context_menu")

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
                if (folder.id == 0) "" else folder.title,
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
    showTwoColumn: Boolean = false,
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
        columns = GridCells.Fixed(if (showTwoColumn) 2 else 1),
        reverseLayout = shouldReverse
    ) {
        itemsIndexed(bookmarks, key = { _, bookmark -> bookmark.id }) { _, bookmark ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // for getting long click point
            var longClickPosition = remember { mutableStateOf(Offset.Zero) }
            var boxPosition = remember { mutableStateOf(Offset.Zero) }


            ReorderableItem(reorderableLazyGridState, key = bookmark.id) { isDragging ->
                BookmarkItem(
                    bookmark = bookmark,
                    bitmap = bookmarkViewModel.getFavicon(bookmark),
                    isPressed = isPressed || isDragging,
                    shouldShowDragHandle = shouldShowDragHandle,
                    dragModifier = Modifier.draggableHandle(),
                    modifier = Modifier.then(
                        if (shouldShowDragHandle) {
                            Modifier
                                .longPressDraggableHandle()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) { onBookmarkClick(bookmark) }
                        } else {
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset -> onBookmarkClick(bookmark) },
                                        onLongPress = { offset ->
                                            longClickPosition.value = offset
                                            onBookmarkLongClick(bookmark, offset.toScreenPoint(boxPosition.value))
                                        }
                                    )
                                }
                                .onGloballyPositioned { boxPosition.value = it.positionOnScreen() }
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
            showTwoColumn = true,
            shouldReverse = true,
            shouldShowDragHandle = false,
            { _, _ -> },
            {},
            {},
            { _, _ -> }
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
                showTwoColumn = true,
                shouldReverse = true,
                shouldShowDragHandle = false,
                { _, _ -> },
                {},
                {},
                { _, _ -> }
            )
        }
    }
}

typealias OnBookmarkClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkLongClick = (bookmark: Bookmark, point: Point) -> Unit
typealias OnBookmarkIconClick = (bookmark: Bookmark) -> Unit

fun Offset.toScreenPoint(boxPosition: Offset): Point {
    return Point((x + boxPosition.x).toInt(), (y + boxPosition.y).toInt())
}

private fun <T> moveItemInTwoColumns(
    originalList: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    // Divide the original list into two lists: odd-positioned and even-positioned items
    val evenList = originalList.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
    val oddList = originalList.filterIndexed { index, _ -> index % 2 != 0 }.toMutableList()

    if (fromIndex < 0 || fromIndex >= originalList.size || toIndex < 0 || toIndex >= originalList.size) {
        return originalList
    }

    val fromItem = if (fromIndex.isEven()) {
        evenList.removeAt(fromIndex / 2)
    } else {
        oddList.removeAt(fromIndex / 2)
    }

    // move item to the target position
    if (toIndex.isEven()) {
        evenList.add(toIndex / 2, fromItem)
    } else {
        oddList.add(toIndex / 2, fromItem)
    }

    // Ensure both lists are balanced (list2 can have up to 2 more items than list1)
    while (oddList.size > evenList.size + 1) {
        evenList.add(oddList.removeLast())
    }

    while (evenList.size > oddList.size + 1) {
        oddList.add(evenList.removeLast())
    }

    // Merge the lists into one
    val resultList = mutableListOf<T>()
    val maxSize = maxOf(evenList.size, oddList.size)

    for (i in 0 until maxSize) {
        if (i < evenList.size) resultList.add(evenList[i])
        if (i < oddList.size) resultList.add(oddList[i])
    }

    return resultList
}

private fun Int.isEven(): Boolean = this % 2 == 0