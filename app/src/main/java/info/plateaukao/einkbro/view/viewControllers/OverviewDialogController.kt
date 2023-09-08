package info.plateaukao.einkbro.view.viewControllers

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.database.RecordType
import info.plateaukao.einkbro.databinding.DialogMenuContextListBinding
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.Album
import info.plateaukao.einkbro.view.compose.HistoryAndTabsView
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class OverviewDialogController(
    private val context: Context,
    private val albumList: MutableState<List<Album>>,
    private val albumFocusIndex: MutableState<Int>,
    private val composeView: HistoryAndTabsView,
    private val gotoUrlAction: (String) -> Unit,
    private val addTabAction: (String, String, Boolean) -> Unit,
    private val addIncognitoTabAction: () -> Unit,
    private val onHistoryChanged: () -> Unit,
    private val splitScreenAction: (String) -> Unit,
    private val addEmptyTabAction: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val recordDb: RecordDb by inject()
    private val dialogManager: DialogManager = DialogManager(context as Activity)

    private val lifecycleScope = (context as LifecycleOwner).lifecycleScope

    private val currentRecordList = mutableListOf<Record>()

    fun isVisible() = composeView.visibility == VISIBLE

    fun show() {
        composeView.visibility = VISIBLE
        openHomePage()
    }

    private fun initViews(showHistory: Boolean = false) {
        val currentAlbumList = albumList
        with(composeView) {
            isHistoryOpen = showHistory
            shouldReverse = !config.isToolbarOnTop
            shouldShowTwoColumns = isWideLayout()
            albumList = currentAlbumList
            albumFocusIndex = albumFocusIndex
            onTabIconClick = { openHomePage() }
            onTabClick = { hide(); it.showOrJumpToTop() }
            onTabLongClick = { it.remove() }

            recordList = currentRecordList
            onHistoryIconClick = { openHistoryPage() }
            onHistoryItemClick = { clickHistoryItem(it) }
            onHistoryItemLongClick = { longClickHistoryItem(it) }
            addIncognitoTab = addIncognitoTabAction
            addTab = { hide(); addEmptyTabAction() }
            closePanel = { hide() }
            onDeleteAction = { hide(); deleteAllItems() }
            launchNewBrowserAction =
                { hide(); IntentUnit.launchNewBrowser(context as Activity, config.favoriteUrl) }
        }
    }

    fun showAlbum(index: Int){
        albumFocusIndex.value = index
    }

    private fun clickHistoryItem(record: Record) {
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

    private fun longClickHistoryItem(record: Record) {
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
        recordDb.deleteHistoryItem(record)
        onHistoryChanged()
        refreshHistoryList()
    }
}

private fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}