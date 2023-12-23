package info.plateaukao.einkbro.view.dialog

import android.R
import android.app.Activity
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.databinding.DialogEditBookmarkBinding
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.databinding.DialogEditShortcutBinding
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.NinjaToast
import kotlinx.coroutines.launch

class ShortcutEditDialog(
        private val activity: Activity,
        private val title: String,
        private val url: String,
        private val bitmap: Bitmap?,
        private val okAction: () -> Unit,
        private val cancelAction: () -> Unit,
) {
    private val dialogManager: DialogManager = DialogManager(activity)

    fun show() {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope

        val binding = DialogEditShortcutBinding.inflate(LayoutInflater.from(activity))
        binding.passTitle.setText(title)
        binding.passUrl.setText(url)

        DialogManager(activity).showOkCancelDialog(
                title = activity.getString(info.plateaukao.einkbro.R.string.menu_sc),
                view = binding.root,
                okAction = { createBookmark(binding, lifecycleScope) },
                cancelAction = { cancelAction.invoke() }
        )
    }

    private fun createBookmark(binding: DialogEditShortcutBinding, lifecycleScope: LifecycleCoroutineScope) {
        try {
            val title = binding.passTitle.text.toString().trim { it <= ' ' }
            val url = binding.passUrl.text.toString().trim { it <= ' ' }

            lifecycleScope.launch {
                HelperUnit.createShortcut(
                    activity,
                    title,
                    url,
                    bitmap
                )
                okAction.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(activity, info.plateaukao.einkbro.R.string.toast_error)
        }
    }
}