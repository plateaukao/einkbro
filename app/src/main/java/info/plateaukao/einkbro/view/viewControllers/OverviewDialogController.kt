package info.plateaukao.einkbro.view.viewControllers

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.DialogMenuContextListBinding
import info.plateaukao.einkbro.activity.ExtraBrowserActivity
import info.plateaukao.einkbro.database.*
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.Album
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.compose.HistoryAndTabsView
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class OverviewDialogController(
    private val context: Context,
    private val composeView: HistoryAndTabsView,
    private val recordDb: RecordDb,
    private val gotoUrlAction: (String) -> Unit,
    private val addTabAction: (String, String, Boolean) -> Unit,
    private val addIncognitoTabAction: () -> Unit,
    private val onHistoryChanged: () -> Unit,
    private val splitScreenAction: (String) -> Unit,
    private val addEmptyTabAction: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager = DialogManager(context as Activity)

    private val lifecycleScope = (context as LifecycleOwner).lifecycleScope

    private val currentRecordList = mutableListOf<Record>()
    val currentAlbumList = mutableListOf<Album>()

    fun addTabPreview(album: Album, index: Int) = currentAlbumList.add(index, album)

    fun removeTabView(album: Album) {
        currentAlbumList.remove(album)
    }

    fun isVisible() = composeView.visibility == VISIBLE

    fun show() {
        composeView.visibility = VISIBLE
        openHomePage()
    }

    private fun initViews(showHistory: Boolean = false) {
        with(composeView) {
            isHistoryOpen = showHistory
            shouldReverse = !config.isToolbarOnTop
            shouldShowTwoColumns = isWideLayout()
            albumList = mutableStateOf(currentAlbumList)
            onTabIconClick = { openHomePage() }
            onTabClick = { hide(); it.show() }
            onTabLongClick = { it.remove() }

            recordList = currentRecordList
            onHistoryIconClick = { openHistoryPage() }
            onHistoryItemClick = { clickHisotryItem(it) }
            onHistoryItemLongClick = { longClickHistoryItem(it) }
            addIncognitoTab = addIncognitoTabAction
            addTab = { hide(); addEmptyTabAction() }
            closePanel = { hide() }
            onDeleteAction = { hide(); deleteAllItems() }
            launchNewBrowserAction = { hide(); launchNewBrowser() }
        }
    }

    fun clickHisotryItem(record: Record) {
        gotoUrlAction(record.url)
        if (record.type == RecordType.Bookmark) {
            config.addRecentBookmark(
                Bookmark(
                    record.title
                        ?: "no title", record.url
                )
            )
        }
        hide()
    }

    fun longClickHistoryItem(record: Record) {
        showHistoryContextMenu(record)
    }

    fun hide() {
        composeView.visibility = GONE
    }

    fun openHistoryPage(amount: Int = 0) {
        composeView.visibility = VISIBLE
        refreshHistoryList(amount)
    }

    private fun refreshHistoryList(amount: Int = 0) {
        lifecycleScope.launch {
            val shouldReverse = !config.isToolbarOnTop
            val finalList = getLatestRecords(amount, shouldReverse)
            currentRecordList.clear()
            currentRecordList.addAll(finalList)
            initViews(showHistory = true)
        }
    }

    private suspend fun getLatestRecords(amount: Int, shouldReverse: Boolean): List<Record> {
        val originalList = recordDb.listEntries(false, amount)
        return if (!shouldReverse) originalList.reversed() else originalList
    }

    private fun isWideLayout(): Boolean =
        ViewUnit.isLandscape(context) || ViewUnit.isTablet(context)

    private fun openHomePage() {
        initViews(showHistory = false)
    }


    private fun deleteAllItems() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.clear_title_history,
            okAction = {
                BrowserUnit.clearHistory(context)
                hide()
                onHistoryChanged()
            })
    }

    private fun showHistoryContextMenu(record: Record) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

        with(dialogView) {
            menuContextListEdit.visibility = GONE
            menuContextListNewTab.setOnClickListener {
                dialog.dismissWithAction {
                    addTabAction(context.getString(R.string.app_name), record.url, false)
                    NinjaToast.show(context, context.getString(R.string.toast_new_tab_successful))
                }
            }
            menuContextListSplitScreen.setOnClickListener {
                dialog.dismissWithAction { splitScreenAction(record.url) }
                hide()
            }
            menuContextListNewTabOpen.setOnClickListener {
                dialog.dismissWithAction {
                    addTabAction(context.getString(R.string.app_name), record.url, true)
                    hide()
                }
            }
            menuContextListDelete.setOnClickListener {
                dialog.dismissWithAction { deleteHistory(record) }
            }
        }
    }

    private fun deleteHistory(record: Record) {
        RecordDb(context).apply {
            open(true)
            deleteHistoryItem(record)
            close()
        }
        onHistoryChanged()
        refreshHistoryList()
    }

    private fun launchNewBrowser() {
        val intent = Intent(context, ExtraBrowserActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            action = Intent.ACTION_VIEW
            data = Uri.parse(config.favoriteUrl)
        }

        context.startActivity(intent)
    }

    fun updateTabView() {
        composeView.focusedAlbumIndex.value = currentAlbumList.indexOfFirst { it.isActivated }
    }

}

private fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}