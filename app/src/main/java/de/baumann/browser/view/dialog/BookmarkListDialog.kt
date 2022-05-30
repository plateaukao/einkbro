package de.baumann.browser.view.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Point
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogBookmarksBinding
import de.baumann.browser.Ninja.databinding.DialogMenuContextListBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.adapter.BookmarkAdapter
import de.baumann.browser.viewmodel.BookmarkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class BookmarkListDialog(
        private val context: Context,
        private val lifecycleScope: LifecycleCoroutineScope,
        private val bookmarkViewModel: BookmarkViewModel,
        private val gotoUrlAction: (String) -> Unit,
        private val addTabAction: (String, String, Boolean) -> Unit,
        private val splitScreenAction: (String) -> Unit,
): KoinComponent {
    private val dialogManager: DialogManager = DialogManager(context as Activity)
    private val config: ConfigManager by inject()
    private val bookmarkManager: BookmarkManager by inject()

    private val binding: DialogBookmarksBinding = DialogBookmarksBinding.inflate(LayoutInflater.from(context))
    private val recyclerView: RecyclerView = binding.bookmarkList

    private lateinit var dialog: AlertDialog

    private val narrowLayoutManager = LinearLayoutManager(context).apply { reverseLayout = !config.isToolbarOnTop }

    private val wideLayoutManager = GridLayoutManager(context, 2).apply { reverseLayout = !config.isToolbarOnTop }

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }
        initViews(binding)

        folderStack.push(Bookmark(context.getString(R.string.bookmarks), ""))
        updateBookmarksContent {
            // handle when create folder dialog is popup, this will cause loop for postAction
            if (this::dialog.isInitialized) return@updateBookmarksContent

            dialog = builder.create().apply {
                window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
                setOnDismissListener { updateContentJob?.cancel() ; updateContentJob = null }
                show()
                //window?.setLayout((getScreenWidth() * .9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    fun getScreenWidth(): Int {
        val activity = context as Activity
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        return size.x
    }

    private fun initViews(binding: DialogBookmarksBinding) {
        recyclerView.layoutManager = if (shouldShowWideList()) wideLayoutManager else narrowLayoutManager
        binding.buttonCloseOverview.setOnClickListener { dialog.dismiss() }
        binding.buttonAddFolder.setOnClickListener { createBookmarkFolder() }
        binding.folderTitle.setOnClickListener { gotoParentFolder() }
        binding.buttonUpFolder.setOnClickListener { gotoParentFolder() }
    }

    private fun gotoParentFolder() {
        if (folderStack.size > 1) {
            folderStack.pop()
            adapterStack.pop()
            updateBookmarksContent()
        }
    }

    private suspend fun getFolderName(): String? {
        return TextInputDialog(
                context,
                context.getString(R.string.folder_name),
                context.getString(R.string.folder_name_description),
                ""
        ).show()
    }

    private fun createBookmarkFolder() {
        lifecycleScope.launch {
            val folderName = getFolderName()
            folderName?.let { bookmarkManager.insertDirectory(it) }
        }
    }

    private var updateContentJob: Job? = null
    private val folderStack: Stack<Bookmark> = Stack()
    private val adapterStack: Stack<Pair<Int, BookmarkAdapter>> = Stack()
    private fun updateBookmarksContent(postAction: (()->Unit)? = null) {
        val currentFolder = folderStack.peek()
        binding.folderTitle.text = currentFolder.title
        if (currentFolder.id == BookmarkManager.BOOKMARK_ROOT_FOLDER_ID) {
            binding.buttonUpFolder.visibility = INVISIBLE
            binding.folderTitle.text = context.getString(R.string.bookmarks)
        } else {
            binding.buttonUpFolder.visibility = VISIBLE
        }

        updateContentJob?.cancel()
        updateContentJob = lifecycleScope.launch {
            bookmarkViewModel.bookmarksByParent(currentFolder.id).collect {
                recyclerView.visibility = if (it.isEmpty()) INVISIBLE else VISIBLE
                binding.emptyView.visibility = if (it.isEmpty()) VISIBLE else INVISIBLE

                val existingAdapter = adapterStack.firstOrNull { pair -> pair.first == currentFolder.id }?.second
                recyclerView.adapter = if (existingAdapter != null) {
                    existingAdapter
                } else {
                    val newAdapter = createBookmarkAdapter()
                    adapterStack.push(Pair(currentFolder.id, newAdapter))
                    newAdapter
                }

                (recyclerView.adapter as BookmarkAdapter).submitList(it)

                withContext(Dispatchers.Main) {
                    postAction?.invoke()
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun createBookmarkAdapter(): BookmarkAdapter =
        BookmarkAdapter(
                onItemClick = {
                    if (it.isDirectory) {
                        folderStack.push(it)
                        updateBookmarksContent()
                    } else {
                        gotoUrlAction(it.url)
                        config.addRecentBookmark(it)
                        dialog.dismiss()
                    }
                },
                onTabIconClick = {
                    addTabAction(it.title, it.url, true)
                    dialog.dismiss()
                },
                onItemLongClick = { showBookmarkContextMenu(it) }
        )

    private fun shouldShowWideList(): Boolean =
            ViewUnit.isLandscape(context) || ViewUnit.isTablet(context)

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

        if (bookmark.isDirectory) {
            dialogView.menuContextListFav.visibility = View.GONE
            dialogView.menuContextLinkSc.visibility = View.GONE
            dialogView.menuContextListNewTab.visibility = View.GONE
            dialogView.menuContextListNewTabOpen.visibility = View.GONE
            dialogView.menuContextListFav.visibility = View.GONE
            dialogView.menuContextListSplitScreen.visibility = View.GONE
        }

        dialogView.menuContextListSplitScreen.setOnClickListener {
            dialog.dismissWithAction {
                splitScreenAction(bookmark.url)
                dialog.dismiss()
            }
        }

        dialogView.menuContextListEdit.visibility = View.VISIBLE
        dialogView.menuContextListFav.setOnClickListener {
            dialog.dismissWithAction { config.favoriteUrl = bookmark.url }
        }
        dialogView.menuContextLinkSc.setOnClickListener {
            dialog.dismissWithAction { HelperUnit.createShortcut(context, bookmark.title, bookmark.url, null) }
        }
        dialogView.menuContextListNewTab.setOnClickListener {
            dialog.dismissWithAction {
                addTabAction(context.getString(R.string.app_name), bookmark.url, false)
                NinjaToast.show(context, context.getString(R.string.toast_new_tab_successful))
            }
        }
        dialogView.menuContextListNewTabOpen.setOnClickListener {
            dialog.dismissWithAction {
                addTabAction(bookmark.title, bookmark.url, true)
                dialog.dismiss()
            }
        }
        dialogView.menuContextListDelete.setOnClickListener {
            dialog.dismissWithAction {
                lifecycleScope.launch {
                    bookmarkManager.delete(bookmark)
                }
            }
        }

        dialogView.menuContextListEdit.setOnClickListener {
            dialog.dismissWithAction {
                BookmarkEditDialog(
                        context as Activity,
                        bookmarkManager,
                        bookmark,
                        { ViewUnit.hideKeyboard(context) },
                        { ViewUnit.hideKeyboard(context) }
                ).show()
            }
        }
    }
}