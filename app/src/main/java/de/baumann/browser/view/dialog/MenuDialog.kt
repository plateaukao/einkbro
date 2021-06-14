package de.baumann.browser.view.dialog

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker.checkSelfPermission
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuBinding
import de.baumann.browser.activity.Settings_Activity
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.task.ScreenshotTask
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView

class MenuDialog(
    private val context: Context,
    private val ninjaWebView: NinjaWebView,
    private val openFavAction: () -> Unit,
    private val closeTabAction: () -> Unit,
    private val saveBookmarkAction: () -> Unit,
    private val searchSiteAction: () -> Unit,
    private val saveEpubAction: () -> Unit,
    private val printPdfAction: () -> Unit,
    private val fontSizeAction: () -> Unit,
) {
    private val config: ConfigManager = ConfigManager(context)

    private lateinit var dialog: AlertDialog
    private val binding: DialogMenuBinding = DialogMenuBinding.inflate(LayoutInflater.from(context))

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews()
        dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM or Gravity.RIGHT)
            window?.setBackgroundDrawableResource(R.drawable.background_with_margin)
        }
        dialog.show()
    }

    private fun initViews() {
        binding.buttonOpenFav.setOnClickListener { dialog.dismissWithAction(openFavAction) }
        binding.buttonSize.setOnClickListener { dialog.dismissWithAction(fontSizeAction) }
        binding.buttonCloseTab.setOnClickListener { dialog.dismissWithAction(closeTabAction) }
        binding.buttonQuit.setOnClickListener { dialog.dismissWithAction { (context as Activity).finish() } }
        binding.buttonBold.setOnClickListener { dialog.dismissWithAction { config.boldFontStyle = !config.boldFontStyle } }
        binding.buttonReader.setOnClickListener { dialog.dismissWithAction { ninjaWebView.toggleReaderMode() } }
        binding.buttonVertical.setOnClickListener { dialog.dismissWithAction { ninjaWebView.toggleVerticalRead() } }
        binding.buttonTouch.setOnClickListener { dialog.dismissWithAction { TouchAreaDialog(context).show() } }
        binding.buttonToolbar.setOnClickListener { dialog.dismissWithAction { ToolbarConfigDialog(context).show() } }
        binding.menuSaveBookmark.setOnClickListener { dialog.dismissWithAction(saveBookmarkAction) }
        binding.menuSaveScreenshot.setOnClickListener { dialog.dismissWithAction(this::saveScreenshot) }
        binding.menuSaveEpub.setOnClickListener { dialog.dismissWithAction(saveEpubAction) }
        binding.menuSavePdf.setOnClickListener { dialog.dismissWithAction(printPdfAction) }
        binding.menuSearchSite.setOnClickListener { dialog.dismissWithAction(searchSiteAction) }
        binding.menuShareLink.setOnClickListener {
            dialog.dismissWithAction { IntentUnit.share(context, ninjaWebView.title, ninjaWebView.url) }
        }
        binding.menuOpenLink.setOnClickListener {
            dialog.dismissWithAction { HelperUnit.showBrowserChooser( context as Activity, ninjaWebView.url, context.getString(R.string.menu_open_with) ) }
        }
        binding.menuSc.setOnClickListener {
            dialog.dismissWithAction { HelperUnit.createShortcut(context, ninjaWebView.title, ninjaWebView.url, ninjaWebView.favicon) }
        }
        binding.menuFav.setOnClickListener {
            dialog.dismissWithAction { config.favoriteUrl = ninjaWebView.url ?: "about:blank" }
        }
        binding.menuDownload.setOnClickListener {
            dialog.dismissWithAction { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
        }
        binding.menuSettings.setOnClickListener {
            dialog.dismissWithAction { context.startActivity(Intent(context, Settings_Activity::class.java)) }
        }
        binding.menuShareClipboard.setOnClickListener {
            dialog.dismissWithAction {
                val clipboard = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("text", ninjaWebView.url)
                clipboard.setPrimaryClip(clip)
                NinjaToast.show(context, R.string.toast_copy_successful)
            }
        }
    }

    private fun saveScreenshot() {
        if (Build.VERSION.SDK_INT in 23..28) {
            val hasPermission = checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                HelperUnit.grantPermissionsStorage(context as Activity)
            } else {
                config.screenshot = 1
                ScreenshotTask(context, ninjaWebView).execute()
            }
        } else {
            config.screenshot = 1
            ScreenshotTask(context, ninjaWebView).execute()
        }
    }
}

private fun Dialog.dismissWithAction(action: ()-> Unit) {
    dismiss()
    action()
}
