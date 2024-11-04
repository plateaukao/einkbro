package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
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
        binding.title.setText(title)
        binding.url.setText(url)

        dialogManager.showOkCancelDialog(
            title = activity.getString(info.plateaukao.einkbro.R.string.menu_sc),
            view = binding.root,
            okAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createShortcut(binding)
                }
            },
            cancelAction = { cancelAction.invoke() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createShortcut(binding: DialogEditShortcutBinding) {
        HelperUnit.createShortcut(
            activity,
            binding.title.text.toString().trim { it <= ' ' },
            binding.url.text.toString().trim { it <= ' ' },
            bitmap
        )
        okAction.invoke()
    }
}