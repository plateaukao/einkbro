package de.baumann.browser.view.dialog

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuBinding
import de.baumann.browser.activity.Settings_Activity
import de.baumann.browser.activity.Settings_UIActivity
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.task.ScreenshotTask
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.util.Constants
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
        binding.buttonOpenFav.setOnClickListener {
            dialog.dismiss()
            openFavAction.invoke()
        }
        binding.buttonSize.setOnClickListener {
            dialog.dismiss()
            showFontSizeChangeDialog()
        }
        binding.buttonCloseTab.setOnClickListener {
            dialog.dismiss()
            closeTabAction.invoke()
        }
        binding.buttonQuit.setOnClickListener {
            dialog.dismiss()
            (context as Activity).finish()
        }
        binding.buttonBold.setOnClickListener {
            dialog.dismiss()
            config.boldFontStyle = !config.boldFontStyle
        }
        binding.buttonReader.setOnClickListener {
            dialog.dismiss()
            ninjaWebView.toggleReaderMode()
        }
        binding.buttonVertical.setOnClickListener {
            dialog.dismiss()
            ninjaWebView.toggleVerticalRead()
        }
        binding.buttonTouch.setOnClickListener {
            dialog.dismiss()
            TouchAreaDialog(context).show()
        }
        binding.buttonToolbar.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(context, Settings_UIActivity::class.java)
                    .putExtra(Constants.ARG_LAUNCH_TOOLBAR_SETTING, true)
            context.startActivity(intent)
            (context as Activity).overridePendingTransition(0, 0);
        }
        binding.menuShareLink.setOnClickListener {
            dialog.dismiss()
            IntentUnit.share(context, ninjaWebView.title, ninjaWebView.url)
        }
        binding.menuShareClipboard.setOnClickListener {
            dialog.dismiss()
            val clipboard = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", ninjaWebView.url)
            clipboard.setPrimaryClip(clip)
            NinjaToast.show(context, R.string.toast_copy_successful)
        }
        binding.menuSc.setOnClickListener {
            dialog.dismiss()
            HelperUnit.createShortcut(context, ninjaWebView.title, ninjaWebView.url, ninjaWebView.favicon)
        }
        binding.menuFav.setOnClickListener {
            dialog.dismiss()
            config.favoriteUrl = ninjaWebView.url ?: "about:blank"
        }
        binding.menuSaveBookmark.setOnClickListener {
            dialog.dismiss()
            saveBookmarkAction.invoke()
        }
        binding.menuSaveScreenshot.setOnClickListener {
            dialog.dismiss()
            saveScreenshot()
        }
        binding.menuSaveEpub.setOnClickListener {
            dialog.dismiss()
            saveEpubAction.invoke()
        }
        binding.menuSavePdf.setOnClickListener {
            dialog.dismiss()
            printPdfAction.invoke()
        }
        binding.menuSearchSite.setOnClickListener {
            dialog.dismiss()
            searchSiteAction.invoke()
        }
        binding.menuDownload.setOnClickListener {
            dialog.dismiss()
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        }
        binding.menuSettings.setOnClickListener {
            dialog.dismiss()
            context.startActivity(Intent(context, Settings_Activity::class.java))
        }
    }

    private fun showFontSizeChangeDialog() {
        val fontArray = context.resources.getStringArray(R.array.setting_entries_font)
        val valueArray = context.resources.getStringArray(R.array.setting_values_font)
        val selected = valueArray.indexOf(config.fontSize.toString())
        AlertDialog.Builder(context).apply{
            setTitle("Choose Font Size")
            setSingleChoiceItems(fontArray, selected) { d, which ->
                config.fontSize = valueArray[which].toInt()
                changeFontSize(config.fontSize)
                d.dismiss()
            }
        }.create().show()
    }

    private fun changeFontSize(size: Int) {
        ninjaWebView.settings.textZoom = size
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