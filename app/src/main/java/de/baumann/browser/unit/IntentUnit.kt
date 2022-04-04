package de.baumann.browser.unit

import android.content.Intent
import de.baumann.browser.Ninja.R
import android.annotation.SuppressLint
import android.content.Context
import de.baumann.browser.unit.IntentUnit

object IntentUnit {
    fun share(context: Context, title: String?, url: String?) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url)
        context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.menu_share_link)))
    }

    // activity holder
    @JvmStatic
    @SuppressLint("StaticFieldLeak")
    var context: Context? = null
}