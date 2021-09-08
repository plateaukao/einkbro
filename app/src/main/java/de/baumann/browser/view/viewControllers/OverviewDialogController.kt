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
import de.baumann.browser.database.RecordAction
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.adapter.BookmarkAdapter
import de.baumann.browser.view.adapter.RecordAdapter
import de.baumann.browser.view.dialog.DialogManager
import de.baumann.browser.view.dialog.TextInputDialog
import kotlinx.coroutines.launch

class OverviewDialogController(
    private val context: Context,
    private val binding: DialogOveriewBinding,
    private val gotoUrlAction: (String) -> Unit,
    private val addTabAction: (String, String, Boolean) -> Unit,
    private val onBookmarksChanged: () -> Unit,
    private val onHistoryChanged: () -> Unit,
) {
    private val config: ConfigManager by lazy { ConfigManager(context) }
    private val dialogManager: DialogManager by lazy { DialogManager(context as Activity) }
    private val bookmarkManager: BookmarkManager by lazy {  BookmarkManager(context) }

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
        with(dialogView ){
            tvAddFolder.setOnClickListener { dialog.dismissWithAction {  createBookmarkFolder() } }
            tvDelete.setOnClickListener { dialog.dismissWithAction { deleteAllItems() } }
        }
    }

    private fun createBookmarkFolder() {
        lifecycleScope.launch {
            val folderName = getFolderName()
            bookmarkManager.insertDirectory(folderName)
            updateBookmarkList()
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
        when(config.overviewTab) {
            OverviewTab.TabPreview -> openHomePage()
            OverviewTab.Bookmarks -> openBookmarkPage()
            OverviewTab.History -> openHistoryPage()
        }
    }

    private fun hideOverview() {
        binding.root.visibility = GONE
    }

    fun openHistoryPage(amount: Int = 0) {
        binding.root.visibility = VISIBLE

        binding.overviewPreview.visibility = View.INVISIBLE
        recyclerView.visibility = VISIBLE
        toggleOverviewFocus(binding.openHistoryView)

        overViewTab = OverviewTab.History

        val action = RecordAction(context)
        action.open(false)
        var adapter: RecordAdapter? = null
        lifecycleScope.launch {
            val list = action.listEntries((context as Activity), false, amount)
            action.close()
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
                        adapter!!,
                        position
                    )
                }
            )
            recyclerView.adapter = adapter
        }
        adapter?.notifyDataSetChanged()
    }

    fun openBookmarkPage() {
        binding.root.visibility = VISIBLE

        binding.overviewPreview.visibility = View.INVISIBLE
        recyclerView.visibility = View.VISIBLE
        toggleOverviewFocus(binding.openBookmarkView)
        overViewTab = OverviewTab.Bookmarks
        updateBookmarkList()
    }

    private fun updateBookmarkList(bookmarkFolderId: Int = 0) {
        lifecycleScope.launch {
            val adapter = BookmarkAdapter(
                bookmarkManager.getBookmarks(bookmarkFolderId).toMutableList(),
                onItemClick = {
                    if (it.isDirectory) {
                        updateBookmarkList(it.id)
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

            recyclerView.adapter = adapter
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
                            updateBookmarkList()
                            onBookmarksChanged()
                        }
                    }
                    OverviewTab.History -> {
                        BrowserUnit.clearHistory(context)
                        (recyclerView.adapter as RecordAdapter).clear()
                        hideOverview()
                        onHistoryChanged()
                    }
                    else -> {}
                }
            }
        )
    }

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

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
                    (recyclerView.adapter as BookmarkAdapter).remove(bookmark)
                }
            }
        }

        dialogView.menuContextListEdit.setOnClickListener {
            dialog.dismissWithAction {
                dialogManager.showBookmarkEditDialog(
                    bookmarkManager,
                    bookmark,
                    { ViewUnit.hideKeyboard(context as Activity) ; updateBookmarkList() },
                    { ViewUnit.hideKeyboard(context as Activity) }
                )
            }
        }
    }

    private fun showHistoryContextMenu(
        title: String,
        url: String,
        recordAdapter: RecordAdapter,
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
            dialog.dismissWithAction {
                dialogManager.showOkCancelDialog(
                    messageResId = R.string.toast_titleConfirm_delete,
                    okAction = { deleteHistory(recordAdapter, location) }
                )
            }
        }
    }

    private fun deleteHistory(recordAdapter: RecordAdapter, location: Int) {
        val record = recordAdapter.getItemAt(location)
        RecordAction(context).apply {
            open(true)
            deleteHistoryItem(record)
            close()
        }
        recordAdapter.removeAt(location)
        onHistoryChanged()
    }
}

private fun Dialog.dismissWithAction(action: ()-> Unit) {
    dismiss()
    action()
}

enum class OverviewTab {
    TabPreview, Bookmarks, History
}