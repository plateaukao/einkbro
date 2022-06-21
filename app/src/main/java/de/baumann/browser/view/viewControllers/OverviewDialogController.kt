package de.baumann.browser.view.viewControllers

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.widget.LinearLayout
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuContextListBinding
import de.baumann.browser.Ninja.databinding.DialogMenuOverviewBinding
import de.baumann.browser.Ninja.databinding.DialogOveriewBinding
import de.baumann.browser.activity.ExtraBrowserActivity
import de.baumann.browser.database.*
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.ViewUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.compose.BrowseHistoryList
import de.baumann.browser.view.compose.MyTheme
import de.baumann.browser.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class OverviewDialogController(
        private val context: Context,
        private val binding: DialogOveriewBinding,
        private val recordDb: RecordDb,
        private val gotoUrlAction: (String) -> Unit,
        private val addTabAction: (String, String, Boolean) -> Unit,
        private val onHistoryChanged: () -> Unit,
        private val splitScreenAction: (String) -> Unit,
        private val addEmptyTabAction: () -> Unit,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager = DialogManager(context as Activity)
    private val bookmarkManager: BookmarkManager by inject()

    private val historyList = binding.historyList

    private var overViewTab: OverviewTab = OverviewTab.TabPreview

    private val lifecycleScope = (context as LifecycleOwner).lifecycleScope

    init {
        initViews()
    }

    fun addTabPreview(view: View, index: Int) {
        binding.tabContainer.addView(
                view,
                index,
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
        )
    }

    fun removeTabView(view: View) {
        binding.tabContainer.removeView(view)
    }

    fun isVisible() = binding.root.visibility == VISIBLE
    fun show() {
        updateLayout()
        binding.root.visibility = VISIBLE
        openHomePage()
    }

    private fun updateLayout() {
        if (config.isToolbarOnTop) {
            binding.homeButtons.moveToTop()
            binding.overviewPreviewContainer.moveToBelowButtons()
            binding.historyList.moveToBelowButtons()
        } else {
            binding.homeButtons.moveToBottom()
            binding.overviewPreviewContainer.moveToAboveButtons()
            binding.historyList.moveToAboveButtons()
        }
    }

    private fun View.moveToTop() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(id, ConstraintSet.BOTTOM)
            connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        }
        constraintSet.applyTo(binding.root)
    }

    private fun View.moveToBelowButtons() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(id, ConstraintSet.BOTTOM)
            connect(id, ConstraintSet.TOP, binding.homeButtons.id, ConstraintSet.BOTTOM)
        }
        constraintSet.applyTo(binding.root)
    }

    private fun View.moveToAboveButtons() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(id, ConstraintSet.TOP)
            connect(id, ConstraintSet.BOTTOM, binding.homeButtons.id, ConstraintSet.TOP)
        }
        constraintSet.applyTo(binding.root)
    }

    private fun View.moveToBottom() {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(id, ConstraintSet.TOP)
            connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }
        constraintSet.applyTo(binding.root)
    }

    private fun initViews() {

        binding.root.setOnClickListener { hide() }

        binding.openMenu.setOnClickListener { openSubMenu() }
        binding.openTabButton.setOnClickListener { openHomePage() }
        binding.openHistoryButton.setOnClickListener { openHistoryPage() }
        binding.tabPlusBottom.setOnClickListener { addEmptyTabAction() ; hide() }
        binding.tabPlusBottom.setOnLongClickListener{ launchNewBrowser() ; hide() ; true}

        binding.buttonCloseOverview.setOnClickListener { hide() }
        openHomePage()

    }

    private fun openSubMenu() {
        val dialogView = DialogMenuOverviewBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)
        with(dialogView) {
            tvDelete.setOnClickListener { dialog.dismissWithAction { deleteAllItems() } }
        }
    }

    fun hide() {
        binding.root.visibility = GONE
    }

    fun openHistoryPage(amount: Int = 0) {
        updateLayout()
        binding.root.visibility = VISIBLE

        binding.overviewPreview.visibility = INVISIBLE
        historyList.visibility = VISIBLE
        toggleOverviewFocus(binding.openHistoryView)

        overViewTab = OverviewTab.History

        refreshHistoryList(amount)
    }

    private fun refreshHistoryList(amount: Int = 0) {
        lifecycleScope.launch {
            val shouldReverse = !config.isToolbarOnTop
            val finalList = getLatestRecords(amount, shouldReverse)
            historyList.setContent {
                val list = remember { mutableStateOf(finalList) }
                list.value = finalList
                MyTheme {
                    BrowseHistoryList(
                        records = list.value,
                        shouldReverse,
                        isWideLayout(),
                        bookmarkManager,
                        onClick = { position ->
                            val record = list.value[position]
                            gotoUrlAction(record.url)
                            if (record.type == RecordType.Bookmark) {
                                config.addRecentBookmark(Bookmark(record.title
                                    ?: "no title", record.url))
                            }
                            hide()
                        },
                        onLongClick = { position ->
                            val record = list.value[position]
                            showHistoryContextMenu(record)
                        }
                    )
                }
            }
        }
    }

    private suspend fun getLatestRecords(amount: Int, shouldReverse: Boolean): List<Record> {
        val originalList = recordDb.listEntries(false, amount)
        return if (!shouldReverse) originalList.reversed() else originalList
    }

    private fun isWideLayout(): Boolean =
        ViewUnit.isLandscape(context) || ViewUnit.isTablet(context)

    private fun openHomePage() {
        updateLayout()
        binding.overviewPreview.visibility = VISIBLE
        historyList.visibility = GONE
        toggleOverviewFocus(binding.openTabView)
        overViewTab = OverviewTab.TabPreview
    }

    private fun toggleOverviewFocus(view: View) {
        with(binding) {
            when(view) {
                openTabView -> {
                    openTabLayout.visibility = VISIBLE
                    openTabView.visibility = VISIBLE

                    openHistoryLayout.visibility = VISIBLE
                    tabPlusIncognito.visibility = VISIBLE
                    tabPlusBottom.visibility = VISIBLE
                    openMenu.visibility = INVISIBLE
                }
                openHistoryView -> {
                    openHistoryLayout.visibility = VISIBLE
                    openHistoryView.visibility = VISIBLE
                    tabPlusIncognito.visibility = GONE
                    tabPlusBottom.visibility = GONE
                    openMenu.visibility = VISIBLE
                }
            }
            openTabView.visibility = if (binding.openTabView == view) VISIBLE else INVISIBLE
            openHistoryView.visibility = if (binding.openHistoryView == view) VISIBLE else INVISIBLE
        }
    }


    private fun deleteAllItems() {
        dialogManager.showOkCancelDialog(
                messageResId = R.string.hint_database,
                okAction = {
                    when (overViewTab) {
                        OverviewTab.History -> {
                            BrowserUnit.clearHistory(context)
                            hide()
                            onHistoryChanged()
                        }
                        else -> { }
                    }
                }
        )
    }

    private fun showHistoryContextMenu(record: Record) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(context))
        val dialog = dialogManager.showOptionDialog(dialogView.root)

        with (dialogView) {
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

}

private fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}

enum class OverviewTab {
    TabPreview, History
}