package de.baumann.browser.view.dialog.compose

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuContextListBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.dialog.BookmarkEditDialog
import de.baumann.browser.view.dialog.DialogManager
import de.baumann.browser.view.dialog.dismissWithAction
import de.baumann.browser.viewmodel.BookmarkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class BookmarksDialogFragment(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val bookmarkViewModel: BookmarkViewModel,
    private val gotoUrlAction: (String) -> Unit,
    private val addTabAction: (String, String, Boolean) -> Unit,
    private val splitScreenAction: (String) -> Unit,
): ComposeDialogFragment(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    private lateinit var composeView: ComposeView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setupDialog()

        composeView = ComposeView(requireContext())
        folderStack.push(Bookmark(getString(R.string.bookmarks), ""))
        updateBookmarksContent()

        return composeView
    }

    override fun onDestroy() {
        updateContentJob?.cancel()
        super.onDestroy()
    }

    private var updateContentJob: Job? = null
    private val folderStack: Stack<Bookmark> = Stack()
    private fun updateBookmarksContent() {
        val currentFolder = folderStack.peek()
        updateContentJob?.cancel()
        updateContentJob = lifecycleScope.launch {
            bookmarkViewModel.bookmarksByParent(currentFolder.id).collect { bookmarks ->
                composeView.apply {
                    setContent {
                        AppCompatTheme {
                            DialogPanel(
                                folder = currentFolder,
                                upParentAction = { gotoParentFolder() },
                                createFolderAction = { createBookmarkFolder(it) },
                                closeAction = { dialog?.dismiss() }) {
                                BookmarkList(
                                    bookmarks = bookmarks,
                                    bookmarkManager = bookmarkManager,
                                    isWideLayout = isWideLayout(),
                                    shouldReverse = !config.isToolbarOnTop,
                                    onBookmarkClick = {
                                        if (!it.isDirectory) {
                                            gotoUrlAction(it.url)
                                            config.addRecentBookmark(it)
                                            dialog?.dismiss()
                                        } else {
                                            folderStack.push(it)
                                            updateBookmarksContent()
                                        }
                                    },
                                    onBookmarkIconClick = { if (!it.isDirectory) addTabAction(it.title, it.url, true) ; dialog?.dismiss() },
                                    onBookmarkLongClick = { showBookmarkContextMenu(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun gotoParentFolder() {
        if (folderStack.size > 1) {
            folderStack.pop()
            updateBookmarksContent()
        }
    }

    private fun createBookmarkFolder(bookmark: Bookmark) {
        lifecycleScope.launch {
            val folderName = dialogManager.getBookmarkFolderName()
            folderName?.let { bookmarkManager.insertDirectory(it, bookmark.id) }
        }
    }

    private fun isWideLayout(): Boolean =
        ViewUnit.isLandscape(requireContext()) || ViewUnit.isTablet(requireContext())

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(requireContext()))
        val optionDialog = dialogManager.showOptionDialog(dialogView.root)

        if (bookmark.isDirectory) {
            dialogView.menuContextListNewTab.visibility = View.GONE
            dialogView.menuContextListNewTabOpen.visibility = View.GONE
            dialogView.menuContextListSplitScreen.visibility = View.GONE
        }

        dialogView.menuContextListSplitScreen.setOnClickListener {
            optionDialog.dismissWithAction { splitScreenAction(bookmark.url) ; dialog?.dismiss() }
        }

        dialogView.menuContextListEdit.visibility = View.VISIBLE
        dialogView.menuContextListNewTab.setOnClickListener {
            optionDialog.dismissWithAction {
                addTabAction(getString(R.string.app_name), bookmark.url, false)
                NinjaToast.show(context, getString(R.string.toast_new_tab_successful))
                dialog?.dismiss()
            }
        }
        dialogView.menuContextListNewTabOpen.setOnClickListener {
            optionDialog.dismissWithAction { addTabAction(bookmark.title, bookmark.url, true) ; dialog?.dismiss() }
        }
        dialogView.menuContextListDelete.setOnClickListener {
            lifecycleScope.launch { bookmarkManager.delete(bookmark) }
            optionDialog.dismiss()
        }

        dialogView.menuContextListEdit.setOnClickListener {
            BookmarkEditDialog(
                requireActivity(),
                bookmarkManager,
                bookmark,
                { ViewUnit.hideKeyboard(requireActivity()) },
                { ViewUnit.hideKeyboard(requireActivity()) }
            ).show()
            optionDialog.dismiss()
        }
    }
}

@Composable
fun DialogPanel(
    folder: Bookmark,
    upParentAction: (Bookmark)->Unit,
    createFolderAction: (Bookmark)->Unit,
    closeAction: ()->Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(Modifier.weight(1F, fill = false)) {
            content()
        }
        HorizontalSeparator()
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            if (folder.id != 0) {
                ActionIcon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    iconResId = R.drawable.icon_arrow_left_gest,
                    action =  { upParentAction(folder) }
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Text(
                folder.title,
                Modifier
                    .weight(1F)
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { if (folder.id != 0) upParentAction(folder) }
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.ic_add_folder,
                action =  { createFolderAction(folder) }
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.icon_arrow_down_gest,
                action =  closeAction
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkList(
    bookmarks: List<Bookmark>,
    bookmarkManager: BookmarkManager? = null,
    isWideLayout: Boolean = false,
    shouldReverse: Boolean = true,
    onBookmarkClick: OnBookmarkClick,
    onBookmarkIconClick: OnBookmarkIconClick,
    onBookmarkLongClick: OnBookmarkLongClick,
) {
    LazyVerticalGrid(
        modifier = Modifier.wrapContentHeight(),
        columns = GridCells.Fixed(if (isWideLayout) 2 else 1),
        reverseLayout = shouldReverse
    ){
        itemsIndexed(bookmarks) { _, bookmark ->
            BookmarkItem(
                bookmark = bookmark,
                bitmap =  bookmarkManager?.findFaviconBy(bookmark.url)?.getBitmap(),
                modifier = Modifier.combinedClickable (
                    onClick = { onBookmarkClick(bookmark) },
                    onLongClick = { onBookmarkLongClick(bookmark) }
                ),
                iconClick = {
                    if (!bookmark.isDirectory) onBookmarkIconClick(bookmark)
                    else onBookmarkClick(bookmark)
                }
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    modifier: Modifier,
    bitmap: Bitmap? = null,
    bookmark: Bookmark,
    iconClick: ()->Unit,
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (bitmap!= null) {
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
                action =  iconClick
            )
        }
        Text(
            modifier = Modifier
                .weight(1F)
                .align(Alignment.CenterVertically),
            text = bookmark.title,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionIcon(modifier: Modifier, iconResId: Int, action: ()->Unit) {
    Icon(
        modifier = modifier
            .size(36.dp)
            .padding(end = 5.dp)
            .clickable { action() },
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )

}
@Preview
@Composable
private fun PreviewBookmarkList() {
    AppCompatTheme {
        BookmarkList(
            bookmarks = listOf(Bookmark("test 1","https://www.google.com", false)),
            null,
            isWideLayout = true,
            shouldReverse = true,
            {},
            {},
            {}
        )
    }
}

typealias OnBookmarkClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkLongClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkIconClick = (bookmark: Bookmark) -> Unit
