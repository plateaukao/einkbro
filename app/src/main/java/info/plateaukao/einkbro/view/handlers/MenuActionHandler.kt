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
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.activity.ToolbarConfigActivity
import info.plateaukao.einkbro.browser.BrowserAction
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
    private val dispatch: (BrowserAction) -> Unit,
    private val currentWebView: () -> EBWebView,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager by lazy { DialogManager(activity) }

    fun handleLongClick(menuItemType: MenuItemType) =
        when (menuItemType) {
            MenuItemType.Translate -> dispatch(BrowserAction.ShowTranslationConfigDialog(true))
            MenuItemType.ReceiveData -> dispatch(BrowserAction.ToggleReceiveTextSearch)
            MenuItemType.SendLink -> dispatch(BrowserAction.ToggleTextSearch)
            MenuItemType.TouchSetting -> dispatch(BrowserAction.ToggleTouchPagination)
            MenuItemType.BoldFont -> dispatch(BrowserAction.ShowFontBoldnessDialog)
            MenuItemType.Settings -> IntentUnit.gotoSettings(activity)
            MenuItemType.Tts -> TtsSettingDialogFragment()
                .show(activity.supportFragmentManager, "TtsSettingDialog")

            MenuItemType.ChatWithWeb -> dispatch(BrowserAction.ChatWithWeb(useSplitScreen = true))

            MenuItemType.Instapaper -> dispatch(BrowserAction.ConfigureInstapaper)

            MenuItemType.SaveArchive -> dispatch(BrowserAction.ShowSavedPages)

            else -> Unit
        }

    fun handle(menuItemType: MenuItemType): Any? {
        val ebWebView = currentWebView()
        return when (menuItemType) {
            MenuItemType.Tts -> dispatch(BrowserAction.HandleTtsButton)
            MenuItemType.QuickToggle -> dispatch(BrowserAction.ShowFastToggleDialog)
            MenuItemType.OpenHome -> dispatch(BrowserAction.UpdateAlbum(config.favoriteUrl))
            MenuItemType.CloseTab -> dispatch(BrowserAction.RemoveAlbum)
            MenuItemType.Quit -> activity.finishAndRemoveTask()

            MenuItemType.SplitScreen -> dispatch(BrowserAction.ToggleSplitScreen())
            MenuItemType.Translate -> dispatch(BrowserAction.ShowTranslation)
            MenuItemType.VerticalRead -> dispatch(BrowserAction.ToggleVerticalRead)
            MenuItemType.ReaderMode -> dispatch(BrowserAction.ToggleReaderMode)
            MenuItemType.TouchSetting -> dispatch(BrowserAction.ShowTouchAreaDialog)
            MenuItemType.ToolbarSetting -> {
                activity.startActivity(Intent(activity, ToolbarConfigActivity::class.java).apply {
                    putExtra(ToolbarConfigActivity.EXTRA_IS_READER_MODE, activity is EpubReaderActivity)
                })
            }

            MenuItemType.ReceiveData -> dispatch(BrowserAction.ToggleReceiveLink)
            MenuItemType.SendLink -> dispatch(BrowserAction.SendToRemote(ebWebView.url.orEmpty()))

            MenuItemType.ShareLink -> dispatch(BrowserAction.ShareLink)

            MenuItemType.OpenWith -> HelperUnit.showBrowserChooser(
                activity,
                ebWebView.url,
                activity.getString(R.string.menu_open_with)
            )

            MenuItemType.CopyLink -> ShareUtil.copyToClipboard(
                activity,
                BrowserUnit.stripUrlQuery(ebWebView.url.orEmpty())
            )

            MenuItemType.Shortcut -> dispatch(BrowserAction.CreateShortcut)

            MenuItemType.Highlights -> IntentUnit.gotoHighlights(activity)
            MenuItemType.SetHome -> config.favoriteUrl = ebWebView.url.orEmpty()
            MenuItemType.SaveBookmark -> dispatch(BrowserAction.SaveBookmark())
            MenuItemType.Epub -> dispatch(BrowserAction.ShowEpubDialog)
            MenuItemType.SavePdf -> printPDF(ebWebView)

            MenuItemType.FontSize -> dispatch(BrowserAction.ShowFontSizeChangeDialog)
            MenuItemType.InvertColor -> dispatch(BrowserAction.InvertColors)

            MenuItemType.WhiteBknd -> {
                val isOn = config.toggleWhiteBackground(ebWebView.url.orEmpty())
                if (isOn) ebWebView.updateCssStyle() else ebWebView.reload()
            }

            MenuItemType.BoldFont -> config::boldFontStyle.toggle()
            MenuItemType.BlackFont -> config::blackFontStyle.toggle()
            MenuItemType.Search -> dispatch(BrowserAction.ShowSearchPanel)
            MenuItemType.Download -> BrowserUnit.openDownloadFolder(activity)
            MenuItemType.SaveArchive -> dispatch(BrowserAction.SavePageForLater)
            MenuItemType.Settings -> IntentUnit.gotoSettings(activity)

            MenuItemType.ChatWithWeb -> dispatch(BrowserAction.ChatWithWeb())
            MenuItemType.Instapaper -> dispatch(BrowserAction.AddToInstapaper)
            MenuItemType.AudioOnly -> ebWebView.toggleAudioOnlyMode()
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
