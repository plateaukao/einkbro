package info.plateaukao.einkbro.unit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.net.Uri
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.ExtraBrowserActivity
import info.plateaukao.einkbro.activity.SettingActivity
import info.plateaukao.einkbro.view.NinjaToast

object IntentUnit {
    fun share(context: Context, title: String?, url: String?) {
        val nonNullUrl = url ?: return
        val strippedUrl = BrowserUnit.stripUrlQuery(nonNullUrl)

        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        sharingIntent.putExtra(Intent.EXTRA_TEXT, strippedUrl)
        context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.menu_share_link)))
    }

    fun gotoSystemTtsSettings(activity: Activity) {
        val intent = Intent().apply {
            action = "com.android.settings.TTS_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            NinjaToast.show(activity, "No Text to Speech settings found")
        }
    }

    fun gotoSettings(activity: Activity) {
        activity.startActivity(Intent(activity, SettingActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NO_ANIMATION)
        })
    }

    fun launchNewBrowser(activity: Activity, url: String) {
        val intent = Intent(activity, ExtraBrowserActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            action = ACTION_VIEW
            data = Uri.parse(url)
        }

        activity.startActivity(intent)
    }
}