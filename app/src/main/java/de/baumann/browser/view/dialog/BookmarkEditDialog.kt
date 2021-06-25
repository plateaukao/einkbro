package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogEditBookmarkBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.launch

class BookmarkEditDialog(
    private val context: Context,
    private val bookmarkManager: BookmarkManager,
    private val bookmark: Bookmark,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) {
    private val menuView = DialogEditBookmarkBinding.inflate(LayoutInflater.from(context))
    private val lifecycleScope = (context as LifecycleOwner).lifecycleScope

    init {
        menuView.passTitle.setText(bookmark.title)
        if (bookmark.isDirectory) {
            menuView.urlContainer.visibility = View.GONE
        } else {
            menuView.passUrl.setText(bookmark.url)
        }
    }

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(menuView.root) }
        val dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_margin)
        }

        // load all folders
        lifecycleScope.launch {
            val folders = bookmarkManager.getBookmarkFolders().toMutableList().apply {  add(0, Bookmark("Top", "", true)) }
            if (bookmark.isDirectory) folders.remove(bookmark)

            menuView.folderSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, folders)
            val selectedIndex = folders.indexOfFirst { it.id == bookmark.parent }
            menuView.folderSpinner.setSelection(selectedIndex)

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
                    bookmarkManager.insert(bookmark)
                    okAction.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                NinjaToast.show(context, R.string.toast_error)
            }
            dialog.hide()
        }

        menuView.actionCancel.setOnClickListener {
            dialog.cancel()
            cancelAction.invoke()
        }

        dialog.show()
    }
}