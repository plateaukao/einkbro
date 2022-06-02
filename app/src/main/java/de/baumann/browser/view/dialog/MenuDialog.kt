package de.baumann.browser.view.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogMenuBinding
import de.baumann.browser.activity.SettingsActivity
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.unit.IntentUnit
import de.baumann.browser.unit.ShareUtil
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MenuDialog(
        private val context: Context,
        private val lifecycleCoroutineScope: LifecycleCoroutineScope,
        private val ninjaWebView: NinjaWebView,
        private val showQuickToggleAction: () -> Unit,
        private val openFavAction: () -> Unit,
        private val closeTabAction: () -> Unit,
        private val saveBookmarkAction: () -> Unit,
        private val searchSiteAction: () -> Unit,
        private val saveEpubAction: () -> Unit,
        private val openEpubAction: () -> Unit,
        private val printPdfAction: () -> Unit,
        private val fontSizeAction: () -> Unit,
        private val saveScreenshotAction: () -> Unit,
        private val toggleSplitScreenAction: () -> Unit,
        private val toggleTouchAction: () -> Unit,
        private val translateAction: () -> Unit,
        private val longPressTranslateAction: () -> Unit,
): KoinComponent {
    private val config: ConfigManager by inject()

    private lateinit var dialog: AlertDialog
    private val binding: DialogMenuBinding = DialogMenuBinding.inflate(LayoutInflater.from(context))

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews()
        dialog = builder.create().apply {
            window?.setGravity(if (config.isToolbarOnTop) Gravity.TOP else Gravity.BOTTOM or Gravity.RIGHT)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
        }
        dialog.show()
    }

    private fun initViews() {
        updateButtonBoldIcon()
        updateButtonWhiteBckndIcon()

        binding.buttonQuickToggle.setOnClickListener { dialog.dismissWithAction(showQuickToggleAction) }
        binding.buttonOpenFav.setOnClickListener { dialog.dismissWithAction(openFavAction) }
        binding.buttonSplitScreen.setOnClickListener { dialog.dismissWithAction(toggleSplitScreenAction) }
        binding.buttonFontSize.setOnClickListener { dialog.dismissWithAction(fontSizeAction) }
        binding.buttonCloseTab.setOnClickListener { dialog.dismissWithAction(closeTabAction) }
        binding.buttonQuit.setOnClickListener { dialog.dismissWithAction { (context as Activity).finishAndRemoveTask() } }
        binding.buttonBold.setOnClickListener { dialog.dismissWithAction { config.boldFontStyle = !config.boldFontStyle } }
        binding.buttonWhiteBackground.setOnClickListener { dialog.dismissWithAction { config.whiteBackground = !config.whiteBackground} }
        binding.buttonReader.setOnClickListener { dialog.dismissWithAction { ninjaWebView.toggleReaderMode() } }
        binding.buttonTranslate.setOnClickListener { dialog.dismissWithAction(translateAction) }
        binding.buttonTranslate.setOnLongClickListener{ dialog.dismissWithAction(longPressTranslateAction); true }
        binding.buttonVertical.setOnClickListener { dialog.dismissWithAction { ninjaWebView.toggleVerticalRead() } }
        binding.buttonTouch.setOnClickListener { dialog.dismissWithAction { TouchAreaDialog(context).show() } }
        binding.buttonTouch.setOnLongClickListener { dialog.dismissWithAction(toggleTouchAction); true }
        binding.buttonToolbar.setOnClickListener { dialog.dismissWithAction { ToolbarConfigDialog(context).show() } }
        binding.menuSaveBookmark.setOnClickListener { dialog.dismissWithAction(saveBookmarkAction) }
        binding.menuSaveScreenshot.setOnClickListener { dialog.dismissWithAction(saveScreenshotAction) }
        binding.menuSaveEpub.setOnClickListener { dialog.dismissWithAction(saveEpubAction) }
        binding.menuOpenEpub.setOnClickListener { dialog.dismissWithAction(openEpubAction) }
        binding.menuSavePdf.setOnClickListener { dialog.dismissWithAction(printPdfAction) }
        binding.menuSearchSite.setOnClickListener { dialog.dismissWithAction(searchSiteAction) }
        binding.menuReceive.setOnClickListener {
            dialog.dismissWithAction {
                ReceiveDataDialog(context, lifecycleCoroutineScope).show { text ->
                    if (text.startsWith("http")) ninjaWebView.loadUrl(text)
                    else {
                        val clip = ClipData.newPlainText("Copied Text", text)
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                        NinjaToast.show(context, "String is Copied!")
                    }
                }
            }
        }
        binding.menuSendLink.setOnClickListener {
            dialog.dismissWithAction { SendLinkDialog(context, lifecycleCoroutineScope).show(ninjaWebView.url ?: "") }
        }
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
            dialog.dismissWithAction { context.startActivity(Intent(context, SettingsActivity::class.java)) }
        }
        binding.menuShareClipboard.setOnClickListener {
            dialog.dismissWithAction { ShareUtil.copyToClipboard(context, ninjaWebView.url ?: "") }
        }
    }

    private fun updateButtonBoldIcon() {
        binding.buttonBold.setImageResource(if (config.boldFontStyle) R.drawable.ic_bold_font_active else R.drawable.ic_bold_font)
    }

    private fun updateButtonWhiteBckndIcon() {
        binding.buttonWhiteBackground.setImageResource(if (config.whiteBackground) R.drawable.ic_white_background_active else R.drawable.ic_white_background)
    }
}