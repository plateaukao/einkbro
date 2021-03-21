package de.baumann.browser.view.dialog

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.preference.PreferenceManager
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
) {
    private val config: ConfigManager = ConfigManager(context)

    private lateinit var dialog: AlertDialog
    private val binding: DialogMenuBinding = DialogMenuBinding.inflate(LayoutInflater.from(context))

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews()
        dialog = builder.create().apply { window?.setGravity(Gravity.BOTTOM) }
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
            HelperUnit.setFavorite(context, ninjaWebView.url)
        }
        binding.menuSaveBookmark.setOnClickListener {
            dialog.dismiss()
            saveBookmarkAction.invoke()
        }
        binding.menuSaveScreenshot.setOnClickListener {
            dialog.dismiss()
            saveScreenshot()
        }
        binding.contextLinkSaveAs.setOnClickListener {
            dialog.dismiss()
            printPDF()
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

    private fun printPDF() {
        try {
            val title = HelperUnit.fileName(ninjaWebView.url)
            val printManager = context.getSystemService(AppCompatActivity.PRINT_SERVICE) as PrintManager
            val printAdapter = ninjaWebView.createPrintDocumentAdapter(title)
            printManager.print(title, printAdapter, PrintAttributes.Builder().build())
            config.pdfCreated = true
        } catch (e: Exception) {
            NinjaToast.show(context, R.string.toast_error)
            config.pdfCreated = false
            e.printStackTrace()
        }
    }
}