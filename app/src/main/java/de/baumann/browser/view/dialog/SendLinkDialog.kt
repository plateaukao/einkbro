package de.baumann.browser.view.dialog

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.unit.ShareUtil

class SendLinkDialog(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
) {

    fun show(url: String) {
        ShareUtil.startBroadcastingUrl(lifecycleCoroutineScope, url)
        AlertDialog.Builder(context, R.style.TouchAreaDialog).apply {
            setView(ProgressBar(context))
            setPositiveButton(R.string.done) { _, _ -> ShareUtil.stopBroadcast() }
            setTitle(R.string.menu_send_link)
        }.show()
    }
}