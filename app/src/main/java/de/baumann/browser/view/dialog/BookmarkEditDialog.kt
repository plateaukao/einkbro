package de.baumann.browser.view.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogEditBookmarkBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BookmarkEditDialog(
    private val context: Context,
    layoutInflater: LayoutInflater,
    private val lifecycleScope: CoroutineScope,
    private val bookmarkManager: BookmarkManager,
    private val bookmark: Bookmark,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) {
    val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
    val menuView = DialogEditBookmarkBinding.inflate(layoutInflater)
    init {
        menuView.passTitle.setText(bookmark.title)
        if (bookmark.isDirectory) {
            menuView.urlContainer.visibility = View.GONE
        } else {
            menuView.passUrl.setText(bookmark.url)
        }
    }

    fun show() {
        try {
            // load all folders
            lifecycleScope.launch {
                val folders = bookmarkManager.getBookmarkFolders().toMutableList().apply {  add(0, Bookmark("Top", "", true)) }
                if (bookmark.isDirectory) folders.remove(bookmark)

                menuView.folderSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, folders)
                menuView.folderSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        bookmark.parent = folders[position].id
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) { }
                }
            }

            menuView.actionOk.setOnClickListener {
                try {
                    bookmark.title = menuView.passTitle.text.toString().trim { it <= ' ' }
                    bookmark.url = menuView.passUrl.text.toString().trim { it <= ' ' }
                    lifecycleScope.launch {
                        bookmarkManager.update(bookmark)
                        okAction.invoke()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    NinjaToast.show(context, R.string.toast_error)
                }
                bottomSheetDialog.hide()
            }

            menuView.actionCancel.setOnClickListener {
                bottomSheetDialog.cancel()
                cancelAction.invoke()
            }

            bottomSheetDialog.setContentView(menuView.root)
            bottomSheetDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(context, R.string.toast_error)
        }
    }
}