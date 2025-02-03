package info.plateaukao.einkbro.view.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.ToolbarConfigActivity
import info.plateaukao.einkbro.browser.BrowserController
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.ReceiveDataDialog
import info.plateaukao.einkbro.view.dialog.compose.MenuItemType
import info.plateaukao.einkbro.view.dialog.compose.TtsSettingDialogFragment
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MenuActionHandler(
    private val activity: FragmentActivity,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val browserController = activity as BrowserController
    private val dialogManager by lazy { DialogManager(activity) }

    fun handleLongClick(menuItemType: MenuItemType) =
        when (menuItemType) {
            MenuItemType.Translate -> browserController.showTranslationConfigDialog(true)
            MenuItemType.ReceiveData -> browserController.toggleReceiveTextSearch()
            MenuItemType.SendLink -> browserController.toggleTextSearch()
            MenuItemType.TouchSetting -> browserController.toggleTouchPagination()
            MenuItemType.BoldFont -> browserController.showFontBoldnessDialog()
            MenuItemType.Settings -> IntentUnit.gotoSettings(activity)
            MenuItemType.Tts -> TtsSettingDialogFragment()
                .show(activity.supportFragmentManager, "TtsSettingDialog")

            else -> Unit
        }

    fun handle(menuItemType: MenuItemType, ebWebView: EBWebView): Any? {
        return when (menuItemType) {
            MenuItemType.Tts -> browserController.handleTtsButton()
            MenuItemType.QuickToggle -> browserController.showFastToggleDialog()
            MenuItemType.OpenHome -> browserController.updateAlbum(config.favoriteUrl)
            MenuItemType.CloseTab -> browserController.removeAlbum()
            MenuItemType.Quit -> activity.finishAndRemoveTask()

            MenuItemType.SplitScreen -> browserController.toggleSplitScreen()
            MenuItemType.Translate -> browserController.showTranslation()
            MenuItemType.VerticalRead -> browserController.toggleVerticalRead()
            MenuItemType.ReaderMode -> browserController.toggleReaderMode()
            MenuItemType.TouchSetting -> browserController.showTouchAreaDialog()
            MenuItemType.ToolbarSetting -> {
//                ToolbarConfigDialogFragment().show(
//                    activity.supportFragmentManager,
//                    "toolbar_config"
//                )
                activity.startActivity(Intent(activity, ToolbarConfigActivity::class.java))
            }

            MenuItemType.ReceiveData -> browserController.toggleReceiveLink()
            MenuItemType.SendLink -> browserController.sendToRemote(ebWebView.url.orEmpty())

            MenuItemType.ShareLink -> browserController.shareLink()

            MenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                activity,
                ebWebView.url,
                activity.getString(R.string.menu_open_with)
            )

            MenuItemType.CopyLink -> ShareUtil.copyToClipboard(
                activity,
                BrowserUnit.stripUrlQuery(ebWebView.url.orEmpty())
            )

            MenuItemType.Shortcut -> browserController.createShortcut()

            MenuItemType.Highlights -> IntentUnit.gotoHighlights(activity)
            MenuItemType.SetHome -> config.favoriteUrl = ebWebView.url.orEmpty()
            MenuItemType.SaveBookmark -> browserController.saveBookmark()
            MenuItemType.OpenEpub -> openSavedEpub()
            MenuItemType.SaveEpub -> browserController.showSaveEpubDialog()
            MenuItemType.SavePdf -> printPDF(ebWebView)

            MenuItemType.FontSize -> browserController.showFontSizeChangeDialog()
            MenuItemType.InvertColor -> browserController.invertColors()

            MenuItemType.WhiteBknd -> {
                val isOn = config.toggleWhiteBackground(ebWebView.url.orEmpty())
                if (isOn) ebWebView.updateCssStyle() else ebWebView.reload()
            }

            MenuItemType.BoldFont -> config::boldFontStyle.toggle()
            MenuItemType.BlackFont -> config::blackFontStyle.toggle()
            MenuItemType.Search -> browserController.showSearchPanel()
            MenuItemType.Download -> BrowserUnit.openDownloadFolder(activity)
            MenuItemType.SaveArchive -> browserController.showWebArchiveFilePicker()
            MenuItemType.Settings -> IntentUnit.gotoSettings(activity)

            MenuItemType.AddToPocket -> ebWebView.url?.let { browserController.addToPocket(it) }
        }
    }


    private fun showReceiveDataDialog(ebWebView: EBWebView) {
        ReceiveDataDialog(activity, activity.lifecycleScope).show { text ->
            if (text.startsWith("http")) ebWebView.loadUrl(text)
            else {
                val clip = ClipData.newPlainText("Copied Text", text)
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(clip)
                EBToast.show(activity, "String is Copied!")
            }
        }
    }

    private fun openSavedEpub() = if (config.savedEpubFileInfos.isEmpty()) {
        browserController.showOpenEpubFilePicker()
    } else {
        dialogManager.showSaveEpubDialog(
            showAddNewEpub = false,
            openEpubAction = { browserController.showOpenEpubFilePicker() },
        ) { uri ->
            HelperUnit.openFile(activity, uri ?: return@showSaveEpubDialog)
        }
    }

    private fun printPDF(ebWebView: EBWebView) {
        try {
            val title = HelperUnit.fileName(ebWebView.url)
            val printManager =
                activity.getSystemService(FragmentActivity.PRINT_SERVICE) as PrintManager
            val printAdapter = ebWebView.createPrintDocumentAdapter(title) {
                showFileListConfirmDialog()
            }
            printManager.print(title, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFileListConfirmDialog() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.toast_downloadComplete,
            okAction = { BrowserUnit.openDownloadFolder(activity) }
        )
    }
}