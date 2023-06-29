package info.plateaukao.einkbro.view.dialog

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ShareUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReceiveDataDialog(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
) {
    fun show(afterAction: (String) -> Unit) {
        val dialog = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setView(ProgressBar(context))
            setPositiveButton(R.string.done) { _, _ -> ShareUtil.stopBroadcast() }
            setTitle(R.string.menu_receive)
            setOnDismissListener {
                afterAction("")
            }
        }.show()

        ShareUtil.startReceiving(lifecycleCoroutineScope) {
            lifecycleCoroutineScope.launch(Dispatchers.Main) {
                dialog.dismiss()
                afterAction(it)
            }
            ShareUtil.stopBroadcast()
        }
    }
}