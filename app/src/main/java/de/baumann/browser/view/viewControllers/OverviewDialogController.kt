package de.baumann.browser.view.viewControllers

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuContextListBinding
import de.baumann.browser.Ninja.databinding.DialogMenuOverviewBinding
import de.baumann.browser.Ninja.databinding.DialogOveriewBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.database.RecordDb
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.adapter.BookmarkAdapter
import de.baumann.browser.view.adapter.RecordAdapter
import de.baumann.browser.view.dialog.BookmarkEditDialog
import de.baumann.browser.view.dialog.DialogManager
import de.baumann.browser.view.dialog.TextInputDialog
import de.baumann.browser.viewmodel.BookmarkViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class OverviewDialogController(
        private val context: Context,
        private val bookmarkViewModel: BookmarkViewModel,
        private val binding: DialogOveriewBinding,
        private val recordDb: RecordDb,
        private val gotoUrlAction: (String) -> Unit,
        private val addTabAction: (String, String, Boolean) -> Unit,
        private val onBookmarksChanged: () -> Unit,
        private val onHistoryChanged: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by inject { parametersOf(context) }
    private val bookmarkManager: BookmarkManager by inject()

    private val recyclerView = binding.homeList2

    private var overViewTab: OverviewTab = OverviewTab.TabPreview

    private val lifecycleScope = (context as LifecycleOwner).lifecycleScope

    init {
        initViews()
    }

    fun addTabPreview(view: View) {
        binding.tabContainer.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    fun removeTabView(view: View) {
        binding.tabContainer.removeView(view)
    }

    fun isVisible() = binding.root.visibility == VISIBLE
    fun show() = showOverview()
    fun hide() = hideOverview()

    private fun showOverview() {
        binding.root.visibility = VISIBLE
        openHomePage()
    }

    private fun initViews() {

        binding.root.setOnClickListener { hideOverview() }

        // allow scrolling in listView without closing the bottomSheetDialog
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
        }

        binding.openMenu.setOnClickListener { openSubMenu() }
        binding.openTabButton.setOnClickListener { openHomePage() }
        binding.openBookmarkButton.setOnClickListener { openBookmarkPage() }
        binding.openHistoryButton.setOnClickListener { openHistoryPage() }

        binding.buttonCloseOverview.setOnClickListener { hideOverview() }
        showCurrentTabInOverview()

    }

    private fun openSubMenu() {
        val dialogView = DialogMenuOverviewBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)
        with(dialogView) {
            tvAddFolder.setOnClickListener { dialog.dismissWithAction { createBookmarkFolder() } }
            tvDelete.setOnClickListener { dialog.dismissWithAction { deleteAllItems() } }
        }
    }

    private fun createBookmarkFolder() {
        lifecycleScope.launch {
            val folderName = getFolderName()
            bookmarkManager.insertDirectory(folderName)
        }
    }

    private suspend fun getFolderName(): String {
        return TextInputDialog(
                context,
                context.getString(R.string.folder_name),
                context.getString(R.string.folder_name_description),
                ""
        ).show() ?: "New Folder"
    }

    private fun showCurrentTabInOverview() {
        when (config.overviewTab) {
            OverviewTab.TabPreview -> openHomePage()
            OverviewTab.Bookmarks -> openBookmarkPage()
            OverviewTab.History -> openHistoryPage()
        }
    }

    private fun hideOverview() {
        binding.root.visibility = GONE
    }

    private var adapter: RecordAdapter? = null
    fun openHistoryPage(amount: Int = 0) {
        binding.root.visibility = VISIBLE

        binding.overviewPreview.visibility = View.INVISIBLE
        recyclerView.visibility = VISIBLE
        toggleOverviewFocus(binding.openHistoryView)

        overViewTab = OverviewTab.History

        lifecycleScope.launch {
            val list = recordDb.listEntries(false, amount)
            adapter = RecordAdapter(
                    list.toMutableList(),
                    { position ->
                        gotoUrlAction(list[position].url)
                        hideOverview()
                    },
                    { position ->
                        showHistoryContextMenu(
                                list[position].title ?: "",
                                list[position].url,
                                position
                        )
                    }
            )
            recyclerView.adapter = adapter
        }

        // reset bookmark parent Id so next time it can be assigned again
        currentBookmarkFolderId = -1
    }

    fun openBookmarkPage() {
        binding.root.visibility = VISIBLE

        binding.overviewPreview.visibility = View.INVISIBLE
        toggleOverviewFocus(binding.openBookmarkView)
        overViewTab = OverviewTab.Bookmarks
        setupBookmarkList()

        recyclerView.visibility = VISIBLE
    }

    // control whether adapter should be reloaded
    private var currentBookmarkFolderId = -1
    private fun setupBookmarkList(bookmarkFolderId: Int = 0) {
        if (currentBookmarkFolderId == 0 && bookmarkFolderId == 0) {
            return
        } else {
            currentBookmarkFolderId = bookmarkFolderId
        }

        val adapter = BookmarkAdapter(
                onItemClick = {
                    if (it.isDirectory) {
                        setupBookmarkList(it.id)
                    } else {
                        gotoUrlAction(it.url)
                        hideOverview()
                    }
                },
                onTabIconClick = {
                    addTabAction(it.title, it.url, true)
                    hideOverview()
                },
                onItemLongClick = { showBookmarkContextMenu(it) }
        )

        lifecycleScope.launch {
            bookmarkViewModel.bookmarksByParent(bookmarkFolderId).collect {
                adapter.submitList(it)
                // when list is loaded from DB, check if it's necessary to swap the adapter for recyclerview.
                // this can prevent list from being gone for a short period of time.
                if (recyclerView.adapter != adapter) {
                    recyclerView.adapter = adapter
                }
            }
        }
    }

    private fun openHomePage() {
        binding.overviewPreview.visibility = VISIBLE
        recyclerView.visibility = GONE
        toggleOverviewFocus(binding.openTabView)
        overViewTab = OverviewTab.TabPreview
    }

    private fun toggleOverviewFocus(view: View) {
        binding.openTabView.visibility = if (binding.openTabView == view) VISIBLE else View.INVISIBLE
        binding.openBookmarkView.visibility = if (binding.openBookmarkView == view) VISIBLE else View.INVISIBLE
        binding.openHistoryView.visibility = if (binding.openHistoryView == view) VISIBLE else View.INVISIBLE
    }


    private fun deleteAllItems() {
        dialogManager.showOkCancelDialog(
                messageResId = R.string.hint_database,
                okAction = {
                    when (overViewTab) {
                        OverviewTab.Bookmarks -> {
                            lifecycleScope.launch {
                                bookmarkManager.deleteAll()
                                onBookmarksChanged()
                            }
                        }
                        OverviewTab.History -> {
                            BrowserUnit.clearHistory(context)
                            (recyclerView.adapter as RecordAdapter).clear()
                            hideOverview()
                            onHistoryChanged()
                        }
                        else -> {
                        }
                    }
                }
        )
    }

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

        if (bookmark.isDirectory) {
            dialogView.menuContextListFav.visibility = GONE
            dialogView.menuContextLinkSc.visibility = GONE
            dialogView.menuContextListNewTab.visibility = GONE
            dialogView.menuContextListNewTabOpen.visibility = GONE
            dialogView.menuContextListFav.visibility = GONE
        }

        dialogView.menuContextListEdit.visibility = VISIBLE
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
                hideOverview()
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

    private fun showHistoryContextMenu(
            title: String,
            url: String,
            location: Int
    ) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

        dialogView.menuContextListEdit.visibility = GONE
        dialogView.menuContextListFav.setOnClickListener {
            dialog.dismissWithAction { config.favoriteUrl = url }
        }
        dialogView.menuContextLinkSc.setOnClickListener {
            dialog.dismissWithAction { HelperUnit.createShortcut(context, title, url, null) }
        }
        dialogView.menuContextListNewTab.setOnClickListener {
            dialog.dismissWithAction {
                addTabAction(context.getString(R.string.app_name), url, false)
                NinjaToast.show(context, context.getString(R.string.toast_new_tab_successful))

            }
        }
        dialogView.menuContextListNewTabOpen.setOnClickListener {
            dialog.dismissWithAction {
                addTabAction(context.getString(R.string.app_name), url, true)
                hideOverview()
            }
        }
        dialogView.menuContextListDelete.setOnClickListener {
            dialog.dismissWithAction { deleteHistory(location) }
        }
    }

    private fun deleteHistory(location: Int) {
        val record = adapter?.getItemAt(location) ?: return
        RecordDb(context).apply {
            open(true)
            deleteHistoryItem(record)
            close()
        }
        adapter?.removeAt(location)
        onHistoryChanged()
    }
}

private fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}

enum class OverviewTab {
    TabPreview, Bookmarks, History
}