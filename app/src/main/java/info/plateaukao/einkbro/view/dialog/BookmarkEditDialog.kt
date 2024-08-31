package info.plateaukao.einkbro.view.dialog

import android.R
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.databinding.DialogEditBookmarkBinding
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import kotlinx.coroutines.launch

class BookmarkEditDialog(
    private val activity: Activity,
    private val bookmarkViewModel: BookmarkViewModel,
    private val bookmark: Bookmark,
    private val okAction: () -> Unit,
    private val cancelAction: () -> Unit,
) {
    private val dialogManager: DialogManager = DialogManager(activity)

    fun show() {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope

        val binding = DialogEditBookmarkBinding.inflate(LayoutInflater.from(activity))
        binding.passTitle.setText(bookmark.title)
        if (bookmark.isDirectory) {
            binding.urlContainer.visibility = View.GONE
        } else {
            binding.passUrl.setText(bookmark.url)
        }

        binding.buttonAddFolder.setOnClickListener { addFolder(lifecycleScope, binding) }

        updateFolderSpinner(binding)

        dialogManager.showOkCancelDialog(
            title = activity.getString(info.plateaukao.einkbro.R.string.menu_save_bookmark),
            view = binding.root,
            okAction = { upsertBookmark(binding, lifecycleScope) },
            cancelAction = { cancelAction.invoke() }
        )
    }

    private fun addFolder(
        lifecycleScope: LifecycleCoroutineScope,
        binding: DialogEditBookmarkBinding,
    ) {
        lifecycleScope.launch {
            val folderName = dialogManager.getBookmarkFolderName() ?: return@launch
            bookmarkViewModel.insertDirectory(folderName)
            updateFolderSpinner(binding, folderName)
        }
    }

    private fun updateFolderSpinner(
        binding: DialogEditBookmarkBinding,
        selectedFolderName: String? = null,
    ) {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope
        lifecycleScope.launch {
            val folders = bookmarkViewModel.getBookmarkFolders().toMutableList()
                .apply { add(0, Bookmark("Top", "", true)) }
            if (bookmark.isDirectory) folders.remove(bookmark)

            binding.folderSpinner.adapter =
                ArrayAdapter(activity, R.layout.simple_spinner_dropdown_item, folders)
            val selectedIndex = if (selectedFolderName == null) {
                folders.indexOfFirst { it.id == bookmark.parent }
            } else {
                folders.indexOfFirst { it.title == selectedFolderName }
            }
            binding.folderSpinner.setSelection(selectedIndex)

            binding.folderSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        bookmark.parent = folders[position].id
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    private fun upsertBookmark(
        binding: DialogEditBookmarkBinding,
        lifecycleScope: LifecycleCoroutineScope,
    ) {
        try {
            bookmark.title = binding.passTitle.text.toString().trim { it <= ' ' }
            bookmark.url = binding.passUrl.text.toString().trim { it <= ' ' }
            lifecycleScope.launch {
                bookmarkViewModel.insertBookmark(bookmark)
                okAction.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(activity, info.plateaukao.einkbro.R.string.toast_error)
        }
    }
}