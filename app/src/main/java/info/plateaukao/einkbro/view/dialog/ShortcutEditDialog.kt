package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import android.graphics.Bitmap
import android.view.LayoutInflater
import info.plateaukao.einkbro.databinding.DialogEditShortcutBinding
import info.plateaukao.einkbro.unit.HelperUnit

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
        val binding = DialogEditShortcutBinding.inflate(LayoutInflater.from(activity))
        binding.passTitle.setText(title)
        binding.passUrl.setText(url)

        dialogManager.showOkCancelDialog(
                title = activity.getString(info.plateaukao.einkbro.R.string.menu_sc),
                view = binding.root,
                okAction = { createShortcut(binding) },
                cancelAction = { cancelAction.invoke() }
        )
    }

    private fun createShortcut(binding: DialogEditShortcutBinding) {
        HelperUnit.createShortcut(
            activity,
            binding.passTitle.text.toString().trim { it <= ' ' },
            binding.passUrl.text.toString().trim { it <= ' ' },
            bitmap
        )
        okAction.invoke()
    }
}