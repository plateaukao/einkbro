package info.plateaukao.einkbro.unit

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.pm.ActivityInfo
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
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.ShareChosenReceiver
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.dialog.DialogManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IntentUnit : KoinComponent {
    private val config: ConfigManager by inject()

    fun share(context: Context, title: String?, url: String?) {
        val nonNullUrl = url ?: return
        val strippedUrl = BrowserUnit.stripUrlQuery(nonNullUrl)

        val sharingIntent = buildShareIntent(title, strippedUrl)
        val chooser = Intent.createChooser(
            sharingIntent,
            context.getString(R.string.menu_share_link),
            buildChosenComponentSender(context),
        )
        context.startActivity(chooser)
    }

    /**
     * Re-share to the previously chosen target without showing the chooser.
     * Falls back to the standard chooser if no target is stored or the target
     * is no longer installed.
     */
    fun shareToLastTarget(context: Context, title: String?, url: String?) {
        val nonNullUrl = url ?: return
        val strippedUrl = BrowserUnit.stripUrlQuery(nonNullUrl)

        val pkg = config.browser.lastShareComponentPackage
        val cls = config.browser.lastShareComponentClass
        if (pkg.isEmpty() || cls.isEmpty()) {
            share(context, title, url)
            return
        }

        val targetCls = resolveBestActivityClass(context, pkg, cls)
        val intent = buildShareIntent(title, strippedUrl).apply {
            component = ComponentName(pkg, targetCls)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            config.browser.lastShareComponentPackage = ""
            config.browser.lastShareComponentClass = ""
            share(context, title, url)
        } catch (_: SecurityException) {
            config.browser.lastShareComponentPackage = ""
            config.browser.lastShareComponentClass = ""
            share(context, title, url)
        }
    }

    /**
     * When a package exposes Sharing Shortcuts (Direct Share), the chooser
     * surfaces multiple labelled targets but the broadcast callback only
     * returns the host activity's ComponentName — so re-firing to that
     * captured class lands on the host's default UI, not the shortcut variant
     * the user picked. Heuristic: if the package has another ACTION_SEND
     * activity, prefer it over the captured (shortcut-host) one.
     */
    private fun resolveBestActivityClass(
        context: Context,
        pkg: String,
        capturedCls: String,
    ): String {
        val probe = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(pkg)
        }
        val activities = try {
            context.packageManager.queryIntentActivities(probe, 0)
        } catch (_: Exception) {
            return capturedCls
        }
        if (activities.size <= 1) return capturedCls
        val alternative = activities.firstOrNull { it.activityInfo.name != capturedCls }
        return alternative?.activityInfo?.name ?: capturedCls
    }

    private fun buildShareIntent(title: String?, url: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
        }

    private fun buildChosenComponentSender(context: Context): android.content.IntentSender {
        val callback = Intent(context, ShareChosenReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, callback, flags).intentSender
    }

    fun gotoSystemTtsSettings(activity: Activity) {
        val intent = Intent().apply {
            action = "com.android.settings.TTS_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            EBToast.show(activity, "No Text to Speech settings found")
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
            EBToast.show(activity, "No Text to Speech settings found")
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
            Intent(activity, activity::class.java).apply {
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