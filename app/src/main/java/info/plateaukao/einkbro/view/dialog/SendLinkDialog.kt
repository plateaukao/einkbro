package info.plateaukao.einkbro.view.dialog

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ShareUtil

class SendLinkDialog(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
) {
    fun show(url: String) {
        ShareUtil.startBroadcastingUrl(lifecycleCoroutineScope, url)

        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setView(ProgressBar(context))
            setNeutralButton(R.string.done) { _, _ -> ShareUtil.stopBroadcast() }
            setTitle(R.string.menu_send_link)
            setOnDismissListener { ShareUtil.stopBroadcast() }
        }.show()
    }
}