package info.plateaukao.einkbro.unit

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserActivity
import info.plateaukao.einkbro.activity.ExtraBrowserActivity
import info.plateaukao.einkbro.activity.HighlightsActivity
import info.plateaukao.einkbro.activity.SettingActivity
import info.plateaukao.einkbro.activity.SettingRoute
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.dialog.DialogManager

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

    fun showFile(activity: Activity, uri: Uri) {
        val intent = Intent(ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }

    fun tts(activity: Activity, text: String) {
        val intent = Intent("android.intent.action.PROCESS_TEXT").apply {
            type = "text/plain"
            component = ComponentName("com.google.android.marvin.talkback", "com.google.android.accessibility.selecttospeak.popup.SelectToSpeakPopupActivity")
            putExtra("android.intent.extra.PROCESS_TEXT_READONLY", true)
            putExtra(Intent.EXTRA_PROCESS_TEXT, text
                .replace("\\n", "")
                .replace("\\t", "")
                .replace("\\\"", "")
            )

        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            NinjaToast.show(activity, "No Text to Speech settings found")
        }
    }

    fun gotoSettings(activity: Activity, route: SettingRoute = SettingRoute.Main) {
        activity.startActivity(SettingActivity.createIntent(activity, route))
    }

    fun gotoHighlights(activity: Activity) {
        activity.startActivity(HighlightsActivity.createIntent(activity).apply {
            addFlags(FLAG_ACTIVITY_NO_ANIMATION)
        })
    }

    fun launchUrl(activity: Activity, url: String) {
        activity.startActivity(
            Intent(activity, BrowserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
            }
        )
    }

    fun readCurrentArticle(activity: Activity) {
        activity.startActivity(
            Intent(activity, BrowserActivity::class.java).apply {
                action = BrowserActivity.ACTION_READ_ALOUD
            }
        )
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

    fun createResultLauncher(
        activity: ComponentActivity,
        postAction: (ActivityResult)->Unit
    ) : ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            postAction(it)
        }

    fun createSaveImageFilePickerLauncher(activity: ComponentActivity) : ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            BrowserUnit.handleSaveImageFilePickerResult(activity, it) { uri ->
                // action to show the downloaded image
                val fileIntent = Intent(ACTION_VIEW).apply {
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                DialogManager(activity).showOkCancelDialog(
                    messageResId = R.string.toast_downloadComplete,
                    okAction = { activity.startActivity(fileIntent) }
                )
            }
        }

    fun isPocketInstalled(context: Context): Boolean = try {
            context.packageManager.getPackageInfo("com.ideashower.readitlater.pro", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            false
        }

    private var isRotated: Boolean = false
    fun rotateScreen(activity: Activity) {
        isRotated = !isRotated
        if (!Build.MANUFACTURER.equals("ONYX")) {
            activity.requestedOrientation = if (!isRotated) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            val intent = Intent().apply {
                action = "com.onyx.action.ROTATION"
                putExtra("rotation", if (isRotated) 1 else 0)
                putExtra("args_rotate_by", 2)
            }
            activity.sendBroadcast(intent)
        }
    }
}